package com.rutaalacima.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Los climas de montaña (iguales a la web, web/js/32-climas.js): sol en la cumbre, tormenta, niebla,
 * valle en calma y cumbre (la bandera). Se usan en Bienestar (tu clima de hoy) y en la reacción
 * "¿Qué clima te deja?" de la comunidad, debajo del corazón.
 */
object ClimaIconos {
    val REACCIONES = listOf("sol", "tormenta", "niebla", "valle", "cumbre")
    val COLOR = mapOf(
        "sol" to Color(0xFFE3A93B), "tormenta" to Color(0xFF7E8FA3), "niebla" to Color(0xFFB9C1C9),
        "valle" to Color(0xFF6FA07A), "cumbre" to Color(0xFFC0473A),
    )

    /** Trazos de 48×48: (d, relleno, trazo, grosor). */
    private class T(val d: String, val relleno: Long? = null, val trazo: Long? = null, val grosor: Float = 0f)
    private val TRAZOS = mapOf(
        "sol" to listOf(
            T("M31 3v3M31 22v3M20 14h3M39 14h3M23.5 6.5l2 2M36.5 19.5l2 2M38.5 6.5l-2 2", trazo = 0xFFE3A93B, grosor = 2.2f),
            T("M25 14a6 6 0 1 0 12 0a6 6 0 1 0-12 0", 0xFFE3A93B), T("M3 43L18 21l8 11 5-6 14 17z", 0xFF8A6A48),
            T("M18 21l4.5 6.5-3-1-3 2-2.5-1.5z", 0xFFF4EFE6),
        ),
        "tormenta" to listOf(
            T("M12 22a8 8 0 0 1 15-4 6 6 0 0 1 9 5 5 5 0 0 1-1 10H13a6 6 0 0 1-1-11z", 0xFF6B7888),
            T("M25 26l-4 8h5l-3 8 9-11h-5l3-5z", 0xFFF0C043), T("M14 37l-2 4M36 37l-2 4", trazo = 0xFF8FB0D3, grosor = 2.2f),
        ),
        "niebla" to listOf(T("M6 36l13-18 8 10 5-5 10 13z", 0xFF6F7B88), T("M6 26h20M14 32h28M8 40h30", trazo = 0xFFBCC4CC, grosor = 3f)),
        "valle" to listOf(
            T("M18 26a6 6 0 1 0 12 0a6 6 0 1 0-12 0", 0xFFE8B26A), T("M2 30l12-12 10 12 10-12 12 12v14H2z", 0xFF4E7B57),
            T("M2 38c8-4 14 2 22-1s14-2 22 0v8H2z", 0xFF8DB894),
        ),
        "cumbre" to listOf(T("M6 44c4-5 16-5 20 0z", 0xFF8A6A48), T("M16 6v36", trazo = 0xFFE9E2D3, grosor = 3f), T("M17 7h20l-5 6 5 6H17z", 0xFFC0473A)),
    )
    private val cache = HashMap<String, ImageVector>()

    fun vector(k: String): ImageVector = cache.getOrPut(k) {
        val b = ImageVector.Builder("clima_$k", 48.dp, 48.dp, 48f, 48f)
        TRAZOS[k].orEmpty().forEach { t ->
            b.addPath(
                pathData = PathParser().parsePathString(t.d).toNodes(),
                fill = t.relleno?.let { SolidColor(Color(it)) },
                stroke = t.trazo?.let { SolidColor(Color(it)) }, strokeLineWidth = t.grosor,
                strokeLineCap = if (t.trazo != null) StrokeCap.Round else StrokeCap.Butt,
            )
        }
        b.build()
    }

    fun nombreReaccion(ctx: android.content.Context, k: String): String {
        val id = ctx.resources.getIdentifier("cl_r_$k", "string", ctx.packageName)
        return if (id != 0) ctx.getString(id) else k
    }
}

@Composable
fun ClimaIcono(clave: String, modifier: Modifier = Modifier, apagado: Boolean = false) {
    Image(
        rememberVectorPainter(ClimaIconos.vector(clave)), null, modifier.then(if (apagado) Modifier.alpha(0.7f) else Modifier),
        colorFilter = if (apagado) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
    )
}

/**
 * "¿Qué clima te deja?": el clima dominante (o el tuyo) dentro de un anillo de cielo, con un tramo por
 * cada clima según cuántas personas lo dejaron. Al tocarlo se abre el abanico; tocar el tuyo lo quita.
 */
@Composable
fun BotonClima(climas: Map<String, Int>, miClima: String?, onElegir: (String?) -> Unit, modifier: Modifier = Modifier, tamano: Dp = 50.dp) {
    val ctx = LocalContext.current
    val haptico = LocalHapticFeedback.current
    var abierto by remember { mutableStateOf(false) }
    val total = ClimaIconos.REACCIONES.sumOf { climas[it] ?: 0 }
    val dom = miClima ?: ClimaIconos.REACCIONES.maxByOrNull { climas[it] ?: 0 }?.takeIf { total > 0 }
    val giro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { giro.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(miClima) { if (miClima != null) { pop.snapTo(0.7f); pop.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) } }
    val etiqueta = miClima?.let { ClimaIconos.nombreReaccion(ctx, it) } ?: ctx.getString(com.rutaalacima.app.R.string.cl_titulo)

    Box(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                abierto = !abierto; haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }.padding(2.dp).semantics { contentDescription = "$etiqueta, $total" },
        ) {
            Box(Modifier.size(tamano).scale(pop.value), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(tamano).graphicsLayer { rotationZ = -120f * (1f - giro.value); alpha = giro.value }) {
                    drawCircle(Color(0x52060A12))
                    val g = 3.5.dp.toPx(); val r = Size(size.width - g, size.height - g); val o = Offset(g / 2, g / 2)
                    if (total == 0) drawArc(Color.White.copy(alpha = 0.35f), 0f, 360f, false, o, r, style = Stroke(g))
                    else {
                        var a = -90f
                        ClimaIconos.REACCIONES.forEach { k ->
                            val n = climas[k] ?: 0
                            if (n > 0) { val b = n * 360f / total; drawArc(ClimaIconos.COLOR.getValue(k), a, (b - 3f).coerceAtLeast(1f), false, o, r, style = Stroke(g)); a += b }
                        }
                    }
                }
                ClimaIcono(dom ?: "sol", Modifier.size(tamano * 0.6f), apagado = dom == null)
            }
            Text(if (total > 0) contarCorto(total) else "", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
        if (abierto) {
            val dens = androidx.compose.ui.platform.LocalDensity.current
            Popup(
                alignment = Alignment.CenterEnd,
                offset = with(dens) { IntOffset(-(tamano + 14.dp).roundToPx(), -10.dp.roundToPx()) },
                onDismissRequest = { abierto = false }, properties = PopupProperties(focusable = true),
            ) {
                AbanicoClimas(miClima) { k ->
                    abierto = false
                    haptico.performHapticFeedback(HapticFeedbackType.LongPress)
                    onElegir(if (k == miClima) null else k)
                }
            }
        }
    }
}

@Composable
private fun AbanicoClimas(miClima: String?, onElegir: (String) -> Unit) {
    val ctx = LocalContext.current
    Row(
        Modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xE60A0E18)).border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ClimaIconos.REACCIONES.forEachIndexed { i, k ->
            val entra = remember { Animatable(0f) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(i * 40L); entra.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
            val nombre = ClimaIconos.nombreReaccion(ctx, k)
            Column(
                Modifier.width(58.dp).graphicsLayer { alpha = entra.value; translationY = (1f - entra.value) * 24f; scaleX = 0.6f + 0.4f * entra.value; scaleY = scaleX }
                    .clip(RoundedCornerShape(20.dp)).background(if (k == miClima) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                    .clickable { onElegir(k) }.padding(vertical = 4.dp).semantics { contentDescription = nombre },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ClimaIcono(k, Modifier.size(34.dp))
                Text(nombre, color = Color.White.copy(alpha = 0.86f), fontSize = 10.5.sp, lineHeight = 12.sp, textAlign = TextAlign.Center, maxLines = 3)
            }
        }
    }
}

private fun contarCorto(n: Int) = when {
    n >= 10000 -> "${n / 1000} k"
    n >= 1000 -> String.format(java.util.Locale.getDefault(), "%.1f k", n / 1000f)
    else -> n.toString()
}
