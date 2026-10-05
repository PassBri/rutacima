package com.rutaalacima.app.ui.mensajes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.Conversacion
import com.rutaalacima.app.data.social.Mensaje
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.comunidad.Avatar
import com.rutaalacima.app.ui.theme.fondoPapel
import com.rutaalacima.app.util.haceCuanto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MensajesViewModel(c: AppContainer) : ViewModel() {
    val repo = c.mensajes
    var conversaciones by mutableStateOf<List<Conversacion>>(emptyList())
    var cargando by mutableStateOf(true)
    var error by mutableStateOf(false)

    suspend fun cargar() {
        if (!repo.disponible) { cargando = false; return }
        runCatching { repo.conversaciones() }.onSuccess { conversaciones = it; error = false }.onFailure { error = true }
        cargando = false
    }
}

/** Mensajes: mis conversaciones 1 a 1 y las solicitudes que me llegaron. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensajesScreen(onBack: () -> Unit, onAbrir: (String) -> Unit, onCuenta: () -> Unit, onCoachVida: () -> Unit) {
    val vm = rutaViewModel { MensajesViewModel(it) }
    LaunchedEffect(Unit) { while (true) { vm.cargar(); delay(15_000) } }
    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.mensajes)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = { IconButton(onClick = onCoachVida) { Icon(Icons.Filled.Hiking, stringResource(R.string.coach_vida)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!vm.repo.disponible) {
                item {
                    RutaCard {
                        Text(stringResource(R.string.mensajes_sin_cuenta), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.padding(4.dp))
                        Button(onClick = onCuenta) { Text(stringResource(R.string.web_ir_cuenta)) }
                    }
                }
                return@LazyColumn
            }
            val solicitudes = vm.conversaciones.filter { it.solicitudParaMi }
            val resto = vm.conversaciones.filter { !it.solicitudParaMi }
            if (solicitudes.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.mensajes_solicitudes)) }
                items(solicitudes, key = { it.id }) { c -> FilaConversacion(c) { onAbrir(c.id) } }
                item { SectionTitle(stringResource(R.string.mensajes)) }
            }
            if (!vm.cargando && resto.isEmpty() && solicitudes.isEmpty()) {
                item {
                    Text(stringResource(R.string.mensajes_vacio), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                }
            }
            items(resto, key = { it.id }) { c -> FilaConversacion(c) { onAbrir(c.id) } }
            if (vm.error) item { Text(stringResource(R.string.mensajes_error), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun FilaConversacion(c: Conversacion, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(c.otroNombre, c.otroAvatar, tamano = 44)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.otroNombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                if (c.esMiCoach || c.laAcompano) {
                    Spacer(Modifier.width(6.dp))
                    Etiqueta(stringResource(if (c.esMiCoach) R.string.tu_coach else R.string.acompanas))
                }
            }
            Text(
                when {
                    c.solicitudParaMi -> stringResource(R.string.mensajes_quiere_escribirte)
                    c.estado == "bloqueada" -> stringResource(R.string.mensajes_bloqueada)
                    c.ultimoTexto.isBlank() -> stringResource(R.string.mensajes_sin_mensajes)
                    c.ultimoEsMio -> stringResource(R.string.mensajes_tu, c.ultimoTexto)
                    else -> c.ultimoTexto
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(haceCuanto(c.ultimoEn), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (c.noLeidos > 0) {
                Box(Modifier.padding(top = 4.dp).size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center) {
                    Text("${c.noLeidos}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun Etiqueta(texto: String) {
    Text(
        texto, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

// ====================================================================== Chat

class ChatViewModel(c: AppContainer) : ViewModel() {
    val repo = c.mensajes
    var conversacion by mutableStateOf<Conversacion?>(null)
    var mensajes by mutableStateOf<List<Mensaje>>(emptyList())
    var texto by mutableStateOf("")
    var aviso by mutableStateOf<String?>(null)

    suspend fun cargar(id: String) {
        runCatching {
            conversacion = repo.conversaciones().firstOrNull { it.id == id } ?: conversacion
            mensajes = repo.mensajes(id)
            if (conversacion?.noLeidos ?: 0 > 0) repo.marcarLeidos(id)
        }
    }

    fun enviar(id: String, error: String) = viewModelScope.launch {
        val t = texto.trim(); if (t.isEmpty()) return@launch
        texto = ""
        runCatching { repo.enviar(id, t) }.onFailure { texto = t; aviso = error }
        cargar(id)
    }

    fun responder(id: String, aceptar: Boolean) = viewModelScope.launch { runCatching { repo.responder(id, aceptar) }; cargar(id) }
    fun bloquear(id: String, si: Boolean) = viewModelScope.launch { runCatching { repo.bloquear(id, si) }; cargar(id) }
    fun reportar(motivo: String, gracias: String) = viewModelScope.launch {
        val c = conversacion ?: return@launch
        runCatching { repo.reportar(c.otroId, c.id, motivo) }.onSuccess { aviso = gracias }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(id: String, onBack: () -> Unit) {
    val vm = rutaViewModel { ChatViewModel(it) }
    LaunchedEffect(id) { while (true) { vm.cargar(id); delay(4_000) } }
    val c = vm.conversacion
    var menu by remember { mutableStateOf(false) }
    var reportando by remember { mutableStateOf(false) }
    val lista = rememberLazyListState()
    LaunchedEffect(vm.mensajes.size) { if (vm.mensajes.isNotEmpty()) lista.animateScrollToItem(vm.mensajes.size - 1) }
    val txtError = stringResource(R.string.mensajes_no_enviado)
    val txtGracias = stringResource(R.string.mensajes_reporte_gracias)

    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (c != null) Avatar(c.otroNombre, c.otroAvatar, tamano = 34)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(c?.otroNombre.orEmpty(), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            if (c != null && (c.esMiCoach || c.laAcompano)) {
                                Text(stringResource(if (c.esMiCoach) R.string.tu_coach else R.string.acompanas),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (c != null && c.estado == "bloqueada" && c.laBloqueeYo) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.mensajes_desbloquear)) }, onClick = { menu = false; vm.bloquear(id, false) })
                        } else {
                            DropdownMenuItem(text = { Text(stringResource(R.string.mensajes_bloquear)) }, onClick = { menu = false; vm.bloquear(id, true) })
                        }
                        DropdownMenuItem(text = { Text(stringResource(R.string.mensajes_reportar)) }, onClick = { menu = false; reportando = true })
                    }
                },
            )
        },
        bottomBar = {
            val puede = c != null && (c.estado == "aceptada" || (c.estado == "pendiente" && c.laInicieYo && vm.mensajes.isEmpty()))
            Column(Modifier.imePadding().padding(12.dp)) {
                vm.aviso?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                when {
                    c != null && c.solicitudParaMi -> RutaCard {
                        Text(stringResource(R.string.mensajes_solicitud_texto, c.otroNombre), style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.responder(id, true) }) { Text(stringResource(R.string.mensajes_aceptar)) }
                            OutlinedButton(onClick = { vm.responder(id, false) }) { Text(stringResource(R.string.mensajes_rechazar)) }
                        }
                    }
                    c != null && c.estado == "pendiente" && c.laInicieYo && vm.mensajes.isNotEmpty() ->
                        Text(stringResource(R.string.mensajes_esperando), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    c != null && c.estado == "bloqueada" ->
                        Text(stringResource(R.string.mensajes_bloqueada), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            vm.texto, { vm.texto = it.take(2000) }, Modifier.weight(1f), enabled = puede,
                            placeholder = { Text(stringResource(if (c?.estado == "pendiente") R.string.mensajes_primer_mensaje else R.string.mensajes_escribe)) },
                            maxLines = 4, shape = RoundedCornerShape(20.dp),
                        )
                        IconButton(onClick = { vm.enviar(id, txtError) }, enabled = puede && vm.texto.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.enviar), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(), state = lista,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(vm.mensajes, key = { it.id }) { m ->
                Box(Modifier.fillMaxWidth(), contentAlignment = if (m.mio) Alignment.CenterEnd else Alignment.CenterStart) {
                    Column(
                        Modifier.widthIn(max = 300.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (m.mio) 16.dp else 4.dp, bottomEnd = if (m.mio) 4.dp else 16.dp))
                            .background(if (m.mio) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(m.texto, style = MaterialTheme.typography.bodyMedium)
                        Text(haceCuanto(m.creadoEn), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
                    }
                }
            }
        }
    }

    if (reportando) {
        var motivo by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportando = false },
            title = { Text(stringResource(R.string.mensajes_reportar)) },
            text = { OutlinedTextField(motivo, { motivo = it.take(1000) }, label = { Text(stringResource(R.string.mensajes_reportar_motivo)) }, minLines = 3) },
            confirmButton = { TextButton(onClick = { vm.reportar(motivo, txtGracias); reportando = false }) { Text(stringResource(R.string.mensajes_reportar)) } },
            dismissButton = { TextButton(onClick = { reportando = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}
