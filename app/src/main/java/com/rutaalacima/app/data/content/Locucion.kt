package com.rutaalacima.app.data.content

/**
 * Convierte un capítulo de una guía en el texto que se lee en voz alta (audiolibro con la voz
 * del teléfono). Lee títulos, párrafos, viñetas, citas, recuadros, tablas y el enunciado de los
 * ejercicios; quita las rayas para llenar ("____") y parte lo largo en fragmentos que el motor de
 * voz acepta. Sin Android: se prueba con JUnit.
 */
object Locucion {
    /** Android admite unos 4000 caracteres por enunciado; dejamos margen. */
    const val MAX_FRAGMENTO = 1200

    private val rayas = Regex("_{2,}")
    private val espacios = Regex("[ \\t]+")
    private val vacioEntreSignos = Regex("\\s*([,.;:])\\s*(?=[,.;:)])")

    /** Limpia un texto para leerlo: sin rayas de llenar, sin espacios dobles ni signos sueltos. */
    fun limpiar(t: String): String = t
        .replace(rayas, " ")
        .replace("…/…", " ")
        .replace(Regex("\\s*/\\s*(?=\\s|$|\\))"), " ")
        .replace(Regex("\\(\\s*%?\\s*:?\\s*\\)"), " ")
        .replace(vacioEntreSignos, "$1")
        .replace(espacios, " ")
        .lines().joinToString(" ") { it.trim() }
        .trim()
        .trim(':', ' ')

    /** Fragmentos del capítulo, en orden, listos para leer. */
    fun fragmentos(s: Section): List<String> {
        val salida = mutableListOf<String>()
        fun agregar(t: String?) {
            val l = limpiar(t.orEmpty())
            if (l.length > 1 && l.any { it.isLetterOrDigit() }) salida += partir(l)
        }
        fun bloque(b: Block) {
            when (b) {
                is HeadingBlock -> agregar(b.text)
                is ParagraphBlock -> agregar(b.text)
                is BulletBlock -> agregar(b.text)
                is QuoteBlock -> agregar(b.text)
                is CalloutBlock -> b.blocks.forEach(::bloque)
                is TableBlock -> {
                    if (b.rows.isEmpty()) agregar(b.headers.joinToString(", "))
                    b.rows.forEach { fila ->
                        agregar(fila.mapIndexed { i, v ->
                            val h = b.headers.getOrNull(i).orEmpty().trim()
                            if (h.isNotEmpty() && v.isNotBlank() && i > 0) "$h: $v" else v
                        }.filter { it.isNotBlank() }.joinToString(". "))
                    }
                }
                is PromptBlock -> { agregar(b.title); agregar(b.label) }
                is ScaleBlock -> agregar(b.label)
                is CheckBlock -> agregar(b.label)
                is ChoiceBlock -> agregar(listOf(b.label, b.options.joinToString(", ")).filter { it.isNotBlank() }.joinToString(": "))
                is InputTableBlock -> agregar(b.rowLabels.joinToString(", "))
                is ImageBlock -> Unit
            }
        }
        agregar(s.title)
        s.blocks.forEach(::bloque)
        // Une frases muy cortas seguidas (viñetas de una palabra) para que la voz no haga pausas eternas
        val unidos = mutableListOf<String>()
        for (f in salida) {
            val ultimo = unidos.lastOrNull()
            if (ultimo != null && ultimo.length < 60 && f.length < 60 && ultimo.length + f.length < MAX_FRAGMENTO) {
                unidos[unidos.lastIndex] = ultimo + (if (ultimo.last() in ".!?:;") " " else ". ") + f
            } else unidos += f
        }
        return unidos
    }

    /** Parte un texto largo por oraciones (y, si hace falta, por palabras). */
    fun partir(t: String, max: Int = MAX_FRAGMENTO): List<String> {
        if (t.length <= max) return listOf(t)
        val partes = mutableListOf<String>()
        val actual = StringBuilder()
        for (oracion in t.split(Regex("(?<=[.!?;:])\\s+"))) {
            if (actual.isNotEmpty() && actual.length + oracion.length + 1 > max) { partes += actual.toString(); actual.clear() }
            if (oracion.length > max) {
                oracion.split(' ').forEach { p ->
                    if (actual.isNotEmpty() && actual.length + p.length + 1 > max) { partes += actual.toString(); actual.clear() }
                    if (actual.isNotEmpty()) actual.append(' ')
                    actual.append(p)
                }
            } else {
                if (actual.isNotEmpty()) actual.append(' ')
                actual.append(oracion)
            }
        }
        if (actual.isNotEmpty()) partes += actual.toString()
        return partes
    }

    /** Minutos aproximados de escucha a velocidad normal (unas 150 palabras por minuto). */
    fun minutos(fragmentos: List<String>): Int =
        (fragmentos.sumOf { f -> f.count { it == ' ' } + 1 } / 150.0).let { kotlin.math.ceil(it).toInt().coerceAtLeast(1) }
}
