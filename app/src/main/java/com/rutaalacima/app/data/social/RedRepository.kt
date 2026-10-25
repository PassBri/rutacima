package com.rutaalacima.app.data.social

import android.content.Context
import com.rutaalacima.app.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.time.Instant

/** Un aviso de la comunidad: alguien impulsó, comentó, respondió, me siguió o me mencionó. */
data class Aviso(
    val id: Long, val tipo: String, val actorId: String, val actorNombre: String, val actorUsuario: String, val actorAvatar: String,
    val postId: String?, val foto: String, val texto: String, val leida: Boolean, val creadoEn: Long,
)

/** Una persona de la comunidad. */
data class Persona(
    val id: String, val nombre: String, val usuario: String, val avatar: String = "",
    val bio: String = "", val cumbre: String = "",
)

/** Un reto del mes, abierto a toda la comunidad, con mi avance. */
data class Reto(
    val id: String, val titulo: String, val descripcion: String, val eje: String?, val inicio: java.time.LocalDate, val fin: java.time.LocalDate,
    val participantes: Int, val marcasHoy: Int, val yoParticipo: Boolean, val misDias: Int, val hoyMarcado: Boolean,
) { val dias: Int get() = (java.time.temporal.ChronoUnit.DAYS.between(inicio, fin) + 1).toInt().coerceAtLeast(1) }

/** Una postulación de coach de vida por revisar (moderación). */
data class CoachPendiente(val id: String, val nombre: String, val usuario: String, val bio: String, val especialidad: String)

/** Perfil público: quién es, cuántos la siguen y lo que comparte. */
data class PerfilPublico(
    val persona: Persona, val seguidores: Int, val siguiendo: Int, val publicaciones: List<Post>,
    val loSigo: Boolean, val meSigue: Boolean, val propio: Boolean,
)

/**
 * La parte de red social de SuRoute que no está en [SocialRepository]: avisos, buscar personas,
 * perfiles públicos y sugerencias de a quién seguir (ver supabase/2026-10-26-red-social.sql).
 */
class RedRepository(private val context: Context, private val supa: SupabaseClient, private val social: SocialRepository) {
    val disponible: Boolean get() = supa.configurado && supa.sesion.value != null
    private val yo: String get() = supa.sesion.value?.userId.orEmpty()
    private val prefs = context.getSharedPreferences("red_social", Context.MODE_PRIVATE)

    private val _noLeidos = MutableStateFlow(0)
    /** Avisos sin leer (el número de la campana). */
    val noLeidos: StateFlow<Int> = _noLeidos.asStateFlow()

    private fun JsonObject.s(k: String) = (this[k] as? JsonPrimitive)?.contentOrNull.orEmpty()
    private fun JsonObject.o(k: String) = this[k] as? JsonObject
    private fun fecha(s: String) = runCatching { Instant.parse(s).toEpochMilli() }
        .recoverCatching { java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli() }.getOrDefault(0L)
    private fun filas(e: JsonElement) = e.jsonArray.map { it.jsonObject }
    private fun cod(t: String) = URLEncoder.encode(t, "UTF-8")

    // ------------------------------------------------------------------ Avisos

    suspend fun avisos(): List<Aviso> = withContext(Dispatchers.IO) {
        val lista = if (!disponible) demoAvisos() else runCatching {
            filas(supa.select("notificaciones",
                "select=id,tipo,texto,leida,created_at,post_id,actor:profiles!notificaciones_actor_id_fkey(id,username,nombre,avatar_url),post:posts(image_url)" +
                    "&order=created_at.desc&limit=80")).map { n ->
                val a = n.o("actor")
                Aviso(n.s("id").toLongOrNull() ?: 0, n.s("tipo"), a?.s("id").orEmpty(), a?.s("nombre").orEmpty().ifBlank { a?.s("username").orEmpty() },
                    a?.s("username").orEmpty(), a?.s("avatar_url").orEmpty(), n.s("post_id").ifBlank { null }, n.o("post")?.s("image_url").orEmpty(),
                    n.s("texto"), (n["leida"] as? JsonPrimitive)?.booleanOrNull ?: false, fecha(n.s("created_at")))
            }
        }.getOrDefault(emptyList())
        _noLeidos.value = lista.count { !it.leida }
        lista
    }

    /** Solo cuenta los nuevos (para la campana y el aviso del teléfono), sin traer todo. */
    suspend fun contarNoLeidos(): Int = withContext(Dispatchers.IO) {
        if (!disponible) return@withContext _noLeidos.value
        runCatching { filas(supa.select("notificaciones", "select=id&leida=eq.false&limit=100")).size }.getOrDefault(0).also { _noLeidos.value = it }
    }

    suspend fun marcarLeidos() = withContext(Dispatchers.IO) {
        _noLeidos.value = 0
        if (disponible) runCatching { supa.actualizar("notificaciones", "leida=eq.false", buildJsonObject { put("leida", true) }) }
        else prefs.edit().putBoolean("demo_leidos", true).apply()
    }

    /** Qué avisos recibir: tipo -> encendido. Lo que no aparece está encendido. */
    suspend fun preferencias(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        if (!disponible) return@withContext emptyMap()
        runCatching {
            val o = filas(supa.select("profiles", "select=avisos&id=eq.$yo")).firstOrNull()?.o("avisos") ?: return@runCatching emptyMap()
            o.mapValues { (it.value as? JsonPrimitive)?.booleanOrNull ?: true }
        }.getOrDefault(emptyMap())
    }

    suspend fun guardarPreferencias(p: Map<String, Boolean>) = withContext(Dispatchers.IO) {
        if (!disponible) return@withContext
        supa.rpc("mis_avisos", buildJsonObject { put("p_avisos", buildJsonObject { p.forEach { (k, v) -> put(k, v) } }) })
    }

    private fun demoAvisos(): List<Aviso> {
        val ahora = System.currentTimeMillis(); val leidos = prefs.getBoolean("demo_leidos", false)
        return listOf(
            Aviso(1, "IMPULSO", "diana", "Diana", "diana", "", "demo-1", "", "", leidos, ahora - 600_000),
            Aviso(2, "IMPULSO", "julian", "Julián", "julian", "", "demo-1", "", "", leidos, ahora - 900_000),
            Aviso(3, "COMENTARIO", "laura", "Laura", "laura", "", "demo-1", "", "¡Qué inspiración! ¿Cuántos días te tomó?", leidos, ahora - 3_600_000),
            Aviso(4, "SEGUIDOR", "andres.sube", "Andrés M.", "andres.sube", "", null, "", "", true, ahora - 86_400_000),
        )
    }

    // ------------------------------------------------------------------ Personas

    private fun persona(o: JsonObject) = Persona(o.s("id"), o.s("nombre").ifBlank { o.s("username") }, o.s("username"), o.s("avatar_url"), o.s("bio"), o.s("cumbre"))

    /** Busca por nombre o @usuario. */
    suspend fun buscar(texto: String): List<Persona> = withContext(Dispatchers.IO) {
        val t = texto.trim().removePrefix("@").replace(Regex("[,()*]"), " ").trim()
        if (t.length < 2) return@withContext emptyList()
        if (!disponible) return@withContext demoPersonas().filter { it.nombre.contains(t, true) || it.usuario.contains(t, true) }
        runCatching {
            filas(supa.select("profiles", "select=id,username,nombre,avatar_url,bio,cumbre&or=(username.ilike.*${cod(t)}*,nombre.ilike.*${cod(t)}*)&limit=25"))
                .map(::persona).filter { it.id != yo }
        }.getOrDefault(emptyList())
    }

    /** A quiénes sigo. */
    suspend fun sigo(): Set<String> = withContext(Dispatchers.IO) {
        if (!disponible) return@withContext prefs.getStringSet("demo_sigo", emptySet()).orEmpty()
        runCatching { filas(supa.select("follows", "select=followed_id&follower_id=eq.$yo")).map { it.s("followed_id") }.toSet() }.getOrDefault(emptySet())
    }

    suspend fun seguir(id: String, si: Boolean) {
        if (!disponible) { val s = prefs.getStringSet("demo_sigo", emptySet()).orEmpty(); prefs.edit().putStringSet("demo_sigo", if (si) s + id else s - id).apply(); return }
        social.seguir(id, si)
    }

    /**
     * A quién seguir: personas que publicaron hace poco en mis ejes (y si no alcanza, las más activas),
     * sin las que ya sigo.
     */
    suspend fun sugeridas(ejes: Collection<String> = emptyList(), cuantas: Int = 8): List<Persona> = withContext(Dispatchers.IO) {
        if (!disponible) return@withContext demoPersonas().take(cuantas)
        runCatching {
            val ya = sigo() + yo
            val filtro = if (ejes.isEmpty()) "" else "&eje=in.(${ejes.joinToString(",")})"
            val recientes = filas(supa.select("posts", "select=user_id,eje,autor:profiles!posts_user_id_fkey(id,username,nombre,avatar_url,bio,cumbre)" +
                "&visibilidad=eq.PUBLICA&order=created_at.desc&limit=200$filtro"))
            val cuenta = recientes.groupingBy { it.s("user_id") }.eachCount()
            recientes.mapNotNull { it.o("autor")?.let(::persona) }.distinctBy { it.id }.filter { it.id !in ya }
                .sortedByDescending { cuenta[it.id] ?: 0 }.take(cuantas)
        }.getOrDefault(emptyList())
    }

    suspend fun perfil(id: String): PerfilPublico? = withContext(Dispatchers.IO) {
        if (!disponible) {
            val p = demoPersonas().firstOrNull { it.id == id } ?: return@withContext null
            return@withContext PerfilPublico(p, 24, 31, social.deAutor(id), id in sigo(), false, false)
        }
        runCatching {
            val p = filas(supa.select("profiles", "select=id,username,nombre,avatar_url,bio,cumbre&id=eq.$id")).firstOrNull()?.let(::persona) ?: return@runCatching null
            suspend fun cuenta(filtro: String) = runCatching { filas(supa.select("follows", "select=follower_id&$filtro&limit=1000")).size }.getOrDefault(0)
            val loSigo = filas(supa.select("follows", "select=followed_id&follower_id=eq.$yo&followed_id=eq.$id")).isNotEmpty()
            val meSigue = filas(supa.select("follows", "select=followed_id&follower_id=eq.$id&followed_id=eq.$yo")).isNotEmpty()
            PerfilPublico(p, cuenta("followed_id=eq.$id"), cuenta("follower_id=eq.$id"), social.deAutor(id), loSigo, meSigue, id == yo)
        }.getOrNull()
    }

    /**
     * "Para ti": lo reciente con apoyo, de quienes sigo y de mis ejes, con variedad de personas (igual que la web).
     * Puntaje = apoyo (impulsos y comentarios) que se apaga con las horas × bonos (sigo, mi eje, foto).
     */
    suspend fun ordenarParaTi(posts: List<Post>, ejes: Set<String>): List<Post> {
        val sigo = sigo(); val ahora = System.currentTimeMillis()
        fun log2(x: Double) = kotlin.math.ln(x) / kotlin.math.ln(2.0)
        fun puntaje(p: Post): Double {
            val h = ((ahora - p.creadoEn) / 3_600_000.0).coerceAtLeast(0.0)
            return (1 + log2(1.0 + p.impulsos) * 1.5 + log2(1.0 + p.comentarios) * 2) / Math.pow(h + 2, 0.75) *
                (if (p.autorId in sigo) 1.8 else 1.0) * (if (p.eje != null && p.eje in ejes) 1.4 else 1.0) *
                (if (p.foto.isNotBlank()) 1.15 else 1.0) * (if (p.propio) 0.6 else 1.0)
        }
        val resto = posts.sortedByDescending(::puntaje).toMutableList(); val r = mutableListOf<Post>()
        while (resto.isNotEmpty()) {
            val i = resto.indexOfFirst { it.autorId != r.lastOrNull()?.autorId }.coerceAtLeast(0)
            r += resto.removeAt(i)
        }
        return r
    }

    /** El id de una persona por su @usuario (para abrir su perfil desde una mención). */
    suspend fun idDeUsuario(usuario: String): String? = withContext(Dispatchers.IO) {
        val u = usuario.removePrefix("@").lowercase()
        if (!disponible) return@withContext demoPersonas().firstOrNull { it.usuario == u }?.id
        runCatching { filas(supa.select("profiles", "select=id&username=eq.${cod(u)}")).firstOrNull()?.s("id") }.getOrNull()
    }

    /** Invitación: quedamos siguiéndonos con quien me invitó. */
    suspend fun aceptarInvitacion(usuario: String): Boolean = withContext(Dispatchers.IO) {
        if (!disponible) return@withContext false
        runCatching { (supa.rpc("aceptar_invitacion", buildJsonObject { put("p_usuario", usuario.removePrefix("@").lowercase().trim()) }) as? JsonPrimitive)?.booleanOrNull == true }.getOrDefault(false)
    }

    suspend fun etiqueta(tag: String): List<Post> = social.publicasCon("texto=ilike.*${cod("#$tag")}*") { it.texto.contains("#$tag", true) }
    suspend fun deEje(eje: String): List<Post> = social.publicasCon("eje=eq.$eje") { it.eje == eje }

    // ------------------------------------------------------------------ Retos de la comunidad

    private var retoDemoUnido = false
    private var retoDemoHoy = false
    suspend fun retos(): List<Reto> = withContext(Dispatchers.IO) {
        if (!disponible) {
            val hoy = java.time.LocalDate.now()
            return@withContext listOf(Reto("reto-demo", "21 días madrugando", "Levántate a la hora que elegiste y marca el día. Sin culpa si fallas uno: vuelve al siguiente.",
                "VOL", hoy.minusDays(5), hoy.plusDays(15), 128, 41, retoDemoUnido, if (retoDemoUnido) 2 + (if (retoDemoHoy) 1 else 0) else 0, retoDemoHoy))
        }
        runCatching {
            filas(supa.rpc("retos_activos")).map { o ->
                fun i(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0
                fun b(k: String) = (o[k] as? JsonPrimitive)?.booleanOrNull ?: false
                Reto(o.s("id"), o.s("titulo"), o.s("descripcion"), o.s("eje").ifBlank { null }, java.time.LocalDate.parse(o.s("inicio")), java.time.LocalDate.parse(o.s("fin")),
                    i("participantes"), i("marcas_hoy"), b("yo_participo"), i("mis_dias"), b("hoy_marcado"))
            }
        }.getOrDefault(emptyList())
    }
    suspend fun unirseReto(id: String) = withContext(Dispatchers.IO) {
        if (!disponible) { retoDemoUnido = true; return@withContext }
        runCatching { supa.insert("reto_participantes", buildJsonObject { put("reto_id", id); put("user_id", yo) }, devolver = false) }
    }
    suspend fun marcarReto(id: String) = withContext(Dispatchers.IO) {
        if (!disponible) { retoDemoHoy = true; return@withContext }
        runCatching { supa.insert("reto_marcas", buildJsonObject { put("reto_id", id); put("user_id", yo); put("dia", java.time.LocalDate.now().toString()) }, devolver = false) }
    }
    suspend fun salirReto(id: String) = withContext(Dispatchers.IO) {
        if (!disponible) { retoDemoUnido = false; retoDemoHoy = false; return@withContext }
        runCatching { supa.delete("reto_participantes", "reto_id=eq.$id&user_id=eq.$yo") }
    }

    // ------------------------------------------------------------------ Coaches verificados

    private var verificadosCache: Set<String>? = null
    /** Quiénes son coaches de vida verificados (para su insignia). */
    suspend fun verificados(): Set<String> = withContext(Dispatchers.IO) {
        verificadosCache ?: (if (!disponible) emptySet() else runCatching {
            filas(supa.select("coaches", "select=user_id&verificado=eq.true&activo=eq.true")).map { it.s("user_id") }.toSet()
        }.getOrDefault(emptySet())).also { verificadosCache = it }
    }
    suspend fun coachesPorVerificar(): List<CoachPendiente> = withContext(Dispatchers.IO) {
        runCatching { filas(supa.rpc("coaches_por_verificar")).map { CoachPendiente(it.s("user_id"), it.s("nombre").ifBlank { it.s("username") }, it.s("username"), it.s("bio"), it.s("especialidad")) } }
            .getOrDefault(emptyList())
    }
    suspend fun verificarCoach(id: String, si: Boolean) = withContext(Dispatchers.IO) {
        supa.rpc("verificar_coach", buildJsonObject { put("p_user", id); put("p_si", si) }); verificadosCache = null
    }

    // ------------------------------------------------------------------ Privacidad y seguridad

    /** (quién puede comentar, soy menor de edad). */
    suspend fun privacidad(): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        if (!disponible) return@withContext prefs.getString("demo_regla", "TODOS").orEmpty() to prefs.getBoolean("demo_menor", false)
        runCatching {
            val o = filas(supa.select("profiles", "select=comentarios_de,menor&id=eq.$yo")).first()
            o.s("comentarios_de").ifBlank { "TODOS" } to ((o["menor"] as? JsonPrimitive)?.booleanOrNull ?: false)
        }.getOrDefault("TODOS" to false)
    }
    suspend fun reglaComentarios(r: String) = withContext(Dispatchers.IO) {
        if (!disponible) { prefs.edit().putString("demo_regla", r).apply(); return@withContext }
        supa.rpc("mis_comentarios_de", buildJsonObject { put("p_regla", r) })
    }
    suspend fun soyMenor(si: Boolean) = withContext(Dispatchers.IO) {
        if (!disponible) { prefs.edit().putBoolean("demo_menor", si).apply(); return@withContext }
        supa.rpc("mi_edad_menor", buildJsonObject { put("p_si", si) })
    }
    suspend fun silenciados(): List<Persona> = withContext(Dispatchers.IO) {
        val ids = social.silenciadosLocales()
        if (!disponible) return@withContext demoPersonas().filter { it.id in ids }
        runCatching { filas(supa.select("silencios", "select=a_quien,persona:profiles!silencios_a_quien_fkey(id,username,nombre,avatar_url)&quien=eq.$yo"))
            .mapNotNull { it.o("persona")?.let(::persona) } }.getOrDefault(emptyList())
    }

    private fun demoPersonas() = listOf(
        Persona("mateo.valor", "Mateo G.", "mateo.valor", cumbre = "Facturar el doble con mi propio negocio"),
        Persona("sofi.voz", "Sofía L.", "sofi.voz", cumbre = "Hablar en público sin miedo"),
        Persona("camilo.evo", "Camilo T.", "camilo.evo", cumbre = "Correr una maratón completa"),
        Persona("vale.cumbre", "Valentina R.", "vale.cumbre", cumbre = "Ser diseñadora independiente"),
        Persona("luciap", "Lucía P.", "luciap", cumbre = "Levantarme a las 5:00 un año entero"),
        Persona("andres.sube", "Andrés M.", "andres.sube", cumbre = "Una casa con jardín para mi familia"),
    )
}


/** Enlaces públicos con vista previa (función compartir de Supabase). Vacío sin servidor o si no es pública. */
object EnlacesRed {
    private val base: String get() = com.rutaalacima.app.BuildConfig.SUPABASE_URL.trimEnd('/').let { if (it.startsWith("https://")) "$it/functions/v1/compartir" else "" }
    fun publicacion(p: Post): String = if (base.isNotEmpty() && !p.demo && p.visibilidad == Visibilidad.PUBLICA && p.id.length == 36) "$base?p=${p.id}" else ""
    fun invitacion(usuario: String): String = if (base.isNotEmpty() && usuario.isNotBlank()) "$base?i=${URLEncoder.encode(usuario, "UTF-8")}" else "https://suroute.com/descargar.html"
}
