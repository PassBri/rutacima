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
    /** Brújula de la Cima compartida (JSON); vacío si la publicación no es una. */
    val brujula: String = "",
    /** Varias fotos o un video (carrusel); vacío si es una sola foto. */
    val media: List<MediaPost> = emptyList(),
    /** Reacción "¿Qué clima te deja?": cuántas personas dejaron cada clima y el mío. */
    val climas: Map<String, Int> = emptyMap(),
    val miClima: String? = null,
)

/** Una pieza del carrusel: foto ("img") o video ("vid"), con su ruta local o URL. */
data class MediaPost(val tipo: String, val url: String) { val esVideo get() = tipo == "vid" }

/** Lee y escribe la lista de medios como JSON [{"t":…,"u":…}]. */
object MediaJson {
    fun leer(e: kotlinx.serialization.json.JsonElement?): List<MediaPost> = runCatching {
        val arr = when (e) { is JsonArray -> e; is kotlinx.serialization.json.JsonPrimitive -> kotlinx.serialization.json.Json.parseToJsonElement(e.content).jsonArray; else -> null } ?: return@runCatching emptyList()
        arr.mapNotNull { x -> val o = x.jsonObject; val u = o["u"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null; MediaPost(o["t"]?.jsonPrimitive?.contentOrNull ?: "img", u) }
    }.getOrDefault(emptyList())
    fun leer(s: String): List<MediaPost> = if (s.isBlank()) emptyList() else leer(kotlinx.serialization.json.JsonPrimitive(s))
    fun json(m: List<MediaPost>): JsonArray = JsonArray(m.map { buildJsonObject { put("t", it.tipo); put("u", it.url) } })
}

data class Comentario(
    val id: String, val autor: String, val texto: String, val creadoEn: Long,
    /** Respuesta a otro comentario (null = comentario principal). */
    val parentId: String? = null, val likes: Int = 0, val yoLike: Boolean = false,
    val propio: Boolean = false, val ocultoAutor: Boolean = false, val autorAvatar: String = "",
)

enum class FiltroFeed { PARA_TI, SIGUIENDO, VISION, GUARDADOS }

/**
 * Comunidad de SuRoute.
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
        brujula = brujula, media = MediaJson.leer(media),
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
        /** Año del recuerdo (por defecto, este año) y mes, para registrar recuerdos de años pasados. */
        anio: Int = LocalDate.now().year,
        mes: Int? = null,
        /** Instantánea de la Brújula de la Cima (JSON) para explorarla en la comunidad. */
        brujula: String = "",
        /** Varias fotos o un video (carrusel). La primera foto queda como portada. */
        medios: List<Uri> = emptyList(),
    ): PublicacionEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val piezas = medios.mapIndexedNotNull { i, u -> runCatching { guardarMedio(u, "$id-$i") }.getOrNull() }
        val ruta = foto?.let { guardarFoto(it, id) } ?: piezas.firstOrNull { !it.esVideo }?.url.orEmpty()
        val hoy = LocalDate.now()
        // Un recuerdo de otro año (u otro mes) queda fechado a mitad de ese mes para verse en su lugar
        val fecha = if (anio == hoy.year && (mes == null || mes == hoy.monthValue)) null
            else LocalDate.of(anio, mes ?: 6, 15).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        var p = PublicacionEntity(
            id = id, tipo = tipo.name, texto = texto.trim(), foto = ruta, eje = eje,
            anio = anio, visibilidad = visibilidad.name, metaTitulo = metaTitulo, brujula = brujula,
            media = if (piezas.size > 1 || piezas.any { it.esVideo }) MediaJson.json(piezas).toString() else "",
        ).let { if (fecha != null) it.copy(creadaEn = fecha) else it }
        dao.upsert(p)
        if (enLinea && visibilidad != Visibilidad.PRIVADA) {
            runCatching { p = subir(p) }
        }
        p
    }

    /**
     * Foto para una Brújula compartida: con cuenta, la sube a la comunidad y devuelve su dirección
     * pública; sin cuenta, devuelve la ruta local (se ve en este teléfono).
     */
    suspend fun fotoParaCompartir(ruta: String, nombre: String): String = withContext(Dispatchers.IO) {
        if (ruta.isBlank() || ruta.startsWith("http")) return@withContext ruta
        val uid = supa.sesion.value?.userId
        val f = File(ruta)
        if (!enLinea || uid == null || !f.exists()) return@withContext ruta
        runCatching { supa.subirFoto("$uid/brujula/$nombre.jpg", f.readBytes()) }.getOrDefault(ruta)
    }

    /** Sube una publicación local (y su foto) a la comunidad. */
    suspend fun subir(p: PublicacionEntity): PublicacionEntity {
        val uid = supa.sesion.value?.userId ?: return p
        val url = if (p.foto.isNotBlank()) supa.subirFoto("$uid/${p.id}.jpg", File(p.foto).readBytes()) else ""
        // Carrusel o video: cada pieza se sube con su tipo
        val media = MediaJson.leer(p.media).mapIndexed { i, m ->
            if (m.url.startsWith("http")) m
            else if (m.esVideo) MediaPost("vid", supa.subirArchivo("media", "$uid/${p.id}-$i.mp4", File(m.url).readBytes(), "video/mp4"))
            else MediaPost("img", supa.subirFoto("$uid/${p.id}-$i.jpg", File(m.url).readBytes()))
        }
        supa.insert("posts", buildJsonObject {
            put("id", p.id); put("user_id", uid); put("tipo", p.tipo); put("texto", p.texto)
            put("image_url", url); p.eje?.let { put("eje", it) }; put("anio", p.anio)
            put("visibilidad", p.visibilidad); put("meta_titulo", p.metaTitulo)
            // Solo las Brújulas llevan la columna nueva (así lo demás funciona aunque no se haya actualizado la base)
            if (p.brujula.isNotBlank()) runCatching { kotlinx.serialization.json.Json.parseToJsonElement(p.brujula) }.getOrNull()?.let { put("brujula", it) }
            if (media.isNotEmpty()) put("media", MediaJson.json(media))
        }, devolver = false)
        val subida = p.copy(remoteId = p.id, fotoUrl = url, media = if (media.isNotEmpty()) MediaJson.json(media).toString() else p.media)
        dao.upsert(subida)
        return subida
    }

    // ------------------------------------------------------------------ Diario de vida

    /**
     * Comparte (o deja de compartir) un año del diario. Con [incluirPrivados], los recuerdos de ese año
     * marcados "Solo yo" pasan a la visibilidad elegida (y se suben si hay cuenta).
     */
    suspend fun compartirAnio(anio: Int, visibilidad: Visibilidad, incluirPrivados: Boolean) = withContext(Dispatchers.IO) {
        if (incluirPrivados && visibilidad != Visibilidad.PRIVADA) {
            dao.todas().filter { it.anio == anio && it.visibilidad == Visibilidad.PRIVADA.name }.forEach { p ->
                val n = p.copy(visibilidad = visibilidad.name)
                dao.upsert(n)
                if (enLinea) runCatching { if (n.remoteId == null) subir(n) else actualizarRemota(n) }
            }
        }
        val uid = supa.sesion.value?.userId
        if (enLinea && uid != null) runCatching {
            supa.upsert("diario_anios", buildJsonObject { put("user_id", uid); put("anio", anio); put("visibilidad", visibilidad.name) })
        }
    }

    /** Cuántos recuerdos "Solo yo" tengo en un año. */
    suspend fun privadosDe(anio: Int): Int = withContext(Dispatchers.IO) {
        dao.todas().count { it.anio == anio && it.visibilidad == Visibilidad.PRIVADA.name }
    }

    private suspend fun actualizarRemota(p: PublicacionEntity) {
        val uid = supa.sesion.value?.userId ?: return
        supa.upsert("posts", buildJsonObject {
            put("id", p.remoteId ?: p.id); put("user_id", uid); put("tipo", p.tipo); put("texto", p.texto)
            put("image_url", p.fotoUrl); p.eje?.let { put("eje", it) }; put("anio", p.anio)
            put("visibilidad", p.visibilidad); put("meta_titulo", p.metaTitulo)
        })
    }

    /** Años que otra persona comparte en su diario (los que puedo ver), del más reciente al más antiguo. */
    suspend fun aniosCompartidos(autorId: String): List<Pair<Int, Visibilidad>> = withContext(Dispatchers.IO) {
        if (!enLinea) {
            // Demostración: las personas de ejemplo comparten este año
            return@withContext if (DemoComunidad.posts(context, emptySet()).any { it.autorId == autorId })
                listOf(LocalDate.now().year to Visibilidad.PUBLICA) else emptyList()
        }
        supa.select("diario_anios", "select=anio,visibilidad&user_id=eq.$autorId&visibilidad=neq.PRIVADA&order=anio.desc").jsonArray.mapNotNull {
            val o = it.jsonObject
            val a = o["anio"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            a to (runCatching { Visibilidad.valueOf(o["visibilidad"]!!.jsonPrimitive.content) }.getOrDefault(Visibilidad.PUBLICA))
        }
    }

    /** El álbum de un año de otra persona: sus publicaciones de ese año que puedo ver. */
    suspend fun albumDe(autorId: String, anio: Int): List<Post> = withContext(Dispatchers.IO) {
        if (!enLinea) return@withContext DemoComunidad.posts(context, impulsados()).filter { it.autorId == autorId && it.anio == anio }
        val uid = supa.sesion.value!!.userId
        val filas = supa.select("posts", "select=*,autor:profiles!posts_user_id_fkey(username,nombre,avatar_url),impulsos:votes(count),comentarios:comments(count)" +
            "&user_id=eq.$autorId&anio=eq.$anio&order=created_at.desc&limit=200").jsonArray
        filas.map { filaAPost(it.jsonObject, uid, emptySet()) }.also(::recordar)
    }

    /** Página a página para no truncar las cifras al límite de filas de PostgREST. */
    private suspend fun filasSociales(tabla: String, consulta: String): List<kotlinx.serialization.json.JsonElement> {
        val filas = mutableListOf<kotlinx.serialization.json.JsonElement>()
        do {
            val pagina = supa.select(tabla, "$consulta&limit=500&offset=${filas.size}").jsonArray
            filas.addAll(pagina)
        } while (pagina.size == 500)
        return filas
    }

    /** Conteos del servidor; null sin sesión o cuando la consulta falla. */
    suspend fun contarSiguiendo(): Int? = contarRelacion("follower_id", "followed_id")
    suspend fun contarSeguidores(): Int? = contarRelacion("followed_id", "follower_id")

    private suspend fun contarRelacion(filtro: String, columna: String): Int? = withContext(Dispatchers.IO) {
        val uid = supa.sesion.value?.userId ?: return@withContext null
        runCatching { filasSociales("follows", "select=$columna&$filtro=eq.$uid&order=follower_id,followed_id").size }.getOrNull()
    }

    suspend fun contarImpulsos(): Int? = withContext(Dispatchers.IO) {
        val uid = supa.sesion.value?.userId ?: return@withContext null
        runCatching {
            val ids = filasSociales("posts", "select=id&user_id=eq.$uid&order=id")
                .mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
            ids.chunked(100).sumOf { lote ->
                filasSociales("votes", "select=post_id&post_id=in.(${lote.joinToString(",")})&order=post_id,user_id").size
            }
        }.getOrNull()
    }

    /** Recupera también guardadas que aún no se han visto en este proceso. */
    suspend fun publicacionesPorIds(ids: Set<String>): List<Post> = withContext(Dispatchers.IO) {
        val locales = dao.todas().map { it.aPost() }.associateBy { it.id }
        val ejemplos = if (enLinea) emptyMap() else DemoComunidad.posts(context, impulsados()).associateBy { it.id }
        val conocidas = ids.mapNotNull { locales[it] ?: cache[it] ?: ejemplos[it] }.associateBy { it.id }.toMutableMap()
        if (enLinea) {
            val remotos = ids.filter { it.matches(Regex("[0-9a-fA-F-]{36}")) }
            remotos.chunked(100).forEach { lote ->
                val filas = supa.select("posts", "select=*,autor:profiles!posts_user_id_fkey(username,nombre,avatar_url),impulsos:votes(count),comentarios:comments(count)&id=in.(${lote.joinToString(",")})").jsonArray
                filas.map { filaAPost(it.jsonObject, supa.sesion.value?.userId.orEmpty(), impulsados()) }
                    .forEach { conocidas[it.id] = it }
            }
        }
        conocidas.values.sortedByDescending { it.creadoEn }.also(::recordar)
    }

    /** Cambia quién ve una publicación propia (en el teléfono y, si ya está en la comunidad, también allá). */
    /** Editar el texto y la meta de una publicación propia (aquí y en la comunidad). */
    suspend fun editarTexto(post: Post, texto: String, meta: String) = withContext(Dispatchers.IO) {
        dao.get(post.id)?.let { dao.upsert(it.copy(texto = texto, metaTitulo = meta)) }
        if (enLinea) runCatching { supa.actualizar("posts", "id=eq.${post.id}&user_id=eq.${supa.sesion.value!!.userId}", buildJsonObject { put("texto", texto); put("meta_titulo", meta) }) }
        cache[post.id]?.let { cache[post.id] = it.copy(texto = texto, metaTitulo = meta) }
    }

    suspend fun cambiarVisibilidad(post: Post, v: Visibilidad) = withContext(Dispatchers.IO) {
        val local = dao.get(post.id)
        if (local != null) {
            val p = local.copy(visibilidad = v.name)
            dao.upsert(p)
            if (enLinea) runCatching {
                if (p.remoteId != null) supa.actualizar("posts", "id=eq.${p.id}", buildJsonObject { put("visibilidad", v.name) })
                else if (v != Visibilidad.PRIVADA) subir(p)
            }
        } else if (enLinea) runCatching { supa.actualizar("posts", "id=eq.${post.id}", buildJsonObject { put("visibilidad", v.name) }) }
        cache[post.id]?.let { cache[post.id] = it.copy(visibilidad = v) }
    }

    suspend fun eliminar(post: Post) {
        dao.delete(post.id)
        if (enLinea) runCatching { supa.delete("posts", "id=eq.${post.id}") }
    }

    /** Comprime la foto (máx. 1440 px, JPEG 85) y la guarda en el almacenamiento privado de la app. */
    /** Copia una foto (achicada) o un video a la carpeta de la app y devuelve la pieza local. */
    private fun guardarMedio(uri: Uri, id: String): MediaPost {
        val tipo = context.contentResolver.getType(uri).orEmpty()
        if (!tipo.startsWith("video/")) return MediaPost("img", guardarFoto(uri, id))
        val dir = File(context.filesDir, "publicaciones").apply { mkdirs() }
        val f = File(dir, "$id.mp4")
        context.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        return MediaPost("vid", f.absolutePath)
    }

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

    // ------------------------------------------------------------------ Guardados (como en Instagram)

    fun guardados(): Set<String> = prefs.getStringSet("guardados", emptySet()).orEmpty()

    /** Guarda o quita una publicación de "Guardados". Devuelve si quedó guardada. */
    fun alternarGuardado(id: String): Boolean {
        val g = guardados()
        val queda = id !in g
        prefs.edit().putStringSet("guardados", if (queda) g + id else g - id).apply()
        return queda
    }

    /** El muro: [filtro] elige qué publicaciones y [eje] (VOL, MAE…) deja solo las de ese eje. */
    /** Publicaciones por página del muro. */
    val paginaFeed = 30

    suspend fun feed(filtro: FiltroFeed, eje: String? = null, desde: Int = 0): List<Post> = withContext(Dispatchers.IO) {
        val guardados = guardados()
        if (!enLinea) {
            if (desde > 0) return@withContext emptyList()
            val mias = dao.todas().filter { it.visibilidad != Visibilidad.PRIVADA.name }.map { it.aPost() }
            val todo = (mias + DemoComunidad.posts(context, impulsados())).sortedByDescending { it.creadoEn }
            val r = when (filtro) {
                FiltroFeed.VISION -> todo.filter { it.tipo == TipoPost.VISION }
                FiltroFeed.SIGUIENDO -> mias
                FiltroFeed.PARA_TI -> todo
                FiltroFeed.GUARDADOS -> todo.filter { it.id in guardados }
            }
            return@withContext conClimas(r.filter { (eje == null || it.eje == eje) && it.id !in ocultosLocales() && it.autorId !in bloqueadosLocales() && it.autorId !in silenciadosLocales() }, null).also(::recordar)
        }
        val uid = supa.sesion.value!!.userId
        val base = "select=*,autor:profiles!posts_user_id_fkey(username,nombre,avatar_url),impulsos:votes(count),comentarios:comments(count)&order=created_at.desc&limit=$paginaFeed&offset=$desde" +
            (eje?.let { "&eje=eq.$it" } ?: "")
        val consulta = when (filtro) {
            FiltroFeed.PARA_TI -> base
            FiltroFeed.VISION -> "$base&tipo=eq.VISION"
            FiltroFeed.GUARDADOS -> {
                // Solo ids con forma de UUID (los de ejemplo no existen en el servidor)
                val ids = guardados.filter { it.matches(Regex("[0-9a-fA-F-]{36}")) }
                if (ids.isEmpty()) return@withContext emptyList()
                "$base&id=in.(${ids.joinToString(",")})"
            }
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
        val silenciados = silenciadosLocales()
        conClimas(filas.map { f -> filaAPost(f.jsonObject, uid, mios) }.filter { it.autorId !in silenciados }, uid).also(::recordar)
    }

    /** Publicaciones públicas con un filtro extra de PostgREST (etiqueta o eje), para sus páginas. */
    suspend fun publicasCon(filtro: String, demo: (Post) -> Boolean): List<Post> = withContext(Dispatchers.IO) {
        if (!enLinea) return@withContext DemoComunidad.posts(context, impulsados()).filter(demo).also(::recordar)
        val uid = supa.sesion.value!!.userId
        val filas = supa.select("posts", "select=*,autor:profiles!posts_user_id_fkey(username,nombre,avatar_url),impulsos:votes(count),comentarios:comments(count)" +
            "&visibilidad=eq.PUBLICA&$filtro&order=created_at.desc&limit=60").jsonArray
        filas.map { filaAPost(it.jsonObject, uid, emptySet()) }.also(::recordar)
    }

    /** Publicaciones visibles de una persona (perfil público). */
    suspend fun deAutor(autorId: String, limite: Int = 60): List<Post> = withContext(Dispatchers.IO) {
        if (!enLinea) return@withContext DemoComunidad.posts(context, impulsados()).filter { it.autorId == autorId }.also(::recordar)
        val uid = supa.sesion.value!!.userId
        val filas = supa.select("posts", "select=*,autor:profiles!posts_user_id_fkey(username,nombre,avatar_url),impulsos:votes(count),comentarios:comments(count)" +
            "&user_id=eq.$autorId&order=created_at.desc&limit=$limite").jsonArray
        val ids = filas.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
        val mios = if (ids.isEmpty()) emptySet() else supa.select("votes", "select=post_id&user_id=eq.$uid&post_id=in.(${ids.joinToString(",")})")
            .jsonArray.mapNotNull { it.jsonObject["post_id"]?.jsonPrimitive?.contentOrNull }.toSet()
        filas.map { filaAPost(it.jsonObject, uid, mios) }.also(::recordar)
    }

    /** Puntos de la expedición de la comunidad por tramo del año, sumados en el servidor (null sin cuenta). */
    suspend fun expedicion(anio: Int): List<Long>? = withContext(Dispatchers.IO) {
        if (!enLinea) return@withContext null
        runCatching {
            val r = LongArray(com.rutaalacima.app.domain.model.Expedicion.TRAMOS)
            supa.rpc("expedicion", buildJsonObject { put("p_anio", anio) }).jsonArray.forEach { f ->
                val o = f.jsonObject
                val t = o["semana"]?.jsonPrimitive?.intOrNull ?: return@forEach
                r[t.coerceIn(0, r.size - 1)] += o["puntos"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
            }
            r.toList()
        }.getOrNull()
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
            brujula = when (val b = o["brujula"]) { is JsonObject -> b.toString(); is kotlinx.serialization.json.JsonPrimitive -> b.contentOrNull.orEmpty(); else -> "" },
            media = MediaJson.leer(o["media"]),
        )
    }

    // ------------------------------------------------------------------ Interacción

    /** Climas guardados en este teléfono (sin cuenta o en publicaciones de ejemplo): id → clima. */
    private fun climasLocales(): Map<String, String> = prefs.getString("climas", "").orEmpty().split(";")
        .mapNotNull { it.split("=").takeIf { p -> p.size == 2 }?.let { p -> p[0] to p[1] } }.toMap()

    /** Conteos de ejemplo estables por publicación (los mismos que la web). */
    private fun climasDemo(id: String): MutableMap<String, Int> {
        var h = 0L; id.forEach { h = (h * 31 + it.code) and 0xFFFFFFFFL }
        return com.rutaalacima.app.ui.components.ClimaIconos.REACCIONES.withIndex()
            .associate { (i, k) -> k to ((h shr (i * 3)) % (if (i == 0 || i == 4) 9 else 5)).toInt() }.toMutableMap()
    }

    /** Agrega los climas de la comunidad (tabla post_climas) y el mío a cada publicación. */
    private suspend fun conClimas(posts: List<Post>, uid: String?): List<Post> {
        val locales = climasLocales()
        val base = posts.map { p ->
            if (uid != null && !p.demo) p.copy(climas = emptyMap(), miClima = null)
            else { val m = climasDemo(p.id); val mio = locales[p.id]; if (mio != null) m[mio] = (m[mio] ?: 0) + 1; p.copy(climas = m, miClima = mio) }
        }
        val ids = base.filter { !it.demo && it.id.matches(Regex("[0-9a-fA-F-]{36}")) }.map { it.id }
        if (uid == null || ids.isEmpty()) return base
        val filas = runCatching { supa.select("post_climas", "select=post_id,user_id,clima&post_id=in.(${ids.joinToString(",")})").jsonArray }.getOrNull() ?: return base
        val cuenta = HashMap<String, MutableMap<String, Int>>(); val mios = HashMap<String, String>()
        filas.forEach { f ->
            val o = f.jsonObject
            val pid = o["post_id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val c = o["clima"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            cuenta.getOrPut(pid) { HashMap() }.merge(c, 1) { a, b -> a + b }
            if (o["user_id"]?.jsonPrimitive?.contentOrNull == uid) mios[pid] = c
        }
        return base.map { p -> if (p.demo) p else p.copy(climas = cuenta[p.id].orEmpty(), miClima = mios[p.id]) }
    }

    /** Deja, cambia o quita (null) mi clima en una publicación. */
    suspend fun reaccionarClima(post: Post, clima: String?) = withContext(Dispatchers.IO) {
        if (enLinea && !post.demo) {
            val uid = supa.sesion.value!!.userId
            if (clima == null) supa.delete("post_climas", "post_id=eq.${post.id}&user_id=eq.$uid")
            else supa.upsert("post_climas", buildJsonObject { put("post_id", post.id); put("user_id", uid); put("clima", clima) })
        } else {
            val m = climasLocales().toMutableMap()
            if (clima == null) m.remove(post.id) else m[post.id] = clima
            prefs.edit().putString("climas", m.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
        }
    }

    /** Da o quita un "impulso" (el voto de SuRoute). */
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
            val uid = supa.sesion.value!!.userId
            fun fila(e: kotlinx.serialization.json.JsonElement): Comentario {
                val o = e.jsonObject; val autor = o["autor"] as? JsonObject
                fun s(k: String) = o[k]?.jsonPrimitive?.contentOrNull.orEmpty()
                return Comentario(
                    id = s("id"), autor = autor?.get("nombre")?.jsonPrimitive?.contentOrNull?.ifBlank { null } ?: autor?.get("username")?.jsonPrimitive?.contentOrNull.orEmpty(),
                    texto = s("texto"), creadoEn = runCatching { Instant.parse(s("created_at")).toEpochMilli() }.getOrDefault(0L),
                    parentId = s("parent_id").ifBlank { null }, likes = conteo(o, "likes"), propio = s("user_id") == uid,
                    ocultoAutor = o["oculto_autor"]?.jsonPrimitive?.contentOrNull == "true", autorAvatar = autor?.get("avatar_url")?.jsonPrimitive?.contentOrNull.orEmpty(),
                )
            }
            // Con respuestas e impulsos (si la base ya los tiene); si no, comentarios simples
            val lista = runCatching {
                supa.select("comments", "select=id,texto,created_at,user_id,parent_id,oculto_autor,autor:profiles!comments_user_id_fkey(username,nombre,avatar_url),likes:comment_likes(count)&post_id=eq.${post.id}&order=created_at")
                    .jsonArray.map(::fila)
            }.getOrElse {
                supa.select("comments", "select=id,texto,created_at,user_id,autor:profiles!comments_user_id_fkey(username,nombre)&post_id=eq.${post.id}&order=created_at").jsonArray.map(::fila)
            }
            val mios = if (lista.isEmpty()) emptySet() else runCatching {
                supa.select("comment_likes", "select=comment_id&user_id=eq.$uid&comment_id=in.(${lista.joinToString(",") { it.id }})").jsonArray
                    .mapNotNull { it.jsonObject["comment_id"]?.jsonPrimitive?.contentOrNull }.toSet()
            }.getOrDefault(emptySet())
            lista.map { it.copy(yoLike = it.id in mios) }
        } else {
            DemoComunidad.comentarios(context, post.id) +
                dao.comentarios(post.id).map { Comentario(it.id.toString(), it.autor, it.texto, it.creadoEn, propio = true) }
        }
    }

    suspend fun comentar(post: Post, texto: String, parentId: String? = null) = withContext(Dispatchers.IO) {
        if (texto.isBlank()) return@withContext
        if (enLinea && !post.demo) {
            val fila = buildJsonObject { put("post_id", post.id); put("user_id", supa.sesion.value!!.userId); put("texto", texto.trim()) }
            if (parentId != null) runCatching { supa.insert("comments", JsonObject(fila + ("parent_id" to kotlinx.serialization.json.JsonPrimitive(parentId))), devolver = false) }
                .getOrElse { supa.insert("comments", fila, devolver = false) }
            else supa.insert("comments", fila, devolver = false)
        } else {
            dao.comentar(ComentarioLocalEntity(publicacionId = post.id, autor = nombrePropio(), texto = texto.trim()))
        }
    }

    /** Impulso a un comentario (como "me gusta"). */
    suspend fun impulsarComentario(c: Comentario) = withContext(Dispatchers.IO) {
        if (!enLinea) return@withContext
        val uid = supa.sesion.value!!.userId
        if (c.yoLike) supa.delete("comment_likes", "comment_id=eq.${c.id}&user_id=eq.$uid")
        else supa.insert("comment_likes", buildJsonObject { put("comment_id", c.id); put("user_id", uid) }, devolver = false)
    }

    /** Ocultar (o mostrar) un comentario en mi publicación. */
    suspend fun ocultarComentario(c: Comentario, si: Boolean) = withContext(Dispatchers.IO) {
        if (enLinea) supa.rpc("ocultar_comentario", buildJsonObject { put("p_comment", c.id); put("p_si", si) })
    }

    /** Silenciar: dejo de ver a esa persona y sus avisos, sin bloquearla. */
    suspend fun silenciar(autorId: String, si: Boolean) = withContext(Dispatchers.IO) {
        val s = prefs.getStringSet("silenciados", emptySet()).orEmpty()
        prefs.edit().putStringSet("silenciados", if (si) s + autorId else s - autorId).apply()
        if (si) cache.values.removeAll { it.autorId == autorId }
        if (enLinea) runCatching {
            val uid = supa.sesion.value!!.userId
            if (si) supa.upsert("silencios", buildJsonObject { put("quien", uid); put("a_quien", autorId) })
            else supa.delete("silencios", "quien=eq.$uid&a_quien=eq.$autorId")
        }
    }
    fun silenciadosLocales(): Set<String> = prefs.getStringSet("silenciados", emptySet()).orEmpty()

    // ------------------------------------------------------------------ Reportar y bloquear

    private fun ocultosLocales(): Set<String> = prefs.getStringSet("ocultos", emptySet()).orEmpty()
    private fun bloqueadosLocales(): Set<String> = prefs.getStringSet("bloqueados", emptySet()).orEmpty()

    /** Reporta una publicación: deja de verse para ti y, con 3 reportes, se oculta para todos hasta revisarla. */
    suspend fun reportar(post: Post, motivo: String) = withContext(Dispatchers.IO) {
        if (enLinea && !post.demo) {
            runCatching {
                supa.insert("reportes", buildJsonObject {
                    put("quien", supa.sesion.value!!.userId); put("a_quien", post.autorId); put("post_id", post.id); put("motivo", motivo.take(1000))
                }, devolver = false)
            }.onFailure { if (it !is com.rutaalacima.app.data.remote.SupabaseException || it.codigo != 409) throw it }   // ya lo habías reportado
        }
        prefs.edit().putStringSet("ocultos", ocultosLocales() + post.id).apply()
        cache.remove(post.id)
    }

    /** Bloquea a una persona: no ves sus publicaciones ni comentarios, ni pueden escribirse. */
    suspend fun bloquear(post: Post) = withContext(Dispatchers.IO) {
        val autorId = post.autorId
        if (enLinea && !post.demo) {
            supa.upsert("bloqueos", buildJsonObject { put("quien", supa.sesion.value!!.userId); put("a_quien", autorId) })
        }
        prefs.edit().putStringSet("bloqueados", bloqueadosLocales() + autorId).apply()
        cache.values.removeAll { it.autorId == autorId }
    }

    suspend fun seguir(autorId: String, seguir: Boolean) = withContext(Dispatchers.IO) {
        if (!enLinea) return@withContext
        val uid = supa.sesion.value!!.userId
        if (seguir) supa.upsert("follows", buildJsonObject { put("follower_id", uid); put("followed_id", autorId) })
        else supa.delete("follows", "follower_id=eq.$uid&followed_id=eq.$autorId")
    }
}
