package com.rutaalacima.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * Formas de Material 3 Expressive hechas con androidx.graphics.shapes, el mismo motor de
 * MaterialShapes: polígonos redondeados que se pueden transformar unos en otros (morph).
 */
object FormasRuta {
    /** "Galleta" de 9 lóbulos, para enmarcar avatares. */
    val galleta: RoundedPolygon = RoundedPolygon.star(9, innerRadius = 0.84f, rounding = CornerRounding(0.42f)).normalized()
    /** Sol de 8 puntas suaves: el impulso encendido. */
    val sol: RoundedPolygon = RoundedPolygon.star(8, innerRadius = 0.72f, rounding = CornerRounding(0.16f)).normalized()
    /** Círculo hecho de 8 vértices (para que el morph con el sol sea suave). */
    val circulo: RoundedPolygon = RoundedPolygon.circle(8).normalized()
    /** Cumbre: triángulo redondeado. */
    val cumbre: RoundedPolygon = RoundedPolygon(3, rounding = CornerRounding(0.28f)).normalized()
}

/** Un [RoundedPolygon] normalizado (0..1) como Shape de Compose, escalado al tamaño del componente. */
class FormaPoligono(private val poligono: RoundedPolygon) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val camino = poligono.toPath().asComposePath()
        camino.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(camino)
    }
}

/**
 * Botón de impulso expresivo: al encenderse, el círculo se transforma en un sol con un resorte
 * que rebota (movimiento "expressive" de Material 3) y sube un "+10 m" hacia la cumbre.
 */
@Composable
fun BotonImpulso(activo: Boolean, cantidad: Int, icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    val haptico = LocalHapticFeedback.current
    val morph = remember { Morph(FormasRuta.circulo, FormasRuta.sol) }
    val p by animateFloatAsState(if (activo) 1f else 0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "morph")
    val giro by animateFloatAsState(if (activo) 22.5f else 0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow), label = "giro")
    val sube = remember { Animatable(1f) }
    val primera = remember { booleanArrayOf(true) }
    LaunchedEffect(activo) {
        if (primera[0]) { primera[0] = false; return@LaunchedEffect }
        if (activo) { sube.snapTo(0f); sube.animateTo(1f, tween(900)) }
    }
    val fondoOn = MaterialTheme.colorScheme.secondaryContainer
    val fondoOff = MaterialTheme.colorScheme.surfaceContainerHigh
    val tinta = if (activo) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = { haptico.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        shape = RoundedCornerShape(50), color = fondoOff,
    ) {
        Row(Modifier.padding(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = giro; scaleX = 0.9f + 0.1f * p; scaleY = scaleX }) {
                    val camino = morph.toPath(p.coerceIn(0f, 1f)).asComposePath()
                    camino.transform(Matrix().apply { scale(size.width, size.height) })
                    drawPath(camino, if (p > 0.02f) fondoOn else fondoOff)
                }
                Icon(icono, descripcion, Modifier.size(18.dp), tint = tinta)
                // "+10 m" que sube y se desvanece
                if (sube.value < 1f) Text(
                    "+10 m", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.offset(y = (-18 - 22 * sube.value).dp).graphicsLayer { alpha = 1f - sube.value },
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("$cantidad", style = MaterialTheme.typography.labelLarge)
        }
    }
}
