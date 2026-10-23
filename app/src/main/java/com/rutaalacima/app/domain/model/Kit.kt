package com.rutaalacima.app.domain.model

/**
 * Herramientas nativas del "Kit de Emergencia".
 */

/**
 * Hábito del checklist diario por eje (Herramienta 2). [texto] vacío = el texto de fábrica, traducido
 * (Textos.habito); con texto, es el que la persona escribió.
 */
data class HabitoDiario(val id: String, val eje: Eje, val texto: String = "")

object ChecklistDiario {
    /** Los 18 de fábrica (3 por eje). La persona puede renombrarlos, agregar y quitar (mínimo 1 por eje). */
    val base: List<HabitoDiario> = listOf(
        HabitoDiario("VOL1", Eje.VOLUNTAD), HabitoDiario("VOL2", Eje.VOLUNTAD), HabitoDiario("VOL3", Eje.VOLUNTAD),
        HabitoDiario("MAE1", Eje.MAESTRIA), HabitoDiario("MAE2", Eje.MAESTRIA), HabitoDiario("MAE3", Eje.MAESTRIA),
        HabitoDiario("VOZ1", Eje.VOZ), HabitoDiario("VOZ2", Eje.VOZ), HabitoDiario("VOZ3", Eje.VOZ),
        HabitoDiario("VAL1", Eje.VALOR), HabitoDiario("VAL2", Eje.VALOR), HabitoDiario("VAL3", Eje.VALOR),
        HabitoDiario("EVO1", Eje.EVOLUCION), HabitoDiario("EVO2", Eje.EVOLUCION), HabitoDiario("EVO3", Eje.EVOLUCION),
        HabitoDiario("TRA1", Eje.TRASCENDENCIA), HabitoDiario("TRA2", Eje.TRASCENDENCIA), HabitoDiario("TRA3", Eje.TRASCENDENCIA),
    )

    /** Clave de la lista propia: se guarda como respuesta del workbook "habitos" y se sincroniza con la web. */
    const val WORKBOOK = "habitos"
    const val CLAVE = "habitos#lista"
    const val MAX_POR_EJE = 6

    /** Lista vigente. Es estado de Compose: las pantallas se actualizan solas cuando cambia. */
    var habitos: List<HabitoDiario> by androidx.compose.runtime.mutableStateOf(base)
        private set

    val TOTAL: Int get() = habitos.size

    /** Aplica la lista guardada (JSON: [{"id","eje","t"}]); si no hay o no sirve, quedan los de fábrica. */
    fun aplicar(json: String?) {
        habitos = desdeJson(json) ?: base
    }

    fun desdeJson(json: String?): List<HabitoDiario>? = runCatching {
        if (json.isNullOrBlank()) return null
        val arr = kotlinx.serialization.json.Json.parseToJsonElement(json) as kotlinx.serialization.json.JsonArray
        val lista = arr.mapNotNull { e ->
            val o = e as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            val id = (o["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@mapNotNull null
            val cod = (o["eje"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            val eje = Eje.entries.firstOrNull { it.codigo == cod } ?: return@mapNotNull null
            HabitoDiario(id, eje, (o["t"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty().take(120))
        }.distinctBy { it.id }
        // Cada eje conserva al menos un hábito: si alguno quedó vacío, vuelven los de fábrica de ese eje
        val completa = Eje.entries.flatMap { e -> lista.filter { it.eje == e }.ifEmpty { base.filter { it.eje == e } } }
        completa.takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun aJson(lista: List<HabitoDiario>): String = kotlinx.serialization.json.buildJsonArray {
        lista.forEach { h -> add(kotlinx.serialization.json.buildJsonObject {
            put("id", kotlinx.serialization.json.JsonPrimitive(h.id)); put("eje", kotlinx.serialization.json.JsonPrimitive(h.eje.codigo))
            put("t", kotlinx.serialization.json.JsonPrimitive(h.texto))
        }) }
    }.toString()

    /** Los cambios de la persona, ya validados (máximo [MAX_POR_EJE] y mínimo 1 por eje). */
    fun renombrar(lista: List<HabitoDiario>, id: String, texto: String) = lista.map { if (it.id == id) it.copy(texto = texto.take(120)) else it }
    fun agregar(lista: List<HabitoDiario>, eje: Eje, texto: String): List<HabitoDiario> {
        if (lista.count { it.eje == eje } >= MAX_POR_EJE || texto.isBlank()) return lista
        var n = lista.size
        var id: String
        do { id = eje.codigo + "-" + java.lang.Long.toString(System.currentTimeMillis() % 2176782336L, 36) + n++ } while (lista.any { it.id == id })
        val i = lista.indexOfLast { it.eje == eje }
        return lista.toMutableList().apply { add(if (i < 0) size else i + 1, HabitoDiario(id, eje, texto.trim().take(120))) }
    }
    fun quitar(lista: List<HabitoDiario>, id: String): List<HabitoDiario> {
        val h = lista.firstOrNull { it.id == id } ?: return lista
        return if (lista.count { it.eje == h.eje } <= 1) lista else lista.filterNot { it.id == id }
    }

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
