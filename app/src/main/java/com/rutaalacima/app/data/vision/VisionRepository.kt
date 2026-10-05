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
import com.rutaalacima.app.domain.model.VisionBoard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    val casillas: Flow<List<VisionCasillaEntity>> = db.visionDao().observe()

    /** Resultado de armar el tablero: true si lo propuso la IA, false si se usaron las reglas sin conexión. */
    suspend fun armar(): Boolean {
        val ia = coach.consultar(VisionBoard.instruccionIa())?.let(VisionBoard::desdeIa)
        val propuestas = ia ?: propuestaLocal()
        val actuales = db.visionDao().todas()
        // Se conservan las casillas que ya tienen foto; las demás se reemplazan.
        actuales.filter { it.publicacionId == null }.forEach { db.visionDao().borrar(it.id) }
        val conFoto = actuales.filter { it.publicacionId != null }
        val yaCubiertas = conFoto.map { it.origen }.toSet()
        var orden = (conFoto.maxOfOrNull { it.orden } ?: -1) + 1
        propuestas.filter { it.origen == "ia" || it.origen !in yaCubiertas }
            .take((VisionBoard.MAXIMO - conFoto.size).coerceAtLeast(0))
            .forEach { p ->
                db.visionDao().guardar(
                    VisionCasillaEntity(
                        orden = orden++, titulo = p.titulo, afirmacion = p.afirmacion, eje = p.eje,
                        sugerencia = p.sugerencia, busqueda = p.busqueda, origen = p.origen,
                    )
                )
            }
        return ia != null
    }

    private suspend fun propuestaLocal(): List<VisionBoard.Propuesta> {
        val perfil = db.perfilDao().get()
        val inicio = perfil?.anioInicioPlan ?: LocalDate.now().year
        val propositos = db.planificadorDao().observePropositos().first()
            .map { VisionBoard.Proposito(it.id, it.titulo, it.eje, inicio + it.horizonte - 1) }
        val anio = LocalDate.now().year
        val metas = db.planificadorDao().observeMetas(anio).first().map { VisionBoard.Meta(it.id, it.titulo, it.eje, it.anio) }
        val debil = db.evaluacionDao().observeUltima().first()?.let { e ->
            VisionBoard.CODIGOS.zip(e.puntajes()).minByOrNull { it.second }?.first
        }
        return VisionBoard.proponer(perfil?.cumbreFrase.orEmpty(), propositos, metas, debil, textos())
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
        val orden = (db.visionDao().todas().maxOfOrNull { it.orden } ?: -1) + 1
        db.visionDao().guardar(
            VisionCasillaEntity(orden = orden, titulo = titulo, afirmacion = afirmacion, sugerencia = sugerencia,
                busqueda = VisionBoard.palabras(afirmacion), origen = "manual")
        )
    }

    suspend fun borrar(c: VisionCasillaEntity) = db.visionDao().borrar(c.id)
}
