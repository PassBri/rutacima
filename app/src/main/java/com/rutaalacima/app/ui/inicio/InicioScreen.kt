package com.rutaalacima.app.ui.inicio

import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.TextButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.seguridad.ModoBloqueo
import com.rutaalacima.app.seguridad.Bloqueo
import com.rutaalacima.app.domain.model.FrasesDelDia
import com.rutaalacima.app.domain.model.FraseLibro
import com.rutaalacima.app.AppContainer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
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
import com.rutaalacima.app.ui.components.SelloDeCera
import com.rutaalacima.app.ui.components.SonidoSello
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class InicioViewModel(private val c: AppContainer) : ViewModel() {
    var frase by mutableStateOf<FraseLibro?>(null)
        private set
    val indice = FrasesDelDia.indice(LocalDate.now())
    val desbloqueadas: StateFlow<Set<Int>> = c.frases.desbloqueadas

    init { viewModelScope.launch { frase = runCatching { c.frases.delDia() }.getOrNull() } }

    fun abrir() = c.frases.desbloquear()
}

/** Estado del sello de la frase del día. */
private enum class Sello { CERRADO, CLAVE, ABIERTO }

/**
 * Ventana de inicio: el sello de cera cae sobre la hoja de papel con su anillo dorado, aparece
 * el nombre y la frase del día (una de 365, tomadas de los libros) cerrada con el sello. La
 * persona rompe el sello con su rostro o su huella (lo verifica el teléfono), con su clave
 * propia o con un toque, según lo que haya elegido en Ajustes. Entonces la frase se revela y
 * aparece el botón para dar el paso de hoy.
 */
@Composable
fun InicioScreen(onTerminar: () -> Unit) {
    val vm = rutaViewModel { InicioViewModel(it) }
    val ctx = LocalContext.current
    val modo = remember { Bloqueo.modo(ctx) }
    val desbloqueadas by vm.desbloqueadas.collectAsStateWithLifecycle()
    var sello by rememberSaveable { mutableStateOf(Sello.CERRADO) }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val entrada = remember { Animatable(0f) }
    val titulo = remember { Animatable(0f) }
    val tarjeta = remember { Animatable(0f) }
    val rotura = remember { Animatable(if (sello == Sello.ABIERTO) 1f else 0f) }
    /** El sello partiéndose: grietas, trozos que saltan y caen (0 = entero, 1 = ya no está). */
    val quiebre = remember { Animatable(if (sello == Sello.ABIERTO) 1f else 0f) }
    val haptico = LocalHapticFeedback.current
    val salida = remember { Animatable(1f) }
    val alcance = rememberCoroutineScope()
    var saliendo by remember { mutableStateOf(false) }
    val salir: () -> Unit = {
        if (!saliendo) { saliendo = true; alcance.launch { salida.animateTo(0f, tween(350)); onTerminar() } }
    }
    val abrir: () -> Unit = {
        vm.abrir(); error = null; sello = Sello.ABIERTO
        haptico.performHapticFeedback(HapticFeedbackType.LongPress)
        SonidoSello.reproducir(ctx)
        alcance.launch { quiebre.animateTo(1f, tween(1500, easing = LinearEasing)) }
        // La frase aparece cuando los trozos ya van saliendo
        alcance.launch { delay(480); rotura.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
    }
    val msgError = stringResource(R.string.biometria_error, "%s")
    val tituloBio = stringResource(R.string.biometria_titulo)
    val subBio = stringResource(R.string.biometria_sub)
    val romper: () -> Unit = {
        when (modo) {
            ModoBloqueo.TOQUE -> abrir()
            ModoBloqueo.CLAVE -> sello = Sello.CLAVE
            ModoBloqueo.BIOMETRIA -> {
                val act = Bloqueo.actividad(ctx)
                if (act == null) abrir()
                else Bloqueo.pedirBiometria(act, tituloBio, subBio, onExito = abrir, onError = { error = if (it.isBlank()) null else msgError.replace("%s", it) })
            }
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope {
            listOf(
                async { entrada.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) },
                async { delay(400); titulo.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) },
                async { delay(800); tarjeta.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) },
            ).awaitAll()
        }
        // Rostro/huella: el diálogo del sistema aparece solo, sin tener que buscar el botón
        if (sello == Sello.CERRADO && modo == ModoBloqueo.BIOMETRIA) romper()
    }
    LaunchedEffect(sello) { if (sello == Sello.ABIERTO) { delay(9000); salir() } }

    val giro = rememberInfiniteTransition(label = "giro")
    val angulo by giro.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart), label = "angulo")
    val oro = MaterialTheme.colorScheme.secondary

    Box(Modifier.fillMaxSize().fondoPapel().graphicsLayer { alpha = salida.value }, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(48.dp))
            // Sello con el anillo dorado (más pequeño cuando el teclado está abierto)
            val tam = if (sello == Sello.CLAVE) 96.dp else 150.dp
            Box(
                Modifier.size(tam + 30.dp).graphicsLayer {
                    val e = 0.6f + 0.4f * entrada.value
                    scaleX = e; scaleY = e; alpha = entrada.value.coerceIn(0f, 1f)
                },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize().rotate(angulo)) {
                    drawCircle(oro, radius = size.minDimension / 2 - 4f,
                        style = Stroke(width = 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 22f)), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
                Image(painterResource(R.drawable.logo_sello), stringResource(R.string.app_name), Modifier.size(tam))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.app_name), fontSize = 36.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer { alpha = titulo.value; translationY = (1 - titulo.value) * 30f },
            )
            Spacer(Modifier.height(18.dp))

            // La frase del día, sellada con cera
            Box(
                Modifier.fillMaxWidth().graphicsLayer { alpha = tarjeta.value; translationY = (1 - tarjeta.value) * 28f },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.fillMaxWidth().hojaPapel(elevacion = 4.dp).padding(horizontal = 22.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.frase_dia_n, vm.indice + 1), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val f = vm.frase
                    Box(contentAlignment = Alignment.Center) {
                        // Texto real (se revela al romper el sello)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = rotura.value }) {
                            Text(
                                if (f != null) "“${f.t}”" else "", style = EstiloCita.copy(fontSize = 21.sp, lineHeight = 29.sp),
                                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            if (f != null) Text("— ${f.libro}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Renglones ocultos mientras está sellada
                        if (rotura.value < 1f) {
                            Column(
                                Modifier.graphicsLayer { alpha = 1f - rotura.value },
                                verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                listOf(0.9f, 1f, 0.75f).forEach { w ->
                                    Box(Modifier.fillMaxWidth(w).height(12.dp).clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant))
                                }
                            }
                        }
                    }
                }
                // Sello de cera sobre la frase: se agrieta y se parte en trozos al abrirla
                // (cada día se rompe distinto: la semilla es el día del año)
                if (quiebre.value < 1f) {
                    val descripcion = stringResource(R.string.romper_sello)
                    SelloDeCera(
                        progreso = quiebre.value,
                        semilla = LocalDate.now().year * 1000L + vm.indice,
                        modifier = Modifier.offset(y = 14.dp).size(84.dp)
                            .semantics { contentDescription = descripcion; role = Role.Button }
                            .clickable(
                                enabled = sello == Sello.CERRADO,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { romper() },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            when (sello) {
                Sello.CERRADO -> {
                    Text(
                        stringResource(
                            when (modo) {
                                ModoBloqueo.BIOMETRIA -> R.string.frase_sellada_rostro
                                ModoBloqueo.CLAVE -> R.string.frase_sellada_clave
                                ModoBloqueo.TOQUE -> R.string.frase_sellada_toque
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = romper,
                        modifier = Modifier.shadow(6.dp, RoundedCornerShape(50), ambientColor = Papel.Sombra, spotColor = Papel.Sombra),
                        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 14.dp),
                    ) {
                        Icon(
                            when (modo) {
                                ModoBloqueo.BIOMETRIA -> Icons.Filled.Face
                                ModoBloqueo.CLAVE -> Icons.Filled.Lock
                                ModoBloqueo.TOQUE -> Icons.Filled.TouchApp
                            },
                            null,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.romper_sello), style = MaterialTheme.typography.titleMedium)
                    }
                    if (modo == ModoBloqueo.BIOMETRIA && Bloqueo.tieneClave(ctx)) {
                        TextButton(onClick = { sello = Sello.CLAVE }) { Text(stringResource(R.string.usar_clave)) }
                    }
                }
                Sello.CLAVE -> {
                    val incorrecta = stringResource(R.string.clave_incorrecta)
                    val espera = stringResource(R.string.clave_espera, (Bloqueo.ESPERA_MS / 1000).toInt())
                    PinPad(
                        titulo = stringResource(R.string.clave_titulo), valor = clave, onCambio = { clave = it; error = null },
                        onConfirmar = {
                            when {
                                Bloqueo.esperaRestante(ctx) > 0 -> error = espera
                                Bloqueo.verificarClave(ctx, clave) -> { clave = ""; abrir() }
                                else -> { clave = ""; error = if (Bloqueo.esperaRestante(ctx) > 0) espera else incorrecta }
                            }
                        },
                        error = error,
                    )
                }
                Sello.ABIERTO -> {
                    Text(stringResource(R.string.frase_contador, desbloqueadas.size), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.graphicsLayer { alpha = rotura.value })
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = salir,
                        modifier = Modifier.graphicsLayer { alpha = rotura.value }
                            .shadow(6.dp, RoundedCornerShape(50), ambientColor = Papel.Sombra, spotColor = Papel.Sombra),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                    ) {
                        Icon(Icons.Filled.Hiking, null)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.inicio_accion), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            // Firma de la serie
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.navigationBarsPadding().graphicsLayer { alpha = titulo.value }) {
                Image(painterResource(R.drawable.logo_sello), null, Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.inicio_serie), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
