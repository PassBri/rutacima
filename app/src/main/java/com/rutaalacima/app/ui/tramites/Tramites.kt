package com.rutaalacima.app.ui.tramites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.ui.components.IconosRuta
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Trámites y plazos (igual que la web): un trámite de la alcaldía de 45 días hábiles, un proceso o una cuota que se paga
 * cada mes. Une el día de inicio con el final, se ve en cada día (día 12 de 45, quedan 33) y avisa antes de vencer.
 * Los días hábiles saltan sábados, domingos y festivos de Colombia (Ley Emiliani) y empiezan a contar al día siguiente.
 * Libro "tramites": "tramite#<id>" → JSON (las mismas claves que la web).
 */
@Serializable
data class Tramite(
    val id: String, val n: String, val tipo: String = "tramite", val ini: String, val dias: Int? = null, val habiles: Boolean = false,
    val fin: String, val avisos: List<Int> = listOf(1, 3, 7), val mensual: Boolean = false, val nota: String = "", val listo: Boolean = false,
)

object Plazos {
    const val WB = "tramites"
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val cache = mutableMapOf<Int, Set<LocalDate>>()

    /** Festivos de Colombia: fijos, los que la Ley Emiliani pasa al lunes y los que dependen de la Pascua. */
    fun festivos(a: Int): Set<LocalDate> = cache.getOrPut(a) {
        fun lunes(d: LocalDate): LocalDate { var x = d; while (x.dayOfWeek != DayOfWeek.MONDAY) x = x.plusDays(1); return x }
        val A = a % 19; val B = a / 100; val C = a % 100; val D = B / 4; val E = B % 4; val F = (B + 8) / 25; val G = (B - F + 1) / 3
        val H = (19 * A + B - D - G + 15) % 30; val I = C / 4; val K = C % 4; val L = (32 + 2 * E + 2 * I - H - K) % 7; val M = (A + 11 * H + 22 * L) / 451
        val pascua = LocalDate.of(a, (H + L - 7 * M + 114) / 31, ((H + L - 7 * M + 114) % 31) + 1)
        val f = { m: Int, d: Int -> LocalDate.of(a, m, d) }
        (listOf(f(1, 1), f(5, 1), f(7, 20), f(8, 7), f(12, 8), f(12, 25), pascua.minusDays(3), pascua.minusDays(2)) +
            listOf(f(1, 6), f(3, 19), f(6, 29), f(8, 15), f(10, 12), f(11, 1), f(11, 11), pascua.plusDays(39), pascua.plusDays(60), pascua.plusDays(68)).map(::lunes)).toSet()
    }
    fun habil(d: LocalDate) = d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY && d !in festivos(d.year)
    fun fin(ini: LocalDate, dias: Int, habiles: Boolean): LocalDate {
        if (!habiles) return ini.plusDays(dias.toLong())
        var d = ini; var n = 0; while (n < dias) { d = d.plusDays(1); if (habil(d)) n++ }; return d
    }
    fun contar(desde: LocalDate, hasta: LocalDate, habiles: Boolean): Int {
        if (!hasta.isAfter(desde)) return 0
        if (!habiles) return ChronoUnit.DAYS.between(desde, hasta).toInt()
        var n = 0; var d = desde; while (d.isBefore(hasta)) { d = d.plusDays(1); if (habil(d)) n++ }; return n
    }
    fun leer(r: Map<String, String>): List<Tramite> = r.filterKeys { it.startsWith("tramite#") }.values.filter { it.isNotBlank() }
        .mapNotNull { runCatching { json.decodeFromString(Tramite.serializer(), it) }.getOrNull() }.sortedBy { it.fin }
    fun pagoEn(t: Tramite, ym: YearMonth): LocalDate = ym.atDay(minOf(LocalDate.parse(t.ini).dayOfMonth, ym.lengthOfMonth()))
    /** "inicio", "fin", "curso", "pago" o null. */
    fun enDia(t: Tramite, f: LocalDate): String? {
        val ini = LocalDate.parse(t.ini); val fin = LocalDate.parse(t.fin)
        if (t.mensual) return if (!f.isBefore(ini) && pagoEn(t, YearMonth.from(f)) == f) "pago" else null
        if (f.isBefore(ini) || f.isAfter(fin)) return null
        return when { f == ini && f == fin -> "fin"; f == ini -> "inicio"; f == fin -> "fin"; else -> "curso" }
    }
    fun proximo(t: Tramite, hoy: LocalDate): LocalDate? {
        if (t.listo) return null
        if (!t.mensual) return LocalDate.parse(t.fin)
        var p = pagoEn(t, YearMonth.from(hoy)); if (p.isBefore(hoy)) p = pagoEn(t, YearMonth.from(hoy).plusMonths(1))
        return maxOf(p, LocalDate.parse(t.ini))
    }
    fun avisarHoy(t: Tramite, hoy: LocalDate): Boolean {
        val p = proximo(t, hoy) ?: return false; if (p.isBefore(hoy)) return false
        val q = ChronoUnit.DAYS.between(hoy, p).toInt(); return q == 0 || q in t.avisos
    }
}

class TramitesViewModel(private val c: AppContainer) : ViewModel() {
    val todos: StateFlow<List<Tramite>> = c.respuestas.observar(Plazos.WB).map { Plazos.leer(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun guardar(t: Tramite) = viewModelScope.launch { c.respuestas.guardar(Plazos.WB, "tramite#" + t.id, Plazos.json.encodeToString(Tramite.serializer(), t)) }
    fun borrar(id: String) = viewModelScope.launch { c.respuestas.guardar(Plazos.WB, "tramite#$id", "") }
}

private fun iconoTipo(tipo: String): ImageVector = when (tipo) { "pago" -> IconosRuta.Finanzas; "proceso" -> Icons.Filled.Balance; else -> Icons.Filled.Schedule }
private fun corto(d: LocalDate) = "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).trimEnd('.')}"

/** La sección «Trámites y plazos» de un día (en el planificador del día y en Hoy). */
@Composable
fun TramitesDia(fecha: LocalDate, modifier: Modifier = Modifier) {
    val vm = rutaViewModel { TramitesViewModel(it) }
    val todos by vm.todos.collectAsStateWithLifecycle()
    val hoy = LocalDate.now()
    val xs = todos.filter { Plazos.enDia(it, fecha) != null || (fecha == hoy && !it.listo && Plazos.avisarHoy(it, hoy)) }
    var editando by remember { mutableStateOf<Tramite?>(null) }
    var nuevo by remember { mutableStateOf(false) }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tr_titulo), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.tr_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { nuevo = true }) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.tr_nuevo)) }
            }
            if (xs.isEmpty()) Text(stringResource(R.string.tr_vacio), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            xs.forEach { t -> FilaTramite(t, fecha, onEditar = { editando = t }, onListo = { vm.guardar(t.copy(listo = !t.listo)) }) }
        }
    }
    if (nuevo) HojaTramite(null, fecha, onGuardar = { vm.guardar(it); nuevo = false }, onBorrar = {}, onCerrar = { nuevo = false })
    editando?.let { t -> HojaTramite(t, fecha, onGuardar = { vm.guardar(it); editando = null }, onBorrar = { vm.borrar(t.id); editando = null }, onCerrar = { editando = null }) }
}

@Composable
private fun FilaTramite(t: Tramite, f: LocalDate, onEditar: () -> Unit, onListo: () -> Unit) {
    val hoy = LocalDate.now(); val ini = LocalDate.parse(t.ini); val fin = LocalDate.parse(t.fin)
    val total = t.dias ?: Plazos.contar(ini, fin, t.habiles)
    val ya = minOf(total, Plazos.contar(ini, f, t.habiles)); val quedan = Plazos.contar(if (f.isBefore(ini)) ini else f, fin, t.habiles)
    val u = if (t.habiles) " " + stringResource(R.string.tr_habiles) else ""
    val rol = Plazos.enDia(t, f)
    val (estado, color) = when {
        t.listo -> stringResource(R.string.tr_listo) to MaterialTheme.colorScheme.tertiary
        t.mensual -> { val p = Plazos.proximo(t, hoy)!!; val q = ChronoUnit.DAYS.between(hoy, p).toInt()
            (if (q == 0) stringResource(R.string.tr_pago_hoy) else stringResource(R.string.tr_proximo_pago, corto(p), q)) to (if (q <= 3) Color(0xFF9A6A00) else MaterialTheme.colorScheme.onSurfaceVariant) }
        hoy.isAfter(fin) -> stringResource(R.string.tr_vencio, corto(fin)) to MaterialTheme.colorScheme.primary
        rol == "inicio" -> stringResource(R.string.tr_empieza, total) + u to MaterialTheme.colorScheme.onSurfaceVariant
        rol == "fin" -> stringResource(R.string.tr_vence_hoy) to MaterialTheme.colorScheme.primary
        else -> stringResource(R.string.tr_dia_de, ya, total) + u + " · " + stringResource(R.string.tr_quedan, quedan) to (if (quedan <= 3) Color(0xFF9A6A00) else MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(start = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(iconoTipo(t.tipo), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(t.n, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (t.listo) TextDecoration.LineThrough else null)
            Text(estado, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
            if (!t.mensual && !t.listo && total > 0) LinearProgressIndicator(progress = { ya.toFloat() / total }, Modifier.fillMaxWidth().height(5.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, drawStopIndicator = {}, gapSize = 0.dp)
            Text(if (t.mensual) stringResource(R.string.tr_cada_mes, ini.dayOfMonth) else "${corto(ini)} → ${corto(fin)} ${fin.year}" + (if (t.nota.isNotBlank()) " · ${t.nota}" else ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onEditar) { Icon(Icons.Filled.Edit, stringResource(R.string.tr_editar)) }
        if (!t.mensual) IconButton(onClick = onListo) { Icon(Icons.Filled.Check, stringResource(R.string.tr_marcar_listo), tint = if (t.listo) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** El formulario: nombre, tipo, inicio y cómo termina (N días, hasta una fecha o cada mes), avisos y nota. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HojaTramite(t: Tramite?, fecha: LocalDate, onGuardar: (Tramite) -> Unit, onBorrar: () -> Unit, onCerrar: () -> Unit) {
    var n by rememberSaveable { mutableStateOf(t?.n.orEmpty()) }
    var tipo by rememberSaveable { mutableStateOf(t?.tipo ?: "tramite") }
    var ini by rememberSaveable { mutableStateOf(t?.ini ?: fecha.toString()) }
    var modo by rememberSaveable { mutableStateOf(when { t == null -> "dias"; t.mensual -> "mensual"; t.dias != null -> "dias"; else -> "fecha" }) }
    var dias by rememberSaveable { mutableStateOf((t?.dias ?: 45).toString()) }
    var habiles by rememberSaveable { mutableStateOf(t?.habiles ?: true) }
    var fin by rememberSaveable { mutableStateOf(t?.fin ?: fecha.toString()) }
    var avisos by rememberSaveable { mutableStateOf(t?.avisos ?: listOf(1, 3, 7)) }
    var nota by rememberSaveable { mutableStateOf(t?.nota.orEmpty()) }
    val iniD = runCatching { LocalDate.parse(ini) }.getOrNull()
    val finCalc = iniD?.let { d -> when (modo) { "dias" -> dias.toIntOrNull()?.takeIf { it > 0 }?.let { Plazos.fin(d, it, habiles) }; "fecha" -> runCatching { LocalDate.parse(fin) }.getOrNull(); else -> d } }
    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(if (t == null) R.string.tr_nuevo_titulo else R.string.tr_editar), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(n, { n = it.take(120) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tr_nombre)) }, singleLine = true,
                placeholder = { Text(stringResource(R.string.tr_nombre_ej)) })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("tramite" to R.string.tr_tipo_tramite, "pago" to R.string.tr_tipo_pago, "proceso" to R.string.tr_tipo_proceso).forEach { (k, s) ->
                    FilterChip(tipo == k, { tipo = k }, label = { Text(stringResource(s)) }, leadingIcon = { Icon(iconoTipo(k), null, Modifier.size(18.dp)) })
                }
            }
            OutlinedTextField(ini, { ini = it.take(10) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tr_inicio)) }, singleLine = true,
                placeholder = { Text("2026-10-20") }, isError = iniD == null)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("dias" to R.string.tr_modo_dias, "fecha" to R.string.tr_modo_fecha, "mensual" to R.string.tr_modo_mensual).forEach { (k, s) ->
                    FilterChip(modo == k, { modo = k }, label = { Text(stringResource(s)) })
                }
            }
            if (modo == "dias") {
                OutlinedTextField(dias, { dias = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tr_dias)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(habiles, { habiles = it }); Text(stringResource(R.string.tr_habiles_largo), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (modo == "fecha") OutlinedTextField(fin, { fin = it.take(10) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tr_final)) }, singleLine = true, placeholder = { Text("2026-12-28") })
            when {
                modo == "mensual" && iniD != null -> Text(stringResource(R.string.tr_cada_mes, iniD.dayOfMonth), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                finCalc != null && modo == "dias" -> Text(stringResource(R.string.tr_termina, finCalc.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + corto(finCalc) + " " + finCalc.year),
                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.tr_avisarme), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 7, 15).forEach { d ->
                    FilterChip(d in avisos, { avisos = if (d in avisos) avisos - d else avisos + d },
                        label = { Text(if (d == 7) stringResource(R.string.tr_una_semana) else stringResource(R.string.tr_n_dias, d)) })
                }
            }
            OutlinedTextField(nota, { nota = it.take(200) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tr_nota)) }, singleLine = true,
                placeholder = { Text(stringResource(R.string.tr_nota_ej)) })
            val valido = n.isNotBlank() && iniD != null && finCalc != null && !finCalc.isBefore(iniD)
            Button(onClick = {
                val d = iniD!!; val f = finCalc!!
                onGuardar(Tramite(t?.id ?: System.currentTimeMillis().toString(), n.trim(), tipo, d.toString(), if (modo == "dias") dias.toIntOrNull() else null,
                    modo == "dias" && habiles, f.toString(), avisos.sorted(), modo == "mensual", nota.trim(), t?.listo ?: false))
            }, enabled = valido, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tr_guardar)) }
            if (t != null) TextButton(onClick = onBorrar, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tr_borrar), color = MaterialTheme.colorScheme.error) }
        }
    }
}
