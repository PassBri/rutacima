package com.rutaalacima.app.domain.model

/**
 * Herramientas nativas del "Kit de Emergencia".
 */

/** Hábito del checklist diario por eje (Herramienta 2). */
data class HabitoDiario(val id: String, val eje: Eje, val texto: String)

object ChecklistDiario {
    val habitos: List<HabitoDiario> = listOf(
        HabitoDiario("VOL1", Eje.VOLUNTAD, "Revisé mi visión/propósito (1 min)"),
        HabitoDiario("VOL2", Eje.VOLUNTAD, "Hice algo hoy que me acerca a mi cumbre"),
        HabitoDiario("VOL3", Eje.VOLUNTAD, "Dije NO a algo que me aleja de mi cumbre"),
        HabitoDiario("MAE1", Eje.MAESTRIA, "Practiqué/perfeccioné una habilidad clave (mín. 30 min)"),
        HabitoDiario("MAE2", Eje.MAESTRIA, "Aprendí algo nuevo relacionado con mi cumbre"),
        HabitoDiario("MAE3", Eje.MAESTRIA, "Salí de mi zona de confort hoy"),
        HabitoDiario("VOZ1", Eje.VOZ, "Expresé mi opinión auténtica en al menos una situación"),
        HabitoDiario("VOZ2", Eje.VOZ, "Compartí mi trabajo/mensaje de alguna forma"),
        HabitoDiario("VOZ3", Eje.VOZ, "No me callé algo importante por miedo"),
        HabitoDiario("VAL1", Eje.VALOR, "Trabajé en algo que genera valor económico"),
        HabitoDiario("VAL2", Eje.VALOR, "Cobré/pedí lo que merezco sin culpa"),
        HabitoDiario("VAL3", Eje.VALOR, "Invertí en mi crecimiento (tiempo o dinero)"),
        HabitoDiario("EVO1", Eje.EVOLUCION, "Reflexioné sobre algo que aprendí hoy (journaling 5 min)"),
        HabitoDiario("EVO2", Eje.EVOLUCION, "Pedí feedback o perspectiva externa"),
        HabitoDiario("EVO3", Eje.EVOLUCION, "Ajusté un hábito o estrategia que no funciona"),
        HabitoDiario("TRA1", Eje.TRASCENDENCIA, "Ayudé a alguien sin esperar nada a cambio"),
        HabitoDiario("TRA2", Eje.TRASCENDENCIA, "Conecté con algo mayor que yo (naturaleza, arte, espiritualidad)"),
        HabitoDiario("TRA3", Eje.TRASCENDENCIA, "Trabajé en mi legado o contribución al mundo"),
    )

    const val TOTAL = 18

    /** "Con 10-12 checks estás en buen camino. Menos de 8, revisa qué está pasando." */
    fun lectura(checks: Int): String = when {
        checks >= 10 -> "Vas en buen camino."
        checks >= 8 -> "Avanzando. Busca llegar a 10-12."
        checks > 0 -> "Menos de 8: revisa qué está pasando."
        else -> "Marca los hábitos que cumpliste hoy."
    }
}

/** Tarjetas de recordatorio (Herramienta 3). */
object Tarjetas {
    val frases: Map<Eje, String> = mapOf(
        Eje.VOLUNTAD to "¿Esto me acerca o me aleja de mi cumbre?",
        Eje.MAESTRIA to "No se trata de talento. Se trata de práctica consistente.",
        Eje.VOZ to "Tu voz auténtica es más valiosa que tu voz aceptable.",
        Eje.VALOR to "Cobro por el valor que genero, no por las horas que trabajo.",
        Eje.EVOLUCION to "Si hoy soy igual que hace un mes, estoy retrocediendo.",
        Eje.TRASCENDENCIA to "Mi cumbre solo importa si ayuda a otros a subir.",
    )
}

/** Matriz de decisiones / filtro de cumbre (Herramienta 4). */
object MatrizDecisiones {
    enum class Respuesta { SI, NO, NEUTRO }

    val preguntas: List<String> = listOf(
        "¿Me acerca a mi cumbre?",
        "¿Fortalece mis ejes más débiles?",
        "¿Está alineada con mis valores?",
        "¿Puedo sostenerla a largo plazo?",
        "¿Me genera energía o me drena?",
    )

    /** Un punto por cada SÍ (o ENERGÍA en la última pregunta). */
    fun puntaje(respuestas: List<Respuesta?>): Int = respuestas.count { it == Respuesta.SI }

    fun recomendacion(a: Int, b: Int, nombreA: String, nombreB: String): String = when {
        a > b -> "\"$nombreA\" suma más puntos ($a vs $b). Generalmente es la mejor opción."
        b > a -> "\"$nombreB\" suma más puntos ($b vs $a). Generalmente es la mejor opción."
        else -> "Empate ($a-$b). Vuelve a tu cumbre en una frase y decide desde ahí."
    }
}

/** Protocolos de "Cuando te pierdes en la niebla" (índice de acceso rápido). */
data class Protocolo(val numero: Int, val titulo: String, val cuando: String)

object Niebla {
    val protocolos = listOf(
        Protocolo(1, "Perdí la claridad", "No sé hacia dónde voy"),
        Protocolo(2, "El plan no funciona", "No veo resultados"),
        Protocolo(3, "Perdí la motivación", "Ya no siento el fuego inicial"),
        Protocolo(4, "Quiero rendirme", "Esto es demasiado difícil"),
        Protocolo(5, "Enfrenté un fracaso", "Caí y no sé cómo levantarme"),
        Protocolo(6, "Dudo de mi cumbre", "¿Será esta la montaña correcta?"),
        Protocolo(7, "Me autosaboteo", "Sigo destruyendo mi propio progreso"),
        Protocolo(8, "¿Persevero o cambio?", "No sé si seguir o pivotar"),
    )
}
