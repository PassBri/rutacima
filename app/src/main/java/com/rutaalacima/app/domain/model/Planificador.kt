package com.rutaalacima.app.domain.model

/**
 * Escalas del Planificador 5 años ("Serie Ruta a la Cima", RAC-2026-CP01).
 */

/** Escala de prioridad A–D (sistema ABCD). */
enum class Prioridad(val clave: String, val nivel: String, val descripcion: String, val color: Long) {
    A("A", "Máxima prioridad", "Urgente e importante. Acción inmediata. Impacto directo en tus objetivos principales.", 0xFFB3261E),
    B("B", "Importante", "No urgente, pero de alto valor. Planifica tiempo específico para avanzar.", 0xFFC9A033),
    C("C", "Reactiva", "Urgente, pero de bajo impacto estratégico. Delega o atiende rápido sin profundidad.", 0xFF2E7D9A),
    D("D", "Observación", "Ni urgente ni importante. Evalúa si realmente vale la pena continuar.", 0xFF8D8D8D);

    companion object {
        fun from(value: String?): Prioridad = entries.firstOrNull { it.name == value } ?: B
    }
}

/** Matriz de decisión: acción estratégica para cada meta según su situación actual. */
enum class Decision(val nombre: String, val descripcion: String) {
    CONTINUAR("Continuar", "La meta avanza a buen ritmo. Mantener el plan actual sin cambios mayores."),
    ACELERAR("Acelerar", "La meta tiene alto potencial. Aumentar recursos, tiempo o esfuerzo."),
    PAUSAR("Pausar", "Existen obstáculos temporales. Detener brevemente y replantear."),
    ELIMINAR("Eliminar", "La meta ya no es relevante o viable. Liberar recursos para otras prioridades.");

    companion object {
        fun from(value: String?): Decision = entries.firstOrNull { it.name == value } ?: CONTINUAR
    }
}

/** Semáforo de avance: seis niveles para medir el progreso de cada meta. */
enum class EstadoMeta(val porcentaje: Int?, val nombre: String, val significado: String, val color: Long) {
    NO_INICIADA(0, "No iniciada", "La meta todavía no ha comenzado o solo está en intención.", 0xFF9E9E9E),
    INICIADA(25, "Iniciada", "Ya diste los primeros pasos, pero el avance aún es básico.", 0xFFE57373),
    EN_PAUSA(null, "En pausa", "La meta está detenida temporalmente por un obstáculo o decisión estratégica.", 0xFF78909C),
    EN_CURSO(50, "En curso", "La meta está activa y avanzando de forma sostenida.", 0xFFFFB300),
    AVANZADA(75, "Avanzada", "La mayor parte del trabajo está hecha; falta cierre o consolidación.", 0xFF9CCC65),
    CUMPLIDA(100, "Cumplida", "La meta fue lograda según lo planeado o con resultados equivalentes.", 0xFF2E7D32);

    companion object {
        fun from(value: String?): EstadoMeta = entries.firstOrNull { it.name == value } ?: NO_INICIADA

        /** Estado sugerido según el porcentaje de avance (sin tocar "En pausa"). */
        fun desdePorcentaje(p: Int): EstadoMeta = when {
            p >= 100 -> CUMPLIDA
            p >= 75 -> AVANZADA
            p >= 50 -> EN_CURSO
            p >= 25 -> INICIADA
            else -> NO_INICIADA
        }
    }
}

/** Horizonte del planificador: años 1..5 a partir del año de inicio. */
fun aniosDelPlan(anioInicio: Int): List<Int> = (0 until 5).map { anioInicio + it }
