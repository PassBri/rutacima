package com.rutaalacima.app.domain.model

/**
 * Resumen técnico de un error para el reporte anónimo: sin mensajes (podrían contener texto de la
 * persona), solo los tipos de excepción y las líneas de código. La firma agrupa los errores iguales.
 */
object Diagnostico {
    const val PAQUETE = "com.rutaalacima"
    const val MAX_RASTRO = 6000
    const val MAX_FIRMA = 300

    private fun raiz(t: Throwable): Throwable {
        var r = t
        val vistos = HashSet<Throwable>()
        while (r.cause != null && r.cause !== r && vistos.add(r)) r = r.cause!!
        return r
    }

    private fun linea(e: StackTraceElement) = "${e.className.substringAfterLast('.')}.${e.methodName}(${e.fileName ?: "?"}:${e.lineNumber})"

    /** "NullPointerException en HoyScreen.pintar(HoyScreen.kt:42)": la primera línea de la app donde falló. */
    fun firma(t: Throwable): String {
        val r = raiz(t)
        val pila = r.stackTrace.ifEmpty { t.stackTrace }
        val propia = pila.firstOrNull { it.className.startsWith(PAQUETE) } ?: pila.firstOrNull()
        return (r.javaClass.simpleName.ifBlank { r.javaClass.name } + (propia?.let { " en " + linea(it) } ?: "")).take(MAX_FIRMA)
    }

    /** Rastro sin mensajes: tipo de cada excepción de la cadena y sus primeras líneas. */
    fun rastro(t: Throwable, lineasPorCausa: Int = 25): String {
        val sb = StringBuilder()
        var actual: Throwable? = t
        val vistos = HashSet<Throwable>()
        var primera = true
        while (actual != null && vistos.add(actual)) {
            sb.append(if (primera) "" else "Causado por: ").append(actual.javaClass.name).append('\n')
            actual.stackTrace.take(lineasPorCausa).forEach { sb.append("  at ").append(it.className).append('.').append(it.methodName)
                .append('(').append(it.fileName ?: "?").append(':').append(it.lineNumber).append(")\n") }
            primera = false
            actual = actual.cause
        }
        return sb.toString().take(MAX_RASTRO)
    }
}
