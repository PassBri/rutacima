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
    /** Avalancha, bola de nieve, flujo de caja, híbrido y mi orden (igual que METODOS_DEUDA en la web). */
    val METODOS = listOf("avalancha", "bola", "flujo", "hibrido", "propio")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class Deuda(val id: String, val nombre: String, val saldo: Double, val tasa: Double = 0.0, val minimo: Double = 0.0, val tipo: String = "mala",
                     val r: List<Cuentas.Recibo> = emptyList(),   // r: documentos (extracto, contrato)
                     /** El saldo es el de esta fecha (y hora): solo los pagos posteriores lo bajan. Vacío: cuentan todos. */
                     val desde: String = "", val desdeT: Long = 0)

    @Serializable
    data class Datos(val metodo: String = "avalancha", val extra: Double = 0.0, val lista: List<Deuda> = emptyList())

    fun leer(v: String?): Datos = if (v.isNullOrBlank()) Datos() else runCatching { json.decodeFromString(Datos.serializer(), v) }.getOrDefault(Datos())
        .let { it.copy(metodo = if (it.metodo in METODOS) it.metodo else "avalancha") }
    fun escribir(d: Datos): String = if (d.lista.isEmpty() && d.extra == 0.0) "" else json.encodeToString(Datos.serializer(), d)

    /** Una deuda con lo pagado (movimientos del libro que la nombran) y lo que se debe hoy. [deuda].minimo es su cuota:
     *  la escrita o, si no hay, lo que se le pagó el último mes con pagos anotados ([cuotaAuto]). */
    data class Hoy(val deuda: Deuda, val pagado: Double, val cuotaAuto: Boolean = false, val nPagos: Int = 0, val minimoPropio: Double = deuda.minimo) {
        val actual get() = (deuda.saldo - pagado).coerceAtLeast(0.0)
    }
    private fun ts(id: String) = id.filter { it.isDigit() }.take(15).toLongOrNull() ?: 0L

    /** Igual que la web: un pago baja el saldo si es posterior a la fecha del saldo ([Deuda.desde]). */
    fun hoy(d: Datos, dias: Map<String, List<Cuentas.Mov>>, hoy: String): List<Hoy> {
        val pagos = dias.filterKeys { it <= hoy }.flatMap { (f, xs) -> xs.filter { it.d != null && !it.prog }.map { f to it } }
        return d.lista.map { x ->
            val mios = pagos.filter { it.second.d == x.id }
            val pagado = mios.filter { (f, m) -> x.desde.isEmpty() || f > x.desde || (f == x.desde && ts(m.id) > x.desdeT) }.sumOf { it.second.m }
            val cuota = if (x.minimo > 0) 0.0 else mios.groupBy { it.first.take(7) }.maxByOrNull { it.key }?.value?.sumOf { it.second.m } ?: 0.0
            Hoy(if (x.minimo > 0) x else x.copy(minimo = cuota), pagado, x.minimo <= 0 && cuota > 0, mios.size, x.minimo)
        }
    }

    /** [serie]: lo que se debe al empezar y al final de cada mes. */
    data class Plan(val meses: Int?, val interes: Double, val fin: Map<String, Int?>, val serie: List<Double> = emptyList(),
                    val pagoMes: Map<String, Double> = emptyMap(), val interesDe: Map<String, Double> = emptyMap(),
                    /** Lo que recibe cada deuda mes a mes (hasta 120 meses) y lo que se debe al final de cada mes. */
                    val pagos: List<Pair<Map<String, Double>, Double>> = emptyList())

    /**
     * Mes a mes: cada deuda suma su interés, se pagan los mínimos y lo que sobra (el extra y los mínimos de las ya
     * pagadas) va a la deuda que elige el método. "minimos" paga solo la cuota de cada una, para comparar.
     */
    fun plan(lista: List<Hoy>, metodo: String, extra: Double): Plan {
        class D(val id: String, var b: Double, val r: Double, val min: Double, val k: Int, var fin: Int? = null)
        val ds = lista.filter { it.actual > 0.005 }.mapIndexed { k, h -> D(h.deuda.id, h.actual, h.deuda.tasa / 100 / 12, h.deuda.minimo, k) }
        val minimos = metodo == "minimos"
        val extraPos = extra.coerceAtLeast(0.0)
        fun rapida(d: D) = d.b <= 3 * (d.min + extraPos)
        val orden: Comparator<D> = when (metodo) {
            "bola" -> compareBy { it.b }
            "flujo" -> compareBy<D> { if (it.min > 0) it.b / it.min else Double.MAX_VALUE }.thenByDescending { it.r }
            "hibrido" -> Comparator { a, b ->
                val qa = rapida(a); val qb = rapida(b)
                when { qa && !qb -> -1; qb && !qa -> 1; qa -> a.b.compareTo(b.b); else -> b.r.compareTo(a.r).takeIf { it != 0 } ?: a.b.compareTo(b.b) }
            }
            "propio" -> compareBy { it.k }
            else -> compareByDescending<D> { it.r }.thenBy { it.b }
        }
        var mes = 0; var interes = 0.0
        val serie = mutableListOf(ds.sumOf { it.b })
        val pago1 = mutableMapOf<String, Double>(); val intDe = mutableMapOf<String, Double>()
        val pagos = mutableListOf<Pair<Map<String, Double>, Double>>()
        while (ds.any { it.b > 0.005 } && mes < 600) {
            mes++
            ds.forEach { if (it.b > 0.005) { val i = it.b * it.r; it.b += i; interes += i; intDe[it.id] = (intDe[it.id] ?: 0.0) + i } }
            val antes = if (mes == 1) ds.associate { it.id to it.b } else null
            val previo = ds.associate { it.id to it.b }
            var bolsa = if (minimos) 0.0 else extraPos
            ds.forEach { if (it.b > 0.005) { val p = minOf(it.min, it.b); it.b -= p; if (!minimos) bolsa += it.min - p } else if (!minimos) bolsa += it.min }
            for (d in ds.filter { it.b > 0.005 }.sortedWith(orden)) { if (bolsa <= 0.005) break; val p = minOf(bolsa, d.b); d.b -= p; bolsa -= p }
            antes?.let { a -> ds.forEach { pago1[it.id] = (a[it.id] ?: 0.0) - it.b } }
            if (mes <= 120) pagos += ds.associate { it.id to ((previo[it.id] ?: 0.0) - it.b).coerceAtLeast(0.0) } to ds.sumOf { it.b.coerceAtLeast(0.0) }
            ds.forEach { if (it.b <= 0.005 && it.fin == null) it.fin = mes }
            serie += ds.sumOf { it.b.coerceAtLeast(0.0) }
        }
        return Plan(if (ds.none { it.b > 0.005 }) mes else null, interes, ds.associate { it.id to it.fin }, serie, pago1, intDe, pagos)
    }
}

object Patrimonio {
    const val CLAVE = "fin#activos"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val TIPOS = listOf("liquidez", "inversion", "bienes", "negocio", "otro")

    @Serializable
    data class Activo(val id: String, val nombre: String, val tipo: String = "otro", val valor: Double = 0.0, val r: List<Cuentas.Recibo> = emptyList())   // r: documentos (escritura, extracto)

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
           propias: Map<String, List<Cuentas.Propia>> = emptyMap(), anioActual: Int = java.time.LocalDate.now().year,
           sinDeudas: Boolean = false): Resultado {
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
        // Sin deudas: no hay saldo ni cuotas, y lo que hoy se va en pagos de deuda queda libre
        val pagosDeuda = if (sinDeudas) s.categorias[Cuentas.Tipo.GAS to "deudas"] ?: 0.0 else 0.0
        val gas = s.gas - pagosDeuda
        val egresos = gas + s.aho + s.inv
        val ds = if (actual && !sinDeudas) deudas else emptyList()
        val pasivos = ds.sumOf { it.actual }
        val buenas = ds.filter { it.deuda.tipo == "buena" }.sumOf { it.actual }
        val deudaMes = if (sinDeudas) 0.0 else maxOf(ds.filter { it.actual > 0 }.sumOf { it.deuda.minimo }, (s.categorias[Cuentas.Tipo.GAS to "deudas"] ?: 0.0) / meses)   // lo pagado de verdad, o las cuotas mínimas si son más
        val totalActivos = (if (actual) activos.sumOf { it.valor } else 0.0) + hasta.construido
        // Fondo de emergencia: el ahorro de «Emergencia» más el efectivo y las cuentas que la persona declara en Activos
        val emergencia = (hasta.categorias[Cuentas.Tipo.AHO to "emergencia"] ?: 0.0) + (if (actual) activos.filter { it.tipo == "liquidez" }.sumOf { it.valor } else 0.0)
        val ing = s.ing
        fun r(a: Double, b: Double) = if (b > 0) a / b else null
        val flujo = r(ing - egresos, ing)
        val liquidez = r(ing, egresos)
        val tasaAhorro = r(s.aho, ing)
        val tasaConstruye = r(s.construido, ing)
        val dti = if (ing > 0) deudaMes / (ing / meses) else null
        val gastoIng = r(gas, ing)
        val fondo = if (fijos > 0) emergencia / (fijos / meses) else null
        val endeuda = if (!actual) null else if (pasivos == 0.0) 0.0 else if (totalActivos > 0) pasivos / totalActivos else Double.POSITIVE_INFINITY
        val pasivosRatio = r(pasivoIng, ing)
        val libertad = r(pasivoIng, gas)
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
