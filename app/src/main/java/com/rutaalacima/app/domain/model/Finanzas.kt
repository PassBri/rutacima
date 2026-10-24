package com.rutaalacima.app.domain.model

import kotlin.math.abs

/**
 * Control financiero (igual que en la web, web/js/17-finanzas.js): el día guarda ingresos, gastos, ahorro e inversión
 * como texto libre ("$ 50.000", "12,5"); aquí se leen como números y se suman por mes.
 * El presupuesto de gastos del mes vive en respuestas: workbook [WB], clave "fin#yyyy-MM#presupuesto".
 */
object Finanzas {
    const val WB = "finanzas"
    fun clavePresupuesto(anio: Int, mes: Int) = "fin#%04d-%02d#presupuesto".format(anio, mes)

    /** Lee un monto escrito a mano: "$ 1.250.000" = 1250000, "12,5" = 12.5, "1,250" = 1250, "-3.000" = -3000. */
    fun monto(texto: String?): Double {
        if (texto.isNullOrBlank()) return 0.0
        val negativo = texto.trim().startsWith("-")
        val s = texto.filter { it.isDigit() || it == '.' || it == ',' }
        if (s.none { it.isDigit() }) return 0.0
        val ult = s.indexOfLast { it == '.' || it == ',' }
        val valor = if (ult >= 0 && s.length - ult - 1 in 1..2) {
            val entero = s.substring(0, ult).filter { it.isDigit() }.ifEmpty { "0" }
            "$entero.${s.substring(ult + 1)}".toDouble()
        } else s.filter { it.isDigit() }.toDouble()
        return if (negativo) -valor else valor
    }

    data class Dia(val ingresos: Double = 0.0, val gastos: Double = 0.0, val ahorro: Double = 0.0, val inversion: Double = 0.0) {
        /** Lo que queda libre: lo que entró menos lo que salió, lo ahorrado y lo invertido. */
        val disponible get() = ingresos - gastos - ahorro - inversion
        val vacio get() = ingresos == 0.0 && gastos == 0.0 && ahorro == 0.0 && inversion == 0.0
        operator fun plus(o: Dia) = Dia(ingresos + o.ingresos, gastos + o.gastos, ahorro + o.ahorro, inversion + o.inversion)
    }

    fun dia(ingresos: String, gastos: String, ahorro: String, inversion: String) = Dia(monto(ingresos), monto(gastos), monto(ahorro), monto(inversion))

    data class Mes(val total: Dia, val gastosPorDia: Map<Int, Double>, val diasConRegistro: Int, val presupuesto: Double) {
        /** Parte de lo que entró que se fue a ahorro e inversión (0..1), o null si no entró nada. */
        val tasaAhorro: Double? get() = if (total.ingresos > 0) ((total.ahorro + total.inversion) / total.ingresos).coerceIn(0.0, 1.0) else null
        /** Gasto del presupuesto (0..), o null sin presupuesto. */
        val usoPresupuesto: Double? get() = if (presupuesto > 0) total.gastos / presupuesto else null
        val vacio get() = diasConRegistro == 0
    }

    /** Suma un mes a partir de sus días (día del mes → montos). */
    fun mes(dias: Map<Int, Dia>, presupuesto: String?): Mes {
        val conDatos = dias.filterValues { !it.vacio }
        return Mes(conDatos.values.fold(Dia()) { a, b -> a + b }, conDatos.mapValues { it.value.gastos }.filterValues { it != 0.0 },
            conDatos.size, abs(monto(presupuesto)))
    }

    /** "1.250.000" o "12,5": sin símbolo de moneda (cada persona usa la suya), con separadores del idioma. */
    fun formato(v: Double, locale: java.util.Locale = java.util.Locale.getDefault()): String =
        java.text.NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = if (v % 1.0 == 0.0) 0 else 2 }.format(v)
}
