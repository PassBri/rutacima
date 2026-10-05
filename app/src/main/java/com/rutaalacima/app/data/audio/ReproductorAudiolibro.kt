package com.rutaalacima.app.data.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import com.rutaalacima.app.data.content.ContentRepository
import com.rutaalacima.app.data.content.Locucion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/** De dónde sale la voz del capítulo que suena. */
enum class FuenteAudio { VOZ, AUTOR }

data class EstadoAudiolibro(
    /** Hay un audiolibro cargado (sonando o en pausa). */
    val activo: Boolean = false,
    val workbookId: String = "",
    val guia: String = "",
    val seccion: Int = 0,
    val totalSecciones: Int = 0,
    val tituloSeccion: String = "",
    val fuente: FuenteAudio = FuenteAudio.VOZ,
    val reproduciendo: Boolean = false,
    val cargando: Boolean = false,
    /** Avance dentro del capítulo, de 0 a 1. */
    val avance: Float = 0f,
    val velocidad: Float = 1f,
    /** Aviso para mostrar una vez (sin voz en español, grabación que no cargó…). */
    val aviso: AvisoAudio? = null,
    /** Temporizador para dormir: hora (epoch ms) en que se pausa, o null. */
    val apagadoEn: Long? = null,
    /** Pausar al terminar el capítulo que suena. */
    val alTerminarCapitulo: Boolean = false,
)

enum class AvisoAudio { SIN_VOZ, ERROR_GRABACION }

/**
 * Audiolibro de las guías. Cada capítulo suena con la grabación del autor si existe
 * ([AudiosRepository]); si no, lo lee la voz del teléfono (TextToSpeech). Al terminar un capítulo
 * sigue con el siguiente. Vive en el [com.rutaalacima.app.AppContainer], así que sigue sonando al
 * cambiar de pantalla, y [AudiolibroService] lo mantiene con la pantalla apagada.
 */
class ReproductorAudiolibro(
    private val context: Context,
    private val contenido: ContentRepository,
    private val audios: AudiosRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _estado = MutableStateFlow(EstadoAudiolibro())
    val estado: StateFlow<EstadoAudiolibro> = _estado.asStateFlow()

    private var fragmentos: List<String> = emptyList()
    private var fragmento = 0
    /** Cambia cada vez que se empieza a hablar: descarta avisos de enunciados viejos. */
    private var generacion = 0

    private var tts: TextToSpeech? = null
    private var ttsListo = false
    private var alEstarListo: (() -> Unit)? = null

    private var player: MediaPlayer? = null
    /** El [player] ya está preparado (antes de eso no admite start/pause/seek). */
    private var playerListo = false
    /** El motor de voz no arrancó: en el próximo intento se crea de nuevo. */
    private var ttsFallo = false
    private var ticker: Job? = null
    private var apagado: Job? = null
    /** Dónde retomar la grabación al prepararse (ms). */
    private var inicioMs = 0
    private var ticks = 0
    /** El temporizador detuvo al final de un capítulo: el próximo play sigue con el siguiente. */
    private var pendienteSiguiente = false

    /** Dónde quedaste en cada guía: "capítulo:frase:ms" (sigue ahí aunque cierres la app). */
    private val posiciones = context.getSharedPreferences("audiolibro_posiciones", Context.MODE_PRIVATE)

    data class Posicion(val seccion: Int, val fragmento: Int, val ms: Int)

    fun posicion(workbookId: String): Posicion? = posiciones.getString(workbookId, null)?.split(':')?.let { p ->
        if (p.size == 3) Posicion(p[0].toIntOrNull() ?: 0, p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0) else null
    }

    private fun guardarPosicion() {
        val e = _estado.value
        if (!e.activo) return
        val ms = player?.takeIf { playerListo }?.let { runCatching { it.currentPosition }.getOrDefault(0) } ?: 0
        posiciones.edit().putString(e.workbookId, "${e.seccion}:$fragmento:$ms").apply()
    }

    /** Sigue la guía donde quedaste (o desde el principio). */
    fun continuar(workbookId: String, seccionPorDefecto: Int = 0) {
        val p = posicion(workbookId)
        if (p == null) reproducir(workbookId, seccionPorDefecto) else reproducir(workbookId, p.seccion, p.fragmento, p.ms)
    }

    /** Temporizador para dormir: pausa en [minutos] (null lo apaga). */
    fun temporizador(minutos: Int?) {
        apagado?.cancel(); apagado = null
        if (minutos == null) { _estado.update { it.copy(apagadoEn = null, alTerminarCapitulo = false) }; return }
        val fin = System.currentTimeMillis() + minutos * 60_000L
        _estado.update { it.copy(apagadoEn = fin, alTerminarCapitulo = false) }
        apagado = scope.launch { delay(minutos * 60_000L); pausar(); _estado.update { it.copy(apagadoEn = null) } }
    }

    /** Pausa cuando termine el capítulo que suena. */
    fun dormirAlTerminarCapitulo() {
        apagado?.cancel(); apagado = null
        _estado.update { it.copy(apagadoEn = null, alTerminarCapitulo = true) }
    }

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val atributos = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val foco = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(atributos)
        .setOnAudioFocusChangeListener { cambio ->
            if (cambio == AudioManager.AUDIOFOCUS_LOSS || cambio == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) scope.launch { pausar() }
        }
        .build()

    /** Si se desconectan los audífonos, pausa (como cualquier reproductor). */
    private val ruidoso = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { if (i?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pausar() }
    }
    private var ruidosoRegistrado = false

    // ------------------------------------------------------------------ Controles

    /** Empieza (o retoma) la guía en el capítulo [seccion], desde una frase o un segundo dados. */
    fun reproducir(workbookId: String, seccion: Int, desdeFragmento: Int = 0, desdeMs: Int = 0) {
        scope.launch {
            val wb = runCatching { contenido.workbook(workbookId) }.getOrNull() ?: return@launch
            val s = wb.sections.getOrNull(seccion) ?: return@launch
            liberarPlayer(); tts?.stop(); generacion++; alEstarListo = null; pendienteSiguiente = false
            fragmentos = Locucion.fragmentos(s); fragmento = desdeFragmento.coerceIn(0, (fragmentos.size - 1).coerceAtLeast(0))
            inicioMs = desdeMs
            val fuente = audios.fuente(workbookId, seccion)
            _estado.update {
                it.copy(
                    activo = true, workbookId = workbookId, guia = wb.title, seccion = seccion, totalSecciones = wb.sections.size,
                    tituloSeccion = s.title, fuente = if (fuente != null) FuenteAudio.AUTOR else FuenteAudio.VOZ,
                    reproduciendo = true, cargando = true, avance = 0f,
                )
            }
            pedirFoco()
            AudiolibroService.iniciar(context)
            if (fuente != null) sonarGrabacion(fuente) else hablar(fragmento)
        }
    }

    fun alternar() = if (_estado.value.reproduciendo) pausar() else reanudar()

    fun pausar() {
        val e = _estado.value
        if (!e.activo || !e.reproduciendo) return
        guardarPosicion()
        if (e.fuente == FuenteAudio.AUTOR) player?.takeIf { playerListo && it.isPlaying }?.pause() else { generacion++; alEstarListo = null; tts?.stop() }
        _estado.update { it.copy(reproduciendo = false, cargando = false) }
        soltarRuidoso()
    }

    fun reanudar() {
        val e = _estado.value
        if (!e.activo || e.reproduciendo) return
        if (pendienteSiguiente) { pendienteSiguiente = false; continuar(e.workbookId); return }
        pedirFoco()
        _estado.update { it.copy(reproduciendo = true) }
        if (e.fuente == FuenteAudio.AUTOR) {
            val p = player
            if (p == null) reproducir(e.workbookId, e.seccion)
            else if (playerListo) { p.start(); aplicarVelocidad(p); empezarTicker(); registrarRuidoso() }
            // si aún se está preparando, onPrepared arranca porque reproduciendo = true
        } else hablar(fragmento)
    }

    /** Detiene y cierra el audiolibro (quita la notificación). */
    fun detener() {
        guardarPosicion()
        apagado?.cancel(); apagado = null
        generacion++; alEstarListo = null
        tts?.stop(); liberarPlayer(); soltarRuidoso()
        audioManager.abandonAudioFocusRequest(foco)
        _estado.value = EstadoAudiolibro(velocidad = _estado.value.velocidad)
    }

    /** Retrocede o avanza: una frase con la voz del teléfono, 15 segundos en una grabación. */
    fun saltar(direccion: Int) {
        val e = _estado.value
        if (!e.activo) return
        if (e.fuente == FuenteAudio.AUTOR) {
            val p = player?.takeIf { playerListo } ?: return
            p.seekTo((p.currentPosition + direccion * 15_000).coerceIn(0, p.duration.coerceAtLeast(0)))
            actualizarAvanceGrabacion()
        } else {
            fragmento = (fragmento + direccion).coerceIn(0, (fragmentos.size - 1).coerceAtLeast(0))
            _estado.update { it.copy(avance = avanceVoz()) }
            if (e.reproduciendo) hablar(fragmento)
        }
    }

    fun capitulo(direccion: Int) {
        val e = _estado.value
        if (!e.activo) return
        val n = e.seccion + direccion
        if (n in 0 until e.totalSecciones) reproducir(e.workbookId, n)
    }

    /** Cambia la velocidad (0.75× a 2×). */
    fun velocidad(v: Float) {
        val nueva = v.coerceIn(0.5f, 2f)
        _estado.update { it.copy(velocidad = nueva) }
        tts?.setSpeechRate(nueva)
        player?.takeIf { playerListo }?.let { p ->
            runCatching {
                val sonando = p.isPlaying
                p.playbackParams = p.playbackParams.setSpeed(nueva)
                if (!sonando && p.isPlaying) p.pause()   // cambiar la velocidad arranca el audio: respeta la pausa
            }
        }
        if (_estado.value.reproduciendo && _estado.value.fuente == FuenteAudio.VOZ) hablar(fragmento)
    }

    fun olvidarAviso() = _estado.update { it.copy(aviso = null) }

    // ------------------------------------------------------------------ Voz del teléfono

    private fun avanceVoz() = if (fragmentos.isEmpty()) 0f else fragmento.toFloat() / fragmentos.size

    private fun hablar(desde: Int) {
        if (ttsFallo) { ttsFallo = false; runCatching { tts?.shutdown() }; tts = null; ttsListo = false }
        val motor = tts
        if (motor == null || !ttsListo) {
            alEstarListo = { hablar(desde) }
            if (motor == null) crearVoz()
            return
        }
        if (fragmentos.isEmpty()) { terminarCapitulo(); return }
        fragmento = desde.coerceIn(0, fragmentos.lastIndex)
        val gen = ++generacion
        motor.setSpeechRate(_estado.value.velocidad)
        motor.speak(fragmentos[fragmento], TextToSpeech.QUEUE_FLUSH, Bundle(), "f-$gen-$fragmento")
        registrarRuidoso()
        _estado.update { it.copy(cargando = false, reproduciendo = true, avance = avanceVoz()) }
        guardarPosicion()
    }

    private fun crearVoz() {
        tts = TextToSpeech(context.applicationContext) { status ->
            scope.launch {
                // Sin motor de voz, onInit puede llegar antes de asignar `tts`: revisa el estado primero
                if (status != TextToSpeech.SUCCESS) {
                    ttsFallo = true; alEstarListo = null
                    _estado.update { it.copy(aviso = AvisoAudio.SIN_VOZ, reproduciendo = false, cargando = false) }
                    return@launch
                }
                val motor = tts ?: return@launch
                // Las guías están en español: busca una voz en español (Colombia primero)
                val idioma = listOf(Locale("es", "CO"), Locale("es", "US"), Locale("es", "MX"), Locale("es", "ES"), Locale("es"))
                    .firstOrNull { motor.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
                if (idioma == null) _estado.update { it.copy(aviso = AvisoAudio.SIN_VOZ) } else motor.setLanguage(idioma)
                motor.setAudioAttributes(atributos)
                motor.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { scope.launch { alTerminarFrase(id) } }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        // Si el motor falla, pausa y avisa (no salta frase tras frase hasta el final del libro)
                        scope.launch {
                            if (id?.split('-')?.getOrNull(1)?.toIntOrNull() != generacion) return@launch
                            pausar(); _estado.update { it.copy(aviso = AvisoAudio.SIN_VOZ) }
                        }
                    }
                })
                ttsListo = true
                alEstarListo?.invoke(); alEstarListo = null
            }
        }
    }

    private fun alTerminarFrase(id: String?) {
        val partes = id?.split('-') ?: return
        if (partes.size != 3 || partes[1].toIntOrNull() != generacion) return   // enunciado viejo (pausa, salto…)
        if (!_estado.value.reproduciendo) return
        val siguiente = (partes[2].toIntOrNull() ?: fragmento) + 1
        if (siguiente >= fragmentos.size) terminarCapitulo() else hablar(siguiente)
    }

    // ------------------------------------------------------------------ Grabación del autor

    private fun sonarGrabacion(fuente: String) {
        val p = MediaPlayer()
        player = p
        playerListo = false
        runCatching {
            p.setAudioAttributes(atributos)
            if (fuente.startsWith("file:")) {
                java.io.FileInputStream(fuente.removePrefix("file:")).use { f -> p.setDataSource(f.fd) }
            } else if (fuente.startsWith("asset:")) {
                context.assets.openFd(fuente.removePrefix("asset:")).use { fd -> p.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
            } else p.setDataSource(fuente)
            p.setOnPreparedListener {
                if (player !== it) return@setOnPreparedListener
                playerListo = true
                if (inicioMs > 0) { runCatching { it.seekTo(inicioMs) }; inicioMs = 0 }
                // pause() no es válido en estado Prepared: si está en pausa, solo queda preparado
                if (_estado.value.reproduciendo) { it.start(); aplicarVelocidad(it); empezarTicker(); registrarRuidoso() }
                _estado.update { e -> e.copy(cargando = false) }
            }
            p.setOnCompletionListener { if (player === it) terminarCapitulo() }
            p.setOnErrorListener { mp, _, _ -> if (player === mp) usarVozEnVez(); true }
            p.prepareAsync()
        }.onFailure { usarVozEnVez() }
    }

    /** La grabación no cargó (sin internet, archivo dañado): sigue con la voz del teléfono. */
    private fun usarVozEnVez() {
        liberarPlayer()
        _estado.update { it.copy(fuente = FuenteAudio.VOZ, aviso = AvisoAudio.ERROR_GRABACION) }
        if (_estado.value.reproduciendo) hablar(0)
    }

    private fun aplicarVelocidad(p: MediaPlayer) {
        runCatching { p.playbackParams = p.playbackParams.setSpeed(_estado.value.velocidad) }
    }

    private fun empezarTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                actualizarAvanceGrabacion()
                if (++ticks % 10 == 0) guardarPosicion()   // cada 5 s
                delay(500)
            }
        }
    }

    private fun actualizarAvanceGrabacion() {
        val p = player?.takeIf { playerListo } ?: return
        val d = runCatching { p.duration }.getOrDefault(0)
        if (d > 0) _estado.update { it.copy(avance = (p.currentPosition.toFloat() / d).coerceIn(0f, 1f)) }
    }

    private fun liberarPlayer() {
        ticker?.cancel(); ticker = null
        player?.let { runCatching { it.reset(); it.release() } }
        player = null
        playerListo = false
    }

    // ------------------------------------------------------------------ Comunes

    private fun terminarCapitulo() {
        val e = _estado.value
        val haySiguiente = e.seccion + 1 < e.totalSecciones
        if (haySiguiente) posiciones.edit().putString(e.workbookId, "${e.seccion + 1}:0:0").apply()
        else posiciones.edit().remove(e.workbookId).apply()   // guía terminada: la próxima vez empieza de nuevo
        when {
            e.alTerminarCapitulo -> {
                // Temporizador "al terminar el capítulo": se detiene aquí; al darle play sigue con el siguiente
                liberarPlayer(); soltarRuidoso()
                pendienteSiguiente = haySiguiente
                _estado.update { it.copy(reproduciendo = false, alTerminarCapitulo = false, avance = 1f) }
            }
            haySiguiente -> reproducir(e.workbookId, e.seccion + 1)
            else -> { liberarPlayer(); soltarRuidoso(); _estado.update { it.copy(reproduciendo = false, avance = 1f) } }
        }
    }

    private fun pedirFoco() { audioManager.requestAudioFocus(foco) }

    private fun registrarRuidoso() {
        if (ruidosoRegistrado) return
        ContextCompat.registerReceiver(context, ruidoso, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
        ruidosoRegistrado = true
    }

    private fun soltarRuidoso() {
        if (!ruidosoRegistrado) return
        runCatching { context.unregisterReceiver(ruidoso) }
        ruidosoRegistrado = false
    }
}
