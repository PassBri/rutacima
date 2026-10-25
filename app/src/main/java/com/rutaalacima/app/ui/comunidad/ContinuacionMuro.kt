package com.rutaalacima.app.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.FiltroFeed
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.launch

class ContinuacionMuroViewModel(private val c: AppContainer) : ViewModel() {
    var posts by mutableStateOf<List<Post>>(emptyList())
        private set
    fun cargar(brujula: String) = viewModelScope.launch {
        val muro = runCatching { c.social.feed(FiltroFeed.PARA_TI) }.getOrDefault(emptyList())
        val i = muro.indexOfFirst { it.brujula == brujula }
        posts = (muro.drop(i + 1) + muro.take(i.coerceAtLeast(0))).filter { it.brujula != brujula }.distinctBy { it.id }
    }
    fun impulsar(p: Post) = viewModelScope.launch {
        runCatching { c.social.impulsar(p) }.onSuccess {
            posts = posts.map { if (it.id == p.id) it.copy(yoImpulse = !p.yoImpulse, impulsos = p.impulsos + if (p.yoImpulse) -1 else 1) else it }
        }
    }
}

/** Continúa dentro del mismo desplazamiento del visor compartido. */
@Composable
fun ContinuacionMuro(brujula: String) {
    val vm = rutaViewModel(key = "continuacion-${brujula.hashCode()}") { ContinuacionMuroViewModel(it) }
    var seleccionado by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
    LaunchedEffect(brujula) { vm.cargar(brujula) }
    if (vm.posts.isNotEmpty()) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.rec_explorar), style = MaterialTheme.typography.titleMedium)
        vm.posts.forEach { p -> key(p.id) {
            PostCard(p, onImpulsar = { vm.impulsar(p) }, onComentar = { seleccionado = p.id }, onAbrir = { seleccionado = p.id })
        } }
    }
    seleccionado?.let { id ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { seleccionado = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            PostDetalleScreen(id, onBack = { seleccionado = null })
        }
    }
}
