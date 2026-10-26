package com.rutaalacima.app.ui.inicio

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rutaalacima.app.R
import kotlinx.coroutines.launch

private val Salida = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val Traza = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

/** Progreso de 0 a 1 entre [desde] y [desde]+[dura] (milisegundos), con su curva. */
private fun tramo(t: Float, desde: Float, dura: Float, curva: (Float) -> Float = Salida::transform): Float =
    curva(((t - desde) / dura).coerceIn(0f, 1f))

/**
 * Apertura de SuRoute (igual que la web): el símbolo se traza, el punto cae al sur
 * y en tres pasos cuenta qué es SuRoute mientras un punto sube por la cordillera hasta la cumbre.
 * La primera vez dura unos 4,8 s; después, solo el símbolo (1,2 s). Se salta con un toque.
 */
@Composable
fun IntroSuRoute(onFin: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("apertura", Context.MODE_PRIVATE) }
    val corta = remember { prefs.getBoolean("vista", false).also { prefs.edit().putBoolean("vista", true).apply() } }
    val sinAnimar = remember { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val total = when { sinAnimar -> 900f; corta -> 1250f; else -> 4800f }
    val salidaDesde = total - 450f
    val t = remember { Animatable(0f) }
    val velo = remember { Animatable(1f) }
    var saliendo by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current
    var vibro by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        t.animateTo(total, tween(total.toInt(), easing = LinearEasing))
        onFin()
    }
    fun saltar() {
        if (saliendo) return
        saliendo = true
        scope.launch { velo.animateTo(0f, tween(240, easing = Salida)); onFin() }
    }
    val v = t.value
    // Tiempos (ms) de cada momento
    val aro = if (sinAnimar) 1f else if (corta) tramo(v, 0f, 550f, Traza::transform) else tramo(v, 150f, 1000f, Traza::transform)
    val caida = if (sinAnimar) 1f else if (corta) tramo(v, 450f, 300f) else tramo(v, 1000f, 450f)
    val aterrizo = caida >= 1f
    LaunchedEffect(aterrizo) { if (aterrizo && !vibro && !sinAnimar) { vibro = true; haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove) } }
    val fuera = tramo(v, salidaDesde, 450f)
    val alfaTodo = (1f - fuera) * velo.value

    val primario = MaterialTheme.colorScheme.primary
    val oro = MaterialTheme.colorScheme.secondary
    val linea = MaterialTheme.colorScheme.outlineVariant
    val suave = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = alfaTodo }
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::saltar)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val crece = 1f + 0.08f * fuera
            Canvas(Modifier.size(112.dp).graphicsLayer { scaleX = crece; scaleY = crece }) {
                val d = size.minDimension
                val grosor = d * 0.05f
                val radio = d * 0.38f
                val c = Offset(size.width / 2f, size.height / 2f)
                val abertura = 13f
                // El aro se traza desde el lado izquierdo del hueco, en sentido horario
                drawArc(primario, 90f + abertura, (360f - 2 * abertura) * aro, false,
                    Offset(c.x - radio, c.y - radio), Size(radio * 2, radio * 2), style = Stroke(grosor, cap = StrokeCap.Butt))
                if (caida > 0f) drawCircle(primario.copy(alpha = (caida * 3f).coerceAtMost(1f)), d * 0.065f,
                    Offset(c.x, c.y + radio * 1.0f - (1f - caida) * d * 0.8f))
            }
            if (!corta && !sinAnimar) {
                val marca = tramo(v, 1350f, 500f); val lema = tramo(v, 1550f, 500f)
                Text("SuRoute", Modifier.padding(top = 18.dp).graphicsLayer { alpha = marca; translationY = (1f - marca) * 24f },
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold))
                Text("SUMMIT ROUTE", Modifier.padding(top = 6.dp).graphicsLayer { alpha = lema; translationY = (1f - lema) * 24f },
                    color = oro, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.5.sp))
                // Tres pasos, uno tras otro
                Box(Modifier.padding(top = 30.dp).height(26.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    listOf(R.string.intro_1 to 2050f, R.string.intro_2 to 2800f, R.string.intro_3 to 3550f).forEachIndexed { i, (txt, desde) ->
                        val entra = tramo(v, desde, 190f)
                        val sale = if (i < 2) tramo(v, desde + 560f, 190f) else 0f
                        Text(stringResource(txt), Modifier.graphicsLayer { alpha = entra * (1f - sale); translationY = (1f - entra) * 20f - sale * 14f },
                            color = suave, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    }
                }
                // La cordillera se dibuja y un punto sube hasta la cumbre
                val cresta = tramo(v, 1900f, 1200f)
                val camino = tramo(v, 2050f, 2200f, CubicBezierEasing(0.45f, 0f, 0.25f, 1f)::transform)
                Canvas(Modifier.padding(top = 14.dp).size(260.dp, 60.dp)) {
                    val k = size.width / 260f
                    val puntos = listOf(0f to 56f, 40f to 40f, 62f to 48f, 104f to 22f, 128f to 34f, 170f to 8f, 196f to 28f, 230f to 18f, 260f to 56f)
                    fun ruta(hasta: Int) = Path().apply { puntos.take(hasta).forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * k, y * k) else lineTo(x * k, y * k) } }
                    val todo = PathMeasure().apply { setPath(ruta(puntos.size), false) }
                    val parcial = Path(); todo.getSegment(0f, todo.length * cresta, parcial, true)
                    drawPath(parcial, linea, style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
                    if (camino > 0f) {
                        val subida = PathMeasure().apply { setPath(ruta(6), false) }
                        drawCircle(oro.copy(alpha = (camino * 10f).coerceAtMost(1f)), 4.dp.toPx(), subida.getPosition(subida.length * camino))
                    }
                }
            }
        }
        if (!corta && !sinAnimar) {
            val saltarAlfa = tramo(v, 1800f, 400f)
            Text(stringResource(R.string.intro_saltar), Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp).graphicsLayer { alpha = saltarAlfa },
                color = suave, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier)
    }
}
