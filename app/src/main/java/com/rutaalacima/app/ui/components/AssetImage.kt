package com.rutaalacima.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Caché simple en memoria de las ilustraciones ya decodificadas. */
private object CacheImagenes {
    private val cache = object : android.util.LruCache<String, ImageBitmap>(24) {}
    fun get(k: String): ImageBitmap? = cache.get(k)
    fun put(k: String, v: ImageBitmap) { cache.put(k, v) }
}

/**
 * Muestra una imagen de assets (por defecto de content/img). Reserva el espacio con
 * [ratio] mientras carga para que la lista no "salte".
 */
@Composable
fun AssetImage(
    path: String,
    ratio: Float,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val ruta = if (path.contains('/')) path else "content/img/$path"
    val bitmap by produceState<ImageBitmap?>(initialValue = CacheImagenes.get(ruta), ruta) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open(ruta).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
                }.getOrNull()
            }?.also { CacheImagenes.put(ruta, it) }
        }
    }
    val forma = modifier.fillMaxWidth().aspectRatio(ratio.coerceIn(0.3f, 3f)).clip(RoundedCornerShape(12.dp))
    val bmp = bitmap
    if (bmp != null) {
        Image(bmp, contentDescription, modifier = forma, contentScale = contentScale)
    } else {
        Box(forma.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
