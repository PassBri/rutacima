package com.rutaalacima.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Rosa de la marca sobre fondos oscuros (el burdeos del modo oscuro). */
val RosaMarca = Color(0xFFF0B3A1)

private val corazon = PathParser().parsePathString(
    "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z",
).toPath()

/**
 * Impulsar: un corazón dentro del símbolo de SuRoute.
 * Sin impulso: aro y corazón de contorno blanco, con el punto en rosa.
 * Al impulsar: el aro se vuelve a trazar desde el punto, el punto cae y el corazón se llena de rosa.
 */
@Composable
fun ImpulsoMarca(activo: Boolean, modifier: Modifier = Modifier, tamano: Dp = 48.dp, base: Color = Color.White, acento: Color = RosaMarca) {
    val sinAnimar = android.provider.Settings.Global.getFloat(LocalContext.current.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    val trazo = remember { Animatable(1f) }
    val lleno = remember { Animatable(if (activo) 1f else 0f) }
    LaunchedEffect(activo) {
        if (activo && !sinAnimar && lleno.value < 1f) {
            trazo.snapTo(0f); lleno.snapTo(0f)
            trazo.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            lleno.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        } else { trazo.snapTo(1f); lleno.snapTo(if (activo) 1f else 0f) }
    }
    Canvas(modifier.size(tamano)) {
        val d = size.minDimension
        val grosor = d * 0.055f
        val radio = d / 2f - grosor / 2f - d * 0.04f
        val c = Offset(size.width / 2f, size.height / 2f)
        val abertura = 15f
        val colorAro = if (activo) acento else base
        // Aro: se traza a ambos lados desde el sur cuando llega el impulso
        val barrido = (180f - abertura) * trazo.value
        val caja = Size(radio * 2, radio * 2); val esquina = Offset(c.x - radio, c.y - radio)
        drawArc(colorAro, 90f + abertura, barrido, false, esquina, caja, style = Stroke(grosor, cap = StrokeCap.Round))
        drawArc(colorAro, 90f - abertura, -barrido, false, esquina, caja, style = Stroke(grosor, cap = StrokeCap.Round))
        // Punto: siempre en rosa; al impulsar cae desde arriba hasta el hueco
        val caida = if (activo) (1f - trazo.value) * radio * 2f else 0f
        drawCircle(acento, d * 0.06f, Offset(c.x, c.y + radio - caida))
        // Corazón: contorno o lleno, con un leve latido al llenarse
        val lado = d * 0.46f
        val latido = if (activo) 0.82f + 0.18f * lleno.value else 1f
        val k = lado / 24f * latido
        translate(c.x, c.y + d * 0.01f) {
            scale(k, k, Offset.Zero) {
                translate(-12f, -12.2f) {
                    if (activo) drawPath(corazon, acento.copy(alpha = 0.35f + 0.65f * lleno.value), style = Fill)
                    drawPath(corazon, if (activo) acento else base, style = Stroke(1.9f, join = StrokeJoin.Round))
                }
            }
        }
    }
}
