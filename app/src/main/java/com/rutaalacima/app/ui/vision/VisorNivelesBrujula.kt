package com.rutaalacima.app.ui.vision

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.VisionCasillaEntity
import com.rutaalacima.app.domain.model.Mandala

@Composable
internal fun CabeceraSeccionVision(titulo: String, onGirar: (() -> Unit)? = null, girada: Boolean = false, onGrande: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        // Girar: gira todas las tarjetas de la sección a la vez (igual que la web)
        if (onGirar != null) {
            val giro by androidx.compose.animation.core.animateFloatAsState(if (girada) 180f else 0f, androidx.compose.animation.core.tween(600), label = "girarTodas")
            IconButton(onClick = onGirar) {
                Icon(Icons.Filled.Autorenew, stringResource(R.string.vision_girar_todas), Modifier.graphicsLayer { rotationZ = giro })
            }
        }
        IconButton(onClick = onGrande) {
            Icon(Icons.Filled.OpenInFull, stringResource(R.string.brujula_ver_nivel, titulo))
        }
    }
}

/** El mismo lienzo se reutiliza en el visor personal y en la Brújula compartida. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LienzoNivelBrujula(
    datos: DatosMandala, nivel: Int, bloque: Int, modo: ModoTarjetas = ModoTarjetas.IMAGEN,
    modifier: Modifier = Modifier,
    propio: Boolean = false,
    onPaso: ((VisionCasillaEntity, Int, String) -> Unit)? = null,
    onNivel: (Int) -> Unit, onBloque: (Int) -> Unit,
) {
    val altoVentana = (LocalConfiguration.current.screenHeightDp.dp - 180.dp).coerceAtLeast(1.dp)
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val proporcion = 1f
        val altoDisponible = if (maxHeight.value.isFinite()) maxHeight else altoVentana
        val ancho = minOf(maxWidth, altoDisponible * proporcion, if (nivel == 0) 400.dp else 560.dp)
        Box(Modifier.width(ancho).aspectRatio(proporcion).clip(RoundedCornerShape(16.dp))) {
            when (nivel) {
                0 -> GiroTarjeta(modo == ModoTarjetas.TEXTO) { texto ->
                    NivelCumbre(datos, Modifier.fillMaxSize().combinedClickable(onClick = { onNivel(1) }, onDoubleClick = { onNivel(1) }), modoTexto = texto)
                }
                1 -> NivelVision(datos, onCentro = { onNivel(2) },
                    onCampamento = { i -> if (modo == ModoTarjetas.EDICION) onNivel(2) else onBloque(Mandala.bloqueDe(i)) },
                    modoTexto = modo == ModoTarjetas.TEXTO)
                else -> Cuadricula(datos, bloque, modoTexto = modo == ModoTarjetas.TEXTO,
                    tonal = propio && modo == ModoTarjetas.IMAGEN, modoEdicion = modo == ModoTarjetas.EDICION && onPaso != null, onPaso = onPaso, onBloque = onBloque)
            }
        }
    }
}

/** Visor de niveles 1 ⇄ 9 ⇄ 81. El visor de tarjetas conserva su propia entrada y sus páginas. */
@Composable
internal fun VisorNivelesBrujula(
    datos: DatosMandala, nivelInicial: Int, bloque: Int,
    modo: ModoTarjetas, onModo: (ModoTarjetas) -> Unit,
    onPaso: (VisionCasillaEntity, Int, String) -> Unit,
    onAbrirBloque: (Int) -> Unit, onCerrar: () -> Unit,
) {
    var nivel by rememberSaveable(nivelInicial) { mutableIntStateOf(nivelInicial.coerceIn(0, 2)) }
    fun volver() { if (nivel > 0) nivel-- else onCerrar() }
    val nombres = listOf(stringResource(R.string.mandala_cumbre), stringResource(R.string.brujula_nivel_vision), stringResource(R.string.brujula_nivel_completa))
    Dialog(onDismissRequest = ::volver, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = ::volver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) }
                    Text(stringResource(R.string.brujula_titulo), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, stringResource(R.string.cerrar_brujula)) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                        nombres.forEachIndexed { i, nombre ->
                            if (i > 0) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(14.dp))
                            TextButton(onClick = { nivel = i }, enabled = nivel != i) {
                                Text(nombre, maxLines = 1, fontWeight = if (nivel == i) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    Text(stringResource(R.string.vision_nivel_numero, listOf(1, 9, 81)[nivel]), style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { nivel-- }, enabled = nivel > 0) { Icon(Icons.Filled.Remove, stringResource(R.string.brujula_alejar)) }
                    IconButton(onClick = { nivel++ }, enabled = nivel < 2) { Icon(Icons.Filled.Add, stringResource(R.string.brujula_acercar)) }
                }
                SelectorModoTarjetas(modo, onModo, Modifier.fillMaxWidth())
                AnimatedContent(targetState = nivel, modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                    transitionSpec = {
                        val acercar = targetState > initialState
                        val resorte = spring<Float>(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                        (scaleIn(resorte, initialScale = if (acercar) 3f else .34f) + fadeIn(tween(220))) togetherWith
                            (scaleOut(tween(260), targetScale = if (acercar) .34f else 3f) + fadeOut(tween(180)))
                    }, label = "brujulaNiveles") { actual ->
                    LienzoNivelBrujula(datos, actual, bloque, modo, Modifier.fillMaxSize(), propio = true,
                        onPaso = onPaso, onNivel = { nivel = it }, onBloque = onAbrirBloque)
                }
                if (nivel == 2 && modo == ModoTarjetas.IMAGEN) LeyendaCalor()
                Text(stringResource(when {
                    nivel == 0 -> R.string.brujula_zoom_1
                    nivel == 1 -> R.string.brujula_zoom_9
                    modo == ModoTarjetas.EDICION -> R.string.brujula_edicion_81_ayuda
                    else -> R.string.brujula_ayuda_81
                }), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}
