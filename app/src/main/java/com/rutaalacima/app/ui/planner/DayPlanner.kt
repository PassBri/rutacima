package com.rutaalacima.app.ui.planner

import com.rutaalacima.app.R
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
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
        val m = metasMes.value.firstOrNull { it.id.toString() == id && it.anio == f.year && it.mes == f.monthValue } ?: return
        if (f.dayOfMonth !in m.diasMarcados()) viewModelScope.launch { c.planAnual.alternarDia(m, f.dayOfMonth) }
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
    val aviso: (String) -> Unit = { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    val f = vm.fecha
    val metas = todas.filter { it.anio == f.year && it.mes == f.monthValue }
    val manana = f.plusDays(1)
    val metasManana = todas.filter { it.anio == manana.year && it.mes == manana.monthValue }

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
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
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
                        onHecha = { si -> val movio = vm.topHecha(f, r, i, si); if (si) aviso(ctx.getString(if (movio) R.string.cumbre_dia_meta else R.string.cumbre_dia)) })
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
            }
            Text(stringResource(R.string.actividades_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val horario = d.horarioMapa()
        val horas = HORAS_DEL_DIA  // las 24 horas, de 4:00 am a 3:00 am (igual que la web)
        val ahora = if (f == hoy()) java.time.LocalTime.now().hour else -1
        items(horas, key = { "h$it" }) { h ->
            val t = horario[h].orEmpty()
            val hecho = r["dia#$f#$h#hecho"] == "1"
            val meta = r["dia#$f#$h#meta"].orEmpty()
            val esAhora = h == ahora
            val metaTxt = metas.firstOrNull { it.id.toString() == meta }?.texto
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (esAhora) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (esAhora) stringResource(R.string.ahora_hora) else etiquetaHora(h), modifier = Modifier.width(64.dp).padding(start = 4.dp),
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
                if (t.isNotBlank()) SelectorMeta(meta, metas) { vm.actMeta(h, it, r) } else Spacer(Modifier.width(48.dp))
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
                Circulos(total = 5, valor = d.energia, color = MaterialTheme.colorScheme.secondary) { n -> vm.editar { it.copy(energia = n) } }
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
        item {
            SectionTitle(stringResource(R.string.control_financiero))
            Text(stringResource(R.string.fin_dia_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { CampoDia(stringResource(R.string.ingresos), d.ingresos, numerico = true) { v -> vm.editar { it.copy(ingresos = v) } } }
                Box(Modifier.weight(1f)) { CampoDia(stringResource(R.string.gastos), d.gastos, numerico = true) { v -> vm.editar { it.copy(gastos = v) } } }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { CampoDia(stringResource(R.string.ahorro), d.ahorro, numerico = true) { v -> vm.editar { it.copy(ahorro = v) } } }
                Box(Modifier.weight(1f)) { CampoDia(stringResource(R.string.inversion), d.inversion, numerico = true) { v -> vm.editar { it.copy(inversion = v) } } }
            }
            DisponibleHoy(d.ingresos, d.gastos, d.ahorro, d.inversion)
        }
    }
}

/** Una de las 3 cumbres: número (la 1 resaltada), casilla, texto y la meta del mes que mueve. */
@Composable
private fun FilaTop(i: Int, x: TopDia?, metas: List<MetaMensualEntity>, marcable: Boolean,
                    onTexto: (String) -> Unit, onMeta: (String) -> Unit, onHecha: (Boolean) -> Unit) {
    val texto = x?.t.orEmpty()
    val hecho = x?.hecho == true
    val ph = stringResource(listOf(R.string.top_1, R.string.top_2, R.string.top_3)[i])
    val metaTxt = metas.firstOrNull { it.id.toString() == x?.meta }?.texto
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
        if (texto.isNotBlank()) SelectorMeta(x?.meta.orEmpty(), metas, onMeta) else Spacer(Modifier.width(48.dp))
    }
}

/** Bandera que abre la lista de metas del mes: rellena si la actividad ya mueve una meta. */
@Composable
private fun SelectorMeta(actual: String, metas: List<MetaMensualEntity>, onElegir: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(if (actual.isNotBlank()) Icons.Filled.Flag else Icons.Outlined.Flag, stringResource(R.string.meta_que_mueve),
                tint = if (actual.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.sin_meta)) }, onClick = { onElegir(""); abierto = false })
            metas.forEach { m ->
                DropdownMenuItem(text = { Text(m.texto, maxLines = 2) }, onClick = { onElegir(m.id.toString()); abierto = false },
                    leadingIcon = if (m.id.toString() == actual) { { Icon(Icons.Filled.Check, null) } } else null)
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
