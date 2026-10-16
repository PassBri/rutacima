package com.rutaalacima.app.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * El cielo real con sus eventos: estrellas, constelaciones, lluvias de meteoros, eclipses,
 * conjunciones, superlunas, equinoccios y solsticios. Los datos salen de `assets/cielo/datos.json`,
 * generado por tools/cielo/generar.js (el mismo archivo que usa la web en js/09b-cielo-datos.js).
 * La lógica es idéntica a la de web/js/10-cielo.js; CieloEventosTest las compara.
 * Regla de oro: nunca se inventa un evento.
 */
class CieloDatos(
    val estrellas: List<Triple<Double, Double, Double>>,          // ascensión recta (°), declinación (°), magnitud
    val lineas: List<Linea>,
    val nombres: Map<String, Nombre>,
    val lluvias: List<Lluvia>,
    val eventos: List<Evento>,
) {
    class Linea(val id: String, val rango: Int, val segmentos: List<List<Pair<Double, Double>>>)
    class Nombre(val ra: Double, val dec: Double, val textos: Map<String, String>) {
        fun en(idioma: String) = textos[idioma] ?: textos["en"] ?: textos.values.first()
    }
    class Lluvia(val id: String, val constelacion: String, val ra: Double, val dec: Double, val thz: Double,
                 val pico: String, val desde: String, val hasta: String, val ancho: Double)

    class Evento(
        val tipo: String, val clase: String?, val max: Long,
        val inicio: Long? = null, val fin: Long? = null,
        val parcialInicio: Long? = null, val parcialFin: Long? = null,
        val totalInicio: Long? = null, val totalFin: Long? = null,
        val oscurecimiento: Double = 0.0, val cuerpos: List<String> = emptyList(), val sep: Double = 0.0, val km: Int = 0,
    )

    companion object {
        fun leer(json: String): CieloDatos {
            val o = Json.parseToJsonElement(json).jsonObject
            fun JsonObject.t(k: String) = this[k]?.jsonPrimitive?.content?.let { Instant.parse(it).toEpochMilli() }
            val estrellas = o.getValue("estrellas").jsonArray.map { val a = it.jsonArray; Triple(a[0].jsonPrimitive.double, a[1].jsonPrimitive.double, a[2].jsonPrimitive.double) }
            val lineas = o.getValue("lineas").jsonArray.map { l ->
                val lo = l.jsonObject
                Linea(lo.getValue("id").jsonPrimitive.content, lo["r"]?.jsonPrimitive?.int ?: 1,
                    lo.getValue("l").jsonArray.map { seg -> seg.jsonArray.map { p -> p.jsonArray.let { it[0].jsonPrimitive.double to it[1].jsonPrimitive.double } } })
            }
            val nombres = o.getValue("nombres").jsonObject.mapValues { (_, v) ->
                val n = v.jsonObject; val pos = n.getValue("pos").jsonArray
                Nombre(pos[0].jsonPrimitive.double, pos[1].jsonPrimitive.double,
                    n.filterKeys { it != "pos" }.mapValues { it.value.jsonPrimitive.content })
            }
            val lluvias = o.getValue("lluvias").jsonArray.map {
                val l = it.jsonObject; fun s(k: String) = l.getValue(k).jsonPrimitive.content; fun d(k: String) = l.getValue(k).jsonPrimitive.double
                Lluvia(s("id"), s("constelacion"), d("ra"), d("dec"), d("thz"), s("pico"), s("desde"), s("hasta"), d("ancho"))
            }
            val eventos = o.getValue("eventos").jsonArray.map {
                val e = it.jsonObject
                Evento(
                    tipo = e.getValue("tipo").jsonPrimitive.content, clase = e["clase"]?.jsonPrimitive?.content, max = e.t("max")!!,
                    inicio = e.t("inicio"), fin = e.t("fin"), parcialInicio = e.t("parcialInicio"), parcialFin = e.t("parcialFin"),
                    totalInicio = e.t("totalInicio"), totalFin = e.t("totalFin"),
                    oscurecimiento = e["oscurecimiento"]?.jsonPrimitive?.double ?: 0.0,
                    cuerpos = (e["cuerpos"] as? JsonArray)?.map { c -> c.jsonPrimitive.content } ?: emptyList(),
                    sep = e["sep"]?.jsonPrimitive?.double ?: 0.0, km = e["km"]?.jsonPrimitive?.int ?: 0,
                )
            }
            return CieloDatos(estrellas, lineas, nombres, lluvias, eventos)
        }
    }
}

object CieloEventos {
    private const val HORA = 3_600_000L
    private const val DIA = 86_400_000L

    /** Una lluvia activa: días al pico ([delta]) y tasa por hora antes de corregir por altura y luna. */
    class LluviaActiva(val lluvia: CieloDatos.Lluvia, val pico: Long, val delta: Double, val tasa: Double)

    /** Lo que toca contar del cielo ahora: un evento de la tabla o una lluvia cerca de su pico. */
    class Actual(val evento: CieloDatos.Evento?, val lluvia: LluviaActiva?) {
        val tipo: String get() = evento?.tipo ?: "lluvia"
    }

    private fun fecha(anio: Int, md: String): Long {
        val (m, d) = md.split("-").map { it.toInt() }
        return LocalDate.of(anio, m, d).atTime(6, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    fun lluviaDe(datos: CieloDatos, ms: Long): LluviaActiva? {
        val y = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).year
        for (l in datos.lluvias) for (a in listOf(y - 1, y, y + 1)) {
            val pico = fecha(a, l.pico)
            var desde = fecha(a, l.desde); var hasta = fecha(a, l.hasta)
            if (desde > pico) desde = fecha(a - 1, l.desde)
            if (hasta < pico) hasta = fecha(a + 1, l.hasta)
            if (ms >= desde - DIA / 4 && ms <= hasta + DIA * 3 / 4) {
                val delta = (ms - pico).toDouble() / DIA
                return LluviaActiva(l, pico, delta, l.thz * exp(-abs(delta) / l.ancho))
            }
        }
        return null
    }

    /** El evento del cielo que toca contar ahora (uno solo, por prioridad), o null. */
    fun eventoDe(datos: CieloDatos, ms: Long): Actual? {
        datos.eventos.firstOrNull { it.tipo.startsWith("eclipse") && ms >= it.inicio!! - DIA && ms <= it.fin!! }?.let { return Actual(it, null) }
        lluviaDe(datos, ms)?.let { if (abs(it.delta) <= 3) return Actual(null, it) }
        fun cerca(tipo: String, h: Long) = datos.eventos.filter { it.tipo == tipo && abs(ms - it.max) <= h * HORA }.minByOrNull { abs(ms - it.max) }
        return (cerca("conjuncion", 18) ?: cerca("superluna", 18) ?: cerca("estacion", 12))?.let { Actual(it, null) }
    }

    /** Cuánto cubre el eclipse en curso (0..1): la sombra crece hasta el máximo y luego se retira. */
    fun cubrimiento(e: CieloDatos.Evento, ms: Long): Double {
        val m = e.max
        if (e.tipo == "eclipse_sol") {
            val a = e.inicio!!; val b = e.fin!!
            if (ms < a || ms > b) return 0.0
            return e.oscurecimiento * (if (ms <= m) (ms - a).toDouble() / (m - a) else (b - ms).toDouble() / (b - m))
        }
        if (e.totalInicio != null && ms >= e.totalInicio && ms <= e.totalFin!!) return 1.0
        if (e.parcialInicio != null && ms >= e.parcialInicio && ms <= e.parcialFin!!) {
            val pico = if (e.totalInicio != null) (if (ms < m) e.totalInicio else e.totalFin!!) else m
            val borde = if (ms < m) e.parcialInicio else e.parcialFin!!
            return max(0.05, (if (e.totalInicio != null) 1.0 else e.oscurecimiento) * (ms - borde).toDouble() / (pico - borde))
        }
        val a = e.inicio!!; val b = e.fin!!
        if (ms < a || ms > b) return 0.0
        return 0.12 * (if (ms <= m) (ms - a).toDouble() / (m - a) else (b - ms).toDouble() / (b - m))
    }
}
