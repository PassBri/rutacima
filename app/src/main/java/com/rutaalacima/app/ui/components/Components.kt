package com.rutaalacima.app.ui.components

import androidx.compose.runtime.remember
import com.rutaalacima.app.ui.theme.flotante
import com.rutaalacima.app.ui.theme.hojaPapel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rutaalacima.app.ui.i18n.texto
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.RutaApp
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.ui.theme.asColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Crea un ViewModel con acceso al contenedor de dependencias de la app. */
@Composable
inline fun <reified VM : ViewModel> rutaViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as RutaApp).container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

/** Título de sección con la línea dorada característica de los workbooks. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(36.dp).height(3.dp).background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp)))
    }
}

/**
 * Tarjeta de la app como hoja de papel: sombra cálida, pliegue diagonal y esquina doblada.
 * Si es tocable, flota y se hunde al tocarla (Material 3: estado presionado visible).
 */
@Composable
fun RutaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val forma = remember { com.rutaalacima.app.ui.theme.FormaHoja() }
    val toque = com.rutaalacima.app.ui.theme.rememberToque()
    val base = modifier.fillMaxWidth()
        .then(if (onClick != null) Modifier.flotante(toque, forma, elevacion = 0.dp) else Modifier)
        .hojaPapel()
    Column(
        (if (onClick != null) base.clip(forma).clickable(interactionSource = toque, indication = androidx.compose.material3.ripple(), onClick = onClick)
        else base).padding(16.dp),
        content = content,
    )
}

@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondary) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
    )
}

/** Selector 1–10 en forma de fichas (equivale a "encierra tu nivel"). */
@Composable
fun ScaleSelector(
    value: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 10,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (n in min..max) {
            val selected = value == n
            val filled = value != null && n <= value
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (filled) color.copy(alpha = if (selected) 1f else 0.25f) else Color.Transparent)
                    .border(1.dp, if (filled) color else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onChange(if (selected) null else n) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$n",
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Gráfico de radar de los 6 ejes. [valores] en el orden de [Eje.entries], escala 0–10.
 * [comparacion] dibuja una segunda evaluación (por ejemplo, la anterior) en línea punteada.
 */
@Composable
fun RadarEjes(
    valores: List<Int>,
    modifier: Modifier = Modifier,
    comparacion: List<Int>? = null,
    mostrarEtiquetas: Boolean = true,
) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.secondary
    val line = MaterialTheme.colorScheme.primary
    val prev = MaterialTheme.colorScheme.onSurfaceVariant
    val ejes = Eje.entries
    // TalkBack lee el radar como una lista de ejes con su puntaje
    val nombres = ejes.map { androidx.compose.ui.res.stringResource(com.rutaalacima.app.ui.i18n.Textos.nombre(it)) }
    val descripcion = nombres.zip(valores).joinToString(", ") { (n, v) -> "$n $v/10" }
    Box(modifier.fillMaxWidth().aspectRatio(1f).semantics(mergeDescendants = true) { contentDescription = descripcion }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth(if (mostrarEtiquetas) 0.72f else 0.95f).aspectRatio(1f)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension / 2
            fun punto(i: Int, v: Float): Offset {
                val ang = -PI / 2 + i * 2 * PI / ejes.size
                return Offset(c.x + (r * v * cos(ang)).toFloat(), c.y + (r * v * sin(ang)).toFloat())
            }
            for (nivel in 1..5) {
                val p = Path()
                ejes.indices.forEach { i ->
                    val pt = punto(i, nivel / 5f)
                    if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
                }
                p.close()
                drawPath(p, grid, style = Stroke(width = 1.dp.toPx()))
            }
            ejes.indices.forEach { i -> drawLine(grid, c, punto(i, 1f), strokeWidth = 1.dp.toPx()) }
            fun poligono(vals: List<Int>): Path = Path().apply {
                vals.forEachIndexed { i, v ->
                    val pt = punto(i, (v.coerceIn(0, 10)) / 10f)
                    if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
                close()
            }
            comparacion?.takeIf { it.size == ejes.size }?.let {
                drawPath(poligono(it), prev.copy(alpha = 0.6f), style = Stroke(width = 1.5.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
            }
            if (valores.size == ejes.size) {
                val p = poligono(valores)
                drawPath(p, fill.copy(alpha = 0.35f))
                drawPath(p, line, style = Stroke(width = 2.dp.toPx()))
                valores.forEachIndexed { i, v ->
                    drawCircle(ejes[i].color.asColor(), radius = 4.dp.toPx(), center = punto(i, v.coerceIn(0, 10) / 10f))
                }
            }
        }
        if (mostrarEtiquetas) {
            // Etiquetas alrededor del radar.
            ejes.forEachIndexed { i, eje ->
                val ang = -PI / 2 + i * 2 * PI / ejes.size
                val dx = (cos(ang) * 0.43f).toFloat()
                val dy = (sin(ang) * 0.43f).toFloat()
                Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = bias(dx * 2, dy * 2)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(eje.texto(), style = MaterialTheme.typography.labelMedium, color = eje.color.asColor(),
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        valores.getOrNull(i)?.let {
                            Text("$it/10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun bias(h: Float, v: Float): Alignment = androidx.compose.ui.BiasAlignment(h.coerceIn(-1f, 1f), v.coerceIn(-1f, 1f))

/** Pastilla de color para un eje. */
@Composable
fun EjeChip(eje: Eje, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(eje.color.asColor().copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(eje.color.asColor()))
        Spacer(Modifier.width(6.dp))
        Text(eje.texto(), style = MaterialTheme.typography.labelMedium, color = eje.color.asColor(), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(titulo: String, texto: String, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
