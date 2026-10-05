package com.rutaalacima.app.data.social

import com.rutaalacima.app.data.remote.SupabaseClient
import com.rutaalacima.app.domain.model.ResumenCoach
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate

/** Una conversación 1 a 1, vista desde mí. */
data class Conversacion(
    val id: String,
    val otroId: String,
    val otroNombre: String,
    val otroUsuario: String,
    val otroAvatar: String,
    /** pendiente, aceptada o bloqueada */
    val estado: String,
    val laInicieYo: Boolean,
    val laBloqueeYo: Boolean,
    val ultimoTexto: String,
    val ultimoEsMio: Boolean,
    val ultimoEn: Long,
    val noLeidos: Int,
    /** La otra persona es mi coach de vida. */
    val esMiCoach: Boolean,
    /** Yo soy coach de la otra persona. */
    val laAcompano: Boolean,
) {
    /** Solicitud que me llegó y debo aceptar o rechazar. */
    val solicitudParaMi get() = estado == "pendiente" && !laInicieYo
}

data class Mensaje(val id: String, val mio: Boolean, val texto: String, val creadoEn: Long)

/** Ficha de un coach de vida verificado. */
data class CoachFicha(
    val userId: String, val nombre: String, val usuario: String, val avatar: String,
    val bio: String, val especialidad: String, val acompanados: Int,
)

/** Acompañamiento visto desde mí: [rol] = "usuario" (la otra persona es mi coach) o "coach" (yo acompaño). */
data class Acompanamiento(
    val id: String, val rol: String, val otroId: String, val otroNombre: String, val otroUsuario: String,
    val otroAvatar: String, val estado: String, val comparteAvance: Boolean, val especialidad: String,
)

/** Mi ficha de coach (si me postulé). */
data class MiFichaCoach(val bio: String, val especialidad: String, val verificado: Boolean)

/**
 * Mensajes 1 a 1 y coaches de vida (todo en Supabase, ver supabase/schema.sql).
 * Funciona solo con cuenta: sin servidor o sin sesión, [disponible] es false.
 */
class MensajesRepository(private val supa: SupabaseClient) {
    val disponible: Boolean get() = supa.configurado && supa.sesion.value != null
    /** Conversación que la persona tiene abierta ahora (no se avisa de ella). */
    @Volatile var conversacionAbierta: String? = null
    private val yo: String get() = supa.sesion.value?.userId.orEmpty()

    private fun JsonObject.s(k: String) = (this[k] as? JsonPrimitive)?.contentOrNull.orEmpty()
    private fun JsonObject.b(k: String) = (this[k] as? JsonPrimitive)?.booleanOrNull ?: false
    private fun JsonObject.i(k: String) = (this[k] as? JsonPrimitive)?.intOrNull ?: 0
    private fun fecha(s: String) = runCatching { Instant.parse(s).toEpochMilli() }
        .recoverCatching { java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli() }.getOrDefault(0L)
    private fun filas(e: JsonElement) = e.jsonArray.map { it.jsonObject }

    // ------------------------------------------------------------------ Mensajes

    suspend fun conversaciones(): List<Conversacion> = filas(supa.rpc("mis_conversaciones")).map { o ->
        Conversacion(
            id = o.s("id"), otroId = o.s("otro"), otroNombre = o.s("otro_nombre").ifBlank { o.s("otro_usuario") },
            otroUsuario = o.s("otro_usuario"), otroAvatar = o.s("otro_avatar"), estado = o.s("estado"),
            laInicieYo = o.s("iniciada_por") == yo, laBloqueeYo = o.s("bloqueada_por") == yo,
            ultimoTexto = o.s("ultimo_texto"), ultimoEsMio = o.s("ultimo_autor") == yo, ultimoEn = fecha(o.s("ultimo_en")),
            noLeidos = o.i("no_leidos"), esMiCoach = o.b("es_coach"), laAcompano = o.b("es_acompanado"),
        )
    }

    suspend fun mensajes(conv: String): List<Mensaje> =
        filas(supa.select("mensajes", "select=id,autor,texto,creado&conversacion_id=eq.$conv&order=creado.asc&limit=500")).map { o ->
            Mensaje(o.s("id"), o.s("autor") == yo, o.s("texto"), fecha(o.s("creado")))
        }

    suspend fun enviar(conv: String, texto: String) {
        supa.insert("mensajes", buildJsonObject {
            put("conversacion_id", conv); put("autor", yo); put("texto", texto.trim().take(2000))
        }, devolver = false)
    }

    /** Abre (o crea) la conversación con otra persona y devuelve su id. */
    suspend fun abrirCon(otro: String): String =
        supa.rpc("abrir_conversacion", buildJsonObject { put("otro", otro) }).jsonPrimitive.content

    suspend fun responder(conv: String, aceptar: Boolean) {
        supa.rpc("responder_solicitud", buildJsonObject { put("conv", conv); put("aceptar", aceptar) })
    }

    suspend fun bloquear(conv: String, bloquear: Boolean) {
        supa.rpc("bloquear_conversacion", buildJsonObject { put("conv", conv); put("bloquear", bloquear) })
    }

    suspend fun marcarLeidos(conv: String) {
        supa.rpc("marcar_leidos", buildJsonObject { put("conv", conv) })
    }

    suspend fun reportar(otro: String, conv: String?, motivo: String) {
        supa.insert("reportes", buildJsonObject {
            put("quien", yo); put("a_quien", otro); conv?.let { put("conversacion_id", it) }; put("motivo", motivo.take(1000))
        }, devolver = false)
    }

    // ------------------------------------------------------------------ Coaches de vida

    suspend fun directorio(): List<CoachFicha> = filas(supa.rpc("directorio_coaches")).map { o ->
        CoachFicha(o.s("user_id"), o.s("nombre").ifBlank { o.s("usuario") }, o.s("usuario"), o.s("avatar"),
            o.s("bio"), o.s("especialidad"), o.i("acompanados"))
    }

    suspend fun acompanamientos(): List<Acompanamiento> = filas(supa.rpc("mis_acompanamientos")).map { o ->
        Acompanamiento(o.s("id"), o.s("rol"), o.s("otro"), o.s("otro_nombre").ifBlank { o.s("otro_usuario") }, o.s("otro_usuario"),
            o.s("otro_avatar"), o.s("estado"), o.b("comparte_avance"), o.s("especialidad"))
    }

    suspend fun solicitarCoach(coach: String) { supa.rpc("solicitar_coach", buildJsonObject { put("coach", coach) }) }
    suspend fun responderAcompanamiento(id: String, aceptar: Boolean) {
        supa.rpc("responder_acompanamiento", buildJsonObject { put("acomp", id); put("aceptar", aceptar) })
    }
    suspend fun terminarAcompanamiento(id: String) { supa.rpc("terminar_acompanamiento", buildJsonObject { put("acomp", id) }) }
    suspend fun compartirAvance(id: String, si: Boolean) {
        supa.rpc("compartir_avance", buildJsonObject { put("acomp", id); put("si", si) })
    }

    suspend fun miFicha(): MiFichaCoach? =
        filas(supa.select("coaches", "select=bio,especialidad,verificado&user_id=eq.$yo")).firstOrNull()?.let {
            MiFichaCoach(it.s("bio"), it.s("especialidad"), it.b("verificado"))
        }

    suspend fun postularme(bio: String, especialidad: String) {
        supa.rpc("postularme_coach", buildJsonObject { put("bio", bio); put("especialidad", especialidad) })
    }

    /** Avance de la persona que acompaño (solo lo que ella comparte). */
    suspend fun avanceDe(usuario: String): ResumenCoach {
        val docs = filas(supa.select("ruta_datos", "select=tipo,clave,datos&user_id=eq.$usuario&limit=1000"))
            .associate { "${it.s("tipo")}/${it.s("clave")}" to it["datos"]!!.jsonObject }
        return ResumenCoach.desde(docs, LocalDate.now())
    }
}
