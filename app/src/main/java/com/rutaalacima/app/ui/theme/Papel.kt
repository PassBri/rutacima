package com.rutaalacima.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Sistema visual "papel": textura de grano, dobleces de una hoja plegada en tercios, viñeta
 * de papel envejecido, hojas con esquina doblada y sombras cálidas, y botones que flotan sobre
 * el papel y se hunden al tocarlos. Todo se dibuja en código (sin imágenes) y respeta los
 * roles de color de Material 3, así funciona igual en papel antiguo, pastel y modo oscuro.
 */
object Papel {
    /**
     * Color de las sombras, nunca gris: en papel blanco son burdeos (el color del sello);
     * en papel antiguo y pastel, café tostado.
     */
    val Sombra: Color
        get() = if (EstiloActual.estilo == EstiloPapel.BLANCO) Color(0xFF18262F) else Color(0xFF4A3218)

    /** Cuánto se nota el papel (grano y dobleces): sutil en blanco, marcado en antiguo. */
    val intensidad: Float
        get() = if (EstiloActual.estilo == EstiloPapel.BLANCO) 0.45f else 1f

    /** Textura de grano: 160×160 px, semilla fija, se genera una sola vez. */
    val grano by lazy {
        val n = 160
        val r = Random(120)
        val px = IntArray(n * n) {
            val a = (r.nextFloat() * r.nextFloat() * 34).toInt() // grano fino y disperso
            (a shl 24) or 0x3A2410
        }
        Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}

/**
 * La luz del día (la misma de la web): al amanecer y al atardecer el papel se calienta apenas,
 * como máximo un 3 % hacia el oro. Se actualiza cada minuto desde la raíz de la app.
 */
object LuzDelDia {
    /** 0 = luz neutra (mediodía o noche), 1 = sol bajo (máxima calidez: 3 % hacia el oro). */
    var calidez by androidx.compose.runtime.mutableFloatStateOf(0f)
        private set

    fun actualizar(ms: Long = System.currentTimeMillis()) {
        val lng = com.rutaalacima.app.domain.model.Astronomia.longitudPorZona(java.util.TimeZone.getDefault().getOffset(ms).toLong())
        val alt = com.rutaalacima.app.domain.model.Astronomia.sol(ms, lng = lng).alturaGrados
        calidez = (1 - kotlin.math.abs(alt - 4) / 14).coerceIn(0.0, 1.0).toFloat()
    }
}

/**
 * Fondo de hoja plegada: color de fondo, grano, un doblez vertical al centro, dos dobleces
 * horizontales (la hoja se dobló en tercios) y una viñeta de papel antiguo en los bordes.
 */
fun Modifier.fondoPapel(): Modifier = composed {
    val fondo = MaterialTheme.colorScheme.background
    val oscuro = MaterialTheme.colorScheme.background.luminanceAprox() < 0.3f
    drawWithCache {
        val grano = ShaderBrush(ImageShader(Papel.grano, TileMode.Repeated, TileMode.Repeated))
        val k = Papel.intensidad
        val sombra = Papel.Sombra.copy(alpha = if (oscuro) 0.35f else 0.10f * k)
        val luz = Color.White.copy(alpha = if (oscuro) 0.04f else 0.35f + 0.4f * (1 - k))
        val vineta = Brush.radialGradient(
            0.6f to Color.Transparent, 1f to Papel.Sombra.copy(alpha = if (oscuro) 0.4f else 0.16f * k),
            center = Offset(size.width / 2, size.height * 0.42f), radius = size.maxDimension * 0.75f,
        )
        val altitud = EstiloActual.estilo == EstiloPapel.BLANCO || oscuro
        onDrawBehind {
            drawRect(fondo)
            if (altitud) { drawRect(grano, alpha = if (oscuro) 0.25f else 0.3f); return@onDrawBehind }
            drawRect(grano, alpha = k)
            // Doblez vertical: sombra de un lado, brillo del otro
            val cx = size.width / 2
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, sombra), startX = cx - 18f, endX = cx),
                topLeft = Offset(cx - 18f, 0f), size = Size(18f, size.height))
            drawRect(Brush.horizontalGradient(listOf(luz, Color.Transparent), startX = cx, endX = cx + 14f),
                topLeft = Offset(cx, 0f), size = Size(14f, size.height))
            // Dobleces horizontales en tercios
            for (t in listOf(1f / 3f, 2f / 3f)) {
                val y = size.height * t
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, sombra), startY = y - 14f, endY = y),
                    topLeft = Offset(0f, y - 14f), size = Size(size.width, 14f))
                drawRect(Brush.verticalGradient(listOf(luz, Color.Transparent), startY = y, endY = y + 10f),
                    topLeft = Offset(0f, y), size = Size(size.width, 10f))
            }
            drawRect(vineta)
        }
    }
}

/** Forma de hoja: esquinas redondeadas; con [doblez] > 0, la esquina superior derecha se dobla (solo papeles especiales). */
class FormaHoja(private val doblez: Dp = 0.dp, private val radio: Dp = 14.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val d = with(density) { doblez.toPx() }.coerceAtMost(size.minDimension / 2)
        val r = with(density) { radio.toPx() }.coerceAtMost(size.minDimension / 2)
        if (d <= 0f) return Outline.Rounded(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, androidx.compose.ui.geometry.CornerRadius(r)))
        val p = Path().apply {
            moveTo(r, 0f)
            lineTo(size.width - d, 0f)
            lineTo(size.width, d)
            lineTo(size.width, size.height - r)
            quadraticBezierTo(size.width, size.height, size.width - r, size.height)
            lineTo(r, size.height)
            quadraticBezierTo(0f, size.height, 0f, size.height - r)
            lineTo(0f, r)
            quadraticBezierTo(0f, 0f, r, 0f)
            close()
        }
        return Outline.Generic(p)
    }
}

/**
 * Hoja: en Altitud, una hoja limpia con esquinas redondeadas, línea fina de instrumento y sombra
 * suave (sin oreja ni pliegue). Con [doblez] > 0 o [pliegue] se dibujan la oreja y el pliegue de
 * los papeles especiales. [color] es el color del papel (por defecto la superficie del tema).
 */
fun Modifier.hojaPapel(
    color: Color? = null,
    elevacion: Dp = 2.dp,
    doblez: Dp = 0.dp,
    pliegue: Boolean = false,
): Modifier = composed {
    val base = color ?: MaterialTheme.colorScheme.surfaceContainerLow
    // Papel de la interfaz en claro: se calienta apenas con el sol bajo (nunca cambia el contraste del texto)
    val papel = if (color == null && base.luminanceAprox() > 0.5f)
        androidx.compose.ui.graphics.lerp(base, MaterialTheme.colorScheme.secondary, 0.03f * LuzDelDia.calidez) else base
    val borde = MaterialTheme.colorScheme.outlineVariant
    val forma = remember(doblez) { FormaHoja(doblez) }
    this
        .shadow(elevacion, forma, clip = false, ambientColor = Papel.Sombra.copy(alpha = 0.5f), spotColor = Papel.Sombra.copy(alpha = 0.5f))
        .drawWithCache {
            val outline = forma.createOutline(size, layoutDirection, this)
            if (doblez.value <= 0f) {
                val rr = (outline as Outline.Rounded).roundRect
                val camino = Path().apply { addRoundRect(rr) }
                return@drawWithCache onDrawWithContent {
                    drawPath(camino, papel)
                    drawPath(camino, borde, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                    drawContent()
                }
            }
            val d = doblez.toPx().coerceAtMost(size.minDimension / 2)
            val grano = ShaderBrush(ImageShader(Papel.grano, TileMode.Repeated, TileMode.Repeated))
            // Pliegue diagonal: una línea de sombra y luz que cruza la hoja
            val diag = Brush.linearGradient(
                0.47f to Color.Transparent, 0.495f to Papel.Sombra.copy(alpha = 0.07f),
                0.505f to Color.White.copy(alpha = 0.30f), 0.54f to Color.Transparent,
                start = Offset(0f, size.height), end = Offset(size.width, 0f),
            )
            val oreja = Path().apply {
                moveTo(size.width - d, 0f); lineTo(size.width - d, d); lineTo(size.width, d); close()
            }
            val orejaBrush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.55f), papel, Papel.Sombra.copy(alpha = 0.18f)),
                start = Offset(size.width - d, d), end = Offset(size.width, 0f),
            )
            onDrawWithContent {
                val p = (outline as Outline.Generic).path
                drawPath(p, papel)
                drawPath(p, grano, alpha = 0.6f * Papel.intensidad)
                if (pliegue) drawPath(p, diag)
                drawContent()
                // Sombra bajo la oreja y la oreja doblada
                drawPath(oreja, Papel.Sombra.copy(alpha = 0.18f))
                drawPath(Path().apply {
                    moveTo(size.width - d, 0f); lineTo(size.width - d, d - 1.5f); lineTo(size.width - 1.5f, d); close()
                }, orejaBrush)
            }
        }
}

/**
 * Botón flotante sobre el papel: proyecta sombra y, al tocarlo, se hunde (escala y sombra
 * bajan). Úsalo en años, meses, semanas y días.
 */
fun Modifier.flotante(
    interaccion: MutableInteractionSource,
    forma: Shape,
    elevacion: Dp = 4.dp,
): Modifier = composed {
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(if (presionado) 0.92f else 1f, spring(dampingRatio = 0.5f), label = "escala")
    val sombra by animateDpAsState(if (presionado) 1.dp else elevacion, label = "sombra")
    this
        .graphicsLayer { scaleX = escala; scaleY = escala }
        .shadow(sombra, forma, clip = false, ambientColor = Papel.Sombra, spotColor = Papel.Sombra)
}

/** Luminancia aproximada (suficiente para saber si el tema es oscuro). */
private fun Color.luminanceAprox(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** Interacción recordada para [flotante]. */
@Composable
fun rememberToque(): MutableInteractionSource = remember { MutableInteractionSource() }
