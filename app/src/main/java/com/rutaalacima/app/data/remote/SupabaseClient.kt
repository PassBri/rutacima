package com.rutaalacima.app.data.remote

import android.content.Context
import com.rutaalacima.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Sesión de Supabase guardada en el teléfono. */
@Serializable
data class Sesion(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiraEn: Long,
)

class SupabaseException(val codigo: Int, mensaje: String) : IOException(mensaje)

/**
 * Cliente mínimo de Supabase sobre HttpURLConnection (sin dependencias extra):
 * Auth (correo y contraseña), PostgREST (tablas), Storage (fotos) y Edge Functions (coach de IA).
 *
 * La URL y la clave pública se configuran en local.properties (ver README). Sin ellas,
 * [configurado] es false y la app funciona en modo demo, con todo guardado en el teléfono.
 */
class SupabaseClient(context: Context) {
    private val url = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val prefs = context.getSharedPreferences("rutacima_sesion", Context.MODE_PRIVATE)
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

    val configurado: Boolean get() = url.startsWith("https://") && anonKey.isNotBlank()

    private val _sesion = MutableStateFlow(cargarSesion())
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    private fun cargarSesion(): Sesion? =
        prefs.getString("sesion", null)?.let { runCatching { json.decodeFromString(Sesion.serializer(), it) }.getOrNull() }

    private fun guardarSesion(s: Sesion?) {
        prefs.edit().apply { if (s == null) remove("sesion") else putString("sesion", json.encodeToString(Sesion.serializer(), s)) }.apply()
        _sesion.value = s
    }

    // ------------------------------------------------------------------ HTTP

    private fun peticion(
        metodo: String,
        ruta: String,
        cuerpo: ByteArray? = null,
        tipo: String = "application/json",
        headers: Map<String, String> = emptyMap(),
        token: String? = _sesion.value?.accessToken,
    ): String {
        val con = (URL("$url$ruta").openConnection() as HttpURLConnection).apply {
            requestMethod = metodo
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Authorization", "Bearer ${token ?: anonKey}")
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            if (cuerpo != null) {
                doOutput = true
                setRequestProperty("Content-Type", tipo)
                outputStream.use { it.write(cuerpo) }
            }
        }
        try {
            val codigo = con.responseCode
            val texto = (if (codigo in 200..299) con.inputStream else con.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (codigo !in 200..299) throw SupabaseException(codigo, mensajeDeError(texto, codigo))
            return texto
        } finally {
            con.disconnect()
        }
    }

    private fun mensajeDeError(texto: String, codigo: Int): String = runCatching {
        val o = json.parseToJsonElement(texto).jsonObject
        (o["msg"] ?: o["message"] ?: o["error_description"] ?: o["error"])?.jsonPrimitive?.contentOrNull
    }.getOrNull() ?: "Error $codigo"

    /** Renueva el token si está por vencer. */
    private suspend fun asegurarSesion() {
        val s = _sesion.value ?: return
        if (System.currentTimeMillis() / 1000 < s.expiraEn - 60) return
        runCatching { autenticar("refresh_token", """{"refresh_token":"${s.refreshToken}"}""", s.email) }
            .onFailure { if (it is SupabaseException && it.codigo in 400..499) guardarSesion(null) }
    }

    // ------------------------------------------------------------------ Auth

    private suspend fun autenticar(grant: String, cuerpo: String, email: String): Sesion = withContext(Dispatchers.IO) {
        val r = json.parseToJsonElement(peticion("POST", "/auth/v1/token?grant_type=$grant", cuerpo.toByteArray(), token = null)).jsonObject
        val s = Sesion(
            accessToken = r["access_token"]!!.jsonPrimitive.content,
            refreshToken = r["refresh_token"]!!.jsonPrimitive.content,
            userId = r["user"]!!.jsonObject["id"]!!.jsonPrimitive.content,
            email = email,
            expiraEn = r["expires_at"]?.jsonPrimitive?.long ?: (System.currentTimeMillis() / 1000 + 3600),
        )
        guardarSesion(s)
        s
    }

    suspend fun iniciarSesion(email: String, clave: String): Sesion =
        autenticar("password", """{"email":${q(email)},"password":${q(clave)}}""", email)

    /** Crea la cuenta. El perfil (username, nombre) lo crea un trigger en la base (ver supabase/schema.sql). */
    suspend fun registrarse(email: String, clave: String, usuario: String, nombre: String, versionLegal: String): Sesion? = withContext(Dispatchers.IO) {
        // versionLegal: documentos legales aceptados; un trigger guarda la prueba de la autorización (Ley 1581)
        val cuerpo = """{"email":${q(email)},"password":${q(clave)},"data":{"username":${q(usuario)},"nombre":${q(nombre)},"legal_version":${q(versionLegal)}}}"""
        val r = json.parseToJsonElement(peticion("POST", "/auth/v1/signup", cuerpo.toByteArray(), token = null)).jsonObject
        if (r["access_token"] != null) iniciarSesion(email, clave) else null // null = debe confirmar el correo
    }

    fun cerrarSesion() = guardarSesion(null)

    /** Texto como literal JSON (con comillas y escapes). */
    private fun q(s: String) = json.encodeToString(String.serializer(), s)

    // ------------------------------------------------------------------ PostgREST

    suspend fun select(tabla: String, consulta: String): JsonElement = withContext(Dispatchers.IO) {
        asegurarSesion()
        json.parseToJsonElement(peticion("GET", "/rest/v1/$tabla?$consulta"))
    }

    suspend fun insert(tabla: String, fila: JsonElement, devolver: Boolean = true): JsonElement? = withContext(Dispatchers.IO) {
        asegurarSesion()
        val r = peticion(
            "POST", "/rest/v1/$tabla", json.encodeToString(JsonElement.serializer(), fila).toByteArray(),
            headers = mapOf("Prefer" to if (devolver) "return=representation" else "return=minimal"),
        )
        if (devolver && r.isNotBlank()) json.parseToJsonElement(r) else null
    }

    suspend fun delete(tabla: String, filtro: String) = withContext(Dispatchers.IO) {
        asegurarSesion()
        peticion("DELETE", "/rest/v1/$tabla?$filtro")
        Unit
    }

    suspend fun upsert(tabla: String, fila: JsonElement) = withContext(Dispatchers.IO) {
        asegurarSesion()
        peticion(
            "POST", "/rest/v1/$tabla", json.encodeToString(JsonElement.serializer(), fila).toByteArray(),
            headers = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        )
        Unit
    }

    /** Llama una función de la base (POST /rest/v1/rpc/nombre). Devuelve JsonNull si no hay cuerpo. */
    suspend fun rpc(nombre: String, args: JsonElement = kotlinx.serialization.json.JsonObject(emptyMap())): JsonElement =
        withContext(Dispatchers.IO) {
            asegurarSesion()
            val r = peticion("POST", "/rest/v1/rpc/$nombre", json.encodeToString(JsonElement.serializer(), args).toByteArray())
            if (r.isBlank()) kotlinx.serialization.json.JsonNull else json.parseToJsonElement(r)
        }

    // ------------------------------------------------------------------ Storage

    /** Sube una foto al bucket público y devuelve su URL. */
    suspend fun subirFoto(rutaEnBucket: String, bytes: ByteArray, bucket: String = "media"): String = withContext(Dispatchers.IO) {
        asegurarSesion()
        peticion("POST", "/storage/v1/object/$bucket/$rutaEnBucket", bytes, tipo = "image/jpeg", headers = mapOf("x-upsert" to "true"))
        "$url/storage/v1/object/public/$bucket/$rutaEnBucket"
    }

    /** Sube cualquier archivo (por ejemplo, una grabación) al bucket público y devuelve su URL. */
    suspend fun subirArchivo(bucket: String, rutaEnBucket: String, bytes: ByteArray, tipo: String): String = withContext(Dispatchers.IO) {
        asegurarSesion()
        peticion("POST", "/storage/v1/object/$bucket/$rutaEnBucket", bytes, tipo = tipo, headers = mapOf("x-upsert" to "true"))
        "$url/storage/v1/object/public/$bucket/$rutaEnBucket"
    }

    // ------------------------------------------------------------------ Edge Functions

    suspend fun funcion(nombre: String, cuerpo: JsonElement): JsonElement = withContext(Dispatchers.IO) {
        asegurarSesion()
        json.parseToJsonElement(peticion("POST", "/functions/v1/$nombre", json.encodeToString(JsonElement.serializer(), cuerpo).toByteArray()))
    }

    companion object {
        fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
    }
}
