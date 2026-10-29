package com.rutaalacima.app.data.social

import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.domain.model.Constancia
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.domain.model.MetodoCima
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate

/** El camino que hay detrás de una publicación: días seguidos con hábitos y pasos ganados de la Brújula. */
data class Camino(val racha: Int, val ganados: Int) { val vacio get() = racha == 0 && ganados == 0 }

/** Un compromiso con fecha: abierto hasta [hasta]; al vencer, el autor dice si lo logró o lo replantea. */
data class Compromiso(val hasta: String, val estado: String = ABIERTO) {
    companion object { const val ABIERTO = "ABIERTO"; const val LOGRADO = "LOGRADO"; const val REPLANTEO = "REPLANTEO" }
}

/** Lo nuevo de una publicación (columnas camino, compromiso y sello). */
data class ExtrasPost(val camino: Camino? = null, val compromiso: Compromiso? = null, val sello: String? = null)

/**
 * Comunidad viva (igual que web/js/45-comunidad-viva.js): lo que hace distinta a SuRoute.
 * Aquí no se premia la atención sino el avance real.
 */
object ComunidadViva {
    const val PASOS_BRUJULA = 64

    /** El camino de hoy de esta persona: racha de días con hábitos y pasos ganados de su Brújula general. */
    suspend fun caminoActual(c: AppContainer): Camino = runCatching {
        val hoy = LocalDate.now()
        val dias = c.checklist.desde(hoy.minusDays(800)).first().filterValues { it.isNotEmpty() }.keys
        val racha = Constancia.racha(dias, hoy)
        val r = c.respuestas.cargar(Mandala.WORKBOOK)
        val generales = c.db.visionDao().todas().filter { Mandala.anioDeOrigen(it.origen) == null }
        val camps = Mandala.repartir(generales, { it.origen }, { it.eje }).second.map { it?.id }
        val ganados = MetodoCima.pasosDe(camps, r).count { it.cumplido }
        Camino(racha, ganados)
    }.getOrDefault(Camino(0, 0))

    /** El eje de tu campamento más flojo (el que menos pasos ha ganado): "Para ti" muestra primero lo de ese tramo. */
    suspend fun tramoMio(c: AppContainer): String? = runCatching {
        val r = c.respuestas.cargar(Mandala.WORKBOOK)
        val generales = c.db.visionDao().todas().filter { Mandala.anioDeOrigen(it.origen) == null }
        val camps = Mandala.repartir(generales, { it.origen }, { it.eje }).second
        camps.mapIndexedNotNull { i, cas -> cas ?: return@mapIndexedNotNull null
            val cod = Mandala.CAMPAMENTOS_FIJOS[i]
            if (cod == "CON" || cod == "CAM") return@mapIndexedNotNull null
            cod to (0 until Mandala.PASOS).count { MetodoCima.leer(cas.id, it, r).cumplido }
        }.minByOrNull { it.second }?.first
    }.getOrNull()

    /** Sellada para quien no es el autor, hasta la fecha en que se abre. */
    fun estaSellada(p: Post): Boolean = !p.propio && p.sello != null && LocalDate.now().toString() < p.sello

    /** Bono de "Para ti": tu tramo y el camino real detrás (igual que la web). */
    fun bono(p: Post, tramo: String?): Double {
        val c = p.camino
        return (if (tramo != null && p.eje == tramo) 1.5 else 1.0) *
            (if (c != null) 1 + minOf(0.5, kotlin.math.ln(1.0 + c.racha + c.ganados) / kotlin.math.ln(2.0) / 12) else 1.0) *
            (if (p.compromiso?.estado == Compromiso.LOGRADO) 1.2 else 1.0)
    }

    // ---------- JSON ----------
    private fun obj(e: JsonElement?): JsonObject? = when (e) {
        is JsonObject -> e
        is JsonPrimitive -> e.contentOrNull?.takeIf { it.startsWith("{") }?.let { runCatching { kotlinx.serialization.json.Json.parseToJsonElement(it).jsonObject }.getOrNull() }
        else -> null
    }
    fun caminoDe(e: JsonElement?): Camino? = obj(e)?.let { o ->
        Camino(o["racha"]?.jsonPrimitive?.intOrNull ?: 0, o["ganados"]?.jsonPrimitive?.intOrNull ?: 0).takeUnless { it.vacio }
    }
    fun compromisoDe(e: JsonElement?): Compromiso? = obj(e)?.let { o ->
        val h = o["hasta"]?.jsonPrimitive?.contentOrNull ?: return@let null
        Compromiso(h, o["estado"]?.jsonPrimitive?.contentOrNull ?: Compromiso.ABIERTO)
    }
    fun selloDe(e: JsonElement?): String? = obj(e)?.get("abre")?.jsonPrimitive?.contentOrNull
    fun json(c: Camino) = buildJsonObject { put("racha", c.racha); put("ganados", c.ganados) }
    fun json(k: Compromiso) = buildJsonObject { put("hasta", k.hasta); put("estado", k.estado) }
    fun jsonSello(abre: String) = buildJsonObject { put("abre", abre) }

    /** Lo que se guarda en el teléfono para una publicación propia. */
    fun aTexto(x: ExtrasPost): String = buildJsonObject {
        x.camino?.let { put("camino", json(it)) }; x.compromiso?.let { put("compromiso", json(it)) }; x.sello?.let { put("sello", jsonSello(it)) }
    }.toString()
    fun deTexto(s: String?): ExtrasPost = if (s.isNullOrBlank()) ExtrasPost() else runCatching {
        val o = kotlinx.serialization.json.Json.parseToJsonElement(s).jsonObject
        ExtrasPost(caminoDe(o["camino"]), compromisoDe(o["compromiso"]), selloDe(o["sello"]))
    }.getOrDefault(ExtrasPost())

    /** Cordada: quien lleva 3 días seguidos sin marcar (sin contar hoy) necesita una cuerda. */
    fun necesitaCuerda(dias: Set<LocalDate>, inicio: LocalDate, hoy: LocalDate = LocalDate.now()): Boolean {
        if (hoy in dias) return false
        val ultimos = (1..3).map { hoy.minusDays(it.toLong()) }
        return ultimos.all { !it.isBefore(inicio) } && ultimos.none { it in dias }
    }
}
