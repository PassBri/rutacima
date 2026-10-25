package com.rutaalacima.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as LienzoAndroid
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rutaalacima.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/** Formato del recorte: proporción ancho/alto (null = la de la foto) y si se ve como círculo. */
data class FormatoFoto(val nombre: Int, val proporcion: Float?, val circulo: Boolean = false)

object FormatosFoto {
    val PERFIL = listOf(FormatoFoto(R.string.ef_cuadrada, 1f, circulo = true))
    val PORTADA = listOf(FormatoFoto(R.string.ef_portada, 3f))
    /** Publicaciones: como en Instagram, cuadrada, vertical 4:5 u horizontal 1,91:1 (o la original, dentro de esos límites). */
    val PUBLICACION = listOf(
        FormatoFoto(R.string.ef_cuadrada, 1f), FormatoFoto(R.string.ef_vertical, 4f / 5f),
        FormatoFoto(R.string.ef_horizontal, 1.91f), FormatoFoto(R.string.ef_original, null),
    )
    /** Límites de proporción para mostrar fotos en el muro (las más largas se recortan a estos bordes). */
    const val MIN = 4f / 5f
    const val MAX = 1.91f
}

/** Filtros sobrios (matrices de color 4×5). */
enum class FiltroFoto(val nombre: Int, val matriz: FloatArray?) {
    ORIGINAL(R.string.ef_f_original, null),
    CALIDO(R.string.ef_f_calido, floatArrayOf(1.08f, 0f, 0f, 0f, 8f, 0f, 1.02f, 0f, 0f, 2f, 0f, 0f, 0.92f, 0f, -6f, 0f, 0f, 0f, 1f, 0f)),
    FRIO(R.string.ef_f_frio, floatArrayOf(0.94f, 0f, 0f, 0f, -4f, 0f, 1.0f, 0f, 0f, 2f, 0f, 0f, 1.08f, 0f, 8f, 0f, 0f, 0f, 1f, 0f)),
    BYN(R.string.ef_f_byn, floatArrayOf(0.30f, 0.59f, 0.11f, 0f, 0f, 0.30f, 0.59f, 0.11f, 0f, 0f, 0.30f, 0.59f, 0.11f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)),
    SEPIA(R.string.ef_f_sepia, floatArrayOf(0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f, 0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)),
    CONTRASTE(R.string.ef_f_contraste, floatArrayOf(1.2f, 0f, 0f, 0f, -24f, 0f, 1.2f, 0f, 0f, -24f, 0f, 0f, 1.2f, 0f, -24f, 0f, 0f, 0f, 1f, 0f)),
}

/** Carga la imagen reducida (lado mayor ≤ 2048 px) y respetando la orientación EXIF. */
private fun cargar(ctx: Context, uri: Uri): Bitmap? = runCatching {
    val cr = ctx.contentResolver
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
    var m = 1
    while (max(o.outWidth, o.outHeight) / m > 2048) m *= 2
    val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = m }) } ?: return null
    val giro = cr.openInputStream(uri)?.use { androidx.exifinterface.media.ExifInterface(it).rotationDegrees } ?: 0
    if (giro == 0) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(giro.toFloat()) }, true)
}.getOrNull()

private fun girar(b: Bitmap) = Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(90f) }, true)

/**
 * Editor de fotos de SuRoute: recortar (con formato), acercar y mover con dos dedos, girar y aplicar un filtro.
 * Devuelve la foto final en JPEG (lado mayor ≤ [ladoMax]).
 */
@Composable
fun EditorFoto(
    uri: Uri,
    formatos: List<FormatoFoto>,
    ladoMax: Int,
    onListo: (ByteArray) -> Unit,
    onCancelar: () -> Unit,
) {
    val ctx = LocalContext.current
    val alcance = rememberCoroutineScope()
    var foto by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) { foto = withContext(Dispatchers.IO) { cargar(ctx, uri) } ?: run { onCancelar(); null } }
    var formato by remember { mutableIntStateOf(0) }
    var filtro by remember { mutableStateOf(FiltroFoto.ORIGINAL) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var desp by remember { mutableStateOf(Offset.Zero) }
    var marco by remember { mutableStateOf(IntSize.Zero) }
    var guardando by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF0E1418)) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCancelar) { Icon(Icons.Filled.Close, stringResource(R.string.cancelar), tint = Color.White) }
                    Text(stringResource(R.string.ef_titulo), color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { foto = foto?.let(::girar); zoom = 1f; desp = Offset.Zero }) {
                        Icon(Icons.Filled.RotateRight, stringResource(R.string.ef_girar), tint = Color.White)
                    }
                }
                val b = foto
                val f = formatos[formato]
                val proporcion = f.proporcion ?: b?.let { (it.width.toFloat() / it.height).coerceIn(FormatosFoto.MIN, FormatosFoto.MAX) } ?: 1f
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (b != null) {
                        // El marco ocupa lo que permita la pantalla con la proporción elegida
                        val anchoMax = maxWidth; val altoMax = maxHeight
                        val ancho = if (anchoMax / altoMax > proporcion) altoMax * proporcion else anchoMax
                        val imagen = remember(b) { b.asImageBitmap() }
                        val filtroCompose = filtro.matriz?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
                        Box(
                            Modifier.width(ancho).aspectRatio(proporcion).clip(RoundedCornerShape(if (f.circulo) 0.dp else 12.dp))
                                .pointerInput(b, proporcion) {
                                    detectTransformGestures { _, pan, z, _ ->
                                        zoom = (zoom * z).coerceIn(1f, 5f)
                                        desp = limitar(desp + pan, b, marco, zoom)
                                    }
                                }
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                marco = IntSize(size.width.toInt(), size.height.toInt())
                                val s = escalaBase(b, size) * zoom
                                val d = limitar(desp, b, marco, zoom)
                                val izq = (size.width - b.width * s) / 2f + d.x
                                val arr = (size.height - b.height * s) / 2f + d.y
                                drawImage(imagen, dstOffset = androidx.compose.ui.unit.IntOffset(izq.toInt(), arr.toInt()),
                                    dstSize = IntSize((b.width * s).toInt(), (b.height * s).toInt()), colorFilter = filtroCompose)
                                if (f.circulo) {
                                    // Lo que queda fuera del círculo se oscurece: así se ve la foto de perfil final
                                    drawRect(Color(0x99000000))
                                    drawCircle(Color.Black, radius = size.minDimension / 2f, blendMode = BlendMode.DstOut)
                                }
                            }
                            // Cuadrícula de tercios mientras se ajusta
                            Canvas(Modifier.fillMaxSize()) {
                                val c = Color.White.copy(alpha = 0.35f)
                                for (k in 1..2) {
                                    drawLine(c, Offset(size.width * k / 3f, 0f), Offset(size.width * k / 3f, size.height), 1f)
                                    drawLine(c, Offset(0f, size.height * k / 3f), Offset(size.width, size.height * k / 3f), 1f)
                                }
                            }
                        }
                    } else {
                        androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (formatos.size > 1) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        formatos.forEachIndexed { i, fo ->
                            FilterChip(selected = i == formato, onClick = { formato = i; zoom = 1f; desp = Offset.Zero },
                                label = { Text(stringResource(fo.nombre)) })
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                // Filtros con una miniatura de la foto
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val mini = remember(foto) { foto?.let { Bitmap.createScaledBitmap(it, 120, (120f * it.height / it.width).toInt().coerceAtLeast(1), true).asImageBitmap() } }
                    FiltroFoto.entries.forEach { fi ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { filtro = fi }) {
                            Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                                .border(if (fi == filtro) 2.dp else 0.dp, if (fi == filtro) Color.White else Color.Transparent, RoundedCornerShape(12.dp))) {
                                if (mini != null) Image(mini, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                                    colorFilter = fi.matriz?.let { ColorFilter.colorMatrix(ColorMatrix(it)) })
                            }
                            Text(stringResource(fi.nombre), color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val bb = foto ?: return@Button
                        guardando = true
                        alcance.launch {
                            val bytes = withContext(Dispatchers.Default) { recortar(bb, marco, zoom, desp, filtro, ladoMax) }
                            guardando = false
                            onListo(bytes)
                        }
                    },
                    enabled = foto != null && !guardando && marco != IntSize.Zero,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.ef_usar)) }
            }
        }
    }
}

private fun escalaBase(b: Bitmap, marco: Size) = max(marco.width / b.width, marco.height / b.height)

/** El desplazamiento nunca deja ver bordes vacíos dentro del marco. */
private fun limitar(d: Offset, b: Bitmap, marco: IntSize, zoom: Float): Offset {
    if (marco == IntSize.Zero) return d
    val s = escalaBase(b, Size(marco.width.toFloat(), marco.height.toFloat())) * zoom
    val mx = max(0f, (b.width * s - marco.width) / 2f)
    val my = max(0f, (b.height * s - marco.height) / 2f)
    return Offset(d.x.coerceIn(-mx, mx), d.y.coerceIn(-my, my))
}

/** Lleva el encuadre visto en pantalla a la foto real, aplica el filtro y la entrega en JPEG. */
private fun recortar(b: Bitmap, marco: IntSize, zoom: Float, desp: Offset, filtro: FiltroFoto, ladoMax: Int): ByteArray {
    val s = escalaBase(b, Size(marco.width.toFloat(), marco.height.toFloat())) * zoom
    val d = limitar(desp, b, marco, zoom)
    val izq = (marco.width - b.width * s) / 2f + d.x
    val arr = (marco.height - b.height * s) / 2f + d.y
    val fuente = RectF(-izq / s, -arr / s, (marco.width - izq) / s, (marco.height - arr) / s)
    val escalaSalida = min(1f, ladoMax / max(fuente.width(), fuente.height()))
    val w = (fuente.width() * escalaSalida).toInt().coerceAtLeast(1)
    val h = (fuente.height() * escalaSalida).toInt().coerceAtLeast(1)
    val salida = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val lienzo = LienzoAndroid(salida)
    val pintura = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        filtro.matriz?.let { colorFilter = ColorMatrixColorFilter(android.graphics.ColorMatrix(it)) }
    }
    lienzo.drawBitmap(b, android.graphics.Rect(fuente.left.toInt(), fuente.top.toInt(), fuente.right.toInt(), fuente.bottom.toInt()),
        android.graphics.Rect(0, 0, w, h), pintura)
    return ByteArrayOutputStream().use { salida.compress(Bitmap.CompressFormat.JPEG, 88, it); it.toByteArray() }
}
