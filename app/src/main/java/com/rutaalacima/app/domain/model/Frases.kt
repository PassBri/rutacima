package com.rutaalacima.app.domain.model

import java.time.LocalDate

/** Frase del día: una por cada día del año (365). El 31 de diciembre de un año bisiesto repite la última. */
object FrasesDelDia {
    const val TOTAL = 365
    fun indice(fecha: LocalDate): Int = (fecha.dayOfYear - 1).coerceIn(0, TOTAL - 1)
}

/** Frase con el libro de Ruta a la Cima del que viene. */
@kotlinx.serialization.Serializable
data class FraseLibro(val t: String, val libro: String)
