package com.rutaalacima.app.data.web

import android.content.Context
import com.rutaalacima.app.BuildConfig
import com.rutaalacima.app.data.frases.FrasesRepository
import com.rutaalacima.app.data.local.AccionEntity
import com.rutaalacima.app.data.local.AgendaDiaEntity
import com.rutaalacima.app.data.local.BalanceAnualEntity
import com.rutaalacima.app.data.local.ChecklistDiarioEntity
import com.rutaalacima.app.data.local.CoachMensajeEntity
import com.rutaalacima.app.data.local.EvaluacionEjesEntity
import com.rutaalacima.app.data.local.MesEntity
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.PerfilEntity
import com.rutaalacima.app.data.local.PropositoEntity
import com.rutaalacima.app.data.local.PublicacionEntity
import com.rutaalacima.app.data.local.RespuestaEntity
import com.rutaalacima.app.data.local.RutaDatabase
import com.rutaalacima.app.data.local.VisionCasillaEntity
import com.rutaalacima.app.data.social.SocialRepository
import com.rutaalacima.app.data.remote.SupabaseClient
import com.rutaalacima.app.domain.model.Sincronia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import java.time.Instant
import java.time.LocalDate

/** Un computador vinculado a la cuenta. */
data class Dispositivo(val webUid: String, val nombre: String, val ultimoUso: Instant?)

data class EstadoWeb(
    /** Hay al menos un computador vinculado y la ruta se comparte con la cuenta. */
    val activo: Boolean = false,
    val sincronizando: Boolean = false,
    val ultima: Long? = null,
    val error: String? = null,
)

/**
 * Rutaalacima Web: vincular un computador (código QR) y mantener la ruta del teléfono
 * sincronizada con la nube (tabla ruta_datos de Supabase), para que la web y el teléfono
 * trabajen sobre lo mismo. Las reglas de quién gana están en [Sincronia].
 *
 * La ruta solo sube a la nube mientras la web está vinculada ([EstadoWeb.activo]).
 */
class RutaWebRepository(
    context: Context,
    private val db: RutaDatabase,
    private val supabase: SupabaseClient,
    private val frases: FrasesRepository,
    private val social: SocialRepository,
) {
    private val prefs = context.getSharedPreferences("ruta_web", Context.MODE_PRIVATE)
    private val json = supabase.json
    private val candado = Mutex()

    val urlWeb: String = BuildConfig.WEB_URL

    private val _estado = MutableStateFlow(
        EstadoWeb(activo = prefs.getBoolean("activo", false), ultima = prefs.getLong("ultima", 0L).takeIf { it > 0 })
    )
    val estado: StateFlow<EstadoWeb> = _estado.asStateFlow()

    /** Código recibido por enlace (rutacima://vincular?codigo=…) que espera confirmación. */
    val codigoPendiente = MutableStateFlow<String?>(null)

    enum class Falta { SERVIDOR, CUENTA }

    /** Qué falta para poder vincular (null = nada). */
    fun falta(): Falta? = when {
        !supabase.configurado -> Falta.SERVIDOR
        supabase.sesion.value == null -> Falta.CUENTA
        else -> null
    }

    // ------------------------------------------------------------------ Vincular

    /** Aprueba el código que muestra la web. Devuelve el nombre del computador. */
    suspend fun vincular(codigo: String): String {
        val r = supabase.rpc("aprobar_vinculo", buildJsonObject { put("codigo", codigo) })
        val nombre = (r as? JsonPrimitive)?.contentOrNull ?: "Navegador"
        activar(true)
        runCatching { sincronizar() }
        return nombre
    }

    suspend fun dispositivos(): List<Dispositivo> {
        val uid = supabase.sesion.value?.userId ?: return emptyList()
        val filas = supabase.select("dispositivos", "select=web_uid,nombre,ultimo_uso&user_id=eq.$uid&order=ultimo_uso.desc")
        return filas.jsonArray.map { f ->
            val o = f.jsonObject
            Dispositivo(
                webUid = o["web_uid"]!!.jsonPrimitive.content,
                nombre = o["nombre"]?.jsonPrimitive?.contentOrNull ?: "Navegador",
                ultimoUso = o["ultimo_uso"]?.jsonPrimitive?.contentOrNull?.let { runCatching { Instant.parse(it) }.getOrNull() },
            )
        }.also { if (it.isNotEmpty() && !_estado.value.activo) activar(true) }
    }

    suspend fun desvincular(webUid: String) {
        supabase.delete("dispositivos", "web_uid=eq.${SupabaseClient.enc(webUid)}")
    }

    /** Deja de compartir: desvincula todo y borra la ruta de la nube (en el teléfono sigue completa). */
    suspend fun dejarDeCompartir() {
        val uid = supabase.sesion.value?.userId ?: return
        supabase.delete("dispositivos", "user_id=eq.$uid")
        supabase.delete("ruta_datos", "user_id=eq.$uid")
        prefs.edit().remove("huellas").remove("huellas_uid").remove("ultima").apply()
        activar(false)
        _estado.value = _estado.value.copy(ultima = null)
    }

    private fun activar(si: Boolean) {
        prefs.edit().putBoolean("activo", si).apply()
        _estado.value = _estado.value.copy(activo = si)
    }

    // ------------------------------------------------------------------ Sincronizar

    /** Sincroniza si la web está vinculada y hay sesión. No lanza errores (quedan en [estado]). */
    suspend fun sincronizarSiActivo() {
        if (_estado.value.activo && supabase.configurado && supabase.sesion.value != null) runCatching { sincronizar() }
    }

    /**
     * Sincroniza cuando cambia algo en el teléfono (con una pausa de 3 s para juntar cambios).
     * Se inicia una vez, en el scope de la aplicación.
     */
    @OptIn(FlowPreview::class)
    fun iniciarAutomatica(scope: CoroutineScope) {
        scope.launch {
            merge(
                db.perfilDao().observe().map { },
                db.planificadorDao().observePropositos().map { },
                db.planificadorDao().observeTodasLasMetas().map { },
                db.planAnualDao().observeTodasMetasMes().map { },
                db.checklistDao().observeDesde(desde().toString()).map { },
                db.evaluacionDao().observeTodas().map { },
                db.respuestaDao().observeUltima().map { },
                db.coachDao().observe().map { },
                db.socialDao().observePublicaciones().map { },
                db.visionDao().observe().map { },
                frases.desbloqueadas.map { },
                _estado.map { it.activo },
            ).debounce(3_000).collect { sincronizarSiActivo() }
        }
    }

    /** Ventana de lo diario (checklist y agenda): desde el 1 de enero del año pasado. */
    private fun desde(): LocalDate = LocalDate.now().minusYears(1).withDayOfYear(1)

    private fun enVentana(k: String, desde: String) =
        Sincronia.tipoDe(k) !in DIARIOS || k.substringAfter('/') >= desde

    suspend fun sincronizar() = candado.withLock {
        val uid = supabase.sesion.value?.userId ?: return@withLock
        _estado.value = _estado.value.copy(sincronizando = true, error = null)
        try {
            val desde = desde().toString()
            val locales = locales(desde)
            val remotos = remotos(uid, desde)
            val huellas = Sincronia.huellasConfiables(remotos, huellas(uid))
            val plan = Sincronia.planear(locales, remotos, huellas, { enVentana(it, desde) }, Sincronia::fusionar)
            val fallidas = mutableSetOf<String>()

            // 1. Lo que cambió en la web → teléfono (padres antes que hijos)
            plan.aplicar.entries.sortedBy { ORDEN.indexOf(Sincronia.tipoDe(it.key)) }.forEach { (k, v) ->
                if (runCatching { aplicar(k, v) }.isFailure) fallidas += k
            }
            if (!Sincronia.borradoSospechoso(plan, locales.size)) {
                plan.borrarLocal.sortedByDescending { ORDEN.indexOf(Sincronia.tipoDe(it)) }.forEach { k ->
                    if (runCatching { borrarLocal(k) }.isFailure) fallidas += k
                }
            } else fallidas += plan.borrarLocal

            // 2. Lo que cambió en el teléfono → nube
            if (plan.subir.isNotEmpty()) {
                val ahora = Instant.now().toString()
                plan.subir.entries.chunked(200).forEach { lote ->
                    supabase.upsert("ruta_datos", buildJsonArray {
                        lote.forEach { (k, v) ->
                            add(buildJsonObject {
                                put("user_id", uid)
                                put("tipo", Sincronia.tipoDe(k))
                                put("clave", k.substringAfter('/'))
                                put("datos", json.parseToJsonElement(v))
                                put("actualizado", ahora)
                            })
                        }
                    })
                }
            }
            plan.borrar.forEach { k ->
                supabase.delete("ruta_datos", "user_id=eq.$uid&tipo=eq.${Sincronia.tipoDe(k)}&clave=eq.${SupabaseClient.enc(k.substringAfter('/'))}")
            }
            guardarHuellas(uid, huellas, plan.huellas.filterKeys { it !in fallidas })

            // 3. Publicaciones: las del teléfono suben a la comunidad y las hechas en la web bajan
            runCatching { publicaciones(uid) }

            val ahora = System.currentTimeMillis()
            prefs.edit().putLong("ultima", ahora).apply()
            _estado.value = _estado.value.copy(sincronizando = false, ultima = ahora)
        } catch (e: Exception) {
            _estado.value = _estado.value.copy(sincronizando = false, error = e.message)
            throw e
        }
    }

    // ---------- Documentos del teléfono (cada fila de Room como JSON, con los mismos nombres)

    private inline fun <reified T> doc(e: T): String = Sincronia.canonico(json.encodeToJsonElement(serializer<T>(), e))
    private inline fun <reified T> de(v: String): T = json.decodeFromString(serializer<T>(), v)

    private suspend fun locales(desde: String): Map<String, String> {
        val d = db.sincroniaDao()
        val m = mutableMapOf<String, String>()
        d.perfiles().forEach { m["perfil/${it.id}"] = doc(it) }
        d.propositos().forEach { m["proposito/${it.id}"] = doc(it) }
        d.acciones().forEach { m["accion/${it.id}"] = doc(it) }
        d.metasAnuales().forEach { m["meta_anio/${it.id}"] = doc(it) }
        d.metasMensuales().forEach { m["meta_mes/${it.id}"] = doc(it) }
        d.balances().forEach { m["balance/${it.anio}"] = doc(it) }
        d.agenda(desde).forEach { m["agenda/${it.fecha}"] = doc(it) }
        d.meses().forEach { m["mes/${it.clave}"] = doc(it) }
        d.checklist(desde).forEach { m["checklist/${it.fecha}"] = doc(it) }
        d.evaluaciones().forEach { m["ejes/${it.id}"] = doc(it) }
        d.respuestas().forEach { m["respuesta/${it.clave}"] = doc(it) }
        d.coach().forEach { m["coach/${it.id}"] = doc(it) }
        d.vision().forEach { m["vision/${it.id}"] = doc(it) }
        for (anio in LocalDate.parse(desde).year..LocalDate.now().year) {
            val dias = frases.abiertasDe(anio)
            if (dias.isNotEmpty()) m["frases/$anio"] = Sincronia.canonico(buildJsonObject {
                put("dias", JsonArray(dias.sorted().map { JsonPrimitive(it) }))
            })
        }
        return m
    }

    // ---------- Documentos de la nube

    private suspend fun remotos(uid: String, desde: String): Map<String, String> {
        val m = mutableMapOf<String, String>()
        var desplazamiento = 0
        while (true) {
            val filas = supabase.select(
                "ruta_datos",
                "select=tipo,clave,datos&user_id=eq.$uid" +
                    "&or=(and(tipo.neq.checklist,tipo.neq.agenda),clave.gte.$desde)" +
                    "&order=tipo,clave&limit=500&offset=$desplazamiento",
            ).jsonArray
            filas.forEach { f ->
                val o = f.jsonObject
                m["${o["tipo"]!!.jsonPrimitive.content}/${o["clave"]!!.jsonPrimitive.content}"] = Sincronia.canonico(o["datos"]!!)
            }
            if (filas.size < 500) break
            desplazamiento += filas.size
        }
        return m
    }

    // ---------- Cambios hechos en la web → teléfono

    private suspend fun aplicar(k: String, v: String) {
        val d = db.sincroniaDao()
        when (Sincronia.tipoDe(k)) {
            "perfil" -> d.guardar(de<PerfilEntity>(v))
            "proposito" -> d.guardar(de<PropositoEntity>(v))
            "accion" -> d.guardar(de<AccionEntity>(v))
            "meta_anio" -> d.guardar(de<MetaAnualEntity>(v))
            "meta_mes" -> d.guardar(de<MetaMensualEntity>(v))
            "balance" -> d.guardar(de<BalanceAnualEntity>(v))
            "agenda" -> d.guardar(de<AgendaDiaEntity>(v))
            "mes" -> d.guardar(de<MesEntity>(v))
            "checklist" -> d.guardar(de<ChecklistDiarioEntity>(v))
            "ejes" -> d.guardar(de<EvaluacionEjesEntity>(v))
            "respuesta" -> d.guardar(de<RespuestaEntity>(v))
            "coach" -> d.guardar(de<CoachMensajeEntity>(v))
            "vision" -> d.guardar(de<VisionCasillaEntity>(v))
            "frases" -> {
                val anio = k.substringAfter('/').toIntOrNull() ?: return
                val dias = (json.parseToJsonElement(v).jsonObject["dias"] as? JsonArray).orEmpty()
                    .mapNotNull { (it as? JsonPrimitive)?.intOrNull }.filter { it in 0..364 }.toSet()
                frases.guardarAbiertas(anio, dias)
            }
        }
    }

    private suspend fun borrarLocal(k: String) {
        val d = db.sincroniaDao()
        val c = k.substringAfter('/')
        when (Sincronia.tipoDe(k)) {
            "proposito" -> c.toLongOrNull()?.let { d.borrarProposito(it) }
            "accion" -> c.toLongOrNull()?.let { d.borrarAccion(it) }
            "meta_anio" -> c.toLongOrNull()?.let { d.borrarMetaAnual(it) }
            "meta_mes" -> c.toLongOrNull()?.let { d.borrarMetaMensual(it) }
            "balance" -> c.toIntOrNull()?.let { d.borrarBalance(it) }
            "agenda" -> d.borrarAgenda(c)
            "mes" -> d.borrarMes(c)
            "checklist" -> d.borrarChecklist(c)
            "ejes" -> c.toLongOrNull()?.let { d.borrarEvaluacion(it) }
            "respuesta" -> d.borrarRespuesta(c)
            "coach" -> c.toLongOrNull()?.let { d.borrarCoach(it) }
            "vision" -> c.toLongOrNull()?.let { d.borrarVision(it) }
            "frases" -> c.toIntOrNull()?.let { frases.guardarAbiertas(it, emptySet()) }
            else -> {}   // el perfil no se borra
        }
    }

    // ---------- Publicaciones (tabla posts de la comunidad)

    private suspend fun publicaciones(uid: String) {
        val dao = db.socialDao()
        // Subir las que solo están en el teléfono (también las privadas: solo tú las ves)
        dao.todas().filter { it.remoteId == null }.forEach { runCatching { social.subir(it) } }
        val remotas = supabase.select(
            "posts", "select=id,tipo,texto,image_url,eje,anio,visibilidad,meta_titulo,created_at&user_id=eq.$uid&limit=1000",
        ).jsonArray.map { it.jsonObject }
        val locales = dao.todas()
        val idsRemotos = remotas.map { it["id"]!!.jsonPrimitive.content }.toSet()
        // Bajar las publicadas desde la web
        val conocidas = locales.flatMap { listOfNotNull(it.id, it.remoteId) }.toSet()
        remotas.filter { it["id"]!!.jsonPrimitive.content !in conocidas }.forEach { o ->
            fun s(c: String) = o[c]?.jsonPrimitive?.contentOrNull.orEmpty()
            val id = s("id")
            dao.upsert(
                PublicacionEntity(
                    id = id, tipo = s("tipo"), texto = s("texto"), eje = s("eje").ifBlank { null },
                    anio = s("anio").toIntOrNull() ?: LocalDate.now().year, visibilidad = s("visibilidad").ifBlank { "PUBLICA" },
                    metaTitulo = s("meta_titulo"), remoteId = id, fotoUrl = s("image_url"),
                    creadaEn = runCatching { java.time.OffsetDateTime.parse(s("created_at")).toInstant().toEpochMilli() }
                        .getOrDefault(System.currentTimeMillis()),
                )
            )
        }
        // Quitar del teléfono las que se borraron en la web
        val subidas = locales.filter { it.remoteId != null }
        val borradas = subidas.filter { it.remoteId !in idsRemotos }
        if (borradas.size <= 10 || borradas.size * 2 <= subidas.size) borradas.forEach { dao.delete(it.id) }
    }

    private companion object {
        /** Lo diario solo se sincroniza dentro de la ventana [desde]. */
        val DIARIOS = setOf("checklist", "agenda")
        /** Orden para aplicar: primero los padres (propósito antes que sus acciones). */
        val ORDEN = listOf("perfil", "proposito", "accion", "meta_anio", "meta_mes", "balance", "mes",
            "agenda", "checklist", "ejes", "respuesta", "coach", "vision", "frases")
    }

    // ---------- Huellas de la última sincronización

    private fun huellas(uid: String): Map<String, String> {
        if (prefs.getString("huellas_uid", null) != uid) return emptyMap()   // otra cuenta: empezar de cero
        val texto = prefs.getString("huellas", null) ?: return emptyMap()
        return runCatching {
            json.parseToJsonElement(texto).jsonObject.mapValues { it.value.jsonPrimitive.content }
        }.getOrDefault(emptyMap())
    }

    private fun guardarHuellas(uid: String, antes: Map<String, String>, cambios: Map<String, String?>) {
        val nuevo = antes.toMutableMap()
        cambios.forEach { (k, h) -> if (h == null) nuevo.remove(k) else nuevo[k] = h }
        val texto = JsonObject(nuevo.mapValues { JsonPrimitive(it.value) }).toString()
        prefs.edit().putString("huellas", texto).putString("huellas_uid", uid).apply()
    }
}
