package com.rutaalacima.app.ui.inicio

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.translate
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** La carta de bienvenida se muestra sola la primera vez; después se puede releer desde el perfil. */
object CartaBienvenida {
    private const val PREFS = "rutacima_inicio"
    private const val CLAVE = "carta_leida"
    fun leida(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CLAVE, false)
    fun marcar(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CLAVE, true).apply()
}

private val SOBRE_CLARO = Color(0xFFF3EADA)
private val SOBRE_OSCURO = Color(0xFFEBDFC9)
private val SOLAPA_CLARA = Color(0xFFEFE4D0)
private val SOLAPA_OSCURA = Color(0xFFE5D7BC)
private val PAPEL_CARTA = Color(0xFFFBF6EC)
private val PLIEGUE = Color(0xFF5A4228)
private val TINTA = Color(0xFF3A2A20)
private val BURDEOS = Color(0xFF6B2A1A)
private val ORO = Color(0xFFB8862F)

/**
 * Carta de bienvenida. Primero un sobre cerrado, con la solapa en V y el sello de cera de Rutaalacima en
 * la punta; al romper el sello (un toque, con el mismo quiebre que la frase del día) el sobre se va y sale
 * la carta: papel marfil doblado en tres, con membrete, lugar y fecha, saludo, el mensaje del fundador y
 * su firma.
 */
@Composable
fun CartaBienvenidaScreen(onTerminar: () -> Unit) {
    val ctx = LocalContext.current
    val haptico = LocalHapticFeedback.current
    val alcance = rememberCoroutineScope()
    var rota by rememberSaveable { mutableStateOf(false) }
    var abierta by rememberSaveable { mutableStateOf(false) }
    val quiebre = remember { Animatable(if (rota) 1f else 0f) }
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val romper: () -> Unit = {
        if (!rota) {
            rota = true
            haptico.performHapticFeedback(HapticFeedbackType.LongPress)
            SonidoSello.reproducir(ctx)
            alcance.launch { quiebre.animateTo(1f, tween(1500, easing = LinearEasing)) }
            alcance.launch { delay(650); abierta = true }
        }
    }
    val parrafos = listOf(R.string.carta_p1, R.string.carta_p2, R.string.carta_p3, R.string.carta_p4, R.string.carta_p5)

    Box(Modifier.fillMaxSize().fondoPapel(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (abierta) Arrangement.Top else Arrangement.Center,
        ) {
            // El sobre cerrado
            AnimatedVisibility(
                visible = !abierta,
                exit = fadeOut(tween(380)) + slideOutVertically(tween(380)) { it / 8 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(48.dp))
                    Sobre(
                        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                            .graphicsLayer { alpha = entrada.value; translationY = (1f - entrada.value) * 40f },
                        sello = {
                            if (quiebre.value < 1f) {
                                val descripcion = stringResource(R.string.romper_sello)
                                SelloDeCera(
                                    progreso = quiebre.value, semilla = 1984L,
                                    modifier = Modifier.size(92.dp).semantics { contentDescription = descripcion; role = Role.Button }
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { romper() },
                                )
                            }
                        },
                        para = stringResource(R.string.carta_para),
                    )
                    Spacer(Modifier.height(20.dp))
                    if (!rota) Button(onClick = romper, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                        Icon(Icons.Filled.TouchApp, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.romper_sello))
                    }
                }
            }
            // La carta
            AnimatedVisibility(
                visible = abierta,
                enter = fadeIn(tween(600, delayMillis = 100)) + slideInVertically(tween(800, easing = FastOutSlowInEasing)) { it / 6 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    Carta(parrafos)
                    Spacer(Modifier.height(22.dp))
                    Button(onClick = onTerminar, contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)) {
                        Icon(Icons.Filled.Hiking, null); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.carta_empezar), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/** El sobre visto por detrás: cuerpo crema, pliegues diagonales abajo y la solapa en V con su sombra. */
@Composable
private fun Sobre(modifier: Modifier, sello: @Composable () -> Unit, para: String) {
    Box(
        modifier
            .aspectRatio(1.6f)
            .shadow(10.dp, RoundedCornerShape(6.dp), ambientColor = Papel.Sombra, spotColor = Papel.Sombra)
            .drawBehind {
                val w = size.width; val h = size.height; val punta = h * 0.58f
                val radio = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                drawRoundRect(Brush.verticalGradient(listOf(SOBRE_CLARO, SOBRE_OSCURO)), cornerRadius = radio)
                // Pliegues de las solapas de abajo: de las esquinas inferiores hacia el centro
                val linea = PLIEGUE.copy(alpha = 0.16f); val grosor = 1.dp.toPx()
                drawLine(linea, androidx.compose.ui.geometry.Offset(0f, h * 0.38f), androidx.compose.ui.geometry.Offset(w / 2, h), grosor)
                drawLine(linea, androidx.compose.ui.geometry.Offset(w, h * 0.38f), androidx.compose.ui.geometry.Offset(w / 2, h), grosor)
                // Solapa en V con una sombra suave debajo
                val solapa = androidx.compose.ui.graphics.Path().apply { moveTo(0f, 0f); lineTo(w, 0f); lineTo(w / 2, punta); close() }
                translate(top = 2.dp.toPx()) { drawPath(solapa, PLIEGUE.copy(alpha = 0.10f)) }
                drawPath(solapa, Brush.verticalGradient(listOf(SOLAPA_CLARA, SOLAPA_OSCURA), endY = punta))
            },
    ) {
        // El sello en la punta de la solapa
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.align(androidx.compose.ui.BiasAlignment(0f, 0.16f))) { sello() }
        }
        Text(
            para, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF5A4636), textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp, start = 16.dp, end = 16.dp),
        )
    }
}

/** La carta: papel marfil verjurado, doblada en tres, con membrete, lugar y fecha, y la firma del fundador. */
@Composable
private fun Carta(parrafos: List<Int>) {
    val fecha = remember { java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG)) }
    Column(
        Modifier
            .widthIn(max = 600.dp).fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(2.dp), ambientColor = Papel.Sombra, spotColor = Papel.Sombra)
            .drawBehind {
                drawRect(PAPEL_CARTA)
                // Textura verjurada muy leve
                var y = 0f; val paso = 4.dp.toPx()
                while (y < size.height) { drawLine(PLIEGUE.copy(alpha = 0.018f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f); y += paso }
                // Los dos pliegues de una carta doblada en tres: una sombra y una luz
                listOf(1f / 3f, 2f / 3f).forEach { f ->
                    val py = size.height * f
                    drawLine(PLIEGUE.copy(alpha = 0.08f), androidx.compose.ui.geometry.Offset(0f, py), androidx.compose.ui.geometry.Offset(size.width, py), 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(0f, py + 1.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width, py + 1.dp.toPx()), 1.dp.toPx())
                }
            }
            .padding(horizontal = 26.dp, vertical = 32.dp),
    ) {
        // Membrete
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.logo_sello), null, Modifier.size(44.dp))
            Spacer(Modifier.height(6.dp))
            Text("RUTA A LA CIMA", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 3.sp, color = BURDEOS)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(ORO.copy(alpha = 0.45f)))
        }
        Spacer(Modifier.height(16.dp))
        Text("Floridablanca, $fecha", fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyMedium,
            color = TINTA.copy(alpha = 0.75f), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.carta_saludo), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge, color = TINTA)
        Spacer(Modifier.height(12.dp))
        parrafos.forEach { p ->
            Text(stringResource(p), fontFamily = FontFamily.Serif, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp), color = TINTA)
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.carta_cierre), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic,
            style = MaterialTheme.typography.titleMedium, color = BURDEOS)
        Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.End) {
            Text(stringResource(R.string.carta_firma), fontFamily = FontFamily.Cursive, fontSize = 28.sp, color = Color(0xFF2E2018))
            Box(Modifier.padding(vertical = 4.dp).width(140.dp).height(1.dp).background(ORO.copy(alpha = 0.7f)))
            Text(stringResource(R.string.carta_firma_rol), style = MaterialTheme.typography.labelMedium, color = TINTA.copy(alpha = 0.7f))
        }
    }
}
