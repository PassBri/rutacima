package com.rutaalacima.app.ui.inicio

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rutaalacima.app.R
import com.rutaalacima.app.ui.components.SelloDeCera
import com.rutaalacima.app.ui.components.SonidoSello
import com.rutaalacima.app.ui.theme.Papel
import com.rutaalacima.app.ui.theme.fondoPapel
import com.rutaalacima.app.ui.theme.hojaPapel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** La carta de bienvenida se muestra sola la primera vez; después se puede releer desde el perfil. */
object CartaBienvenida {
    private const val PREFS = "rutacima_inicio"
    private const val CLAVE = "carta_leida"
    fun leida(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CLAVE, false)
    fun marcar(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CLAVE, true).apply()
}

private val PERGAMINO = Color(0xFFF4E8D0)
private val TINTA = Color(0xFF3A2A20)
private val BURDEOS = Color(0xFF6B2A1A)
private val ORO = Color(0xFFB8862F)

/**
 * Carta de bienvenida: una carta antigua en papel, doblada y cerrada con el sello de cera de
 * Rutaalacima. Al romper el sello (un toque, con el mismo quiebre que la frase del día) la carta se
 * despliega y deja leer el mensaje del fundador.
 */
@Composable
fun CartaBienvenidaScreen(onTerminar: () -> Unit) {
    val ctx = LocalContext.current
    val haptico = LocalHapticFeedback.current
    val alcance = rememberCoroutineScope()
    var abierta by rememberSaveable { mutableStateOf(false) }
    val quiebre = remember { Animatable(if (abierta) 1f else 0f) }
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) }
    val romper: () -> Unit = {
        if (!abierta) {
            haptico.performHapticFeedback(HapticFeedbackType.LongPress)
            SonidoSello.reproducir(ctx)
            alcance.launch { quiebre.animateTo(1f, tween(1500, easing = LinearEasing)) }
            alcance.launch { delay(450); abierta = true }
        }
    }
    val parrafos = listOf(R.string.carta_p1, R.string.carta_p2, R.string.carta_p3, R.string.carta_p4, R.string.carta_p5)

    Box(Modifier.fillMaxSize().fondoPapel(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.carta_titulo).uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp, color = BURDEOS, modifier = Modifier.graphicsLayer { alpha = entrada.value })
            Spacer(Modifier.height(16.dp))
            // La carta: pergamino con borde envejecido
            Box(
                Modifier.widthIn(max = 560.dp).fillMaxWidth()
                    .graphicsLayer { val e = 0.92f + 0.08f * entrada.value; scaleX = e; scaleY = e; alpha = entrada.value.coerceIn(0f, 1f) }
                    .hojaPapel(color = PERGAMINO, elevacion = 6.dp, doblez = 30.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Borde gastado: viñeta cálida hacia las orillas
                Box(Modifier.matchParentSize().background(Brush.radialGradient(listOf(Color.Transparent, Color(0x22A0703A)), radius = 1400f)))
                Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.carta_para), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.titleMedium, color = TINTA, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    // Doblez de la carta con el sello encima
                    Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Papel.Sombra.copy(alpha = 0.35f), Color.Transparent))))
                        if (quiebre.value < 1f) {
                            val descripcion = stringResource(R.string.romper_sello)
                            SelloDeCera(
                                progreso = quiebre.value, semilla = 1984L,
                                modifier = Modifier.size(96.dp).semantics { contentDescription = descripcion; role = Role.Button }
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { romper() },
                            )
                        }
                    }
                    AnimatedVisibility(visible = !abierta) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.carta_romper), style = MaterialTheme.typography.bodyMedium, color = TINTA.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = romper, modifier = Modifier.shadow(6.dp, RoundedCornerShape(50), ambientColor = Papel.Sombra, spotColor = Papel.Sombra),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                                Icon(Icons.Filled.TouchApp, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.romper_sello))
                            }
                        }
                    }
                    // El mensaje se despliega al romper el sello
                    AnimatedVisibility(visible = abierta, enter = expandVertically(tween(900, easing = FastOutSlowInEasing)) + fadeIn(tween(900, delayMillis = 200))) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(stringResource(R.string.carta_saludo), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge, color = TINTA)
                            parrafos.forEach { p ->
                                Text(stringResource(p), fontFamily = FontFamily.Serif, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp),
                                    color = TINTA)
                            }
                            Text(stringResource(R.string.carta_cierre), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic,
                                style = MaterialTheme.typography.titleMedium, color = BURDEOS)
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                Box(Modifier.width(120.dp).height(1.dp).background(ORO))
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(R.string.carta_firma), fontFamily = FontFamily.Cursive, fontSize = 26.sp, color = TINTA)
                                Text(stringResource(R.string.carta_firma_rol), style = MaterialTheme.typography.labelMedium, color = TINTA.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            AnimatedVisibility(visible = abierta, enter = fadeIn(tween(600, delayMillis = 900))) {
                Button(onClick = onTerminar, modifier = Modifier.shadow(6.dp, RoundedCornerShape(50), ambientColor = Papel.Sombra, spotColor = Papel.Sombra),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)) {
                    Icon(Icons.Filled.Hiking, null); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.carta_empezar), style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
