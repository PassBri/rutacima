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
import androidx.compose.foundation.lazy.itemsIndexed
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
    // Una sola Comunidad: el muro para recorrer y, al tocar una publicación, Cimas a pantalla
    // completa desde esa misma publicación (como Instagram: del muro a la vista inmersiva y de vuelta).
    var cimasDesde by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Int?>(null) }
    val lista = androidx.compose.foundation.lazy.rememberLazyListState()
    var volverA by androidx.compose.runtime.remember { mutableStateOf<Int?>(null) }
    val desde = cimasDesde
    if (desde != null && vm.posts.isNotEmpty()) {
        val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = desde.coerceIn(0, vm.posts.lastIndex)) { vm.posts.size }
        val cerrar = { volverA = cimasDesde; cimasDesde = null }
        androidx.activity.compose.BackHandler(onBack = cerrar)
        Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            CimasFeed(
                posts = vm.posts, estado = pager,
                onImpulsar = { vm.impulsar(it) }, onAbrir = { onAbrirPost(it.id) },
                modifier = Modifier.fillMaxSize(),
                padding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 8.dp),
            )
            androidx.compose.material3.FilledTonalIconButton(onClick = cerrar, modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 20.dp)) {
                Icon(Icons.AutoMirrored.Filled.ViewList, stringResource(R.string.comunidad_vista_lista))
            }
        }
        // Al volver, el muro queda en la publicación que estabas viendo
        LaunchedEffect(pager) {
            androidx.compose.runtime.snapshotFlow { pager.currentPage }.collect { cimasDesde = it }
        }
        return
    }

    // Al volver de Cimas, el muro se ubica en la publicación que estabas viendo
    LaunchedEffect(volverA) {
        val i = volverA ?: return@LaunchedEffect
        val cabecera = (if (!vm.enLinea) 1 else 0) + 2 + (if (vm.error != null) 1 else 0) +
            (if (vm.filtro == FiltroFeed.PARA_TI && vm.eje == null) 1 else 0)
        lista.scrollToItem(cabecera + i)
        volverA = null
    }
    LazyColumn(
        state = lista,
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
        // La cumbre de la semana: toda la comunidad sube la misma montaña
        if (vm.filtro == FiltroFeed.PARA_TI && vm.eje == null && vm.posts.isNotEmpty()) {
            item(key = "cumbre") { CumbreComunidadCard(vm.posts) }
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
                com.rutaalacima.app.ui.components.EstadoVacio(
                    semilla = "comunidad", titulo = stringResource(R.string.vacio_comunidad_titulo), texto = stringResource(if (vm.filtro == FiltroFeed.GUARDADOS) R.string.vacio_guardados else R.string.feed_vacio),
                    accion = stringResource(R.string.publicar), onAccion = { onPublicar("LOGRO") }, modifier = Modifier.padding(24.dp),
                )
            }
        }
        itemsIndexed(vm.posts, key = { _, p -> p.id }) { i, p ->
            PostCard(p, onImpulsar = { vm.impulsar(p) }, onComentar = { onAbrirPost(p.id) }, onAbrir = { cimasDesde = i },
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

