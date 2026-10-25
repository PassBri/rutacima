package com.rutaalacima.app.ui.perfil

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.Cascada
import com.rutaalacima.app.data.local.EvaluacionEjesEntity
import com.rutaalacima.app.data.local.PerfilEntity
import com.rutaalacima.app.data.local.puntajes
import com.rutaalacima.app.data.local.total
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.data.social.Visibilidad
import com.rutaalacima.app.ui.components.IconosRuta
import com.rutaalacima.app.ui.components.RadarEjes
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.comunidad.Avatar
import com.rutaalacima.app.ui.comunidad.PostImagen
import com.rutaalacima.app.ui.i18n.Textos
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PerfilViewModel(private val c: AppContainer) : ViewModel() {
    val sesion = c.supabase.sesion
    var siguiendo by mutableStateOf<Int?>(null)
        private set
    var seguidores by mutableStateOf<Int?>(null)
        private set
    var impulsos by mutableStateOf<Int?>(null)
        private set
    var guardadas by mutableStateOf<List<Post>>(emptyList())
        private set
    private var cargaSocial: Job? = null
    fun cargarSocial() {
        cargaSocial?.cancel()
        cargaSocial = viewModelScope.launch {
            val siguiendoRemoto = async { c.social.contarSiguiendo() }
            val seguidoresRemoto = async { c.social.contarSeguidores() }
            val impulsosRemoto = async { c.social.contarImpulsos() }
            val guardadasRemoto = async { runCatching { c.social.publicacionesPorIds(c.social.guardados()) }.getOrDefault(emptyList()) }
            siguiendo = siguiendoRemoto.await()
            seguidores = seguidoresRemoto.await()
            impulsos = impulsosRemoto.await()
            guardadas = guardadasRemoto.await()
        }
    }
    fun cambiarMeta(anios: Int) = viewModelScope.launch { c.perfil.actualizar { it.copy(esperanzaVida = anios) } }
    private fun <T> estado(f: kotlinx.coroutines.flow.Flow<T>, i: T) = f.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), i)
    val perfil: StateFlow<PerfilEntity> = estado(c.perfil.perfil, PerfilEntity())
    val posts: StateFlow<List<Post>> = estado(c.social.misPublicaciones, emptyList())
    val evaluacion: StateFlow<EvaluacionEjesEntity?> = estado(c.ejes.ultima, null)
    val cascada: StateFlow<Cascada?> = estado(c.cascada.cascada, null)
    val usuario: String get() = c.social.usuarioPropio()
    val frasesAbiertas: StateFlow<Set<Int>> = c.frases.desbloqueadas
    fun ponerFoto(t: com.rutaalacima.app.data.FotosPerfil.Tipo, bytes: ByteArray) = c.appScope.launch { c.fotosPerfil.ponerBytes(t, bytes) }
    fun quitarFoto(t: com.rutaalacima.app.data.FotosPerfil.Tipo) = c.appScope.launch { c.fotosPerfil.quitar(t) }
    fun guardarQuienSoy(texto: String) = viewModelScope.launch { c.perfil.actualizar { it.copy(quienSoy = texto) } }
}

/** Perfil: quién soy, mi cumbre, mis publicaciones, mi vida por años, mi vision board y mis ejes. */
@Composable
fun PerfilScreen(
    contentPadding: PaddingValues,
    onAbrirPost: (String) -> Unit,
    onPublicar: (String) -> Unit,
    onEvaluarEjes: () -> Unit,
    onAjustes: () -> Unit,
    onFrases: () -> Unit = {},
    onVision: () -> Unit = {},
    onIrA: (String) -> Unit = {},
) {
    val vm = rutaViewModel { PerfilViewModel(it) }
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val posts by vm.posts.collectAsStateWithLifecycle()
    val eval by vm.evaluacion.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(posts, tab) { vm.cargarSocial() }
    LifecycleResumeEffect(sesion?.userId) {
        vm.cargarSocial()
        onPauseOrDispose { }
    }
    // Foto de perfil y portada: elegir → editar (recortar, girar, filtro) → guardar
    var tipoFoto by rememberSaveable { mutableStateOf(com.rutaalacima.app.data.FotosPerfil.Tipo.PERFIL) }
    var editando by remember { mutableStateOf<android.net.Uri?>(null) }
    val elegirFoto = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) editando = uri }
    editando?.let { uri ->
        val perfilFoto = tipoFoto == com.rutaalacima.app.data.FotosPerfil.Tipo.PERFIL
        com.rutaalacima.app.ui.components.EditorFoto(
            uri,
            formatos = if (perfilFoto) com.rutaalacima.app.ui.components.FormatosFoto.PERFIL else com.rutaalacima.app.ui.components.FormatosFoto.PORTADA,
            ladoMax = tipoFoto.ladoMax,
            onListo = { bytes -> vm.ponerFoto(tipoFoto, bytes); editando = null },
            onCancelar = { editando = null },
        )
    }
    LazyColumn(
        contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Cabecera centrada con la identidad y las cifras sociales.
        item {
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.clip(androidx.compose.foundation.shape.CircleShape).clickable {
                        tipoFoto = com.rutaalacima.app.data.FotosPerfil.Tipo.PERFIL
                        elegirFoto.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }.semantics { contentDescription = ctx.getString(R.string.mv_cambiar_foto) }) {
                        Avatar(perfil.nombre, perfil.fotoPerfilUrl.ifBlank { perfil.fotoPerfil }, tamano = 96)
                    }
                    Text(perfil.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (vm.usuario.isNotBlank()) Text("@${vm.usuario}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Dato(if (sesion == null) null else vm.siguiendo, stringResource(R.string.filtro_siguiendo))
                        Dato(if (sesion == null) null else vm.seguidores, stringResource(R.string.visibilidad_seguidores))
                        Dato(if (sesion == null) null else vm.impulsos, stringResource(R.string.perfil_impulsos))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onAjustes) { Text(stringResource(R.string.perfil_editar)) }
                        val invitacion = stringResource(R.string.perfil_invitacion, vm.usuario).replace("https://suroute.com/descargar.html", com.rutaalacima.app.data.social.EnlacesRed.invitacion(vm.usuario))
                        IconButton(onClick = {
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, invitacion)
                            }, null))
                        }) { Icon(Icons.Filled.PersonAdd, stringResource(R.string.perfil_invitar)) }
                    }
                    if (perfil.cumbreFrase.isNotBlank()) Text(perfil.cumbreFrase, style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Box(Modifier.padding(horizontal = 16.dp)) { com.rutaalacima.app.ui.ruta.QuienSoyCard(perfil.quienSoy) { vm.guardarQuienSoy(it) } } }
        // Accesos agrupados: tu ascenso, las personas que te acompañan y lo demás
        // Si la cuenta está suspendida, la persona lo ve aquí con el motivo y cómo pedir revisión
        item { com.rutaalacima.app.ui.moderacion.AvisoSuspension(Modifier.padding(horizontal = 16.dp)) }
        item { AccesosPerfil(onIrA) }
        item {
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                val pestañas = listOf(
                    R.string.perfil_tab_publicaciones to Icons.Outlined.GridOn,
                    R.string.perfil_privadas to Icons.Outlined.Lock,
                    R.string.perfil_guardadas to Icons.Outlined.BookmarkBorder,
                    R.string.perfil_tab_ejes to IconosRuta.Ejes,
                )
                pestañas.forEachIndexed { i, (titulo, icono) ->
                    val descripcion = stringResource(titulo)
                    Tab(selected = tab == i, onClick = { tab = i }, modifier = Modifier.semantics { contentDescription = descripcion },
                        icon = { Icon(icono, null) })
                }
            }
        }
        when (tab) {
            0 -> cuadricula(posts.filter { it.visibilidad != Visibilidad.PRIVADA }, onAbrirPost, vacio = R.string.sin_publicaciones) { onPublicar("LOGRO") }
            1 -> cuadricula(posts.filter { it.visibilidad == Visibilidad.PRIVADA }, onAbrirPost, privada = true, vacio = R.string.perfil_privadas_vacio) { onPublicar("LOGRO") }
            2 -> cuadricula(vm.guardadas, onAbrirPost, vacio = R.string.perfil_guardadas_vacio) {}
            else -> item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    val e = eval
                    if (e == null) {
                        Text(stringResource(R.string.ejes_sin_evaluar), style = MaterialTheme.typography.bodyLarge)
                    } else {
                        RadarEjes(e.puntajes())
                        Text("${e.total()}/60", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(Textos.interpretacionTotal(e.total())), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onEvaluarEjes, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.evaluar_mis_ejes)) }
                }
            }
        }
    }
}

@Composable
private fun Dato(n: Int?, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(n?.let { java.text.NumberFormat.getIntegerInstance().format(it) } ?: stringResource(R.string.perfil_sin_cifra), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

/** Rejilla de tres columnas con fotos verticales y el indicador de cada pestaña. */
private fun androidx.compose.foundation.lazy.LazyListScope.cuadricula(
    posts: List<Post>,
    onAbrir: (String) -> Unit,
    columnas: Int = 3,
    privada: Boolean = false,
    vacio: Int,
    onCrear: () -> Unit,
) {
    if (posts.isEmpty()) {
        item {
            RutaCard(Modifier.padding(horizontal = 16.dp), onClick = onCrear) {
                Text(stringResource(vacio), style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }
    items(posts.chunked(columnas)) { fila ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            fila.forEach { p ->
                Box(Modifier.weight(1f).aspectRatio(9f / 16f).clickable { onAbrir(p.id) }) {
                    PostImagen(p, Modifier.fillMaxWidth(), conTexto = true, ratio = 9f / 16f)
                    Box(Modifier.align(Alignment.BottomStart).padding(4.dp).background(Color.Black.copy(alpha = .55f), RoundedCornerShape(4.dp)).padding(3.dp)) {
                        if (privada) Icon(Icons.Filled.Lock, stringResource(R.string.perfil_privadas), tint = Color.White, modifier = Modifier.size(16.dp))
                        else Text(stringResource(R.string.perfil_impulsos_n, p.impulsos), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}


@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AccesosPerfil(onIrA: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val contenedor = androidx.compose.runtime.remember { (ctx.applicationContext as com.rutaalacima.app.RutaApp).container }
    val sesion by contenedor.supabase.sesion.collectAsStateWithLifecycle()
    var modero by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(sesion?.userId) { modero = contenedor.moderacion.esModerador() }
    data class Acceso(val texto: Int, val icono: androidx.compose.ui.graphics.vector.ImageVector, val ruta: String)
    val grupos = listOf(
        R.string.perfil_grupo_ascenso to listOf(
            Acceso(R.string.metas, com.rutaalacima.app.ui.components.IconosRuta.Metas, com.rutaalacima.app.ui.Rutas.planificador(0)),
            Acceso(R.string.constancia_titulo, Icons.Filled.LocalFireDepartment, com.rutaalacima.app.ui.Rutas.CONSTANCIA),
            Acceso(R.string.diario_titulo, Icons.Filled.PhotoLibrary, com.rutaalacima.app.ui.Rutas.DIARIO),
            Acceso(R.string.carta_titulo, Icons.Filled.Drafts, com.rutaalacima.app.ui.Rutas.CARTA),
            Acceso(R.string.revision_semana, Icons.Filled.EventRepeat,
                com.rutaalacima.app.ui.Rutas.revision(com.rutaalacima.app.domain.model.Revision.Tipo.SEMANA, java.time.LocalDate.now())),
        ),
        R.string.perfil_grupo_personas to listOf(
            Acceso(R.string.mensajes, com.rutaalacima.app.ui.components.IconosRuta.Radio, com.rutaalacima.app.ui.Rutas.MENSAJES),
            Acceso(R.string.coach_vida, Icons.Filled.Hiking, com.rutaalacima.app.ui.Rutas.COACH_VIDA),
            Acceso(R.string.cordadas, Icons.Filled.Diversity3, com.rutaalacima.app.ui.Rutas.CORDADAS),
        ),
        R.string.perfil_grupo_mas to listOfNotNull(
            Acceso(R.string.web_titulo, Icons.Filled.Computer, com.rutaalacima.app.ui.Rutas.WEB),
            Acceso(R.string.plan_titulo, Icons.Filled.Terrain, com.rutaalacima.app.ui.Rutas.PLAN),
            Acceso(R.string.ajustes, Icons.Filled.Settings, com.rutaalacima.app.ui.Rutas.AJUSTES),
            // Solo para cuentas nombradas como moderadoras en el servidor
            if (modero) Acceso(R.string.moderacion_titulo, Icons.Filled.Gavel, com.rutaalacima.app.ui.Rutas.MODERACION) else null,
        ),
    )
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        grupos.forEach { (titulo, accesos) ->
            Text(stringResource(titulo).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                accesos.forEach { a ->
                    androidx.compose.material3.AssistChip(
                        onClick = { onIrA(a.ruta) }, label = { Text(stringResource(a.texto)) },
                        leadingIcon = { Icon(a.icono, null, Modifier.size(18.dp)) },
                    )
                }
            }
        }
    }
}
