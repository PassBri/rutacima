package com.rutaalacima.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Publicar: un «+» dentro de una forma suave de Material 3 (squircle), igual que la web. */
val IconoPublicar: ImageVector by lazy {
    ImageVector.Builder(name = "Publicar", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = PathParser().parsePathString("M9.5 3h5a6.5 6.5 0 0 1 6.5 6.5v5a6.5 6.5 0 0 1-6.5 6.5h-5A6.5 6.5 0 0 1 3 14.5v-5A6.5 6.5 0 0 1 9.5 3zM9.5 4.7A4.8 4.8 0 0 0 4.7 9.5v5a4.8 4.8 0 0 0 4.8 4.8h5a4.8 4.8 0 0 0 4.8-4.8v-5a4.8 4.8 0 0 0-4.8-4.8zM11 9a1 1 0 0 1 2 0v2h2a1 1 0 0 1 0 2h-2v2a1 1 0 0 1-2 0v-2H9a1 1 0 0 1 0-2h2z").toNodes(),
            fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd,
        ).build()
}
