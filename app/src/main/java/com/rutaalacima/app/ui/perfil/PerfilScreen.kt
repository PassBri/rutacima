package com.rutaalacima.app.ui.perfil

import androidx.compose.material.icons.filled.FormatQuote
import kotlinx.coroutines.launch
import com.rutaalacima.app.util.hoy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
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
import com.rutaalacima.app.data.social.TipoPost
import com.rutaalacima.app.ui.comunidad.Avatar
import com.rutaalacima.app.ui.i18n.Textos
import com.rutaalacima.app.ui.comunidad.PostImagen
import com.rutaalacima.app.ui.comunidad.TipoBadge
import com.rutaalacima.app.ui.components.MontanaArte
import com.rutaalacima.app.ui.components.RadarEjes
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PerfilViewModel(private val c: AppContainer) : ViewModel() {
    fun cambiarMeta(anios: Int) = viewModelScope.launch { c.perfil.actualizar { it.copy(esperanzaVida = anios) } }
    private fun <T> estado(f: kotlinx.coroutines.flow.Flow<T>, i: T) = f.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), i)
    val perfil: StateFlow<PerfilEntity> = estado(c.perfil.perfil, PerfilEntity())
    val posts: StateFlow<List<Post>> = estado(c.social.misPublicaciones, emptyList())
    val evaluacion: StateFlow<EvaluacionEjesEntity?> = estado(c.ejes.ultima, null)
    val cascada: StateFlow<Cascada?> = estado(c.cascada.cascada, null)
    val usuario: String = c.social.usuarioPropio()
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
    val posts by vm.posts.collectAsStateWithLifecycle()
    val eval by vm.evaluacion.collectAsStateWithLifecycle()
    val cascada by vm.cascada.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
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
    // Calendario de vida: década, año y mes seleccionados
    var anioSel by rememberSaveable { mutableStateOf<Int?>(hoy().year) }
    var mesSel by rememberSaveable { mutableStateOf<Int?>(null) }
    val logros = posts.count { it.tipo == TipoPost.LOGRO }
    val metasCumplidas = cascada?.let { c ->
        (c.propositos.flatMap { it.anios } + c.aniosSueltos).count { it.avance >= 1f } +
            (c.propositos.flatMap { p -> p.anios.flatMap { it.meses } } + c.aniosSueltos.flatMap { it.meses } + c.mesesSueltos).count { it.avance >= 1f }
    } ?: 0

    LazyColumn(
        contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Perfil como una página personal: portada, foto, nombre, edad, la cumbre y «Quién soy»
        item {
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)) {
                val nac = perfil.anioNacimiento?.let { java.time.LocalDate.of(it, perfil.mesNacimiento ?: 1, 1) }
                val hoyF = java.time.LocalDate.now()
                com.rutaalacima.app.ui.ruta.MiVidaCabecera(
                    perfil,
                    edad = nac?.let { java.time.Period.between(it, hoyF).years },
                    diaDeVida = nac?.let { (java.time.temporal.ChronoUnit.DAYS.between(it, hoyF) + 1).toInt() },
                    onFoto = { t -> tipoFoto = t; elegirFoto.launch(androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onQuitar = { vm.quitarFoto(it) },
                )
                val abiertas by vm.frasesAbiertas.collectAsStateWithLifecycle()
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Dato(posts.size, stringResource(R.string.publicaciones))
                    Dato(logros, stringResource(R.string.logros))
                    Dato(metasCumplidas, stringResource(R.string.metas_cumplidas))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (vm.usuario.isNotBlank()) Text("@${vm.usuario}", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    else Spacer(Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = onFrases) {
                        Icon(Icons.Filled.FormatQuote, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.mis_frases_n, abiertas.size), style = MaterialTheme.typography.labelLarge)
                    }
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
                listOf(R.string.perfil_tab_publicaciones, R.string.perfil_tab_ejes)
                    .forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(t), maxLines = 1) }) }
            }
        }
        when (tab) {
            0 -> cuadricula(posts, onAbrirPost, vacio = R.string.sin_publicaciones) { onPublicar("LOGRO") }
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
private fun Dato(n: Int, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

/** Cuadrícula de bitácora: 2 columnas, fichas 4:5 redondeadas con el tipo de publicación. */
private fun androidx.compose.foundation.lazy.LazyListScope.cuadricula(
    posts: List<Post>,
    onAbrir: (String) -> Unit,
    columnas: Int = 2,
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
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fila.forEach { p ->
                Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable { onAbrir(p.id) }) {
                    PostImagen(p, Modifier.fillMaxWidth(), conTexto = true, ratio = 4f / 5f)
                    Box(Modifier.align(Alignment.TopStart).padding(8.dp)) { TipoBadge(p.tipo) }
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
            Acceso(R.string.metas, Icons.Filled.Flag, com.rutaalacima.app.ui.Rutas.planificador(0)),
            Acceso(R.string.constancia_titulo, Icons.Filled.LocalFireDepartment, com.rutaalacima.app.ui.Rutas.CONSTANCIA),
            Acceso(R.string.diario_titulo, Icons.Filled.PhotoLibrary, com.rutaalacima.app.ui.Rutas.DIARIO),
            Acceso(R.string.carta_titulo, Icons.Filled.Drafts, com.rutaalacima.app.ui.Rutas.CARTA),
            Acceso(R.string.revision_semana, Icons.Filled.EventRepeat,
                com.rutaalacima.app.ui.Rutas.revision(com.rutaalacima.app.domain.model.Revision.Tipo.SEMANA, java.time.LocalDate.now())),
        ),
        R.string.perfil_grupo_personas to listOf(
            Acceso(R.string.mensajes, Icons.AutoMirrored.Filled.Chat, com.rutaalacima.app.ui.Rutas.MENSAJES),
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
