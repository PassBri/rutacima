@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.rutaalacima.app.ui.comunidad

import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Diversity3
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Close
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notifications
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
    /** Avisos sin leer (la campana). */
    val noLeidos = c.red.noLeidos
    /** Retos vigentes de la comunidad. */
    var retos by mutableStateOf<List<com.rutaalacima.app.data.social.Reto>>(emptyList())
        private set
    init {
        viewModelScope.launch { runCatching { c.red.contarNoLeidos() } }
        viewModelScope.launch { retos = runCatching { c.red.retos() }.getOrDefault(emptyList()) }
        viewModelScope.launch { InsigniasRed.verificados = runCatching { c.red.verificados() }.getOrDefault(emptySet()) }
    }
    private fun recargarRetos() = viewModelScope.launch { retos = c.red.retos() }
    fun unirseReto(id: String) = viewModelScope.launch { c.red.unirseReto(id); recargarRetos() }
    fun marcarReto(id: String) = viewModelScope.launch { c.red.marcarReto(id); recargarRetos() }
    fun salirReto(id: String) = viewModelScope.launch { c.red.salirReto(id); recargarRetos() }

    fun elegirEje(e: String?) { eje = e; cargar() }

    fun alternarGuardado(p: Post) {
        c.social.alternarGuardado(p.id)
        guardados = c.social.guardados()
        if (filtro == FiltroFeed.GUARDADOS) posts = posts.filter { it.id in guardados }
    }

    /** Quedan más publicaciones en el servidor (se cargan al llegar al final). */
    var hayMas by mutableStateOf(false)
        private set
    private var cargadas = 0
    private var cargandoMas = false
    private suspend fun ordenar(f: FiltroFeed, ps: List<Post>): List<Post> {
        if (f != FiltroFeed.PARA_TI) return ps
        val ejes = runCatching { c.respuestas.cargar("red")["red:ejes"].orEmpty().split(",").filter { it.isNotBlank() }.toSet() }.getOrDefault(emptySet())
        return runCatching { c.red.ordenarParaTi(ps, ejes) }.getOrDefault(ps)
    }

    fun cargar(f: FiltroFeed = filtro) {
        filtro = f
        cargando = true
        viewModelScope.launch {
            if (f == FiltroFeed.PARA_TI) launch { tramos = c.social.expedicion(java.time.LocalDate.now().year) }
            runCatching { c.social.feed(f, eje) }
                .onSuccess { cargadas = it.size; hayMas = c.social.enLinea && it.size >= c.social.paginaFeed; posts = ordenar(f, it); error = null }
                .onFailure { error = it.message }
            cargando = false
        }
    }

    /** La página siguiente: "Para ti" ordena cada página sin mover lo que ya viste. */
    fun cargarMas() {
        if (!hayMas || cargandoMas || cargando) return
        cargandoMas = true
        val f = filtro
        viewModelScope.launch {
            runCatching { c.social.feed(f, eje, cargadas) }.onSuccess { nuevos ->
                if (f != filtro) return@onSuccess
                cargadas += nuevos.size; hayMas = nuevos.size >= c.social.paginaFeed
                val ids = posts.map { it.id }.toSet()
                posts = posts + ordenar(f, nuevos.filter { it.id !in ids })
            }
            cargandoMas = false
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

    /** Comentario al que se está respondiendo (null = comentario nuevo). */
    var respondiendo by mutableStateOf<com.rutaalacima.app.data.social.Comentario?>(null)
    fun comentar(p: Post, texto: String) {
        val t = texto.trim(); if (t.isEmpty()) return
        val padre = respondiendo?.let { it.parentId ?: it.id }
        comentarios = comentarios + com.rutaalacima.app.data.social.Comentario("local-${System.currentTimeMillis()}", c.social.nombrePropio(), t, System.currentTimeMillis(), parentId = padre, propio = true)
        posts = posts.map { if (it.id == p.id) it.copy(comentarios = it.comentarios + 1) else it }
        respondiendo = null
        viewModelScope.launch { runCatching { c.social.comentar(p, t, padre) }; comentarios = runCatching { c.social.comentarios(p) }.getOrDefault(comentarios) }
    }
    fun impulsarComentario(k: com.rutaalacima.app.data.social.Comentario) {
        comentarios = comentarios.map { if (it.id == k.id) it.copy(yoLike = !k.yoLike, likes = (k.likes + if (k.yoLike) -1 else 1).coerceAtLeast(0)) else it }
        viewModelScope.launch { runCatching { c.social.impulsarComentario(k) } }
    }
    fun ocultarComentario(k: com.rutaalacima.app.data.social.Comentario) {
        comentarios = comentarios.map { if (it.id == k.id) it.copy(ocultoAutor = !k.ocultoAutor) else it }
        viewModelScope.launch { runCatching { c.social.ocultarComentario(k, !k.ocultoAutor) } }
    }
    fun silenciar(p: Post) {
        posts = posts.filter { it.autorId != p.autorId }
        viewModelScope.launch { runCatching { c.social.silenciar(p.autorId, true) } }
    }

    /** Reportar: deja de verse al instante (y con 3 reportes se oculta para todos hasta revisarla). */
    fun reportar(p: Post, motivo: String) {
        posts = posts.filter { it.id != p.id }
        viewModelScope.launch { runCatching { c.social.reportar(p, motivo) } }
    }

    /** Quién ve una publicación propia: cambia al instante y luego se guarda. */
    fun cambiarVisibilidad(p: Post, v: com.rutaalacima.app.data.social.Visibilidad) {
        posts = posts.map { if (it.id == p.id) it.copy(visibilidad = v) else it }
        viewModelScope.launch { runCatching { c.social.cambiarVisibilidad(p, v) } }
    }

    fun editar(p: Post, texto: String, meta: String) {
        posts = posts.map { if (it.id == p.id) it.copy(texto = texto, metaTitulo = meta) else it }
        viewModelScope.launch { runCatching { c.social.editarTexto(p, texto, meta) } }
    }

    fun eliminar(p: Post) {
        posts = posts.filter { it.id != p.id }
        viewModelScope.launch { runCatching { c.social.eliminar(p) } }
    }

    /** Abre (o crea) la conversación con quien publicó. */
    fun escribir(p: Post, onChat: (String) -> Unit) {
        viewModelScope.launch { runCatching { c.mensajes.abrirCon(p.autorId) }.onSuccess(onChat) }
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
    onChat: (String) -> Unit = {},
    onAvisos: () -> Unit = {},
    onBuscar: () -> Unit = {},
) {
    val vm = rutaViewModel { ComunidadViewModel(it) }
    val noLeidos by vm.noLeidos.collectAsStateWithLifecycle()
    // La primera vez: elegir ejes y seguir a algunas personas antes de ver el muro
    val bienvenida = rutaViewModel { BienvenidaViewModel(it) }
    if (bienvenida.mostrar == true) { PrimerosPasosComunidad(bienvenida, contentPadding) { vm.cargar() }; return }
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
                        val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = pagina.coerceIn(0, vm.posts.lastIndex)) { vm.posts.size + if (vm.hayMas) 0 else 1 }
                        LaunchedEffect(pager) { androidx.compose.runtime.snapshotFlow { pager.currentPage }.collect { pagina = it.coerceAtMost(vm.posts.lastIndex); if (it >= vm.posts.size - 3) vm.cargarMas() } }
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
                    vm.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { com.rutaalacima.app.ui.components.CargandoMarca() }
                    else -> com.rutaalacima.app.ui.components.EstadoVacio(
                        semilla = "comunidad", titulo = stringResource(R.string.vacio_comunidad_titulo),
                        texto = vm.error?.let { stringResource(R.string.error_cargar, it) } ?: stringResource(if (vm.filtro == FiltroFeed.GUARDADOS) R.string.vacio_guardados else R.string.feed_vacio),
                        accion = stringResource(R.string.publicar), onAccion = { onPublicar("LOGRO") },
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                PestanasCimas(vm, onMuro = { volverA = pagina; enMuro = true }, onCordadas = onCordadas, onMensajes = onMensajes, onCuenta = onCuenta, noLeidos = noLeidos, onAvisos = onAvisos)
            }
            comentandoEn?.let { p -> HojaComentarios(vm, p, onCerrar = { comentandoEn = null }, onAbrirDetalle = { comentandoEn = null; onAbrirPost(p.id) }) }
            opcionesDe?.let { p -> HojaOpciones(vm, p, onChat = onChat, onCerrar = { opcionesDe = null }) }
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
    // Al acercarse al final del muro se carga la página siguiente
    LaunchedEffect(lista) {
        androidx.compose.runtime.snapshotFlow { lista.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { if (it >= lista.layoutInfo.totalItemsCount - 4) vm.cargarMas() }
    }
    // M3: jalar hacia abajo para recargar el muro
    // Deslizar para actualizar con el indicador de carga de Material 3 Expressive (cambia de forma mientras carga)
    val estadoActualizar = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    val actualizando = vm.cargando && vm.posts.isNotEmpty()
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = actualizando, onRefresh = { vm.cargar() }, modifier = Modifier.fillMaxSize(), state = estadoActualizar,
        indicator = {
            androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator(
                state = estadoActualizar, isRefreshing = actualizando, modifier = Modifier.align(Alignment.TopCenter))
        },
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
                    androidx.compose.material3.IconButton(onClick = onBuscar) { Icon(Icons.Filled.Search, stringResource(R.string.red_buscar)) }
                    Campana(noLeidos, onAvisos, MaterialTheme.colorScheme.onSurface)
                }
                androidx.compose.material3.FilledTonalButton(onClick = { enMuro = false }, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.Landscape, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.cimas_pantalla_completa))
                }
            }
            // El reto del mes, abierto a toda la comunidad
            vm.retos.firstOrNull()?.let { r -> item(key = "reto") { RetoCard(r, { vm.unirseReto(r.id) }, { vm.marcarReto(r.id) }, { vm.salirReto(r.id) }) } }
            // La cumbre de la semana: toda la comunidad sube la misma montaña
            if (vm.filtro == FiltroFeed.PARA_TI && vm.eje == null && vm.posts.isNotEmpty()) {
                item(key = "cumbre") { CumbreComunidadCard(vm.posts, vm.tramos) }
            }
            if (vm.cargando && vm.posts.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { com.rutaalacima.app.ui.components.CargandoMarca() } }
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
            FilterChip(selected = vm.eje == null, leadingIcon = if (vm.eje == null) { { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, androidx.compose.ui.Modifier.size(androidx.compose.material3.FilterChipDefaults.IconSize)) } } else null,  onClick = { vm.elegirEje(null) }, label = { Text(stringResource(R.string.todos_los_ejes)) })
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
private fun PestanasCimas(vm: ComunidadViewModel, onMuro: () -> Unit, onCordadas: () -> Unit, onMensajes: () -> Unit, onCuenta: () -> Unit, noLeidos: Int = 0, onAvisos: () -> Unit = {}) {
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
            Campana(noLeidos, onAvisos, Color.White)
            androidx.compose.material3.IconButton(onClick = onMensajes) { Icon(com.rutaalacima.app.ui.components.IconosRuta.Radio, stringResource(R.string.mensajes), tint = Color.White) }
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
                vm.cargandoComentarios -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { com.rutaalacima.app.ui.components.CargandoMarca() }
                vm.comentarios.isEmpty() -> Text(stringResource(R.string.sin_comentarios), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp))
                else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    comentariosConRespuestas(vm.comentarios, post.propio, onResponder = { vm.respondiendo = it }, onImpulsar = vm::impulsarComentario, onOcultar = vm::ocultarComentario)
                }
            }
            vm.respondiendo?.let { r ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.red_respondiendo, r.autor), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    androidx.compose.material3.IconButton(onClick = { vm.respondiendo = null }) { Icon(Icons.Filled.Close, stringResource(R.string.cancelar)) }
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

/** "Más" de una cima: copiar; en las propias, quién la ve y borrar; en las de otros, escribir, reportar o bloquear. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HojaOpciones(vm: ComunidadViewModel, post: Post, onChat: (String) -> Unit, onCerrar: () -> Unit) {
    var reportando by androidx.compose.runtime.remember { mutableStateOf(false) }
    var bloqueando by androidx.compose.runtime.remember { mutableStateOf(false) }
    var borrando by androidx.compose.runtime.remember { mutableStateOf(false) }
    var editando by androidx.compose.runtime.remember { mutableStateOf(false) }
    var vis by androidx.compose.runtime.remember(post.id) { mutableStateOf(post.visibilidad) }
    val portapapeles = androidx.compose.ui.platform.LocalClipboardManager.current
    val txtCopiado = stringResource(R.string.texto_copiado)
    val txtReportada = stringResource(R.string.reportada_gracias)
    val txtBloqueado = stringResource(R.string.bloqueado_aviso, post.autorNombre)
    if (editando) {
        var texto by androidx.compose.runtime.remember { mutableStateOf(post.texto) }
        var meta by androidx.compose.runtime.remember { mutableStateOf(post.metaTitulo) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text(stringResource(R.string.red_editar)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(texto, { texto = it.take(2200) }, Modifier.fillMaxWidth(), minLines = 3)
                    androidx.compose.material3.OutlinedTextField(meta, { meta = it.take(120) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.red_meta_relacionada)) })
                }
            },
            confirmButton = { TextButton(onClick = { if (texto.isNotBlank()) { vm.editar(post, texto.trim(), meta.trim()); onCerrar() } }) { Text(stringResource(R.string.guardar)) } },
            dismissButton = { TextButton(onClick = onCerrar) { Text(stringResource(R.string.cancelar)) } },
        )
        return
    }
    if (!reportando && !bloqueando && !borrando) androidx.compose.material3.ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            androidx.compose.material3.ListItem(
                headlineContent = { Text(stringResource(R.string.copiar_texto)) },
                leadingContent = { Icon(Icons.Filled.ContentCopy, null) },
                modifier = Modifier.clickable {
                    portapapeles.setText(androidx.compose.ui.text.AnnotatedString(listOf(post.metaTitulo, post.texto).filter { it.isNotBlank() }.joinToString("\n\n")))
                    com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(txtCopiado); onCerrar()
                },
            )
            if (post.propio) {
                Text(stringResource(R.string.quien_lo_ve), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp))
                val opciones = listOf(
                    com.rutaalacima.app.data.social.Visibilidad.PUBLICA to R.string.visibilidad_publica,
                    com.rutaalacima.app.data.social.Visibilidad.SEGUIDORES to R.string.visibilidad_seguidores,
                    com.rutaalacima.app.data.social.Visibilidad.PRIVADA to R.string.visibilidad_privada,
                )
                androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    opciones.forEachIndexed { i, (v, txt) ->
                        SegmentedButton(
                            selected = vis == v,
                            onClick = { vis = v; vm.cambiarVisibilidad(post, v) },
                            shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(i, opciones.size),
                        ) { Text(stringResource(txt), maxLines = 1) }
                    }
                }
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(R.string.red_editar)) },
                    leadingContent = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.padding(top = 8.dp).clickable { editando = true },
                )
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(R.string.borrar_publicacion), color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.padding(top = 8.dp).clickable { borrando = true },
                )
            } else {
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(R.string.red_silenciar, post.autorNombre)) },
                    leadingContent = { Icon(Icons.Filled.VisibilityOff, null) },
                    modifier = Modifier.clickable { vm.silenciar(post); onCerrar() },
                )
                if (!post.demo) androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(R.string.enviar_mensaje_a, post.autorNombre)) },
                    leadingContent = { Icon(com.rutaalacima.app.ui.components.IconosRuta.Radio, null) },
                    modifier = Modifier.clickable { vm.escribir(post, onChat); onCerrar() },
                )
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(R.string.reportar_publicacion)) },
                    leadingContent = { Icon(Icons.Outlined.Report, null) },
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
    if (borrando) androidx.compose.material3.AlertDialog(
        onDismissRequest = { borrando = false; onCerrar() },
        title = { Text(stringResource(R.string.borrar_publicacion_q)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { vm.eliminar(post); borrando = false; onCerrar() }) {
            Text(stringResource(R.string.eliminar), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { borrando = false; onCerrar() }) { Text(stringResource(R.string.cancelar)) } },
    )
    if (reportando) DialogoReportar(onReportar = { motivo -> vm.reportar(post, motivo); com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(txtReportada) }, onCerrar = { reportando = false; onCerrar() })
    if (bloqueando) DialogoBloquear(post.autorNombre, onBloquear = { vm.bloquear(post); com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(txtBloqueado) }, onCerrar = { bloqueando = false; onCerrar() })
}


/** La campana de avisos con el número de nuevos. */
@Composable
private fun Campana(noLeidos: Int, onClick: () -> Unit, color: Color) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        androidx.compose.material3.BadgedBox(badge = { if (noLeidos > 0) androidx.compose.material3.Badge { Text(if (noLeidos > 99) "99+" else noLeidos.toString()) } }) {
            Icon(Icons.Filled.Notifications, stringResource(R.string.red_avisos), tint = color)
        }
    }
}
