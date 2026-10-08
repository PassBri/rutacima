package com.rutaalacima.app.ui.comunidad

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.ui.theme.asColor
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
    /** Eje elegido para explorar (null = todos). */
    var eje by mutableStateOf<String?>(null)
        private set
    var guardados by mutableStateOf(c.social.guardados())
        private set
    var posts by mutableStateOf<List<Post>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    val enLinea: Boolean get() = c.social.enLinea

    fun elegirEje(e: String?) { eje = e; cargar() }

    fun alternarGuardado(p: Post) {
        c.social.alternarGuardado(p.id)
        guardados = c.social.guardados()
        if (filtro == FiltroFeed.GUARDADOS) posts = posts.filter { it.id in guardados }
    }

    fun cargar(f: FiltroFeed = filtro) {
        filtro = f
        cargando = true
        viewModelScope.launch {
            runCatching { c.social.feed(f, eje) }
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
    // Vista elegida (se recuerda): lista hacia abajo o Cimas a pantalla completa
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs = androidx.compose.runtime.remember { ctx.getSharedPreferences("comunidad", android.content.Context.MODE_PRIVATE) }
    var cimas by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(prefs.getBoolean("cimas", false)) }
    val cambiarVista: (Boolean) -> Unit = { cimas = it; prefs.edit().putBoolean("cimas", it).apply() }

    if (cimas) {
        val pager = androidx.compose.foundation.pager.rememberPagerState { vm.posts.size }
        Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SelectorVista(cimas, cambiarVista)
                Filtros(vm)
            }
            when {
                vm.cargando && vm.posts.isEmpty() ->
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                vm.posts.isEmpty() ->
                    com.rutaalacima.app.ui.components.EstadoVacio(
                        semilla = "comunidad", titulo = stringResource(R.string.vacio_comunidad_titulo), texto = stringResource(if (vm.filtro == FiltroFeed.GUARDADOS) R.string.vacio_guardados else R.string.feed_vacio),
                        accion = stringResource(R.string.publicar), onAccion = { onPublicar("LOGRO") }, modifier = Modifier.padding(24.dp),
                    )
                else -> CimasFeed(
                    posts = vm.posts, estado = pager,
                    onImpulsar = { vm.impulsar(it) }, onAbrir = { onAbrirPost(it.id) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    padding = PaddingValues(top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 8.dp),
                )
            }
        }
        return
    }

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
        item { SelectorVista(cimas, cambiarVista) }
        // Historias: atajos para publicar cada tipo
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(TipoPost.entries) { t ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { onPublicar(t.name) }.padding(6.dp),
                    ) {
                        Box(
                            Modifier.size(58.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                            contentAlignment = Alignment.Center,
                        ) { Icon(iconoTipo(t), null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                        Spacer(Modifier.height(6.dp))
                        Text(t.texto(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item { Filtros(vm) }
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
                com.rutaalacima.app.ui.components.EstadoVacio(
                    semilla = "comunidad", titulo = stringResource(R.string.vacio_comunidad_titulo), texto = stringResource(if (vm.filtro == FiltroFeed.GUARDADOS) R.string.vacio_guardados else R.string.feed_vacio),
                    accion = stringResource(R.string.publicar), onAccion = { onPublicar("LOGRO") }, modifier = Modifier.padding(24.dp),
                )
            }
        }
        items(vm.posts, key = { it.id }) { p ->
            PostCard(p, onImpulsar = { vm.impulsar(p) }, onComentar = { onAbrirPost(p.id) }, onAbrir = { onAbrirPost(p.id) },
                guardado = p.id in vm.guardados, onGuardar = { vm.alternarGuardado(p) })
        }
    }
}

@Composable
private fun Filtros(vm: ComunidadViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FiltroFeed.entries.forEach { f ->
                FilterChip(
                    selected = vm.filtro == f, onClick = { vm.cargar(f) },
                    label = {
                        Text(stringResource(when (f) {
                            FiltroFeed.PARA_TI -> R.string.filtro_para_ti
                            FiltroFeed.SIGUIENDO -> R.string.filtro_siguiendo
                            FiltroFeed.VISION -> R.string.filtro_vision
                            FiltroFeed.GUARDADOS -> R.string.filtro_guardados
                        }))
                    },
                    leadingIcon = if (f == FiltroFeed.GUARDADOS) { { Icon(Icons.Filled.Bookmark, null, Modifier.size(16.dp)) } } else null,
                )
            }
        }
        // Explorar por eje: como los temas de Instagram o Pinterest, pero con los 6 ejes del método
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = vm.eje == null, onClick = { vm.elegirEje(null) }, label = { Text(stringResource(R.string.todos_los_ejes)) })
            Eje.entries.forEach { e ->
                FilterChip(
                    selected = vm.eje == e.codigo, onClick = { vm.elegirEje(if (vm.eje == e.codigo) null else e.codigo) },
                    label = { Text(e.texto()) },
                    leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(e.color.asColor())) },
                )
            }
        }
    }
}

/** Lista (hacia abajo, como un muro) o Cimas (a pantalla completa, deslizando hacia arriba). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SelectorVista(cimas: Boolean, onCambiar: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf(false to R.string.comunidad_vista_lista, true to R.string.comunidad_vista_cimas).forEachIndexed { i, (valor, texto) ->
            SegmentedButton(
                selected = cimas == valor, onClick = { onCambiar(valor) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                icon = {
                    Icon(if (valor) Icons.Filled.Landscape else Icons.AutoMirrored.Filled.ViewList,
                        null, Modifier.size(18.dp))
                },
            ) { Text(stringResource(texto)) }
        }
    }
}
