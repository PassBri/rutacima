package com.rutaalacima.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.IsoFields

/**
 * Bienestar (igual que la web, web/js/31-bienestar.js): inteligencia emocional y vida saludable.
 * Cada día se guarda en "bien#aaaa-mm-dd" (workbook "bienestar"). Es privado.
 * · clima: sol (energía alta, agradable), tormenta (alta, desagradable), niebla (baja, desagradable) y
 *   valle (baja, agradable): el medidor del ánimo de Yale (RULER). emo: la emoción; int: 1 a 5.
 * · Vida saludable con las metas de la OMS: sueño (h), mov (min), fyv (porciones), aire (min), pant
 *   (sin pantallas antes de dormir). El agua viene de la agenda del día (cantimploras).
 */
object Bienestar {
    const val WB = "bienestar"
    fun clave(f: LocalDate) = "bien#$f"
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }

    @Serializable
    data class Dia(val clima: String? = null, val emo: String? = null, val int: Int? = null, val causas: List<String> = emptyList(),
                   val nota: String? = null, val herr: List<String> = emptyList(), val sueno: Double? = null, val mov: Int? = null,
                   val fyv: Int? = null, val aire: Int? = null, val pant: Boolean? = null, val t: Long? = null)

    fun leer(v: String?): Dia = if (v.isNullOrBlank()) Dia() else runCatching { json.decodeFromString(Dia.serializer(), v) }.getOrDefault(Dia())
    fun escribir(d: Dia): String = if (d == Dia()) "" else json.encodeToString(Dia.serializer(), d)

    /** Los cuatro climas con sus emociones (las claves son las mismas de la web; el nombre se traduce en la app). */
    val CLIMAS = listOf("sol", "tormenta", "niebla", "valle")
    val EMOCIONES = mapOf(
        "sol" to listOf("Alegre", "Entusiasmado", "Motivado", "Orgulloso", "Esperanzado", "Inspirado"),
        "tormenta" to listOf("Enojado", "Ansioso", "Frustrado", "Estresado", "Preocupado", "Irritado"),
        "niebla" to listOf("Triste", "Cansado", "Desanimado", "Solo", "Aburrido", "Agotado"),
        "valle" to listOf("Tranquilo", "Agradecido", "Relajado", "Satisfecho", "En paz", "Seguro"),
    )
    val CAUSAS = listOf("Trabajo", "Familia", "Pareja", "Amistades", "Salud", "Dinero", "Estudio", "Sueño", "Yo mismo", "Otra")
    val HERRAMIENTAS = listOf("caja", "stop", "mirada", "mover", "hablar")
    val PILARES = listOf("sueno", "mov", "fyv", "agua", "aire", "pant")

    /** Cuánto se cumplió cada pilar en un día (0 a 1; null si no se registró). */
    fun pilares(d: Dia, agua: Int): Map<String, Double?> = mapOf(
        "sueno" to d.sueno?.let { minOf(1.0, it / 7) * (if (it > 10) 0.8 else 1.0) },
        "mov" to d.mov?.let { minOf(1.0, it / 30.0) },
        "fyv" to d.fyv?.let { minOf(1.0, it / 5.0) },
        "agua" to if (agua > 0 || d.sueno != null || d.mov != null) minOf(1.0, agua / 8.0) else null,
        "aire" to d.aire?.let { minOf(1.0, it / 20.0) },
        "pant" to d.pant?.let { if (it) 1.0 else 0.0 },
    )

    data class Indice(val pilares: Map<String, Double?>, val indice: Int?, val racha: Int, val movSemana: Int)

    /** Índice de vida saludable de los últimos 7 días (0 a 100) y racha de días con 4 de 6 pilares cumplidos. */
    fun indice(dias: Map<LocalDate, Dia>, agua: Map<LocalDate, Int>, hoy: LocalDate): Indice {
        val semana = (0 until 7).map { hoy.minusDays(it.toLong()) }
        val pil = PILARES.associateWith { k ->
            val v = semana.mapNotNull { f -> pilares(dias[f] ?: Dia(), agua[f] ?: 0)[k] }
            if (v.isEmpty()) null else v.sum() / 7
        }.toMutableMap()
        val movSem = semana.sumOf { dias[it]?.mov ?: 0 }
        if (movSem > 0 || semana.any { dias[it]?.mov != null }) pil["mov"] = minOf(1.0, movSem / 150.0)
        val vals = pil.values.filterNotNull()
        val indice = if (vals.isEmpty()) null else Math.round(vals.sum() / PILARES.size * 100).toInt()
        var racha = 0
        for (i in 0 until 365) {
            val f = hoy.minusDays(i.toLong()); val ok = pilares(dias[f] ?: Dia(), agua[f] ?: 0).values.count { it != null && it >= 0.99 }
            if (ok >= 4) racha++ else if (i > 0) break
        }
        return Indice(pil, indice, racha, movSem)
    }

    data class Patrones(val dias: Int, val cuenta: Map<String, Int>, val emociones: List<Pair<String, Int>>, val causas: List<Pair<String, Int>>, val sueno: Pair<Int, Int>?)

    fun patrones(dias: Map<LocalDate, Dia>, hoy: LocalDate, n: Int = 30): Patrones {
        val l = (0 until n).map { hoy.minusDays(it.toLong()) }.mapNotNull { f -> dias[f]?.takeIf { it.clima != null } }
        val cuenta = CLIMAS.associateWith { c -> l.count { it.clima == c } }
        val emo = l.mapNotNull { it.emo }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
        val causas = l.filter { it.clima == "tormenta" || it.clima == "niebla" }.flatMap { it.causas }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3).map { it.key to it.value }
        val conSueno = l.filter { it.sueno != null }; val bien = conSueno.filter { it.sueno!! >= 7 }; val mal = conSueno - bien.toSet()
        fun agr(x: List<Dia>) = x.count { it.clima == "sol" || it.clima == "valle" }
        val sueno = if (bien.size >= 3 && mal.size >= 3) Math.round(agr(bien) * 100.0 / bien.size).toInt() to Math.round(agr(mal) * 100.0 / mal.size).toInt() else null
        return Patrones(l.size, cuenta, emo, causas, sueno)
    }

    /** Puntaje de un día (0 a 1) y cuántos de los 6 hábitos se cumplieron; null si no hay nada anotado. */
    data class Puntaje(val puntaje: Double, val cumplidos: Int, val pilares: Map<String, Double?>)
    fun puntaje(d: Dia, agua: Int): Puntaje? {
        val p = pilares(d, agua); val v = p.values.filterNotNull()
        if (v.isEmpty()) return null
        return Puntaje(v.sum() / PILARES.size, v.count { it >= 0.99 }, p)
    }

    /** El acumulado de bienestar de unas fechas (igual que statsBien de la web). */
    data class Acumulado(
        val dias: Int, val registrados: Int, val conClima: Int, val clima: Map<String, Int>, val dominante: String?,
        val emociones: List<Pair<String, Int>>, val causas: List<Pair<String, Int>>, val herramientas: Int,
        val suenoProm: Double?, val mov: Int, val fyvProm: Double?, val aguaProm: Double?, val aire: Int, val pant: Int,
        val indice: Int?, val conDatos: Int, val metas: Map<String, Int>,
    )
    fun acumulado(fechas: List<LocalDate>, dias: Map<LocalDate, Dia>, agua: Map<LocalDate, Int>): Acumulado {
        val clima = CLIMAS.associateWith { 0 }.toMutableMap(); val emo = HashMap<String, Int>(); val causas = HashMap<String, Int>()
        val metas = PILARES.associateWith { 0 }.toMutableMap()
        var herr = 0; var mov = 0; var aire = 0; var pant = 0; var registrados = 0
        val sueno = ArrayList<Double>(); val fyv = ArrayList<Int>(); val aguas = ArrayList<Int>(); val puntajes = ArrayList<Double>()
        fechas.forEach { f ->
            val d = dias[f] ?: Dia(); val a = agua[f] ?: 0; val p = puntaje(d, a)
            d.clima?.let { clima[it] = (clima[it] ?: 0) + 1 }
            d.emo?.let { emo[it] = (emo[it] ?: 0) + 1 }
            if (d.clima == "tormenta" || d.clima == "niebla") d.causas.forEach { causas[it] = (causas[it] ?: 0) + 1 }
            herr += d.herr.size
            d.sueno?.let(sueno::add); mov += d.mov ?: 0; d.fyv?.let(fyv::add); if (a > 0) aguas += a
            aire += d.aire ?: 0; if (d.pant == true) pant++
            if (p != null) { puntajes += p.puntaje; p.pilares.forEach { (k, v) -> if (v != null && v >= 0.99) metas[k] = (metas[k] ?: 0) + 1 } }
            if (d.clima != null || p != null) registrados++
        }
        val conClima = clima.values.sum()
        return Acumulado(
            fechas.size, registrados, conClima, clima, if (conClima > 0) clima.maxByOrNull { it.value }?.key else null,
            emo.entries.sortedByDescending { it.value }.take(4).map { it.key to it.value },
            causas.entries.sortedByDescending { it.value }.take(3).map { it.key to it.value }, herr,
            sueno.takeIf { it.isNotEmpty() }?.average(), mov, fyv.takeIf { it.isNotEmpty() }?.average(), aguas.takeIf { it.isNotEmpty() }?.average(),
            aire, pant, if (puntajes.isEmpty()) null else Math.round(puntajes.average() * 100).toInt(), puntajes.size, metas,
        )
    }

    /** La práctica de la semana (0 a 4): rota los cinco componentes de Goleman. */
    fun practica(hoy: LocalDate) = hoy.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) % 5
}
