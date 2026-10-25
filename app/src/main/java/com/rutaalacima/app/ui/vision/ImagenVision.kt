package com.rutaalacima.app.ui.vision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.request.ImageRequest
import java.io.File
import kotlin.math.roundToInt

/** Nunca se decodifica el original completo: miniaturas y visor piden su tamaño acotado. */
@Composable
internal fun solicitudFotoVision(ruta: String, ancho: Dp, alto: Dp = ancho): ImageRequest {
    val contexto = LocalContext.current.applicationContext
    val densidad = LocalDensity.current
    val anchoPx = with(densidad) { ancho.toPx().roundToInt().coerceIn(1, 1600) }
    val altoPx = with(densidad) { alto.toPx().roundToInt().coerceIn(1, 1600) }
    return remember(contexto, ruta, anchoPx, altoPx) {
        ImageRequest.Builder(contexto)
            .data(if (ruta.startsWith("http") || ruta.startsWith("content:")) ruta else File(ruta))
            .size(anchoPx, altoPx)
            .crossfade(false)
            .build()
    }
}
