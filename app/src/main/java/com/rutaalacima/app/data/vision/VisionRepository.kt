package com.rutaalacima.app.data.vision

import android.content.Context
import android.net.Uri
import com.rutaalacima.app.R
import com.rutaalacima.app.data.coach.CoachRepository
import com.rutaalacima.app.data.local.RutaDatabase
import com.rutaalacima.app.data.local.VisionCasillaEntity
import com.rutaalacima.app.data.local.puntajes
import com.rutaalacima.app.data.social.SocialRepository
import com.rutaalacima.app.data.social.TipoPost
import com.rutaalacima.app.data.social.Visibilidad
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.domain.model.VisionBoard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Vision board armado con IA: propone las casillas con los datos de la persona y guarda la
 * foto de cada casilla como una publicación de visión privada (así también llega a la web).
 */
class VisionRepository(
    private val context: Context,
    private val db: RutaDatabase,
    private val coach: CoachRepository,
    private val social: SocialRepository,
) {
    private val preferencias = context.getSharedPreferences("vision", Context.MODE_PRIVATE)
    private val _anio = MutableStateFlow(preferencias.getInt("brujula_anio", 0).takeIf { it > 0 })
    /** La Brújula que se ve: null = la General (toda la vida) o la de un año. */
    val anioActivo: StateFlow<Int?> = _anio.asStateFlow()
    fun elegirAnio(anio: Int?) {
        _anio.value = anio
        preferencias.edit().apply { if (anio == null) remove("brujula_anio") else putInt("brujula_anio", anio) }.apply()
    }
    private val todas: Flow<List<VisionCasillaEntity>> = db.visionDao().observe()
    /** Las casillas de la Brújula elegida (las de los otros años no se mezclan). */
    val casillas: Flow<List<VisionCasillaEntity>> = combine(todas, _anio) { cs, a -> cs.filter { Mandala.anioDeOrigen(it.origen) == a } }
    /** Los años con Brújula, más este año y el que viene. */
    val anios: Flow<List<Int>> = combine(todas, _anio) { cs, a ->
        val h = LocalDate.now().year
        (cs.mapNotNull { Mandala.anioDeOrigen(it.origen) } + listOf(h, h + 1) + listOfNotNull(a)).distinct().sorted()
    }
    val aniosConBrujula: Flow<Set<Int>> = todas.map { cs -> cs.mapNotNull { Mandala.anioDeOrigen(it.origen) }.toSet() }
    private suspend fun actualesDelAnio() = db.visionDao().todas().filter { Mandala.anioDeOrigen(it.origen) == _anio.value }

    /** Resultado de armar el tablero: true si lo propuso la IA, false si se usaron las reglas sin conexión. */
    /**
     * Arma las 9 casillas de la Brújula de la Cima: la cumbre y los 8 campamentos fijos. Las casillas
     * se actualizan en su lugar (conservan su id y, con él, los pasos del 9×9); las que ya tienen foto
     * no cambian. Las de antes sin foto que no tienen lugar se quitan.
     */
    suspend fun armar(sinIa: Boolean = false): Boolean {
        val anio = _anio.value
        // La del año nace de su palabra y sus metas (sin IA); la general puede armarse con IA o solo con tus datos
        val ia = if (sinIa || anio != null) null else coach.consultar(VisionBoard.instruccionIa())?.let(VisionBoard::desdeIa)
        val local = propuestaLocal()
        val actuales = actualesDelAnio().sortedWith(compareBy({ it.orden }, { it.id })).toMutableList()
        val (cumbre, campamentos) = Mandala.repartir(actuales, { it.origen }, { it.eje })
        val ubicadas = listOf(cumbre) + campamentos
        VisionBoard.ORIGENES.forEachIndexed { orden, origen0 ->
            val origen = Mandala.origenEnAnio(anio, origen0)
            val p = ia?.firstOrNull { it.origen == origen0 } ?: local.firstOrNull { it.origen == origen0 } ?: return@forEachIndexed
            val existente = ubicadas[orden]
            when {
                existente == null -> db.visionDao().guardar(
                    VisionCasillaEntity(orden = orden, titulo = p.titulo, afirmacion = p.afirmacion, eje = p.eje,
                        sugerencia = p.sugerencia, busqueda = p.busqueda, origen = origen)
                )
                existente.publicacionId != null -> db.visionDao().guardar(existente.copy(orden = orden, origen = origen))
                else -> db.visionDao().guardar(existente.copy(orden = orden, titulo = p.titulo, afirmacion = p.afirmacion,
                    eje = p.eje, sugerencia = p.sugerencia, busqueda = p.busqueda, origen = origen))
            }
        }
        val usadas = ubicadas.filterNotNull().map { it.id }.toSet()
        actuales.filter { it.id !in usadas && it.publicacionId == null }.forEach { db.visionDao().borrar(it.id) }
        return ia != null
    }

    /** Crea la casilla de un campamento fijo que falta (con la frase propuesta por el método). */
    suspend fun crearCampamento(codigo: String) {
        val origen = Mandala.origenCampamento(codigo)
        if (actualesDelAnio().any { Mandala.origenBase(it.origen) == origen }) return
        val p = propuestaLocal().firstOrNull { it.origen == origen } ?: return
        db.visionDao().guardar(
            VisionCasillaEntity(orden = VisionBoard.ORIGENES.indexOf(origen), titulo = p.titulo, afirmacion = p.afirmacion, eje = p.eje,
                sugerencia = p.sugerencia, busqueda = p.busqueda, origen = Mandala.origenEnAnio(_anio.value, origen))
        )
    }

    private suspend fun propuestaLocal(): List<VisionBoard.Propuesta> {
        val perfil = db.perfilDao().get()
        val inicio = perfil?.anioInicioPlan ?: LocalDate.now().year
        val propositos = db.planificadorDao().observePropositos().first()
            .map { VisionBoard.Proposito(it.id, it.titulo, it.eje, inicio + it.horizonte - 1) }
        val delAnio = _anio.value
        val anio = delAnio ?: LocalDate.now().year
        val metas = db.planificadorDao().observeMetas(anio).first().map { VisionBoard.Meta(it.id, it.titulo, it.eje, it.anio) }
        val debil = db.evaluacionDao().observeUltima().first()?.let { e ->
            VisionBoard.CODIGOS.zip(e.puntajes()).minByOrNull { it.second }?.first
        }
        val cumbre = perfil?.cumbreFrase.orEmpty()
        if (delAnio == null) return VisionBoard.proponer(cumbre, propositos, metas, debil, textos())
        // En la Brújula del año mandan sus metas; la cumbre sale de la palabra del año
        val palabra = db.respuestaDao().getWorkbook("vida").firstOrNull { it.clave == "vida#$delAnio#palabra" }?.valor?.trim().orEmpty()
        val frase = when {
            palabra.isNotEmpty() -> context.getString(R.string.brujula_anio_cumbre, delAnio, palabra.lowercase())
            cumbre.isNotBlank() -> context.getString(R.string.brujula_anio_tramo, delAnio, cumbre.trim())
            else -> ""
        }
        val r = VisionBoard.proponer(frase, emptyList(), metas, debil, textos())
        return listOf(r.first().copy(titulo = if (palabra.isNotEmpty()) "$delAnio: $palabra" else "$delAnio")) + r.drop(1)
    }

    private fun textos(): VisionBoard.Textos {
        fun s(id: Int) = context.getString(id)
        return VisionBoard.Textos(
            tituloCumbre = s(R.string.vision_titulo_cumbre),
            tituloProposito = s(R.string.vision_titulo_proposito),
            tituloMeta = s(R.string.vision_titulo_meta),
            sugerenciaCumbre = s(R.string.vision_sug_cumbre),
            sugerenciaRelacionada = s(R.string.vision_sug_relacionada),
            ejes = mapOf(
                "VOL" to Triple(s(R.string.eje_vol), s(R.string.vision_eje_vol_af), s(R.string.vision_eje_vol_foto)),
                "MAE" to Triple(s(R.string.eje_mae), s(R.string.vision_eje_mae_af), s(R.string.vision_eje_mae_foto)),
                "VOZ" to Triple(s(R.string.eje_voz), s(R.string.vision_eje_voz_af), s(R.string.vision_eje_voz_foto)),
                "VAL" to Triple(s(R.string.eje_val), s(R.string.vision_eje_val_af), s(R.string.vision_eje_val_foto)),
                "EVO" to Triple(s(R.string.eje_evo), s(R.string.vision_eje_evo_af), s(R.string.vision_eje_evo_foto)),
                "TRA" to Triple(s(R.string.eje_tra), s(R.string.vision_eje_tra_af), s(R.string.vision_eje_tra_foto)),
            ),
            campamentos = mapOf(
                "CON" to Triple(s(R.string.brujula_con), s(R.string.brujula_con_af), s(R.string.brujula_con_foto)),
                "CAM" to Triple(s(R.string.brujula_cam), s(R.string.brujula_cam_af), s(R.string.brujula_cam_foto)),
            ),
            cumbreVacia = s(R.string.brujula_cumbre_vacia),
        )
    }

    /** Pone la foto en la casilla: crea una publicación de visión privada y la enlaza. */
    suspend fun ponerFoto(casilla: VisionCasillaEntity, foto: Uri) {
        val p = social.publicar(
            tipo = TipoPost.VISION, texto = casilla.afirmacion, foto = foto, eje = casilla.eje,
            visibilidad = Visibilidad.PRIVADA, metaTitulo = casilla.titulo,
        )
        db.visionDao().guardar(casilla.copy(publicacionId = p.id))
    }

    suspend fun guardar(c: VisionCasillaEntity) { db.visionDao().guardar(c) }

    suspend fun nueva(titulo: String, afirmacion: String, sugerencia: String) {
        val orden = (actualesDelAnio().maxOfOrNull { it.orden } ?: -1) + 1
        db.visionDao().guardar(
            VisionCasillaEntity(orden = orden, titulo = titulo, afirmacion = afirmacion, sugerencia = sugerencia,
                busqueda = VisionBoard.palabras(afirmacion), origen = Mandala.origenEnAnio(_anio.value, "manual"))
        )
    }

    suspend fun borrar(c: VisionCasillaEntity) = db.visionDao().borrar(c.id)
}
