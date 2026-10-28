package com.rutaalacima.app.ui.planner

import com.rutaalacima.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.diasMarcados
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.data.local.AgendaDiaEntity
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.util.formatoDia
import com.rutaalacima.app.util.hoy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** Franjas de "Planifique su día": de 4:00 am a 3:00 am. */
val HORAS_DEL_DIA: List<Int> = (4..23).toList() + (0..3).toList()

/** 24 h: "13:00"; 12 h: "1:00 pm" (la de siempre en la app). */
fun etiquetaHora(h: Int, formato24: Boolean): String = if (formato24) "%02d:00".format(h) else etiquetaHora(h)

fun etiquetaHora(h: Int): String = when {
    h == 0 -> "12:00 am"
    h < 12 -> "$h:00 am"
    h == 12 -> "12:00 pm"
    else -> "${h - 12}:00 pm"
}

private val horarioSerializer = MapSerializer(String.serializer(), String.serializer())
private val json = Json { ignoreUnknownKeys = true }

fun AgendaDiaEntity.horarioMapa(): Map<Int, String> =
    runCatching { json.decodeFromString(horarioSerializer, horario) }.getOrDefault(emptyMap())
        .mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()

/** Workbook de respuestas del día (el mismo que usa la web): "dia#<fecha>#top", "dia#<fecha>#<h>#meta|hecho", "dia#<fecha>#traido". */
const val WB_DIA = "dia"

/** Una de las 3 cumbres del día (método Ivy Lee): texto, meta del mes que mueve (id) y si quedó hecha. */
@Serializable
data class TopDia(val t: String = "", val meta: String = "", val hecho: Boolean = false)

private val topsSerializer = ListSerializer(TopDia.serializer())

fun leerTops(v: String?): List<TopDia> =
    if (v.isNullOrBlank()) emptyList() else runCatching { json.decodeFromString(topsSerializer, v) }.getOrDefault(emptyList()).take(3)

/** Lo que quedó sin hacer un día: sus cumbres no hechas y sus actividades no hechas (hora, texto, meta). */
data class Pendientes(val tops: List<TopDia>, val acts: List<Triple<Int, String, String>>) {
    val total get() = tops.size + acts.size
}

class AgendaViewModel(private val c: AppContainer) : ViewModel() {
    var fecha by mutableStateOf(hoy())
        private set
    var dia by mutableStateOf<AgendaDiaEntity?>(null)
        private set
    private var carga: Job? = null
    private var guardado: Job? = null

    /** Respuestas del día (cumbres, metas de cada hora, hechas), compartidas con la web. */
    val resp: StateFlow<Map<String, String>> = c.respuestas.observar(WB_DIA).stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val metasMes: StateFlow<List<MetaMensualEntity>> = c.planAnual.todasMetasMes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** Para unir cada acción a una sub-meta (paso de la Brújula del año) y saber cuánto aporta (igual que la web). */
    val anuales: StateFlow<List<com.rutaalacima.app.data.local.MetaAnualEntity>> = c.planificador.todasLasMetas.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val casillas: StateFlow<List<com.rutaalacima.app.data.local.VisionCasillaEntity>> = c.db.visionDao().observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val mandala: StateFlow<Map<String, String>> = c.respuestas.observar(com.rutaalacima.app.domain.model.Mandala.WORKBOOK).stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    fun casillaDeMes(m: MetaMensualEntity?) = com.rutaalacima.app.data.vision.SincroBrujula.casillaDe(
        m?.metaAnualId?.let { id -> anuales.value.firstOrNull { it.id == id } }, anuales.value, casillas.value)
    /** Las sub-metas que puede mover una acción: las escogidas para el mes (o todas si la meta va completa), con su aporte en %. */
    fun pasosDe(m: MetaMensualEntity): List<Triple<Int, String, Float>> {
        val cas = casillaDeMes(m) ?: return emptyList(); val r = mandala.value
        val elegidos = com.rutaalacima.app.data.vision.LibroMetas.pasosDelMes(m.id)
        return (0 until com.rutaalacima.app.domain.model.Mandala.PASOS).mapNotNull { p ->
            val t = r[com.rutaalacima.app.domain.model.Mandala.clave(cas.id, p)].orEmpty().trim()
            if (t.isEmpty() || (elegidos.isNotEmpty() && p !in elegidos)) null
            else Triple(p, t, com.rutaalacima.app.data.vision.SincroBrujula.aporte(cas.id, p, r))
        }
    }
    /** Lo que aportaron hoy las actividades y cumbres hechas, en %. */
    fun aporteDelDia(f: LocalDate, r: Map<String, String>): Float {
        val vs = (0 until 24).filter { r["dia#$f#$it#hecho"] == "1" }.map { r["dia#$f#$it#meta"].orEmpty() } + tops(f, r).filter { it.hecho }.map { it.meta }
        return vs.sumOf { v -> val (id, p) = (v.split('|') + "").let { it[0] to it[1].toIntOrNull() }
            if (p == null) 0.0 else casillaDeMes(metasMes.value.firstOrNull { it.id.toString() == id })?.let {
                com.rutaalacima.app.data.vision.SincroBrujula.aporte(it.id, p, mandala.value).toDouble() } ?: 0.0 }.toFloat()
    }

    /** Lo escrito y aún no guardado (para que el campo no salte mientras se escribe). */
    private val topsLocales = mutableStateMapOf<String, List<TopDia>>()
    private val guardadoTops = mutableMapOf<String, Job>()

    init {
        carga = viewModelScope.launch { dia = c.agenda.dia(fecha) }
    }

    fun abrir(f: LocalDate) {
        if (f == fecha && dia != null) return
        guardarYa()
        fecha = f
        dia = null
        carga?.cancel()
        carga = viewModelScope.launch { dia = c.agenda.dia(f) }
    }

    fun editar(transform: (AgendaDiaEntity) -> AgendaDiaEntity) {
        val actual = dia ?: return
        val nuevo = transform(actual)
        dia = nuevo
        guardado?.cancel()
        guardado = c.appScope.launch {
            delay(500)
            c.agenda.guardar(nuevo)
        }
    }

    fun hora(h: Int, texto: String) = editar { d ->
        val mapa = d.horarioMapa().toMutableMap()
        if (texto.isBlank()) mapa.remove(h) else mapa[h] = texto
        d.copy(horario = json.encodeToString(horarioSerializer, mapa.mapKeys { it.key.toString() }))
    }

    // ---------------------------------------------------------------- Las 3 cumbres del día

    fun tops(f: LocalDate, r: Map<String, String>): List<TopDia> = topsLocales[f.toString()] ?: leerTops(r["dia#$f#top"])

    fun guardarTops(f: LocalDate, xs: List<TopDia>, inmediato: Boolean = false) {
        val k = f.toString()
        topsLocales[k] = xs
        guardadoTops[k]?.cancel()
        guardadoTops[k] = c.appScope.launch {
            if (!inmediato) delay(600)
            val l = xs.take(3).map { it.copy(t = it.t.take(160)) }.filter { it.t.isNotBlank() }
            c.respuestas.guardar(WB_DIA, "dia#$k#top", if (l.isEmpty()) "" else json.encodeToString(topsSerializer, l))
            // La primera también va a "prioridad", el campo que ya existía.
            val p = l.firstOrNull()?.t.orEmpty()
            if (f == fecha) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { if (dia?.prioridad != p) editar { it.copy(prioridad = p) } }
            else c.agenda.dia(f).let { if (it.prioridad != p) c.agenda.guardar(it.copy(prioridad = p)) }
        }
    }

    fun topTexto(f: LocalDate, r: Map<String, String>, i: Int, t: String) {
        val xs = tops(f, r).toMutableList(); while (xs.size <= i) xs.add(TopDia())
        xs[i] = xs[i].copy(t = t); guardarTops(f, xs)
    }

    fun topMeta(f: LocalDate, r: Map<String, String>, i: Int, meta: String) {
        val xs = tops(f, r).toMutableList(); if (i >= xs.size) return
        xs[i] = xs[i].copy(meta = meta); guardarTops(f, xs, true)
        if (xs[i].hecho && meta.isNotBlank()) marcarMeta(meta, f)
    }

    /** Devuelve true si la cumbre quedó hecha y movió una meta del mes. */
    fun topHecha(f: LocalDate, r: Map<String, String>, i: Int, si: Boolean): Boolean {
        val xs = tops(f, r).toMutableList(); if (i >= xs.size) return false
        xs[i] = xs[i].copy(hecho = si); guardarTops(f, xs, true)
        return if (si && xs[i].meta.isNotBlank() && !f.isAfter(hoy())) { marcarMeta(xs[i].meta, f); true } else false
    }

    /** Marca el día en una meta del mes (sin desmarcar si ya estaba). */
    fun marcarMeta(id: String, f: LocalDate) {
        if (f.isAfter(hoy())) return  // un día que aún no llega no cuenta en la meta
        // "id" es la meta del mes; "id|paso" además suma una jornada a esa sub-meta de la Brújula
        val partes = id.split('|')
        val m = metasMes.value.firstOrNull { it.id.toString() == partes[0] && it.anio == f.year && it.mes == f.monthValue } ?: return
        if (f.dayOfMonth !in m.diasMarcados()) viewModelScope.launch { c.planAnual.alternarDia(m, f.dayOfMonth) }
        val p = partes.getOrNull(1)?.toIntOrNull(); val cas = casillaDeMes(m)
        if (p != null && cas != null) viewModelScope.launch { jornadaPaso(c, cas.id, p, f) }
    }

    // ---------------------------------------------------------------- Actividades por hora unidas a metas

    fun actMeta(h: Int, meta: String, r: Map<String, String>) {
        val f = fecha
        viewModelScope.launch { c.respuestas.guardar(WB_DIA, "dia#$f#$h#meta", meta) }
        if (meta.isNotBlank() && r["dia#$f#$h#hecho"] == "1") marcarMeta(meta, f)
    }

    fun actHecha(h: Int, si: Boolean, r: Map<String, String>) {
        val f = fecha
        viewModelScope.launch { c.respuestas.guardar(WB_DIA, "dia#$f#$h#hecho", if (si) "1" else "") }
        val meta = r["dia#$f#$h#meta"].orEmpty()
        if (si && meta.isNotBlank()) marcarMeta(meta, f)
    }

    // ---------------------------------------------------------------- Pendientes que pasan de un día a otro

    suspend fun pendientes(desde: LocalDate, r: Map<String, String>): Pendientes {
        val ag = if (desde == fecha) dia ?: c.agenda.dia(desde) else c.agenda.dia(desde)
        val acts = ag.horarioMapa().filter { (h, t) -> t.isNotBlank() && r["dia#$desde#$h#hecho"] != "1" }
            .map { (h, t) -> Triple(h, t, r["dia#$desde#$h#meta"].orEmpty()) }.sortedBy { it.first }
        return Pendientes(tops(desde, r).filter { !it.hecho && it.t.isNotBlank() }, acts)
    }

    /** Pasa lo pendiente de [desde] a [hasta]: cumbres primero (hasta 3) y actividades a su misma hora si está libre. */
    fun pasar(desde: LocalDate, hasta: LocalDate, r: Map<String, String>, alTerminar: (Int) -> Unit) = viewModelScope.launch {
        val p = pendientes(desde, r)
        val xs = tops(hasta, r).toMutableList()
        p.tops.forEach { x -> if (xs.size < 3 && xs.none { it.t == x.t }) xs.add(x.copy(hecho = false)) }
        guardarTops(hasta, xs, true)
        val mover: (AgendaDiaEntity) -> AgendaDiaEntity = { d ->
            val mapa = d.horarioMapa().toMutableMap()
            p.acts.forEach { (h, t, _) -> if (mapa[h].isNullOrBlank()) mapa[h] = t }
            d.copy(horario = json.encodeToString(horarioSerializer, mapa.mapKeys { it.key.toString() }))
        }
        if (hasta == fecha) editar(mover) else c.agenda.guardar(mover(c.agenda.dia(hasta)))
        p.acts.forEach { (h, _, meta) -> if (meta.isNotBlank()) c.respuestas.guardar(WB_DIA, "dia#$hasta#$h#meta", meta) }
        c.respuestas.guardar(WB_DIA, "dia#$hasta#traido", "1")
        alTerminar(p.total)
    }

    fun soltar(f: LocalDate) = viewModelScope.launch { c.respuestas.guardar(WB_DIA, "dia#$f#traido", "1") }

    private fun guardarYa() {
        val d = dia ?: return
        if (guardado?.isActive == true) {
            guardado?.cancel()
            c.appScope.launch { c.agenda.guardar(d) }
        }
    }

    override fun onCleared() {
        guardarYa()
    }
}

/**
 * Planificador diario, igual que en la web: el plan de la mañana (intención y las 3 cumbres del día, unidas a metas del mes),
 * las actividades por hora (cada una puede mover una meta) y el cierre, que deja escritas las 3 de mañana.
 */
@Composable
fun DayPlannerTab(vm: AgendaViewModel, bottom: Dp) {
    val d = vm.dia
    val r by vm.resp.collectAsStateWithLifecycle()
    val todas by vm.metasMes.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val aviso: (String) -> Unit = { com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(it) }
    val f = vm.fecha
    val metas = todas.filter { it.anio == f.year && it.mes == f.monthValue }
    val manana = f.plusDays(1)
    val metasManana = todas.filter { it.anio == manana.year && it.mes == manana.monthValue }
    // Formato de las horas (12 h o 24 h), recordado en el teléfono; al cambiarlo, las horas giran como una tablilla
    val prefs = remember { ctx.getSharedPreferences("suroute_ui", android.content.Context.MODE_PRIVATE) }
    var formato24 by remember { mutableStateOf(prefs.getBoolean("hora24", false)) }
    val giro = remember { androidx.compose.animation.core.Animatable(0f) }
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    val reducir = !android.animation.ValueAnimator.areAnimatorsEnabled()
    val cambiarFormato: () -> Unit = {
        alcance.launch {
            if (!reducir) giro.animateTo(90f, androidx.compose.animation.core.tween(150))
            formato24 = !formato24; prefs.edit().putBoolean("hora24", formato24).apply()
            if (!reducir) { giro.snapTo(-90f); giro.animateTo(0f, androidx.compose.animation.core.tween(170, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.abrir(f.minusDays(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.dia_anterior)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatoDia(f), style = MaterialTheme.typography.titleMedium)
                    if (f != hoy()) {
                        TextButton(onClick = { vm.abrir(hoy()) }) { Text(stringResource(R.string.ir_a_hoy)) }
                    }
                }
                IconButton(onClick = { vm.abrir(f.plusDays(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.dia_siguiente)) }
            }
        }
        if (d == null) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { com.rutaalacima.app.ui.components.CargandoMarca() } }
            return@LazyColumn
        }
        // 1. El plan de la mañana
        item(key = "plan") {
            RutaCard {
                Text(stringResource(R.string.plan_dia), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.plan_dia_ayuda), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                CampoDia(stringResource(R.string.intencion_dia), d.intencion) { v -> vm.editar { it.copy(intencion = v) } }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.tres_cumbres_hoy), style = MaterialTheme.typography.labelLarge)
                val tops = vm.tops(f, r)
                (0..2).forEach { i ->
                    FilaTop(i, tops.getOrNull(i), metas, marcable = true,
                        onTexto = { vm.topTexto(f, r, i, it) }, onMeta = { vm.topMeta(f, r, i, it) },
                        onHecha = { si -> val movio = vm.topHecha(f, r, i, si); if (si) aviso(ctx.getString(if (movio) R.string.cumbre_dia_meta else R.string.cumbre_dia)) },
                        pasosDe = vm::pasosDe)
                }
                // Lo que quedó ayer: traerlo o soltarlo (una vez por día)
                if (f == hoy() && r["dia#$f#traido"] != "1") {
                    val p by produceState<Pendientes?>(null, f, r) { value = vm.pendientes(f.minusDays(1), r) }
                    p?.takeIf { it.total > 0 }?.let { pend ->
                        Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp)) {
                            Text(stringResource(R.string.pendientes_ayer, pend.total), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { vm.pasar(f.minusDays(1), f, r) { n -> aviso(ctx.getString(R.string.traidos_hoy, n)) } }) {
                                    Text(stringResource(R.string.traer_a_hoy))
                                }
                                TextButton(onClick = { vm.soltar(f) }) { Text(stringResource(R.string.soltar)) }
                            }
                        }
                    }
                }
            }
        }
        // 2. Actividades por hora
        item(key = "acts-tit") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.actividades_dia), Modifier.weight(1f))
                androidx.compose.material3.FilterChip(selected = formato24, onClick = cambiarFormato,
                    label = { Text(if (formato24) "24 h" else "12 h") },
                    leadingIcon = { Icon(Icons.Outlined.Schedule, stringResource(if (formato24) R.string.formato_a_12 else R.string.formato_a_24), Modifier.size(18.dp)) })
                // "3 / 24 hechas" con un anillo de avance (igual que la web)
                val escritas = d.horarioMapa().filterValues { it.isNotBlank() }.keys
                if (escritas.isNotEmpty()) {
                    val hechas = escritas.count { r["dia#$f#$it#hecho"] == "1" }
                    val desc = stringResource(R.string.acts_hechas_desc, hechas, escritas.size)
                    Row(Modifier.padding(start = 8.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp).semantics(mergeDescendants = true) { contentDescription = desc },
                        verticalAlignment = Alignment.CenterVertically) {
                        val av by androidx.compose.animation.core.animateFloatAsState(hechas.toFloat() / escritas.size, label = "acts")
                        androidx.compose.material3.CircularProgressIndicator(progress = { av }, modifier = Modifier.size(26.dp), strokeWidth = 3.5.dp,
                            color = if (hechas == escritas.size) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, gapSize = 0.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("$hechas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(" / ${escritas.size} " + stringResource(R.string.acts_hechas), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(stringResource(R.string.actividades_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Cuánto aportaron hoy las acciones hechas a las sub-metas (jornadas del Método Cima)
            val aporte = vm.aporteDelDia(f, r)
            if (aporte > 0f) Text(stringResource(R.string.aporte_dia, "%.1f".format(aporte)), style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 8.dp))
        }
        val horario = d.horarioMapa()
        val horas = HORAS_DEL_DIA  // las 24 horas, de 4:00 am a 3:00 am (igual que la web)
        val ahora = if (f == hoy()) java.time.LocalTime.now().hour else -1
        items(horas, key = { "h$it" }) { h ->
            val t = horario[h].orEmpty()
            val hecho = r["dia#$f#$h#hecho"] == "1"
            val meta = r["dia#$f#$h#meta"].orEmpty()
            val esAhora = h == ahora
            val metaTxt = metas.firstOrNull { it.id.toString() == meta.substringBefore('|') }?.texto
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (esAhora) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (esAhora) stringResource(R.string.ahora_hora) else etiquetaHora(h, formato24),
                    modifier = Modifier.width(64.dp).padding(start = 4.dp).graphicsLayer { rotationX = giro.value; cameraDistance = 12f * density },
                    style = MaterialTheme.typography.labelMedium, fontWeight = if (esAhora) FontWeight.Bold else FontWeight.Normal,
                    color = if (esAhora) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Checkbox(checked = hecho, onCheckedChange = { vm.actHecha(h, it, r) }, enabled = t.isNotBlank(),
                    modifier = Modifier.semantics { contentDescription = ctx.getString(R.string.hecha) })
                OutlinedTextField(
                    value = t,
                    onValueChange = { vm.hora(h, it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (hecho) TextDecoration.LineThrough else null),
                    supportingText = if (metaTxt != null) { { Text("↑ $metaTxt", maxLines = 1, overflow = TextOverflow.Ellipsis) } } else null,
                )
                if (t.isNotBlank()) SelectorMeta(meta, metas, vm::pasosDe) { vm.actMeta(h, it, r) } else Spacer(Modifier.width(48.dp))
            }
        }
        // 3. Cierre del día y las 3 de mañana
        item { SectionTitle(stringResource(R.string.cierre_dia)) }
        item { CampoDia(stringResource(R.string.victorias_dia), d.victorias, 2) { v -> vm.editar { it.copy(victorias = v) } } }
        item { CampoDia(stringResource(R.string.aprendizaje), d.aprendizaje, 2) { v -> vm.editar { it.copy(aprendizaje = v) } } }
        item { CampoDia(stringResource(R.string.gratitud), d.gratitud, 2) { v -> vm.editar { it.copy(gratitud = v) } } }
        item {
            RutaCard {
                Text(stringResource(R.string.energia), style = MaterialTheme.typography.labelLarge)
                com.rutaalacima.app.ui.components.NivelEnergia(d.energia, { n -> vm.editar { it.copy(energia = n) } })
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.agua_vasos), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                com.rutaalacima.app.ui.components.VasosDeAgua(d.agua, { n -> vm.editar { it.copy(agua = n) } }, Modifier.fillMaxWidth())
            }
        }
        item { CampoDia(stringResource(R.string.notas), d.notas, 3) { v -> vm.editar { it.copy(notas = v) } } }
        item(key = "manana") {
            RutaCard {
                Text(stringResource(R.string.manana_cumbres), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.manana_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val tm = vm.tops(manana, r)
                (0..2).forEach { i ->
                    FilaTop(i, tm.getOrNull(i), metasManana, marcable = false,
                        onTexto = { vm.topTexto(manana, r, i, it) }, onMeta = { vm.topMeta(manana, r, i, it) }, onHecha = {})
                }
                val p by produceState<Pendientes?>(null, f, r, d.horario) { value = vm.pendientes(f, r) }
                p?.takeIf { it.total > 0 }?.let { pend ->
                    OutlinedButton(onClick = { vm.pasar(f, manana, r) { n -> aviso(ctx.getString(R.string.pasados_manana, n)) } },
                        modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.pasar_a_manana, pend.total)) }
                }
            }
        }
        // Campos de antes: se muestran solo si ya tienen algo escrito (las 3 cumbres los reemplazan)
        if (d.metas.isNotBlank()) item { CampoDia(stringResource(R.string.metas), d.metas, 3) { v -> vm.editar { it.copy(metas = v) } } }
        if (d.pendientes.isNotBlank()) item { CampoDia(stringResource(R.string.pendientes), d.pendientes, 3) { v -> vm.editar { it.copy(pendientes = v) } } }
        // Control financiero: los movimientos del día en Ascenso financiero (igual que en la web)
        item(key = "bienestar-$f") { com.rutaalacima.app.ui.bienestar.BienestarDiaCard(f) }
        // El control financiero del día se quitó de aquí: se anota en Ascenso financiero (igual que la web)
    }
}

/** Una de las 3 cumbres: número (la 1 resaltada), casilla, texto y la meta del mes que mueve. */
@Composable
private fun FilaTop(i: Int, x: TopDia?, metas: List<MetaMensualEntity>, marcable: Boolean,
                    onTexto: (String) -> Unit, onMeta: (String) -> Unit, onHecha: (Boolean) -> Unit,
                    pasosDe: (MetaMensualEntity) -> List<Triple<Int, String, Float>> = { emptyList() }) {
    val texto = x?.t.orEmpty()
    val hecho = x?.hecho == true
    val ph = stringResource(listOf(R.string.top_1, R.string.top_2, R.string.top_3)[i])
    val metaTxt = metas.firstOrNull { it.id.toString() == x?.meta?.substringBefore('|') }?.texto
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(CircleShape)
            .background(if (i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center) {
            Text("${i + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                color = if (i == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Checkbox(checked = hecho, onCheckedChange = onHecha, enabled = marcable && texto.isNotBlank(),
            modifier = Modifier.semantics { contentDescription = ph })
        OutlinedTextField(
            value = texto, onValueChange = onTexto, modifier = Modifier.weight(1f), singleLine = true,
            placeholder = { Text(ph) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDecoration = if (hecho) TextDecoration.LineThrough else null),
            supportingText = if (metaTxt != null) { { Text("↑ $metaTxt", maxLines = 1, overflow = TextOverflow.Ellipsis) } } else null,
        )
        if (texto.isNotBlank()) SelectorMeta(x?.meta.orEmpty(), metas, pasosDe, onMeta) else Spacer(Modifier.width(48.dp))
    }
}

/** Bandera que abre la lista de metas del mes: rellena si la actividad ya mueve una meta. */
@Composable
private fun SelectorMeta(actual: String, metas: List<MetaMensualEntity>, pasosDe: (MetaMensualEntity) -> List<Triple<Int, String, Float>> = { emptyList() }, onElegir: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(if (actual.isNotBlank()) Icons.Filled.Flag else com.rutaalacima.app.ui.components.IconosRuta.MetasContorno, stringResource(R.string.meta_que_mueve),
                tint = if (actual.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.sin_meta)) }, onClick = { onElegir(""); abierto = false })
            metas.forEach { m ->
                DropdownMenuItem(text = { Text(m.texto, maxLines = 2, fontWeight = FontWeight.SemiBold) }, onClick = { onElegir(m.id.toString()); abierto = false },
                    leadingIcon = if (m.id.toString() == actual) { { Icon(Icons.Filled.Check, null) } } else null)
                // Sus sub-metas (las escogidas para el mes) y cuánto aporta una jornada a cada una
                pasosDe(m).forEach { (p, t, aporte) ->
                    val v = "${m.id}|$p"
                    DropdownMenuItem(text = { Text("${p + 1}. $t · +${"%.1f".format(aporte)} %", maxLines = 2, style = MaterialTheme.typography.bodySmall) },
                        onClick = { onElegir(v); abierto = false }, modifier = Modifier.padding(start = 16.dp),
                        leadingIcon = if (v == actual) { { Icon(Icons.Filled.Check, null) } } else null)
                }
            }
        }
    }
}

@Composable
private fun CampoDia(titulo: String, valor: String, lineas: Int = 1, numerico: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(titulo) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = if (numerico) androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            else androidx.compose.foundation.text.KeyboardOptions.Default,
        minLines = lineas,
        singleLine = lineas == 1,
    )
}

/** Fila de círculos marcables (○ ○ ○ ○ ○). Tocar el último marcado lo desmarca. */
@Composable
fun Circulos(total: Int, valor: Int, color: Color, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..total) {
            val lleno = i <= valor
            Box(
                Modifier.size(26.dp).clip(CircleShape)
                    .background(if (lleno) color else Color.Transparent)
                    .border(1.5.dp, if (lleno) color else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onChange(if (valor == i) i - 1 else i) },
                contentAlignment = Alignment.Center,
            ) {
                Text("$i", style = MaterialTheme.typography.labelSmall,
                    color = if (lleno) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}
