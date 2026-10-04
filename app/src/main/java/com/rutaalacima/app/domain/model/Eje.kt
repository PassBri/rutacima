package com.rutaalacima.app.domain.model

/**
 * Los 6 ejes de la Cumbre Personal (modelo Ruta a la Cima).
 * Fuente: "Los 6 Ejes de tu Cumbre Personal" y "Descubre tu Cumbre Personal".
 */
enum class Eje(
    val nombre: String,
    val codigo: String,
    val lema: String,
    val definicion: String,
    val preguntaClave: String,
    val preguntaEvaluacion: String,
    /** Color ARGB del eje (se usa en gráficos y etiquetas). */
    val color: Long,
) {
    VOLUNTAD(
        nombre = "Voluntad",
        codigo = "VOL",
        lema = "La llama que enciende tu camino",
        definicion = "Lo que despierta tu impulso interior",
        preguntaClave = "¿Qué me mueve?",
        preguntaEvaluacion = "¿Tengo claridad absoluta sobre lo que me mueve profundamente?",
        color = 0xFFC0392B,
    ),
    MAESTRIA(
        nombre = "Maestría",
        codigo = "MAE",
        lema = "La fortaleza que construyes con el tiempo",
        definicion = "Lo que construyes con disciplina",
        preguntaClave = "¿En qué soy excelente?",
        preguntaEvaluacion = "¿He desarrollado habilidades sólidas y disciplina consistente?",
        color = 0xFFB8860B,
    ),
    VOZ(
        nombre = "Voz",
        codigo = "VOZ",
        lema = "Tu expresión auténtica en el mundo",
        definicion = "Lo que tu autenticidad representa",
        preguntaClave = "¿Qué mensaje tengo?",
        preguntaEvaluacion = "¿Expreso mi autenticidad y mi mensaje al mundo sin miedo?",
        color = 0xFF2E7D9A,
    ),
    VALOR(
        nombre = "Valor",
        codigo = "VAL",
        lema = "La capacidad de sostener tu vida con coherencia",
        definicion = "Lo que puedes monetizar",
        preguntaClave = "¿Cómo sostengo mi vida?",
        preguntaEvaluacion = "¿He transformado mi talento en valor económico sostenible?",
        color = 0xFF2E7D32,
    ),
    EVOLUCION(
        nombre = "Evolución",
        codigo = "EVO",
        lema = "Tu crecimiento constante",
        definicion = "Lo que te hace crecer",
        preguntaClave = "¿Cómo estoy mejorando?",
        preguntaEvaluacion = "¿Estoy creciendo y evolucionando constantemente?",
        color = 0xFF6A4C93,
    ),
    TRASCENDENCIA(
        nombre = "Trascendencia",
        codigo = "TRA",
        lema = "Tu conexión con tu legado",
        definicion = "Lo que te conecta con lo mayor",
        preguntaClave = "¿Qué legado dejo?",
        preguntaEvaluacion = "¿Mi vida tiene un sentido que trasciende mis necesidades individuales?",
        color = 0xFF8E5A2B,
    );

    companion object {
        fun fromCodigo(codigo: String?): Eje? = entries.firstOrNull { it.codigo == codigo || it.name == codigo }
    }
}

/** Interpretación del puntaje por eje (workbook "Los 6 Ejes"). */
fun interpretarEje(puntaje: Int): String = when {
    puntaje >= 8 -> "Fuerte y activo"
    puntaje >= 5 -> "En desarrollo"
    puntaje >= 1 -> "Necesita atención urgente"
    else -> "Sin evaluar"
}

/** Interpretación del puntaje total sobre 60 ("Descubre tu Cumbre Personal", paso 2). */
fun interpretarTotal(total: Int): String = when {
    total >= 48 -> "Estás cerca de tu cumbre. Enfócate en perfeccionar los ejes más débiles."
    total >= 30 -> "Estás en proceso de ascenso. Prioriza 2-3 ejes para trabajar intensamente."
    total >= 6 -> "Estás al inicio del viaje. Esto es PERFECTO. Tienes todo el camino por delante."
    else -> "Evalúa tus 6 ejes para ver tu punto de partida."
}
