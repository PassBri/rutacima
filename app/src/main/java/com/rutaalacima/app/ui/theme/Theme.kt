package com.rutaalacima.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Identidad visual tomada de los workbooks: sello de lacre burdeos, franja dorada,
 * papel kraft/crema y tipografía Georgia (serif) para la lectura.
 */
object Marca {
    val Burdeos = Color(0xFF5A0C08)
    val BurdeosClaro = Color(0xFF8A1C14)
    val Oro = Color(0xFFC9A033)
    val OroClaro = Color(0xFFD5B65F)
    val OroPalido = Color(0xFFF5EED9)
    val Kraft = Color(0xFFD69A6B)
    val Crema = Color(0xFFFBF7EE)
    val TintaCafe = Color(0xFF2B0A05)
    val Noche = Color(0xFF1C1210)
    val NocheSuperficie = Color(0xFF2A1D19)
}

private val Claro = lightColorScheme(
    primary = Marca.Burdeos,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3DAD5),
    onPrimaryContainer = Marca.TintaCafe,
    secondary = Marca.Oro,
    onSecondary = Marca.TintaCafe,
    secondaryContainer = Marca.OroPalido,
    onSecondaryContainer = Marca.TintaCafe,
    tertiary = Marca.Kraft,
    onTertiary = Marca.TintaCafe,
    background = Marca.Crema,
    onBackground = Marca.TintaCafe,
    surface = Marca.Crema,
    onSurface = Marca.TintaCafe,
    surfaceVariant = Color(0xFFF2EADB),
    onSurfaceVariant = Color(0xFF5B4A42),
    surfaceContainer = Color(0xFFF7F1E4),
    surfaceContainerLow = Color(0xFFFAF5EA),
    surfaceContainerHigh = Color(0xFFF2EADB),
    outline = Color(0xFFCCBFA8),
    outlineVariant = Color(0xFFE5DACA),
)

private val Oscuro = darkColorScheme(
    primary = Marca.OroClaro,
    onPrimary = Marca.TintaCafe,
    primaryContainer = Marca.BurdeosClaro,
    onPrimaryContainer = Marca.OroPalido,
    secondary = Color(0xFFE6A49E),
    onSecondary = Marca.TintaCafe,
    secondaryContainer = Color(0xFF4A2A1E),
    onSecondaryContainer = Marca.OroPalido,
    tertiary = Marca.Kraft,
    background = Marca.Noche,
    onBackground = Color(0xFFF1E6D6),
    surface = Marca.Noche,
    onSurface = Color(0xFFF1E6D6),
    surfaceVariant = Color(0xFF3A2C26),
    onSurfaceVariant = Color(0xFFD8C6B5),
    surfaceContainer = Marca.NocheSuperficie,
    surfaceContainerLow = Color(0xFF231815),
    surfaceContainerHigh = Color(0xFF34261F),
    outline = Color(0xFF7D6A5E),
    outlineVariant = Color(0xFF4A3B33),
)

private val Lectura = FontFamily.Serif
private val Titular = FontFamily.SansSerif

private val RutaTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = Titular, fontWeight = FontWeight.Black),
        headlineLarge = headlineLarge.copy(fontFamily = Titular, fontWeight = FontWeight.ExtraBold),
        headlineMedium = headlineMedium.copy(fontFamily = Titular, fontWeight = FontWeight.ExtraBold),
        headlineSmall = headlineSmall.copy(fontFamily = Titular, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = Titular, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = Titular, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontFamily = Lectura, fontSize = 17.sp, lineHeight = 26.sp),
        bodyMedium = TextStyle(fontFamily = Lectura, fontSize = 15.sp, lineHeight = 22.sp),
    )
}

/** Estilo para citas (las frases entre comillas de los workbooks). */
val EstiloCita = TextStyle(
    fontFamily = Lectura,
    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
    fontSize = 19.sp,
    lineHeight = 27.sp,
)

@Composable
fun RutaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) Oscuro else Claro,
        typography = RutaTypography,
        content = content,
    )
}

/** Convierte los colores ARGB del dominio (Long) a Color de Compose. */
fun Long.asColor(): Color = Color(this)
