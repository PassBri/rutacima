package com.rutaalacima.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.Currency
import java.util.Locale

/**
 * Ascenso financiero (igual que en la web, web/js/18-ascenso.js): un libro de cuentas que sube y baja,
 * balance general → año → mes → día. Cada día guarda sus movimientos en la respuesta "fin#mov#<aaaa-mm-dd>"
 * (workbook [Finanzas.WB]) como JSON: [{ id, t: "ing"|"gas"|"aho"|"inv", m: monto, c: categoría, n: concepto }].
 * Los gastos van a las tres mochilas del libro Cima financiera: verde (lo esencial, ~50 %), amarilla
 * (gastos variables, ~30 %) y roja (ahorro e inversión, ≥ 20 %). Los montos del control financiero de
 * antes (agenda: ingresos, gastos, ahorro, inversión) se cuentan como movimientos del día.
 */
object Cuentas {
    const val WB = Finanzas.WB
    const val CLAVE_MONEDA = "fin#moneda"
    fun claveDia(fecha: String) = "fin#mov#$fecha"
    const val PREFIJO = "fin#mov#"

    @Serializable
    data class Mov(val id: String, val t: String, val m: Double, val c: String = "otro", val n: String = "") {
        /** Si viene del control financiero de antes: el campo de la agenda ("ingresos"…). */
        @kotlinx.serialization.Transient var agenda: String? = null
    }

    enum class Tipo(val codigo: String) { ING("ing"), GAS("gas"), AHO("aho"), INV("inv");
        companion object { fun de(c: String) = entries.firstOrNull { it.codigo == c } ?: GAS }
    }
    enum class Mochila(val ideal: Double) { VERDE(0.5), AMARILLA(0.3), ROJA(0.2) }

    /** Categorías por tipo (los códigos son los mismos de la web); la mochila solo aplica a gastos. */
    val CATEGORIAS: Map<Tipo, List<Pair<String, Mochila?>>> = mapOf(
        Tipo.ING to listOf("trabajo", "negocio", "ventas", "honorarios", "pasivo", "otro").map { it to null },
        Tipo.GAS to listOf("alimentacion", "vivienda", "servicios", "transporte", "salud", "deudas").map { it to Mochila.VERDE } +
            listOf("entretenimiento", "restaurantes", "ropa", "suscripciones", "hormiga", "otro").map { it to Mochila.AMARILLA },
        Tipo.AHO to listOf("emergencia", "meta", "retiro", "otro").map { it to Mochila.ROJA },
        Tipo.INV to listOf("educacion", "herramientas", "fondos", "negocio", "inmuebles", "otro").map { it to Mochila.ROJA },
    )

    fun mochila(m: Mov): Mochila? = when (Tipo.de(m.t)) {
        Tipo.ING -> null
        Tipo.AHO, Tipo.INV -> Mochila.ROJA
        Tipo.GAS -> CATEGORIAS.getValue(Tipo.GAS).firstOrNull { it.first == m.c }?.second ?: Mochila.AMARILLA
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val ser = ListSerializer(Mov.serializer())

    fun leer(v: String?): List<Mov> =
        if (v.isNullOrBlank()) emptyList()
        else runCatching { json.decodeFromString(ser, v) }.getOrDefault(emptyList()).filter { it.m.isFinite() && Tipo.entries.any { t -> t.codigo == it.t } }

    fun escribir(xs: List<Mov>): String {
        val l = xs.filter { it.agenda == null }.map { it.copy(m = Math.round(it.m * 100) / 100.0, n = it.n.take(120)) }
        return if (l.isEmpty()) "" else json.encodeToString(ser, l)
    }

    /** Los montos del control financiero de antes, como movimientos del día. */
    fun deAgenda(ingresos: String, gastos: String, ahorro: String, inversion: String): List<Mov> =
        listOf("ingresos" to ("ing" to ingresos), "gastos" to ("gas" to gastos), "ahorro" to ("aho" to ahorro), "inversion" to ("inv" to inversion))
            .mapNotNull { (campo, p) ->
                val v = kotlin.math.abs(Finanzas.monto(p.second))
                if (v == 0.0) null else Mov("ag-$campo", p.first, v, "otro", "").also { it.agenda = campo }
            }

    data class Suma(
        val ing: Double = 0.0, val gas: Double = 0.0, val aho: Double = 0.0, val inv: Double = 0.0, val n: Int = 0,
        val mochilas: Map<Mochila, Double> = emptyMap(), val categorias: Map<Pair<Tipo, String>, Double> = emptyMap(),
    ) {
        /** Lo que queda libre. */
        val disponible get() = ing - gas - aho - inv
        /** Lo que suma a tu patrimonio. */
        val construido get() = aho + inv
        val tasa: Double? get() = if (ing > 0) (construido / ing).coerceIn(0.0, 1.0) else null
        fun de(t: Tipo) = when (t) { Tipo.ING -> ing; Tipo.GAS -> gas; Tipo.AHO -> aho; Tipo.INV -> inv }
    }

    fun sumar(movs: List<Mov>): Suma {
        var s = Suma()
        val moch = mutableMapOf<Mochila, Double>(); val cat = mutableMapOf<Pair<Tipo, String>, Double>()
        movs.forEach { x ->
            val t = Tipo.de(x.t)
            s = when (t) { Tipo.ING -> s.copy(ing = s.ing + x.m); Tipo.GAS -> s.copy(gas = s.gas + x.m); Tipo.AHO -> s.copy(aho = s.aho + x.m); Tipo.INV -> s.copy(inv = s.inv + x.m) }
            mochila(x)?.let { moch[it] = (moch[it] ?: 0.0) + x.m }
            cat[t to x.c] = (cat[t to x.c] ?: 0.0) + x.m
        }
        return s.copy(n = movs.size, mochilas = moch, categorias = cat)
    }

    // ---------------------------------------------------------------- Moneda

    private val POR_IDIOMA = mapOf("es" to "COP", "en" to "USD", "pt" to "BRL", "fr" to "EUR", "de" to "EUR", "it" to "EUR",
        "zh" to "CNY", "ja" to "JPY", "ko" to "KRW", "ar" to "SAR", "hi" to "INR", "ru" to "RUB")
    val MONEDAS = listOf("COP", "USD", "EUR", "MXN", "ARS", "CLP", "PEN", "BOB", "PYG", "UYU", "VES", "GTQ", "CRC", "DOP", "HNL", "NIO",
        "BRL", "CAD", "GBP", "CHF", "AUD", "CNY", "JPY", "KRW", "INR", "RUB", "SAR", "AED", "EGP", "MAD")

    /** La elegida o la de tu país (y si no se sabe el país, la de tu idioma). */
    fun moneda(elegida: String?, locale: Locale = Locale.getDefault()): String =
        elegida?.takeIf { it.isNotBlank() }
            ?: runCatching { if (locale.country.isNotBlank()) Currency.getInstance(locale).currencyCode else null }.getOrNull()
            ?: POR_IDIOMA[locale.language] ?: "USD"

    /** "$ 1.250.000", "US$ 12,50", "1.234,50 €"… sin decimales si el monto es entero. */
    fun formato(v: Double, moneda: String, locale: Locale = Locale.getDefault()): String {
        val f = java.text.NumberFormat.getCurrencyInstance(locale)
        runCatching { f.currency = Currency.getInstance(moneda) }
        val entero = kotlin.math.abs(v % 1.0) < 0.005
        f.minimumFractionDigits = if (entero) 0 else 2
        f.maximumFractionDigits = if (entero) 0 else 2
        return f.format(v)
    }

    /** El número sin símbolo, con los separadores del idioma (para editar). */
    fun numero(v: Double, locale: Locale = Locale.getDefault()): String =
        java.text.NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }.format(v)
}
