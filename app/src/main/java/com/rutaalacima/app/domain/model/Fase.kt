package com.rutaalacima.app.domain.model

/**
 * Las 7 fases del Viaje Transformativo.
 * Fuente: "El Viaje Transformativo" y "Descubre tu Cumbre Personal" (paso 4).
 */
enum class Fase(
    val numero: Int,
    val nombre: String,
    val esencia: String,
    val lema: String,
    /** Descripción en primera persona para autoidentificarse ("¿En qué fase estás HOY?"). */
    val autodiagnostico: String,
    val ejeActivado: Eje,
) {
    ORIENTACION(
        1, "Orientación", "Claridad sobre tu dirección", "La brújula de todo tu camino",
        "Estoy confundido sobre mi dirección. No tengo claridad.", Eje.VOLUNTAD,
    ),
    PREPARACION(
        2, "Preparación", "Equiparte antes de emprender", "Equiparse antes de emprender la marcha",
        "Ya sé hacia dónde voy, pero necesito desarrollar habilidades.", Eje.MAESTRIA,
    ),
    TRAVESIA(
        3, "Travesía", "Aplicar lo aprendido, caminar", "El momento de caminar ha llegado",
        "Estoy aplicando lo aprendido, enfrentando obstáculos reales.", Eje.EVOLUCION,
    ),
    ASCENSO(
        4, "Ascenso", "Ver resultados, ajustar, crecer", "Tu voz comienza a resonar",
        "Veo resultados, pero necesito ajustar y fortalecer.", Eje.VOZ,
    ),
    CULMINACION(
        5, "Culminación", "Alcanzar una cumbre concreta", "El momento del logro",
        "He alcanzado una meta importante, estoy en la cima.", Eje.VALOR,
    ),
    CONTEMPLACION(
        6, "Contemplación", "Asimilar, valorar, integrar", "La pausa consciente",
        "Estoy asimilando, integrando, valorando lo logrado.", Eje.TRASCENDENCIA,
    ),
    DESCENSO(
        7, "Descenso", "Prepararse para nuevas cumbres", "Evolución aplicada",
        "Estoy preparándome para la siguiente montaña.", Eje.TRASCENDENCIA,
    );

    /** Portal que se cruza al salir de esta fase ("Portales y Transiciones"). */
    val siguiente: Fase get() = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): Fase? = entries.firstOrNull { it.name == name }
    }
}
