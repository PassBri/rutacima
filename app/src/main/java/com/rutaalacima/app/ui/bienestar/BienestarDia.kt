package com.rutaalacima.app.ui.bienestar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Bienestar
import com.rutaalacima.app.ui.components.ClimaIcono
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
import java.time.format.TextStyle
import java.util.Locale

/*
 * Bienestar en cada día, mes y año (igual que la web, web/js/33-bienestar-dia.js).
 * El clima interior y los hábitos se controlan todos los días, por eso viven en el día;
 * el mes y el año muestran el acumulado. Mismos datos de Bienestar ("bien#aaaa-mm-dd") y el agua de la agenda.
 */

private val VERDE = Color(0xFF2E7D52)
private val COLOR = mapOf("sol" to Color(0xFFE8A317), "tormenta" to Color(0xFFE0662A), "niebla" to Color(0xFF7A8896), "valle" to Color(0xFF3E8A5A))
private fun txt(ctx: android.content.Context, n: String, def: String = n) = ctx.resources.getIdentifier(n, "string", ctx.packageName).let { if (it != 0) ctx.getString(it) else def }
private fun emoTxt(ctx: android.content.Context, e: String): String {
    Bienestar.EMOCIONES.forEach { (c, l) -> val i = l.indexOf(e); if (i >= 0) return txt(ctx, "bi_e_${c}_$i", e) }
    return e
}
private fun causaTxt(ctx: android.content.Context, c: String) = Bienestar.CAUSAS.indexOf(c).let { if (it < 0) c else txt(ctx, "bi_c_$it", c) }
private fun num1(x: Double?) = x?.let { String.format(Locale.getDefault(), "%.1f", it).removeSuffix(",0").removeSuffix(".0") } ?: "—"

/** Los días de Bienestar y el agua de la agenda de unos meses. */
class BienestarRangoViewModel(private val c: AppContainer, meses: List<YearMonth>) : ViewModel() {
    val datos: StateFlow<DatosBienestar> = combine(
        listOf(c.respuestas.observar(Bienestar.WB)) + meses.map { c.agenda.delMes(it.year, it.monthValue) },
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val r = arr[0] as Map<String, String>
        val agua = arr.drop(1).flatMap { (it as List<com.rutaalacima.app.data.local.AgendaDiaEntity>) }
            .mapNotNull { a -> runCatching { LocalDate.parse(a.fecha) to a.agua }.getOrNull() }.toMap()
        DatosBienestar(
            r.entries.mapNotNull { (k, v) -> if (k.startsWith("bien#")) runCatching { LocalDate.parse(k.removePrefix("bien#")) to Bienestar.leer(v) }.getOrNull() else null }.toMap(),
            agua,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DatosBienestar())

    fun cambiar(f: LocalDate, cambio: (Bienestar.Dia) -> Bienestar.Dia) = viewModelScope.launch {
        val d = cambio(datos.value.dias[f] ?: Bienestar.Dia())
        c.respuestas.guardar(Bienestar.WB, Bienestar.clave(f), Bienestar.escribir(d))
    }
}

@Composable
private fun anillo(p: Float, tam: Dp, texto: String, sub: String) {
    val pista = MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.size(tam), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(tam)) {
            val g = 5.dp.toPx(); val o = Offset(g / 2, g / 2); val s = Size(size.width - g, size.height - g)
            drawArc(pista, 0f, 360f, false, o, s, style = Stroke(g))
            drawArc(VERDE, -90f, 360f * p.coerceIn(0f, 1f), false, o, s, style = Stroke(g, cap = StrokeCap.Round))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun Cabecera(titulo: String, sub: String, derecha: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(VERDE.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.SelfImprovement, null, tint = VERDE)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        derecha()
    }
}

@Composable
private fun Sub(t: String) = Text(t, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))

// ---------------------------------------------------------------- El día

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BienestarDiaCard(fecha: LocalDate, modifier: Modifier = Modifier) {
    if (fecha.isAfter(LocalDate.now())) return
    val ym = YearMonth.from(fecha)
    val vm = rutaViewModel(key = "bien-$ym") { BienestarRangoViewModel(it, listOf(ym)) }
    val datos by vm.datos.collectAsState()
    val ctx = LocalContext.current
    val d = datos.dias[fecha] ?: Bienestar.Dia(); val agua = datos.agua[fecha] ?: 0
    val pd = Bienestar.puntaje(d, agua)
    val cambiar: ((Bienestar.Dia) -> Bienestar.Dia) -> Unit = { vm.cambiar(fecha, it) }
    var mas by rememberSaveable(fecha) { mutableStateOf(false) }

    RutaCard(modifier) {
        Cabecera(stringResource(R.string.bd_titulo), stringResource(R.string.bd_sub)) {
            if (pd != null) Box(Modifier.semantics { contentDescription = ctx.getString(R.string.bd_cumplidos, pd.cumplidos) }) { anillo(pd.cumplidos / 6f, 52.dp, "${pd.cumplidos}", "/6") }
        }
        Sub(stringResource(R.string.bd_clima))
        Bienestar.CLIMAS.chunked(2).forEach { fila ->
            Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { k ->
                    val col = COLOR.getValue(k); val sel = d.clima == k
                    Row(
                        Modifier.weight(1f).heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (sel) col.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface)
                            .border(if (sel) 2.dp else 1.dp, if (sel) col else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .clickable { cambiar { x -> if (x.clima == k) x.copy(clima = null, emo = null, int = null) else x.copy(clima = k, emo = null, int = x.int ?: 3, t = System.currentTimeMillis()) } }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ClimaIcono(k, Modifier.size(32.dp)); Spacer(Modifier.width(8.dp))
                        Text(txt(ctx, "bi_clima_$k"), style = MaterialTheme.typography.labelLarge, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
        val clima = d.clima
        AnimatedVisibility(clima != null) {
            val col = clima?.let { COLOR[it] } ?: VERDE
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (clima?.let { Bienestar.EMOCIONES[it] }).orEmpty().forEach { e -> Ficha(emoTxt(ctx, e), d.emo == e, col) { cambiar { x -> x.copy(emo = if (x.emo == e) null else e) } } }
                }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.bd_int), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 4.dp))
                    (1..5).forEach { n ->
                        val si = (d.int ?: 0) >= n
                        Box(Modifier.size(40.dp).clip(CircleShape).background(if (si) col else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (si) col else MaterialTheme.colorScheme.outline, CircleShape).clickable { cambiar { x -> x.copy(int = n) } }, contentAlignment = Alignment.Center) {
                            Text("$n", color = if (si) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { mas = !mas }.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.bd_mas), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Icon(if (mas) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                }
                AnimatedVisibility(mas) {
                    Column {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Bienestar.CAUSAS.forEach { c -> Ficha(causaTxt(ctx, c), c in d.causas, MaterialTheme.colorScheme.primary) { cambiar { x -> x.copy(causas = if (c in x.causas) x.causas - c else x.causas + c) } } }
                        }
                        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Bienestar.HERRAMIENTAS.forEach { k -> Ficha(txt(ctx, "bi_h_$k"), k in d.herr, VERDE) { cambiar { x -> x.copy(herr = if (k in x.herr) x.herr - k else x.herr + k) } } }
                        }
                        var nota by remember(fecha, d.nota) { mutableStateOf(d.nota.orEmpty()) }
                        OutlinedTextField(nota, { nota = it.take(600) }, label = { Text(stringResource(R.string.bi_nota)) }, minLines = 2, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                        LaunchedEffect(nota) { if (nota != d.nota.orEmpty()) { delay(700); cambiar { x -> x.copy(nota = nota.trim().ifBlank { null }) } } }
                    }
                }
            }
        }
        if ((clima == "tormenta" || clima == "niebla") && (d.int ?: 0) >= 5) Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(top = 10.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(R.string.bi_apoyo_t), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(stringResource(R.string.bi_apoyo_d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Sub(stringResource(R.string.bi_p_salud))
        val pil = pd?.pilares.orEmpty()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Habito(stringResource(R.string.bi_pil_sueno), d.sueno?.let { "${num1(it)} h" }, stringResource(R.string.bi_meta_sueno), pil["sueno"],
                { cambiar { x -> x.copy(sueno = ((x.sueno ?: 0.0) - 0.5).coerceAtLeast(0.0)) } }, { cambiar { x -> x.copy(sueno = ((x.sueno ?: 0.0) + 0.5).coerceAtMost(14.0)) } })
            Habito(stringResource(R.string.bi_pil_mov), d.mov?.let { "$it min" }, stringResource(R.string.bd_meta_mov_dia), pil["mov"],
                { cambiar { x -> x.copy(mov = ((x.mov ?: 0) - 10).coerceAtLeast(0)) } }, { cambiar { x -> x.copy(mov = ((x.mov ?: 0) + 10).coerceAtMost(600)) } })
            Habito(stringResource(R.string.bi_pil_fyv), d.fyv?.let { "$it" }, stringResource(R.string.bi_meta_fyv), pil["fyv"],
                { cambiar { x -> x.copy(fyv = ((x.fyv ?: 0) - 1).coerceAtLeast(0)) } }, { cambiar { x -> x.copy(fyv = ((x.fyv ?: 0) + 1).coerceAtMost(12)) } })
            Habito(stringResource(R.string.bi_pil_aire), d.aire?.let { "$it min" }, stringResource(R.string.bi_meta_aire), pil["aire"],
                { cambiar { x -> x.copy(aire = ((x.aire ?: 0) - 10).coerceAtLeast(0)) } }, { cambiar { x -> x.copy(aire = ((x.aire ?: 0) + 10).coerceAtMost(600)) } })
            Habito(stringResource(R.string.bi_pil_agua), "$agua / 8", stringResource(R.string.bd_agua_agenda), pil["agua"], null, null)
            Contenedor(pil["pant"]) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.bi_pil_pant), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.bi_meta_pant), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(d.pant == true, { v -> cambiar { x -> x.copy(pant = v) } })
            }
        }
    }
}

@Composable
private fun Ficha(texto: String, sel: Boolean, color: Color, onClick: () -> Unit) {
    Text((if (sel) "✓ " else "") + texto, Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
        .border(1.dp, if (sel) color else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
}

@Composable
private fun Contenedor(nivel: Double?, contenido: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val fondo = when { nivel == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f); nivel >= 0.99 -> VERDE.copy(alpha = 0.12f); else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(fondo).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, content = contenido)
}

@Composable
private fun Habito(nombre: String, valor: String?, meta: String, nivel: Double?, menos: (() -> Unit)?, mas: (() -> Unit)?) {
    Contenedor(nivel) {
        Column(Modifier.weight(1f)) {
            Text(nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text((if (nivel != null && nivel >= 0.99) "✓ " else "") + meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (menos != null) FilledTonalIconButton(onClick = menos, modifier = Modifier.size(40.dp)) { Text("−", fontSize = 18.sp) }
        Text(valor ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.width(72.dp))
        if (mas != null) FilledTonalIconButton(onClick = mas, modifier = Modifier.size(40.dp)) { Text("+", fontSize = 18.sp) }
    }
}

// ---------------------------------------------------------------- El mes

@Composable
private fun Kpi(valor: @Composable () -> Unit, etiqueta: String, sub: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(12.dp)) {
        valor()
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun Cifra(t: String) = Text(t, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

private class KpiD(val valor: @Composable () -> Unit, val etiqueta: String, val sub: String)

@Composable
private fun Kpis(items: List<KpiD>) {
    items.chunked(2).forEach { fila ->
        Row(Modifier.padding(top = 8.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fila.forEach { k -> Kpi(k.valor, k.etiqueta, k.sub, Modifier.weight(1f).fillMaxHeight()) }
            if (fila.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun BarraClimas(clima: Map<String, Int>, alto: Dp) {
    val total = clima.values.sum()
    Row(Modifier.fillMaxWidth().height(alto).clip(RoundedCornerShape(alto / 2)).background(MaterialTheme.colorScheme.surfaceVariant), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (total > 0) Bienestar.CLIMAS.forEach { k -> val n = clima[k] ?: 0; if (n > 0) Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(COLOR.getValue(k))) }
    }
}

@Composable
private fun MetasPilares(a: Bienestar.Acumulado) {
    val ctx = LocalContext.current
    Bienestar.PILARES.forEach { k ->
        val p = if (a.conDatos > 0) (a.metas[k] ?: 0).toFloat() / a.conDatos else 0f
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(txt(ctx, "bi_pil_$k"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(130.dp), maxLines = 2)
            com.rutaalacima.app.ui.components.ProgressLine(p, Modifier.weight(1f), color = VERDE)
            Text("${Math.round(p * 100)} %", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun Repite(a: Bienestar.Acumulado) {
    val ctx = LocalContext.current
    if (a.conClima > 0) {
        Sub(stringResource(R.string.bm_reparto))
        BarraClimas(a.clima, 14.dp)
        FlowRow2 { Bienestar.CLIMAS.forEach { k -> Leyenda(COLOR.getValue(k), "${txt(ctx, "bi_clima_$k")} · ${a.clima[k] ?: 0}") } }
    }
    if (a.emociones.isNotEmpty()) Text(stringResource(R.string.bm_emociones, a.emociones.joinToString(", ") { "${emoTxt(ctx, it.first)} (${it.second})" }), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
    if (a.causas.isNotEmpty()) Text(stringResource(R.string.bi_causas_mal, a.causas.joinToString(", ") { "${causaTxt(ctx, it.first).lowercase()} (${it.second})" }), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    if (a.conDatos > 0) { Sub(stringResource(R.string.bm_metas)); MetasPilares(a) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun FlowRow2(c: @Composable () -> Unit) = FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { c() }
@Composable private fun Leyenda(c: Color, t: String) = Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(c)); Spacer(Modifier.width(6.dp))
    Text(t, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun BienestarMesCard(ym: YearMonth, modifier: Modifier = Modifier, onDia: (LocalDate) -> Unit = {}) {
    val hoy = LocalDate.now(); if (ym.atDay(1).isAfter(hoy)) return
    val vm = rutaViewModel(key = "bien-$ym") { BienestarRangoViewModel(it, listOf(ym)) }
    val datos by vm.datos.collectAsState()
    val ctx = LocalContext.current
    val fechas = (1..ym.lengthOfMonth()).map { ym.atDay(it) }.filter { !it.isAfter(hoy) }
    val a = Bienestar.acumulado(fechas, datos.dias, datos.agua)
    RutaCard(modifier) {
        Cabecera(stringResource(R.string.bm_titulo), if (a.registrados == 0) stringResource(R.string.bm_vacio) else stringResource(R.string.bm_registrados, a.registrados, fechas.size)) {
            a.indice?.let { anillo(it / 100f, 64.dp, "$it", "%") }
        }
        if (a.registrados > 0) Kpis(listOf(
            KpiD({ a.dominante?.let { ClimaIcono(it, Modifier.size(30.dp)) } ?: Cifra("—") }, stringResource(R.string.bm_clima_rep), a.dominante?.let { txt(ctx, "bi_clima_$it") }.orEmpty()),
            KpiD({ Cifra(a.suenoProm?.let { "${num1(it)} h" } ?: "—") }, stringResource(R.string.bm_sueno), stringResource(R.string.bm_noches7, a.metas["sueno"] ?: 0)),
            KpiD({ Cifra("${a.mov} min") }, stringResource(R.string.bi_pil_mov), stringResource(R.string.bm_mov_sem, num1(a.mov / fechas.size.coerceAtLeast(1).toDouble() * 7))),
            KpiD({ Cifra(num1(a.fyvProm)) }, stringResource(R.string.bm_fyv), stringResource(R.string.bm_dias5, a.metas["fyv"] ?: 0)),
            KpiD({ Cifra(num1(a.aguaProm)) }, stringResource(R.string.bm_agua), stringResource(R.string.bm_dias8, a.metas["agua"] ?: 0)),
            KpiD({ Cifra("${a.aire} min") }, stringResource(R.string.bi_pil_aire), stringResource(R.string.bm_pant_n, a.pant)),
        ))
        Sub(stringResource(R.string.bi_cada_dia))
        // Calendario: el ícono del clima de cada día y una línea con los hábitos cumplidos
        val ini = ym.atDay(1).dayOfWeek.value - 1
        val celdas = List(ini) { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
        celdas.chunked(7).forEach { semana ->
            Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                semana.forEach { f ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (f != null) {
                            val d = datos.dias[f]; val c = d?.clima
                            val p = if (!f.isAfter(hoy)) Bienestar.puntaje(d ?: Bienestar.Dia(), datos.agua[f] ?: 0) else null
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                .background(c?.let { COLOR.getValue(it).copy(alpha = 0.16f) } ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .then(if (f == hoy) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier)
                                .clickable { onDia(f) }, contentAlignment = Alignment.Center) {
                                if (c != null) ClimaIcono(c, Modifier.fillMaxWidth(0.7f).aspectRatio(1f))
                                else Text("${f.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (p != null) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).fillMaxWidth(0.64f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outlineVariant)) {
                                    Box(Modifier.fillMaxWidth(p.cumplidos / 6f).fillMaxHeight().background(VERDE))
                                }
                            }
                        }
                    }
                }
                repeat(7 - semana.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Repite(a)
    }
}

// ---------------------------------------------------------------- El año

@Composable
fun BienestarAnioCard(anio: Int, modifier: Modifier = Modifier, onMes: (YearMonth) -> Unit = {}) {
    val hoy = LocalDate.now(); if (anio > hoy.year) return
    val meses = (1..12).map { YearMonth.of(anio, it) }.filter { !it.atDay(1).isAfter(hoy) }
    val vm = rutaViewModel(key = "bien-anio-$anio") { BienestarRangoViewModel(it, meses) }
    val datos by vm.datos.collectAsState()
    val ctx = LocalContext.current
    val fechasDe: (YearMonth) -> List<LocalDate> = { ym -> (1..ym.lengthOfMonth()).map { ym.atDay(it) }.filter { !it.isAfter(hoy) } }
    val porMes = meses.associateWith { Bienestar.acumulado(fechasDe(it), datos.dias, datos.agua) }
    val a = Bienestar.acumulado(meses.flatMap(fechasDe), datos.dias, datos.agua)
    if (a.registrados == 0) return
    RutaCard(modifier) {
        Cabecera(stringResource(R.string.ba_titulo, anio), stringResource(R.string.ba_registrados, a.registrados)) {
            a.indice?.let { anillo(it / 100f, 64.dp, "$it", "%") }
        }
        Kpis(listOf(
            KpiD({ a.dominante?.let { ClimaIcono(it, Modifier.size(30.dp)) } ?: Cifra("—") }, stringResource(R.string.ba_clima), a.dominante?.let { txt(ctx, "bi_clima_$it") }.orEmpty()),
            KpiD({ Cifra(a.suenoProm?.let { "${num1(it)} h" } ?: "—") }, stringResource(R.string.bm_sueno), stringResource(R.string.bm_noches7, a.metas["sueno"] ?: 0)),
            KpiD({ Cifra("${a.mov / 60} h") }, stringResource(R.string.ba_mov), "${a.mov} min"),
            KpiD({ Cifra("${a.pant}") }, stringResource(R.string.ba_pant), stringResource(R.string.ba_aire_h, a.aire / 60)),
        ))
        Sub(stringResource(R.string.ba_mes_a_mes))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..12).forEach { m ->
                val ym = YearMonth.of(anio, m); val x = porMes[ym]
                Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(enabled = x != null) { onMes(ym) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(64.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        val ix = x?.indice
                        if (ix != null) Box(Modifier.width(14.dp).fillMaxHeight((ix / 100f).coerceAtLeast(0.06f)).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp)).background(VERDE))
                    }
                    Text(x?.indice?.toString() ?: "", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                    Box(Modifier.padding(horizontal = 2.dp)) { BarraClimas(x?.clima.orEmpty(), 6.dp) }
                    Text(ym.month.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        FlowRow2 { Leyenda(VERDE, stringResource(R.string.bi_indice)); Bienestar.CLIMAS.forEach { k -> Leyenda(COLOR.getValue(k), txt(ctx, "bi_clima_$k")) } }
        Repite(a)
    }
}
