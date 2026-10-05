package com.rutaalacima.app.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.RoturaSello
import com.rutaalacima.app.domain.model.RoturaSello.P
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Sello de cera de la frase del día. Con [progreso] en 0 está entero; al animarlo de 0 a 1 se
 * agrieta desde el punto de impacto, tiembla y se parte en trozos con canto y sombra que saltan,
 * giran y caen, soltando migas de cera (receta en [RoturaSello]; la web la dibuja igual).
 * Los trozos pueden salirse del tamaño del componente: el dibujo no se recorta.
 */
@Composable
fun SelloDeCera(progreso: Float, semilla: Long, modifier: Modifier = Modifier) {
    val imagen = ImageBitmap.imageResource(R.drawable.logo_sello)
    val rotura = remember(semilla) { RoturaSello.generar(semilla) }
    Canvas(modifier) {
        val r = size.minDimension / 2
        val t = progreso.coerceIn(0f, 1f)
        translate(center.x, center.y) {
            when {
                t <= 0f -> { sombraReposo(r); imagenEntera(imagen, r, 0f, 0f) }
                t < RoturaSello.FIN_GRIETAS -> {
                    val tb = RoturaSello.temblor(t)
                    sombraReposo(r)
                    translate(tb.x * r, tb.y * r) {
                        imagenEntera(imagen, r, 0f, 0f)
                        grietas(rotura, r, RoturaSello.grietas(t))
                    }
                }
                else -> trozos(imagen, rotura, r, RoturaSello.vuelo(t))
            }
        }
    }
}

private val Canto = Color(0xFF360A05)
private val Reves = Color(0xFF6A1A10)
private val SombraCera = Color(0xFF46120A)

private fun camino(pts: List<P>, r: Float, ox: Float = 0f, oy: Float = 0f) = Path().apply {
    pts.forEachIndexed { i, p -> if (i == 0) moveTo((p.x - ox) * r, (p.y - oy) * r) else lineTo((p.x - ox) * r, (p.y - oy) * r) }
}

/** Dibuja la imagen del sello centrada en (cx, cy) con radio r. */
private fun DrawScope.imagenEntera(img: ImageBitmap, r: Float, cx: Float, cy: Float) {
    drawImage(
        img, srcOffset = IntOffset.Zero, srcSize = IntSize(img.width, img.height),
        dstOffset = IntOffset((cx - r).roundToInt(), (cy - r).roundToInt()), dstSize = IntSize((2 * r).roundToInt(), (2 * r).roundToInt()),
        filterQuality = FilterQuality.High,
    )
}

private fun DrawScope.sombraReposo(r: Float) {
    drawCircle(
        Brush.radialGradient(listOf(SombraCera.copy(alpha = 0.35f), Color.Transparent), center = Offset(0f, r * 0.12f), radius = r * 1.12f),
        radius = r * 1.12f, center = Offset(0f, r * 0.12f),
    )
}

/** Las grietas avanzando (hasta [g] de su largo): línea oscura con un brillo fino al lado. */
private fun DrawScope.grietas(rot: RoturaSello.Rotura, r: Float, g: Float) {
    fun parcial(pts: List<P>, prop: Float): Path {
        val largos = pts.zipWithNext { a, b -> hypot(b.x - a.x, b.y - a.y) }
        var resto = largos.sum() * prop
        return Path().apply {
            moveTo(pts[0].x * r, pts[0].y * r)
            for (i in largos.indices) {
                if (resto <= 0f) break
                val f = (resto / largos[i]).coerceAtMost(1f)
                val a = pts[i]; val b = pts[i + 1]
                lineTo((a.x + (b.x - a.x) * f) * r, (a.y + (b.y - a.y) * f) * r)
                resto -= largos[i]
            }
        }
    }
    val borde = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset.Zero, r * 0.96f)) }
    clipPath(borde) {
        listOf(Triple(Color(0x59FFCDB9), r * 0.022f, -r * 0.012f), Triple(Color(0xE6190402), r * 0.03f, 0f)).forEach { (color, ancho, d) ->
            translate(d, d) {
                val trazo = Stroke(width = ancho, cap = StrokeCap.Round, join = StrokeJoin.Round)
                rot.grietas.forEach { drawPath(parcial(it, g), color, style = trazo) }
                if (g > 0.45f) {
                    val fino = Stroke(width = ancho * 0.55f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    rot.finas.forEach { drawPath(parcial(it, (g - 0.45f) / 0.55f), color, style = fino) }
                }
            }
        }
    }
}

/** Los trozos en el aire: sombra en el papel, canto (grosor de la cera), cara del sello y filo. */
private fun DrawScope.trozos(img: ImageBitmap, rot: RoturaSello.Rotura, r: Float, u: Float) {
    for (f in rot.fragmentos) {
        val p = RoturaSello.pose(f, u)
        if (p.alfa <= 0f) continue
        val px = (f.pivote.x + p.dx) * r
        val py = (f.pivote.y + p.dy) * r
        val ox = f.pivote.x; val oy = f.pivote.y

        // Sombra: más lejos y más tenue mientras el trozo está en el aire (tres pasadas = borde suave)
        val sx = r * (0.04f + 0.12f * p.altura); val sy = r * (0.07f + 0.18f * p.altura)
        repeat(3) { k ->
            val extra = 1f + 0.05f * k * (1 + p.altura)
            withTransform({
                translate(px + sx, py + sy); rotate(p.grados, Offset.Zero); scale(p.escalaX * extra, extra, Offset.Zero)
            }) { drawPath(camino(f.cara, r, ox, oy), SombraCera.copy(alpha = (0.13f - 0.04f * p.altura) * p.alfa)) }
        }

        withTransform({ translate(px, py); rotate(p.grados, Offset.Zero); scale(p.escalaX, 1f, Offset.Zero) }) {
            val canto = r * 0.05f * (0.4f + abs(sin(f.volteo * Math.PI.toFloat() * u)) * 1.4f)
            translate(0f, canto) { drawPath(camino(f.cara, r, ox, oy), Canto.copy(alpha = p.alfa)) }
            clipPath(camino(f.contorno, r, ox, oy)) {
                if (p.escalaX < 0f) drawPath(camino(f.cara, r, ox, oy), Reves.copy(alpha = p.alfa))   // el revés de la cera, liso
                else drawImage(
                    img, srcOffset = IntOffset.Zero, srcSize = IntSize(img.width, img.height),
                    dstOffset = IntOffset(((-ox - 1f) * r).roundToInt(), ((-oy - 1f) * r).roundToInt()),
                    dstSize = IntSize((2 * r).roundToInt(), (2 * r).roundToInt()),
                    alpha = p.alfa, filterQuality = FilterQuality.High,
                )
            }
            listOf(Color(0xB3140301) to r * 0.03f, Color(0x47FFC8B4) to r * 0.01f).forEach { (color, ancho) ->
                f.filos.forEach { drawPath(camino(it, r, ox, oy), color.copy(alpha = color.alpha * p.alfa), style = Stroke(ancho, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
            }
        }
    }
    for (m in rot.migas) {
        val p = RoturaSello.pose(m, u)
        if (p.alfa <= 0f) continue
        val tam = m.tamano * r
        withTransform({ translate((m.origen.x + p.dx) * r, (m.origen.y + p.dy) * r); rotate(p.grados, Offset.Zero) }) {
            drawPath(Path().apply {
                moveTo(-tam, -tam * 0.4f); lineTo(tam * 0.3f, -tam); lineTo(tam, tam * 0.2f); lineTo(-tam * 0.2f, tam); close()
            }, (if (m.tamano > 0.05f) Color(0xFF5A140B) else Color(0xFF8E2618)).copy(alpha = p.alfa))
        }
    }
}

/** Crujido de la cera al romperse el sello (solo con el teléfono en modo sonido). */
object SonidoSello {
    private const val MUESTREO = 44_100
    @Volatile private var muestras: ShortArray? = null

    fun reproducir(c: Context) {
        val am = c.getSystemService(AudioManager::class.java) ?: return
        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        thread(name = "crujido-sello") {
            runCatching {
                val m = muestras ?: RoturaSello.sonido(MUESTREO).also { muestras = it }
                val pista = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(MUESTREO)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                    )
                    .setBufferSizeInBytes(m.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                try {
                    pista.write(m, 0, m.size)
                    pista.setVolume(0.6f)
                    pista.play()
                    Thread.sleep(m.size * 1000L / MUESTREO + 200)
                } finally {
                    pista.release()
                }
            }
        }
    }
}
