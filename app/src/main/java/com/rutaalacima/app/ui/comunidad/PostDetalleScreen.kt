package com.rutaalacima.app.ui.comunidad

import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.outlined.Report
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
    var explorando by mutableStateOf<List<Post>>(emptyList())
        private set
    fun cargarExploracion() = viewModelScope.launch {
        if (post?.brujula.isNullOrBlank()) return@launch
        val muro = runCatching { c.social.feed(com.rutaalacima.app.data.social.FiltroFeed.PARA_TI) }.getOrDefault(emptyList())
        val i = muro.indexOfFirst { it.id == post?.id }
        explorando = (muro.drop(i + 1) + muro.take(i.coerceAtLeast(0))).filter { it.id != post?.id }.distinctBy { it.id }
    }
    fun abrirExplorado(p: Post) {
        post = p
        comentarios = emptyList()
        nuevo = ""
        cargar()
        cargarExploracion()
    }
    fun impulsarOtra(p: Post) = viewModelScope.launch {
        runCatching { c.social.impulsar(p) }.onSuccess {
            explorando = explorando.map { if (it.id == p.id) it.copy(yoImpulse = !p.yoImpulse, impulsos = p.impulsos + if (p.yoImpulse) -1 else 1) else it }
        }
    }
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

    var respondiendo by mutableStateOf<Comentario?>(null)
    fun impulsarComentario(k: Comentario) {
        comentarios = comentarios.map { if (it.id == k.id) it.copy(yoLike = !k.yoLike, likes = (k.likes + if (k.yoLike) -1 else 1).coerceAtLeast(0)) else it }
        viewModelScope.launch { runCatching { c.social.impulsarComentario(k) } }
    }
    fun ocultarComentario(k: Comentario) {
        comentarios = comentarios.map { if (it.id == k.id) it.copy(ocultoAutor = !k.ocultoAutor) else it }
        viewModelScope.launch { runCatching { c.social.ocultarComentario(k, !k.ocultoAutor) } }
    }

    fun comentar() {
        val p = post ?: return
        val t = nuevo
        val padre = respondiendo?.let { it.parentId ?: it.id }
        nuevo = ""; respondiendo = null
        viewModelScope.launch {
            runCatching { c.social.comentar(p, t, padre) }
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
    LaunchedEffect(postId) { vm.cargar(); vm.cargarExploracion() }
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
                Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
                vm.respondiendo?.let { r ->
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.red_respondiendo, r.autor), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.respondiendo = null }) { Icon(Icons.Filled.Close, stringResource(R.string.cancelar)) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(vm.nuevo, { vm.nuevo = it }, placeholder = { Text(stringResource(R.string.escribe_comentario)) },
                        modifier = Modifier.weight(1f), maxLines = 4)
                    IconButton(onClick = vm::comentar, enabled = vm.nuevo.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.comentar), tint = MaterialTheme.colorScheme.primary)
                    }
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
            if (post.brujula.isNotBlank() && vm.explorando.isNotEmpty()) {
                item(key = "explorar-titulo") { Text(stringResource(R.string.rec_explorar), style = MaterialTheme.typography.titleMedium) }
                items(vm.explorando, key = { "explorar-${it.id}" }) { p ->
                    PostCard(p, onImpulsar = { vm.impulsarOtra(p) }, onComentar = { vm.abrirExplorado(p) }, onAbrir = { vm.abrirExplorado(p) })
                }
            }
            item { Text(stringResource(R.string.comentarios_n, vm.comentarios.size), style = MaterialTheme.typography.titleMedium) }
            comentariosConRespuestas(vm.comentarios, vm.post?.propio == true, onResponder = { vm.respondiendo = it },
                onImpulsar = vm::impulsarComentario, onOcultar = vm::ocultarComentario)
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
            DropdownMenuItem(text = { Text(stringResource(R.string.reportar_publicacion)) }, leadingIcon = { Icon(Icons.Outlined.Report, null) },
                onClick = { abierto = false; reportando = true })
            DropdownMenuItem(text = { Text(stringResource(R.string.bloquear_a, autor)) }, leadingIcon = { Icon(Icons.Outlined.Block, null) },
                onClick = { abierto = false; bloqueando = true })
        }
    }
    if (reportando) DialogoReportar(onReportar = onReportar, onCerrar = { reportando = false })
    if (bloqueando) DialogoBloquear(autor, onBloquear = onBloquear, onCerrar = { bloqueando = false })
}

/** Reportar una publicación con su motivo (lo usan el detalle, el muro y Cimas). */
@Composable
internal fun DialogoReportar(onReportar: (String) -> Unit, onCerrar: () -> Unit) {
    val motivos = listOf(R.string.motivo_spam, R.string.motivo_ofensivo, R.string.motivo_acoso, R.string.motivo_falso, R.string.motivo_otro)
        .map { stringResource(it) }
    var elegido by remember { mutableStateOf(motivos.first()) }
    var detalle by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCerrar,
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
        confirmButton = { TextButton(onClick = { onCerrar(); onReportar(listOf(elegido, detalle.trim()).filter { it.isNotEmpty() }.joinToString(": ")) }) {
            Text(stringResource(R.string.mensajes_reportar)) } },
        dismissButton = { TextButton(onClick = onCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/** Bloquear a quien publicó (lo usan el detalle, el muro y Cimas). */
@Composable
internal fun DialogoBloquear(autor: String, onBloquear: () -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.bloquear_a, autor)) },
        text = { Text(stringResource(R.string.bloquear_texto)) },
        confirmButton = { TextButton(onClick = { onCerrar(); onBloquear() }) { Text(stringResource(R.string.mensajes_bloquear), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}
