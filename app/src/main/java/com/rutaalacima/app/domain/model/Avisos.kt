package com.rutaalacima.app.domain.model

/**
 * Qué avisar de los mensajes y del coach de vida (sin Android: se prueba con JUnit). La app
 * recuerda hasta dónde avisó de cada conversación para no repetir la misma notificación.
 */
object Avisos {
    data class Conv(
        val id: String, val nombre: String, val ultimoTexto: String, val ultimoEn: Long,
        val noLeidos: Int, val solicitud: Boolean, val ultimoEsMio: Boolean,
    )

    sealed interface Aviso { val clave: String }
    data class Mensajes(val conv: String, val nombre: String, val texto: String, val cuantos: Int) : Aviso { override val clave = "m:$conv" }
    data class Solicitud(val conv: String, val nombre: String, val texto: String) : Aviso { override val clave = "s:$conv" }
    data class PideCoach(val acomp: String, val nombre: String) : Aviso { override val clave = "c:$acomp" }

    /**
     * Avisos nuevos desde la última revisión. [avisados] guarda, por conversación, la fecha del
     * último mensaje del que ya se avisó; [conversacionAbierta] no avisa (la persona la está leyendo).
     */
    fun nuevos(
        convs: List<Conv>,
        avisados: Map<String, Long>,
        coachesPendientes: List<Pair<String, String>> = emptyList(),
        coachAvisados: Set<String> = emptySet(),
        conversacionAbierta: String? = null,
    ): List<Aviso> {
        val r = mutableListOf<Aviso>()
        for (c in convs) {
            if (c.id == conversacionAbierta || c.ultimoEsMio || c.ultimoEn <= (avisados[c.id] ?: 0L)) continue
            when {
                c.solicitud -> r += Solicitud(c.id, c.nombre, c.ultimoTexto)
                c.noLeidos > 0 -> r += Mensajes(c.id, c.nombre, c.ultimoTexto, c.noLeidos)
            }
        }
        coachesPendientes.filter { (id, _) -> id !in coachAvisados }.forEach { (id, nombre) -> r += PideCoach(id, nombre) }
        return r
    }

    /** Después de avisar: hasta dónde quedó avisada cada conversación. */
    fun marcar(convs: List<Conv>, avisados: Map<String, Long>): Map<String, Long> =
        avisados + convs.associate { it.id to maxOf(it.ultimoEn, avisados[it.id] ?: 0L) }
}
