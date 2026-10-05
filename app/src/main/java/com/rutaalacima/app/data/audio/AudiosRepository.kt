package com.rutaalacima.app.data.audio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.rutaalacima.app.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Grabaciones propias de los capítulos (audiolibro con la voz del autor).
 *
 * Por cada capítulo se busca, en este orden:
 * 1. la grabación subida al servidor (tabla `audios`, bucket `audios`);
 * 2. un archivo incluido en la app: assets/audios/{guía}/{capítulo}.mp3 (o .m4a, .ogg);
 * 3. si no hay ninguna, la app lee el capítulo con la voz del teléfono.
 */
class AudiosRepository(private val context: Context, private val supa: SupabaseClient) {
    private val prefs = context.getSharedPreferences("audiolibros", Context.MODE_PRIVATE)

    /** "guía/capítulo" → URL de la grabación en el servidor. */
    private val _remotos = MutableStateFlow(leerGuardados())
    val remotos: StateFlow<Map<String, String>> = _remotos.asStateFlow()

    private val _soyAutor = MutableStateFlow(false)
    /** Quien está en la tabla `autores` puede subir, cambiar y quitar grabaciones. */
    val soyAutor: StateFlow<Boolean> = _soyAutor.asStateFlow()

    private var ultimaCarga = 0L
    private val incluidos: Map<String, String> by lazy {
        val r = mutableMapOf<String, String>()
        runCatching {
            context.assets.list("audios")?.forEach { guia ->
                context.assets.list("audios/$guia")?.forEach { archivo ->
                    val sec = archivo.substringBeforeLast('.').toIntOrNull()
                    if (sec != null && archivo.substringAfterLast('.').lowercase() in EXTENSIONES) r["$guia/$sec"] = "audios/$guia/$archivo"
                }
            }
        }
        r
    }

    private fun clave(guia: String, seccion: Int) = "$guia/$seccion"

    /**
     * Fuente de la grabación del capítulo: "file:…" si la descargaste (y sigue vigente), la URL
     * remota, "asset:ruta" si viene en la app, o null (se usa la voz del teléfono).
     */
    fun fuente(guia: String, seccion: Int): String? {
        val k = clave(guia, seccion)
        val remota = _remotos.value[k]
        val local = archivoDescargado(guia, seccion)
        if (local != null && (remota == null || descargas.getString(k, null) == remota)) return "file:${local.absolutePath}"
        return remota ?: incluidos[k]?.let { "asset:$it" }
    }

    // ------------------------------------------------------------------ Descargas (escuchar sin internet)

    private val descargas = context.getSharedPreferences("audiolibros_descargas", Context.MODE_PRIVATE)
    private fun carpeta(guia: String) = java.io.File(context.filesDir, "audios/$guia")
    private fun archivoDescargado(guia: String, seccion: Int): java.io.File? =
        carpeta(guia).listFiles()?.firstOrNull { it.nameWithoutExtension == seccion.toString() && it.length() > 0 }

    /** Capítulos de la guía con grabación en el servidor. */
    fun grabacionesRemotas(guia: String): Map<Int, String> =
        _remotos.value.filterKeys { it.substringBefore('/') == guia }.mapKeys { it.key.substringAfter('/').toInt() }

    /** Todas las grabaciones de la guía están descargadas y al día. */
    fun descargada(guia: String): Boolean {
        val r = grabacionesRemotas(guia)
        return r.isNotEmpty() && r.all { (sec, url) -> archivoDescargado(guia, sec) != null && descargas.getString(clave(guia, sec), null) == url }
    }

    /** Descarga las grabaciones de la guía para escucharlas sin internet. */
    suspend fun descargar(guia: String, alAvanzar: (hechas: Int, total: Int) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        val r = grabacionesRemotas(guia)
        carpeta(guia).mkdirs()
        r.entries.forEachIndexed { i, (sec, url) ->
            if (descargas.getString(clave(guia, sec), null) != url || archivoDescargado(guia, sec) == null) {
                val ext = url.substringAfterLast('.', "mp3").substringBefore('?').take(5)
                val destino = java.io.File(carpeta(guia), "$sec.$ext")
                val temporal = java.io.File(carpeta(guia), "$sec.descargando")
                java.net.URL(url).openStream().use { entrada -> temporal.outputStream().use { entrada.copyTo(it) } }
                carpeta(guia).listFiles()?.filter { it.nameWithoutExtension == sec.toString() }?.forEach { it.delete() }
                temporal.renameTo(destino)
                descargas.edit().putString(clave(guia, sec), url).apply()
            }
            alAvanzar(i + 1, r.size)
        }
    }

    /** Borra las grabaciones descargadas de la guía (vuelven a sonar por internet). */
    fun borrarDescargas(guia: String) {
        carpeta(guia).deleteRecursively()
        descargas.edit().apply { descargas.all.keys.filter { it.startsWith("$guia/") }.forEach { remove(it) } }.apply()
    }

    fun tieneGrabacion(guia: String, seccion: Int) = _remotos.value.containsKey(clave(guia, seccion)) || incluidos.containsKey(clave(guia, seccion))

    /** Trae del servidor la lista de grabaciones (como mucho cada 10 minutos, salvo [forzar]). */
    suspend fun actualizar(forzar: Boolean = false) {
        if (!supa.configurado) return
        if (!forzar && System.currentTimeMillis() - ultimaCarga < 10 * 60_000) return
        runCatching {
            val filas = supa.select("audios", "select=workbook_id,seccion,url&limit=2000").jsonArray.map { it.jsonObject }
            val mapa = filas.mapNotNull { o ->
                val g = (o["workbook_id"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                val s = (o["seccion"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
                val u = (o["url"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                clave(g, s) to u
            }.toMap()
            _remotos.value = mapa
            prefs.edit().putString("remotos", mapa.entries.joinToString("\n") { "${it.key}\t${it.value}" }).apply()
            ultimaCarga = System.currentTimeMillis()
        }
        _soyAutor.value = supa.sesion.value != null &&
            runCatching { (supa.rpc("es_autor") as? JsonPrimitive)?.booleanOrNull == true }.getOrDefault(false)
    }

    private fun leerGuardados(): Map<String, String> =
        prefs.getString("remotos", null).orEmpty().lines().mapNotNull { l ->
            l.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }.toMap()

    /** Sube la grabación de un capítulo (solo autores). Reemplaza la que hubiera. */
    suspend fun subir(guia: String, seccion: Int, uri: Uri) {
        val (bytes, tipo, ext) = withContext(Dispatchers.IO) {
            val cr = context.contentResolver
            val tam = cr.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
            }
            if (tam != null && tam > MAX_BYTES) throw IllegalArgumentException("muy grande")
            val tipo = cr.getType(uri) ?: "audio/mpeg"
            val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: throw IllegalArgumentException("sin archivo")
            if (bytes.size > MAX_BYTES) throw IllegalArgumentException("muy grande")
            val ext = when {
                "mp4" in tipo || "m4a" in tipo || "aac" in tipo -> "m4a"
                "ogg" in tipo || "opus" in tipo -> "ogg"
                "wav" in tipo -> "wav"
                "webm" in tipo -> "webm"
                else -> "mp3"
            }
            Triple(bytes, tipo, ext)
        }
        // Nombre nuevo en cada subida: así nadie escucha una versión vieja guardada en caché
        val ruta = "$guia/$seccion-${System.currentTimeMillis() / 1000}.$ext"
        val url = supa.subirArchivo("audios", ruta, bytes, tipo)
        supa.upsert("audios", buildJsonObject {
            put("workbook_id", guia); put("seccion", seccion); put("url", url); put("ruta", ruta)
        })
        actualizar(forzar = true)
    }

    /** Quita la grabación del capítulo: vuelve a leerse con la voz del teléfono. */
    suspend fun quitar(guia: String, seccion: Int) {
        supa.delete("audios", "workbook_id=eq.${SupabaseClient.enc(guia)}&seccion=eq.$seccion")
        actualizar(forzar = true)
    }

    companion object {
        const val MAX_BYTES = 50L * 1024 * 1024
        val EXTENSIONES = setOf("mp3", "m4a", "aac", "ogg", "opus", "wav")
    }
}
