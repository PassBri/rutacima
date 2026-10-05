package com.rutaalacima.app.ui.workbook

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.audio.AvisoAudio
import com.rutaalacima.app.data.audio.FuenteAudio
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.launch

class AudiolibroViewModel(c: AppContainer) : ViewModel() {
    val reproductor = c.audiolibro
    val audios = c.audios
    val estado = reproductor.estado
    val remotos = audios.remotos
    val soyAutor = audios.soyAutor
    var subiendo by mutableStateOf(false)
        private set

    init { viewModelScope.launch { audios.actualizar() } }

    fun subir(guia: String, seccion: Int, uri: Uri, avisar: (Boolean) -> Unit) = viewModelScope.launch {
        subiendo = true
        val r = runCatching { audios.subir(guia, seccion, uri) }
        subiendo = false
        avisar(r.isSuccess)
        // Si ese capítulo está sonando, que ya suene con la grabación nueva
        val e = estado.value
        if (r.isSuccess && e.activo && e.workbookId == guia && e.seccion == seccion) reproductor.reproducir(guia, seccion)
    }

    fun quitar(guia: String, seccion: Int, avisar: (Boolean) -> Unit) = viewModelScope.launch {
        subiendo = true
        val r = runCatching { audios.quitar(guia, seccion) }
        subiendo = false
        avisar(r.isSuccess)
    }
}

private val VELOCIDADES = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
private fun Float.comoVelocidad() = (if (this % 1f == 0f) "${toInt()}" else "$this").replace('.', ',') + "×"

/**
 * Barra del audiolibro en un capítulo: "Escuchar este capítulo" o, si ya suena, sus controles.
 * Si eres autor, desde aquí subes o quitas tu grabación del capítulo.
 */
@Composable
fun BarraAudiolibro(workbookId: String, seccion: Int, minutos: Int) {
    val vm = rutaViewModel { AudiolibroViewModel(it) }
    val e by vm.estado.collectAsStateWithLifecycle()
    val remotos by vm.remotos.collectAsStateWithLifecycle()
    val soyAutor by vm.soyAutor.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val tieneGrabacion = remember(remotos, workbookId, seccion) { vm.audios.tieneGrabacion(workbookId, seccion) }
    val tieneRemota = remotos.containsKey("$workbookId/$seccion")
    val aqui = e.activo && e.workbookId == workbookId && e.seccion == seccion

    // Avisos del reproductor (una sola vez)
    val txtSinVoz = stringResource(R.string.audio_sin_voz)
    val txtErrorGrabacion = stringResource(R.string.audio_error_reproducir)
    LaunchedEffect(e.aviso) {
        when (e.aviso) {
            AvisoAudio.SIN_VOZ -> Toast.makeText(contexto, txtSinVoz, Toast.LENGTH_LONG).show()
            AvisoAudio.ERROR_GRABACION -> Toast.makeText(contexto, txtErrorGrabacion, Toast.LENGTH_LONG).show()
            null -> Unit
        }
        if (e.aviso != null) vm.reproductor.olvidarAviso()
    }

    val txtSubido = stringResource(R.string.audio_subido)
    val txtQuitado = stringResource(R.string.audio_quitado)
    val txtErrorSubir = stringResource(R.string.audio_error_subir)
    val elegir = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.subir(workbookId, seccion, uri) { ok ->
            Toast.makeText(contexto, if (ok) txtSubido else txtErrorSubir, Toast.LENGTH_LONG).show()
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!aqui) {
                FilledTonalIconButton(onClick = { vm.reproductor.reproducir(workbookId, seccion) }) {
                    Icon(Icons.Filled.Headphones, stringResource(R.string.audio_escuchar))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { vm.reproductor.reproducir(workbookId, seccion) }) {
                    Text(stringResource(R.string.audio_escuchar_capitulo, minutos), style = MaterialTheme.typography.labelLarge)
                    Fuente(tieneGrabacion)
                }
            } else {
                IconButton(onClick = { vm.reproductor.saltar(-1) }) { Icon(Icons.Filled.FastRewind, stringResource(R.string.audio_atras)) }
                FilledIconButton(onClick = { vm.reproductor.alternar() }) {
                    if (e.cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(if (e.reproduciendo) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        stringResource(if (e.reproduciendo) R.string.audio_pausar else R.string.audio_reanudar))
                }
                IconButton(onClick = { vm.reproductor.saltar(+1) }) { Icon(Icons.Filled.FastForward, stringResource(R.string.audio_adelante)) }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Fuente(e.fuente == FuenteAudio.AUTOR)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(progress = { e.avance }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (soyAutor) {
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }, enabled = !vm.subiendo) {
                        if (vm.subiendo) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.UploadFile, stringResource(R.string.audio_subir))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(if (tieneRemota) R.string.audio_reemplazar else R.string.audio_subir)) },
                            leadingIcon = { Icon(Icons.Filled.Mic, null) },
                            onClick = { menu = false; elegir.launch("audio/*") },
                        )
                        if (tieneRemota) DropdownMenuItem(
                            text = { Text(stringResource(R.string.audio_quitar)) },
                            leadingIcon = { Icon(Icons.Filled.Close, null) },
                            onClick = {
                                menu = false
                                vm.quitar(workbookId, seccion) { ok -> Toast.makeText(contexto, if (ok) txtQuitado else txtErrorSubir, Toast.LENGTH_LONG).show() }
                            },
                        )
                    }
                }
            }
        }
        // Segunda fila mientras suena: velocidad, temporizador para dormir y cerrar
        if (aqui) Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                val i = VELOCIDADES.indexOfFirst { it >= e.velocidad - 0.01f }.coerceAtLeast(0)
                vm.reproductor.velocidad(VELOCIDADES[(i + 1) % VELOCIDADES.size])
            }) { Text(stringResource(R.string.audio_velocidad_n, e.velocidad.comoVelocidad()), style = MaterialTheme.typography.labelLarge) }
            Temporizador(e.apagadoEn != null || e.alTerminarCapitulo, onElegir = { vm.reproductor.temporizador(it) },
                onFinCapitulo = { vm.reproductor.dormirAlTerminarCapitulo() })
            if (e.apagadoEn != null || e.alTerminarCapitulo) {
                val faltan = e.apagadoEn?.let { ((it - System.currentTimeMillis()) / 60_000L + 1).toInt().coerceAtLeast(1) }
                Text(if (faltan != null) stringResource(R.string.audio_se_apaga_en, faltan) else stringResource(R.string.audio_se_apaga_fin),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.reproductor.detener() }) {
                Icon(Icons.Filled.Close, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.audio_cerrar_corto))
            }
        }
        if (vm.subiendo) Text(stringResource(R.string.audio_subiendo), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun Fuente(autor: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (autor) Icons.Filled.Mic else Icons.Filled.Headphones, null, Modifier.size(14.dp),
            tint = if (autor) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(stringResource(if (autor) R.string.audio_voz_autor else R.string.audio_voz_app), style = MaterialTheme.typography.labelSmall,
            color = if (autor) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Reproductor pequeño sobre la barra de pestañas mientras suena un audiolibro. */
@Composable
fun MiniReproductor(onAbrir: (String, Int) -> Unit) {
    val vm = rutaViewModel { AudiolibroViewModel(it) }
    val e by vm.estado.collectAsStateWithLifecycle()
    if (!e.activo) return
    Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            LinearProgressIndicator(progress = { e.avance }, modifier = Modifier.fillMaxWidth().height(2.dp))
            Row(
                Modifier.fillMaxWidth().clickable { onAbrir(e.workbookId, e.seccion) }.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (e.fuente == FuenteAudio.AUTOR) Icons.Filled.Mic else Icons.Filled.Headphones, null,
                    tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.tituloSeccion, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(e.guia, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { vm.reproductor.alternar() }) {
                    Icon(if (e.reproduciendo) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        stringResource(if (e.reproduciendo) R.string.audio_pausar else R.string.audio_reanudar))
                }
                IconButton(onClick = { vm.reproductor.detener() }) { Icon(Icons.Filled.Close, stringResource(R.string.audio_cerrar)) }
            }
            HorizontalDivider()
        }
    }
}


/** Temporizador para dormir: 15, 30, 45 o 60 minutos, al terminar el capítulo, o apagado. */
@Composable
private fun Temporizador(activo: Boolean, onElegir: (Int?) -> Unit, onFinCapitulo: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menu = true }) {
            Icon(Icons.Filled.Bedtime, stringResource(R.string.audio_temporizador),
                tint = if (activo) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            listOf(15, 30, 45, 60).forEach { m ->
                DropdownMenuItem(text = { Text(stringResource(R.string.audio_en_minutos, m)) }, onClick = { menu = false; onElegir(m) })
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.audio_al_terminar_capitulo)) }, onClick = { menu = false; onFinCapitulo() })
            if (activo) DropdownMenuItem(text = { Text(stringResource(R.string.audio_temporizador_apagar)) }, onClick = { menu = false; onElegir(null) })
        }
    }
}
