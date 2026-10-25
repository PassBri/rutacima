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

    // Hoy: huellas · Mis frases: piolet · Mochila (herramientas): mosquetón
    val Huellas: ImageVector by lazy { icono("Huellas", "M8.5 2.4c2.3 0 3.8 2.3 3.8 5.6s-1.5 4.8-3.8 4.8S4.7 11.3 4.7 8s1.5-5.6 3.8-5.6zM8.5 13.8c1.8 0 2.9 1.1 2.9 2.7s-1.1 2.6-2.9 2.6-2.9-1-2.9-2.6 1.1-2.7 2.9-2.7zM15.5 5.4c2.3 0 3.8 2.3 3.8 5.6s-1.5 4.8-3.8 4.8-3.8-1.5-3.8-4.8 1.5-5.6 3.8-5.6zM15.5 16.8c1.8 0 2.9 1.1 2.9 2.7s-1.1 2.6-2.9 2.6-2.9-1-2.9-2.6 1.1-2.7 2.9-2.7z", true, PathFillType.NonZero) }
    val HuellasContorno: ImageVector by lazy { icono("HuellasContorno", "M8.5 3c2 0 3.2 2.1 3.2 5s-1.2 4.2-3.2 4.2S5.3 10.9 5.3 8 6.5 3 8.5 3zM8.5 14.4c1.4 0 2.3.9 2.3 2.1s-.9 2-2.3 2-2.3-.8-2.3-2 .9-2.1 2.3-2.1zM15.5 6c2 0 3.2 2.1 3.2 5s-1.2 4.2-3.2 4.2-3.2-1.3-3.2-4.2S13.5 6 15.5 6zM15.5 17.4c1.4 0 2.3.9 2.3 2.1s-.9 2-2.3 2-2.3-.8-2.3-2 .9-2.1 2.3-2.1z", false) }
    val Piolet: ImageVector by lazy { icono("Piolet", "M7.2 21.6l7.3-15.9 1.6.8-7.3 15.9zM9.9 4.5c3-2 7.2-1.7 10 1.1l.6.6-3.4.4-.6-.6c-1.7-1.3-4-1.5-5.8-.5zM10.3 6.1L7.6 5.1l.5-1.5 2.9 1z", true, PathFillType.NonZero) }
    val Mosqueton: ImageVector by lazy { icono("Mosqueton", "M9.05 13.99L9.19 14.19L9.47 14.38L9.71 14.44L10.04 14.42L10.33 14.26L10.50 14.08L10.61 13.86L10.65 13.53L8.19 7.28L8.06 7.07L7.86 6.88L8.08 6.56L8.22 5.67L8.58 4.95L8.95 4.58L9.42 4.31L10.23 4.15L15.02 4.18L15.44 4.26L16.22 4.59L16.58 4.82L17.18 5.42L17.60 6.16L17.82 6.98L17.85 16.37L17.74 17.24L17.60 17.64L17.18 18.38L16.90 18.70L16.22 19.21L15.44 19.54L15.02 19.62L9.63 19.65L8.82 19.49L8.15 19.05L7.71 18.38L7.55 17.57L7.51 14.92L7.39 14.67L7.20 14.47L6.88 14.29L6.60 14.25L6.32 14.29L6.00 14.47L5.81 14.67L5.67 15.01L5.69 18.18L5.80 18.68L6.15 19.52L6.51 20.05L6.85 20.44L7.25 20.77L8.03 21.22L8.64 21.43L9.15 21.52L14.60 21.55L15.87 21.39L16.51 21.18L17.23 20.83L17.78 20.45L18.65 19.58L19.09 18.92L19.38 18.31L19.59 17.67L19.71 17.01L19.75 7.34L19.59 6.13L19.33 5.37L19.03 4.77L18.65 4.22L17.78 3.35L17.23 2.97L16.63 2.67L15.99 2.44L15.33 2.30L14.60 2.25L10.14 2.25L9.12 2.40L8.28 2.75L7.75 3.11L7.11 3.75L6.75 4.28L6.40 5.12L6.28 5.75L6.27 6.41L6.42 6.74L6.76 7.04L6.57 7.42L6.58 7.84Z", true) }
    // Aprende: el mapa plegado con la ruta (aprender es leer el mapa)
    val MapaRuta: ImageVector by lazy { icono("MapaRuta", "M19.48 7.55L19.20 7.02L18.73 6.63L18.15 6.46L17.55 6.52L17.02 6.80L16.63 7.27L16.46 7.85L16.52 8.45L16.80 8.98L17.27 9.37L17.85 9.54L18.45 9.48L18.98 9.20L19.37 8.73L19.54 8.15ZM2.09 20.77L2.26 21.09L2.45 21.28L2.79 21.43L3.15 21.44L8.54 19.50L15.24 21.41L15.53 21.45L15.82 21.39L21.32 19.39L21.65 19.19L21.87 18.88L21.95 18.50L21.95 3.50L21.88 3.14L21.68 2.84L21.30 2.60L21.03 2.55L20.76 2.58L15.46 4.50L8.76 2.59L8.37 2.56L2.68 4.61L2.35 4.81L2.13 5.12L2.05 5.50L2.05 20.50ZM16.73 10.26L16.51 9.85L16.11 9.54L16.11 6.29L20.05 4.86L20.05 17.83L16.11 19.27L16.11 11.66L16.43 11.43L16.69 11.05L16.77 10.72ZM14.89 9.66L14.62 9.95L14.51 10.15L14.42 10.60L14.47 10.94L14.56 11.16L14.89 11.54L14.89 11.83L14.56 11.56L14.23 11.44L13.88 11.43L13.55 11.51L13.25 11.69L13.02 11.95L12.87 12.26L12.82 12.56L12.45 12.31L12.12 12.23L11.77 12.24L11.35 12.42L11.09 12.65L10.96 12.84L10.84 13.17L10.82 13.40L10.91 13.85L11.17 14.23L11.55 14.49L12.00 14.58L12.45 14.49L12.83 14.23L13.09 13.85L13.18 13.44L13.55 13.69L14.00 13.78L14.45 13.69L14.65 13.58L14.89 13.37L14.89 19.34L9.11 17.69L9.11 13.25L9.35 13.39L9.68 13.47L10.03 13.46L10.36 13.34L10.55 13.21L10.78 12.95L10.93 12.64L10.98 12.30L10.93 11.96L10.84 11.74L10.63 11.47L10.36 11.26L10.03 11.14L9.68 11.13L9.35 11.21L9.11 11.35L9.11 4.66L14.89 6.31ZM3.95 6.17L7.89 4.73L7.89 12.26L7.37 12.24L7.15 12.31L6.85 12.49L6.56 12.84L6.44 13.17L6.42 13.40L6.51 13.85L6.62 14.05L6.85 14.31L7.15 14.49L7.37 14.56L7.89 14.54L7.89 17.71L3.95 19.14ZM6.69 14.75L6.43 14.37L6.05 14.11L5.60 14.02L5.15 14.11L4.77 14.37L4.51 14.75L4.42 15.20L4.51 15.65L4.77 16.03L5.15 16.29L5.60 16.38L6.05 16.29L6.43 16.03L6.69 15.65L6.78 15.20Z", true) }
    val MapaRutaContorno: ImageVector by lazy { icono("MapaRutaContorno", "M19.48 7.55L19.20 7.02L18.73 6.63L18.15 6.46L17.55 6.52L17.02 6.80L16.63 7.27L16.46 7.85L16.52 8.45L16.80 8.98L17.27 9.37L17.85 9.54L18.45 9.48L18.98 9.20L19.37 8.73L19.54 8.15ZM2.28 20.71L2.41 20.97L2.63 21.15L2.90 21.24L3.19 21.23L8.53 19.29L15.29 21.22L15.53 21.25L15.76 21.20L21.26 19.20L21.51 19.05L21.69 18.80L21.75 18.50L21.75 3.50L21.70 3.22L21.54 2.98L21.37 2.85L21.17 2.77L20.81 2.77L15.47 4.71L8.71 2.78L8.40 2.76L2.74 4.80L2.49 4.95L2.31 5.20L2.25 5.50L2.25 20.50ZM16.49 10.33L16.32 10.01L15.98 9.75L15.98 6.12L20.25 4.57L20.25 17.97L15.98 19.53L15.98 11.45L16.26 11.26L16.46 10.96L16.53 10.60ZM15.01 9.88L14.74 10.24L14.67 10.60L14.78 11.04L15.02 11.33L15.02 19.58L8.98 17.86L8.98 12.74L9.21 13.02L9.44 13.16L9.80 13.23L10.07 13.19L10.32 13.07L10.57 12.82L10.69 12.57L10.73 12.30L10.69 12.03L10.57 11.78L10.32 11.53L10.07 11.41L9.80 11.37L9.44 11.44L9.21 11.58L8.98 11.86L8.98 4.42L15.02 6.14ZM3.75 6.03L8.02 4.47L8.02 12.57L7.78 12.49L7.51 12.47L7.08 12.63L6.78 12.96L6.67 13.40L6.78 13.84L7.08 14.17L7.51 14.33L7.78 14.31L8.02 14.23L8.02 17.88L3.75 19.43ZM14.89 12.33L14.72 12.01L14.44 11.78L14.09 11.67L13.73 11.71L13.41 11.88L13.18 12.16L13.07 12.51L13.11 12.87L13.28 13.19L13.56 13.42L13.91 13.53L14.27 13.49L14.59 13.32L14.82 13.04L14.93 12.69ZM12.86 13.04L12.66 12.74L12.36 12.54L12.00 12.47L11.64 12.54L11.34 12.74L11.14 13.04L11.07 13.40L11.14 13.76L11.34 14.06L11.64 14.26L12.00 14.33L12.36 14.26L12.66 14.06L12.86 13.76L12.93 13.40ZM6.46 14.84L6.26 14.54L5.96 14.34L5.60 14.27L5.24 14.34L4.94 14.54L4.74 14.84L4.67 15.20L4.74 15.56L4.94 15.86L5.24 16.06L5.60 16.13L5.96 16.06L6.26 15.86L6.46 15.56L6.53 15.20Z", true) }
    // Impulso (me gusta): un corazón con la montaña adentro (el nombre se conserva)
    val Banderin: ImageVector by lazy { icono("Impulso", "M12 21.3S2.2 15.4 2.2 8.9C2.2 5.6 4.6 3.2 7.6 3.2c1.8 0 3.4.8 4.4 2.2 1-1.4 2.6-2.2 4.4-2.2 3 0 5.4 2.4 5.4 5.7 0 6.5-9.8 12.4-9.8 12.4zM7 14.2l3.5-4.6 2 2.6 1.4-1.8 3.1 3.8z", true) }
    val BanderinContorno: ImageVector by lazy { icono("ImpulsoContorno", "M12 20.5S3 15 3 8.9C3 6 5.1 4 7.6 4c1.8 0 3.4 1 4.4 2.6C13 5 14.6 4 16.4 4 18.9 4 21 6 21 8.9 21 15 12 20.5 12 20.5z M7.5 13.5l3-4 2 2.6 1.4-1.8 2.6 3.2", false) }
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
