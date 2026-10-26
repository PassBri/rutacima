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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Identidad de SuRoute (igual que web/estilos.css): burdeos del símbolo como acento único,
 * oro viejo solo para momentos de cima, papel blanco y tinta grafito.
 */
object Marca {
    val Burdeos = Color(0xFF6B2A1A)
    val BurdeosClaro = Color(0xFFF3E3DC)
    val Oro = Color(0xFF86683A)
    val OroClaro = Color(0xFFC9AE84)
    val OroPalido = Color(0xFFF6EAD3)
    val Kraft = Color(0xFFD69A6B)
    val Crema = Color(0xFFFBF7EE)
    val TintaCafe = Color(0xFF1D2427)
    val Noche = Color(0xFF0A0E12)
    val NocheSuperficie = Color(0xFF12181D)
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
 * Altitud (por defecto): el papel blanco a 4.000 metros. Niebla fría de fondo, hojas blancas con
 * líneas finas de instrumento y tinta de grafito; el color vive solo en el lacre burdeos y el oro.
 */
private val PapelBlanco = lightColorScheme(
    primary = Color(0xFF6B2A1A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF6E3DD),
    onPrimaryContainer = Color(0xFF3A1A10),
    secondary = Color(0xFF86683A),
    onSecondary = Color(0xFF2E1F0E),
    secondaryContainer = Color(0xFFF6EBD3),
    onSecondaryContainer = Color(0xFF3A2A1C),
    tertiary = Color(0xFF3E5868),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFEEF1F2),
    onBackground = Color(0xFF1D2427),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D2427),
    surfaceVariant = Color(0xFFE4E9EB),
    onSurfaceVariant = Color(0xFF5A666B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF7F9F9),
    surfaceContainerHigh = Color(0xFFEFF3F4),
    surfaceContainerHighest = Color(0xFFE6EBED),
    outline = Color(0xFFB9C3C7),
    outlineVariant = Color(0xFFDCE2E4),
)

/** Papel antiguo: pergamino, tinta sepia, lacre y ocre. Contraste de texto ≥ 4.5:1. */
private val PapelAntiguo = lightColorScheme(
    primary = Color(0xFF6B2A1A),
    onPrimary = Color(0xFFFBF3E3),
    primaryContainer = Color(0xFFEFD9C8),
    onPrimaryContainer = Color(0xFF3A1A10),
    secondary = Color(0xFF86683A),
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

/** Modo oscuro: la noche en altura. Grafito azulado, líneas finas y el oro y el lacre como únicos acentos. */
private val Oscuro = darkColorScheme(
    primary = Color(0xFFF0B3A1),
    onPrimary = Color(0xFF2B1209),
    primaryContainer = Color(0xFF5A2418),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFC9AE84),
    onSecondary = Color(0xFF2B2108),
    secondaryContainer = Color(0xFF2E2817),
    onSecondaryContainer = Color(0xFFF6E3BC),
    tertiary = Color(0xFF9EC1D6),
    onTertiary = Color(0xFF0B1D28),
    background = Color(0xFF0A0E12),
    onBackground = Color(0xFFE8EDEF),
    surface = Color(0xFF12181D),
    onSurface = Color(0xFFE8EDEF),
    surfaceVariant = Color(0xFF1F282F),
    onSurfaceVariant = Color(0xFF98A6AD),
    surfaceContainerLowest = Color(0xFF080B0E),
    surfaceContainerLow = Color(0xFF12181D),
    surfaceContainer = Color(0xFF151C21),
    surfaceContainerHigh = Color(0xFF1B2329),
    surfaceContainerHighest = Color(0xFF222C33),
    outline = Color(0xFF5B6A73),
    outlineVariant = Color(0xFF243039),
)

private val Lectura = FontFamily.Serif
private val Titular = FontFamily.SansSerif

private val RutaTypography = Typography().run {
    copy(
        // Altitud: títulos ligeros y con aire (como los instrumentos de una estación en las nubes);
        // la serif queda para los momentos humanos: citas, la carta y la cumbre.
        displaySmall = displaySmall.copy(fontFamily = Titular, fontWeight = FontWeight.Light),
        headlineLarge = headlineLarge.copy(fontFamily = Titular, fontWeight = FontWeight.Light),
        headlineMedium = headlineMedium.copy(fontFamily = Titular, fontWeight = FontWeight.Light),
        headlineSmall = headlineSmall.copy(fontFamily = Titular, fontWeight = FontWeight.Normal),
        titleLarge = titleLarge.copy(fontFamily = Titular, fontWeight = FontWeight.Normal),
        titleMedium = titleMedium.copy(fontFamily = Titular, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontFamily = Titular, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
        bodyMedium = TextStyle(fontFamily = Titular, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    )
}

/** Estilo para citas (las frases entre comillas de los workbooks). */
val EstiloCita = TextStyle(
    fontFamily = Lectura,
    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
    fontSize = 19.sp,
    lineHeight = 27.sp,
)

/** La noche en altura, para las vistas inmersivas (Cimas, Mirar el cielo) aunque el teléfono esté en claro. */
@Composable
fun TemaNoche(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Oscuro, typography = RutaTypography, shapes = FormasRuta, content = content)
}

/**
 * Avisos de Material 3: un Snackbar en la raíz de la app (en vez de Toast). Cualquier pantalla puede
 * llamar a AvisoGlobal.mostrar("…"); el aviso anterior se reemplaza.
 */
object AvisoGlobal {
    val host = androidx.compose.material3.SnackbarHostState()
    private val alcance = kotlinx.coroutines.MainScope()
    fun mostrar(texto: String) {
        alcance.launch {
            host.currentSnackbarData?.dismiss()
            host.showSnackbar(texto, withDismissAction = true, duration = androidx.compose.material3.SnackbarDuration.Long)
        }
    }
}

/** Escala de forma de Material 3 (4 · 8 · 12 · 16 · 28 dp): una sola, usada por todos los componentes. */
val FormasRuta = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

/** Si una pantalla ocupa todo (como Cimas): la raíz esconde la barra superior y oscurece la inferior. */
object Inmersivo {
    var activo by androidx.compose.runtime.mutableStateOf(false)
}

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
        shapes = FormasRuta,
        content = content,
    )
}

/** Convierte los colores ARGB del dominio (Long) a Color de Compose. */
fun Long.asColor(): Color = Color(this)
