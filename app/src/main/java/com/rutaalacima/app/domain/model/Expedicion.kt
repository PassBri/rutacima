package com.rutaalacima.app.domain.model

import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

/**
 * La expedición de la comunidad: una sola montaña de 8.848.000 mm por año para todas las personas.
 *
 * Idea propia inspirada en el ajuste de dificultad de Bitcoin (sin cadena de bloques ni minería):
 *  - Cada semana es un "tramo". Al empezar un tramo se recalcula la dificultad con los aportes del
 *    tramo anterior, para que a ese ritmo la cumbre se alcance justo al final del año.
 *  - Si la comunidad crece a millones de personas, cada aporte vale fracciones de milímetro: la
 *    montaña nunca se vuelve fácil.
 *  - Como en Bitcoin, la dificultad no puede cambiar más de 4 veces de un tramo al siguiente, y
 *    nunca baja de un mínimo (una comunidad pequeña tampoco la sube en una semana).
 *  - Ningún tramo sube más de 4 veces lo esperado (4/52 de la montaña): aunque llegue una avalancha
 *    de gente, la cumbre no se alcanza antes de 13 semanas.
 */
object Expedicion {
    const val ALTURA_MM = 8_848_000L
    const val TRAMOS = 52
    const val PUNTOS_PUBLICACION = 10
    const val PUNTOS_IMPULSO = 1
    /**
     * Puntos por semana que se suponen como mínimo (unas 10.000 publicaciones), aunque la comunidad
     * sea pequeña: desde el primer día una publicación vale milímetros, no metros.
     */
    const val DIFICULTAD_MIN = 100_000L
    /** Cuánto puede cambiar la dificultad de un tramo al siguiente (igual que Bitcoin). */
    const val AJUSTE_MAX = 4L

    data class Estado(
        /** Milímetros subidos por toda la comunidad en el año. */
        val mm: Double,
        /** Lo que vale un punto en el tramo actual. */
        val mmPorPunto: Double,
        /** Puntos por semana que la montaña espera en este tramo. */
        val dificultad: Long,
        /** Tramo actual (0..51). */
        val tramo: Int,
        val diasParaAjuste: Int,
        val puntosTramo: Long,
        /** Mi aporte en el tramo actual, en milímetros. */
        val miAporteMm: Double,
    ) {
        val cumbre: Boolean get() = mm >= ALTURA_MM
        val fraccion: Double get() = (mm / ALTURA_MM).coerceIn(0.0, 1.0)
        /** Lo que vale una publicación ahora mismo. */
        val mmPorPublicacion: Double get() = mmPorPunto * PUNTOS_PUBLICACION
        val mmPorImpulso: Double get() = mmPorPunto * PUNTOS_IMPULSO
    }

    fun puntos(publicaciones: Int, impulsos: Int): Long = publicaciones.toLong() * PUNTOS_PUBLICACION + impulsos.toLong() * PUNTOS_IMPULSO

    fun tramoDe(fecha: LocalDate): Int = min((fecha.dayOfYear - 1) / 7, TRAMOS - 1)

    fun mmPorPunto(dificultad: Long): Double = ALTURA_MM.toDouble() / (max(dificultad, 1L) * TRAMOS)

    /** Dificultad de cada tramo, calculada con los puntos del tramo anterior. */
    fun dificultades(puntos: List<Long>): List<Long> {
        val r = ArrayList<Long>(puntos.size)
        var d = DIFICULTAD_MIN
        for (i in puntos.indices) {
            if (i > 0) d = max(DIFICULTAD_MIN, puntos[i - 1].coerceIn(d / AJUSTE_MAX, d * AJUSTE_MAX))
            r += d
        }
        return r
    }

    /** [puntos]: puntos de cada tramo del año (el índice es el tramo); faltantes = 0. */
    fun calcular(puntos: List<Long>, misPuntosTramo: Long, hoy: LocalDate): Estado {
        val t = tramoDe(hoy)
        val p = List(t + 1) { puntos.getOrElse(it) { 0L } }
        val d = dificultades(p)
        val tope = ALTURA_MM.toDouble() * AJUSTE_MAX / TRAMOS
        val mm = p.indices.sumOf { min(p[it] * mmPorPunto(d[it]), tope) }.coerceAtMost(ALTURA_MM.toDouble())
        val fin = if (t == TRAMOS - 1) LocalDate.of(hoy.year + 1, 1, 1) else LocalDate.ofYearDay(hoy.year, (t + 1) * 7 + 1)
        val porPunto = mmPorPunto(d[t])
        return Estado(
            mm = mm, mmPorPunto = porPunto, dificultad = d[t], tramo = t,
            diasParaAjuste = (fin.toEpochDay() - hoy.toEpochDay()).toInt(),
            puntosTramo = p[t], miAporteMm = misPuntosTramo * porPunto,
        )
    }

    /** Agrupa aportes sueltos (fecha, puntos) por tramo del año [anio]. */
    fun porTramo(aportes: List<Pair<LocalDate, Long>>, anio: Int): List<Long> {
        val r = LongArray(TRAMOS)
        aportes.filter { it.first.year == anio }.forEach { (f, pts) -> r[tramoDe(f)] += pts }
        return r.toList()
    }
}
