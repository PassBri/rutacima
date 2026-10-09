package com.rutaalacima.app.ui.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import com.rutaalacima.app.domain.model.Mandala
import java.io.File
import java.io.FileOutputStream

/**
 * La imagen del 9×9 para compartir en la comunidad (1080 × 1350, el formato vertical de los muros):
 * la cumbre arriba, la cuadrícula con las fotos del vision board en el centro y el avance del
 * Método Cima 9×52 abajo.
 */
object MandalaImagen {
    /** Textos ya traducidos que van en la imagen. */
    data class Textos(val etiqueta: String, val avance: String, val montana: String, val marca: String)

    private const val ANCHO = 1080
    private const val ALTO = 1350
    private val PAPEL = 0xFFF7F3EE.toInt()
    private val TINTA = 0xFF2B1E18.toInt()
    private val BURDEOS = 0xFF6B2A1A.toInt()
    private val ORO = 0xFFC9973B.toInt()
    private val VACIO = 0xFFEDE4D9.toInt()

    fun generar(context: Context, datos: DatosMandala, t: Textos): File {
        val bmp = Bitmap.createBitmap(ANCHO, ALTO, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(PAPEL)
        val serif = Typeface.create(Typeface.SERIF, Typeface.BOLD)

        // Encabezado: etiqueta y la cumbre
        texto(c, t.etiqueta.uppercase(), 40f, 40f, 1000, 26f, BURDEOS, Typeface.DEFAULT_BOLD, 1, Layout.Alignment.ALIGN_NORMAL)
        texto(c, datos.tituloCumbre, 40f, 78f, 1000, 44f, TINTA, serif, 2, Layout.Alignment.ALIGN_NORMAL)

        // Cuadrícula
        val x0 = 40f; val y0 = 190f; val lado = 1000f
        val sepBloque = 10f; val sepCelda = 3f
        val bloque = (lado - 2 * sepBloque) / 3
        val celda = (bloque - 2 * sepCelda) / 3
        val fondo = Paint(Paint.ANTI_ALIAS_FLAG)
        Mandala.CELDAS.forEach { cel ->
            val bx = cel.col / 3; val by = cel.fila / 3
            val x = x0 + bx * (bloque + sepBloque) + (cel.col % 3) * (celda + sepCelda)
            val y = y0 + by * (bloque + sepBloque) + (cel.fila % 3) * (celda + sepCelda)
            val r = RectF(x, y, x + celda, y + celda)
            when (cel.tipo) {
                Mandala.Tipo.CUMBRE -> {
                    fondo.shader = LinearGradient(x, y, x, y + celda, 0xFFFFF8EC.toInt(), ORO, Shader.TileMode.CLAMP)
                    c.drawRoundRect(r, 14f, 14f, fondo); fondo.shader = null
                    val foto = datos.cumbre?.foto?.let { foto(it) }
                    if (foto != null) { dibujarFoto(c, foto, r, 0x66000000) }
                    texto(c, datos.tituloCumbre, x + 6, y, (celda - 12).toInt(), 17f, if (foto != null) 0xFFFFFFFF.toInt() else 0xFF3A1A10.toInt(),
                        Typeface.DEFAULT_BOLD, 5, Layout.Alignment.ALIGN_CENTER, alto = celda)
                }
                Mandala.Tipo.CAMPAMENTO -> {
                    val cv = datos.casilla(cel.campamento)
                    val color = if (cv != null) datos.color(cel.campamento).toArgb() else VACIO
                    fondo.color = color; c.drawRoundRect(r, 14f, 14f, fondo)
                    val foto = cv?.foto?.let { foto(it) }
                    if (foto != null) dibujarFoto(c, foto, r, (color and 0x00FFFFFF) or 0x8C000000.toInt())
                    texto(c, cv?.let { nombreCampamento(it) }.orEmpty(), x + 6, y, (celda - 12).toInt(), 15f, 0xFFFFFFFF.toInt(),
                        Typeface.DEFAULT_BOLD, 5, Layout.Alignment.ALIGN_CENTER, alto = celda)
                }
                Mandala.Tipo.PASO -> {
                    val color = datos.color(cel.campamento).toArgb()
                    val estado = datos.estado(cel.campamento, cel.paso)
                    fondo.color = when (estado) {
                        Mandala.EstadoPaso.VACIO -> VACIO
                        Mandala.EstadoPaso.ESCRITO -> mezclar(0xFFFFFFFF.toInt(), color, 0.16f)
                        Mandala.EstadoPaso.HECHO -> mezclar(0xFFFFFFFF.toInt(), color, 0.45f)
                    }
                    c.drawRoundRect(r, 10f, 10f, fondo)
                    texto(c, datos.paso(cel.campamento, cel.paso), x + 5, y, (celda - 10).toInt(), 14f, TINTA,
                        Typeface.DEFAULT, 5, Layout.Alignment.ALIGN_CENTER, alto = celda)
                    if (estado == Mandala.EstadoPaso.HECHO) {
                        fondo.color = color; c.drawCircle(x + celda - 14, y + 14, 10f, fondo)
                        texto(c, "✓", x + celda - 24, y + 3, 20, 15f, 0xFFFFFFFF.toInt(), Typeface.DEFAULT_BOLD, 1, Layout.Alignment.ALIGN_CENTER)
                    }
                }
            }
        }

        // Pie: avance, montaña en milímetros y la marca
        val pie = y0 + lado + 14
        texto(c, t.avance, 40f, pie, 1000, 30f, TINTA, Typeface.DEFAULT_BOLD, 1, Layout.Alignment.ALIGN_NORMAL)
        texto(c, t.montana, 40f, pie + 44, 1000, 26f, BURDEOS, Typeface.DEFAULT, 1, Layout.Alignment.ALIGN_NORMAL)
        fondo.color = BURDEOS; c.drawRect(0f, ALTO - 64f, ANCHO.toFloat(), ALTO.toFloat(), fondo)
        texto(c, t.marca, 40f, ALTO - 50f, 1000, 26f, 0xFFFFFFFF.toInt(), serif, 1, Layout.Alignment.ALIGN_CENTER)

        val dir = File(context.cacheDir, "compartir").apply { mkdirs() }
        val f = File(dir, "mandala-${System.currentTimeMillis()}.jpg")
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bmp.recycle()
        return f
    }

    private fun texto(
        c: Canvas, s: String, x: Float, y: Float, ancho: Int, tam: Float, color: Int, tf: Typeface,
        lineas: Int, alinear: Layout.Alignment, alto: Float? = null,
    ) {
        if (s.isBlank() || ancho <= 0) return
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = tam; this.color = color; typeface = tf }
        val l = StaticLayout.Builder.obtain(s, 0, s.length, p, ancho).setAlignment(alinear)
            .setMaxLines(lineas).setEllipsize(TextUtils.TruncateAt.END).setLineSpacing(0f, 1.05f).build()
        c.save()
        c.translate(x, if (alto != null) y + (alto - l.height) / 2 else y)
        l.draw(c)
        c.restore()
    }

    /** Foto local reducida (las fotos remotas no se descargan aquí: se usa el color del campamento). */
    private fun foto(ruta: String): Bitmap? {
        if (ruta.startsWith("http")) return null
        val f = File(ruta); if (!f.exists()) return null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, o)
        var m = 1
        while (minOf(o.outWidth, o.outHeight) / (m * 2) >= 220) m *= 2
        return BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = m })
    }

    private fun dibujarFoto(c: Canvas, b: Bitmap, r: RectF, velo: Int) {
        val lado = minOf(b.width, b.height)
        val src = Rect((b.width - lado) / 2, (b.height - lado) / 2, (b.width + lado) / 2, (b.height + lado) / 2)
        c.save()
        c.clipRect(r)
        c.drawBitmap(b, src, r, Paint(Paint.FILTER_BITMAP_FLAG))
        c.drawColor(velo)
        c.restore()
    }

    private fun mezclar(a: Int, b: Int, t: Float): Int {
        fun canal(x: Int, s: Int) = (x shr s) and 0xFF
        fun m(s: Int) = (canal(a, s) + (canal(b, s) - canal(a, s)) * t).toInt()
        return (0xFF shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
    }
}
