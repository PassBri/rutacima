package com.rutaalacima.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth

/**
 * Metas financieras (igual que la web, web/js/30-metas-fin.js): lo que la persona quiere lograr con su dinero.
 * Se guardan en "fin#metas". Lo ahorrado = lo que ya tenía (inicial) + los movimientos del libro que nombran
 * la meta (Mov.meta): un aporte es ahorro y un retiro, un ahorro negativo.
 */
object MetasFin {
    const val CLAVE = "fin#metas"
    /** Tipo y la categoría de ahorro con que se anotan sus aportes. */
    val TIPOS = listOf("emergencia" to "emergencia", "viaje" to "meta", "vivienda" to "meta", "educacion" to "meta",
        "vehiculo" to "meta", "retiro" to "retiro", "compra" to "meta", "otra" to "meta")
    fun categoria(tipo: String) = TIPOS.firstOrNull { it.first == tipo }?.second ?: "meta"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class Meta(val id: String, val tipo: String = "otra", val nombre: String, val objetivo: Double, val fecha: String = "",
                    val inicial: Double = 0.0, val creada: String = "")

    private val ser = ListSerializer(Meta.serializer())
    fun leer(v: String?): List<Meta> = if (v.isNullOrBlank()) emptyList() else runCatching { json.decodeFromString(ser, v) }.getOrDefault(emptyList())
    fun escribir(l: List<Meta>): String = if (l.isEmpty()) "" else json.encodeToString(ser, l)

    enum class Estado { LOGRADA, A_TIEMPO, ATRASADA, VENCIDA, SIN_FECHA }
    data class Hoy(val meta: Meta, val ahorrado: Double, val falta: Double, val pct: Double, val meses: Int?, val aporteMes: Double,
                   val ritmo: Double, val estado: Estado, val llegaEn: Int?)

    private fun entre(a: String, b: String) = runCatching { (YearMonth.parse(b).year - YearMonth.parse(a).year) * 12 + YearMonth.parse(b).monthValue - YearMonth.parse(a).monthValue }.getOrNull()

    fun hoy(l: List<Meta>, dias: Map<String, List<Cuentas.Mov>>, hoy: LocalDate): List<Hoy> {
        val ym = YearMonth.from(hoy).toString(); val desde = YearMonth.from(hoy).minusMonths(2).toString()
        val aportes = dias.flatMap { (f, xs) -> xs.filter { it.meta != null && !it.virtual }.map { f to it } }.groupBy { it.second.meta }
        return l.map { m ->
            val movs = aportes[m.id].orEmpty()
            val ahorrado = (m.inicial + movs.sumOf { it.second.m }).coerceAtLeast(0.0)
            val falta = (m.objetivo - ahorrado).coerceAtLeast(0.0); val pct = if (m.objetivo > 0) (ahorrado / m.objetivo).coerceAtMost(1.0) else 0.0
            val meses = if (m.fecha.isNotEmpty()) entre(ym, m.fecha)?.plus(1) else null
            val aporteMes = if (falta > 0 && meses != null) falta / meses.coerceAtLeast(1) else 0.0
            val ritmo = movs.filter { it.first.take(7) in desde..ym }.sumOf { it.second.m } / 3
            val total = if (m.fecha.isNotEmpty() && m.creada.isNotEmpty()) entre(m.creada, m.fecha)?.plus(1)?.coerceAtLeast(1) else null
            val pasados = if (m.creada.isNotEmpty()) (entre(m.creada, ym) ?: 0).coerceAtLeast(0) else 0
            val esperado = total?.let { m.objetivo * minOf(1.0, pasados.toDouble() / it) } ?: 0.0
            val estado = when {
                pct >= 1 -> Estado.LOGRADA; meses != null && meses <= 0 -> Estado.VENCIDA; m.fecha.isEmpty() -> Estado.SIN_FECHA
                ahorrado + 0.5 >= esperado * 0.95 -> Estado.A_TIEMPO; else -> Estado.ATRASADA
            }
            Hoy(m, ahorrado, falta, pct, meses, aporteMes, ritmo, estado, if (falta > 0 && ritmo > 0) Math.ceil(falta / ritmo).toInt() else null)
        }
    }
}
