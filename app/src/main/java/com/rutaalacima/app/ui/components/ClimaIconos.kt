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
        "sol" to Color(0xFFE8A317), "tormenta" to Color(0xFFE0662A), "niebla" to Color(0xFF7A8896),
        "valle" to Color(0xFF3E8A5A), "cumbre" to Color(0xFFC0473A),
    )

    /** Material 3 Expressive: forma de color (48×48) + símbolo de Material Rounded relleno (960×960) en blanco. */
    private class F(val forma: String, val color: Long, val simbolo: String)
    private val FORMAS = mapOf(
        "sol" to F("M47.2 24.0 L47.0 25.2 46.3 26.3 45.4 27.4 44.3 28.3 43.2 29.2 42.3 29.9 41.6 30.8 41.2 31.7 41.1 32.7 41.2 33.9 41.4 35.3 41.5 36.7 41.5 38.1 41.1 39.4 40.4 40.4 39.4 41.1 38.1 41.5 36.7 41.5 35.3 41.4 34.0 41.2 32.7 41.1 31.7 41.2 30.8 41.6 29.9 42.3 29.2 43.2 28.3 44.3 27.4 45.4 26.3 46.3 25.2 47.0 24.0 47.2 22.8 47.0 21.7 46.3 20.6 45.4 19.7 44.3 18.8 43.2 18.1 42.3 17.2 41.6 16.3 41.2 15.3 41.1 14.1 41.2 12.7 41.4 11.3 41.5 9.9 41.5 8.6 41.1 7.6 40.4 6.9 39.4 6.5 38.1 6.5 36.7 6.6 35.3 6.8 33.9 6.9 32.7 6.8 31.7 6.4 30.8 5.7 29.9 4.8 29.2 3.7 28.3 2.6 27.4 1.7 26.3 1.0 25.2 0.8 24.0 1.0 22.8 1.7 21.7 2.6 20.6 3.7 19.7 4.8 18.8 5.7 18.1 6.4 17.2 6.8 16.3 6.9 15.3 6.8 14.1 6.6 12.7 6.5 11.3 6.5 9.9 6.9 8.6 7.6 7.6 8.6 6.9 9.9 6.5 11.3 6.5 12.7 6.6 14.0 6.8 15.3 6.9 16.3 6.8 17.2 6.4 18.1 5.7 18.8 4.8 19.7 3.7 20.6 2.6 21.7 1.7 22.8 1.0 24.0 0.8 25.2 1.0 26.3 1.7 27.4 2.6 28.3 3.7 29.2 4.8 29.9 5.7 30.8 6.4 31.7 6.8 32.7 6.9 34.0 6.8 35.3 6.6 36.7 6.5 38.1 6.5 39.4 6.9 40.4 7.6 41.1 8.6 41.5 9.9 41.5 11.3 41.4 12.7 41.2 14.1 41.1 15.3 41.2 16.3 41.6 17.2 42.3 18.1 43.2 18.8 44.3 19.7 45.4 20.6 46.3 21.7 47.0 22.8z", 0xFFE8A317, "M458.5-778.63Q450-787.25 450-800v-90q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v90q0 12.75-8.68 21.37-8.67 8.63-21.5 8.63-12.82 0-21.32-8.63ZM685-685q-9-9-9-21.1 0-12.1 9-20.9l63-64q9.07-9 21.53-9 12.47 0 21.47 9 9 9 9 21t-9 21l-64 64q-9 9-21 9t-21-9Zm115 235q-12.75 0-21.37-8.68-8.63-8.67-8.63-21.5 0-12.82 8.63-21.32 8.62-8.5 21.37-8.5h90q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5h-90ZM458.5-48.63Q450-57.25 450-70v-90q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v90q0 12.75-8.68 21.37-8.67 8.63-21.5 8.63-12.82 0-21.32-8.63ZM233-685l-64-63q-9-8.87-9-21.43 0-12.57 9.39-21.57 8.61-9 20.61-9t21 9l64 64q9 9 9 21t-9 21q-9.27 8-21.64 8-12.36 0-20.36-8Zm516 516-64-64q-9-8.8-9-20.9 0-12.1 9-21.1 8.25-8 20.63-8 12.37 0 21.37 8l65 63q9 9 8.63 21.6-.37 12.61-8.53 21.5-8.89 8.9-21.5 8.9-12.6 0-21.6-9ZM70-450q-12.75 0-21.37-8.68-8.63-8.67-8.63-21.5 0-12.82 8.63-21.32Q57.25-510 70-510h90q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5H70Zm99 280.61q-9-8.61-9-20.61t9-21l64-64q8.25-8 20.63-8 12.37 0 21.37 8.25 9 9 9 21.37 0 12.38-9 21.38l-63 63q-8.87 9-21.43 9-12.57 0-21.57-9.39ZM310-310q-70-70-70-170t70-170q70-70 170-70t170 70q70 70 70 170t-70 170q-70 70-170 70t-170-70Z"),
        "tormenta" to F("M14 2h20a12 12 0 0 1 12 12v20a12 12 0 0 1-12 12H14A12 12 0 0 1 2 34V14A12 12 0 0 1 14 2z", 0xFFE0662A, "M360-360H217q-18 0-26.5-16t2.5-31l338-488q8-11 20-15t24 1q12 5 19 16t5 24l-39 309h176q19 0 27 17t-4 32L388-66q-8 10-20.5 13T344-55q-11-5-17.5-16T322-95l38-265Z"),
        "niebla" to F("M2 24A22 22 0 0 1 46 24v12a10 10 0 0 1-10 10H12A10 10 0 0 1 2 36z", 0xFF7A8896, "M720-205q-14 0-24.5-10.5T685-240q0-14 10.5-24.5T720-275q14 0 24.5 10.5T755-240q0 14-10.5 24.5T720-205ZM280-80q-14 0-24.5-10.5T245-115q0-14 10.5-24.5T280-150q14 0 24.5 10.5T315-115q0 14-10.5 24.5T280-80Zm-40-125q-14 0-24.5-10.5T205-240q0-14 10.5-24.5T240-275h360q14 0 24.5 10.5T635-240q0 14-10.5 24.5T600-205H240ZM400-80q-14 0-24.5-10.5T365-115q0-14 10.5-24.5T400-150h280q14 0 24.5 10.5T715-115q0 14-10.5 24.5T680-80H400ZM290-340q-87 0-148.5-61.5T80-550q0-79 56.5-141T277-759q32-56 84.5-88.5T480-880q91 0 152.5 57.5T708-680q79 4 125.5 53.5T880-510q0 70-49.5 120T710-340H290Z"),
        "valle" to F("M2 24A22 22 0 0 1 24 2A22 22 0 0 1 46 24A22 22 0 0 1 24 46H8a6 6 0 0 1-6-6z", 0xFF3E8A5A, "m76-288 180-240q5-6 11-9t13-3q7 0 13 3t11 9l162 216q5 6 11 9t13 3q19 0 27-16.5t-3-31.5L411-485q-6-8-6-18t6-18l125-167q5-6 11-9t13-3q7 0 13 3t11 9l300 400q11 15 3 31.5T860-240H100q-19 0-27-16.5t3-31.5Z"),
        "cumbre" to F("M8 2h16A22 22 0 0 1 46 24A22 22 0 0 1 24 46A22 22 0 0 1 2 24V8a6 6 0 0 1 6-6z", 0xFFC0473A, "M260-429v279q0 12.75-8.68 21.37-8.67 8.63-21.5 8.63-12.82 0-21.32-8.63-8.5-8.62-8.5-21.37v-620q0-12.75 8.63-21.38Q217.25-800 230-800h289q10.5 0 18.75 6.5T548-776l14 62h208q12.75 0 21.38 8.62Q800-696.75 800-684v310q0 12.75-8.62 21.37Q782.75-344 770-344H568q-10.5 0-18.75-6T539-367l-14-62H260Z"),
    )
    private val cache = HashMap<String, ImageVector>()

    fun vector(k: String): ImageVector = cache.getOrPut(k) {
        val f = FORMAS[k] ?: FORMAS.getValue("sol")
        ImageVector.Builder("clima_$k", 48.dp, 48.dp, 48f, 48f)
            .addPath(PathParser().parsePathString(f.forma).toNodes(), fill = SolidColor(Color(f.color)))
            .addGroup(scaleX = 0.02708f, scaleY = 0.02708f, translationX = 11f, translationY = 37f)
            .addPath(PathParser().parsePathString(f.simbolo).toNodes(), fill = SolidColor(Color.White))
            .clearGroup()
            .build()
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
                ClimaIcono(dom ?: "sol", Modifier.size(tamano * 0.68f), apagado = dom == null)
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
