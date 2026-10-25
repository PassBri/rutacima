package com.rutaalacima.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Íconos propios de SuRoute (los mismos trazos que la web, web/js/01-base.js):
 * Comunidad es una fogata, porque alrededor del fuego se reúne la gente; Aprende es un libro abierto con la cumbre.
 * Relleno para la pestaña activa y contorno para las demás, como los íconos de Material.
 */
object IconosRuta {
    private const val FOGATA = "M12 1.8c.6 2.6 4.4 4.3 4.4 8.3 0 2.7-2 4.7-4.4 4.7S7.6 12.8 7.6 10.1c0-1.9 1-3.2 1.9-4.1.2 1.5 1 2.5 2 2.7-.5-2.2-.4-4.6.5-6.9zm0 7.4c-.9 1-1.6 1.9-1.6 3 0 1 .7 1.8 1.6 1.8s1.6-.8 1.6-1.8c0-1.1-.7-2-1.6-3zM3.4 17.4c-.3-.6 0-1.3.6-1.5l.2-.1 15.8 4.3c.6.2.9.8.8 1.4-.2.6-.8 1-1.4.8L4.1 18.1a1 1 0 0 1-.7-.7zM20.6 17.4c.3-.6 0-1.3-.6-1.5l-.2-.1L4 20.1c-.6.2-.9.8-.8 1.4.2.6.8 1 1.4.8l15.3-4.2c.3-.1.6-.4.7-.7z"
    private const val LIBRO = "M2 8.2c3.1-1 6.3-.6 9.2 1.3V21c-2.9-1.7-6.1-2-9.2-1zM22 8.2c-3.1-1-6.3-.6-9.2 1.3V21c2.9-1.7 6.1-2 9.2-1zM8.4 7.1 12 1.6l3.6 5.5c-1.2-.3-2.4-.2-3.6.5-1.2-.7-2.4-.8-3.6-.5z"
    private const val LIBRO_CONTORNO = "M2 8.2c3.1-1 6.3-.6 9.2 1.3V21c-2.9-1.7-6.1-2-9.2-1zM3.6 9.5c2.2-.4 4.4-.1 6 .9v7.9c-1.9-.8-4-1.1-6-.8zM22 8.2c-3.1-1-6.3-.6-9.2 1.3V21c2.9-1.7 6.1-2 9.2-1zM20.4 9.5c-2.2-.4-4.4-.1-6 .9v7.9c1.9-.8 4-1.1 6-.8zM8.4 7.1 12 1.6l3.6 5.5c-1.2-.3-2.4-.2-3.6.5-1.2-.7-2.4-.8-3.6-.5z"
    private const val CIMA = "M1 21 9.6 8.6l3.2 4.4 2.3-3.1L23 21zM9 2h1.3v7.2H9zM10.3 2.2l5.4 1.9-5.4 1.9z"
    private const val BROTE = "M11.2 12.6h1.6V20h-1.6zM11.5 13.2C11.4 9.6 8.6 7 4.6 7c0 3.8 2.9 6.2 6.9 6.2zM12.5 11.6c0-4 3-7 7-7 0 4-3 7-7 7zM6 19.6c0-.6.4-1 1-1h10c.6 0 1 .4 1 1v1.4H6zM5.6 1.4l.6 1.6 1.6.6-1.6.6-.6 1.6L5 4.2l-1.6-.6L5 3z"
    private const val PERFIL = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 4c1.93 0 3.5 1.57 3.5 3.5S13.93 13 12 13s-3.5-1.57-3.5-3.5S10.07 6 12 6zm0 14c-2.03 0-4.43-.82-6.14-2.88C7.55 15.8 9.68 15 12 15s4.45.8 6.14 2.12C16.43 19.18 14.03 20 12 20z"
    private const val PERFIL_CONTORNO = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zM7.35 18.5C8.66 17.56 10.26 17 12 17s3.34.56 4.65 1.5c-1.31.94-2.91 1.5-4.65 1.5s-3.34-.56-4.65-1.5zm10.79-1.38C16.45 15.8 14.32 15 12 15s-4.45.8-6.14 2.12C4.7 15.73 4 13.95 4 12c0-4.42 3.58-8 8-8s8 3.58 8 8c0 1.95-.7 3.73-1.86 5.12zM12 6c-1.93 0-3.5 1.57-3.5 3.5S10.07 13 12 13s3.5-1.57 3.5-3.5S13.93 6 12 6zm0 5c-.83 0-1.5-.67-1.5-1.5S11.17 8 12 8s1.5.67 1.5 1.5S12.83 11 12 11z"
    private const val MORRAL = "M20 8v12c0 1.1-.9 2-2 2H6c-1.1 0-2-.9-2-2V8c0-1.86 1.28-3.41 3-3.86V2h3v2h4V2h3v2.14c1.72.45 3 2 3 3.86zM6 12v2h10v2h2v-4H6z"
    private const val MORRAL_CONTORNO = "M17 4.14V2h-3v2h-4V2H7v2.14c-1.72.45-3 2-3 3.86v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8c0-1.86-1.28-3.41-3-3.86zM18 20H6V8c0-1.1.9-2 2-2h8c1.1 0 2 .9 2 2v12zm-1.5-8v4h-2v-2h-7v-2h9z"
    private const val EJES = "M12.00 2.00L20.66 7.00L20.66 17.00L12.00 22.00L3.34 17.00L3.34 7.00ZM4.81 7.85L4.81 16.15L12.00 20.30L19.19 16.15L19.19 7.85L12.00 3.70ZM12.00 5.40L15.81 9.80L17.02 14.90L12.00 15.60L6.63 15.10L7.84 9.60Z"

    private fun icono(nombre: String, d: String, relleno: Boolean, tipo: PathFillType = PathFillType.EvenOdd) = ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
        val nodos = PathParser().parsePathString(d).toNodes()
        if (relleno) addPath(nodos, pathFillType = tipo, fill = SolidColor(Color.Black))
        else addPath(nodos, pathFillType = PathFillType.EvenOdd, stroke = SolidColor(Color.Black), strokeLineWidth = 1.4f, strokeLineJoin = StrokeJoin.Round)
    }.build()

    val Fogata: ImageVector by lazy { icono("Fogata", FOGATA, true) }
    val FogataContorno: ImageVector by lazy { icono("FogataContorno", FOGATA, false) }
    // Aprende: un libro abierto con la cumbre encima (las guías, los libros y los audiolibros del método)
    val LibroCumbre: ImageVector by lazy { icono("LibroCumbre", LIBRO, true) }
    val LibroCumbreContorno: ImageVector by lazy { icono("LibroCumbreContorno", LIBRO_CONTORNO, true) }
    // Cumbre y Brújula (y Plan Cumbre): la montaña con la bandera en la cima
    val Cima: ImageVector by lazy { icono("Cima", CIMA, true) }
    // Coach: un brote con una chispa (la naturaleza que crece y el aprendizaje guiado)
    val Coach: ImageVector by lazy { icono("Coach", BROTE, true, PathFillType.NonZero) }
    // Perfil: account_circle de Material Symbols (la referencia de Material 3 para el perfil)
    val Perfil: ImageVector by lazy { icono("Perfil", PERFIL, true, PathFillType.NonZero) }
    val PerfilContorno: ImageVector by lazy { icono("PerfilContorno", PERFIL_CONTORNO, true, PathFillType.NonZero) }
    // Mochila: backpack de Material Symbols, con las herramientas del método
    val Morral: ImageVector by lazy { icono("Morral", MORRAL, true, PathFillType.NonZero) }
    val MorralContorno: ImageVector by lazy { icono("MorralContorno", MORRAL_CONTORNO, true, PathFillType.NonZero) }
    // Mis ejes: un radar de seis ejes
    val Ejes: ImageVector by lazy { icono("Ejes", EJES, true) }
}
