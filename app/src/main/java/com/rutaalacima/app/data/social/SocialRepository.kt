package com.rutaalacima.app.data.social

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.ComentarioLocalEntity
import com.rutaalacima.app.data.local.PublicacionEntity
import com.rutaalacima.app.data.local.RutaDatabase
import com.rutaalacima.app.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Tipos de publicación. */
enum class TipoPost { LOGRO, EVIDENCIA, VISION, META, REFLEXION }

enum class Visibilidad { PUBLICA, SEGUIDORES, PRIVADA }

/** Publicación lista para mostrar (propia, de la comunidad o de ejemplo). */
data class Post(
    val id: String,
    val autorId: String,
    val autorNombre: String,
    val autorUsuario: String,
    val avatarUrl: String = "",
    val tipo: TipoPost,
    val texto: String,
    /** URL remota o ruta local de la foto; vacío = arte vectorial. */
    val foto: String = "",
    val eje: String? = null,
    val anio: Int,
    val metaTitulo: String = "",
    val impulsos: Int = 0,
    val comentarios: Int = 0,
    val yoImpulse: Boolean = false,
    val creadoEn: Long,
    val propio: Boolean = false,
    val demo: Boolean = false,
    val visibilidad: Visibilidad = Visibilidad.PUBLICA,
)

data class Comentario(val id: String, val autor: String, val texto: String, val creadoEn: Long)

enum class FiltroFeed { PARA_TI, SIGUIENDO, VISION }

/**
 * Comunidad de Rutaalacima.
 *  - Mis publicaciones SIEMPRE se guardan en el teléfono: son el registro de cada año de la vida.
 *  - Con cuenta (Supabase configurado + sesión) se suben y se ven, votan y comentan en la comunidad.
 *  - Sin cuenta, la comunidad muestra publicaciones de ejemplo (modo demo).
 */
class SocialRepository(
    private val context: Context,
    private val db: RutaDatabase,
    private val supa: SupabaseClient,
) {
    private val dao = db.socialDao()
    private val prefs = context.getSharedPreferences("rutacima_social", Context.MODE_PRIVATE)

    val enLinea: Boolean get() = supa.configurado && supa.sesion.value != null

    /** Mis publicaciones (todas, incluidas las privadas), de la más reciente a la más antigua. */
    val misPublicaciones: Flow<List<Post>> = dao.observePublicaciones().map { lista -> lista.map { it.aPost() }.also(::recordar) }

    private fun PublicacionEntity.aPost() = Post(
        id = id, autorId = supa.sesion.value?.userId ?: "yo", autorNombre = nombrePropio(), autorUsuario = usuarioPropio(),
        tipo = runCatching { TipoPost.valueOf(tipo) }.getOrDefault(TipoPost.REFLEXION),
        texto = texto, foto = fotoUrl.ifBlank { foto }, eje = eje, anio = anio, metaTitulo = metaTitulo,
        impulsos = impulsos + if (id in impulsados()) 1 else 0, yoImpulse = id in impulsados(),
        creadoEn = creadaEn, propio = true, visibilidad = runCatching { Visibilidad.valueOf(visibilidad) }.getOrDefault(Visibilidad.PUBLICA),
    )

    fun nombrePropio(): String = prefs.getString("nombre", "").orEmpty().ifBlank { context.getString(R.string.yo) }
    fun usuarioPropio(): String = prefs.getString("usuario", "").orEmpty()
    fun guardarIdentidad(nombre: String, usuario: String) = prefs.edit().putString("nombre", nombre).putString("usuario", usuario).apply()

    /** Últimas publicaciones vistas (para abrir el detalle con sus comentarios). */
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Post>()
    fun enCache(id: String): Post? = cache[id]
    fun recordar(posts: List<Post>) = posts.forEach { cache[it.id] = it }

    private fun impulsados(): Set<String> = prefs.getStringSet("impulsos", emptySet()).orEmpty()

    // ------------------------------------------------------------------ Publicar

    suspend fun publicar(
        tipo: TipoPost,
        texto: String,
        foto: Uri?,
        eje: String?,
        visibilidad: Visibilidad,
        metaTitulo: String = "",
    ): PublicacionEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val ruta = foto?.let { guardarFoto(it, id) }.orEmpty()
        var p = PublicacionEntity(
            id = id, tipo = tipo.name, texto = texto.trim(), foto = ruta, eje = eje,
            anio = LocalDate.now().year, visibilidad = visibilidad.name, metaTitulo = metaTitulo,
        )
        dao.upsert(p)
        if (enLinea && visibilidad != Visibilidad.PRIVADA) {
            runCatching { p = subir(p) }
        }
        p
    }

    /** Sube una publicación local (y su foto) a la comunidad. */
    suspend fun subir(p: PublicacionEntity): PublicacionEntity {
        val uid = supa.sesion.value?.userId ?: return p
        val url = if (p.foto.isNotBlank()) supa.subirFoto("$uid/${p.id}.jpg", File(p.foto).readBytes()) else ""
        supa.insert("posts", buildJsonObject {
            put("id", p.id); put("user_id", uid); put("tipo", p.tipo); put("texto", p.texto)
            put("image_url", url); p.eje?.let { put("eje", it) }; put("anio", p.anio)
            put("visibilidad", p.visibilidad); put("meta_titulo", p.metaTitulo)
        }, devolver = false)
        val subida = p.copy(remoteId = p.id, fotoUrl = url)
        dao.upsert(subida)
        return subida
    }

    suspend fun eliminar(post: Post) {
        dao.delete(post.id)
        if (enLinea) runCatching { supa.delete("posts", "id=eq.${post.id}") }
    }

    /** Comprime la foto (máx. 1440 px, JPEG 85) y la guarda en el almacenamiento privado de la app. */
    private fun guardarFoto(uri: Uri, id: String): String {
        val cr = context.contentResolver
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        var muestra = 1
        while (maxOf(opciones.outWidth, opciones.outHeight) / muestra > 2880) muestra *= 2
        val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra }) }
            ?: return ""
        val escala = minOf(1f, 1440f / maxOf(bmp.width, bmp.height))
        val final = if (escala < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * escala).toInt(), (bmp.height * escala).toInt(), true) else bmp
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val f = File(dir, "$id.jpg")
        ByteArrayOutputStream().use { out ->
            final.compress(Bitmap.CompressFormat.JPEG, 85, out)
            f.writeBytes(out.toByteArray())
        }
        return f.absolutePath
    }

    // ------------------------------------------------------------------ Feed

    suspend fun feed(filtro: FiltroFeed): List<Post> = withContext(Dispatchers.IO) {
        if (!enLinea) {
            val mias = dao.todas().filter { it.visibilidad != Visibilidad.PRIVADA.name }.map { it.aPost() }
            val todo = (mias + DemoComunidad.posts(context, impulsados())).sortedByDescending { it.creadoEn }
            val r = when (filtro) {
                FiltroFeed.VISION -> todo.filter { it.tipo == TipoPost.VISION }
                FiltroFeed.SIGUIENDO -> mias
                FiltroFeed.PARA_TI -> todo
            }
            return@withContext r.also(::recordar)
        }
        val uid = supa.sesion.value!!.userId
        val base = "select=*,autor:profiles(username,nombre,avatar_url),impulsos:votes(count),comentarios:comments(count)&order=created_at.desc&limit=40"
        val consulta = when (filtro) {
            FiltroFeed.PARA_TI -> base
            FiltroFeed.VISION -> "$base&tipo=eq.VISION"
            FiltroFeed.SIGUIENDO -> {
                val seguidos = supa.select("follows", "select=followed_id&follower_id=eq.$uid").jsonArray
                    .mapNotNull { it.jsonObject["followed_id"]?.jsonPrimitive?.contentOrNull }
                if (seguidos.isEmpty()) return@withContext emptyList()
                "$base&user_id=in.(${seguidos.joinToString(",")})"
            }
        }
        val filas = supa.select("posts", consulta).jsonArray
        val ids = filas.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
        val mios = if (ids.isEmpty()) emptySet() else supa.select("votes", "select=post_id&user_id=eq.$uid&post_id=in.(${ids.joinToString(",")})")
            .jsonArray.mapNotNull { it.jsonObject["post_id"]?.jsonPrimitive?.contentOrNull }.toSet()
        filas.map { f -> filaAPost(f.jsonObject, uid, mios) }.also(::recordar)
    }

    private fun conteo(o: JsonObject, campo: String): Int =
        (o[campo] as? JsonArray)?.firstOrNull()?.jsonObject?.get("count")?.jsonPrimitive?.intOrNull ?: 0

    private fun filaAPost(o: JsonObject, uid: String, mios: Set<String>): Post {
        fun s(k: String) = o[k]?.jsonPrimitive?.contentOrNull.orEmpty()
        val autor = o["autor"] as? JsonObject
        fun a(k: String) = autor?.get(k)?.jsonPrimitive?.contentOrNull.orEmpty()
        return Post(
            id = s("id"), autorId = s("user_id"), autorNombre = a("nombre").ifBlank { a("username") }, autorUsuario = a("username"),
            avatarUrl = a("avatar_url"), tipo = runCatching { TipoPost.valueOf(s("tipo")) }.getOrDefault(TipoPost.REFLEXION),
            texto = s("texto"), foto = s("image_url"), eje = s("eje").ifBlank { null }, anio = s("anio").toIntOrNull() ?: LocalDate.now().year,
            metaTitulo = s("meta_titulo"), impulsos = conteo(o, "impulsos"), comentarios = conteo(o, "comentarios"),
            yoImpulse = s("id") in mios, creadoEn = runCatching { Instant.parse(s("created_at")).toEpochMilli() }.getOrDefault(0L),
            propio = s("user_id") == uid, visibilidad = runCatching { Visibilidad.valueOf(s("visibilidad")) }.getOrDefault(Visibilidad.PUBLICA),
        )
    }

    // ------------------------------------------------------------------ Interacción

    /** Da o quita un "impulso" (el voto de Rutaalacima). */
    suspend fun impulsar(post: Post) = withContext(Dispatchers.IO) {
        if (enLinea && !post.demo) {
            val uid = supa.sesion.value!!.userId
            if (post.yoImpulse) supa.delete("votes", "post_id=eq.${post.id}&user_id=eq.$uid")
            else supa.insert("votes", buildJsonObject { put("post_id", post.id); put("user_id", uid) }, devolver = false)
        } else {
            val set = impulsados().toMutableSet()
            if (!set.add(post.id)) set.remove(post.id)
            prefs.edit().putStringSet("impulsos", set).apply()
        }
    }

    suspend fun comentarios(post: Post): List<Comentario> = withContext(Dispatchers.IO) {
        if (enLinea && !post.demo) {
            supa.select("comments", "select=id,texto,created_at,autor:profiles(username,nombre)&post_id=eq.${post.id}&order=created_at")
                .jsonArray.map { e ->
                    val o = e.jsonObject
                    val autor = o["autor"] as? JsonObject
                    Comentario(
                        id = o["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        autor = autor?.get("nombre")?.jsonPrimitive?.contentOrNull ?: autor?.get("username")?.jsonPrimitive?.contentOrNull.orEmpty(),
                        texto = o["texto"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        creadoEn = runCatching { Instant.parse(o["created_at"]!!.jsonPrimitive.content).toEpochMilli() }.getOrDefault(0L),
                    )
                }
        } else {
            DemoComunidad.comentarios(context, post.id) +
                dao.comentarios(post.id).map { Comentario(it.id.toString(), it.autor, it.texto, it.creadoEn) }
        }
    }

    suspend fun comentar(post: Post, texto: String) = withContext(Dispatchers.IO) {
        if (texto.isBlank()) return@withContext
        if (enLinea && !post.demo) {
            supa.insert("comments", buildJsonObject {
                put("post_id", post.id); put("user_id", supa.sesion.value!!.userId); put("texto", texto.trim())
            }, devolver = false)
        } else {
            dao.comentar(ComentarioLocalEntity(publicacionId = post.id, autor = nombrePropio(), texto = texto.trim()))
        }
    }

    suspend fun seguir(autorId: String, seguir: Boolean) = withContext(Dispatchers.IO) {
        if (!enLinea) return@withContext
        val uid = supa.sesion.value!!.userId
        if (seguir) supa.upsert("follows", buildJsonObject { put("follower_id", uid); put("followed_id", autorId) })
        else supa.delete("follows", "follower_id=eq.$uid&followed_id=eq.$autorId")
    }
}
