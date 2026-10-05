package com.rutaalacima.app.data.social

import com.rutaalacima.app.data.remote.SupabaseClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/** Una cordada vista desde mí. */
data class Cordada(
    val id: String, val nombre: String, val reto: String, val eje: String?, val inicio: LocalDate, val dias: Int,
    val codigo: String, val miembros: Int, val misDias: Int, val diasGrupo: Int, val marqueHoy: Boolean,
) {
    val fin: LocalDate get() = inicio.plusDays(dias - 1L)
    fun diaDelReto(hoy: LocalDate = LocalDate.now()): Int = (java.time.temporal.ChronoUnit.DAYS.between(inicio, hoy) + 1).toInt().coerceIn(0, dias)
    fun activa(hoy: LocalDate = LocalDate.now()) = !hoy.isAfter(fin)
}

data class Companero(val userId: String, val nombre: String, val usuario: String, val avatar: String, val dias: Set<LocalDate>, val soyYo: Boolean)
data class NotaCordada(val id: String, val autorId: String, val autor: String, val texto: String, val creada: Long, val mia: Boolean)
data class SesionCoach(val id: String, val inicio: Long, val minutos: Int, val enlace: String, val tema: String)

/**
 * Cordadas (grupos de 3 a 6 con un reto compartido) y la agenda del coach de vida.
 * Todo en Supabase (ver supabase/schema.sql); sin cuenta, [disponible] es false.
 */
class CordadasRepository(private val supa: SupabaseClient) {
    val disponible: Boolean get() = supa.configurado && supa.sesion.value != null
    private val yo: String get() = supa.sesion.value?.userId.orEmpty()

    private fun JsonObject.s(k: String) = (this[k] as? JsonPrimitive)?.contentOrNull.orEmpty()
    private fun JsonObject.i(k: String) = (this[k] as? JsonPrimitive)?.intOrNull ?: 0
    private fun JsonObject.b(k: String) = (this[k] as? JsonPrimitive)?.booleanOrNull ?: false
    private fun filas(e: JsonElement) = (e as? JsonArray)?.map { it.jsonObject }.orEmpty()
    private fun fecha(s: String) = runCatching { Instant.parse(s).toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(s).toInstant().toEpochMilli() }.getOrDefault(0L)

    suspend fun mias(): List<Cordada> = filas(supa.rpc("mis_cordadas")).map { o ->
        Cordada(
            o.s("id"), o.s("nombre"), o.s("reto"), o.s("eje").ifBlank { null },
            runCatching { LocalDate.parse(o.s("inicio")) }.getOrDefault(LocalDate.now()), o.i("dias"), o.s("codigo"),
            o.i("miembros"), o.i("mis_dias"), o.i("dias_grupo"), o.b("marque_hoy"),
        )
    }

    /** Crea la cordada y devuelve su id. */
    suspend fun crear(nombre: String, reto: String, eje: String?, dias: Int): String =
        filas(supa.rpc("crear_cordada", buildJsonObject {
            put("nombre", nombre.trim()); put("reto", reto.trim()); eje?.let { put("eje", it) }; put("dias", dias)
        })).firstOrNull()?.s("id").orEmpty()

    suspend fun unirse(codigo: String): String =
        (supa.rpc("unirse_cordada", buildJsonObject { put("codigo", codigo.trim()) }) as? JsonPrimitive)?.contentOrNull.orEmpty()

    suspend fun salir(id: String) { supa.rpc("salir_cordada", buildJsonObject { put("c", id) }) }

    suspend fun companeros(id: String): List<Companero> = filas(supa.rpc("cordada_detalle", buildJsonObject { put("c", id) })).map { o ->
        Companero(
            o.s("user_id"), o.s("nombre"), o.s("usuario"), o.s("avatar"),
            (o["dias"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.let { d -> runCatching { LocalDate.parse(d) }.getOrNull() } }.toSet(),
            o.b("soy_yo"),
        )
    }

    suspend fun marcar(id: String, dia: LocalDate, si: Boolean) {
        if (si) supa.upsert("cordada_checkins", buildJsonObject { put("cordada_id", id); put("user_id", yo); put("dia", dia.toString()) })
        else supa.delete("cordada_checkins", "cordada_id=eq.$id&user_id=eq.$yo&dia=eq.$dia")
    }

    suspend fun notas(id: String): List<NotaCordada> =
        filas(supa.select("cordada_notas", "select=id,user_id,texto,creada,autor:profiles(nombre,username)&cordada_id=eq.$id&order=creada.desc&limit=100"))
            .map { o ->
                val a = o["autor"] as? JsonObject
                NotaCordada(o.s("id"), o.s("user_id"), a?.s("nombre")?.ifBlank { a.s("username") }.orEmpty(), o.s("texto"), fecha(o.s("creada")), o.s("user_id") == yo)
            }

    suspend fun escribir(id: String, texto: String) {
        supa.insert("cordada_notas", buildJsonObject { put("cordada_id", id); put("user_id", yo); put("texto", texto.trim().take(500)) }, devolver = false)
    }

    suspend fun borrarNota(nota: String) { supa.delete("cordada_notas", "id=eq.$nota") }

    // ------------------------------------------------------------------ Coach de vida: sesiones y notas privadas

    suspend fun sesiones(acomp: String): List<SesionCoach> =
        filas(supa.select("sesiones_coach", "select=id,inicio,minutos,enlace,tema&acomp_id=eq.$acomp&order=inicio.asc&limit=50")).map { o ->
            SesionCoach(o.s("id"), fecha(o.s("inicio")), o.i("minutos").takeIf { it > 0 } ?: 45, o.s("enlace"), o.s("tema"))
        }

    suspend fun agendar(acomp: String, inicio: Instant, minutos: Int, enlace: String, tema: String) {
        supa.insert("sesiones_coach", buildJsonObject {
            put("acomp_id", acomp); put("inicio", inicio.toString()); put("minutos", minutos)
            put("enlace", enlace.trim().takeIf { it.startsWith("https://") }.orEmpty()); put("tema", tema.trim().take(200)); put("creada_por", yo)
        }, devolver = false)
    }

    suspend fun cancelarSesion(id: String) { supa.delete("sesiones_coach", "id=eq.$id") }

    suspend fun notasPrivadas(acomp: String): String =
        filas(supa.select("notas_coach", "select=texto&acomp_id=eq.$acomp")).firstOrNull()?.s("texto").orEmpty()

    suspend fun guardarNotasPrivadas(acomp: String, texto: String) {
        supa.upsert("notas_coach", buildJsonObject { put("acomp_id", acomp); put("texto", texto.take(8000)); put("actualizada", Instant.now().toString()) })
    }
}
