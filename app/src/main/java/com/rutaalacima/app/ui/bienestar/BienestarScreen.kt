package com.rutaalacima.app.ui.bienestar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Bienestar
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Los días de Bienestar y el agua de la agenda. */
data class DatosBienestar(val dias: Map<LocalDate, Bienestar.Dia> = emptyMap(), val agua: Map<LocalDate, Int> = emptyMap())

class BienestarViewModel(private val c: AppContainer) : ViewModel() {
    private val hoy = LocalDate.now()
    val datos: StateFlow<DatosBienestar> = combine(
        c.respuestas.observar(Bienestar.WB),
        c.agenda.delMes(hoy.year, hoy.monthValue),
        YearMonth.from(hoy).minusMonths(1).let { c.agenda.delMes(it.year, it.monthValue) },
    ) { r, a1, a2 ->
        DatosBienestar(
            r.entries.mapNotNull { (k, v) -> if (k.startsWith("bien#")) runCatching { LocalDate.parse(k.removePrefix("bien#")) to Bienestar.leer(v) }.getOrNull() else null }.toMap(),
            (a1 + a2).mapNotNull { a -> runCatching { LocalDate.parse(a.fecha) to a.agua }.getOrNull() }.toMap(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DatosBienestar())

    fun cambiar(cambio: (Bienestar.Dia) -> Bienestar.Dia) = viewModelScope.launch {
        val d = cambio(datos.value.dias[hoy] ?: Bienestar.Dia())
        c.respuestas.guardar(Bienestar.WB, Bienestar.clave(hoy), Bienestar.escribir(d))
    }
    fun agua(n: Int) = viewModelScope.launch { val d = c.agenda.dia(hoy); c.agenda.guardar(d.copy(agua = n.coerceIn(0, 8))) }
}

private val COLOR_CLIMA = mapOf("sol" to Color(0xFFC98A12), "tormenta" to Color(0xFFB3261E), "niebla" to Color(0xFF3E5868), "valle" to Color(0xFF2E7D52))
private fun ctxStr(ctx: android.content.Context, n: String, def: String = n) = ctx.resources.getIdentifier(n, "string", ctx.packageName).let { if (it != 0) ctx.getString(it) else def }

/** El nombre de una emoción o causa en el idioma de la app (se guardan con su clave de la web). */
@Composable private fun nombreEmocion(clima: String, e: String): String {
    val i = Bienestar.EMOCIONES[clima]?.indexOf(e) ?: -1
    return if (i < 0) e else ctxStr(LocalContext.current, "bi_e_${clima}_$i", e)
}
@Composable private fun nombreCausa(c: String): String { val i = Bienestar.CAUSAS.indexOf(c); return if (i < 0) c else ctxStr(LocalContext.current, "bi_c_$i", c) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BienestarScreen(onBack: () -> Unit) {
    val vm = rutaViewModel { BienestarViewModel(it) }
    val datos by vm.datos.collectAsState()
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val pestanas = listOf(R.string.bi_p_clima, R.string.bi_p_salud, R.string.bi_p_mes, R.string.bi_p_respirar, R.string.bi_p_practica)
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.bi_titulo)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.atras)) } })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = pestana, edgePadding = 12.dp) {
                pestanas.forEachIndexed { i, t -> Tab(selected = pestana == i, onClick = { pestana = i }, text = { Text(stringResource(t)) }) }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (pestana) {
                    0 -> item { ClimaHoy(datos, vm) }
                    1 -> item { VidaSaludable(datos, vm) }
                    2 -> item { ClimaMes(datos) }
                    3 -> item { Respirar { vm.cambiar { d -> d.copy(herr = (d.herr + "caja").distinct()) } } }
                    else -> item { Practica() }
                }
                item { Text(stringResource(R.string.bi_privado), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClimaHoy(datos: DatosBienestar, vm: BienestarViewModel) {
    val d = datos.dias[LocalDate.now()] ?: Bienestar.Dia()
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RutaCard {
            Text(stringResource(R.string.bi_como), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.bi_como_d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Bienestar.CLIMAS.chunked(2).forEach { fila ->
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    fila.forEach { k ->
                        val col = COLOR_CLIMA.getValue(k); val sel = d.clima == k
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (sel) col.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                            .border(if (sel) 2.dp else 1.dp, if (sel) col else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .clickable { vm.cambiar { x -> if (x.clima == k) x else x.copy(clima = k, emo = null, int = x.int ?: 3, t = System.currentTimeMillis()) } }.padding(12.dp)) {
                            com.rutaalacima.app.ui.components.ClimaIcono(k, Modifier.size(40.dp))
                            Text(ctxStr(ctx, "bi_clima_$k"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
                            Text(ctxStr(ctx, "bi_clima_${k}_d"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        val clima = d.clima
        if (clima != null) {
            val col = COLOR_CLIMA.getValue(clima)
            RutaCard {
                Text(stringResource(R.string.bi_nombre), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.bi_nombre_d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bienestar.EMOCIONES.getValue(clima).forEach { e ->
                        Chip(nombreEmocion(clima, e), d.emo == e, col) { vm.cambiar { x -> x.copy(emo = if (x.emo == e) null else e) } }
                    }
                }
                Text(stringResource(R.string.bi_fuerte), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp))
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { n ->
                        val si = (d.int ?: 0) >= n
                        Box(Modifier.size(44.dp).clip(CircleShape).background(if (si) col else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (si) col else MaterialTheme.colorScheme.outline, CircleShape).clickable { vm.cambiar { x -> x.copy(int = n) } }, contentAlignment = Alignment.Center) {
                            Text("$n", color = if (si) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text(stringResource(R.string.bi_causa), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp))
                FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bienestar.CAUSAS.forEach { c -> Chip(nombreCausa(c), c in d.causas, MaterialTheme.colorScheme.primary) { vm.cambiar { x -> x.copy(causas = if (c in x.causas) x.causas - c else x.causas + c) } } }
                }
                var nota by remember(d.nota) { mutableStateOf(d.nota.orEmpty()) }
                OutlinedTextField(nota, { nota = it.take(600) }, label = { Text(stringResource(R.string.bi_nota)) }, minLines = 3, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                LaunchedEffect(nota) { if (nota != d.nota.orEmpty()) { delay(700); vm.cambiar { x -> x.copy(nota = nota.trim().ifBlank { null }) } } }
            }
            if ((clima == "tormenta" || clima == "niebla") && (d.int ?: 0) >= 5) RutaCard {
                Text(stringResource(R.string.bi_apoyo_t), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.bi_apoyo_d), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            RutaCard {
                val bueno = clima == "sol" || clima == "valle"
                Text(stringResource(if (bueno) R.string.bi_guarda else R.string.bi_minuto), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(if (bueno) R.string.bi_guarda_d else R.string.bi_minuto_d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Bienestar.HERRAMIENTAS.forEach { k ->
                    val si = k in d.herr
                    Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .border(1.dp, if (si) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .background(if (si) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { vm.cambiar { x -> x.copy(herr = if (k in x.herr) x.herr - k else x.herr + k) } }.padding(12.dp)) {
                        Text(ctxStr(ctx, "bi_h_$k") + if (si) " ✓" else "", style = MaterialTheme.typography.titleSmall)
                        Text(ctxStr(ctx, "bi_h_${k}_d"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(texto: String, sel: Boolean, color: Color, onClick: () -> Unit) {
    Text((if (sel) "✓ " else "") + texto, Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface)
        .border(1.dp, if (sel) color else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
}

@Composable
private fun VidaSaludable(datos: DatosBienestar, vm: BienestarViewModel) {
    val hoy = LocalDate.now(); val d = datos.dias[hoy] ?: Bienestar.Dia(); val agua = datos.agua[hoy] ?: 0
    val ix = Bienestar.indice(datos.dias, datos.agua, hoy)
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RutaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text(ix.indice?.let { "$it %" } ?: "—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(stringResource(R.string.bi_indice), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.bi_indice_d, ix.racha), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Bienestar.PILARES.forEach { k ->
                val v = ix.pilares[k]
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(ctxStr(ctx, "bi_pil_$k"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(150.dp))
                    com.rutaalacima.app.ui.components.ProgressLine((v ?: 0.0).toFloat(), Modifier.weight(1f),
                        color = when { v == null -> MaterialTheme.colorScheme.outline; v >= 0.99 -> Color(0xFF2E7D52); v >= 0.6 -> Color(0xFFC98A12); else -> MaterialTheme.colorScheme.error })
                }
            }
            Text(stringResource(R.string.bi_mov_semana, ix.movSemana), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
        RutaCard {
            Text(stringResource(R.string.bi_hoy), style = MaterialTheme.typography.titleMedium)
            Paso(stringResource(R.string.bi_pil_sueno), d.sueno?.let { "${if (it % 1.0 == 0.0) it.toInt() else it} h" }, stringResource(R.string.bi_meta_sueno),
                { vm.cambiar { x -> x.copy(sueno = ((x.sueno ?: 0.0) - 0.5).coerceAtLeast(0.0)) } }, { vm.cambiar { x -> x.copy(sueno = ((x.sueno ?: 0.0) + 0.5).coerceAtMost(14.0)) } })
            Paso(stringResource(R.string.bi_pil_mov), d.mov?.let { "$it min" }, stringResource(R.string.bi_meta_mov),
                { vm.cambiar { x -> x.copy(mov = ((x.mov ?: 0) - 10).coerceAtLeast(0)) } }, { vm.cambiar { x -> x.copy(mov = (x.mov ?: 0) + 10) } })
            Paso(stringResource(R.string.bi_pil_fyv), d.fyv?.let { "$it" }, stringResource(R.string.bi_meta_fyv),
                { vm.cambiar { x -> x.copy(fyv = ((x.fyv ?: 0) - 1).coerceAtLeast(0)) } }, { vm.cambiar { x -> x.copy(fyv = ((x.fyv ?: 0) + 1).coerceAtMost(12)) } })
            Paso(stringResource(R.string.bi_pil_agua), "$agua / 8", stringResource(R.string.bi_meta_agua), { vm.agua(agua - 1) }, { vm.agua(agua + 1) })
            Paso(stringResource(R.string.bi_pil_aire), d.aire?.let { "$it min" }, stringResource(R.string.bi_meta_aire),
                { vm.cambiar { x -> x.copy(aire = ((x.aire ?: 0) - 10).coerceAtLeast(0)) } }, { vm.cambiar { x -> x.copy(aire = (x.aire ?: 0) + 10) } })
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().clickable { vm.cambiar { x -> x.copy(pant = !(x.pant ?: false)) } }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(d.pant == true, { vm.cambiar { x -> x.copy(pant = it) } })
                Column { Text(stringResource(R.string.bi_pil_pant), style = MaterialTheme.typography.bodyMedium); Text(stringResource(R.string.bi_meta_pant), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Text(stringResource(R.string.bi_oms), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Paso(titulo: String, valor: String?, meta: String, menos: () -> Unit, mas: () -> Unit) {
    Row(Modifier.padding(top = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedIconButton(onClick = menos) { Icon(Icons.Filled.Remove, null) }
        Text(valor ?: "—", Modifier.width(76.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        OutlinedIconButton(onClick = mas) { Icon(Icons.Filled.Add, null) }
    }
}

@Composable
private fun ClimaMes(datos: DatosBienestar) {
    val hoy = LocalDate.now(); val p = Bienestar.patrones(datos.dias, hoy)
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RutaCard {
            Text(stringResource(R.string.bi_cada_dia), style = MaterialTheme.typography.titleMedium)
            (29 downTo 0).map { hoy.minusDays(it.toLong()) }.chunked(7).forEach { semana ->
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    semana.forEach { f ->
                        val c = datos.dias[f]?.clima?.let { COLOR_CLIMA[it] }
                        Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(c ?: MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                            Text("${f.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = if (c != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    repeat(7 - semana.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        RutaCard {
            Text(stringResource(R.string.bi_repite), style = MaterialTheme.typography.titleMedium)
            if (p.dias == 0) Text(stringResource(R.string.bi_repite_vacio), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else {
                Bienestar.CLIMAS.forEach { k ->
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(ctxStr(ctx, "bi_clima_$k"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(120.dp))
                        com.rutaalacima.app.ui.components.ProgressLine((p.cuenta[k] ?: 0).toFloat() / p.dias, Modifier.weight(1f), color = COLOR_CLIMA.getValue(k))
                        Text("${p.cuenta[k] ?: 0}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                    }
                }
                if (p.emociones.isNotEmpty()) Text(stringResource(R.string.bi_frecuentes, p.emociones.joinToString { (e, n) -> "$e ($n)" }), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
                if (p.causas.isNotEmpty()) Text(stringResource(R.string.bi_causas_mal, p.causas.joinToString { (c, n) -> "$c ($n)" }), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                p.sueno?.let { (a, b) -> Text(stringResource(R.string.bi_sueno_cuenta, a, b), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
    }
}

@Composable
private fun Respirar(onHecho: () -> Unit) {
    var activo by remember { mutableStateOf(false) }
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(activo) { if (activo) { t = 0; while (t < 64) { delay(1000); t++ }; activo = false; onHecho() } }
    val fase = (t / 4) % 4
    val fases = listOf(R.string.bi_inhala, R.string.bi_sosten, R.string.bi_exhala, R.string.bi_sosten)
    val prog by animateFloatAsState(if (activo) ((t % 16) + 1) / 16f else 0f, tween(1000), label = "caja")
    val color = MaterialTheme.colorScheme.primary; val pista = MaterialTheme.colorScheme.outlineVariant
    RutaCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                    val w = size.width; val h = size.height
                    drawRoundRect(pista, cornerRadius = androidx.compose.ui.geometry.CornerRadius(40f), style = Stroke(6f))
                    // El punto recorre el cuadrado: sube (inhala), derecha (sostén), baja (exhala), izquierda (sostén)
                    val s = prog * 4; val lado = s.toInt().coerceAtMost(3); val f = s - lado
                    val pt = when (lado) { 0 -> Offset(0f, h * (1 - f)); 1 -> Offset(w * f, 0f); 2 -> Offset(w, h * f); else -> Offset(w * (1 - f), h) }
                    drawCircle(color, 14f, if (activo) pt else Offset(0f, h))
                }
                Text(if (activo) stringResource(fases[fase]) + " · ${4 - t % 4}" else if (t >= 64) stringResource(R.string.bi_listo) else stringResource(R.string.bi_toca), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
            Text(stringResource(R.string.bi_resp_d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            FilledTonalButton(onClick = { activo = false; t = 0; activo = true }, modifier = Modifier.padding(top = 10.dp)) { Icon(Icons.Filled.SelfImprovement, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.bi_empezar)) }
        }
    }
}

@Composable
private fun Practica() {
    val k = Bienestar.practica(LocalDate.now()); val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RutaCard {
            Text(stringResource(R.string.bi_esta_semana), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
            Text(ctxStr(ctx, "bi_g_$k"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 4.dp))
            Text(ctxStr(ctx, "bi_g_${k}_d"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
        }
        RutaCard {
            Text(stringResource(R.string.bi_goleman), style = MaterialTheme.typography.titleMedium)
            (0 until 5).forEach { i ->
                Column(Modifier.padding(top = 10.dp)) {
                    Text("${i + 1}. " + ctxStr(ctx, "bi_g_$i"), style = MaterialTheme.typography.titleSmall, color = if (i == k) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    Text(ctxStr(ctx, "bi_g_${i}_d"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
