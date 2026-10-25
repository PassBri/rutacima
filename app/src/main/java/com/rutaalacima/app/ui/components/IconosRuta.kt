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

    // Impulso (me gusta): un banderín clavado en la cima
    val Banderin: ImageVector by lazy { icono("Banderin", "M7.2 2.5h1.6v15.8H7.2z M8.8 3.2l9.7 3.4-9.7 3.5z M2.8 21.5c1.4-2.4 3.4-3.7 5.2-3.7s3.8 1.3 5.2 3.7z", true, PathFillType.NonZero) }
    val BanderinContorno: ImageVector by lazy { icono("BanderinContorno", "M8 18.3V2.8 M8.8 3.3l9 3.3-9 3.3z M3 21.2c1.4-2.2 3.2-3.3 5-3.3s3.6 1.1 5 3.3z", false) }
    // Comunidad: la fogata, limpia (llama con su núcleo y dos leños cruzados)
    val Fogata: ImageVector by lazy { icono("Fogata", "M12 3c1 3 4.5 4.6 4.5 8.4A4.5 4.5 0 0 1 12 16a4.5 4.5 0 0 1-4.5-4.6c0-2 1-3.3 2-4.2.3 1.5 1 2.4 2 2.6C11 7.6 11 5.3 12 3z M12 10.2c-.9 1-1.5 1.8-1.5 2.8 0 .9.7 1.6 1.5 1.6s1.5-.7 1.5-1.6c0-1-.6-1.8-1.5-2.8z M19.70 18.00L19.95 17.88L20.20 17.61L20.32 17.35L20.34 16.99L20.23 16.63L20.06 16.41L19.74 16.21L19.47 16.15L12.00 17.78L4.81 16.17L4.44 16.16L4.01 16.35L3.70 16.80L3.68 17.35L3.98 17.82L4.39 18.03L7.63 18.75L4.22 19.53L3.80 19.89L3.66 20.24L3.66 20.51L3.77 20.87L4.01 21.15L4.35 21.32L4.71 21.34L12.00 19.72L19.19 21.33L19.56 21.34L20.06 21.09L20.33 20.61L20.29 20.06L20.02 19.68L19.61 19.47L16.37 18.75Z", true) }
    val FogataContorno: ImageVector by lazy { icono("FogataContorno", "M12 3c1 3 4.5 4.6 4.5 8.4A4.5 4.5 0 0 1 12 16a4.5 4.5 0 0 1-4.5-4.6c0-2 1-3.3 2-4.2.3 1.5 1 2.4 2 2.6C11 7.6 11 5.3 12 3z M4.6 20.4L19.4 17.1M19.4 20.4L4.6 17.1", false) }
    // Aprende: un libro abierto con la cumbre encima (las guías, los libros y los audiolibros del método)
    val LibroCumbre: ImageVector by lazy { icono("LibroCumbre", LIBRO, true) }
    val LibroCumbreContorno: ImageVector by lazy { icono("LibroCumbreContorno", LIBRO_CONTORNO, true) }
    // Cumbre y Brújula (y Plan Cumbre): la montaña con la bandera en la cima
    val Cima: ImageVector by lazy { icono("Cima", CIMA, true) }
    // Coach: un brote con una chispa (la naturaleza que crece y el aprendizaje guiado)
    // Coach: un brote dentro de un diálogo (la conversación que te hace crecer)
    val Coach: ImageVector by lazy { icono("Coach", "M4 4.5h16a1.5 1.5 0 0 1 1.5 1.5v10a1.5 1.5 0 0 1-1.5 1.5H10l-4.5 3.5v-3.5H4A1.5 1.5 0 0 1 2.5 16V6A1.5 1.5 0 0 1 4 4.5z M11.3 10.4h1.4V15h-1.4z M11.9 11.6c-.2-2-1.6-3.3-3.7-3.3 0 2.1 1.4 3.3 3.7 3.3z M12.1 10.5c0-2.1 1.4-3.4 3.7-3.4 0 2.1-1.4 3.4-3.7 3.4z", true) }
    // Perfil: account_circle de Material Symbols (la referencia de Material 3 para el perfil)
    val Perfil: ImageVector by lazy { icono("Perfil", PERFIL, true, PathFillType.NonZero) }
    val PerfilContorno: ImageVector by lazy { icono("PerfilContorno", PERFIL_CONTORNO, true, PathFillType.NonZero) }
    // Mochila: backpack de Material Symbols, con las herramientas del método
    val Morral: ImageVector by lazy { icono("Morral", MORRAL, true, PathFillType.NonZero) }
    val MorralContorno: ImageVector by lazy { icono("MorralContorno", MORRAL_CONTORNO, true, PathFillType.NonZero) }
    // Mis ejes: un radar de seis ejes
    val Ejes: ImageVector by lazy { icono("Ejes", EJES, true) }
}
