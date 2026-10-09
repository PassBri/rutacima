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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 1..meta) {
                val nivel by animateFloatAsState(
                    if (i <= vasos) 1f else 0f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "vaso$i",
                )
                val desc = stringResource(R.string.vaso_n, i)
                Vaso(
                    nivel, ola,
                    Modifier.weight(1f).aspectRatio(0.72f).clip(RoundedCornerShape(6.dp))
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

/** Un vaso: contorno de vaso (más ancho arriba) y el agua con su ola subiendo hasta [nivel]. */
@Composable
private fun Vaso(nivel: Float, ola: Float, modifier: Modifier) {
    val borde = MaterialTheme.colorScheme.outline
    val vacio = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val inset = w * 0.14f
        val vaso = Path().apply {
            moveTo(w * 0.04f, h * 0.06f); lineTo(w * 0.96f, h * 0.06f)
            lineTo(w - inset, h * 0.96f); lineTo(inset, h * 0.96f); close()
        }
        drawPath(vaso, vacio)
        clipPath(vaso) {
            val n = nivel.coerceIn(0f, 1.08f)
            if (n > 0.001f) {
                val sup = h * 0.96f - (h * 0.88f) * n.coerceAtMost(1f)
                val amp = h * 0.035f * (1f - (n - 1f).coerceAtLeast(0f) * 10f).coerceIn(0.3f, 1f)
                val agua = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w) {
                        lineTo(x, sup + sin((x / w + ola) * 2f * PI.toFloat()) * amp)
                        x += w / 16f
                    }
                    lineTo(w, h); close()
                }
                drawPath(agua, Agua.copy(alpha = 0.85f))
                // Brillo del vidrio
                drawLine(Color.White.copy(alpha = 0.35f), Offset(w * 0.24f, h * 0.18f), Offset(w * 0.3f, h * 0.8f), strokeWidth = w * 0.06f)
            }
        }
        drawPath(vaso, borde, style = Stroke(width = 1.5.dp.toPx()))
    }
}
