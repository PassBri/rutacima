package com.rutaalacima.app.data.social

import android.content.Context
import com.rutaalacima.app.R
import java.time.LocalDate

/**
 * Comunidad de ejemplo para el modo demo (sin servidor configurado o sin sesión):
 * muestra cómo se ve SuRoute con gente compartiendo su ascenso. Los textos están en
 * strings.xml para que salgan en el idioma de la app.
 */
object DemoComunidad {
    private data class Demo(val id: String, val nombre: String, val usuario: String, val tipo: TipoPost, val texto: Int,
                            val eje: String, val meta: Int?, val impulsos: Int, val horas: Long)

    private val lista = listOf(
        Demo("demo-1", "Valentina R.", "vale.cumbre", TipoPost.LOGRO, R.string.demo_post_1, "MAE", R.string.demo_meta_1, 128, 3),
        Demo("demo-2", "Andrés M.", "andres.sube", TipoPost.VISION, R.string.demo_post_2, "TRA", null, 96, 7),
        Demo("demo-3", "Lucía P.", "luciap", TipoPost.EVIDENCIA, R.string.demo_post_3, "VOL", R.string.demo_meta_3, 74, 12),
        Demo("demo-4", "Mateo G.", "mateo.valor", TipoPost.META, R.string.demo_post_4, "VAL", R.string.demo_meta_4, 51, 20),
        Demo("demo-5", "Sofía L.", "sofi.voz", TipoPost.REFLEXION, R.string.demo_post_5, "VOZ", null, 63, 26),
        Demo("demo-6", "Camilo T.", "camilo.evo", TipoPost.LOGRO, R.string.demo_post_6, "EVO", R.string.demo_meta_6, 142, 40),
    )

    fun posts(context: Context, impulsados: Set<String>): List<Post> {
        val ahora = System.currentTimeMillis()
        return lista.map { d ->
            val yo = d.id in impulsados
            Post(
                id = d.id, autorId = d.usuario, autorNombre = d.nombre, autorUsuario = d.usuario, tipo = d.tipo,
                texto = context.getString(d.texto), eje = d.eje, anio = LocalDate.now().year,
                metaTitulo = d.meta?.let { context.getString(it) }.orEmpty(),
                impulsos = d.impulsos + if (yo) 1 else 0, comentarios = 2, yoImpulse = yo,
                creadoEn = ahora - d.horas * 3_600_000, demo = true,
                // Comunidad viva de ejemplo: el camino detrás, un compromiso abierto, uno cumplido y una carta sellada
                camino = when (d.id) { "demo-1" -> Camino(30, 14); "demo-3" -> Camino(21, 9); "demo-4" -> Camino(5, 3); "demo-6" -> Camino(64, 22); else -> null },
                compromiso = when (d.id) { "demo-4" -> Compromiso(LocalDate.now().plusDays(45).toString()); "demo-6" -> Compromiso(LocalDate.now().minusDays(3).toString(), Compromiso.LOGRADO); else -> null },
                sello = if (d.id == "demo-5") LocalDate.now().plusDays(240).toString() else null,
            )
        }
    }

    fun comentarios(context: Context, postId: String): List<Comentario> =
        if (postId.startsWith("demo-")) listOf(
            Comentario("$postId-c1", "Diana", context.getString(R.string.demo_comentario_1), System.currentTimeMillis() - 3_600_000),
            Comentario("$postId-c2", "Julián", context.getString(R.string.demo_comentario_2), System.currentTimeMillis() - 1_800_000),
            Comentario("$postId-c3", "Camilo T.", context.getString(R.string.demo_comentario_cuerda), System.currentTimeMillis() - 900_000, cuerda = "CONSEJO"),
        ) else emptyList()
}
