package com.rutaalacima.app.ui.finanzas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.GraficaAscenso
import com.rutaalacima.app.domain.model.GraficaAscenso.Gran
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.planner.nombreMes
import com.rutaalacima.app.ui.theme.Marca
import com.rutaalacima.app.util.hoy
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/*
 * Gráfica del ascenso, estilo bolsa (igual que la web, web/js/21-grafica.js): rango rápido (1M…Todo),
 * periodo (días, semanas, meses, años), tipo (línea, área, barras, velas del saldo acumulado), series,
 * media móvil y panel de salud financiera en %. Se amplía pellizcando o con + y −, se mueve arrastrando,
 * se lee tocando; doble toque restablece. Un solo eje de dinero a la derecha; los % en su propio panel.
 */

private data class Serie(val k: String, val nombre: Int, val color: @Composable () -> Color)

@Composable
private fun series(): List<Serie> = listOf(
    Serie("saldo", R.string.gr_saldo) { MaterialTheme.colorScheme.onSurface },
    Serie("patr", R.string.gr_patr) { Marca.Oro },
    Serie("ing", R.string.cu_ing) { vc(V_FIJOS) },
    Serie("gas", R.string.cu_gas) { vc(V_VARIABLES) },
    Serie("aho", R.string.cu_aho) { vc(V_AHORRO) },
    Serie("inv", R.string.cu_inv) { vc(V_INVERSION) },
    Serie("disp", R.string.cu_disp) { MaterialTheme.colorScheme.primary },
)

private val RANGOS = listOf(Triple(R.string.gr_r1m, 1, Gran.D), Triple(R.string.gr_r3m, 3, Gran.D), Triple(R.string.gr_r6m, 6, Gran.S),
    Triple(R.string.gr_r1a, 12, Gran.M), Triple(R.string.gr_r5a, 60, Gran.M), Triple(R.string.gr_todo, 0, Gran.M))

private fun compacto(v: Double): String = runCatching {
    android.icu.text.CompactDecimalFormat.getInstance(Locale.getDefault(), android.icu.text.CompactDecimalFormat.CompactStyle.SHORT).apply { maximumFractionDigits = 1 }.format(v)
}.getOrElse { when { kotlin.math.abs(v) >= 1e6 -> "%.1fM".format(v / 1e6); kotlin.math.abs(v) >= 1e3 -> "%.0fk".format(v / 1e3); else -> "%.0f".format(v) } }

private fun pctTxt(v: Double?) = v?.let { "${Math.round(it * 100)} %" } ?: "—"

private fun etqCorta(p: GraficaAscenso.Periodo, g: Gran) = when (g) {
    Gran.D, Gran.S -> "${p.inicio.dayOfMonth} ${nombreMes(p.inicio.monthValue).take(3)}"
    Gran.M -> "${nombreMes(p.inicio.monthValue).take(3)} ${p.inicio.year % 100}"
    Gran.A -> "${p.inicio.year}"
}

@Composable
private fun etqLarga(p: GraficaAscenso.Periodo, g: Gran): String {
    val medio = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    return when (g) {
        Gran.D -> p.inicio.format(medio)
        Gran.S -> stringResource(R.string.gr_semana_de, p.inicio.format(medio))
        Gran.M -> "${nombreMes(p.inicio.monthValue)} ${p.inicio.year}"
        Gran.A -> "${p.inicio.year}"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GraficaAscensoCard(libro: Libro, plata: (Double) -> String) {
    if (libro.dias.isEmpty()) return
    var gran by rememberSaveable { mutableStateOf(Gran.M) }
    var tipo by rememberSaveable { mutableIntStateOf(0) }            // 0 línea, 1 área, 2 barras, 3 velas
    var rango by rememberSaveable { mutableIntStateOf(3) }           // índice en RANGOS; -1 = ventana propia
    var activas by rememberSaveable { mutableStateOf(listOf("saldo", "ing", "gas")) }
    var media by rememberSaveable { mutableStateOf(false) }
    var salud by rememberSaveable { mutableStateOf(true) }
    var tabla by rememberSaveable { mutableStateOf(false) }
    val datos = remember(libro, gran) { GraficaAscenso.de(libro.dias, gran, hoy(), libro.propias) }
    val n = datos.size
    if (n == 0) return
    // ventana visible: desde (en periodos, con fracción para que arrastrar sea suave) y cuántos
    val inicial = if (rango >= 0) GraficaAscenso.cuantos(RANGOS[rango].second, gran).coerceIn(minOf(2, n), n) else n
    var ancho by remember(datos) { mutableFloatStateOf(inicial.toFloat()) }
    var desde by remember(datos) { mutableFloatStateOf((n - inicial).toFloat()) }
    var sel by remember(datos) { mutableIntStateOf(n - 1) }
    fun aplicarRango(i: Int) {
        val c = GraficaAscenso.cuantos(RANGOS[i].second, gran).coerceIn(minOf(2, n), n)
        ancho = c.toFloat(); desde = (n - c).toFloat(); sel = n - 1
    }
    fun limitar() { ancho = ancho.coerceIn(minOf(5, n).toFloat(), n.toFloat()); desde = desde.coerceIn(0f, n - ancho) }
    fun zoom(f: Float, centro: Float = desde + ancho / 2) { val p = (centro - desde) / ancho; ancho /= f; limitar(); desde = centro - p * ancho; limitar(); rango = -1 }

    val i0 = desde.toInt().coerceIn(0, n - 1)
    val i1 = (desde + ancho).toInt().coerceIn(i0 + 1, n) - 1
    val vis = datos.subList(i0, i1 + 1)
    val sers = series()
    val velas = tipo == 3
    val visibles = sers.filter { it.k in activas && !(velas && it.k == "saldo") }
    val colores = visibles.associate { it.k to it.color() }
    val cSaldoArriba = ST_OK; val cSaldoAbajo = ST_BAJO
    val tinta = MaterialTheme.colorScheme.onSurface; val suave = MaterialTheme.colorScheme.onSurfaceVariant
    val linea = MaterialTheme.colorScheme.outlineVariant; val fondo = MaterialTheme.colorScheme.surface
    val pctColores = listOf("flujo" to vc(V_FIJOS), "constr" to vc(V_AHORRO), "gastoP" to vc(V_VARIABLES))
    val medidor = rememberTextMeasurer()
    val estiloEje = TextStyle(fontSize = 10.sp, color = suave)
    val oculto = plata(1.0) == "•••••"
    val selP = datos[sel.coerceIn(0, n - 1)]

    RutaCard {
        Text(stringResource(R.string.gr_titulo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.gr_ayuda), style = MaterialTheme.typography.bodySmall, color = suave)
        // Rango, periodo y tipo
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SingleChoiceSegmentedButtonRow {
                RANGOS.forEachIndexed { i, (t, _, g) ->
                    SegmentedButton(rango == i, { rango = i; gran = if (i == 5 && GraficaAscenso.de(libro.dias, Gran.M, hoy(), libro.propias).size > 60) Gran.A else g; aplicarRango(i) },
                        SegmentedButtonDefaults.itemShape(i, RANGOS.size), icon = {}) { Text(stringResource(t), style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                }
            }
            SingleChoiceSegmentedButtonRow {
                listOf(Gran.D to R.string.gr_dias, Gran.S to R.string.gr_semanas, Gran.M to R.string.gr_meses, Gran.A to R.string.gr_anios).forEachIndexed { i, (g, t) ->
                    SegmentedButton(gran == g, { gran = g; if (rango < 0) rango = 5 }, SegmentedButtonDefaults.itemShape(i, 4), icon = {}) { Text(stringResource(t), style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SingleChoiceSegmentedButtonRow {
                listOf(R.string.gr_linea, R.string.gr_area, R.string.gr_barras, R.string.gr_velas).forEachIndexed { i, t ->
                    SegmentedButton(tipo == i, { tipo = i; if (i == 3 && "saldo" !in activas) activas = activas + "saldo" }, SegmentedButtonDefaults.itemShape(i, 4), icon = {}) {
                        Text(stringResource(t), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
            Row {
                IconButton(onClick = { zoom(1 / 1.5f) }) { Icon(Icons.Filled.Remove, stringResource(R.string.gr_alejar)) }
                IconButton(onClick = { zoom(1.5f) }) { Icon(Icons.Filled.Add, stringResource(R.string.gr_acercar)) }
                IconButton(onClick = { gran = Gran.M; rango = 3; aplicarRango(3) }) { Icon(Icons.Filled.Refresh, stringResource(R.string.gr_restablecer)) }
            }
        }
        // Series e indicadores
        FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sers.forEach { s ->
                val on = s.k in activas
                FilterChip(on, { activas = if (on) (activas - s.k).ifEmpty { listOf("saldo") } else activas + s.k }, { Text(stringResource(s.nombre)) },
                    enabled = !(velas && s.k == "saldo"),
                    leadingIcon = { Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(s.color().copy(alpha = if (on) 1f else 0.35f))) })
            }
            FilterChip(media, { media = !media }, { Text(stringResource(R.string.gr_media)) })
            FilterChip(salud, { salud = !salud }, { Text(stringResource(R.string.gr_salud)) })
        }
        // Lectura del periodo tocado
        Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(etqLarga(selP, gran), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val filas = buildList {
                    if (velas) listOf(R.string.gr_abrio to selP.o, R.string.gr_maximo to selP.h, R.string.gr_minimo to selP.l, R.string.gr_cerro to selP.c)
                        .forEach { (t, v) -> add(Triple(stringResource(t), plata(v), if (selP.c >= selP.o) cSaldoArriba else cSaldoAbajo)) }
                    visibles.forEach { s -> add(Triple(stringResource(s.nombre), plata(selP.serie(s.k)), colores.getValue(s.k))) }
                    if (salud) listOf(R.string.cu_c_flujo to "flujo", R.string.cu_ind_construye to "constr", R.string.cu_ind_gasto to "gastoP").forEachIndexed { i, (t, k) ->
                        add(Triple(stringResource(t), pctTxt(selP.pct(k)), pctColores[i].second)) }
                }
                filas.forEach { (t, v, c) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(c)); Spacer(Modifier.width(5.dp))
                        Text("$t ", style = MaterialTheme.typography.labelSmall, color = suave)
                        Text(v, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        val alto = if (salud) 330.dp else 250.dp
        val desc = "${etqLarga(vis.first(), gran)} — ${etqLarga(vis.last(), gran)}: ${stringResource(R.string.gr_saldo)} ${plata(vis.last().saldo)}"
        Canvas(Modifier.fillMaxWidth().padding(top = 10.dp).height(alto).semantics { contentDescription = desc }
            .pointerInput(datos) {
                detectTransformGestures { centroide, pan, z, _ ->
                    val L = 4.dp.toPx(); val pw = size.width - L - 56.dp.toPx(); val paso = pw / ancho
                    if (z != 1f) zoom(z, desde + ((centroide.x - L) / pw).coerceIn(0f, 1f) * ancho)
                    if (pan.x != 0f) { desde -= pan.x / paso; limitar(); rango = -1 }
                    sel = (desde + ((centroide.x - L) / pw).coerceIn(0f, 0.999f) * ancho).toInt().coerceIn(0, n - 1)
                }
            }
            .pointerInput(datos) {
                detectTapGestures(
                    onTap = { o -> val L = 4.dp.toPx(); val pw = size.width - L - 56.dp.toPx(); sel = (desde + ((o.x - L) / pw).coerceIn(0f, 0.999f) * ancho).toInt().coerceIn(0, n - 1) },
                    onDoubleTap = { gran = Gran.M; rango = 3; aplicarRango(3) },
                )
            }) {
            val L = 4.dp.toPx(); val R = 56.dp.toPx(); val T = 6.dp.toPx(); val xAlto = 18.dp.toPx()
            val pctAlto = if (salud) 78.dp.toPx() else 0f; val gap = if (salud) 14.dp.toPx() else 0f
            val pt = T; val pb = size.height - xAlto - pctAlto - gap
            val nv = vis.size; val pw = size.width - L - R; val paso = pw / nv
            fun cx(i: Int) = L + (i + 0.5f) * paso
            val vals = mutableListOf(0.0); vis.forEach { p -> visibles.forEach { vals += p.serie(it.k) }; if (velas) { vals += p.h; vals += p.l } }
            var lo = vals.min(); var hi = vals.max(); if (hi == lo) hi = lo + 1; val pad = (hi - lo) * 0.06; if (lo < 0) lo -= pad; hi += pad
            fun y(v: Double) = (pt + (hi - v) / (hi - lo) * (pb - pt)).toFloat()
            // cuadrícula y eje derecho
            GraficaAscenso.marcas(lo, hi).forEach { v ->
                drawLine(if (v == 0.0) suave.copy(alpha = 0.6f) else linea, Offset(L, y(v)), Offset(size.width - R, y(v)), 1.dp.toPx())
                val t = medidor.measure(if (oculto) "•••" else compacto(v), estiloEje); drawText(t, topLeft = Offset(size.width - R + 6.dp.toPx(), y(v) - t.size.height / 2f))
            }
            val flujos = visibles.filter { it.k in setOf("ing", "gas", "aho", "inv", "disp") }; val acum = visibles.filter { it.k in setOf("saldo", "patr") }
            fun trazo(k: String, area: Boolean) {
                val c = colores.getValue(k); val path = Path()
                vis.forEachIndexed { i, p -> if (i == 0) path.moveTo(cx(i), y(p.serie(k))) else path.lineTo(cx(i), y(p.serie(k))) }
                if (area) { val a = Path().apply { addPath(path); lineTo(cx(nv - 1), y(maxOf(lo, 0.0))); lineTo(cx(0), y(maxOf(lo, 0.0))); close() }; drawPath(a, c.copy(alpha = 0.14f)) }
                drawPath(path, c, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
            }
            if (tipo == 2 && flujos.isNotEmpty()) {
                val gw = paso * 0.78f; val gapB = 2.dp.toPx(); val bw = ((gw - gapB * (flujos.size - 1)) / flujos.size).coerceAtLeast(1f)
                vis.forEachIndexed { i, p -> flujos.forEachIndexed { j, s ->
                    val v = p.serie(s.k); val ya = y(maxOf(0.0, v)); val yb = y(minOf(0.0, v)); val x0 = cx(i) - gw / 2 + j * (bw + gapB)
                    barra(colores.getValue(s.k), x0, ya, bw, (yb - ya).coerceAtLeast(1f), v >= 0)
                } }
            } else flujos.forEach { trazo(it.k, tipo == 1) }
            acum.forEach { trazo(it.k, tipo == 1 && it.k == "saldo") }
            if (velas) {
                val bw = (paso * 0.66f).coerceIn(1f, 18.dp.toPx())
                vis.forEachIndexed { i, p ->
                    val up = p.c >= p.o; val c = if (up) cSaldoArriba else cSaldoAbajo
                    drawLine(c, Offset(cx(i), y(p.h)), Offset(cx(i), y(p.l)), 1.dp.toPx())
                    val ya = y(maxOf(p.o, p.c)); val h = kotlin.math.abs(y(p.o) - y(p.c)).coerceAtLeast(1f)
                    if (up) { drawRect(fondo, Offset(cx(i) - bw / 2, ya), Size(bw, h)); drawRect(c, Offset(cx(i) - bw / 2, ya), Size(bw, h), style = Stroke(1.5.dp.toPx())) }
                    else drawRect(c, Offset(cx(i) - bw / 2, ya), Size(bw, h))
                }
            }
            if (media) {
                val k = if (velas) "saldo" else (acum.firstOrNull() ?: flujos.firstOrNull())?.k
                if (k != null) {
                    val path = Path(); var empezo = false
                    vis.forEachIndexed { i, _ -> val j = i0 + i; if (j >= 2) { val m = (datos[j].serie(k) + datos[j - 1].serie(k) + datos[j - 2].serie(k)) / 3
                        if (!empezo) { path.moveTo(cx(i), y(m)); empezo = true } else path.lineTo(cx(i), y(m)) } }
                    drawPath(path, suave, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
                }
            }
            // panel de salud en %
            if (salud) {
                val qt = size.height - xAlto - pctAlto; val qb = size.height - xAlto
                val pv = vis.flatMap { p -> listOfNotNull(p.flujo, p.constr, p.gastoP) }
                val plo = minOf(0.0, pv.minOrNull() ?: 0.0).coerceAtLeast(-1.0); val phi = maxOf(0.3, pv.maxOrNull() ?: 0.3).coerceAtMost(1.5)
                fun py(v: Double) = (qt + (phi - v.coerceIn(plo, phi)) / (phi - plo) * (qb - qt)).toFloat()
                drawText(medidor.measure("%", estiloEje), topLeft = Offset(L, qt - 12.dp.toPx()))
                listOf(plo, 0.0, 0.2, phi).distinct().forEach { v ->
                    drawLine(if (v == 0.2) suave else linea, Offset(L, py(v)), Offset(size.width - R, py(v)), 1.dp.toPx(),
                        pathEffect = if (v == 0.2) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())) else null)
                    val t = medidor.measure(pctTxt(v), estiloEje); drawText(t, topLeft = Offset(size.width - R + 6.dp.toPx(), py(v) - t.size.height / 2f))
                }
                pctColores.forEach { (k, c) ->
                    val path = Path(); var abierto = false
                    vis.forEachIndexed { i, p -> val v = p.pct(k); if (v == null) abierto = false else { if (abierto) path.lineTo(cx(i), py(v)) else path.moveTo(cx(i), py(v)); abierto = true } }
                    drawPath(path, c, style = Stroke(1.5.dp.toPx()))
                }
            }
            // eje de tiempo
            val cada = maxOf(1, Math.ceil(64.dp.toPx() / paso.toDouble()).toInt())
            vis.forEachIndexed { i, p -> if ((i0 + i) % cada == 0) {
                val t = medidor.measure(etqCorta(p, gran), estiloEje)
                drawText(t, topLeft = Offset((cx(i) - t.size.width / 2f).coerceIn(0f, size.width - R - t.size.width), size.height - t.size.height))
            } }
            // crucero en el periodo elegido
            val si = sel - i0
            if (si in 0 until nv) {
                drawLine(suave, Offset(cx(si), T), Offset(cx(si), size.height - xAlto), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
                visibles.filter { !(tipo == 2 && it.k in setOf("ing", "gas", "aho", "inv", "disp")) }.forEach { s ->
                    drawCircle(fondo, 6.dp.toPx(), Offset(cx(si), y(vis[si].serie(s.k)))); drawCircle(colores.getValue(s.k), 4.dp.toPx(), Offset(cx(si), y(vis[si].serie(s.k))))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.gr_ventana, etqLarga(vis.first(), gran), etqLarga(vis.last(), gran), vis.size), style = MaterialTheme.typography.labelSmall, color = suave, modifier = Modifier.weight(1f))
            TextButton(onClick = { tabla = !tabla }) { Text(stringResource(if (tabla) R.string.gr_ocultar_tabla else R.string.gr_ver_tabla)) }
        }
        if (tabla) Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).horizontalScroll(rememberScrollState())) {
            val cols = listOf(R.string.gr_periodo, R.string.cu_ing, R.string.cu_gas, R.string.cu_aho, R.string.cu_inv, R.string.cu_disp, R.string.gr_saldo)
            Row(Modifier.background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 6.dp)) {
                cols.forEachIndexed { i, c -> Text(stringResource(c), Modifier.width(if (i == 0) 150.dp else 112.dp).padding(horizontal = 6.dp), style = MaterialTheme.typography.labelSmall,
                    textAlign = if (i == 0) TextAlign.Start else TextAlign.End, color = suave) }
            }
            vis.asReversed().take(120).forEach { p ->
                Row(Modifier.padding(vertical = 5.dp)) {
                    Text(etqLarga(p, gran), Modifier.width(150.dp).padding(horizontal = 6.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    listOf(p.ing, p.gas, p.aho, p.inv, p.disp, p.saldo).forEach { v ->
                        Text(if (v == 0.0) "—" else plata(v), Modifier.width(112.dp).padding(horizontal = 6.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, maxLines = 1,
                            color = if (v < 0) MaterialTheme.colorScheme.error else tinta)
                    }
                }
            }
        }
    }
}

/** Barra con la punta redondeada (la base, sobre el cero, queda recta). */
private fun DrawScope.barra(c: Color, x0: Float, y0: Float, w: Float, h: Float, arriba: Boolean) {
    val r = minOf(4.dp.toPx(), w / 2, h)
    drawRoundRect(c, Offset(x0, y0), Size(w, h), CornerRadius(r))
    if (arriba) drawRect(c, Offset(x0, y0 + h - r), Size(w, r)) else drawRect(c, Offset(x0, y0), Size(w, r))
}

/** En el balance: el acceso a la gráfica, que vive en su propia pantalla (como en la web). */
@Composable
fun GraficaAcceso(onAbrir: () -> Unit) {
    RutaCard(onClick = onAbrir) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CandlestickChart, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.gr_titulo), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.gr_acceso_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}
