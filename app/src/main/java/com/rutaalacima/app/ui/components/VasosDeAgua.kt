package com.rutaalacima.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/** Azul del agua (el mismo del eje Voz). */
private val Agua = Color(0xFF2E7D9A)

/**
 * Vasos de agua del día: ocho vasos que se llenan con una ola al tocarlos (resorte de Material 3),
 * con el total en litros (250 ml por vaso) y botones grandes de más y menos para no apuntar al vaso.
 */
@Composable
fun VasosDeAgua(vasos: Int, onCambio: (Int) -> Unit, modifier: Modifier = Modifier, meta: Int = 8) {
    val haptico = LocalHapticFeedback.current
    val cambiar = { n: Int ->
        val v = n.coerceIn(0, meta)
        if (v != vasos) { haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove); onCambio(v) }
    }
    // La ola se mueve sola; con "Quitar animaciones" del sistema el reloj de Compose se detiene
    val ola by rememberInfiniteTransition(label = "ola").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "fase",
    )
    Column(modifier) {
        Row(Modifier.fillMaxWidth().widthIn(max = 280.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 1..meta) {
                val nivel by animateFloatAsState(
                    if (i <= vasos) 1f else 0f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "vaso$i",
                )
                val desc = stringResource(R.string.vaso_n, i)
                Vaso(
                    nivel, ola,
                    Modifier.weight(1f).aspectRatio(0.78f).clip(RoundedCornerShape(6.dp))
                        // Tocar el último vaso lleno lo vacía; tocar otro llena hasta ahí
                        .clickable(role = Role.Button) { cambiar(if (vasos == i) i - 1 else i) }
                        .semantics { contentDescription = desc; stateDescription = if (i <= vasos) "✓" else "" },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val litros = String.format(Locale.getDefault(), "%.2f", vasos * 0.25).trimEnd('0').trimEnd(',', '.')
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.agua_resumen, vasos, litros), style = MaterialTheme.typography.labelLarge, color = Agua)
                if (vasos >= meta) Text(stringResource(R.string.agua_cumplida), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary)
            }
            FilledTonalIconButton(onClick = { cambiar(vasos - 1) }, enabled = vasos > 0) {
                Icon(Icons.Filled.Remove, stringResource(R.string.agua_quitar))
            }
            Spacer(Modifier.width(4.dp))
            FilledTonalIconButton(onClick = { cambiar(vasos + 1) }, enabled = vasos < meta) {
                Icon(Icons.Filled.Add, stringResource(R.string.agua_sumar))
            }
        }
    }
}

/** Una cantimplora de senderista: tapa, cuello y cuerpo redondo con su funda; el agua sube con una ola hasta [nivel]. */
@Composable
private fun Vaso(nivel: Float, ola: Float, modifier: Modifier) {
    val borde = MaterialTheme.colorScheme.outline
    val vacio = MaterialTheme.colorScheme.surfaceContainerHighest
    val tapa = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val r = w * 0.46f
        val c = Offset(w / 2f, h - r - h * 0.02f)
        val cuerpo = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
        // Tapa y cuello
        drawRoundRect(tapa, Offset(w * 0.34f, h * 0.01f), androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.11f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
        drawRect(borde, Offset(w * 0.4f, h * 0.11f), androidx.compose.ui.geometry.Size(w * 0.2f, c.y - r - h * 0.11f + 2f))
        drawPath(cuerpo, vacio)
        clipPath(cuerpo) {
            val n = nivel.coerceIn(0f, 1.08f)
            if (n > 0.001f) {
                val alto = r * 2f; val fondo = c.y + r
                val sup = fondo - alto * n.coerceAtMost(1f)
                val amp = h * 0.035f * (1f - (n - 1f).coerceAtLeast(0f) * 10f).coerceIn(0.3f, 1f)
                val agua = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w) { lineTo(x, sup + sin((x / w + ola) * 2f * PI.toFloat()) * amp); x += w / 16f }
                    lineTo(w, h); close()
                }
                drawPath(agua, Agua.copy(alpha = 0.85f))
            }
        }
        // Funda: costura interior y la correa que cruza
        drawCircle(borde.copy(alpha = 0.5f), r * 0.78f, c, style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f, 3f))))
        drawPath(cuerpo, borde, style = Stroke(width = 1.5.dp.toPx()))
    }
}

/** Nivel de energía como altitud: cinco cumbres que crecen; se encienden hasta la elegida. */
@Composable
fun NivelEnergia(valor: Int, onChange: (Int) -> Unit, color: Color = MaterialTheme.colorScheme.secondary, total: Int = 5) {
    // Cinco cordilleras que crecen, de la niebla (agotado) al sol en la cumbre (a tope). Igual que la web (energiaMontes).
    val colores = listOf(Color(0xFF7A8896), Color(0xFF6F8FA0), Color(0xFF3E8A5A), Color(0xFFC98A12), Color(0xFFE8A317))
    val nombres = listOf(R.string.energia_n1, R.string.energia_n2, R.string.energia_n3, R.string.energia_n4, R.string.energia_n5).map { stringResource(it) }
    val vacio = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)
    val borde = MaterialTheme.colorScheme.outlineVariant
    val fondo = MaterialTheme.colorScheme.surface
    val nombre = stringResource(R.string.energia)
    androidx.compose.foundation.layout.Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            for (i in 1..total.coerceAtMost(5)) {
                val lleno = i <= valor; val c = colores[i - 1]; val actual = i == valor
                androidx.compose.foundation.layout.Box(
                    Modifier.size(50.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (lleno) c.copy(alpha = 0.12f) else fondo)
                        .border(if (actual) 2.dp else 1.dp, if (lleno) c.copy(alpha = if (actual) 1f else 0.45f) else borde, RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button) { onChange(if (valor == i) i - 1 else i) }
                        .semantics { contentDescription = "$nombre $i/$total: ${nombres[i - 1]}"; stateDescription = if (lleno) "✓" else "" }
                        .padding(5.dp),
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        val k = size.width / 40f
                        val ph = 8f + 5f * i; val b = 34f; val y = b - ph
                        fun o(x: Float, yy: Float) = Offset(x * k, yy * k)
                        val monte = Path().apply {
                            moveTo(o(3f, b).x, o(3f, b).y); lineTo(o(12f, b - .6f * ph).x, o(12f, b - .6f * ph).y)
                            lineTo(o(16.5f, b - .45f * ph).x, o(16.5f, b - .45f * ph).y); lineTo(o(24f, y).x, o(24f, y).y)
                            lineTo(o(37f, b).x, o(37f, b).y); close()
                        }
                        drawPath(monte, if (lleno) c else vacio)
                        if (lleno) {
                            val nieve = Path().apply {
                                moveTo(o(24f, y).x, o(24f, y).y); lineTo(o(24f + 13f * .3f, y + ph * .3f).x, o(24f + 13f * .3f, y + ph * .3f).y)
                                lineTo(o(25.4f, y + ph * .22f).x, o(25.4f, y + ph * .22f).y); lineTo(o(23.4f, y + ph * .3f).x, o(23.4f, y + ph * .3f).y)
                                lineTo(o(24f - 7.5f * .3f, y + .55f * ph * .3f).x, o(24f - 7.5f * .3f, y + .55f * ph * .3f).y); close()
                            }
                            drawPath(nieve, Color.White)
                        }
                    }
                }
            }
        }
        if (valor in 1..5) Text(
            "${nombres[valor - 1]} · $valor / 5", color = colores[valor - 1], style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
