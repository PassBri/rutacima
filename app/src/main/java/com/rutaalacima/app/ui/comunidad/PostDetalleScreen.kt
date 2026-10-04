package com.rutaalacima.app.ui.comunidad

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Delete
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

    fun eliminar(alTerminar: () -> Unit) {
        val p = post ?: return
        viewModelScope.launch { c.social.eliminar(p); alTerminar() }
    }
}

/** Detalle de una publicación con sus comentarios. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetalleScreen(postId: String, onBack: () -> Unit) {
    val vm = rutaViewModel(key = "post-$postId") { PostDetalleViewModel(it, postId) }
    LaunchedEffect(postId) { vm.cargar() }
    val post: Post? = vm.post

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.publicacion)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = {
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
