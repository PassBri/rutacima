package com.rutaalacima.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rutaalacima.app.R

/** "hace 5 min", "hace 3 h", "hace 2 d" o la fecha (en el idioma de la app). */
@Composable
fun haceCuanto(millis: Long): String {
    if (millis <= 0) return ""
    val min = (System.currentTimeMillis() - millis) / 60_000
    return when {
        min < 1 -> stringResource(R.string.ahora)
        min < 60 -> stringResource(R.string.hace_min, min)
        min < 60 * 24 -> stringResource(R.string.hace_h, min / 60)
        min < 60 * 24 * 7 -> stringResource(R.string.hace_d, min / (60 * 24))
        else -> formatoFecha(millis)
    }
}
