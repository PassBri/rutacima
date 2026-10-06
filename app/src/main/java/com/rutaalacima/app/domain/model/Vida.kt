package com.rutaalacima.app.domain.model

/**
 * Calendario de vida: cada año de la vida es una fila de 12 meses, desde el nacimiento
 * hasta la esperanza de vida. Sirve para ver lo vivido, lo que queda y registrar cada año.
 */
object Vida {
    /** Horizonte de un propósito a largo plazo: la persona elige cualquier número de años en este rango. */
    const val HORIZONTE_MIN = 2
    const val HORIZONTE_MAX = 50
    const val HORIZONTE_DEFECTO = 5
    /** Atajos que se ofrecen junto al selector (no limitan la elección). */
    val HORIZONTES = listOf(3, 5, 10, 15, 20, 30)

    fun horizonte(anios: Int): Int = anios.coerceIn(HORIZONTE_MIN, HORIZONTE_MAX)

    const val ESPERANZA_MUNDIAL = 73

    /** "Camino hacia los 100 años": meta de vida por defecto y máxima. */
    const val META_DEFECTO = 100
    const val META_MAXIMA = 120
    val METAS = listOf(80, 90, 100, 110, 120)

    /**
     * Esperanza de vida al nacer aproximada por país (años, estimaciones recientes de la ONU/OMS,
     * redondeadas). Es solo un punto de partida: la persona la puede ajustar.
     */
    private val PAISES = mapOf(
        "CO" to 77, "MX" to 75, "AR" to 77, "CL" to 81, "PE" to 77, "EC" to 78, "VE" to 72, "BO" to 68,
        "PY" to 74, "UY" to 78, "CR" to 80, "PA" to 79, "GT" to 72, "HN" to 71, "SV" to 72, "NI" to 75,
        "DO" to 74, "CU" to 78, "PR" to 80, "ES" to 84, "PT" to 82, "BR" to 76, "US" to 78, "CA" to 82,
        "GB" to 81, "FR" to 83, "DE" to 81, "IT" to 84, "CH" to 84, "NL" to 82, "BE" to 82, "AT" to 82,
        "JP" to 85, "KR" to 84, "CN" to 78, "TW" to 81, "HK" to 85, "SG" to 83, "IN" to 72, "RU" to 72,
        "UA" to 72, "SA" to 78, "AE" to 80, "EG" to 71, "MA" to 75, "DZ" to 77, "AU" to 84, "NZ" to 82,
    )

    fun esperanzaPais(pais: String?): Int = PAISES[pais?.uppercase()] ?: ESPERANZA_MUNDIAL

    /** Meses vividos completos desde el nacimiento hasta (anio, mes). */
    fun mesesVividos(anioNac: Int, mesNac: Int, anioHoy: Int, mesHoy: Int): Int =
        ((anioHoy - anioNac) * 12 + (mesHoy - mesNac)).coerceAtLeast(0)

    /** Estado de un mes en el calendario de vida. */
    enum class EstadoMes { ANTES_DE_NACER, VIVIDO, ACTUAL, POR_VIVIR }

    fun estado(anio: Int, mes: Int, anioNac: Int, mesNac: Int, anioHoy: Int, mesHoy: Int): EstadoMes {
        val m = anio * 12 + mes
        return when {
            m < anioNac * 12 + mesNac -> EstadoMes.ANTES_DE_NACER
            m < anioHoy * 12 + mesHoy -> EstadoMes.VIVIDO
            m == anioHoy * 12 + mesHoy -> EstadoMes.ACTUAL
            else -> EstadoMes.POR_VIVIR
        }
    }

    /** Años calendario de la vida: del año de nacimiento al año en que se cumple la esperanza. */
    fun anios(anioNac: Int, esperanza: Int): List<Int> = (anioNac..anioNac + esperanza).toList()
}

/** Años que cubre el plan: tantos como el horizonte más largo de los propósitos (mínimo 5). */
fun aniosDelPlan(anioInicio: Int, horizonte: Int): List<Int> =
    (0 until horizonte.coerceIn(5, Vida.HORIZONTE_MAX)).map { anioInicio + it }

/**
 * Recordatorio del día del "camino hacia los N años": qué día de la vida es hoy, cuántos
 * días quedan hasta la meta y una frase que cambia cada día.
 */
data class RecordatorioVida(
    val diaDeVida: Long,
    val diasTotales: Long,
    val diasRestantes: Long,
    val edad: Int,
    val meta: Int,
    /** Índice 0..TOTAL_FRASES-1 de la frase del día (rota cada día). */
    val frase: Int,
) {
    val avance: Float get() = if (diasTotales <= 0) 0f else (diaDeVida.toFloat() / diasTotales).coerceIn(0f, 1f)

    companion object {
        const val TOTAL_FRASES = 14

        fun calcular(anioNac: Int, mesNac: Int, meta: Int, hoy: java.time.LocalDate): RecordatorioVida {
            val nacimiento = java.time.LocalDate.of(anioNac, mesNac.coerceIn(1, 12), 1)
            val fin = nacimiento.plusYears(meta.toLong())
            val dia = java.time.temporal.ChronoUnit.DAYS.between(nacimiento, hoy) + 1
            val total = java.time.temporal.ChronoUnit.DAYS.between(nacimiento, fin)
            val edad = java.time.Period.between(nacimiento, hoy).years
            return RecordatorioVida(
                diaDeVida = dia.coerceAtLeast(1), diasTotales = total,
                diasRestantes = (total - dia).coerceAtLeast(0), edad = edad, meta = meta,
                frase = (hoy.toEpochDay() % TOTAL_FRASES).toInt(),
            )
        }
    }
}
