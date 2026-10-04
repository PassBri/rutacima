package com.rutaalacima.app.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.rutaalacima.app.data.social.FiltroFeed
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.data.social.TipoPost
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.i18n.texto
import kotlinx.coroutines.launch

class ComunidadViewModel(private val c: AppContainer) : ViewModel() {
    var filtro by mutableStateOf(FiltroFeed.PARA_TI)
        private set
    var posts by mutableStateOf<List<Post>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    val enLinea: Boolean get() = c.social.enLinea

    fun cargar(f: FiltroFeed = filtro) {
        filtro = f
        cargando = true
        viewModelScope.launch {
            runCatching { c.social.feed(f) }
                .onSuccess { posts = it; error = null }
                .onFailure { error = it.message }
            cargando = false
        }
    }

    /** Impulso optimista: se ve al instante y luego se confirma con el servidor. */
    fun impulsar(p: Post) {
        posts = posts.map { if (it.id == p.id) it.copy(yoImpulse = !p.yoImpulse, impulsos = p.impulsos + if (p.yoImpulse) -1 else 1) else it }
        c.social.recordar(posts)
        viewModelScope.launch { runCatching { c.social.impulsar(p) } }
    }
}

/** Comunidad: el muro donde la gente comparte logros, evidencias, metas y su vision board. */
@Composable
fun ComunidadScreen(
    contentPadding: PaddingValues,
    onAbrirPost: (String) -> Unit,
    onPublicar: (String) -> Unit,
    onCuenta: () -> Unit,
) {
    val vm = rutaViewModel { ComunidadViewModel(it) }
    LaunchedEffect(Unit) { vm.cargar() }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 12.dp, end = 12.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!vm.enLinea) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.modo_demo_titulo), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.modo_demo_texto), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = onCuenta) { Text(stringResource(R.string.crear_cuenta)) }
                    }
                }
            }
        }
        // Historias: atajos para publicar cada tipo
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(TipoPost.entries) { t ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
                        Box(Modifier) {
                            Avatar(nombre = when (t) {
                                TipoPost.LOGRO -> "🏆"; TipoPost.EVIDENCIA -> "📸"; TipoPost.VISION -> "✨"; TipoPost.META -> "🎯"; TipoPost.REFLEXION -> "💭"
                            }, tamano = 58)
                        }
                        TextButton(onClick = { onPublicar(t.name) }, contentPadding = PaddingValues(0.dp)) {
                            Text(t.texto(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroFeed.entries.forEach { f ->
                    FilterChip(
                        selected = vm.filtro == f, onClick = { vm.cargar(f) },
                        label = {
                            Text(stringResource(when (f) {
                                FiltroFeed.PARA_TI -> R.string.filtro_para_ti
                                FiltroFeed.SIGUIENDO -> R.string.filtro_siguiendo
                                FiltroFeed.VISION -> R.string.filtro_vision
                            }))
                        },
                    )
                }
            }
        }
        if (vm.cargando && vm.posts.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        vm.error?.let { e ->
            item {
                Text(stringResource(R.string.error_cargar, e), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { vm.cargar() }) { Text(stringResource(R.string.reintentar)) }
            }
        }
        if (!vm.cargando && vm.posts.isEmpty() && vm.error == null) {
            item {
                Text(stringResource(R.string.feed_vacio), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(24.dp))
            }
        }
        items(vm.posts, key = { it.id }) { p ->
            PostCard(p, onImpulsar = { vm.impulsar(p) }, onComentar = { onAbrirPost(p.id) }, onAbrir = { onAbrirPost(p.id) })
        }
    }
}
