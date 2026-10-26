package com.rutaalacima.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth

/**
 * Movimientos fijos, igual que la web (web/js/20-fijos.js): un gasto, ingreso, ahorro o inversión que se
 * repite cada mes el mismo día (arriendo, salario, internet…). Se guardan en "fin#fijos" del workbook "finanzas".
 * Cada mes aparecen solos en su día (sin pasar de hoy). Si se cambian en un día, ese mes queda como movimiento
 * propio (fx = id del fijo); si se quitan de un día, ese mes se salta; si se cambia el monto, rige desde este mes.
 */
object Fijos {
    const val CLAVE = "fin#fijos"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class Fijo(val id: String, val t: String, val m: Double, val c: String = "otro", val n: String = "", val dia: Int = 1,
                    val desde: String, val hasta: String = "", val saltar: List<String> = emptyList(),
                    /** La deuda que paga cada mes (Salir de deudas), si es una cuota. */
                    val d: String? = null)

    private val ser = ListSerializer(Fijo.serializer())
    fun leer(v: String?): List<Fijo> = if (v.isNullOrBlank()) emptyList()
        else runCatching { json.decodeFromString(ser, v) }.getOrDefault(emptyList())
            .filter { it.m > 0 && Cuentas.Tipo.entries.any { t -> t.codigo == it.t } && Regex("\\d{4}-\\d{2}").matches(it.desde) }
    fun escribir(l: List<Fijo>): String = if (l.isEmpty()) "" else json.encodeToString(ser, l)

    /** El día del fijo en ese mes (el 31 cae el último día de los meses cortos). */
    fun fecha(x: Fijo, ym: YearMonth): LocalDate = ym.atDay(x.dia.coerceIn(1, ym.lengthOfMonth()))
    fun activoEn(x: Fijo, ym: String) = ym >= x.desde && (x.hasta.isEmpty() || ym <= x.hasta) && ym !in x.saltar

    /** Los movimientos virtuales de los fijos, por fecha: hasta hoy, o con [programados] hasta diciembre de este año
     *  (los de días que aún no llegan quedan marcados como programados: no cuentan en las sumas). */
    fun virtuales(l: List<Fijo>, hoy: LocalDate, programados: Boolean = false): Map<String, List<Cuentas.Mov>> {
        val r = mutableMapOf<String, MutableList<Cuentas.Mov>>()
        val tope = if (programados) YearMonth.of(hoy.year, 12) else YearMonth.from(hoy)
        l.forEach { x ->
            var ym = runCatching { YearMonth.parse(x.desde) }.getOrNull() ?: return@forEach
            val fin = if (x.hasta.isNotEmpty()) minOf(runCatching { YearMonth.parse(x.hasta) }.getOrDefault(tope), tope) else tope
            var n = 0
            while (ym <= fin && n++ < 600) {
                val f = fecha(x, ym)
                if (activoEn(x, ym.toString()) && (programados || !f.isAfter(hoy)))
                    r.getOrPut(f.toString()) { mutableListOf() } += Cuentas.Mov("fx-${x.id}", x.t, x.m, x.c, x.n, d = x.d, fx = x.id).also { it.virtual = true; it.prog = f.isAfter(hoy) }
                ym = ym.plusMonths(1)
            }
        }
        return r
    }

    /** Los fijos programados de los días que aún no llegan (hasta diciembre), sin los que ya se cambiaron a mano. */
    fun programados(dias: Map<String, List<Cuentas.Mov>>, l: List<Fijo>, hoy: LocalDate): Map<String, List<Cuentas.Mov>> =
        virtuales(l, hoy, programados = true).filterKeys { it > hoy.toString() }
            .mapValues { (f, vs) -> vs.filter { v -> dias[f].orEmpty().none { it.fx == v.fx } } }.filterValues { it.isNotEmpty() }

    /** Marcar «se repite cada mes» en un movimiento ya anotado el día [f]: nace un fijo desde ese mes. */
    fun desdeMovimiento(x: Cuentas.Mov, f: LocalDate, id: String): Fijo =
        Fijo(id, x.t, x.m, x.c, x.n, f.dayOfMonth, YearMonth.from(f).toString())

    /** Desmarcarlo en el mes [ym]: deja de repetirse desde el mes siguiente (si nació ese mes, desaparece). */
    fun dejarDesde(l: List<Fijo>, id: String, ym: YearMonth): List<Fijo> =
        l.mapNotNull { if (it.id != id) it else if (it.desde >= ym.toString()) null else it.copy(hasta = ym.minusMonths(1).toString()) }

    /** Une los fijos con lo guardado: el fijo no aparece el día que ya se cambió a mano (mismo fx). */
    fun unir(dias: Map<String, List<Cuentas.Mov>>, l: List<Fijo>, hoy: LocalDate): Map<String, List<Cuentas.Mov>> {
        if (l.isEmpty()) return dias
        val r = dias.toMutableMap()
        virtuales(l, hoy).forEach { (f, vs) ->
            val g = r[f].orEmpty()
            val nuevos = vs.filter { v -> g.none { it.fx == v.fx } }
            if (nuevos.isNotEmpty()) r[f] = nuevos + g
        }
        return r
    }

    /** Cambiar el monto: si el fijo ya corría en meses pasados, esos quedan con el monto de antes. */
    fun cambiarMonto(l: List<Fijo>, id: String, v: Double, hoy: LocalDate, nuevoId: String): List<Fijo> {
        val x = l.firstOrNull { it.id == id } ?: return l
        val hoyYm = YearMonth.from(hoy).toString()
        if (x.desde >= hoyYm) return l.map { if (it.id == id) it.copy(m = v) else it }
        val viejo = x.copy(hasta = YearMonth.from(hoy).minusMonths(1).toString(), saltar = x.saltar.filter { it < hoyYm })
        val nuevo = x.copy(id = nuevoId, m = v, desde = hoyYm, saltar = x.saltar.filter { it >= hoyYm })
        return l.map { if (it.id == id) viejo else it } + nuevo
    }

    /** Dejar de repetir: este mes cuenta, los siguientes no. */
    fun parar(l: List<Fijo>, id: String, hoy: LocalDate): List<Fijo> {
        val hoyYm = YearMonth.from(hoy).toString()
        return l.mapNotNull { if (it.id != id) it else if (it.desde > hoyYm) null else it.copy(hasta = hoyYm) }
    }

    fun saltar(l: List<Fijo>, id: String, ym: String, si: Boolean): List<Fijo> =
        l.map { if (it.id != id) it else it.copy(saltar = if (si) (it.saltar + ym).distinct() else it.saltar - ym) }
}
