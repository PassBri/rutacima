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
    private const val EJES = "M12.00 2.00L20.66 7.00L20.66 17.00L12.00 22.00L3.34 17.00L3.34 7.00ZM4.81 7.85L4.81 16.15L12.00 20.30L19.19 16.15L19.19 7.85L12.00 3.70ZM12.00 5.40L15.81 9.80L17.02 14.90L12.00 15.60L6.63 15.10L7.84 9.60Z"

    private fun icono(nombre: String, d: String, relleno: Boolean) = ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
        val nodos = PathParser().parsePathString(d).toNodes()
        if (relleno) addPath(nodos, pathFillType = PathFillType.EvenOdd, fill = SolidColor(Color.Black))
        else addPath(nodos, pathFillType = PathFillType.EvenOdd, stroke = SolidColor(Color.Black), strokeLineWidth = 1.4f, strokeLineJoin = StrokeJoin.Round)
    }.build()

    val Fogata: ImageVector by lazy { icono("Fogata", FOGATA, true) }
    val FogataContorno: ImageVector by lazy { icono("FogataContorno", FOGATA, false) }
    // Aprende: un libro abierto con la cumbre encima (las guías, los libros y los audiolibros del método)
    val LibroCumbre: ImageVector by lazy { icono("LibroCumbre", LIBRO, true) }
    val LibroCumbreContorno: ImageVector by lazy { icono("LibroCumbreContorno", LIBRO_CONTORNO, true) }
    // Cumbre y Brújula (y Plan Cumbre): la montaña con la bandera en la cima
    val Cima: ImageVector by lazy { icono("Cima", CIMA, true) }
    // Mis ejes: un radar de seis ejes
    val Ejes: ImageVector by lazy { icono("Ejes", EJES, true) }
}
