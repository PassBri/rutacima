package com.rutaalacima.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * El símbolo de SuRoute: un círculo abierto al sur con un punto en el hueco.
 * [grosor] y [punto] son fracciones del diámetro; el centro y el radio salen del tamaño del lienzo.
 */
fun DrawScope.simboloSuRoute(color: Color, grosor: Float = 0.071f, punto: Float = 0.097f, abertura: Float = 13f) {
    val trazo = size.minDimension * grosor
    val radio = size.minDimension / 2f - trazo / 2f - size.minDimension * 0.02f
    val centro = Offset(size.width / 2f, size.height / 2f)
    drawArc(
        color, startAngle = 90f + abertura, sweepAngle = 360f - 2 * abertura, useCenter = false,
        topLeft = Offset(centro.x - radio, centro.y - radio), size = Size(radio * 2, radio * 2),
        style = Stroke(trazo, cap = StrokeCap.Butt),
    )
    drawCircle(color, radius = size.minDimension * punto / 2f, center = Offset(centro.x, centro.y + radio))
}

/** Anillo del símbolo alrededor de una foto de perfil: toque sutil de marca en los encabezados de perfil y en Cimas. */
@Composable
fun AroMarca(tamano: Dp, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val margen = tamano * 0.09f
    Box(
        modifier
            .size(tamano + margen * 2)
            .drawBehind { simboloSuRoute(color, grosor = 0.024f, punto = 0.07f) },
        contentAlignment = Alignment.Center,
    ) { contenido() }
}

/** Cargando: el símbolo gira despacio, como una brújula que busca el norte. Con animaciones apagadas, solo late. */
@Composable
fun CargandoMarca(modifier: Modifier = Modifier, tamano: Dp = 40.dp, color: Color = MaterialTheme.colorScheme.secondary, descripcion: String? = null) {
    val sinAnimar = android.provider.Settings.Global.getFloat(LocalContext.current.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    val t = rememberInfiniteTransition(label = "cargando")
    val giro by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing)), label = "giro")
    val latido by t.animateFloat(1f, 0.4f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), label = "latido")
    Canvas(modifier.size(tamano).semantics { if (descripcion != null) contentDescription = descripcion }) {
        if (sinAnimar) simboloSuRoute(color.copy(alpha = color.alpha * latido))
        else rotate(giro) { simboloSuRoute(color) }
    }
}
