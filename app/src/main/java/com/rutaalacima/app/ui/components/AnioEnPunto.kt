package com.rutaalacima.app.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * El año dentro de un punto de "Tu vida en puntos". La letra se ajusta al tamaño del punto
 * (tipografía labelSmall de Material 3, cifras tabulares): el año completo si cabe; si el punto
 * es muy pequeño, solo las dos últimas cifras ('84).
 */
@Composable
fun AnioEnPunto(anio: Int, color: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) { Etiqueta(anio, color) }
}

@Composable
private fun BoxWithConstraintsScope.Etiqueta(anio: Int, color: Color) {
    val ancho = maxWidth.value
    val completo = maxWidth >= 30.dp
    val texto = if (completo) anio.toString() else "'" + (anio % 100).toString().padStart(2, '0')
    val tam = (ancho * if (completo) 0.27f else 0.32f).coerceIn(7f, 13f)
    Text(
        texto,
        // El punto ya anuncia el año (contentDescription del botón); no se lee dos veces.
        modifier = Modifier.clearAndSetSemantics { },
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = tam.sp, lineHeight = tam.sp, letterSpacing = (-0.02).em,
            fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum", textAlign = TextAlign.Center,
        ),
        color = color,
        maxLines = 1,
        softWrap = false,
    )
}
