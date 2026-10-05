package com.rutaalacima.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/**
 * Cómo acompaña la app el camino de cada persona (sin Android: se prueba con JUnit):
 * primeros pasos guiados, regreso sin culpa, revisiones semanal y mensual, y constancia
 * (racha y mapa del año de los días con hábitos).
 */
object PrimerosPasos {
    enum class Paso { CUMBRE, NACIMIENTO, EJES, PROPOSITO, META_MES, HABITOS, GUIA, VISION, COMUNIDAD }

    data class Estado(
        val cumbre: Boolean = false,
        val nacimiento: Boolean = false,
        val ejes: Boolean = false,
        val proposito: Boolean = false,
        val metaMes: Boolean = false,
        val habitos: Boolean = false,
        val guia: Boolean = false,
        val vision: Boolean = false,
        val comunidad: Boolean = false,
    )

    fun hecho(e: Estado, p: Paso) = when (p) {
        Paso.CUMBRE -> e.cumbre
        Paso.NACIMIENTO -> e.nacimiento
        Paso.EJES -> e.ejes
        Paso.PROPOSITO -> e.proposito
        Paso.META_MES -> e.metaMes
        Paso.HABITOS -> e.habitos
        Paso.GUIA -> e.guia
        Paso.VISION -> e.vision
        Paso.COMUNIDAD -> e.comunidad
    }

    /** El siguiente paso pendiente, en orden (de la cumbre a lo de hoy); null si ya hizo todos. */
    fun siguiente(e: Estado): Paso? = Paso.entries.firstOrNull { !hecho(e, it) }

    fun hechos(e: Estado): Int = Paso.entries.count { hecho(e, it) }
}

object Regreso {
    /** A partir de cuántos días sin abrir la app se le da la bienvenida de regreso. */
    const val DIAS = 5

    fun diasFuera(ultimaVez: LocalDate?, hoy: LocalDate): Int =
        if (ultimaVez == null) 0 else ChronoUnit.DAYS.between(ultimaVez, hoy).toInt().coerceAtLeast(0)

    fun debeRecibir(ultimaVez: LocalDate?, hoy: LocalDate) = diasFuera(ultimaVez, hoy) >= DIAS
}

object Revision {
    enum class Tipo { SEMANA, MES }

    /** Clave estable de la semana ISO ("2026-W40") o del mes ("2026-10"). */
    fun clave(tipo: Tipo, dia: LocalDate): String = when (tipo) {
        Tipo.SEMANA -> "%d-W%02d".format(dia.get(IsoFields.WEEK_BASED_YEAR), dia.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
        Tipo.MES -> YearMonth.from(dia).toString()
    }

    /** Primer y último día del período que contiene [dia]. */
    fun periodo(tipo: Tipo, dia: LocalDate): Pair<LocalDate, LocalDate> = when (tipo) {
        Tipo.SEMANA -> dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { it to it.plusDays(6) }
        Tipo.MES -> dia.withDayOfMonth(1) to dia.with(TemporalAdjusters.lastDayOfMonth())
    }

    /**
     * Qué revisión toca hoy, si alguna: la del mes en sus 2 últimos días y los 2 primeros del
     * siguiente (tiene prioridad); la de la semana el domingo y el lunes. Devuelve el tipo y un día
     * dentro del período a revisar.
     */
    fun pendiente(hoy: LocalDate, hechas: Set<String>): Pair<Tipo, LocalDate>? {
        val finDeMes = hoy.lengthOfMonth() - hoy.dayOfMonth < 2
        val mes = when {
            finDeMes -> hoy
            hoy.dayOfMonth <= 2 -> hoy.minusMonths(1)
            else -> null
        }
        if (mes != null && clave(Tipo.MES, mes) !in hechas) return Tipo.MES to mes
        val semana = when (hoy.dayOfWeek) {
            DayOfWeek.SUNDAY -> hoy
            DayOfWeek.MONDAY -> hoy.minusDays(1)
            else -> null
        }
        if (semana != null && clave(Tipo.SEMANA, semana) !in hechas) return Tipo.SEMANA to semana
        return null
    }

    data class Resumen(
        val dias: Int,
        /** Días con al menos un hábito marcado. */
        val diasConHabitos: Int,
        val promedio: Float,
        val mejorDia: LocalDate?,
        val mejorDiaHabitos: Int,
        /** Hábito más constante del período (id) y cuántos días se cumplió. */
        val habitoFuerte: Pair<String, Int>?,
    )

    fun resumen(tipo: Tipo, dia: LocalDate, checks: Map<LocalDate, Set<String>>, hoy: LocalDate): Resumen {
        val (desde, hastaPeriodo) = periodo(tipo, dia)
        val hasta = minOf(hastaPeriodo, hoy)
        val dias = generateSequence(desde) { it.plusDays(1) }.takeWhile { !it.isAfter(hasta) }.toList()
        val cuentas = dias.map { it to (checks[it]?.size ?: 0) }
        val mejor = cuentas.maxByOrNull { it.second }?.takeIf { it.second > 0 }
        val fuerte = dias.flatMap { checks[it].orEmpty() }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.toPair()
        return Resumen(
            dias = dias.size,
            diasConHabitos = cuentas.count { it.second > 0 },
            promedio = if (dias.isEmpty()) 0f else cuentas.sumOf { it.second }.toFloat() / dias.size,
            mejorDia = mejor?.first, mejorDiaHabitos = mejor?.second ?: 0,
            habitoFuerte = fuerte,
        )
    }
}

object Constancia {
    /**
     * Racha: días seguidos con al menos un hábito, contando hasta hoy. Si hoy todavía no marcas
     * nada, la racha de ayer sigue viva (no se pierde a las 7 de la mañana).
     */
    fun racha(dias: Set<LocalDate>, hoy: LocalDate): Int {
        var d = if (hoy in dias) hoy else hoy.minusDays(1)
        var n = 0
        while (d in dias) { n++; d = d.minusDays(1) }
        return n
    }

    /** La racha más larga dentro de los días dados. */
    fun mejorRacha(dias: Set<LocalDate>): Int {
        var mejor = 0
        for (d in dias) {
            if (d.minusDays(1) in dias) continue   // solo empieza a contar en el primer día de cada racha
            var n = 0; var x = d
            while (x in dias) { n++; x = x.plusDays(1) }
            mejor = maxOf(mejor, n)
        }
        return mejor
    }

    /**
     * Mapa del año como un calendario de semanas (columnas) × días (filas, lunes arriba): cada
     * celda es el número de hábitos de ese día, o null si cae fuera del año o en el futuro.
     */
    fun mapaDelAnio(anio: Int, conteos: Map<LocalDate, Int>, hoy: LocalDate): List<List<Int?>> {
        val inicio = LocalDate.of(anio, 1, 1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val fin = LocalDate.of(anio, 12, 31)
        val semanas = mutableListOf<List<Int?>>()
        var lunes = inicio
        while (!lunes.isAfter(fin)) {
            semanas += (0..6).map { k ->
                val d = lunes.plusDays(k.toLong())
                if (d.year != anio || d.isAfter(hoy)) null else conteos[d] ?: 0
            }
            lunes = lunes.plusWeeks(1)
        }
        return semanas
    }

    /** Intensidad 0–4 para pintar una celda (0 = nada; 4 = 12 hábitos o más). */
    fun nivel(habitos: Int): Int = when {
        habitos <= 0 -> 0
        habitos < 4 -> 1
        habitos < 8 -> 2
        habitos < 12 -> 3
        else -> 4
    }
}
