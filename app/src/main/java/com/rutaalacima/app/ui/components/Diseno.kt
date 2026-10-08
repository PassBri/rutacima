package com.rutaalacima.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pantalla vacía con una pequeña montaña ilustrada: dice qué falta y cuál es el primer paso,
 * en vez de una lista en blanco.
 */
@Composable
fun EstadoVacio(semilla: String, titulo: String, texto: String, accion: String? = null, onAccion: () -> Unit = {}, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        MontanaArte(semilla, Modifier.fillMaxWidth().widthIn(max = 420.dp).height(120.dp).clip(RoundedCornerShape(20.dp)), bandera = true) {}
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp))
        if (accion != null) {
            Spacer(Modifier.height(10.dp))
            Button(onClick = onAccion) { Text(accion) }
        }
    }
}

/** Un hito alcanzado (meta del mes o del año cumplida). */
data class Celebracion(val titulo: String, val anual: Boolean)

/**
 * Celebración al cumplir una meta: sobre la cumbre se clava la bandera, ondea y saltan chispas
 * doradas. Dura unos segundos; tocar la cierra antes.
 */
@Composable
fun CelebracionCumbre(c: Celebracion, onFin: () -> Unit, onCompartir: (() -> Unit)? = null) {
    val haptico = LocalHapticFeedback.current
    val entrada = remember { Animatable(0f) }
    val asta = remember { Animatable(0f) }
    val ondear = remember { Animatable(0f) }
    LaunchedEffect(c) {
        haptico.performHapticFeedback(HapticFeedbackType.LongPress)
        coroutineScope {
            listOf(
                async { entrada.animateTo(1f, tween(350, easing = FastOutSlowInEasing)) },
                async { delay(250); asta.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) },
                async { delay(700); ondear.animateTo(1f, tween(2200, easing = LinearEasing)) },
            ).awaitAll()
        }
        delay(if (onCompartir != null) 4000 else 600)
        onFin()
    }
    val oro = Color(0xFFE0B566)
    val burdeos = MaterialTheme.colorScheme.primary
    val montana = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f * entrada.value))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onFin),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(32.dp).widthIn(max = 360.dp).graphicsLayer { alpha = entrada.value; scaleX = 0.9f + 0.1f * entrada.value; scaleY = scaleX }
                .clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface).padding(24.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Canvas(Modifier.size(180.dp)) {
                val w = size.width; val h = size.height
                // Montaña con su cumbre
                val cima = Offset(w * 0.5f, h * 0.42f)
                drawPath(Path().apply { moveTo(0f, h); lineTo(w * 0.22f, h * 0.62f); lineTo(w * 0.32f, h * 0.7f); lineTo(cima.x, cima.y)
                    lineTo(w * 0.7f, h * 0.66f); lineTo(w * 0.8f, h * 0.58f); lineTo(w, h); close() }, montana)
                drawPath(Path().apply { moveTo(cima.x, cima.y); lineTo(w * 0.43f, h * 0.53f); lineTo(w * 0.5f, h * 0.5f); lineTo(w * 0.57f, h * 0.54f); close() }, Color.White.copy(alpha = 0.85f))
                // Asta que se clava (crece hacia arriba desde la cumbre)
                val alto = h * 0.36f * asta.value
                val tope = Offset(cima.x, cima.y - alto)
                drawLine(oro, cima, tope, strokeWidth = w * 0.018f, cap = StrokeCap.Round)
                // Bandera ondeando
                if (asta.value > 0.6f) {
                    val t = ondear.value * 2 * PI.toFloat() * 2
                    val largo = w * 0.22f * ((asta.value - 0.6f) / 0.4f)
                    val bandera = Path().apply {
                        moveTo(tope.x, tope.y)
                        for (k in 0..10) { val x = k / 10f; lineTo(tope.x + largo * x, tope.y + sin(t + x * 5f) * w * 0.012f * x) }
                        for (k in 10 downTo 0) { val x = k / 10f; lineTo(tope.x + largo * x, tope.y + w * 0.12f + sin(t + x * 5f) * w * 0.012f * x) }
                        close()
                    }
                    drawPath(bandera, burdeos)
                }
                // Chispas doradas que salen de la cumbre
                if (asta.value >= 1f) {
                    val u = ondear.value
                    repeat(14) { i ->
                        val a = -PI.toFloat() / 2 + (i - 7) * 0.22f
                        val d = w * (0.12f + 0.4f * u) * (0.7f + (i % 3) * 0.15f)
                        drawCircle(oro.copy(alpha = (1f - u).coerceIn(0f, 1f)), radius = w * 0.012f * (1 + i % 2),
                            center = Offset(tope.x + cos(a) * d, tope.y + sin(a) * d + w * 0.3f * u * u))
                    }
                }
            }
            Text(stringResource(if (c.anual) R.string.celebra_anio else R.string.celebra_mes), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Text(c.titulo, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            if (onCompartir != null) {
                Spacer(Modifier.height(6.dp))
                Button(onClick = onCompartir) { Text(stringResource(R.string.celebra_compartir)) }
            }
        }
    }
}
