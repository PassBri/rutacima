package com.rutaalacima.app.ui.finanzas

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Cuentas
import com.rutaalacima.app.domain.model.Cuentas.Tipo
import com.rutaalacima.app.domain.model.Finanzas
import com.rutaalacima.app.util.hoy
import java.time.LocalDate
import java.time.YearMonth

/* =====================================================================================
 * La barra del Ascenso financiero (igual que web/js/46-barra-finanzas.js):
 * año con sus 12 meses (aro verde si quedó disponible, burdeos si no; con presupuesto, semáforo de gasto),
 * el mes con sus días (puntos por tipo), atajos, la cordillera del patrimonio y «así va el mes».
 * En el teléfono va arriba y se pliega tocando el título.
 * ===================================================================================== */

private fun nombreMesCorto(m: Int): String = java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()).replace(".", "")
    .replaceFirstChar { it.uppercase() }
private fun nombreMesLargo(m: Int): String = java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, java.util.Locale.getDefault())
    .replaceFirstChar { it.uppercase() }

/** Cómo cerró un periodo: verde si sobró (o quedó en cero pero ahorró o invirtió: todo tuvo destino), dorado si quedó en cero sin ahorro, burdeos si salió más. */
private fun colorCierre(t: Cuentas.Suma, verde: Color, oro: Color, burdeos: Color): Color = when {
    t.disponible < -0.5 -> burdeos
    kotlin.math.abs(t.disponible) < 0.5 -> if (t.construido > 0) verde else oro
    else -> verde
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BarraFinanzas(nivel: NivelCuentas, libro: Libro, plata: (Double) -> String, ir: (NivelCuentas) -> Unit, nacimiento: Int? = null, vida: Int = 100, onInforme: () -> Unit) {
    val h = hoy()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // Lo que muestra el centro manda; las flechas solo miran otro año o mes sin abrirlo
    val lugarA = when (nivel) { is NivelCuentas.Anio -> nivel.a; is NivelCuentas.Mes -> nivel.ym.year; is NivelCuentas.Dia -> nivel.f.year; else -> null }
    val lugarM = when (nivel) { is NivelCuentas.Mes -> nivel.ym.monthValue; is NivelCuentas.Dia -> nivel.f.monthValue; else -> null }
    var verA by remember(nivel) { mutableIntStateOf(lugarA ?: h.year) }
    var verM by remember(nivel) { mutableIntStateOf(lugarM ?: if (lugarA != null && lugarA != h.year) 1 else h.monthValue) }
    val prefs = remember { ctx.getSharedPreferences("suroute_ui", android.content.Context.MODE_PRIVATE) }
    // Lo abierto junto al riel: -1 nada, 0 calendario, 2 resumen (se recuerda)
    var panel by rememberSaveable { mutableIntStateOf(prefs.getInt("fb_panel", -1).let { if (it == 0 || it == 2) it else -1 }) }
    val cambiarPanel: (Int) -> Unit = { k -> panel = if (panel == k) -1 else k; prefs.edit().putInt("fb_panel", panel).apply() }
    val verdeRiel = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF8CC79A) else Color(0xFF2E6B3A)
    var mapa by rememberSaveable { mutableStateOf(false) }

    // Totales por día y por mes, una sola vez por libro
    val porDia = remember(libro) { libro.dias.mapValues { (_, xs) -> Cuentas.sumar(xs, libro.propias) to xs.filter { !it.prog }.map { Tipo.de(it.t) }.toSet() } }
    val porMes = remember(libro) { libro.dias.keys.groupBy { it.take(7) }.mapValues { (_, fs) -> Cuentas.sumar(fs.flatMap { libro.dias[it].orEmpty() }, libro.propias) } }
    val porAnio = remember(libro) { libro.dias.keys.groupBy { it.take(4) }.mapValues { (_, fs) -> Cuentas.sumar(fs.flatMap { libro.dias[it].orEmpty() }, libro.propias) } }

    val donde = when (nivel) {
        is NivelCuentas.Dia -> "${nivel.f.dayOfMonth} ${nombreMesCorto(nivel.f.monthValue)} ${nivel.f.year}"
        is NivelCuentas.Mes -> "${nombreMesLargo(nivel.ym.monthValue)} ${nivel.ym.year}"
        is NivelCuentas.Anio -> "${nivel.a}"
        else -> stringResource(R.string.cu_titulo)
    }
    val verde = colorTipo(Tipo.ING); val burdeos = MaterialTheme.colorScheme.primary; val oro = MaterialTheme.colorScheme.secondary
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column {
            // Riel de íconos (como la barra de la izquierda en la web): subir, calendario, resumen y los atajos uno tras otro,
            // solo íconos verdes; el nombre aparece al dejar el dedo encima. Calendario y resumen se despliegan debajo.
            val atajos = listOf<Triple<NivelCuentas?, androidx.compose.ui.graphics.vector.ImageVector, Int>>(
                Triple(NivelCuentas.Balance, Icons.Filled.AttachMoney, R.string.cu_balance), Triple(NivelCuentas.FijosN, Icons.Filled.Repeat, R.string.fb_fijos),
                Triple(NivelCuentas.DeudasN, Icons.Filled.CreditCard, R.string.cu_deudas), Triple(NivelCuentas.MetasN, Icons.Filled.Savings, R.string.mf_titulo),
                Triple(NivelCuentas.SaludN(null), Icons.Filled.MonitorHeart, R.string.cu_salud), Triple(NivelCuentas.GraficaN, Icons.Filled.CandlestickChart, R.string.gr_titulo),
                Triple(NivelCuentas.PatrimonioN, Icons.Filled.AccountBalance, R.string.cu_patrimonio_titulo), Triple(null, Icons.Filled.PictureAsPdf, R.string.fb_informe),
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                BotonRiel(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.fb_subir), false, verdeRiel, enabled = nivel.arriba() != null) { nivel.arriba()?.let(ir) }
                BotonRiel(Icons.Filled.CalendarMonth, stringResource(R.string.fb_tab_calendario), panel == 0, verdeRiel) { cambiarPanel(0) }
                BotonRiel(Icons.AutoMirrored.Filled.ViewList, stringResource(R.string.fb_tab_resumen), panel == 2, verdeRiel) { cambiarPanel(2) }
                Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                atajos.forEach { (n, ic, s) ->
                    BotonRiel(ic, stringResource(s), n != null && n == nivel, verdeRiel) { if (n == null) onInforme() else ir(n) }
                }
            }
            AnimatedVisibility(panel >= 0) {
                Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (panel == 0) donde else stringResource(R.string.fb_tab_resumen), style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                            color = burdeos, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 4.dp))
                        if (panel == 0) OutlinedButton(onClick = { ir(NivelCuentas.Dia(h)) }) { Text(stringResource(R.string.fb_hoy)) }
                        IconButton(onClick = { cambiarPanel(panel) }) { Icon(Icons.Filled.Close, stringResource(R.string.fb_ocultar)) }
                    }
                    if (panel == 0) {
                    // Año y sus 12 meses
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { verA-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.cu_anterior)) }
                        // Tocar el año despliega todos tus años (igual que Mi ruta)
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { mapa = !mapa }.padding(4.dp)
                            .semantics { contentDescription = "$verA" }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$verA", style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif), fontWeight = FontWeight.SemiBold)
                                Icon(if (mapa) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, stringResource(R.string.fb_todos_anios), Modifier.size(18.dp))
                            }
                            Text(porAnio["$verA"]?.let { stringResource(R.string.fb_libre, plata(it.disponible)) } ?: stringResource(R.string.fb_sin_mov), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { verA++ }, enabled = verA <= h.year) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.siguiente)) }
                    }
                    if (mapa && nacimiento != null) {
                        // Todos tus años en miniatura: un aro por año de vida, una fila por década; el color dice cómo cerró
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                (0 until vida).chunked(10).forEach { dec ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${dec.first()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(20.dp))
                                        dec.forEach { i ->
                                            val y = nacimiento + i; val t = porAnio["$y"]
                                            val c = when { t != null && t.n > 0 -> colorCierre(t, verde, oro, burdeos); y <= h.year -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f); else -> MaterialTheme.colorScheme.outlineVariant }
                                            Box(Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(if (y == verA) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                                .clickable { if (y == verA) { mapa = false; ir(NivelCuentas.Anio(y)) } else verA = y }
                                                .semantics { contentDescription = "$y" }, contentAlignment = Alignment.Center) {
                                                Canvas(Modifier.fillMaxWidth(0.7f).aspectRatio(1f)) {
                                                    drawCircle(c, size.minDimension / 2 - 1.5.dp.toPx(), style = Stroke((if (y == h.year) 2.4 else if (t != null && t.n > 0) 2.0 else 1.2).dp.toPx()))
                                                }
                                            }
                                        }
                                    }
                                }
                                Text(stringResource(R.string.fb_mapa_ayuda), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                                TextButton(onClick = { mapa = false; ir(NivelCuentas.Balance) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.fb_balance_todos)) }
                            }
                        }
                    } else if (mapa) {
                        val desde = minOf(h.year, porAnio.keys.mapNotNull { it.toIntOrNull() }.minOrNull() ?: h.year)
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ((h.year + 1) downTo desde).chunked(3).forEach { fila ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        fila.forEach { y ->
                                            val t = porAnio["$y"]; val viendo = y == verA
                                            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                                .background(if (viendo) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                                                .border(1.5.dp, if (t == null || t.n == 0) MaterialTheme.colorScheme.outlineVariant else colorCierre(t, verde, oro, burdeos).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                                .clickable { if (viendo) { mapa = false; ir(NivelCuentas.Anio(y)) } else verA = y }.padding(vertical = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("$y", style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Serif), color = if (y == h.year) burdeos else MaterialTheme.colorScheme.onSurface)
                                                Text(if (t != null && t.n > 0) plata(t.disponible) else "—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                                Text(stringResource(R.string.fb_mapa_ayuda), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                TextButton(onClick = { mapa = false; ir(NivelCuentas.Balance) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.fb_balance_todos)) }
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        (1..12).chunked(4).forEach { fila ->
                            Row(Modifier.fillMaxWidth()) {
                                fila.forEach { k ->
                                    val ym = YearMonth.of(verA, k); val t = porMes[ym.toString()]
                                    val pres = kotlin.math.abs(Finanzas.monto(libro.presupuesto(ym)))
                                    val (fr, color) = when {
                                        t == null || t.n == 0 -> 0f to MaterialTheme.colorScheme.outlineVariant
                                        pres > 0 -> (t.gas / pres).let { u -> u.toFloat().coerceIn(0f, 1f) to (if (u > 1) burdeos else if (u > 0.8) oro else verde) }
                                        else -> (if (t.ing > 0) ((maxOf(0.0, t.disponible) + t.construido) / t.ing).toFloat().coerceIn(0.12f, 1f) else 1f) to colorCierre(t, verde, oro, burdeos)
                                    }
                                    val abierto = lugarA == verA && lugarM == k; val actual = verA == h.year && k == h.monthValue
                                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (abierto) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                        .clickable { ir(NivelCuentas.Mes(ym)) }.padding(vertical = 6.dp)
                                        .semantics { contentDescription = "${nombreMesLargo(k)} $verA" }, horizontalAlignment = Alignment.CenterHorizontally) {
                                        val base = MaterialTheme.colorScheme.outlineVariant
                                        Canvas(Modifier.size(28.dp)) {
                                            val r = size.minDimension / 2 - 3.dp.toPx()
                                            drawCircle(base, r, style = Stroke(2.4.dp.toPx()))
                                            if (fr > 0f) drawArc(color, -90f, 360f * fr, false, Offset(center.x - r, center.y - r), androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
                                            if (t != null && t.n > 0) drawCircle(color, 2.4.dp.toPx())
                                            if (actual) drawCircle(burdeos, r + 2.6.dp.toPx(), style = Stroke(1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f))))
                                        }
                                        Text(nombreMesCorto(k), style = MaterialTheme.typography.labelSmall, color = if (abierto) burdeos else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    // El mes y sus días
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (verM == 1) { verM = 12; verA-- } else verM-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.cu_anterior)) }
                        TextButton(onClick = { ir(NivelCuentas.Mes(YearMonth.of(verA, verM))) }, modifier = Modifier.weight(1f)) {
                            Text(nombreMesLargo(verM), style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Serif))
                        }
                        IconButton(onClick = { if (verM == 12) { verM = 1; verA++ } else verM++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.siguiente)) }
                    }
                    val ym = YearMonth.of(verA, verM); val ini = ym.atDay(1).dayOfWeek.value - 1
                    val celdas = List(ini) { null } + (1..ym.lengthOfMonth()).toList()
                    celdas.chunked(7).forEach { semana ->
                        Row(Modifier.fillMaxWidth()) {
                            (0 until 7).forEach { j ->
                                val d = semana.getOrNull(j)
                                Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                    if (d != null) {
                                        val f = ym.atDay(d); val x = porDia[f.toString()]; val esHoy = f == h
                                        val abierto = nivel is NivelCuentas.Dia && nivel.f == f
                                        Column(Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape)
                                            .background(when { abierto -> burdeos; x != null -> verde.copy(alpha = 0.10f); else -> Color.Transparent })
                                            .then(if (esHoy && !abierto) Modifier.border(1.5.dp, burdeos, CircleShape) else Modifier)
                                            .clickable { ir(NivelCuentas.Dia(f)) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Text("$d", fontSize = 12.sp, fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal,
                                                color = if (abierto) MaterialTheme.colorScheme.onPrimary else if (esHoy) burdeos else MaterialTheme.colorScheme.onSurface)
                                            x?.second?.let { tipos -> Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Tipo.entries.filter { it in tipos }.forEach { tp -> Box(Modifier.size(4.dp).clip(CircleShape).background(colorTipo(tp))) } } }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Leyenda de los puntos de cada día
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(Tipo.ING to R.string.cu_ing1, Tipo.GAS to R.string.cu_gas1, Tipo.AHO to R.string.cu_aho1, Tipo.INV to R.string.cu_inv1, Tipo.GEN to R.string.cu_gen).forEach { (tp, s) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(colorTipo(tp))); Spacer(Modifier.width(3.dp))
                                Text(stringResource(s), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                    } else {
                        AsiVaElMes(libro, porMes, porDia.keys, plata, verde, burdeos, oro) { ir(NivelCuentas.Mes(YearMonth.from(h))) }
                        Cordillera(porAnio, verA, plata) { ir(NivelCuentas.Anio(it)) }
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

/** La cordillera del patrimonio: un pico por año, alto según lo construido (acumulado) en ahorro e inversión. */
@Composable
private fun Cordillera(porAnio: Map<String, Cuentas.Suma>, viendo: Int, plata: (Double) -> String, onAnio: (Int) -> Unit) {
    val anios = porAnio.keys.mapNotNull { it.toIntOrNull() }.sorted(); if (anios.isEmpty()) return
    var acum = 0.0
    val picos = anios.map { a -> acum += maxOf(0.0, porAnio["$a"]!!.construido); a to acum }
    val max = picos.maxOf { it.second }.coerceAtLeast(1.0)
    val burdeos = MaterialTheme.colorScheme.primary; val fondo = MaterialTheme.colorScheme.surface
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.fb_cordillera), style = MaterialTheme.typography.labelLarge)
        Box(Modifier.fillMaxWidth().height(72.dp)) {
            Canvas(Modifier.fillMaxWidth().height(72.dp)) {
                val paso = size.width / (picos.size + 1)
                val pts = picos.mapIndexed { i, (_, v) -> Offset(paso * (i + 1), size.height - 10f - (v / max).toFloat() * (size.height - 26f)) }
                val p = Path().apply { moveTo(0f, size.height); pts.forEachIndexed { i, o -> lineTo(o.x - paso / 2, size.height - if (i == 0) 6f else 12f); lineTo(o.x, o.y) }; lineTo(size.width, size.height - 6f); lineTo(size.width, size.height); close() }
                drawPath(p, Brush.verticalGradient(listOf(burdeos.copy(alpha = 0.5f), burdeos.copy(alpha = 0.06f))))
                drawPath(p, burdeos, style = Stroke(1.4.dp.toPx()))
                pts.forEachIndexed { i, o -> drawCircle(if (picos[i].first == viendo) burdeos else fondo, 4.dp.toPx(), o); drawCircle(burdeos, 4.dp.toPx(), o, style = Stroke(1.6.dp.toPx())) }
            }
            Row(Modifier.fillMaxWidth().height(72.dp)) {
                Spacer(Modifier.weight(0.5f))
                picos.forEach { (a, _) -> Box(Modifier.weight(1f).height(72.dp).clickable { onAnio(a) }.semantics { contentDescription = "$a" }, contentAlignment = Alignment.BottomCenter) {
                    Text("'${a % 100}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                Spacer(Modifier.weight(0.5f))
            }
        }
        Text(stringResource(R.string.fb_construido, plata(acum)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Así va el mes: disponible, presupuesto gastado, días para cerrar, racha de registro y la frase financiera del día. */
@Composable
private fun AsiVaElMes(libro: Libro, porMes: Map<String, Cuentas.Suma>, diasCon: Set<String>, plata: (Double) -> String, verde: Color, burdeos: Color, oro: Color, onPresupuesto: () -> Unit) {
    val h = hoy(); val ym = YearMonth.from(h); val t = porMes[ym.toString()]
    val pres = kotlin.math.abs(Finanzas.monto(libro.presupuesto(ym))); val uso = if (pres > 0) (t?.gas ?: 0.0) / pres else null
    val faltan = ym.lengthOfMonth() - h.dayOfMonth
    var d = if (h.toString() in diasCon) h else h.minusDays(1); var racha = 0
    while (d.toString() in diasCon && racha < 3650) { racha++; d = d.minusDays(1) }
    val frases = androidx.compose.ui.res.stringArrayResource(R.array.fb_frases)
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.fb_asi_va, nombreMesLargo(h.monthValue).lowercase()), style = MaterialTheme.typography.labelLarge)
            Row {
                Column(Modifier.weight(1f)) {
                    Text(plata(t?.disponible ?: 0.0), style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif), color = if ((t?.disponible ?: 0.0) < 0) burdeos else MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.fb_disponible), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text("$faltan", style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif))
                    Text(stringResource(R.string.fb_dias_cerrar), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (uso != null) {
                LinearProgressIndicator(progress = { uso.toFloat().coerceIn(0f, 1f) }, Modifier.fillMaxWidth(), color = if (uso > 1) burdeos else if (uso > 0.8) oro else verde)
                Text(stringResource(R.string.fb_pres_gastado, (uso * 100).toInt()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else TextButton(onClick = onPresupuesto, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text(stringResource(R.string.fb_poner_pres)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Terrain, null, tint = burdeos, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                Text(if (racha > 0) stringResource(R.string.fb_racha, racha) else stringResource(R.string.fb_sin_racha), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (frases.isNotEmpty()) Text(frases[(h.toEpochDay() % frases.size).toInt()], style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif), textAlign = TextAlign.Start)
        }
    }
}

/** Botón del riel: solo ícono verde en un círculo; el activo lleva un fondo verde suave. El nombre sale al dejar el dedo encima. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BotonRiel(icono: androidx.compose.ui.graphics.vector.ImageVector, nombre: String, activo: Boolean, verde: Color, enabled: Boolean = true, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(nombre) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, enabled = enabled,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(if (activo) verde.copy(alpha = 0.14f) else Color.Transparent)
                .semantics { selected = activo }) {
            Icon(icono, nombre, tint = if (enabled) verde else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
        }
    }
}
