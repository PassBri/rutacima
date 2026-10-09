package com.rutaalacima.app.domain.model

import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Método Cima 9×52: el modelo propio de Rutaalacima.
 *
 * Une tres ideas:
 *  1. La cuadrícula 9×9 (Mandala Chart): la cumbre al centro, 8 campamentos y 64 pasos ([Mandala]).
 *  2. Los 52 tramos del año ([Expedicion]): la montaña personal mide 8.848.000 mm y cada paso vale
 *     1/64 de ella (138.250 mm). Se sube durante el año, tramo a tramo.
 *  3. El ajuste de dificultad de Bitcoin, aplicado a una sola persona: un paso no se marca, se gana
 *     con **jornadas** (días distintos en que avanzaste en él) — una prueba de constancia en lugar
 *     de una prueba de trabajo. Cada 4 tramos (un ciclo, como las 2.016 bloques de Bitcoin) se
 *     recalcula cuántas jornadas pide un paso nuevo, comparando los pasos ganados en ese ciclo con
 *     el ritmo que lleva a la cumbre el 31 de diciembre. Si vas muy rápido, cada paso pide más jornadas; si te frenaste, pide menos para que
 *     vuelvas. Como en Bitcoin, nunca cambia más de 4 veces de un tramo al siguiente, y la dificultad
 *     queda fija para cada paso desde su primera jornada (no se mueve la meta a mitad de camino).
 */
object MetodoCima {
    const val TOTAL_PASOS = Mandala.CAMPAMENTOS * Mandala.PASOS
    const val MM_POR_PASO = Expedicion.ALTURA_MM / TOTAL_PASOS
    const val DIFICULTAD_INICIAL = 3
    const val DIFICULTAD_MIN = 1
    const val DIFICULTAD_MAX = 30
    const val AJUSTE_MAX = 4
    /** Tramos por ciclo de ajuste. */
    const val CICLO = 4

    fun claveJornadas(casillaId: Long, paso: Int) = Mandala.clave(casillaId, paso) + "#jornadas"
    fun claveRequeridas(casillaId: Long, paso: Int) = Mandala.clave(casillaId, paso) + "#req"

    /** Un paso de la mandala con sus jornadas. [cumplido] = fecha en que se ganó (null si no). */
    data class Paso(
        val escrito: Boolean,
        val jornadas: Set<LocalDate> = emptySet(),
        val requeridas: Int? = null,
        val cumplido: Boolean = false,
        val cumplidoEn: LocalDate? = null,
    )

    /** Lee un paso desde las respuestas guardadas. */
    fun leer(casillaId: Long?, paso: Int, r: Map<String, String>): Paso {
        if (casillaId == null) return Paso(escrito = false)
        val hecho = r[Mandala.claveHecho(casillaId, paso)].orEmpty()
        return Paso(
            escrito = !r[Mandala.clave(casillaId, paso)].isNullOrBlank(),
            jornadas = r[claveJornadas(casillaId, paso)].orEmpty().split(',').mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }.toSet(),
            requeridas = r[claveRequeridas(casillaId, paso)]?.toIntOrNull(),
            cumplido = hecho.isNotBlank(),
            cumplidoEn = runCatching { LocalDate.parse(hecho) }.getOrNull(),
        )
    }

    /** Lo que cambia al registrar la jornada de hoy en un paso. */
    data class Avance(val jornadas: Set<LocalDate>, val requeridas: Int, val cumplidoEn: LocalDate?, val nueva: Boolean)

    fun avanzar(p: Paso, hoy: LocalDate, dificultadActual: Int): Avance {
        val req = p.requeridas ?: dificultadActual
        if (p.cumplido) return Avance(p.jornadas, req, p.cumplidoEn ?: hoy, nueva = false)
        val nueva = hoy !in p.jornadas
        val j = p.jornadas + hoy
        return Avance(j, req, if (j.size >= req) hoy else null, nueva)
    }

    /** Fracción ganada de un paso (0..1). */
    fun avance(p: Paso, dificultadActual: Int): Float = when {
        p.cumplido -> 1f
        p.jornadas.isEmpty() -> 0f
        else -> min(0.99f, p.jornadas.size.toFloat() / (p.requeridas ?: dificultadActual))
    }

    /**
     * Dificultad (jornadas por paso) de cada tramo del año hasta [tramoActual].
     * [ganados]: pasos ganados en cada tramo; [pendientesAlInicio]: pasos que faltaban el 1 de enero.
     */
    fun dificultades(ganados: List<Int>, pendientesAlInicio: Int, tramoActual: Int): List<Int> {
        val r = ArrayList<Int>(tramoActual + 1)
        var d = DIFICULTAD_INICIAL
        var restantes = pendientesAlInicio
        var ganadosCiclo = 0
        var esperadoCiclo = 0.0
        for (n in 0..tramoActual) {
            if (n > 0) {
                val previo = ganados.getOrElse(n - 1) { 0 }
                ganadosCiclo += previo
                esperadoCiclo += restantes.toDouble() / (Expedicion.TRAMOS - (n - 1))
                restantes = max(0, restantes - previo)
                if (n % CICLO == 0) {
                    if (esperadoCiclo > 0) {
                        val propuesto = (d * ganadosCiclo / esperadoCiclo).roundToInt()
                        d = propuesto.coerceIn(max(1, d / AJUSTE_MAX), d * AJUSTE_MAX).coerceIn(DIFICULTAD_MIN, DIFICULTAD_MAX)
                    }
                    ganadosCiclo = 0; esperadoCiclo = 0.0
                }
            }
            r += d
        }
        return r
    }

    /** Primer día del próximo ajuste (inicio del siguiente ciclo de 4 tramos). */
    fun proximoAjuste(hoy: LocalDate): LocalDate {
        val siguiente = (Expedicion.tramoDe(hoy) / CICLO + 1) * CICLO
        return if (siguiente >= Expedicion.TRAMOS) LocalDate.of(hoy.year + 1, 1, 1) else LocalDate.ofYearDay(hoy.year, siguiente * 7 + 1)
    }

    data class Estado(
        val tramo: Int,
        /** Jornadas que pide un paso que empiezas en este tramo. */
        val dificultad: Int,
        val diasParaAjuste: Int,
        /** Pasos que el ritmo espera en este tramo (para llegar a la cumbre el 31 de diciembre). */
        val esperadoTramo: Double,
        val ganadosTramo: Int,
        val ganadosAnio: Int,
        val restantes: Int,
        /** Pasos ganados de más (+) o de menos (−) frente al ritmo del año hasta hoy. */
        val adelanto: Double,
        /** Altura de tu montaña personal en milímetros (los pasos a medias suben en proporción). */
        val mm: Long,
    ) {
        val cumbre: Boolean get() = restantes == 0
    }

    fun calcular(pasos: List<Paso>, hoy: LocalDate): Estado {
        val anio = hoy.year
        val t = Expedicion.tramoDe(hoy)
        val antes = pasos.count { it.cumplido && (it.cumplidoEn == null || it.cumplidoEn.year < anio) }
        val pendientesAlInicio = TOTAL_PASOS - antes
        val ganados = IntArray(Expedicion.TRAMOS)
        pasos.forEach { p -> p.cumplidoEn?.takeIf { p.cumplido && it.year == anio }?.let { ganados[Expedicion.tramoDe(it)]++ } }
        val d = dificultades(ganados.toList(), pendientesAlInicio, t)
        val ganadosAntesDeEsteTramo = (0 until t).sumOf { ganados[it] }
        val restantesAlEmpezar = pendientesAlInicio - ganadosAntesDeEsteTramo
        val ganadosAnio = ganadosAntesDeEsteTramo + ganados[t]
        val fin = proximoAjuste(hoy)
        val transcurrido = hoy.dayOfYear.toDouble() / hoy.lengthOfYear()
        val mm = pasos.sumOf { (avance(it, d[t]) * MM_POR_PASO).toLong() }
        return Estado(
            tramo = t, dificultad = d[t],
            diasParaAjuste = (fin.toEpochDay() - hoy.toEpochDay()).toInt(),
            esperadoTramo = restantesAlEmpezar.toDouble() / (Expedicion.TRAMOS - t),
            ganadosTramo = ganados[t], ganadosAnio = ganadosAnio,
            restantes = max(0, pendientesAlInicio - ganadosAnio),
            adelanto = ganadosAnio - pendientesAlInicio * transcurrido,
            mm = min(mm, Expedicion.ALTURA_MM),
        )
    }

    /** Todos los pasos de la mandala (8 campamentos × 8), en orden; sin campamento = paso vacío. */
    fun pasosDe(campamentos: List<Long?>, r: Map<String, String>): List<Paso> =
        (0 until Mandala.CAMPAMENTOS).flatMap { c -> (0 until Mandala.PASOS).map { p -> leer(campamentos.getOrNull(c), p, r) } }
}
