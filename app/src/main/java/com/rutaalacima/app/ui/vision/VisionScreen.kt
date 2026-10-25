package com.rutaalacima.app.ui.vision

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import com.rutaalacima.app.ui.components.IconosRuta
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.VisionCasillaEntity
import com.rutaalacima.app.data.social.TipoPost
import com.rutaalacima.app.data.social.Visibilidad
import com.rutaalacima.app.domain.model.BrujulaCompartida
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.domain.model.MetodoCima
import com.rutaalacima.app.domain.model.Travesia
import com.rutaalacima.app.domain.model.VisionBoard
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.fondoPapel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/** Una casilla con la foto que la llena (ruta local o URL), si ya la tiene. */
data class CasillaVista(val casilla: VisionCasillaEntity, val foto: String?)

class VisionViewModel(private val c: AppContainer) : ViewModel() {
    val casillas = combine(c.vision.casillas, c.social.misPublicaciones) { cs, posts ->
        val fotos = posts.associate { it.id to it.foto }
        cs.map { CasillaVista(it, it.publicacionId?.let { id -> fotos[id] }?.takeIf { f -> f.isNotBlank() }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val preferencias = c.contexto.getSharedPreferences("vision", Context.MODE_PRIVATE)
    var modo by mutableStateOf(
        ModoTarjetas.entries.firstOrNull { it.clave == preferencias.getString("modo_tarjetas", null) }
            ?: if (preferencias.getBoolean("modo_texto", false)) ModoTarjetas.TEXTO else ModoTarjetas.IMAGEN
    )
        private set
    fun cambiarModo(nuevo: ModoTarjetas) {
        modo = nuevo
        preferencias.edit().putString("modo_tarjetas", nuevo.clave).remove("modo_texto").apply()
    }
    private val edicion = Mutex()
    internal fun editarCampo(id: Long, campo: CampoTarjeta, texto: String) = viewModelScope.launch {
        edicion.lock()
        try {
            val actual = c.vision.casillas.first().firstOrNull { it.id == id } ?: return@launch
            c.vision.guardar(when (campo) {
                CampoTarjeta.FRASE -> actual.copy(afirmacion = texto, busqueda = VisionBoard.palabras(texto))
                CampoTarjeta.ROTULO -> actual.copy(titulo = texto)
                CampoTarjeta.SUGERENCIA -> actual.copy(sugerencia = texto)
                CampoTarjeta.QUITAR_FOTO -> actual.copy(publicacionId = null)
            })
        } finally { edicion.unlock() }
    }
    val cascada = c.cascada.cascada.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val apodos = c.respuestas.observar("apodo").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val estados = c.respuestas.observar("vision").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    fun guardarApodo(id: Long, texto: String) = viewModelScope.launch { c.respuestas.guardar("apodo", "apodo:vision:$id", texto.trim()) }
    fun guardarPorcentaje(id: Long, pct: Int) = viewModelScope.launch {
        c.respuestas.guardar("vision", "vision:pct:$id", pct.toString())
        c.respuestas.guardar("vision", "vision:estado:$id", if (pct >= 100) "LOGRADA" else if (pct > 0) "CAMINO" else "ABIERTA")
    }

    var armando by mutableStateOf(false)
    /** null = sin mensaje; true = lo armó la IA; false = se armó sin IA. */
    var conIa by mutableStateOf<Boolean?>(null)

    /** La Brújula que se ve: null = la General o la de un año. */
    val anio = c.vision.anioActivo
    val anios = c.vision.anios.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val aniosConBrujula = c.vision.aniosConBrujula.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    fun elegirAnio(a: Int?) { c.vision.elegirAnio(a); conIa = null }

    fun armar(sinIa: Boolean = false) = viewModelScope.launch {
        armando = true
        conIa = runCatching { c.vision.armar(sinIa) }.getOrDefault(false).takeIf { !sinIa && c.vision.anioActivo.value == null }
        llenarCampamentosVacios()
        armando = false
    }

    /**
     * Al llenar el vision board se llena el 9×9: cada campamento que todavía no tiene ningún paso
     * recibe 8 pasos sugeridos (las acciones de su propósito y el banco de acciones de su eje).
     */
    private suspend fun llenarCampamentosVacios() {
        val todas = c.vision.casillas.first().sortedWith(compareBy({ it.orden }, { it.id }))
        val (_, campamentos) = Mandala.repartir(todas, { it.origen }, { it.eje })
        val r = c.respuestas.cargar(Mandala.WORKBOOK)
        campamentos.filterNotNull().filter { cs -> (0 until Mandala.PASOS).all { r[Mandala.clave(cs.id, it)].isNullOrBlank() } }
            .forEach { llenarPasos(it, r) }
    }

    var compartiendo by mutableStateOf(false)
        private set
    /** null = nada; true = publicado; false = no se pudo. */
    var compartido by mutableStateOf<Boolean?>(null)

    /**
     * Publica la Brújula en la comunidad: la imagen (para descargarla o verla en versiones viejas) y la
     * instantánea para explorarla con el zoom. Con cuenta, las fotos que salen se suben a la comunidad.
     */
    fun compartirMandala(ctx: android.content.Context, datos: DatosMandala, textos: MandalaImagen.Textos, vis: Visibilidad,
                         conPasos: Boolean = true, conEvidencias: Boolean = false) = viewModelScope.launch {
        compartiendo = true
        compartido = runCatching {
            val f = withContext(Dispatchers.Default) { MandalaImagen.generar(ctx.applicationContext, datos, textos) }
            val marca = System.currentTimeMillis()
            suspend fun foto(ruta: String?, nombre: String): String = ruta?.let { c.social.fotoParaCompartir(it, "$marca-$nombre") }.orEmpty()
            val camps = (0 until Mandala.CAMPAMENTOS).mapNotNull { i ->
                datos.casilla(i)?.let { cv ->
                    BrujulaCompartida.Campamento(
                        c = datos.codigo(i), t = nombreCampamento(cv), f = foto(cv.foto, "c$i"),
                        p = (0 until Mandala.PASOS).map { p ->
                            BrujulaCompartida.Paso(
                                t = if (conPasos) datos.paso(i, p) else "", e = BrujulaCompartida.letra(datos.estado(i, p)),
                                f = if (conEvidencias) foto(datos.fotoPaso(i, p), "e$i-$p") else "",
                            )
                        },
                    )
                }
            }
            val pr = datos.progreso
            val inst = BrujulaCompartida.Instantanea(
                cumbre = datos.tituloCumbre, fotoCumbre = foto(datos.cumbre?.foto, "cumbre"), camps = camps, mm = datos.ritmo.mm,
                ganados = pr.hechos, escritos = pr.escritos, evidencias = if (conEvidencias) datos.evidencias else 0,
                fecha = java.time.LocalDate.now().toString(), conPasos = conPasos,
            )
            c.social.publicar(TipoPost.VISION, datos.tituloCumbre + "\n#MetodoCima9x52", Uri.fromFile(f), null, vis,
                metaTitulo = textos.etiqueta, brujula = BrujulaCompartida.codificar(inst))
            f.delete()
        }.isSuccess
        compartiendo = false
    }

    /** Pasos de la mandala 9×9 (respuestas del workbook "mandala"). */
    val mandala = c.respuestas.observar(Mandala.WORKBOOK).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val cumbreFrase = c.perfil.perfil.map { it.cumbreFrase }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun guardarPaso(casilla: VisionCasillaEntity, paso: Int, texto: String) = viewModelScope.launch {
        c.respuestas.guardar(Mandala.WORKBOOK, Mandala.clave(casilla.id, paso), texto.trim())
    }

    /** Registra la jornada de hoy en un paso (Método Cima 9×52): el paso se gana al juntar las jornadas que pide. */
    fun avanzarPaso(casilla: VisionCasillaEntity, paso: Int, campamentos: List<Long?>) = viewModelScope.launch {
        val r = c.respuestas.cargar(Mandala.WORKBOOK)
        val hoy = java.time.LocalDate.now()
        val ritmo = MetodoCima.calcular(MetodoCima.pasosDe(campamentos, r), hoy)
        val a = MetodoCima.avanzar(MetodoCima.leer(casilla.id, paso, r), hoy, ritmo.dificultad)
        c.respuestas.guardar(Mandala.WORKBOOK, MetodoCima.claveJornadas(casilla.id, paso), a.jornadas.sorted().joinToString(","))
        c.respuestas.guardar(Mandala.WORKBOOK, MetodoCima.claveRequeridas(casilla.id, paso), a.requeridas.toString())
        a.cumplidoEn?.let { c.respuestas.guardar(Mandala.WORKBOOK, Mandala.claveHecho(casilla.id, paso), it.toString()) }
    }

    /** Llena los pasos vacíos de un campamento: primero las acciones de su propósito, luego el banco de acciones de su eje. */
    fun sugerirPasos(casilla: VisionCasillaEntity) = viewModelScope.launch { llenarPasos(casilla, c.respuestas.cargar(Mandala.WORKBOOK)) }

    /**
     * Llena los pasos vacíos de un campamento: primero las acciones de los propósitos de ese eje (o, en
     * Confluencia, de los propósitos sin eje); luego el banco de acciones multi-eje (Bono 5). Las acciones
     * del banco que activan otros ejes quedan enlazadas: son pasos de confluencia.
     */
    private suspend fun llenarPasos(casilla: VisionCasillaEntity, r: Map<String, String>) {
        val codigo = casilla.origen.removePrefix("campamento:").takeIf { it in Mandala.CAMPAMENTOS_FIJOS } ?: casilla.eje
        val actuales = List(Mandala.PASOS) { r[Mandala.clave(casilla.id, it)].orEmpty() }
        val banco = runCatching { c.bancos.bancos().acciones }.getOrDefault(emptyList())
        val ejesDe = banco.associate { it.texto.trim().lowercase() to it.ejes }
        val sugerencias = buildList {
            val propios = c.planificador.propositos.first().filter { p ->
                when (codigo) { "CON" -> p.eje == null; "CAM" -> false; else -> p.eje == codigo }
            } + listOfNotNull(casilla.origen.removePrefix("proposito:").toLongOrNull()?.let { id -> c.planificador.propositos.first().firstOrNull { it.id == id } })
            propios.distinctBy { it.id }.forEach { p -> addAll(c.planificador.acciones(p.id).first().filter { !it.hecha }.map { it.texto }) }
            val delBanco = when (codigo) {
                "CON" -> banco.filter { it.ejes.size >= 3 }
                "CAM" -> banco.filter { a -> Regex("mentor|grupo|cordada|comunidad|red |amig|familia|acompa|equipo", RegexOption.IGNORE_CASE).containsMatchIn(a.texto) }
                    .ifEmpty { banco }
                else -> banco.filter { codigo in it.ejes }
            }
            addAll(delBanco.sortedByDescending { it.ejes.size }.map { it.texto }.shuffled())
        }
        var enlazados = (0 until Mandala.PASOS).count { !r[Travesia.claveEnlaces(casilla.id, it)].isNullOrBlank() }
        Mandala.completar(actuales, sugerencias).forEachIndexed { i, t ->
            if (t != actuales[i]) {
                c.respuestas.guardar(Mandala.WORKBOOK, Mandala.clave(casilla.id, i), t)
                // Solo las acciones que activan 3 ejes o más quedan como pasos de confluencia (hasta 2 por campamento)
                val ejes = ejesDe[t.trim().lowercase()].orEmpty()
                if (ejes.size >= 3 && enlazados < Travesia.ENLACES_AUTO) {
                    c.respuestas.guardar(Mandala.WORKBOOK, Travesia.claveEnlaces(casilla.id, i), Travesia.enlacesDeAccion(ejes, codigo.orEmpty()))
                    enlazados++
                }
            }
        }
    }

    /** Reconoce una caída (Guía de Caídas): el paso conserva sus jornadas y pide menos para volver. */
    fun registrarCaida(casilla: VisionCasillaEntity, paso: Int, tipo: String, campamentos: List<Long?>) = viewModelScope.launch {
        val r = c.respuestas.cargar(Mandala.WORKBOOK)
        val hoy = java.time.LocalDate.now()
        val ritmo = MetodoCima.calcular(MetodoCima.pasosDe(campamentos, r), hoy)
        val req = Travesia.requeridasTrasCaida(MetodoCima.leer(casilla.id, paso, r), ritmo.dificultad)
        c.respuestas.guardar(Mandala.WORKBOOK, Travesia.claveCaida(casilla.id, paso), "$tipo|$hoy")
        c.respuestas.guardar(Mandala.WORKBOOK, MetodoCima.claveRequeridas(casilla.id, paso), req.toString())
    }

    fun guardarEnlaces(casilla: VisionCasillaEntity, paso: Int, enlaces: String) = viewModelScope.launch {
        c.respuestas.guardar(Mandala.WORKBOOK, Travesia.claveEnlaces(casilla.id, paso), enlaces)
    }

    /**
     * Cierre del año (Desde la Cima): guarda las 7 lecciones y el resumen, y lleva la imagen de la
     * Brújula del año al diario de vida como reflexión privada con las lecciones.
     */
    fun cerrarAnio(ctx: android.content.Context, datos: DatosMandala, textos: MandalaImagen.Textos, lecciones: List<String>, titulo: String) = viewModelScope.launch {
        val anio = java.time.LocalDate.now().year
        lecciones.forEachIndexed { n, l -> c.respuestas.guardar(Mandala.WORKBOOK, Travesia.claveLeccion(anio, n), l.trim()) }
        compartiendo = true
        compartido = runCatching {
            val f = withContext(Dispatchers.Default) { MandalaImagen.generar(ctx.applicationContext, datos, textos) }
            val texto = (listOf(titulo) + lecciones.mapIndexedNotNull { n, l -> l.trim().takeIf { it.isNotEmpty() }?.let { "${n + 1}. $it" } }).joinToString("\n")
            c.social.publicar(TipoPost.REFLEXION, texto, Uri.fromFile(f), null, Visibilidad.PRIVADA, metaTitulo = textos.etiqueta, anio = anio)
            f.delete()
            val pr = datos.progreso
            c.respuestas.guardar(Mandala.WORKBOOK, Travesia.claveCierre(anio),
                Travesia.resumen(pr.hechos, pr.escritos, datos.ritmo.mm, datos.evidencias, datos.confluencias))
        }.isSuccess
        compartiendo = false
    }

    /** Nueva montaña: libera los pasos ganados (quedan en el diario) y deja los que van a medio camino. */
    fun nuevaMontana(campamentos: List<Long?>) = viewModelScope.launch {
        val r = c.respuestas.cargar(Mandala.WORKBOOK)
        Travesia.clavesALiberar(campamentos, r).forEach { c.respuestas.guardar(Mandala.WORKBOOK, it, "") }
        // Los pasos liberados se llenan con pasos nuevos
        val todas = c.vision.casillas.first()
        val r2 = c.respuestas.cargar(Mandala.WORKBOOK)
        campamentos.filterNotNull().mapNotNull { id -> todas.firstOrNull { it.id == id } }.forEach { llenarPasos(it, r2) }
    }

    fun ponerFoto(cv: VisionCasillaEntity, uri: Uri) = c.appScope.launch {
        edicion.lock()
        try { c.vision.casillas.first().firstOrNull { it.id == cv.id }?.let { c.vision.ponerFoto(it, uri) } }
        finally { edicion.unlock() }
    }
    fun crearCampamento(codigo: String) = viewModelScope.launch { c.vision.crearCampamento(codigo); llenarCampamentosVacios() }

    /** Fotos de mis publicaciones (para las evidencias de los pasos). */
    val fotos = c.social.misPublicaciones.map { ps -> ps.associate { it.id to it.foto } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Kit de Evidencias: la foto real del paso ganado se guarda como publicación privada (y aparece en el diario de vida). */
    fun ponerEvidencia(casilla: VisionCasillaEntity, paso: Int, uri: Uri) = c.appScope.launch {
        val texto = c.respuestas.cargar(Mandala.WORKBOOK)[Mandala.clave(casilla.id, paso)].orEmpty()
        val p = c.social.publicar(TipoPost.EVIDENCIA, texto, uri, casilla.eje, Visibilidad.PRIVADA, metaTitulo = casilla.afirmacion.take(80))
        c.respuestas.guardar(Mandala.WORKBOOK, Mandala.claveFoto(casilla.id, paso), p.id)
    }

    /** El portal al ganar un paso: qué suelto y qué llevo. */
    fun guardarPortal(casilla: VisionCasillaEntity, paso: Int, soltar: String, llevar: String) = viewModelScope.launch {
        c.respuestas.guardar(Mandala.WORKBOOK, Mandala.claveSoltar(casilla.id, paso), soltar)
        c.respuestas.guardar(Mandala.WORKBOOK, Mandala.claveLlevar(casilla.id, paso), llevar)
    }
    fun borrar(cv: VisionCasillaEntity) = viewModelScope.launch {
        edicion.lock()
        try { c.vision.borrar(cv) } finally { edicion.unlock() }
    }
}

/**
 * Mi vision board: la IA propone las casillas con la cumbre, los propósitos, las metas y los
 * ejes de la persona, y ella las llena con sus propias fotos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionScreen(onBack: () -> Unit, onGuia: (String) -> Unit = {}, compartir: Boolean = false, onIrA: (String) -> Unit = {}) {
    val vm = rutaViewModel { VisionViewModel(it) }
    val ctx = LocalContext.current
    val casillas by vm.casillas.collectAsStateWithLifecycle()
    val anioActivo by vm.anio.collectAsStateWithLifecycle()
    val anios by vm.anios.collectAsStateWithLifecycle()
    val aniosConBrujula by vm.aniosConBrujula.collectAsStateWithLifecycle()
    var paraFoto by remember { mutableStateOf<VisionCasillaEntity?>(null) }
    val cascada by vm.cascada.collectAsStateWithLifecycle()
    val apodos by vm.apodos.collectAsStateWithLifecycle()
    val estados by vm.estados.collectAsStateWithLifecycle()
    var giradas by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var visor by rememberSaveable { mutableStateOf<Long?>(null) }
    var visorNivel by rememberSaveable { mutableStateOf<Int?>(null) }
    var bloqueAbierto by rememberSaveable { mutableStateOf<Int?>(null) }
    var cambioModo by remember { mutableStateOf(0) }
    fun cambiarModo(nuevo: ModoTarjetas) {
        if (nuevo != vm.modo) {
            if (nuevo.muestraDorso != vm.modo.muestraDorso) { giradas = emptyList(); cambioModo++ }
            if (nuevo == ModoTarjetas.EDICION) bloqueAbierto = null
            vm.cambiarModo(nuevo)
        }
    }
    fun girar(id: Long) { giradas = if (id in giradas) giradas - id else giradas + id }
    fun editarTarjeta(id: Long) {
        cambiarModo(ModoTarjetas.EDICION)
        if (!(vm.modo.muestraDorso xor (id in giradas))) girar(id)
    }
    fun abrirVisor(id: Long, editar: Boolean = false) {
        if (editar) editarTarjeta(id)
        visorNivel = null
        visor = id
    }
    fun abrirNiveles(nivel: Int) { visor = null; visorNivel = nivel }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val reparto = remember(casillas) { Mandala.repartir(casillas, { it.casilla.origen }, { it.casilla.eje }) }
    val (cumbre, campamentos) = reparto
    // Solo se presentan las nueve posiciones fijas. Las demás entidades se conservan.
    val tarjetasVisor = remember(reparto) { listOfNotNull(cumbre) + campamentos.filterNotNull() }
    fun irSeccion(indice: Int) { scope.launch { gridState.scrollToItem(indice) } }
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val c = paraFoto
        if (uri != null && c != null) vm.ponerFoto(c, uri)
        paraFoto = null
    }
    fun pedirFoto(c: VisionCasillaEntity) {
        paraFoto = c
        elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    fun ideas(c: VisionCasillaEntity) {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(VisionBoard.urlIdeas(c.busqueda)))) }
    }
    val conFoto = remember(tarjetasVisor) { tarjetasVisor.count { it.foto != null } }
    val pasos by vm.mandala.collectAsStateWithLifecycle()
    val cumbreFrase by vm.cumbreFrase.collectAsStateWithLifecycle()
    val fotos by vm.fotos.collectAsStateWithLifecycle()
    var paraEvidencia by remember { mutableStateOf<Pair<VisionCasillaEntity, Int>?>(null) }
    val elegirEvidencia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val e = paraEvidencia
        if (uri != null && e != null) vm.ponerEvidencia(e.first, e.second, uri)
        paraEvidencia = null
    }

    val hoy = java.time.LocalDate.now()
    val fotosDePasos = remember(pasos, fotos) {
        pasos.filterKeys { it.endsWith("#foto") }.values.mapNotNull { id -> fotos[id]?.let { id to it } }.toMap()
    }
    val campamentoIds = remember(reparto) { campamentos.map { it?.casilla?.id } }
    val datos = remember(reparto, cumbreFrase, pasos, fotosDePasos, hoy) { DatosMandala(cumbre, cumbreFrase, campamentos, pasos, fotosDePasos) }
    val metasVision = remember(cumbre?.casilla?.id, campamentoIds, cascada, hoy) { calcularMetasVision(cumbre, campamentos, cascada, hoy) }
    val porcentajes = remember(datos, estados, metasVision) { porcentajesVision(datos, estados, metasVision.avancePorCodigo) }
    val guardarPaso: (VisionCasillaEntity, Int, String) -> Unit = remember(vm) {
        { casilla, paso, texto -> vm.guardarPaso(casilla, paso, texto) }
    }
    val acciones = remember(vm, elegirFoto, ctx, onIrA) { AccionesTarjeta(
        onFoto = ::pedirFoto, onCampo = vm::editarCampo, onApodo = vm::guardarApodo,
        onPorcentaje = vm::guardarPorcentaje, onBorrar = { cv -> vm.borrar(cv); if (visor == cv.id) visor = null }, onIdeas = ::ideas,
        onMeta = { visor = null; onIrA(it) },
    ) }
    val pintarTarjeta: @Composable (CasillaVista, Boolean, Boolean, Modifier, Int, String?, (() -> Unit)?) -> Unit = { cv, compacta, grande, modificador, indice, rumbo, onPasos ->
        val id = cv.casilla.id
        TarjetaVision(cv, apodos["apodo:vision:$id"].orEmpty(), vm.modo.muestraDorso xor (id in giradas),
            cambioModo, indice, porcentajes[id] ?: 0, vm.modo,
            acciones, onEditar = { editarTarjeta(id) }, onGirar = { girar(id) }, onVer = { abrirVisor(id) }, onAmpliar = { abrirVisor(id, true) },
            compacta = compacta, cumbre = grande, rumbo = rumbo, onPasos = onPasos,
            metas = metasVision.sostenPorId[id].orEmpty(), apodosMetas = apodos, modifier = modificador)
    }

    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(anioActivo?.let { stringResource(R.string.brujula_de_anio, it) } ?: stringResource(R.string.vision_titulo)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "navegacion", span = { GridItemSpan(2) }) {
                NavegacionVision(vm.modo, ::cambiarModo, casillas.isNotEmpty(),
                    onSeccion = { irSeccion(when (it) { 0 -> 2; 1 -> 3; else -> 4 }) },
                    onGrande = { abrirNiveles(0) })
            }
            item(key = "introduccion", span = { GridItemSpan(2) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Arriba se elige cuál Brújula: la General (toda tu vida) o la de un año
                    SelectorBrujula(anioActivo, anios, aniosConBrujula, onElegir = { giradas = emptyList(); bloqueAbierto = null; vm.elegirAnio(it) })
                    IntroduccionVision(
                        anio = anioActivo, vacia = casillas.isEmpty(), armando = vm.armando, conIa = vm.conIa,
                        conFoto = conFoto, total = tarjetasVisor.size, onArmar = { vm.armar(it) },
                    )
                }
            }
            if (casillas.isNotEmpty()) {
                item(key = "cumbre", span = { GridItemSpan(2) }) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CabeceraSeccionVision(stringResource(R.string.vision_seccion_cumbre)) { abrirNiveles(0) }
                        cumbre?.let { cv -> key(cv.casilla.id) { pintarTarjeta(cv, false, true, Modifier.widthIn(max = 400.dp), 0, null, null) } }
                            ?: Text(cumbreFrase, style = MaterialTheme.typography.headlineSmall)
                    }
                }
                item(key = "rosa", span = { GridItemSpan(2) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CabeceraSeccionVision(stringResource(R.string.vision_seccion_rosa)) { abrirNiveles(1) }
                        RosaVision(cumbre, campamentos, onCrear = vm::crearCampamento) { cv, indice, rumbo, modificador ->
                            pintarTarjeta(cv, true, false, modificador, indice + 1, rumbo,
                                rumbo?.let { _ -> {
                                    val camp = campamentos.indexOfFirst { it?.casilla?.id == cv.casilla.id }
                                    if (vm.modo != ModoTarjetas.EDICION) bloqueAbierto = Mandala.bloqueDe(camp)
                                    irSeccion(4)
                                } })
                        }
                    }
                }
            }
            if (casillas.isNotEmpty()) item(key = "brujula", span = { GridItemSpan(2) }) {
                val nf = java.text.NumberFormat.getIntegerInstance()
                val cierreTitulo = stringResource(R.string.brujula_cierre_post, java.time.LocalDate.now().year)
                val textos = MandalaImagen.Textos(
                    etiqueta = stringResource(R.string.mandala_imagen_etiqueta),
                    avance = stringResource(R.string.mandala_progreso, datos.progreso.escritos, datos.progreso.hechos),
                    montana = stringResource(R.string.metodo_altura, nf.format(datos.ritmo.mm)),
                    marca = stringResource(R.string.mandala_imagen_marca),
                )
                RutaCard {
                    MandalaSeccion(
                        datos,
                        modoTexto = vm.modo == ModoTarjetas.TEXTO, modoEdicion = vm.modo == ModoTarjetas.EDICION,
                        onVerGrande = { abrirNiveles(2) }, bloqueAbierto = bloqueAbierto, onBloque = { bloqueAbierto = it },
                        onPaso = guardarPaso,
                        onAvanzar = { cs, p -> vm.avanzarPaso(cs, p, campamentoIds) },
                        onSugerir = { vm.sugerirPasos(it) },
                        onAgregarCampamento = { vm.crearCampamento(it) },
                        onEvidencia = { cs, p ->
                            paraEvidencia = cs to p
                            elegirEvidencia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPortal = { cs, p, so, ll -> vm.guardarPortal(cs, p, so, ll) },
                        onCaida = { cs, p, tipo -> vm.registrarCaida(cs, p, tipo, campamentoIds) },
                        onEnlaces = { cs, p, en -> vm.guardarEnlaces(cs, p, en) },
                        onGuia = onGuia,
                        onCierre = { lecciones, _ -> vm.cerrarAnio(ctx, datos, textos, lecciones, cierreTitulo) },
                        onNuevaMontana = { vm.nuevaMontana(campamentoIds) },
                        onCompartir = { vis, pasos, evid -> vm.compartirMandala(ctx, datos, textos, vis, pasos, evid) },
                        compartiendo = vm.compartiendo,
                        abrirCompartir = compartir,
                    )
                }
            }
            if (casillas.isNotEmpty()) {
                item(key = "privacidad", span = { GridItemSpan(2) }) {
                    Text(stringResource(R.string.vision_foto_privada), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    vm.compartido?.let { ok ->
        LaunchedEffect(ok) {
            com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(ctx.getString(if (ok) R.string.mandala_compartido else R.string.mandala_no_compartido))
            vm.compartido = null
        }
    }

    visorNivel?.let { nivel ->
        VisorNivelesBrujula(datos, nivel, bloqueAbierto ?: 4, vm.modo, ::cambiarModo,
            onPaso = guardarPaso,
            onAbrirBloque = { bloque ->
                visorNivel = null
                if (vm.modo != ModoTarjetas.EDICION) bloqueAbierto = bloque
                irSeccion(4)
            }, onCerrar = { visorNivel = null })
    }
    visor?.let { id ->
        if (tarjetasVisor.isNotEmpty()) VisorTarjetas(tarjetasVisor, id, apodos, vm.modo, ::cambiarModo,
            giradas, cambioModo, onGirar = ::girar, onEditar = ::editarTarjeta, acciones = acciones,
            porcentajes = porcentajes, metas = metasVision.sostenPorId, onCerrar = { visor = null })
    }
}

internal fun colorEje(codigo: String): Color = when (codigo) {
    "VOL" -> Color(0xFF8E3B26); "MAE" -> Color(0xFF5C4A8A); "VOZ" -> Color(0xFF2F6F7A)
    "VAL" -> Color(0xFF6B5530); "EVO" -> Color(0xFF3F7A4A); else -> Color(0xFF6B2A1A)
}


/** General · 2026 · 2027…: la Brújula de toda la vida y una por año. El "+" marca los años aún sin crear. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorBrujula(anio: Int?, anios: List<Int>, conBrujula: Set<Int>, onElegir: (Int?) -> Unit) {
    val hoy = java.time.LocalDate.now().year
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = anio == null, onClick = { onElegir(null) }, label = { Text(stringResource(R.string.brujula_general)) },
            leadingIcon = { Icon(IconosRuta.Cima, null, Modifier.size(18.dp)) })
        anios.forEach { a ->
            FilterChip(selected = anio == a, onClick = { onElegir(a) },
                label = { Text(if (a == hoy) stringResource(R.string.brujula_este_anio, a) else a.toString()) },
                trailingIcon = if (a !in conBrujula) ({ Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) }) else null)
        }
    }
}

/** Vacía: qué es y dos maneras de empezar. Armada: el avance y, plegado, cómo se arma y "volver a proponer". */
@Composable
private fun IntroduccionVision(anio: Int?, vacia: Boolean, armando: Boolean, conIa: Boolean?, conFoto: Int, total: Int, onArmar: (Boolean) -> Unit) {
    var abierta by rememberSaveable { mutableStateOf(false) }
    RutaCard {
        if (vacia) {
            Text(anio?.let { stringResource(R.string.brujula_de_anio, it) } ?: stringResource(R.string.vision_titulo), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(anio?.let { stringResource(R.string.brujula_anio_intro, it) } ?: stringResource(R.string.vision_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        } else {
            Text(stringResource(R.string.vision_progreso, conFoto, total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { conFoto.toFloat() / total.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
        }
        when {
            armando -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.vision_armando))
            }
            vacia && anio != null -> Button(onClick = { onArmar(true) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.brujula_crear_anio, anio))
            }
            vacia -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onArmar(false) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.vision_armar))
                }
                OutlinedButton(onClick = { onArmar(true) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.vision_con_mis_datos))
                }
            }
            else -> {
                TextButton(onClick = { abierta = !abierta }) {
                    Text(stringResource(R.string.vision_rearmar)); Spacer(Modifier.width(4.dp))
                    Icon(if (abierta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                }
                AnimatedVisibility(abierta) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.vision_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { onArmar(anio != null) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.vision_rearmar))
                        }
                        Text(stringResource(R.string.vision_rearmar_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (conIa == false) Text(stringResource(R.string.vision_sin_ia), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
