package com.rutaalacima.app.ui.comunidad

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink

/**
 * A dónde lleva tocar una @mención ("@usuario") o una #etiqueta ("#tag") dentro de un texto.
 * RutaRoot lo provee con la navegación; por defecto no hace nada.
 */
val LocalAbrirEnlaceRed = staticCompositionLocalOf<(String) -> Unit> { {} }

private val PATRON = Regex("(?<=^|[\\s(])([@#])([\\p{L}\\p{N}._]{2,40})")

/** Texto de una publicación o comentario con @menciones y #etiquetas tocables. */
@Composable
fun TextoEnlazado(
    texto: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalContentColor.current, maxLines: Int = Int.MAX_VALUE, enlace: Color = MaterialTheme.colorScheme.primary,
) {
    val abrir = LocalAbrirEnlaceRed.current
    val estilo = TextLinkStyles(SpanStyle(color = enlace, fontWeight = FontWeight.SemiBold))
    val anotado = buildAnnotatedString {
        var i = 0
        PATRON.findAll(texto).forEach { m ->
            append(texto.substring(i, m.range.first))
            val valor = m.value.trimEnd('.')
            withLink(LinkAnnotation.Clickable(valor, estilo) { abrir(valor) }) { append(valor) }
            i = m.range.first + valor.length
        }
        append(texto.substring(i))
    }
    Text(anotado, modifier, color = color, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}
