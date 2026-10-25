package com.rutaalacima.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.rutaalacima.app.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Foto de perfil y foto de portada de Mi vida.
 * Siempre se guardan en el teléfono; con cuenta, además se suben al bucket público "media" (carpeta propia)
 * para que la web y la comunidad las vean. La foto de perfil también queda como avatar de la comunidad.
 */
class FotosPerfil(
    private val context: Context,
    private val perfil: PerfilRepository,
    private val supa: SupabaseClient,
) {
    enum class Tipo(val archivo: String, val ladoMax: Int) { PERFIL("avatar", 720), PORTADA("portada", 1600) }

    /** Guarda la foto elegida. Devuelve false si no se pudo leer la imagen. */
    suspend fun poner(tipo: Tipo, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val bytes = comprimir(uri, tipo.ladoMax) ?: return@withContext false
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val f = File(dir, "${tipo.archivo}-${System.currentTimeMillis()}.jpg").apply { writeBytes(bytes) }
        val uid = supa.sesion.value?.userId
        // Con cuenta: subir y guardar la URL (con versión, para que nadie vea la foto anterior guardada en caché)
        val url = if (uid != null) runCatching {
            supa.subirFoto("$uid/perfil/${tipo.archivo}.jpg", bytes) + "?v=" + System.currentTimeMillis()
        }.getOrNull() else null
        var anterior = ""
        perfil.actualizar { p ->
            when (tipo) {
                Tipo.PERFIL -> { anterior = p.fotoPerfil; p.copy(fotoPerfil = f.absolutePath, fotoPerfilUrl = url ?: p.fotoPerfilUrl.takeIf { uid != null }.orEmpty()) }
                Tipo.PORTADA -> { anterior = p.fotoPortada; p.copy(fotoPortada = f.absolutePath, fotoPortadaUrl = url ?: p.fotoPortadaUrl.takeIf { uid != null }.orEmpty()) }
            }
        }
        if (anterior.isNotBlank() && anterior != f.absolutePath) runCatching { File(anterior).delete() }
        if (tipo == Tipo.PERFIL && uid != null && url != null) runCatching {
            supa.actualizar("profiles", "id=eq.$uid", buildJsonObject { put("avatar_url", url) })
        }
        true
    }

    /** Quita la foto (en el teléfono y, si hay cuenta, del avatar de la comunidad). */
    suspend fun quitar(tipo: Tipo) = withContext(Dispatchers.IO) {
        var anterior = ""
        perfil.actualizar { p ->
            when (tipo) {
                Tipo.PERFIL -> { anterior = p.fotoPerfil; p.copy(fotoPerfil = "", fotoPerfilUrl = "") }
                Tipo.PORTADA -> { anterior = p.fotoPortada; p.copy(fotoPortada = "", fotoPortadaUrl = "") }
            }
        }
        if (anterior.isNotBlank()) runCatching { File(anterior).delete() }
        val uid = supa.sesion.value?.userId ?: return@withContext
        runCatching { supa.borrarArchivo("media", "$uid/perfil/${tipo.archivo}.jpg") }
        if (tipo == Tipo.PERFIL) runCatching { supa.actualizar("profiles", "id=eq.$uid", buildJsonObject { put("avatar_url", "") }) }
    }

    /** JPEG 85 con el lado mayor en [ladoMax] px como máximo. */
    private fun comprimir(uri: Uri, ladoMax: Int): ByteArray? {
        val cr = context.contentResolver
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        if (o.outWidth <= 0) return null
        var muestra = 1
        while (maxOf(o.outWidth, o.outHeight) / muestra > ladoMax * 2) muestra *= 2
        val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra }) }
            ?: return null
        val e = minOf(1f, ladoMax.toFloat() / maxOf(bmp.width, bmp.height))
        val final = if (e < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * e).toInt(), (bmp.height * e).toInt(), true) else bmp
        return ByteArrayOutputStream().use { final.compress(Bitmap.CompressFormat.JPEG, 85, it); it.toByteArray() }
    }
}

/** Lo que se muestra: la foto del teléfono si existe; si no, la de la cuenta (por ejemplo, puesta desde la web). */
fun fotoVisible(local: String, url: String): String =
    local.takeIf { it.isNotBlank() && File(it).exists() } ?: url
