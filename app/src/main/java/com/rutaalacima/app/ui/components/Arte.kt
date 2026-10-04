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
import java.util.Random

/** Paleta de un paisaje: cielo (arriba, abajo), sol y tres capas de montaña (lejana → cercana). */
data class Paleta(val cielo1: Color, val cielo2: Color, val sol: Color, val c1: Color, val c2: Color, val c3: Color, val estrellas: Boolean = false)

val PALETAS = listOf(
    Paleta(Color(0xFFF7D9A8), Color(0xFFFBEFD9), Color(0xFFE8A33A), Color(0xFFD9A77A), Color(0xFFB0683F), Color(0xFF5A0C08)),      // amanecer dorado
    Paleta(Color(0xFF5A0C08), Color(0xFFC9603A), Color(0xFFF5C451), Color(0xFF8E2A1E), Color(0xFF5E1610), Color(0xFF2B0A05)),      // atardecer burdeos
    Paleta(Color(0xFF0F1B33), Color(0xFF34497A), Color(0xFFF5EED9), Color(0xFF3B4E7E), Color(0xFF26355C), Color(0xFF121C35), true), // noche estrellada
    Paleta(Color(0xFFBFE3D0), Color(0xFFEAF6EE), Color(0xFFF5C451), Color(0xFF7FB89A), Color(0xFF3E7D5E), Color(0xFF1F4A37)),      // bosque
    Paleta(Color(0xFFD9CFF2), Color(0xFFF4EEFB), Color(0xFFF2B5C8), Color(0xFFA996D6), Color(0xFF6A4C93), Color(0xFF3A2860)),      // lavanda
    Paleta(Color(0xFFCFE7F5), Color(0xFFF2F8FC), Color(0xFFFFFFFF), Color(0xFFA8C6DD), Color(0xFF5C87A8), Color(0xFF2E4E6B)),      // nieve
    Paleta(Color(0xFFF5D3B3), Color(0xFFFCEBDD), Color(0xFFE76F51), Color(0xFFD9956A), Color(0xFFB0603A), Color(0xFF6E3220)),      // desierto
    Paleta(Color(0xFF9FD3E0), Color(0xFFE6F5F8), Color(0xFFF5C451), Color(0xFF5FA7B8), Color(0xFF2E7D9A), Color(0xFF184B5E)),      // océano
)

fun paletaDe(semilla: String): Int = (semilla.hashCode() and 0x7fffffff) % PALETAS.size

/**
 * Paisaje de montaña vectorial y único para cada semilla (guía, mes, publicación).
 * Reemplaza las imágenes de los libros: no usa archivos, se dibuja en tiempo real.
 */
@Composable
fun MontanaArte(
    semilla: String,
    modifier: Modifier = Modifier,
    paleta: Int = paletaDe(semilla),
    bandera: Boolean = true,
    contenido: (@Composable BoxScope.() -> Unit)? = null,
) {
    val p = PALETAS[paleta.mod(PALETAS.size)]
    val forma = remember(semilla) {
        val r = Random(semilla.hashCode().toLong())
        Triple(
            List(7) { 0.30f + r.nextFloat() * 0.22f },
            List(6) { 0.46f + r.nextFloat() * 0.22f },
            List(5) { 0.66f + r.nextFloat() * 0.18f },
        ) to Pair(0.18f + r.nextFloat() * 0.64f, 0.16f + r.nextFloat() * 0.14f)
    }
    val estrellas = remember(semilla) { val r = Random(semilla.hashCode() * 31L); List(28) { Offset(r.nextFloat(), r.nextFloat() * 0.5f) } }
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Brush.verticalGradient(listOf(p.cielo1, p.cielo2)))
            if (p.estrellas) estrellas.forEach { drawCircle(Color.White.copy(alpha = 0.7f), radius = 1.2f * density, center = Offset(it.x * w, it.y * h)) }
            val (solX, solY) = forma.second
            drawCircle(p.sol.copy(alpha = 0.35f), radius = h * 0.16f, center = Offset(solX * w, solY * h))
            drawCircle(p.sol, radius = h * 0.10f, center = Offset(solX * w, solY * h))

            fun capa(alturas: List<Float>, color: Color): Offset {
                val path = Path()
                path.moveTo(0f, h)
                var cima = Offset(0f, h)
                alturas.forEachIndexed { i, y ->
                    val x = w * i / (alturas.size - 1)
                    val pico = Offset(x, y * h)
                    if (pico.y < cima.y) cima = pico
                    if (i == 0) path.lineTo(0f, pico.y)
                    else {
                        // valle intermedio para que los picos se vean afilados
                        val xv = w * (i - 0.5f) / (alturas.size - 1)
                        path.lineTo(xv, (maxOf(y, alturas[i - 1]) + 0.12f).coerceAtMost(1f) * h)
                        path.lineTo(x, pico.y)
                    }
                }
                path.lineTo(w, h)
                path.close()
                drawPath(path, color)
                return cima
            }
            val (lejos, medio, cerca) = forma.first
            capa(lejos, p.c1)
            val cima = capa(medio, p.c2)
            capa(cerca, p.c3)
            if (bandera && cima.x in (w * 0.08f)..(w * 0.92f)) {
                val asta = h * 0.12f
                drawLine(Color(0xFFF5EED9), cima, Offset(cima.x, cima.y - asta), strokeWidth = 1.6f * density)
                val tela = Path().apply {
                    moveTo(cima.x, cima.y - asta)
                    lineTo(cima.x + asta * 0.6f, cima.y - asta * 0.78f)
                    lineTo(cima.x, cima.y - asta * 0.56f)
                    close()
                }
                drawPath(tela, Color(0xFFC9A033))
                drawPath(tela, Color(0xFF5A0C08), style = Stroke(width = 0.8f * density))
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
