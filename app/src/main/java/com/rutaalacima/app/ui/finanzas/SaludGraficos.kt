package com.rutaalacima.app.ui.finanzas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Salud
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.planner.nombreMes
import java.text.NumberFormat
import java.util.Locale

/*
 * Gráficos de la salud financiera, iguales a la web (web/js/19-cuentas-mas.js):
 * · cada peso que entra: barra 100 % apilada (fijos, variables, ahorro, inversión, libre)
 * · indicadores: medidores con la meta marcada y el estado con ícono y palabra (nunca solo color)
 * · lo que tienes y lo que debes: dos barras en la misma escala
 * · mes a mes: flujo neto % y ahorro + inversión % con la meta del 20 % (al tocar se lee cada mes)
 * Paleta validada para daltonismo y contraste; en oscuro, sus propios tonos.
 */

internal val V_FIJOS = Color(0xFF2E8B57) to Color(0xFF45A86E)
internal val V_VARIABLES = Color(0xFFD98A1C) to Color(0xFFC98500)
internal val V_AHORRO = Color(0xFF2A78D6) to Color(0xFF3987E5)
internal val V_INVERSION = Color(0xFFC2447E) to Color(0xFFD55181)
internal val V_LIBRE = Color(0xFFC9D1D4) to Color(0xFF3A4750)
internal val ST_OK = Color(0xFF0CA30C)
internal val ST_MEDIO = Color(0xFFFAB219)
internal val ST_BAJO = Color(0xFFD03B3B)

@Composable internal fun oscuro() = MaterialTheme.colorScheme.surface.luminance() < 0.5f
@Composable internal fun vc(p: Pair<Color, Color>) = if (oscuro()) p.second else p.first
private fun colorSt(e: Salud.Estado) = when (e) { Salud.Estado.OK -> ST_OK; Salud.Estado.MEDIO -> ST_MEDIO; Salud.Estado.BAJO -> ST_BAJO; Salud.Estado.NA -> Color.Gray }

private fun pctG(v: Double) = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 0 }.format(v * 100) + " %"
private fun numG(v: Double) = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 2 }.format(v)

/** Escala del medidor de cada indicador: hasta dónde llega el riel, la meta y si más es mejor. */
private data class Escala(val max: Double, val meta: Double, val alto: Boolean, val meta2: Double? = null)
private val ESCALAS = mapOf(
    "flujo" to Escala(0.4, 0.2, true), "liquidez" to Escala(2.0, 1.2, true), "ahorro" to Escala(0.3, 0.1, true),
    "construye" to Escala(0.4, 0.2, true), "dti" to Escala(0.6, 0.3, false), "gasto" to Escala(1.2, 0.7, false),
    "fondo" to Escala(12.0, 3.0, true, 6.0), "endeuda" to Escala(1.0, 0.3, false), "pasivos" to Escala(0.5, 0.2, true),
    "libertad" to Escala(1.2, 1.0, true), "buenas" to Escala(1.0, 0.5, true), "netoing" to Escala(5.0, 1.0, true),
)

@Composable
private fun metaTexto(k: String, e: Escala) = when (k) {
    "liquidez", "libertad" -> numG(e.meta)
    "fondo" -> stringResource(R.string.cu_meses_x, "3–6")
    "netoing" -> stringResource(R.string.cu_meta_anio)
    else -> pctG(e.meta)
}

/** El estado con ícono y palabra: Bien, Atención, Por mejorar. */
@Composable
fun ChipEstado(e: Salud.Estado, soloIcono: Boolean = false) {
    if (e == Salud.Estado.NA) return
    val (icono, txt) = when (e) {
        Salud.Estado.OK -> Icons.Filled.Check to R.string.cu_est_ok
        Salud.Estado.MEDIO -> Icons.Filled.PriorityHigh to R.string.cu_est_medio
        else -> Icons.Filled.Close to R.string.cu_est_bajo
    }
    if (soloIcono) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(colorSt(e)), contentAlignment = Alignment.Center) {
            Icon(icono, null, Modifier.size(13.dp), tint = if (e == Salud.Estado.MEDIO) Color(0xFF0B0B0B) else Color.White)
        }
        return
    }
    Row(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(colorSt(e)), contentAlignment = Alignment.Center) {
            Icon(icono, null, Modifier.size(11.dp), tint = if (e == Salud.Estado.MEDIO) Color(0xFF0B0B0B) else Color.White)
        }
        Spacer(Modifier.width(4.dp))
        Text(stringResource(txt), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** La escala del ISF: riesgo, moderada, buena y excelente, con el tramo actual resaltado. */
@Composable
fun EscalaIsf(isf: Int) {
    val tramos = listOf(Triple(R.string.cu_riesgo_c, 0, 20), Triple(R.string.cu_nivel_moderada, 20, 50), Triple(R.string.cu_nivel_buena, 50, 80), Triple(R.string.cu_nivel_excelente, 80, 101))
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        tramos.forEach { (n, a, b) ->
            val aqui = isf in a until b
            Column(Modifier.weight((b - a).toFloat()), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (aqui) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest))
                Text(stringResource(n), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), textAlign = TextAlign.Center,
                    fontWeight = if (aqui) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (aqui) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

/** ¿Qué haces con cada peso que entra? Barra apilada con su leyenda (porcentaje y monto al mes). */
@Composable
fun BarraPeso(r: Salud.Resultado, plata: (Double) -> String) {
    val s = r.suma
    if (s.ing <= 0) return
    val libre = s.ing - s.gas - s.aho - s.inv - s.gen
    val partes = listOf(
        Triple(stringResource(R.string.cu_seg_fijos), r.fijos, vc(V_FIJOS)),
        Triple(stringResource(R.string.cu_seg_variables), s.gas - r.fijos, vc(V_VARIABLES)),
        Triple(stringResource(R.string.cu_aho), s.aho, vc(V_AHORRO)),
        Triple(stringResource(R.string.cu_inv), s.inv, vc(V_INVERSION)),
        Triple(stringResource(R.string.cu_gen), s.gen, Color(0xFFB5653A)),
        Triple(stringResource(if (libre >= 0) R.string.cu_seg_libre else R.string.cu_seg_deficit), kotlin.math.abs(libre), vc(V_LIBRE)),
    ).filter { it.second > 0.0 }
    val base = maxOf(s.ing, s.gas + s.aho + s.inv + s.gen)
    Text(stringResource(R.string.cu_peso_titulo), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
    val desc = partes.joinToString(", ") { "${it.first} ${pctG(it.second / s.ing)}" }
    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).semantics { contentDescription = desc }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        partes.forEach { (_, v, c) -> Box(Modifier.weight((v / base).toFloat().coerceAtLeast(0.004f)).height(10.dp).background(c)) }
    }
    if (libre < 0) Text("✕ " + stringResource(R.string.cu_deficit_aviso, plata(Math.round(-libre / r.meses).toDouble())), style = MaterialTheme.typography.bodySmall,
        color = ST_BAJO, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
    // Filas claras (igual que la web): punto, nombre, % y monto al mes; debajo, una pista fina con su parte
    Column(Modifier.padding(top = 12.dp)) {
        partes.forEach { (n, v, c) ->
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(c))
                    Spacer(Modifier.width(10.dp))
                    Text(n, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(pctG(v / s.ing), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.cu_por_mes, plata(Math.round(v / r.meses).toDouble())), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Box(Modifier.padding(start = 18.dp, top = 6.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Box(Modifier.fillMaxWidth((v / s.ing).toFloat().coerceIn(0f, 1f)).height(4.dp).clip(RoundedCornerShape(50)).background(c))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Un indicador: nombre, valor y estado; el riel con la zona buena, la meta y el avance. */
@Composable
fun Medidor(i: Salud.Indicador, nombre: String, ayuda: String, valor: String) {
    val e = ESCALAS[i.clave]
    val colorValor = when (i.estado) { Salud.Estado.OK -> ST_OK; Salud.Estado.BAJO -> ST_BAJO; else -> MaterialTheme.colorScheme.onSurface }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(nombre, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            ChipEstado(i.estado)
        }
        Text(valor, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.headlineMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif),
            fontWeight = FontWeight.SemiBold, color = colorValor)
        if (e != null && i.valor.isFinite()) {
            val pista = MaterialTheme.colorScheme.surfaceContainerHighest
            val zona = ST_OK.copy(alpha = if (oscuro()) 0.22f else 0.16f)
            val relleno = colorSt(i.estado); val tinta = MaterialTheme.colorScheme.onSurface
            val meta = metaTexto(i.clave, e)
            Canvas(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(16.dp).semantics { contentDescription = "$nombre: $valor · $meta" }) {
                val rtl = layoutDirection == LayoutDirection.Rtl
                scale(if (rtl) -1f else 1f, 1f) {
                    val w = size.width; val rh = 10.dp.toPx(); val y0 = (size.height - rh) / 2
                    fun x(v: Double) = (v / e.max).coerceIn(0.0, 1.0).toFloat() * w
                    drawRoundRect(pista, Offset(0f, y0), Size(w, rh), CornerRadius(rh / 2))
                    val (za, zb) = if (e.alto) x(e.meta) to (e.meta2?.let { x(it) } ?: w) else 0f to x(e.meta)
                    drawRoundRect(zona, Offset(za, y0), Size(zb - za, rh), CornerRadius(rh / 2))
                    drawRoundRect(relleno, Offset(0f, y0 + 2.dp.toPx()), Size(x(i.valor).coerceAtLeast(4.dp.toPx()), rh - 4.dp.toPx()), CornerRadius(4.dp.toPx()))
                    listOfNotNull(e.meta, e.meta2).forEach { m -> drawRect(tinta, Offset(x(m) - 1.dp.toPx(), 0f), Size(2.dp.toPx(), size.height)) }
                }
            }
            Text(buildAnnotatedString {
                append("$ayuda ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) { append(stringResource(R.string.cu_meta, meta)) }
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Text(ayuda, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Lo que tienes y lo que debes, en la misma escala, con el patrimonio neto. */
@Composable
fun BalanceBarras(r: Salud.Resultado, plata: (Double) -> String) {
    if (r.activos <= 0 && r.pasivos <= 0) return
    val max = maxOf(r.activos, r.pasivos, 1.0)
    Text(stringResource(R.string.cu_tienes_debes), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))
    listOf(Triple(R.string.cu_activos_c, r.activos, vc(V_FIJOS)), Triple(R.string.cu_pasivos_c, r.pasivos, vc(V_INVERSION))).forEach { (n, v, c) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(n), Modifier.width(76.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.weight(1f).height(14.dp)) {
                Box(Modifier.fillMaxWidth((v / max).toFloat().coerceIn(0.01f, 1f)).height(14.dp).clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).background(c))
            }
            Spacer(Modifier.width(10.dp))
            Text(plata(v), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
    Text(buildAnnotatedString {
        append(stringResource(R.string.cu_c_neto) + " ")
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = if (r.neto < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)) { append(plata(r.neto)) }
        if (r.activos > 0) append(" · " + stringResource(R.string.cu_debes_de, pctG(r.pasivos / r.activos)))
    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
/** Mes a mes: flujo neto y ahorro + inversión sobre lo que entra, con la meta del 20 %. Se lee al tocar o deslizar. */
@Composable
fun LineaMeses(anio: Int?, libro: Libro) {
    val meses = remember(libro, anio) {
        libro.dias.keys.map { it.take(7) }.distinct().sorted().let { l -> if (anio != null) l.filter { it.startsWith("$anio-") } else l.takeLast(12) }
    }
    if (meses.size < 2) return
    val datos = remember(libro, meses) {
        meses.map { m -> val s = libro.suma("$m-"); Triple(m, if (s.ing > 0) (s.ing - s.gas - s.aho - s.inv - s.gen) / s.ing else null, if (s.ing > 0) (s.aho + s.inv) / s.ing else null) }
    }
    var sel by remember(datos) { mutableIntStateOf(datos.lastIndex) }
    val cFlujo = vc(V_FIJOS); val cCons = vc(V_AHORRO)
    val guia = MaterialTheme.colorScheme.outlineVariant; val suave = MaterialTheme.colorScheme.onSurfaceVariant; val fondo = MaterialTheme.colorScheme.surface
    val medidor = rememberTextMeasurer()
    val estiloEje = TextStyle(fontSize = 10.sp, color = suave)
    fun etq(m: String) = nombreMes(m.drop(5).toInt()).take(3) + if (anio == null) " " + m.substring(2, 4) else ""

    Text(stringResource(R.string.cu_mes_a_mes), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(Triple(R.string.cu_c_flujo, cFlujo, false), Triple(R.string.cu_ind_construye, cCons, false), Triple(R.string.cu_meta20, suave, true)).forEach { (n, c, discont) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.width(16.dp).height(8.dp)) {
                    drawLine(c, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2.dp.toPx(),
                        pathEffect = if (discont) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null)
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(n), style = MaterialTheme.typography.labelSmall, color = suave)
            }
        }
    }
    // Lectura del mes elegido
    val (mSel, fSel, cSel) = datos[sel]
    Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${nombreMes(mSel.drop(5).toInt())} ${mSel.take(4)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        listOf(cFlujo to fSel, cCons to cSel).forEach { (c, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(4.dp))
                Text(v?.let { pctG(it) } ?: "—", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    val vals = datos.flatMap { listOfNotNull(it.second, it.third) }
    val lo = minOf(0.0, vals.minOrNull() ?: 0.0); val hi = maxOf(0.3, vals.maxOrNull() ?: 0.3)
    val desc = datos.joinToString("; ") { (m, f, c) -> "${etq(m)}: ${f?.let { pctG(it) } ?: "—"}, ${c?.let { pctG(it) } ?: "—"}" }
    Canvas(Modifier.fillMaxWidth().padding(top = 8.dp).height(180.dp)
        .semantics { contentDescription = desc }
        .pointerInput(datos) {
            fun elegir(px: Float) { val l = 34.dp.toPx(); val r = 8.dp.toPx(); sel = (((px - l) / (size.width - l - r)) * (datos.size - 1)).let { Math.round(it) }.coerceIn(0, datos.lastIndex) }
            detectTapGestures { elegir(it.x) }
        }
        .pointerInput(datos) {
            val l = 34.dp.toPx(); val r = 8.dp.toPx()
            detectHorizontalDragGestures { c, _ -> sel = Math.round(((c.position.x - l) / (size.width - l - r)) * (datos.size - 1)).coerceIn(0, datos.lastIndex) }
        }) {
        val L = 34.dp.toPx(); val R = 8.dp.toPx(); val T = 8.dp.toPx(); val B = 20.dp.toPx()
        fun x(i: Int) = L + i * (size.width - L - R) / (datos.size - 1)
        fun y(v: Double) = (T + (hi - v) / (hi - lo) * (size.height - T - B)).toFloat()
        listOf(lo, 0.0, 0.2, hi).distinct().forEach { v ->
            val yy = y(v)
            drawLine(guia, Offset(L, yy), Offset(size.width - R, yy), 1.dp.toPx(),
                pathEffect = if (v == 0.2) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())) else null)
            val t = medidor.measure(pctG(v), estiloEje)
            drawText(t, topLeft = Offset(0f, yy - t.size.height / 2f))
        }
        drawLine(suave, Offset(x(sel), T), Offset(x(sel), size.height - B), 1.dp.toPx())
        listOf(cFlujo to { d: Triple<String, Double?, Double?> -> d.second }, cCons to { d: Triple<String, Double?, Double?> -> d.third }).forEach { (c, f) ->
            val camino = androidx.compose.ui.graphics.Path(); var abierto = false
            datos.forEachIndexed { i, d -> val v = f(d); if (v == null) abierto = false else { if (abierto) camino.lineTo(x(i), y(v)) else camino.moveTo(x(i), y(v)); abierto = true } }
            drawPath(camino, c, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            datos.forEachIndexed { i, d -> f(d)?.let { v ->
                drawCircle(fondo, if (i == sel) 6.dp.toPx() else 5.dp.toPx(), Offset(x(i), y(v)))
                drawCircle(c, if (i == sel) 4.5.dp.toPx() else 3.5.dp.toPx(), Offset(x(i), y(v)))
            } }
        }
        val paso = if (datos.size > 8) 2 else 1
        datos.forEachIndexed { i, d ->
            if (i % paso == 0 || i == sel) {
                val t = medidor.measure(etq(d.first), estiloEje.copy(fontWeight = if (i == sel) FontWeight.SemiBold else FontWeight.Normal))
                drawText(t, topLeft = Offset((x(i) - t.size.width / 2f).coerceIn(0f, size.width - t.size.width), size.height - t.size.height))
            }
        }
    }
    Text(stringResource(R.string.cu_toca_mes_lectura), style = MaterialTheme.typography.labelSmall, color = suave)
}

/** En el balance y en el año: la salud en una línea, con su ícono. Lleva a su pantalla. */
@Composable
fun SaludAcceso(r: Salud.Resultado, anio: Int?, onAbrir: () -> Unit) {
    val isf = r.isf ?: return
    val nivel = stringResource(when (r.nivel!!) {
        Salud.Nivel.EXCELENTE -> R.string.cu_nivel_excelente; Salud.Nivel.BUENA -> R.string.cu_nivel_buena
        Salud.Nivel.MODERADA -> R.string.cu_nivel_moderada; Salud.Nivel.RIESGO -> R.string.cu_nivel_riesgo
    })
    val conEstado = r.indicadores.filter { it.estado != Salud.Estado.NA }
    RutaCard(onClick = onAbrir) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MonitorHeart, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text((if (anio != null) stringResource(R.string.cu_salud_de, "$anio") else stringResource(R.string.cu_salud)) + ": $isf % · $nivel", style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.cu_salud_sub, conEstado.count { it.estado == Salud.Estado.OK }, conEstado.size),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

/** La pantalla de salud financiera: el periodo (últimos 12 meses o un año) y todos sus gráficos. */
fun LazyListScope.saludNivel(anio: Int?, libro: Libro, plata: (Double) -> String, ir: (NivelCuentas) -> Unit) {
    item(key = "periodos") {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(anio == null, { ir(NivelCuentas.SaludN(null)) }, { Text(stringResource(R.string.cu_ultimos_12)) }, leadingIcon = if (anio == null) { { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, androidx.compose.ui.Modifier.size(androidx.compose.material3.FilterChipDefaults.IconSize)) } } else null)
            libro.anios.sortedDescending().forEach { a -> FilterChip(anio == a, { ir(NivelCuentas.SaludN(a)) }, { Text("$a") }, leadingIcon = if (anio == a) { { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, androidx.compose.ui.Modifier.size(androidx.compose.material3.FilterChipDefaults.IconSize)) } } else null) }
        }
    }
    val r = libro.salud(anio)
    item(key = "salud-${anio ?: 0}") {
        if (r.suma.n == 0) RutaCard { Text(stringResource(R.string.cu_salud_vacia), style = MaterialTheme.typography.bodyMedium) }
        else SaludCard(r, anio, libro, plata)
    }
    item(key = "atajos-salud") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { ir(NivelCuentas.PatrimonioN) }, Modifier.weight(1f)) {
                Icon(Icons.Filled.SwapVert, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.cu_patrimonio_titulo), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = { ir(NivelCuentas.DeudasN) }, Modifier.weight(1f)) {
                Icon(Icons.Filled.CreditCard, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.cu_deudas), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Botón para mostrar u ocultar la tabla con las cifras exactas. */
@Composable
fun VerCifras(abierto: Boolean, onCambio: () -> Unit) {
    TextButton(onClick = onCambio, modifier = Modifier.padding(top = 8.dp)) {
        Text(stringResource(if (abierto) R.string.cu_ocultar_cifras else R.string.cu_ver_cifras))
    }
}
