package com.rutaalacima.app.diagnostico

import android.content.Context
import android.os.Build
import com.rutaalacima.app.BuildConfig
import com.rutaalacima.app.data.remote.SupabaseClient
import com.rutaalacima.app.domain.model.Diagnostico
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.Instant

/**
 * Reporte de errores propio y anónimo. Si la app se cierra por un error, se guarda en el teléfono
 * (filesDir/errores) qué falló, sin mensajes ni datos de la persona, y se envía al abrirla otra vez a la
 * tabla "errores" de Supabase sin la sesión de la cuenta. Se puede apagar en Ajustes.
 */
object Errores {
    private const val CARPETA = "errores"
    private const val PREFS = "diagnostico"
    private const val CLAVE = "enviar"
    private const val MAX_PENDIENTES = 10
    private val json = Json { ignoreUnknownKeys = true }

    fun activo(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CLAVE, true)
    fun configurar(c: Context, si: Boolean) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CLAVE, si).apply()
        if (!si) carpeta(c).listFiles()?.forEach { it.delete() }
    }

    private fun carpeta(c: Context) = File(c.filesDir, CARPETA)

    /** Captura los cierres inesperados y deja que Android siga con su manejo normal. */
    fun instalar(c: Context) {
        val app = c.applicationContext
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
            runCatching { if (activo(app)) guardar(app, error) }
            anterior?.uncaughtException(hilo, error)
        }
    }

    private fun guardar(c: Context, t: Throwable) {
        val dir = carpeta(c).apply { mkdirs() }
        if ((dir.list()?.size ?: 0) >= MAX_PENDIENTES) return
        val fila = buildJsonObject {
            put("origen", "app")
            put("version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            put("sistema", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            put("equipo", "${Build.MANUFACTURER} ${Build.MODEL}".take(80))
            put("firma", Diagnostico.firma(t))
            put("rastro", Diagnostico.rastro(t))
            put("cuando", Instant.now().toString())
        }
        File(dir, "${System.currentTimeMillis()}.json").writeText(fila.toString())
    }

    /** Envía lo pendiente (al abrir la app). Lo que no se puede enviar se reintenta la próxima vez; a los 30 días se borra. */
    fun enviarPendientes(c: Context, supa: SupabaseClient) {
        val app = c.applicationContext
        if (!supa.configurado || !activo(app)) return
        val archivos = carpeta(app).listFiles()?.sortedBy { it.name }.orEmpty()
        if (archivos.isEmpty()) return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val limite = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
            for (f in archivos) {
                if (f.lastModified() < limite) { f.delete(); continue }
                val fila = runCatching { json.parseToJsonElement(f.readText()).jsonObject }.getOrNull()
                if (fila == null || (fila["firma"] as? JsonPrimitive)?.content.isNullOrBlank()) { f.delete(); continue }
                runCatching { supa.insertAnonimo("errores", JsonObject(fila)) }.onSuccess { f.delete() }.onFailure { return@launch }
            }
        }
    }
}
