package com.rutaalacima.app.data.social

import com.rutaalacima.app.data.remote.SupabaseClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Moderación de la comunidad (ver supabase/schema.sql): los moderadores revisan lo reportado,
 * lo mantienen visible, lo ocultan o lo eliminan, y pueden suspender cuentas. Todo queda en el historial.
 */
class ModeracionRepository(private val supa: SupabaseClient) {

    enum class Tipo(val clave: String) { POST("post"), COMENTARIO("comentario"), CONVERSACION("conversacion"), PERSONA("persona") }
    enum class Accion(val clave: String) { RESTAURAR("restaurar"), OCULTAR("ocultar"), ELIMINAR("eliminar") }

    data class Autor(val id: String, val usuario: String, val nombre: String, val avatar: String, val suspendidoHasta: String?, val sanciones: Int)
    data class Caso(
        val tipo: Tipo, val objetivo: String, val reportes: Int, val motivos: List<String>, val ultimo: String,
        val autor: Autor?, val texto: String, val imagen: String, val oculto: Boolean, val mensajes: List<String>,
    )
    data class Decision(val accion: String, val tipo: String, val nota: String, val creado: String, val moderador: String, val usuario: String)
    data class Suspension(val hasta: String, val motivo: String, val permanente: Boolean)

    suspend fun esModerador(): Boolean =
        supa.configurado && supa.sesion.value != null &&
            runCatching { (supa.rpc("es_moderador") as? JsonPrimitive)?.booleanOrNull == true }.getOrDefault(false)

    suspend fun pendientes(): List<Caso> = (supa.rpc("moderacion_pendientes") as? JsonArray).orEmpty().mapNotNull { leerCaso(it) }

    suspend fun historial(): List<Decision> = (supa.rpc("moderacion_historial") as? JsonArray).orEmpty().map {
        val o = it.jsonObject
        Decision(o.txt("accion"), o.txt("tipo"), o.txt("nota"), o.txt("creado"), o.txt("moderador"), o.txt("usuario"))
    }

    suspend fun moderar(c: Caso, accion: Accion, nota: String = "") {
        supa.rpc("moderar", buildJsonObject {
            put("p_tipo", c.tipo.clave); put("p_objetivo", c.objetivo); put("p_accion", accion.clave); put("p_nota", nota.take(500))
        })
    }

    /** días > 0 temporal, días < 0 permanente, 0 levanta la suspensión. */
    suspend fun suspender(usuarioId: String, dias: Int, motivo: String) {
        supa.rpc("suspender", buildJsonObject { put("p_usuario", usuarioId); put("p_dias", dias); put("p_motivo", motivo.take(500)) })
    }

    /** La suspensión vigente de quien usa la app, o null. */
    suspend fun miSuspension(): Suspension? {
        if (!supa.configurado || supa.sesion.value == null) return null
        val o = runCatching { supa.rpc("mi_suspension") }.getOrNull() as? JsonObject ?: return null
        return Suspension(o.txt("hasta"), o.txt("motivo"), o["permanente"]?.jsonPrimitive?.booleanOrNull == true)
    }

    companion object {
        private fun JsonObject.txt(k: String) = (this[k] as? JsonPrimitive)?.contentOrNull.orEmpty()

        /** Convierte un caso de moderacion_pendientes(); ignora tipos desconocidos. */
        fun leerCaso(e: JsonElement): Caso? {
            val o = e as? JsonObject ?: return null
            val tipo = Tipo.entries.firstOrNull { it.clave == o.txt("tipo") } ?: return null
            val a = o["autor"] as? JsonObject
            val autor = a?.takeIf { it.txt("id").isNotBlank() }?.let {
                Autor(it.txt("id"), it.txt("usuario"), it.txt("nombre"), it.txt("avatar"),
                    it.txt("suspendido_hasta").ifBlank { null }, (it["sanciones"] as? JsonPrimitive)?.intOrNull ?: 0)
            }
            val motivos = (o["motivos"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { m -> m.isNotBlank() } }
            val mensajes = (o["mensajes"].takeIf { it != null && it !is JsonNull } as? JsonArray).orEmpty()
                .mapNotNull { (it as? JsonObject)?.txt("texto")?.takeIf { t -> t.isNotBlank() } }
            return Caso(
                tipo, o.txt("objetivo"), (o["reportes"] as? JsonPrimitive)?.intOrNull ?: 1, motivos, o.txt("ultimo"),
                autor, o.txt("texto"), o.txt("imagen"), o["oculto"]?.jsonPrimitive?.booleanOrNull == true, mensajes,
            )
        }
    }
}
