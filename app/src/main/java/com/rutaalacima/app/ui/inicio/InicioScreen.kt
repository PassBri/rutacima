package com.rutaalacima.app.ui.inicio

import com.rutaalacima.app.ui.theme.EstiloCita
import com.rutaalacima.app.ui.theme.hojaPapel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rutaalacima.app.R
import com.rutaalacima.app.ui.theme.Papel
import com.rutaalacima.app.ui.theme.fondoPapel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * Frases del senderista tomadas de los libros de Ruta a la Cima, con el libro de origen.
 * Cada vez que se abre la app aparece una distinta y llama a dar el paso de hoy.
 */
private val FRASES = listOf(
    R.string.inicio_frase_1 to "Descubre tu Cumbre Personal",
    R.string.inicio_frase_2 to "El Viaje Transformativo",
    R.string.inicio_frase_3 to "Guía de Caídas",
    R.string.inicio_frase_4 to "Descubre tu Cumbre Personal",
    R.string.inicio_frase_5 to "Cuando te pierdes en la niebla",
    R.string.inicio_frase_6 to "Desde la Cima",
    R.string.inicio_frase_7 to "Diagnóstico Personal",
    R.string.inicio_frase_8 to "Los 6 Ejes de tu Cumbre",
    R.string.inicio_frase_9 to "El Viaje Transformativo",
    R.string.inicio_frase_10 to "Portales y Transiciones",
    R.string.inicio_frase_11 to "Campamento Base",
    R.string.inicio_frase_12 to "Guía de Caídas",
)

/**
 * Ventana de inicio: el sello de cera cae sobre la hoja de papel con su anillo dorado girando,
 * aparece el nombre y luego un mensaje de viaje del senderista, tomado de los libros, con un
 * botón que invita a dar el paso de hoy. Continúa sola a los 7 s o al tocar la pantalla.
 */
@Composable
fun InicioScreen(onTerminar: () -> Unit) {
    val sello = remember { Animatable(0f) }
    val titulo = remember { Animatable(0f) }
    val frase = remember { Animatable(0f) }
    val accion = remember { Animatable(0f) }
    val salida = remember { Animatable(1f) }
    // Una frase distinta en cada arranque
    val (fraseRes, libro) = remember { FRASES.random() }
    var saliendo by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()
    val salir: () -> Unit = {
        if (!saliendo) {
            saliendo = true
            alcance.launch { salida.animateTo(0f, tween(350)); onTerminar() }
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope {
            listOf(
                async { sello.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) },
                async { delay(450); titulo.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) },
                async { delay(1000); frase.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) },
                async { delay(1900); accion.animateTo(1f, tween(500)) },
            ).awaitAll()
        }
        delay(5000)
        salir()
    }
    val giro = rememberInfiniteTransition(label = "giro")
    val angulo by giro.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart), label = "angulo")
    val oro = MaterialTheme.colorScheme.secondary

    Box(
        Modifier.fillMaxSize().fondoPapel().graphicsLayer { alpha = salida.value }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { salir() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 28.dp)) {
            Box(
                Modifier.size(196.dp).graphicsLayer {
                    val e = 0.6f + 0.4f * sello.value
                    scaleX = e; scaleY = e; alpha = sello.value.coerceIn(0f, 1f)
                },
                contentAlignment = Alignment.Center,
            ) {
                // Anillo dorado punteado que gira alrededor del sello
                Canvas(Modifier.fillMaxSize().rotate(angulo)) {
                    drawCircle(oro, radius = size.minDimension / 2 - 4f,
                        style = Stroke(width = 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 22f)), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
                // Sello de cera de Ruta a la Cima (con su propia sombra y relieve)
                Image(painterResource(R.drawable.logo_sello), stringResource(R.string.app_name), Modifier.size(162.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.app_name), fontSize = 40.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer { alpha = titulo.value; translationY = (1 - titulo.value) * 40f },
            )
            Spacer(Modifier.height(22.dp))
            // Mensaje de viaje del senderista, en una hoja de papel
            Column(
                Modifier.fillMaxWidth()
                    .graphicsLayer { alpha = frase.value; translationY = (1 - frase.value) * 28f }
                    .hojaPapel(elevacion = 4.dp)
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("“", fontSize = 48.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, color = oro, modifier = Modifier.height(30.dp))
                Text(
                    stringResource(fraseRes), style = EstiloCita.copy(fontSize = 22.sp, lineHeight = 30.sp),
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text("— $libro", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
            // Llamado a la acción
            Button(
                onClick = salir,
                modifier = Modifier.graphicsLayer { alpha = accion.value; scaleX = 0.9f + 0.1f * accion.value; scaleY = 0.9f + 0.1f * accion.value }
                    .shadow(6.dp, RoundedCornerShape(50), ambientColor = Papel.Sombra, spotColor = Papel.Sombra),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            ) {
                Icon(Icons.Filled.Hiking, null)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.inicio_accion), style = MaterialTheme.typography.titleMedium)
            }
        }
        // Firma de la serie
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp).graphicsLayer { alpha = titulo.value },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.logo_sello), null, Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.inicio_serie), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
        }
    }
}
