package com.rutaalacima.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Arte de Ruta a la Cima: la cordillera andina dibujada en código, idéntica a la de la web
 * (web/js/16-arte.js). La misma semilla da el mismo paisaje: crestas fractales (desplazamiento del
 * punto medio), 5 capas con perspectiva atmosférica, nieve en las cumbres lejanas, bruma entre capas,
 * el sol o la luna del momento y la bandera en la cima. Cuatro momentos del día, sin colores sueltos.
 */
object Cordillera {
    const val N = 64
    val BASE = floatArrayOf(0.42f, 0.52f, 0.62f, 0.72f, 0.84f)
    val AMP = floatArrayOf(0.20f, 0.22f, 0.24f, 0.22f, 0.16f)

    class Momento(val cieloArriba: Color, val cieloHorizonte: Color, val lejos: Color, val cerca: Color, val astro: Color, val noche: Boolean = false)

    /** 0 amanecer, 1 día, 2 atardecer, 3 noche (los colores del cielo real). */
    val MOMENTOS = listOf(
        Momento(Color(0xFF2C3A62), Color(0xFFEEAC7A), Color(0xFF9A8FA6), Color(0xFF2A2F45), Color(0xFFFFE2A6)),
        Momento(Color(0xFF4E84BA), Color(0xFFDEE9EE), Color(0xFFA9BFCF), Color(0xFF33485A), Color(0xFFFFF4D8)),
        Momento(Color(0xFF3B2A4A), Color(0xFFE8946A), Color(0xFFB07A78), Color(0xFF2B1E2A), Color(0xFFFFC27A)),
        Momento(Color(0xFF060A14), Color(0xFF1E283C), Color(0xFF2A3550), Color(0xFF0A0F1A), Color(0xFFEEF1F4), noche = true),
    )

    /** FNV-1a de 32 bits sobre las unidades UTF-16 del texto (igual que la web). */
    fun semillaDe(texto: String): Int {
        var h = 0x811c9dc5L.toInt()
        for (c in texto) { h = h xor c.code; h *= 0x01000193 }
        return h
    }

    /** mulberry32: números entre 0 y 1, reproducibles (igual que la web). */
    fun azar(semilla: Int): () -> Double {
        var a = semilla
        return {
            a += 0x6D2B79F5
            var t = (a xor (a ushr 15)) * (a or 1)
            t = t xor (t + (t xor (t ushr 7)) * (t or 61))
            (t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0
        }
    }

    /** Las 5 crestas (de lejos a cerca) con 65 alturas (fracción del alto) y la posición del astro. */
    fun crestas(texto: String): Pair<List<DoubleArray>, DoubleArray> {
        val r = azar(semillaDe(texto))
        val capas = List(5) { k ->
            val y = DoubleArray(N + 1)
            var amp = AMP[k].toDouble()
            y[0] = BASE[k] + (r() - 0.5) * amp; y[N] = BASE[k] + (r() - 0.5) * amp
            var paso = N
            while (paso > 1) {
                var i = 0
                while (i < N) { y[i + paso / 2] = (y[i] + y[i + paso]) / 2 + (r() - 0.5) * amp; i += paso }
                amp *= 0.55
                paso /= 2
            }
            y
        }
        return capas to doubleArrayOf(r(), r())
    }

    fun momentoDe(texto: String): Int = (semillaDe(texto).toUInt() % 4u).toInt()

    private fun mix(a: IntArray, b: IntArray, t: Double) = IntArray(3) { i -> Math.round(a[i] + (b[i] - a[i]) * t).toInt() }
    private fun Color.rgb() = intArrayOf((red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())

    /** Color de la capa k (0 lejos … 4 cerca), con la bruma del horizonte sobre lo lejano. */
    fun colorCapa(m: Momento, k: Int): IntArray {
        val t = k / 4.0
        return mix(m.cieloHorizonte.rgb(), mix(m.lejos.rgb(), m.cerca.rgb(), Math.pow(t, 1.2)), 0.45 + 0.55 * t)
    }
}

/** Compatibilidad: el "paleta" de antes ahora es el momento del día (0-3). */
fun paletaDe(semilla: String): Int = Cordillera.momentoDe(semilla)

/**
 * Paisaje de cordillera único para cada semilla (guía, mes, publicación, perfil).
 * [paleta] es el momento del día (0 amanecer, 1 día, 2 atardecer, 3 noche).
 */
@Composable
fun MontanaArte(
    semilla: String,
    modifier: Modifier = Modifier,
    paleta: Int = paletaDe(semilla),
    bandera: Boolean = true,
    contenido: (@Composable BoxScope.() -> Unit)? = null,
) {
    val m = Cordillera.MOMENTOS[paleta.mod(4)]
    val geo = remember(semilla) { Cordillera.crestas(semilla) }
    val estrellas = remember(semilla) { val r = Cordillera.azar(Cordillera.semillaDe("$semilla*")); List(40) { Triple(r() * 400, r() * 120, 0.4 + r() * 0.7) to (0.4 + r() * 0.5) } }
    val colores = remember(paleta) { List(5) { k -> Cordillera.colorCapa(m, k).let { Color(it[0], it[1], it[2]) } } }
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val detalle = size.height >= 120.dp.toPx()
            val s = maxOf(size.width / 400f, size.height / 240f)
            val ox = (size.width - 400f * s) / 2f
            val oy = size.height - 240f * s
            clipRect {
                translate(ox, oy) {
                    scale(s, s, pivot = Offset.Zero) {
                        drawRect(Brush.verticalGradient(0f to m.cieloArriba, 0.75f to m.cieloHorizonte, startY = 0f, endY = 240f), size = Size(400f, 240f))
                        if (m.noche && detalle) estrellas.forEach { (p, a) -> drawCircle(Color.White.copy(alpha = a.toFloat()), p.third.toFloat(), Offset(p.first.toFloat(), p.second.toFloat())) }
                        val ax = 40f + geo.second[0].toFloat() * 320f; val ay = 30f + geo.second[1].toFloat() * 50f
                        drawCircle(Brush.radialGradient(listOf(m.astro.copy(alpha = 0.55f), m.astro.copy(alpha = 0f)), Offset(ax, ay), 34f), 34f, Offset(ax, ay))
                        drawCircle(m.astro, if (m.noche) 9f else 12f, Offset(ax, ay))
                        fun camino(y: DoubleArray) = Path().apply {
                            moveTo(0f, 240f)
                            y.forEachIndexed { i, v -> lineTo(i * 400f / Cordillera.N, v.toFloat() * 240f) }
                            lineTo(400f, 240f); close()
                        }
                        geo.first.forEachIndexed { k, y ->
                            val c = camino(y)
                            drawPath(c, colores[k])
                            // Nieve en las cumbres lejanas (capas 1 y 2)
                            if (detalle && (k == 1 || k == 2)) {
                                val linea = (Cordillera.BASE[k] - Cordillera.AMP[k] * 0.3f) * 240f
                                clipRect(0f, 0f, 400f, linea) { drawPath(c, Color(0xFFF4F7F9).copy(alpha = if (m.noche) 0.35f else 0.85f)) }
                            }
                            // Bruma entre capas
                            if (detalle && (k == 2 || k == 3)) {
                                val yb = Cordillera.BASE[k + 1] * 240f - 18f
                                drawRect(Brush.verticalGradient(0f to m.cieloHorizonte.copy(alpha = 0f), 0.5f to m.cieloHorizonte.copy(alpha = 0.55f), 1f to m.cieloHorizonte.copy(alpha = 0f), startY = yb, endY = yb + 28f),
                                    topLeft = Offset(0f, yb), size = Size(400f, 28f))
                            }
                        }
                        // Bandera en la cima más alta de la capa 3
                        if (bandera) {
                            val y3 = geo.first[3]; var im = 0
                            y3.forEachIndexed { i, v -> if (v < y3[im]) im = i }
                            val x = im * 400f / Cordillera.N; val y = y3[im].toFloat() * 240f
                            if (x > 40f && x < 360f) {
                                drawLine(Color(0xFFF5EED9), Offset(x, y), Offset(x, y - 22f), 1.4f)
                                val tela = Path().apply { moveTo(x, y - 22f); lineTo(x + 13f, y - 18f); lineTo(x, y - 13f); close() }
                                drawPath(tela, Color(0xFFB8862F)); drawPath(tela, Color(0xFF6B2A1A), style = Stroke(0.7f))
                            }
                        }
                    }
                }
            }
        }
        contenido?.invoke(this)
    }
}

/** Ícono profesional de cada guía, bono o lectura. */
fun iconoGuia(id: String): ImageVector = when (id) {
    "descubre" -> Icons.Filled.Explore
    "diagnostico" -> Icons.Filled.Psychology
    "seis_ejes" -> Icons.Filled.Hub
    "viaje" -> Icons.Filled.Route
    "confluencia" -> Icons.Filled.Merge
    "proposito_valor" -> Icons.Filled.Paid
    "campamento" -> Icons.Filled.Groups
    "companero" -> Icons.Filled.Favorite
    "portales" -> Icons.Filled.DoorFront
    "caidas" -> Icons.Filled.Healing
    "niebla" -> Icons.Filled.Cloud
    "bono_anti_abandono" -> Icons.Filled.Shield
    "kit" -> Icons.Filled.LocalHospital
    "desde_cima" -> Icons.Filled.Flag
    "bono_evidencias" -> Icons.Filled.PhotoCamera
    "planificador_cierre" -> Icons.Filled.EmojiEvents
    "bono_indicadores" -> Icons.Filled.Insights
    "bono_metas" -> Icons.Filled.TrackChanges
    "bono_acciones" -> Icons.Filled.Bolt
    "bono_confluencia" -> Icons.AutoMirrored.Filled.TrendingUp
    "bono_cierre" -> Icons.Filled.Checklist
    "ebook" -> Icons.Filled.AutoStories
    "teoria" -> Icons.Filled.Science
    "facilitadores" -> Icons.Filled.School
    else -> Icons.AutoMirrored.Filled.MenuBook
}

val IconoMontana: ImageVector get() = Icons.Filled.Terrain
