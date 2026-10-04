package com.rutaalacima.app.data.content

import com.rutaalacima.app.domain.model.Eje
import java.text.Normalizer

/*
 * Cálculo de resultados de las evaluaciones 1-10 de los workbooks (sin dependencias de Android,
 * para poder probarlo con el contenido real).
 */

/** Resultado de un grupo de escalas (un eje, una sección del instrumento, un área de vida…). */
data class GrupoResultado(val titulo: String?, val suma: Int, val respondidas: Int, val total: Int, val eje: Eje?) {
    val pct: Float get() = if (respondidas == 0) 0f else suma / (respondidas * 10f)
    val promedio: Float get() = if (respondidas == 0) 0f else suma.toFloat() / respondidas
}

data class ResultadoSeccion(val grupos: List<GrupoResultado>) {
    val suma: Int get() = grupos.sumOf { it.suma }
    val respondidas: Int get() = grupos.sumOf { it.respondidas }
    val total: Int get() = grupos.sumOf { it.total }
    val pct: Float get() = if (respondidas == 0) 0f else suma / (respondidas * 10f)

    /** Promedio por eje si las escalas cubren los 6 ejes (para guardarlo en Mis Ejes). */
    val ejes: List<Int>? get() {
        val porEje = grupos.filter { it.eje != null && it.respondidas > 0 }.groupBy { it.eje!! }
        if (porEje.size < 6) return null
        return Eje.entries.map { e -> porEje[e]!!.let { g -> Math.round(g.sumOf { it.suma }.toFloat() / g.sumOf { it.respondidas }) } }
    }
}

private fun normal(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").uppercase().trim()

/** Detecta el eje al que se refiere un texto ("1. VOLUNTAD · …", "VOL (Voluntad)", "Autoevaluación de tu VOZ"). */
fun ejeDe(texto: String): Eje? {
    val t = normal(texto).trimStart { !it.isLetter() }
    val nombres = mapOf(
        "VOLUNTAD" to Eje.VOLUNTAD, "MAESTRIA" to Eje.MAESTRIA, "VOZ" to Eje.VOZ, "VALOR" to Eje.VALOR,
        "EVOLUCION" to Eje.EVOLUCION, "TRASCEND" to Eje.TRASCENDENCIA,
    )
    nombres.forEach { (n, e) -> if (t.startsWith(n) || t.startsWith(e.codigo + " ")) return e }
    // "Autoevaluación de tu VOZ" → la última palabra
    val ultima = t.split(Regex("\\s+")).lastOrNull().orEmpty()
    return nombres.entries.firstOrNull { ultima.startsWith(it.key) }?.value
}

/** Calcula los resultados de las escalas 1-10 de una sección, agrupadas por su subtítulo. */
fun calcularResultados(seccion: Section, respuestas: Map<String, String>): ResultadoSeccion? {
    val planos = mutableListOf<Block>()
    fun aplanar(b: Block) { if (b is CalloutBlock) b.blocks.forEach(::aplanar) else planos += b }
    seccion.blocks.forEach(::aplanar)
    if (planos.count { it is ScaleBlock } < 2) return null

    val grupos = mutableListOf<GrupoResultado>()
    var titulo: String? = null
    var escalas = mutableListOf<ScaleBlock>()
    fun cerrar() {
        if (escalas.isEmpty()) return
        val valores = escalas.mapNotNull { respuestas[it.id]?.toIntOrNull() }
        val ejeGrupo = titulo?.let(::ejeDe)
        if (ejeGrupo == null && escalas.map { ejeDe(it.label) }.distinct().size > 1) {
            // Cada escala es un eje distinto (p. ej. "Identifica tus 6 ejes"): un grupo por escala.
            escalas.forEach { s ->
                val v = respuestas[s.id]?.toIntOrNull()
                grupos += GrupoResultado(s.label.take(40), v ?: 0, if (v != null) 1 else 0, 1, ejeDe(s.label))
            }
        } else {
            grupos += GrupoResultado(titulo, valores.sum(), valores.size, escalas.size, ejeGrupo ?: escalas.firstNotNullOfOrNull { ejeDe(it.label) }?.takeIf { escalas.size == 1 })
        }
        escalas = mutableListOf()
    }
    planos.forEach { b ->
        when {
            b is HeadingBlock -> { cerrar(); titulo = b.text }
            b is ParagraphBlock && b.text.length < 70 -> { cerrar(); titulo = b.text }
            b is ScaleBlock && !normal(b.label).startsWith("TOTAL") -> escalas += b
        }
    }
    cerrar()
    if (grupos.isEmpty()) return null
    return ResultadoSeccion(grupos)
}

