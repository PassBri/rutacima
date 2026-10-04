package com.rutaalacima.app.data.coach

import android.content.Context
import com.rutaalacima.app.R
import com.rutaalacima.app.data.bancos.BancosRepository
import com.rutaalacima.app.data.local.CoachMensajeEntity
import com.rutaalacima.app.data.local.RutaDatabase
import com.rutaalacima.app.data.local.avance
import com.rutaalacima.app.data.local.puntajes
import com.rutaalacima.app.data.remote.SupabaseClient
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.Fase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.Locale

/**
 * Coach de IA de RutaCima.
 *  - Con servidor: llama a la Edge Function "coach" de Supabase (supabase/functions/coach),
 *    que guarda la clave de la IA en el servidor y responde con el método Ruta a la Cima.
 *  - Sin servidor: un coach basado en reglas del método (ejes débiles, protocolos, bancos).
 * En ambos casos se envía un resumen del contexto del usuario (cumbre, fase, ejes y metas).
 */
class CoachRepository(
    private val context: Context,
    private val db: RutaDatabase,
    private val supa: SupabaseClient,
    private val bancos: BancosRepository,
) {
    val conversacion: Flow<List<CoachMensajeEntity>> = db.coachDao().observe()
    val conIA: Boolean get() = supa.configurado

    suspend fun borrar() = db.coachDao().borrarTodo()

    /** Envía un mensaje al coach, guarda la conversación y devuelve la respuesta. */
    suspend fun enviar(texto: String): String {
        db.coachDao().insert(CoachMensajeEntity(rol = "user", texto = texto.trim()))
        val respuesta = runCatching { if (supa.configurado) remoto() else null }.getOrNull() ?: local(texto)
        db.coachDao().insert(CoachMensajeEntity(rol = "assistant", texto = respuesta))
        return respuesta
    }

    /** Pregunta puntual sin guardar en la conversación (p. ej. "mejorar esta meta"). */
    suspend fun consultar(instruccion: String): String? = runCatching {
        if (!supa.configurado) return null
        val r = supa.funcion("coach", buildJsonObject {
            put("contexto", contexto())
            put("idioma", Locale.getDefault().language)
            put("mensajes", buildJsonArray { add(buildJsonObject { put("role", "user"); put("content", instruccion) }) })
        })
        r.jsonObject["texto"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    private suspend fun remoto(): String? {
        val historial = db.coachDao().ultimos(16).reversed()
        val r = supa.funcion("coach", buildJsonObject {
            put("contexto", contexto())
            put("idioma", Locale.getDefault().language)
            put("mensajes", buildJsonArray {
                historial.forEach { m -> add(buildJsonObject { put("role", m.rol); put("content", m.texto) }) }
            })
        })
        return r.jsonObject["texto"]?.jsonPrimitive?.contentOrNull
    }

    /** Resumen del usuario para que el coach responda con su realidad. */
    suspend fun contexto(): String {
        val perfil = db.perfilDao().get()
        val eval = db.evaluacionDao().observeUltima().first()
        val props = db.planificadorDao().observePropositos().first()
        val anio = LocalDate.now().year
        val anuales = db.planificadorDao().observeMetas(anio).first()
        val hoy = LocalDate.now()
        val mensuales = db.planAnualDao().observeMetasMes(hoy.year, hoy.monthValue).first()
        return buildString {
            perfil?.let {
                appendLine("Nombre: ${it.nombre}")
                if (it.cumbreFrase.isNotBlank()) appendLine("Cumbre personal: ${it.cumbreFrase}")
                Fase.fromName(it.faseActual)?.let { f -> appendLine("Fase actual: ${f.numero}. ${f.nombre}") }
            }
            eval?.let { e ->
                appendLine("Ejes (1-10): " + Eje.entries.zip(e.puntajes()).joinToString { "${it.first.nombre} ${it.second}" })
            }
            if (props.isNotEmpty()) appendLine("Propósitos a 5 años: " + props.joinToString("; ") { "${it.titulo} (${it.progreso}%)" })
            if (anuales.isNotEmpty()) appendLine("Metas $anio: " + anuales.joinToString("; ") { "${it.titulo} (${it.avance}%)" })
            if (mensuales.isNotEmpty()) appendLine("Metas de este mes: " + mensuales.joinToString("; ") { "${it.texto} (${(it.avance() * 100).toInt()}%)" })
        }
    }

    // ------------------------------------------------------------------ Coach sin servidor

    private suspend fun local(texto: String): String {
        val t = texto.lowercase()
        fun tiene(vararg p: String) = p.any { it in t }
        val eval = db.evaluacionDao().observeUltima().first()
        val debil = eval?.let { e -> Eje.entries.zip(e.puntajes()).minByOrNull { it.second }?.first }
        val b = bancos.bancos()
        return when {
            tiene("motiv", "ganas", "cansad", "fuego", "motivation", "tired") -> context.getString(R.string.coach_local_motivacion)
            tiene("rendir", "abandon", "dejar", "quit", "give up") -> context.getString(R.string.coach_local_rendirse)
            tiene("perdid", "confund", "no sé", "no se", "lost", "confus") -> context.getString(R.string.coach_local_perdido)
            tiene("meta", "objetivo", "propósito", "proposito", "goal", "purpose") -> {
                val ejemplo = b.metas.filter { it.area in b.areasSugeridas(debil?.codigo) }.randomOrNull() ?: b.metas.random()
                context.getString(R.string.coach_local_meta, ejemplo.meta, ejemplo.observable, ejemplo.plazo)
            }
            else -> {
                val eje = debil ?: Eje.VOLUNTAD
                val acciones = b.accionesDe(eje.codigo).take(3).joinToString("\n") { "• ${it.texto}" }
                if (eval == null) context.getString(R.string.coach_local_sin_eval)
                else context.getString(R.string.coach_local_eje, context.getString(com.rutaalacima.app.ui.i18n.Textos.nombre(eje)), acciones)
            }
        }
    }
}
