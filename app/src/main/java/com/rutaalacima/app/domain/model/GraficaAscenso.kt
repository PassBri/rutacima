package com.rutaalacima.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Datos de la gráfica del ascenso (igual que la web, web/js/21-grafica.js): los periodos (días, semanas,
 * meses o años) desde el primer movimiento hasta hoy, con sus sumas, el saldo acumulado como vela
 * (abrió, máximo, mínimo, cerró), el patrimonio construido y los indicadores de salud en %.
 */
object GraficaAscenso {
    enum class Gran { D, S, M, A }

    data class Periodo(
        val inicio: LocalDate, var ing: Double = 0.0, var gas: Double = 0.0, var aho: Double = 0.0, var inv: Double = 0.0,
        var o: Double = 0.0, var h: Double = 0.0, var l: Double = 0.0, var c: Double = 0.0, var patr: Double = 0.0,
    ) {
        val disp get() = ing - gas - aho - inv
        val saldo get() = c
        val flujo get() = if (ing > 0) disp / ing else null
        val constr get() = if (ing > 0) (aho + inv) / ing else null
        val gastoP get() = if (ing > 0) gas / ing else null
        fun serie(k: String): Double = when (k) { "saldo" -> c; "patr" -> patr; "ing" -> ing; "gas" -> gas; "aho" -> aho; "inv" -> inv; "disp" -> disp; else -> 0.0 }
        fun pct(k: String): Double? = when (k) { "flujo" -> flujo; "constr" -> constr; else -> gastoP }
    }

    fun inicioDe(d: LocalDate, g: Gran): LocalDate = when (g) {
        Gran.D -> d
        Gran.S -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        Gran.M -> d.withDayOfMonth(1)
        Gran.A -> d.withDayOfYear(1)
    }

    fun de(dias: Map<String, List<Cuentas.Mov>>, g: Gran, hoy: LocalDate, propias: Map<String, List<Cuentas.Propia>> = emptyMap()): List<Periodo> {
        val porDia = dias.mapNotNull { (f, xs) -> runCatching { LocalDate.parse(f) }.getOrNull()?.let { it to Cuentas.sumar(xs, propias) } }.toMap()
        if (porDia.isEmpty()) return emptyList()
        val ini = minOf(porDia.keys.min(), hoy)
        val out = mutableListOf<Periodo>()
        var saldo = 0.0; var constr = 0.0; var cur: Periodo? = null
        var d = ini; var n = 0
        while (!d.isAfter(hoy) && n++ < 20000) {
            val k = inicioDe(d, g)
            if (cur == null || cur.inicio != k) { cur = Periodo(k, o = saldo, h = saldo, l = saldo, c = saldo); out += cur }
            porDia[d]?.let { s -> cur.ing += s.ing; cur.gas += s.gas; cur.aho += s.aho; cur.inv += s.inv; saldo += s.disponible; constr += s.aho + s.inv }
            cur.h = maxOf(cur.h, saldo); cur.l = minOf(cur.l, saldo); cur.c = saldo; cur.patr = constr
            d = d.plusDays(1)
        }
        return out
    }

    /** Cuántos periodos muestra un rango rápido (1M, 3M, 6M, 1A, 5A; 0 = todo). */
    fun cuantos(meses: Int, g: Gran): Int = if (meses == 0) Int.MAX_VALUE else Math.ceil(meses * 30.4 / when (g) { Gran.D -> 1.0; Gran.S -> 7.0; Gran.M -> 30.4; Gran.A -> 365.0 }).toInt()

    /** Marcas "redondas" del eje de dinero. */
    fun marcas(lo: Double, hi: Double, n: Int = 5): List<Double> {
        val p0 = (hi - lo) / n; if (p0 <= 0) return listOf(lo)
        val m = Math.pow(10.0, Math.floor(Math.log10(p0)))
        val paso = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * m }.firstOrNull { it >= p0 } ?: p0
        val r = mutableListOf<Double>(); var v = Math.ceil(lo / paso) * paso
        while (v <= hi) { r += (Math.round(v / paso) * paso).let { if (it == -0.0) 0.0 else it }; v += paso }
        return r
    }
}
