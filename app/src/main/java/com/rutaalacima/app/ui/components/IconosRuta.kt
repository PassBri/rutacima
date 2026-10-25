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
 * Comunidad es una fogata, porque alrededor del fuego se reúne la gente; Aprende es alguien haciendo fuego.
 * Relleno para la pestaña activa y contorno para las demás, como los íconos de Material.
 */
object IconosRuta {
    private const val FOGATA = "M12 1.8c.6 2.6 4.4 4.3 4.4 8.3 0 2.7-2 4.7-4.4 4.7S7.6 12.8 7.6 10.1c0-1.9 1-3.2 1.9-4.1.2 1.5 1 2.5 2 2.7-.5-2.2-.4-4.6.5-6.9zm0 7.4c-.9 1-1.6 1.9-1.6 3 0 1 .7 1.8 1.6 1.8s1.6-.8 1.6-1.8c0-1.1-.7-2-1.6-3zM3.4 17.4c-.3-.6 0-1.3.6-1.5l.2-.1 15.8 4.3c.6.2.9.8.8 1.4-.2.6-.8 1-1.4.8L4.1 18.1a1 1 0 0 1-.7-.7zM20.6 17.4c.3-.6 0-1.3-.6-1.5l-.2-.1L4 20.1c-.6.2-.9.8-.8 1.4.2.6.8 1 1.4.8l15.3-4.2c.3-.1.6-.4.7-.7z"
    private const val HACER_FUEGO = "M6.6 2.4a2.1 2.1 0 1 1 0 4.2 2.1 2.1 0 0 1 0-4.2zM4.4 8.6c0-.8.7-1.4 1.5-1.4h1.2c.5 0 1 .3 1.2.7l2.1 3.4 2.6-.5.3 1.7-3.6.8-1.5-2.1v2.2l3.1 1.8c.4.2.6.6.6 1V21H10v-4.1l-3.6-2c-1.2-.7-2-1.9-2-3.3zM12.3 12.3l.8-.9 3.5 2.9-.8.9zM18.6 10.2c.4 1.7 2.9 2.8 2.9 5.4 0 1.7-1.3 3-2.9 3s-2.9-1.3-2.9-3c0-1.2.6-2.1 1.3-2.6.1.9.6 1.6 1.3 1.8-.3-1.5-.2-3 .3-4.6zM14.4 19.4c-.2-.4 0-.9.4-1l.1-.1 6.8 1.9c.4.1.6.5.5.9-.1.4-.5.6-.9.5l-6.4-1.8a.7.7 0 0 1-.5-.4zM22.3 19.4c.2-.4 0-.9-.4-1l-.1-.1-6.8 1.9c-.4.1-.6.5-.5.9.1.4.5.6.9.5l6.4-1.8c.2-.1.4-.2.5-.4z"

    private fun icono(nombre: String, d: String, relleno: Boolean) = ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
        val nodos = PathParser().parsePathString(d).toNodes()
        if (relleno) addPath(nodos, pathFillType = PathFillType.EvenOdd, fill = SolidColor(Color.Black))
        else addPath(nodos, pathFillType = PathFillType.EvenOdd, stroke = SolidColor(Color.Black), strokeLineWidth = 1.4f, strokeLineJoin = StrokeJoin.Round)
    }.build()

    val Fogata: ImageVector by lazy { icono("Fogata", FOGATA, true) }
    val FogataContorno: ImageVector by lazy { icono("FogataContorno", FOGATA, false) }
    val HacerFuego: ImageVector by lazy { icono("HacerFuego", HACER_FUEGO, true) }
    // La figura es pequeña: en contorno se pierde, así que la pestaña inactiva usa la silueta (solo cambia el color)
    val HacerFuegoContorno: ImageVector by lazy { icono("HacerFuegoContorno", HACER_FUEGO, true) }
}
