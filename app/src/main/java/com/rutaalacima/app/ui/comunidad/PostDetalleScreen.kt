package com.rutaalacima.app.ui.comunidad

import com.rutaalacima.app.ui.theme.fondoPapel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.remember
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.Comentario
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.util.haceCuanto
import kotlinx.coroutines.launch

class PostDetalleViewModel(private val c: AppContainer, id: String) : ViewModel() {
    var post by mutableStateOf(c.social.enCache(id))
        private set
    var comentarios by mutableStateOf<List<Comentario>>(emptyList())
        private set
    var nuevo by mutableStateOf("")

    /** Se puede escribirle al autor (no es mío, no es de ejemplo y hay cuenta). */
    val puedoEscribir: Boolean get() = post.let { it != null && !it.propio && !it.demo && it.autorId.isNotBlank() } && c.mensajes.disponible

    fun escribir(onChat: (String) -> Unit) {
        val p = post ?: return
        viewModelScope.launch { runCatching { c.mensajes.abrirCon(p.autorId) }.onSuccess(onChat) }
    }

    fun cargar() {
        val p = post ?: return
        viewModelScope.launch { comentarios = runCatching { c.social.comentarios(p) }.getOrDefault(emptyList()) }
    }

    fun comentar() {
        val p = post ?: return
        val t = nuevo
        nuevo = ""
        viewModelScope.launch {
            runCatching { c.social.comentar(p, t) }
            post = p.copy(comentarios = p.comentarios + 1)
            cargar()
        }
    }

    fun impulsar() {
        val p = post ?: return
        post = p.copy(yoImpulse = !p.yoImpulse, impulsos = p.impulsos + if (p.yoImpulse) -1 else 1)
        c.social.recordar(listOfNotNull(post))
        viewModelScope.launch { runCatching { c.social.impulsar(p) } }
    }

    /** Reporta la publicación (deja de verse para ti) y vuelve atrás. */
    fun reportar(motivo: String, alTerminar: () -> Unit) {
        val p = post ?: return
        viewModelScope.launch { runCatching { c.social.reportar(p, motivo) }; alTerminar() }
    }

    /** Bloquea al autor (no ves nada suyo ni pueden escribirse) y vuelve atrás. */
    fun bloquear(alTerminar: () -> Unit) {
        val p = post ?: return
        viewModelScope.launch { runCatching { c.social.bloquear(p) }; alTerminar() }
    }

    fun eliminar(alTerminar: () -> Unit) {
        val p = post ?: return
        viewModelScope.launch { c.social.eliminar(p); alTerminar() }
    }
}

/** Detalle de una publicación con sus comentarios. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetalleScreen(postId: String, onBack: () -> Unit, onChat: (String) -> Unit = {}) {
    val vm = rutaViewModel(key = "post-$postId") { PostDetalleViewModel(it, postId) }
    LaunchedEffect(postId) { vm.cargar() }
    val post: Post? = vm.post

    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                title = { Text(stringResource(R.string.publicacion)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = {
                    if (vm.puedoEscribir) {
                        androidx.compose.material3.TextButton(onClick = { vm.escribir(onChat) }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.mensajes_enviar_mensaje))
                        }
                    }
                    if (post != null && !post.propio) MenuModeracion(
                        autor = post.autorNombre,
                        onReportar = { motivo -> vm.reportar(motivo, onBack) },
                        onBloquear = { vm.bloquear(onBack) },
                    )
                    if (post?.propio == true) IconButton(onClick = { vm.eliminar(onBack) }) { Icon(Icons.Outlined.Delete, stringResource(R.string.eliminar)) }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(vm.nuevo, { vm.nuevo = it }, placeholder = { Text(stringResource(R.string.escribe_comentario)) },
                        modifier = Modifier.weight(1f), maxLines = 4)
                    IconButton(onClick = vm::comentar, enabled = vm.nuevo.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.comentar), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (post == null) {
                item { Text(stringResource(R.string.publicacion_no_disponible), modifier = Modifier.padding(24.dp)) }
                return@LazyColumn
            }
            item { PostCard(post, onImpulsar = vm::impulsar, onComentar = {}, onAbrir = {}) }
            item { Text(stringResource(R.string.comentarios_n, vm.comentarios.size), style = MaterialTheme.typography.titleMedium) }
            items(vm.comentarios, key = { it.id }) { c ->
                Row(verticalAlignment = Alignment.Top) {
                    Avatar(c.autor, tamano = 32)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(c.autor, style = MaterialTheme.typography.labelLarge)
                        Text(c.texto, style = MaterialTheme.typography.bodyMedium)
                        Text(haceCuanto(c.creadoEn), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}


/** Menú ⋮ de una publicación ajena: reportarla (con el motivo) o bloquear a quien la publicó. */
@Composable
private fun MenuModeracion(autor: String, onReportar: (String) -> Unit, onBloquear: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    var reportando by remember { mutableStateOf(false) }
    var bloqueando by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        IconButton(onClick = { abierto = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.mas_opciones)) }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.reportar_publicacion)) }, leadingIcon = { Icon(Icons.Outlined.Flag, null) },
                onClick = { abierto = false; reportando = true })
            DropdownMenuItem(text = { Text(stringResource(R.string.bloquear_a, autor)) }, leadingIcon = { Icon(Icons.Outlined.Block, null) },
                onClick = { abierto = false; bloqueando = true })
        }
    }
    if (reportando) {
        val motivos = listOf(R.string.motivo_spam, R.string.motivo_ofensivo, R.string.motivo_acoso, R.string.motivo_falso, R.string.motivo_otro)
            .map { stringResource(it) }
        var elegido by remember { mutableStateOf(motivos.first()) }
        var detalle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportando = false },
            title = { Text(stringResource(R.string.reportar_publicacion)) },
            text = {
                Column {
                    Text(stringResource(R.string.reportar_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    motivos.forEach { m ->
                        Row(Modifier.fillMaxWidth().selectable(selected = m == elegido, onClick = { elegido = m }, role = Role.RadioButton),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = m == elegido, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(m, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 10.dp))
                        }
                    }
                    OutlinedTextField(detalle, { detalle = it.take(800) }, label = { Text(stringResource(R.string.mensajes_reportar_motivo)) },
                        modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            },
            confirmButton = { TextButton(onClick = { reportando = false; onReportar(listOf(elegido, detalle.trim()).filter { it.isNotEmpty() }.joinToString(": ")) }) {
                Text(stringResource(R.string.mensajes_reportar)) } },
            dismissButton = { TextButton(onClick = { reportando = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
    if (bloqueando) {
        AlertDialog(
            onDismissRequest = { bloqueando = false },
            title = { Text(stringResource(R.string.bloquear_a, autor)) },
            text = { Text(stringResource(R.string.bloquear_texto)) },
            confirmButton = { TextButton(onClick = { bloqueando = false; onBloquear() }) { Text(stringResource(R.string.mensajes_bloquear), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { bloqueando = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}
