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
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Block
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
    /** Puntos por tramo de la expedición según el servidor (null = calcular con el muro cargado). */
    var tramos by mutableStateOf<List<Long>?>(null)
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
            if (f == FiltroFeed.PARA_TI) launch { tramos = c.social.expedicion(java.time.LocalDate.now().year) }
            runCatching { c.social.feed(f, eje) }
                .onSuccess { posts = it; error = null }
                .onFailure { error = it.message }
            cargando = false
        }
    }

    // Comentarios en una hoja encima de Cimas (sin salir del feed)
    var comentarios by mutableStateOf<List<com.rutaalacima.app.data.social.Comentario>>(emptyList())
        private set
    var cargandoComentarios by mutableStateOf(false)
        private set

    fun cargarComentarios(p: Post) {
        comentarios = emptyList(); cargandoComentarios = true
        viewModelScope.launch { comentarios = runCatching { c.social.comentarios(p) }.getOrDefault(emptyList()); cargandoComentarios = false }
    }

    fun comentar(p: Post, texto: String) {
        val t = texto.trim(); if (t.isEmpty()) return
        comentarios = comentarios + com.rutaalacima.app.data.social.Comentario("local-${System.currentTimeMillis()}", c.social.nombrePropio(), t, System.currentTimeMillis())
        posts = posts.map { if (it.id == p.id) it.copy(comentarios = it.comentarios + 1) else it }
        viewModelScope.launch { runCatching { c.social.comentar(p, t) } }
    }

    /** Reportar: deja de verse al instante (y con 3 reportes se oculta para todos hasta revisarla). */
    fun reportar(p: Post, motivo: String) {
        posts = posts.filter { it.id != p.id }
        viewModelScope.launch { runCatching { c.social.reportar(p, motivo) } }
    }

    /** Bloquear: desaparecen todas las publicaciones de esa persona. */
    fun bloquear(p: Post) {
        posts = posts.filter { it.autorId != p.autorId }
        viewModelScope.launch { runCatching { c.social.bloquear(p) } }
    }

    /** Impulso optimista: se ve al instante y luego se confirma con el servidor. */
    fun impulsar(p: Post) {
        posts = posts.map { if (it.id == p.id) it.copy(yoImpulse = !p.yoImpulse, impulsos = p.impulsos + if (p.yoImpulse) -1 else 1) else it }
        c.social.recordar(posts)
        viewModelScope.launch { runCatching { c.social.impulsar(p) } }
    }
}

/** Comunidad: el muro donde la gente comparte logros, evidencias, metas y su vision board. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ComunidadScreen(
    contentPadding: PaddingValues,
    onAbrirPost: (String) -> Unit,
    onPublicar: (String) -> Unit,
    onCuenta: () -> Unit,
    /** Tocar el nombre de una persona abre su diario de vida (los años que comparte). */
    onAutor: (String, String) -> Unit = { _, _ -> },
    onCordadas: () -> Unit = {},
    onMensajes: () -> Unit = {},
    onHoy: () -> Unit = {},
) {
    val vm = rutaViewModel { ComunidadViewModel(it) }
    LaunchedEffect(Unit) { vm.cargar() }
    // La Comunidad se abre a pantalla completa (Cimas), como los videos cortos. El muro (lista, filtros
    // por eje, la expedición y los atajos para publicar) queda a un toque, arriba a la izquierda.
    var enMuro by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var pagina by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val lista = androidx.compose.foundation.lazy.rememberLazyListState()
    var volverA by androidx.compose.runtime.remember { mutableStateOf<Int?>(null) }
    val vista = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(enMuro) {
        com.rutaalacima.app.ui.theme.Inmersivo.activo = !enMuro
        // Íconos claros en la barra de estado mientras se ve Cimas (fondo de noche)
        var ctx = vista.context
        while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) ctx = ctx.baseContext
        val controlador = (ctx as? android.app.Activity)?.window?.let { androidx.core.view.WindowCompat.getInsetsController(it, vista) }
        val antes = controlador?.isAppearanceLightStatusBars
        if (!enMuro) controlador?.isAppearanceLightStatusBars = false
        onDispose {
            com.rutaalacima.app.ui.theme.Inmersivo.activo = false
            if (antes != null) controlador?.isAppearanceLightStatusBars = antes
        }
    }
    androidx.activity.compose.BackHandler(enabled = enMuro) { enMuro = false }
    var comentandoEn by androidx.compose.runtime.remember { mutableStateOf<Post?>(null) }
    var opcionesDe by androidx.compose.runtime.remember { mutableStateOf<Post?>(null) }
    if (!enMuro) {
        com.rutaalacima.app.ui.theme.TemaNoche {
            Box(Modifier.fillMaxSize().background(Color(0xFF060A12)).padding(bottom = contentPadding.calculateBottomPadding())) {
                when {
                    vm.posts.isNotEmpty() -> {
                        val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = pagina.coerceIn(0, vm.posts.lastIndex)) { vm.posts.size + 1 }
                        LaunchedEffect(pager) { androidx.compose.runtime.snapshotFlow { pager.currentPage }.collect { pagina = it.coerceAtMost(vm.posts.lastIndex) } }
                        CimasFeed(
                            posts = vm.posts, estado = pager,
                            onImpulsar = { vm.impulsar(it) },
                            onAbrir = { comentandoEn = it; vm.cargarComentarios(it) },
                            modifier = Modifier.fillMaxSize(),
                            guardados = vm.guardados, onGuardar = { vm.alternarGuardado(it) },
                            onAutor = { onAutor(it.autorId, it.autorNombre) },
                            onMas = { opcionesDe = it },
                            onFin = onHoy,
                        )
                    }
                    vm.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else -> com.rutaalacima.app.ui.components.EstadoVacio(
                        semilla = "comunidad", titulo = stringResource(R.string.vacio_comunidad_titulo),
                        texto = vm.error?.let { stringResource(R.string.error_cargar, it) } ?: stringResource(if (vm.filtro == FiltroFeed.GUARDADOS) R.string.vacio_guardados else R.string.feed_vacio),
                        accion = stringResource(R.string.publicar), onAccion = { onPublicar("LOGRO") },
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                PestanasCimas(vm, onMuro = { volverA = pagina; enMuro = true }, onCordadas = onCordadas, onMensajes = onMensajes, onCuenta = onCuenta)
            }
            comentandoEn?.let { p -> HojaComentarios(vm, p, onCerrar = { comentandoEn = null }, onAbrirDetalle = { comentandoEn = null; onAbrirPost(p.id) }) }
            opcionesDe?.let { p -> HojaOpciones(vm, p, onCerrar = { opcionesDe = null }) }
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
    // M3: jalar hacia abajo para recargar el muro
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = vm.cargando && vm.posts.isNotEmpty(), onRefresh = { vm.cargar() }, modifier = Modifier.fillMaxSize(),
    ) {
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
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) { Icon(iconoTipo(t), null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(26.dp)) }
                            Spacer(Modifier.height(6.dp))
                            Text(t.texto(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { Filtros(vm) }
                }
                androidx.compose.material3.FilledTonalButton(onClick = { enMuro = false }, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.Landscape, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.cimas_pantalla_completa))
                }
            }
            // La cumbre de la semana: toda la comunidad sube la misma montaña
            if (vm.filtro == FiltroFeed.PARA_TI && vm.eje == null && vm.posts.isNotEmpty()) {
                item(key = "cumbre") { CumbreComunidadCard(vm.posts, vm.tramos) }
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
                PostCard(p, onImpulsar = { vm.impulsar(p) }, onComentar = { onAbrirPost(p.id) }, onAbrir = { pagina = i; enMuro = false },
                    guardado = p.id in vm.guardados, onGuardar = { vm.alternarGuardado(p) }, onAutor = { onAutor(p.autorId, p.autorNombre) })
            }
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


/**
 * Arriba de Cimas: el muro a la izquierda, las pestañas al centro (Siguiendo · Para ti · Visión) con un
 * subrayado dorado fino, y las cordadas y los mensajes a la derecha. Todo sobre un velo para leerse.
 */
@Composable
private fun PestanasCimas(vm: ComunidadViewModel, onMuro: () -> Unit, onCordadas: () -> Unit, onMensajes: () -> Unit, onCuenta: () -> Unit) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(onClick = onMuro) {
                Icon(Icons.AutoMirrored.Filled.ViewList, stringResource(R.string.comunidad_vista_lista), tint = Color.White)
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                listOf(FiltroFeed.SIGUIENDO to R.string.filtro_siguiendo, FiltroFeed.PARA_TI to R.string.filtro_para_ti, FiltroFeed.VISION to R.string.filtro_vision).forEach { (f, nombre) ->
                    val activo = vm.filtro == f
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp)).clickable { vm.cargar(f) }.padding(horizontal = 10.dp, vertical = 8.dp)
                            .semantics { selected = activo },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(nombre), color = if (activo) Color.White else Color.White.copy(alpha = 0.66f),
                            style = MaterialTheme.typography.titleSmall, fontWeight = if (activo) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.width(22.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(if (activo) MaterialTheme.colorScheme.secondary else Color.Transparent))
                    }
                }
            }
            androidx.compose.material3.IconButton(onClick = onCordadas) { Icon(Icons.Filled.Diversity3, stringResource(R.string.cordadas), tint = Color.White) }
            androidx.compose.material3.IconButton(onClick = onMensajes) { Icon(Icons.AutoMirrored.Filled.Chat, stringResource(R.string.mensajes), tint = Color.White) }
        }
        if (!vm.enLinea) {
            Text(
                stringResource(R.string.modo_demo_titulo), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(50)).clickable(onClick = onCuenta)
                    .background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** Comentarios encima de Cimas (hoja inferior M3): se lee, se escribe y se vuelve al mismo lugar del feed. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HojaComentarios(vm: ComunidadViewModel, post: Post, onCerrar: () -> Unit, onAbrirDetalle: () -> Unit) {
    var texto by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    // La hoja se abre completa y se apoya encima del teclado: lo que se escribe siempre se ve
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets.ime.union(androidx.compose.foundation.layout.WindowInsets.navigationBars) },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.comentarios_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)
                    .semantics { heading() })
                TextButton(onClick = onAbrirDetalle) { Text(post.autorNombre, maxLines = 1) }
            }
            when {
                vm.cargandoComentarios -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                vm.comentarios.isEmpty() -> Text(stringResource(R.string.sin_comentarios), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp))
                else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vm.comentarios, key = { it.id }) { c ->
                        Row(verticalAlignment = Alignment.Top) {
                            Avatar(c.autor, tamano = 32)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.autor, style = MaterialTheme.typography.labelLarge)
                                Text(c.texto, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(texto, { texto = it.take(1000) }, placeholder = { Text(stringResource(R.string.escribe_comentario)) },
                    modifier = Modifier.weight(1f), maxLines = 4, shape = MaterialTheme.shapes.extraLarge)
                androidx.compose.material3.IconButton(onClick = { vm.comentar(post, texto); texto = "" }, enabled = texto.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.comentar), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** "Más" de una cima: reportar, bloquear y copiar el texto, siempre a un toque. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HojaOpciones(vm: ComunidadViewModel, post: Post, onCerrar: () -> Unit) {
    var reportando by androidx.compose.runtime.remember { mutableStateOf(false) }
    var bloqueando by androidx.compose.runtime.remember { mutableStateOf(false) }
    val portapapeles = androidx.compose.ui.platform.LocalClipboardManager.current
    val txtCopiado = stringResource(R.string.texto_copiado)
    val txtReportada = stringResource(R.string.reportada_gracias)
    val txtBloqueado = stringResource(R.string.bloqueado_aviso, post.autorNombre)
    if (!reportando && !bloqueando) androidx.compose.material3.ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            androidx.compose.material3.ListItem(
                headlineContent = { Text(stringResource(R.string.copiar_texto)) },
                leadingContent = { Icon(Icons.Filled.ContentCopy, null) },
                modifier = Modifier.clickable {
                    portapapeles.setText(androidx.compose.ui.text.AnnotatedString(listOf(post.metaTitulo, post.texto).filter { it.isNotBlank() }.joinToString("\n\n")))
                    com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(txtCopiado); onCerrar()
                },
            )
            if (!post.propio) {
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(R.string.reportar_publicacion)) },
                    leadingContent = { Icon(Icons.Outlined.Flag, null) },
                    modifier = Modifier.clickable { reportando = true },
                )
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(R.string.bloquear_a, post.autorNombre), color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Outlined.Block, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { bloqueando = true },
                )
            }
        }
    }
    if (reportando) DialogoReportar(onReportar = { motivo -> vm.reportar(post, motivo); com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(txtReportada) }, onCerrar = { reportando = false; onCerrar() })
    if (bloqueando) DialogoBloquear(post.autorNombre, onBloquear = { vm.bloquear(post); com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(txtBloqueado) }, onCerrar = { bloqueando = false; onCerrar() })
}
