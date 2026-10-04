package com.rutaalacima.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

/** Estilos de papel de la app. Se eligen en Ajustes. */
enum class EstiloPapel { BLANCO, ANTIGUO, PASTEL }

/** Estilo actual (se carga en MainActivity y se cambia en vivo desde Ajustes). */
object EstiloActual {
    var estilo by androidx.compose.runtime.mutableStateOf(EstiloPapel.BLANCO)
    private const val PREFS = "ajustes_estilo"

    fun cargar(c: android.content.Context) {
        estilo = runCatching { EstiloPapel.valueOf(c.getSharedPreferences(PREFS, 0).getString("estilo", null) ?: "") }
            .getOrDefault(EstiloPapel.BLANCO)
    }

    fun cambiar(c: android.content.Context, e: EstiloPapel) {
        estilo = e
        c.getSharedPreferences(PREFS, 0).edit().putString("estilo", e.name).apply()
    }
}

/**
 * Papel blanco (por defecto): hojas blancas sobre un fondo blanco cálido; el color de la app
 * vive en el sello, los botones y las sombras (burdeos y dorado). Limpio y de alto contraste.
 */
private val PapelBlanco = lightColorScheme(
    primary = Color(0xFF6B2A1A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF6E3DD),
    onPrimaryContainer = Color(0xFF3A1A10),
    secondary = Color(0xFFB8862F),
    onSecondary = Color(0xFF2E1F0E),
    secondaryContainer = Color(0xFFF6EBD3),
    onSecondaryContainer = Color(0xFF3A2A1C),
    tertiary = Color(0xFF8E4A2E),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F3EE),
    onBackground = Color(0xFF2E211B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2E211B),
    surfaceVariant = Color(0xFFEFE7DE),
    onSurfaceVariant = Color(0xFF6B5B52),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFBF8F4),
    surfaceContainerHigh = Color(0xFFF3EDE6),
    surfaceContainerHighest = Color(0xFFEDE5DC),
    outline = Color(0xFFCDBFB3),
    outlineVariant = Color(0xFFE8DED4),
)

/** Papel antiguo: pergamino, tinta sepia, lacre y ocre. Contraste de texto ≥ 4.5:1. */
private val PapelAntiguo = lightColorScheme(
    primary = Color(0xFF6B2A1A),
    onPrimary = Color(0xFFFBF3E3),
    primaryContainer = Color(0xFFEFD9C8),
    onPrimaryContainer = Color(0xFF3A1A10),
    secondary = Color(0xFFB8862F),
    onSecondary = Color(0xFF2E1F0E),
    secondaryContainer = Color(0xFFF0E0B8),
    onSecondaryContainer = Color(0xFF3A2A1C),
    tertiary = Color(0xFF8E4A2E),
    onTertiary = Color(0xFFFBF3E3),
    background = Color(0xFFEADCBE),
    onBackground = Color(0xFF3A2A1C),
    surface = Color(0xFFF5EAD2),
    onSurface = Color(0xFF3A2A1C),
    surfaceVariant = Color(0xFFE2D1AC),
    onSurfaceVariant = Color(0xFF6E5A45),
    surfaceContainerLowest = Color(0xFFFBF3E3),
    surfaceContainerLow = Color(0xFFF5EAD2),
    surfaceContainer = Color(0xFFF0E3C6),
    surfaceContainerHigh = Color(0xFFE8D9B8),
    surfaceContainerHighest = Color(0xFFE2D1AC),
    outline = Color(0xFFB9A27A),
    outlineVariant = Color(0xFFD7C49D),
)

/** Pastel marrón: café con leche, moca y caramelo. */
private val PastelMarron = lightColorScheme(
    primary = Color(0xFF7A5544),
    onPrimary = Color(0xFFFFF8F2),
    primaryContainer = Color(0xFFEBD8CC),
    onPrimaryContainer = Color(0xFF3B2A22),
    secondary = Color(0xFFC98B4E),
    onSecondary = Color(0xFF2E1F12),
    secondaryContainer = Color(0xFFF3E1CC),
    onSecondaryContainer = Color(0xFF3B2A22),
    tertiary = Color(0xFFB07D7A),
    onTertiary = Color(0xFF2E1F1E),
    background = Color(0xFFEFE4D8),
    onBackground = Color(0xFF4A3A33),
    surface = Color(0xFFFAF3EC),
    onSurface = Color(0xFF4A3A33),
    surfaceVariant = Color(0xFFE8DCCF),
    onSurfaceVariant = Color(0xFF6F5E55),
    surfaceContainerLowest = Color(0xFFFFFBF7),
    surfaceContainerLow = Color(0xFFFAF3EC),
    surfaceContainer = Color(0xFFF5ECE3),
    surfaceContainerHigh = Color(0xFFEDE1D5),
    surfaceContainerHighest = Color(0xFFE8DCCF),
    outline = Color(0xFFC2AE9F),
    outlineVariant = Color(0xFFDCCBBB),
)

/** Modo oscuro: noche cálida con oro y crema. */
private val Oscuro = darkColorScheme(
    primary = Marca.OroClaro,
    onPrimary = Marca.TintaCafe,
    primaryContainer = Marca.BurdeosClaro,
    onPrimaryContainer = Marca.OroPalido,
    secondary = Marca.Oro,
    onSecondary = Marca.TintaCafe,
    tertiary = Marca.Kraft,
    onTertiary = Marca.TintaCafe,
    background = Marca.Noche,
    onBackground = Marca.OroPalido,
    surface = Marca.NocheSuperficie,
    onSurface = Marca.OroPalido,
    surfaceVariant = Color(0xFF3A2A24),
    onSurfaceVariant = Color(0xFFD8C6B0),
    surfaceContainerLowest = Color(0xFF160E0C),
    surfaceContainerLow = Color(0xFF241915),
    surfaceContainer = Marca.NocheSuperficie,
    surfaceContainerHigh = Color(0xFF33251F),
    surfaceContainerHighest = Color(0xFF3A2A24),
    outline = Color(0xFF8C7765),
    outlineVariant = Color(0xFF4E3D34),
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
        colorScheme = when {
            darkTheme -> Oscuro
            EstiloActual.estilo == EstiloPapel.PASTEL -> PastelMarron
            EstiloActual.estilo == EstiloPapel.ANTIGUO -> PapelAntiguo
            else -> PapelBlanco
        },
        typography = RutaTypography,
        content = content,
    )
}

/** Convierte los colores ARGB del dominio (Long) a Color de Compose. */
fun Long.asColor(): Color = Color(this)
