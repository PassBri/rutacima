package com.rutaalacima.app.ui.inicio

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
 * Ventana de inicio: la hoja de papel se ilumina, el sello de RutaCima cae sobre ella con su
 * anillo dorado girando, aparece el nombre, el lema y diez puntos que se llenan como los años
 * de la vida. Abajo, la firma de la serie. Dura ~2,4 s; tocar la pantalla la salta.
 */
@Composable
fun InicioScreen(onTerminar: () -> Unit) {
    val sello = remember { Animatable(0f) }
    val titulo = remember { Animatable(0f) }
    val lema = remember { Animatable(0f) }
    val puntos = remember { Animatable(0f) }
    val salida = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            listOf(
                async { sello.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) },
                async { delay(450); titulo.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) },
                async { delay(800); lema.animateTo(1f, tween(500)) },
                async { delay(950); puntos.animateTo(1f, tween(1000, easing = LinearEasing)) },
            ).awaitAll()
        }
        delay(250)
        salida.animateTo(0f, tween(350))
        onTerminar()
    }
    val giro = rememberInfiniteTransition(label = "giro")
    val angulo by giro.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart), label = "angulo")
    val oro = MaterialTheme.colorScheme.secondary

    Box(
        Modifier.fillMaxSize().fondoPapel().graphicsLayer { alpha = salida.value }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTerminar() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(214.dp).graphicsLayer {
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
                Image(painterResource(R.drawable.logo_sello), stringResource(R.string.app_name), Modifier.size(178.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.app_name), fontSize = 44.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer { alpha = titulo.value; translationY = (1 - titulo.value) * 40f },
            )
            Text(
                stringResource(R.string.inicio_lema), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp).graphicsLayer { alpha = lema.value; translationY = (1 - lema.value) * 24f },
            )
            Spacer(Modifier.height(22.dp))
            // Diez puntos que se llenan: los años de la vida
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.graphicsLayer { alpha = lema.value }) {
                repeat(10) { i ->
                    val lleno = puntos.value * 10 > i
                    Box(
                        Modifier.size(14.dp).shadow(if (lleno) 3.dp else 0.dp, CircleShape, ambientColor = Papel.Sombra, spotColor = Papel.Sombra)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !lleno -> MaterialTheme.colorScheme.surfaceVariant
                                    i == 9 -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.primary
                                },
                            ),
                    )
                }
            }
        }
        // Firma de la serie (logos)
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp).graphicsLayer { alpha = lema.value },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.logo_sello), null, Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.inicio_serie), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
        }
    }
}
