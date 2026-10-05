package com.rutaalacima.app.ui.workbook

import com.rutaalacima.app.ui.theme.fondoPapel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rutaalacima.app.R
import com.rutaalacima.app.data.content.calcularResultados
import com.rutaalacima.app.ui.components.MontanaArte
import com.rutaalacima.app.ui.components.iconoGuia
import com.rutaalacima.app.ui.i18n.Textos
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel

/** Portada e índice de secciones de un workbook. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkbookIndexScreen(
    workbookId: String,
    onBack: () -> Unit,
    onOpenSection: (Int) -> Unit,
) {
    val vm = rutaViewModel(key = "index-$workbookId") { WorkbookIndexViewModel(it, workbookId) }
    val respuestas by vm.respuestas.collectAsStateWithLifecycle()
    val wb = vm.workbook
    val contenedor = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.rutaalacima.app.RutaApp).container
    val audiolibro = contenedor.audiolibro
    val grabaciones by contenedor.audios.remotos.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { contenedor.audios.actualizar() }

    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                title = { Text(wb?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        if (wb == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(Modifier.padding(bottom = 8.dp)) {
                    MontanaArte(wb.id, Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)), bandera = false) {
                        Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.BottomStart) {
                            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xE6FFFFFF)), contentAlignment = Alignment.Center) {
                                Icon(iconoGuia(wb.id), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(wb.subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    val p = wb.progreso(respuestas)
                    ProgressLine(p)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (wb.fields.isEmpty()) stringResource(R.string.lectura_secciones, wb.sections.size)
                        else stringResource(R.string.completado_ejercicios, (p * 100).toInt(), wb.fields.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    val ultima = respuestas["${wb.id}#ultima"]?.toIntOrNull()
                    val siguiente = (ultima ?: wb.sections.indexOfFirst { it.progreso(respuestas) in 0f..0.999f }).coerceIn(0, wb.sections.lastIndex)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { onOpenSection(siguiente) }) {
                            Text(stringResource(if (p > 0f || ultima != null) R.string.continuar else R.string.comenzar))
                        }
                        val pos = remember(wb.id) { audiolibro.posicion(wb.id) }
                        OutlinedButton(onClick = {
                            audiolibro.continuar(wb.id, siguiente)
                            onOpenSection(pos?.seccion ?: siguiente)
                        }) {
                            Icon(Icons.Filled.Headphones, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(if (pos != null) R.string.audio_seguir_escuchando else R.string.audio_escuchar))
                        }
                    }
                    // Grabaciones del autor: descargarlas para escuchar sin internet
                    val remotas = remember(grabaciones, wb.id) { contenedor.audios.grabacionesRemotas(wb.id) }
                    if (remotas.isNotEmpty()) {
                        val alcance = androidx.compose.runtime.rememberCoroutineScope()
                        var progreso by androidx.compose.runtime.remember(wb.id) { androidx.compose.runtime.mutableStateOf<Pair<Int, Int>?>(null) }
                        var lista by androidx.compose.runtime.remember(wb.id, grabaciones) { androidx.compose.runtime.mutableStateOf(contenedor.audios.descargada(wb.id)) }
                        val txtError = stringResource(R.string.audio_descarga_error)
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Icon(if (lista) Icons.Filled.DownloadDone else Icons.Filled.Download, null, Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(6.dp))
                            val p = progreso
                            Text(
                                when {
                                    p != null -> stringResource(R.string.audio_descargando, p.first, p.second)
                                    lista -> stringResource(R.string.audio_descargados)
                                    else -> stringResource(R.string.audio_n_grabaciones, remotas.size)
                                },
                                style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f),
                            )
                            if (p == null) androidx.compose.material3.TextButton(onClick = {
                                if (lista) { contenedor.audios.borrarDescargas(wb.id); lista = false }
                                else alcance.launch {
                                    progreso = 0 to remotas.size
                                    runCatching { contenedor.audios.descargar(wb.id) { h, t -> progreso = h to t } }
                                        .onFailure { android.widget.Toast.makeText(ctx, txtError, android.widget.Toast.LENGTH_LONG).show() }
                                    progreso = null
                                    lista = contenedor.audios.descargada(wb.id)
                                }
                            }) { Text(stringResource(if (lista) R.string.audio_borrar_descargas else R.string.audio_descargar)) }
                        }
                    }
                }
            }
            // Resultados de todas las evaluaciones del workbook
            val resultados = wb.sections.mapIndexedNotNull { i, s -> calcularResultados(s, respuestas)?.takeIf { it.respondidas > 0 }?.let { Triple(i, s, it) } }
            if (resultados.isNotEmpty()) {
                item {
                    RutaCard {
                        Text(stringResource(R.string.tus_resultados), style = MaterialTheme.typography.titleMedium)
                        resultados.forEach { (i, s, r) ->
                            Row(Modifier.fillMaxWidth().clickable { onOpenSection(i) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(s.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(stringResource(Textos.interpretacion(r.pct)), style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${(r.pct * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            itemsIndexed(wb.sections) { i, s ->
                val p = s.progreso(respuestas)
                RutaCard(onClick = { onOpenSection(i) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icono = when {
                            p < 0f -> Icons.AutoMirrored.Outlined.MenuBook
                            p >= 1f -> Icons.Filled.CheckCircle
                            else -> Icons.Filled.RadioButtonUnchecked
                        }
                        Icon(
                            icono,
                            contentDescription = null,
                            tint = if (p >= 1f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(s.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                                if (grabaciones.containsKey("${wb.id}/$i") || contenedor.audios.tieneGrabacion(wb.id, i)) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Filled.Mic, stringResource(R.string.audio_voz_autor), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            if (p >= 0f) {
                                Spacer(Modifier.height(6.dp))
                                ProgressLine(p)
                            } else {
                                Text(stringResource(R.string.lectura), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Lectura y diligenciamiento de una sección, con navegación anterior/siguiente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionScreen(
    workbookId: String,
    startIndex: Int,
    onBack: () -> Unit,
) {
    val vm = rutaViewModel(key = "editor-$workbookId") { WorkbookEditorViewModel(it, workbookId) }
    val wb = vm.workbook
    var index by rememberSaveable(workbookId) { mutableIntStateOf(startIndex) }
    val listState = rememberLazyListState()
    LaunchedEffect(index) { listState.scrollToItem(0) }
    LaunchedEffect(index, vm.cargado) {
        // Recuerda la última sección abierta (sirve de marcador en las lecturas).
        if (vm.cargado) vm.fijar("$workbookId#ultima", index.toString())
    }
    val respuestas = vm.comoRespuestas()
    val audio by (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.rutaalacima.app.RutaApp)
        .container.audiolibro.estado.collectAsStateWithLifecycle()
    LaunchedEffect(audio.seccion, audio.workbookId, audio.activo) {
        if (audio.activo && audio.workbookId == workbookId && audio.seccion != index) index = audio.seccion
    }

    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                title = {
                    Column {
                        Text(wb?.sections?.getOrNull(index)?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium)
                        Text(wb?.title ?: "", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
        bottomBar = {
            if (wb != null) {
                Surface(tonalElevation = 3.dp) {
                  Column {
                    val minutos = remember(wb.id, index) {
                        wb.sections.getOrNull(index)?.let { com.rutaalacima.app.data.content.Locucion.minutos(com.rutaalacima.app.data.content.Locucion.fragmentos(it)) } ?: 1
                    }
                    BarraAudiolibro(wb.id, index, minutos)
                    androidx.compose.material3.HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = { index-- }, enabled = index > 0) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                        Text(
                            "${index + 1} / ${wb.sections.size}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        if (index < wb.sections.lastIndex) {
                            Button(onClick = { index++ }) {
                                Text(stringResource(R.string.siguiente))
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                            }
                        } else {
                            Button(onClick = onBack) { Text(stringResource(R.string.terminar)) }
                        }
                    }
                  }
                }
            }
        },
    ) { padding ->
        val seccion = wb?.sections?.getOrNull(index)
        if (wb == null || !vm.cargado || seccion == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        // Resultados automáticos de las escalas 1-10 de esta sección (se recalculan al responder).
        val resultado = calcularResultados(seccion, vm.respuestas)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "titulo-$index") {
                Text(seccion.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            itemsIndexed(seccion.blocks, key = { i, _ -> "$index-$i" }) { _, block ->
                BlockView(block, respuestas)
            }
            if (resultado != null) {
                item(key = "resultados-$index") {
                    ResultadosCard(resultado, onGuardarEjes = { vm.guardarEnEjes(it) })
                    if (vm.ejesGuardados) {
                        Text(stringResource(R.string.ejes_guardados), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
