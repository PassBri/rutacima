package com.rutaalacima.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Salir de deudas (libro 3, Cargas financieras) y salud financiera (libros 4 a 7), igual que la web
 * (web/js/19-cuentas-mas.js). Se guardan como respuestas del workbook "finanzas":
 * "fin#deudas", "fin#activos", "fin#visibilidad", "fin#bloqueo", "fin#ocultar", "fin#cats".
 */
object Deudas {
    const val CLAVE = "fin#deudas"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class Deuda(val id: String, val nombre: String, val saldo: Double, val tasa: Double = 0.0, val minimo: Double = 0.0, val tipo: String = "mala")

    @Serializable
    data class Datos(val metodo: String = "avalancha", val extra: Double = 0.0, val lista: List<Deuda> = emptyList())

    fun leer(v: String?): Datos = if (v.isNullOrBlank()) Datos() else runCatching { json.decodeFromString(Datos.serializer(), v) }.getOrDefault(Datos())
        .let { it.copy(metodo = if (it.metodo == "bola") "bola" else "avalancha") }
    fun escribir(d: Datos): String = if (d.lista.isEmpty() && d.extra == 0.0) "" else json.encodeToString(Datos.serializer(), d)

    /** Una deuda con lo pagado (movimientos del libro que la nombran) y lo que se debe hoy. */
    data class Hoy(val deuda: Deuda, val pagado: Double) { val actual get() = (deuda.saldo - pagado).coerceAtLeast(0.0) }

    fun hoy(d: Datos, movs: Collection<Cuentas.Mov>): List<Hoy> {
        val pagos = movs.filter { it.d != null }.groupBy { it.d }.mapValues { e -> e.value.sumOf { it.m } }
        return d.lista.map { Hoy(it, pagos[it.id] ?: 0.0) }
    }

    data class Plan(val meses: Int?, val interes: Double, val fin: Map<String, Int?>)

    /**
     * Mes a mes: cada deuda suma su interés, se pagan los mínimos y lo que sobra (el extra y los mínimos
     * de las ya pagadas) va a la deuda que elige el método: avalancha = la de mayor interés; bola de nieve = la más pequeña.
     */
    fun plan(lista: List<Hoy>, metodo: String, extra: Double): Plan {
        class D(val id: String, var b: Double, val r: Double, val min: Double, var fin: Int? = null)
        val ds = lista.filter { it.actual > 0.005 }.map { D(it.deuda.id, it.actual, it.deuda.tasa / 100 / 12, it.deuda.minimo) }
        var mes = 0; var interes = 0.0
        while (ds.any { it.b > 0.005 } && mes < 600) {
            mes++
            ds.forEach { if (it.b > 0.005) { val i = it.b * it.r; it.b += i; interes += i } }
            var bolsa = extra.coerceAtLeast(0.0)
            ds.forEach { if (it.b > 0.005) { val p = minOf(it.min, it.b); it.b -= p; bolsa += it.min - p } else bolsa += it.min }
            val orden = ds.filter { it.b > 0.005 }.let { l -> if (metodo == "bola") l.sortedBy { it.b } else l.sortedWith(compareByDescending<D> { it.r }.thenBy { it.b }) }
            for (d in orden) { if (bolsa <= 0.005) break; val p = minOf(bolsa, d.b); d.b -= p; bolsa -= p }
            ds.forEach { if (it.b <= 0.005 && it.fin == null) it.fin = mes }
        }
        return Plan(if (ds.none { it.b > 0.005 }) mes else null, interes, ds.associate { it.id to it.fin })
    }
}

object Patrimonio {
    const val CLAVE = "fin#activos"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val TIPOS = listOf("liquidez", "inversion", "bienes", "negocio", "otro")

    @Serializable
    data class Activo(val id: String, val nombre: String, val tipo: String = "otro", val valor: Double = 0.0)

    private val ser = ListSerializer(Activo.serializer())
    fun leer(v: String?): List<Activo> = if (v.isNullOrBlank()) emptyList() else runCatching { json.decodeFromString(ser, v) }.getOrDefault(emptyList())
    fun escribir(l: List<Activo>): String = if (l.isEmpty()) "" else json.encodeToString(ser, l)
}

/**
 * Salud financiera de un periodo (promedio mensual de los meses con movimientos), con el Índice de
 * Salud Financiera (ISF): 80-100 excelente, 50-79 buena, 20-49 moderada, menos de 20 en riesgo.
 */
object Salud {
    enum class Estado { OK, MEDIO, BAJO, NA }
    enum class Nivel { EXCELENTE, BUENA, MODERADA, RIESGO }

    data class Indicador(val clave: String, val valor: Double, val estado: Estado)

    data class Resultado(
        val meses: Int, val suma: Cuentas.Suma, val ingActivos: Double, val ingPasivos: Double, val fijos: Double, val variables: Double,
        val egresos: Double, val flujoNeto: Double, val deudaMes: Double, val activos: Double, val pasivos: Double, val actual: Boolean,
        val indicadores: List<Indicador>, val isf: Int?,
    ) {
        val neto get() = activos - pasivos
        val nivel: Nivel? get() = isf?.let { when { it >= 80 -> Nivel.EXCELENTE; it >= 50 -> Nivel.BUENA; it >= 20 -> Nivel.MODERADA; else -> Nivel.RIESGO } }
        fun mensual(v: Double) = v / meses
    }

    /**
     * @param dias movimientos por fecha ISO; @param anio null = últimos 12 meses con movimientos.
     * Las deudas y los activos declarados solo cuentan en el año actual y en el balance general.
     */
    fun de(dias: Map<String, List<Cuentas.Mov>>, anio: Int?, deudas: List<Deudas.Hoy>, activos: List<Patrimonio.Activo>,
           propias: Map<String, List<Cuentas.Propia>> = emptyMap(), anioActual: Int = java.time.LocalDate.now().year): Resultado {
        val fechas = dias.keys.sorted()
        val rango = if (anio != null) fechas.filter { it.startsWith("$anio-") }
            else { val ms = fechas.map { it.take(7) }.distinct().takeLast(12).toSet(); fechas.filter { it.take(7) in ms } }
        val s = Cuentas.sumar(rango.flatMap { dias[it].orEmpty() }, propias)
        val meses = rango.map { it.take(7) }.distinct().size.coerceAtLeast(1)
        val corte = if (anio != null) "$anio-12-31" else "9999-12-31"
        val hasta = Cuentas.sumar(fechas.filter { it <= corte }.flatMap { dias[it].orEmpty() }, propias)
        val actual = anio == null || anio == anioActual
        val pasivoIng = s.categorias[Cuentas.Tipo.ING to "pasivo"] ?: 0.0
        val fijos = s.mochilas[Cuentas.Mochila.VERDE] ?: 0.0
        val variables = s.mochilas[Cuentas.Mochila.AMARILLA] ?: 0.0
        val egresos = s.gas + s.aho + s.inv
        val ds = if (actual) deudas else emptyList()
        val pasivos = ds.sumOf { it.actual }
        val buenas = ds.filter { it.deuda.tipo == "buena" }.sumOf { it.actual }
        val deudaMes = ds.filter { it.actual > 0 }.sumOf { it.deuda.minimo }.takeIf { it > 0 } ?: ((s.categorias[Cuentas.Tipo.GAS to "deudas"] ?: 0.0) / meses)
        val totalActivos = (if (actual) activos.sumOf { it.valor } else 0.0) + hasta.construido
        val emergencia = hasta.categorias[Cuentas.Tipo.AHO to "emergencia"] ?: 0.0
        val ing = s.ing
        fun r(a: Double, b: Double) = if (b > 0) a / b else null
        val flujo = r(ing - egresos, ing)
        val liquidez = r(ing, egresos)
        val tasaAhorro = r(s.aho, ing)
        val tasaConstruye = r(s.construido, ing)
        val dti = if (ing > 0) deudaMes / (ing / meses) else null
        val gastoIng = r(s.gas, ing)
        val fondo = if (fijos > 0) emergencia / (fijos / meses) else null
        val endeuda = if (!actual) null else if (pasivos == 0.0) 0.0 else if (totalActivos > 0) pasivos / totalActivos else Double.POSITIVE_INFINITY
        val pasivosRatio = r(pasivoIng, ing)
        val libertad = r(pasivoIng, s.gas)
        val buenasRatio = r(buenas, pasivos)
        val netoIng = if (actual && ing > 0) (totalActivos - pasivos) / (ing / meses * 12) else null

        fun mayor(v: Double?, ok: Double, medio: Double) = when { v == null -> Estado.NA; v >= ok -> Estado.OK; v >= medio -> Estado.MEDIO; else -> Estado.BAJO }
        fun menor(v: Double?, ok: Double, medio: Double) = when { v == null -> Estado.NA; v <= ok -> Estado.OK; v <= medio -> Estado.MEDIO; else -> Estado.BAJO }
        val ind = listOfNotNull(
            flujo?.let { Indicador("flujo", it, mayor(it, 0.2, 0.0)) },
            liquidez?.let { Indicador("liquidez", it, mayor(it, 1.2, 1.0)) },
            tasaAhorro?.let { Indicador("ahorro", it, mayor(it, 0.1, 0.05)) },
            tasaConstruye?.let { Indicador("construye", it, mayor(it, 0.2, 0.1)) },
            dti?.let { Indicador("dti", it, menor(it, 0.3, 0.4)) },
            gastoIng?.let { Indicador("gasto", it, menor(it, 0.7, 0.9)) },
            fondo?.let { Indicador("fondo", it, mayor(it, 3.0, 1.0)) },
            endeuda?.let { Indicador("endeuda", it, menor(it, 0.3, 0.5)) },
            pasivosRatio?.let { Indicador("pasivos", it, mayor(it, 0.2, 0.05)) },
            libertad?.let { Indicador("libertad", it, mayor(it, 1.0, 0.25)) },
            buenasRatio?.let { Indicador("buenas", it, if (it >= 0.5) Estado.OK else Estado.MEDIO) },
            netoIng?.let { Indicador("netoing", it, mayor(it, 1.0, 0.3)) },
        )
        fun lin(v: Double?, malo: Double, bueno: Double): Double? = when {
            v == null -> null
            v.isInfinite() -> if (bueno > malo) 1.0 else 0.0
            else -> ((v - malo) / (bueno - malo)).coerceIn(0.0, 1.0)
        }
        val partes = listOf(lin(flujo, 0.0, 0.2) to 2.0, lin(tasaConstruye, 0.0, 0.2) to 2.0, lin(dti, 0.5, 0.2) to 1.5, lin(gastoIng, 1.0, 0.7) to 1.5,
            lin(liquidez, 1.0, 1.2) to 1.0, lin(fondo, 0.0, 6.0) to 1.5, (endeuda?.let { lin(it, 1.0, 0.3) }) to 1.5,
            (if (actual) (if (totalActivos - pasivos >= 0) 1.0 else 0.0) else null) to 1.0, lin(libertad, 0.0, 1.0) to 0.5)
            .mapNotNull { (v, w) -> v?.let { it to w } }
        val isf = if (partes.isNotEmpty() && ing > 0) Math.round(partes.sumOf { it.first * it.second } / partes.sumOf { it.second } * 100).toInt() else null
        return Resultado(meses, s, ing - pasivoIng, pasivoIng, fijos, variables, egresos, ing - egresos, deudaMes, totalActivos, pasivos, actual, ind, isf)
    }
}
