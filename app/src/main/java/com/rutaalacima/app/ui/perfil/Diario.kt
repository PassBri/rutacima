package com.rutaalacima.app.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.data.social.Visibilidad
import com.rutaalacima.app.domain.model.DiarioVida
import com.rutaalacima.app.domain.model.EstadoMeta
import com.rutaalacima.app.ui.components.EstadoVacio
import com.rutaalacima.app.ui.theme.fondoPapel
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.comunidad.PostImagen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiarioViewModel(private val c: AppContainer) : ViewModel() {
    private fun <T> e(f: kotlinx.coroutines.flow.Flow<T>, i: T) = f.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), i)

    val posts: StateFlow<List<Post>> = e(c.social.misPublicaciones, emptyList())
    val cumplidas: StateFlow<List<MetaAnualEntity>> = e(c.planificador.todasLasMetas.map { m -> m.filter { EstadoMeta.from(it.estado) == EstadoMeta.CUMPLIDA } }, emptyList())
    val visibilidades: StateFlow<Map<Int, String>> = e(c.respuestas.observar(DiarioVida.WORKBOOK).map { DiarioVida.visibilidades(it) }, emptyMap())
    val anioNacimiento: StateFlow<Int?> = e(c.perfil.perfil.map { it.anioNacimiento }, null)

    /** Cambia quién ve el año; si quedan recuerdos privados, la pantalla pregunta antes con [privadosDe]. */
    fun compartir(anio: Int, vis: String, incluirPrivados: Boolean) = viewModelScope.launch {
        c.respuestas.guardar(DiarioVida.WORKBOOK, DiarioVida.clave(anio), vis)
        runCatching { c.social.compartirAnio(anio, Visibilidad.valueOf(vis), incluirPrivados) }
    }

    suspend fun privadosDe(anio: Int) = c.social.privadosDe(anio)
}

private fun iconoVis(v: String): ImageVector = when (v) {
    "PUBLICA" -> Icons.Filled.Public
    "SEGUIDORES" -> Icons.Filled.Group
    else -> Icons.Filled.Lock
}

private fun textoVis(v: String): Int = when (v) {
    "PUBLICA" -> R.string.visibilidad_publica
    "SEGUIDORES" -> R.string.visibilidad_seguidores
    else -> R.string.visibilidad_privada
}

/** Mi diario de vida: un álbum por cada año con recuerdos o metas cumplidas, del más reciente al más antiguo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiarioScreen(onBack: () -> Unit, onAnio: (Int) -> Unit, onRecuerdo: (Int) -> Unit) {
    val vm = rutaViewModel { DiarioViewModel(it) }
    val posts by vm.posts.collectAsStateWithLifecycle()
    val cumplidas by vm.cumplidas.collectAsStateWithLifecycle()
    val vis by vm.visibilidades.collectAsStateWithLifecycle()
    val nac by vm.anioNacimiento.collectAsStateWithLifecycle()
    val hoy = java.time.LocalDate.now().year
    val anios = DiarioVida.anios(posts.map { it.anio }.toSet(), cumplidas.map { it.anio }.toSet(), hoy)
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Column { Text(stringResource(R.string.diario_titulo)); Text(stringResource(R.string.diario_sub), style = MaterialTheme.typography.labelMedium) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        if (anios.isEmpty()) {
            EstadoVacio("diario", stringResource(R.string.diario_titulo), stringResource(R.string.diario_vacio),
                stringResource(R.string.album_agregar, hoy), { onRecuerdo(hoy) }, Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 32.dp),
            verticalItemSpacing = 12.dp, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(anios, key = { it }) { a ->
                val delAnio = posts.filter { it.anio == a }
                val portada = delAnio.firstOrNull { it.foto.isNotBlank() } ?: delAnio.firstOrNull()
                val v = vis[a] ?: "PRIVADA"
                Surface(onClick = { onAnio(a) }, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column {
                        Box {
                            if (portada != null) PostImagen(portada, Modifier.fillMaxWidth(), conTexto = false, ratio = if (a % 3 == 0) 3f / 4f else 1f)
                            else Box(Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.EmojiEvents, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                            Icon(iconoVis(v), stringResource(textoVis(v)), Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp)
                                .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.45f)).padding(5.dp), tint = Color.White)
                        }
                        Column(Modifier.padding(12.dp)) {
                            Text("$a", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            val edad = nac?.let { a - it }
                            Text(listOfNotNull(edad?.let { stringResource(R.string.ruta_n_anios, it) }, stringResource(R.string.album_recuerdos, delAnio.size)).joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Álbum de un año propio: quién lo ve, metas cumplidas y los recuerdos en mosaico. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumAnioScreen(anio: Int, onBack: () -> Unit, onAbrirPost: (String) -> Unit, onRecuerdo: (Int) -> Unit) {
    val vm = rutaViewModel { DiarioViewModel(it) }
    val posts by vm.posts.collectAsStateWithLifecycle()
    val cumplidas by vm.cumplidas.collectAsStateWithLifecycle()
    val vis by vm.visibilidades.collectAsStateWithLifecycle()
    val nac by vm.anioNacimiento.collectAsStateWithLifecycle()
    val alcance = rememberCoroutineScope()
    var preguntar by remember { mutableStateOf<Pair<String, Int>?>(null) }
    val actual = vis[anio] ?: "PRIVADA"
    val delAnio = posts.filter { it.anio == anio }
    val metas = cumplidas.filter { it.anio == anio }
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(nac?.let { stringResource(R.string.vida_este_anio, anio, anio - it) } ?: "$anio") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 32.dp),
            verticalItemSpacing = 10.dp, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.album_quien), style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        DiarioVida.VISIBILIDADES.forEachIndexed { i, v ->
                            SegmentedButton(
                                selected = actual == v,
                                onClick = {
                                    if (v == actual) return@SegmentedButton
                                    alcance.launch {
                                        val privados = if (v != "PRIVADA") vm.privadosDe(anio) else 0
                                        if (privados > 0) preguntar = v to privados else vm.compartir(anio, v, false)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(i, DiarioVida.VISIBILIDADES.size),
                                icon = { Icon(iconoVis(v), null, Modifier.size(16.dp)) },
                            ) { Text(stringResource(textoVis(v)), maxLines = 1) }
                        }
                    }
                    Text(stringResource(when (actual) { "PUBLICA" -> R.string.album_nota_publica; "SEGUIDORES" -> R.string.album_nota_seguidores; else -> R.string.album_nota_privada }),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (metas.isNotEmpty()) {
                        Text(stringResource(R.string.album_metas), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
                        metas.forEach { m -> AssistChip(onClick = {}, label = { Text(m.titulo, maxLines = 2) }, leadingIcon = { Icon(Icons.Filled.EmojiEvents, null, Modifier.size(18.dp)) }) }
                    }
                    OutlinedButton(onClick = { onRecuerdo(anio) }) {
                        Icon(Icons.Filled.AddAPhoto, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.album_agregar, anio))
                    }
                }
            }
            items(delAnio, key = { it.id }) { p -> TarjetaRecuerdo(p) { onAbrirPost(p.id) } }
        }
    }
    preguntar?.let { (v, n) ->
        AlertDialog(
            onDismissRequest = { preguntar = null },
            title = { Text(stringResource(R.string.album_incluir_titulo)) },
            text = { Text(stringResource(R.string.album_incluir_texto, n, anio)) },
            confirmButton = { TextButton(onClick = { vm.compartir(anio, v, true); preguntar = null }) { Text(stringResource(R.string.album_incluir_si)) } },
            dismissButton = { TextButton(onClick = { vm.compartir(anio, v, false); preguntar = null }) { Text(stringResource(R.string.album_incluir_no)) } },
        )
    }
}

/** Un recuerdo en el mosaico: la foto (con su alto natural de postal) o la frase como cita. */
@Composable
fun TarjetaRecuerdo(p: Post, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            if (p.foto.isNotBlank()) PostImagen(p, Modifier.fillMaxWidth(), conTexto = false, ratio = if (p.id.hashCode() % 2 == 0) 3f / 4f else 4f / 3f)
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (p.metaTitulo.isNotBlank()) Text(p.metaTitulo, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (p.texto.isNotBlank()) Text(if (p.foto.isBlank()) "“${p.texto}”" else p.texto, style = MaterialTheme.typography.bodySmall,
                    fontStyle = if (p.foto.isBlank()) FontStyle.Italic else FontStyle.Normal, maxLines = 6, overflow = TextOverflow.Ellipsis)
                if (p.visibilidad == Visibilidad.PRIVADA) Icon(Icons.Filled.Lock, stringResource(R.string.visibilidad_privada), Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ====================================================================== Diario de otra persona

class DiarioAjenoViewModel(private val c: AppContainer, private val autorId: String) : ViewModel() {
    var anios by mutableStateOf<List<Pair<Int, Visibilidad>>?>(null)
        private set
    var anio by mutableStateOf<Int?>(null)
        private set
    var album by mutableStateOf<List<Post>?>(null)
        private set

    init {
        viewModelScope.launch {
            anios = runCatching { c.social.aniosCompartidos(autorId) }.getOrDefault(emptyList())
            anios?.firstOrNull()?.let { elegir(it.first) }
        }
    }

    fun elegir(a: Int) {
        anio = a; album = null
        viewModelScope.launch { album = runCatching { c.social.albumDe(autorId, a) }.getOrDefault(emptyList()) }
    }
}

/** El diario de vida de otra persona: solo los años que decidió compartir conmigo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiarioAjenoScreen(autorId: String, nombre: String, onBack: () -> Unit, onAbrirPost: (String) -> Unit) {
    val vm = rutaViewModel(key = "diario-$autorId") { DiarioAjenoViewModel(it, autorId) }
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.diario_de, nombre)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        val anios = vm.anios
        when {
            anios == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            anios.isEmpty() -> EstadoVacio("diario-$autorId", stringResource(R.string.diario_titulo), stringResource(R.string.diario_ajeno_vacio, nombre),
                modifier = Modifier.padding(padding).padding(24.dp))
            else -> LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 32.dp),
                verticalItemSpacing = 10.dp, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        anios.take(8).forEach { (a, v) ->
                            androidx.compose.material3.FilterChip(selected = vm.anio == a, onClick = { vm.elegir(a) }, label = { Text("$a") },
                                leadingIcon = { Icon(iconoVis(v.name), null, Modifier.size(16.dp)) })
                        }
                    }
                }
                val album = vm.album
                if (album == null) item(span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else items(album, key = { it.id }) { p -> TarjetaRecuerdo(p) { onAbrirPost(p.id) } }
            }
        }
    }
}

/** Botón chico para el pie de un año en Mi ruta o en Perfil: abre su álbum. */
@Composable
fun BotonAlbum(anio: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.AddAPhoto, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.album_ver, anio), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
