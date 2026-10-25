package com.rutaalacima.app.ui.comunidad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.Aviso
import com.rutaalacima.app.data.social.PerfilPublico
import com.rutaalacima.app.data.social.Persona
import com.rutaalacima.app.ui.components.IconosRuta
import com.rutaalacima.app.ui.i18n.texto
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.fondoPapel
import com.rutaalacima.app.util.haceCuanto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ====================================================================== Avisos */

/** Avisos juntos: "Diana y 3 más · le dieron impulso a tu publicación". */
data class GrupoAvisos(val tipo: String, val actores: List<Aviso>, val primero: Aviso)

fun agruparAvisos(lista: List<Aviso>): List<GrupoAvisos> {
    val r = mutableListOf<GrupoAvisos>()
    for (a in lista) {
        val i = r.indexOfFirst { g -> a.tipo in setOf("IMPULSO", "LIKE_COMENTARIO", "SEGUIDOR") && g.tipo == a.tipo && g.primero.postId == a.postId &&
            g.primero.leida == a.leida && kotlin.math.abs(g.primero.creadoEn - a.creadoEn) < 2 * 86_400_000L }
        if (i >= 0) r[i] = r[i].copy(actores = r[i].actores + a) else r += GrupoAvisos(a.tipo, listOf(a), a)
    }
    return r
}

class AvisosViewModel(private val c: AppContainer) : ViewModel() {
    var avisos by mutableStateOf<List<Aviso>?>(null)
        private set
    var sigo by mutableStateOf<Set<String>>(emptySet())
        private set
    var prefs by mutableStateOf<Map<String, Boolean>>(emptyMap())
        private set
    init { cargar() }
    fun cargar() = viewModelScope.launch {
        avisos = c.red.avisos(); sigo = c.red.sigo()
        if (avisos.orEmpty().any { !it.leida }) c.red.marcarLeidos()
    }
    fun cargarPrefs() = viewModelScope.launch { prefs = c.red.preferencias() }
    fun cambiarPref(tipo: String, si: Boolean) = viewModelScope.launch { prefs = prefs + (tipo to si); runCatching { c.red.guardarPreferencias(prefs) } }
    fun seguir(id: String) = viewModelScope.launch { c.red.seguir(id, true); sigo = sigo + id }
}

private val ICONO_AVISO: Map<String, ImageVector> by lazy {
    mapOf("IMPULSO" to IconosRuta.Banderin, "LIKE_COMENTARIO" to IconosRuta.Banderin, "COMENTARIO" to IconosRuta.Radio,
        "RESPUESTA" to IconosRuta.Radio, "SEGUIDOR" to IconosRuta.Perfil, "MENCION" to IconosRuta.Radio)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvisosScreen(onBack: () -> Unit, onPost: (String) -> Unit, onPersona: (String) -> Unit, onPrivacidad: () -> Unit = {}) {
    val vm = rutaViewModel { AvisosViewModel(it) }
    var ajustes by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.red_avisos)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = {
                    IconButton(onClick = onPrivacidad) { Icon(Icons.Filled.Lock, stringResource(R.string.red_privacidad)) }
                    IconButton(onClick = { ajustes = !ajustes; if (ajustes) vm.cargarPrefs() }) { Icon(Icons.Filled.Settings, stringResource(R.string.red_que_recibir)) }
                },
            )
        },
    ) { padding ->
        val lista = vm.avisos
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp)) {
            if (ajustes) item("prefs") { PreferenciasAvisos(vm.prefs, vm::cambiarPref) }
            when {
                lista == null -> item("cargando") { Text("…", Modifier.padding(24.dp)) }
                lista.isEmpty() -> item("vacio") {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Notifications, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        Text(stringResource(R.string.red_avisos_vacio_t), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.red_avisos_vacio), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    val ahora = System.currentTimeMillis()
                    val grupos = agruparAvisos(lista).groupBy { g ->
                        val d = ahora - g.primero.creadoEn
                        if (d < 86_400_000L) R.string.red_hoy else if (d < 7 * 86_400_000L) R.string.red_semana else R.string.red_antes
                    }
                    grupos.forEach { (tramo, gs) ->
                        item("t$tramo") {
                            Text(stringResource(tramo), Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(gs, key = { "a${it.primero.id}" }) { g ->
                            FilaAviso(g, g.primero.actorId in vm.sigo,
                                onClick = { if (g.tipo == "SEGUIDOR" || g.primero.postId == null) onPersona(g.primero.actorId) else onPost(g.primero.postId) },
                                onSeguir = { vm.seguir(g.primero.actorId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaAviso(g: GrupoAvisos, loSigo: Boolean, onClick: () -> Unit, onSeguir: () -> Unit) {
    val a = g.primero
    val nombres = when (g.actores.size) {
        1 -> a.actorNombre
        2 -> stringResource(R.string.red_dos, a.actorNombre, g.actores[1].actorNombre)
        else -> stringResource(R.string.red_varios, a.actorNombre, g.actores.size - 1)
    }
    val muchos = g.actores.size > 1
    val accion = stringResource(when (g.tipo) {
        "IMPULSO" -> if (muchos) R.string.red_a_impulso_pl else R.string.red_a_impulso
        "COMENTARIO" -> R.string.red_a_comentario
        "RESPUESTA" -> R.string.red_a_respuesta
        "SEGUIDOR" -> if (muchos) R.string.red_a_seguidor_pl else R.string.red_a_seguidor
        "LIKE_COMENTARIO" -> if (muchos) R.string.red_a_like_com_pl else R.string.red_a_like_com
        else -> R.string.red_a_mencion
    })
    ListItem(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp).clip(RoundedCornerShape(16.dp)),
        colors = ListItemDefaults.colors(containerColor = if (a.leida) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        leadingContent = {
            Box(Modifier.size(52.dp)) {
                Avatar(a.actorNombre, a.actorAvatar, 48)
                Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(ICONO_AVISO[g.tipo] ?: IconosRuta.Banderin, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        headlineContent = { Text(nombres, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(if (a.texto.isNotBlank() && g.tipo in setOf("COMENTARIO", "RESPUESTA", "MENCION")) "$accion: “${a.texto}”" else accion, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(haceCuanto(a.creadoEn), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        trailingContent = when {
            a.foto.isNotBlank() -> ({ AsyncImage(a.foto, null, Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop) })
            g.tipo == "SEGUIDOR" && !loSigo && a.actorId.isNotBlank() -> ({ Button(onClick = onSeguir) { Text(stringResource(R.string.red_seguir)) } })
            else -> null
        },
    )
}

@Composable
private fun PreferenciasAvisos(prefs: Map<String, Boolean>, onCambiar: (String, Boolean) -> Unit) {
    val tipos = listOf("IMPULSO" to R.string.red_p_impulso, "COMENTARIO" to R.string.red_p_comentario, "RESPUESTA" to R.string.red_p_respuesta,
        "SEGUIDOR" to R.string.red_p_seguidor, "LIKE_COMENTARIO" to R.string.red_p_like_com, "MENCION" to R.string.red_p_mencion)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(8.dp)) {
        Text(stringResource(R.string.red_que_recibir), Modifier.padding(12.dp), style = MaterialTheme.typography.titleMedium)
        tipos.forEach { (k, n) ->
            Row(Modifier.fillMaxWidth().clickable { onCambiar(k, prefs[k] == false) }.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(n), Modifier.weight(1f))
                Switch(checked = prefs[k] != false, onCheckedChange = { onCambiar(k, it) })
            }
        }
    }
}

/* ====================================================================== Buscar personas */

class PersonasViewModel(private val c: AppContainer) : ViewModel() {
    var resultados by mutableStateOf<List<Persona>>(emptyList())
        private set
    var sugeridas by mutableStateOf<List<Persona>>(emptyList())
        private set
    var sigo by mutableStateOf<Set<String>>(emptySet())
        private set
    var buscando by mutableStateOf(false)
        private set
    private var trabajo: Job? = null
    init { viewModelScope.launch { sigo = c.red.sigo(); sugeridas = c.red.sugeridas(cuantas = 12) } }
    fun buscar(t: String) {
        trabajo?.cancel()
        trabajo = viewModelScope.launch { delay(300); buscando = true; resultados = c.red.buscar(t); buscando = false }
    }
    fun alternar(id: String) = viewModelScope.launch { val si = id !in sigo; c.red.seguir(id, si); sigo = if (si) sigo + id else sigo - id }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscarPersonasScreen(onBack: () -> Unit, onPersona: (String) -> Unit) {
    val vm = rutaViewModel { PersonasViewModel(it) }
    var texto by rememberSaveable { mutableStateOf("") }
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.red_buscar)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp)) {
            item("campo") {
                OutlinedTextField(
                    texto, { texto = it; vm.buscar(it) }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.red_buscar_hint)) }, singleLine = true, shape = RoundedCornerShape(28.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = if (texto.isNotEmpty()) ({ IconButton(onClick = { texto = ""; vm.buscar("") }) { Icon(Icons.Filled.Close, null) } }) else null,
                )
            }
            val lista = if (texto.trim().length >= 2) vm.resultados else vm.sugeridas
            if (texto.trim().length < 2 && lista.isNotEmpty()) item("sug") {
                Text(stringResource(R.string.red_sugeridas), Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (texto.trim().length >= 2 && !vm.buscando && lista.isEmpty()) item("nada") {
                Text(stringResource(R.string.red_sin_resultados), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(lista, key = { it.id }) { p -> FilaPersona(p, p.id in vm.sigo, { onPersona(p.id) }, { vm.alternar(p.id) }) }
        }
    }
}

@Composable
fun FilaPersona(p: Persona, loSigo: Boolean, onClick: () -> Unit, onSeguir: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Avatar(p.nombre, p.avatar, 48) },
        headlineContent = { Text(p.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(if (p.cumbre.isNotBlank()) "@${p.usuario} · ${p.cumbre}" else "@${p.usuario}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            if (loSigo) OutlinedButton(onClick = onSeguir) { Text(stringResource(R.string.red_siguiendo)) }
            else Button(onClick = onSeguir) { Text(stringResource(R.string.red_seguir)) }
        },
    )
}

/* ====================================================================== Perfil público */

class PersonaViewModel(private val c: AppContainer, private val id: String) : ViewModel() {
    var perfil by mutableStateOf<PerfilPublico?>(null)
        private set
    var cargado by mutableStateOf(false)
        private set
    init { viewModelScope.launch { perfil = c.red.perfil(id); cargado = true } }
    fun escribir(onChat: (String) -> Unit) = viewModelScope.launch { runCatching { c.mensajes.abrirCon(id) }.onSuccess(onChat) }
    fun alternar() = viewModelScope.launch {
        val p = perfil ?: return@launch
        c.red.seguir(id, !p.loSigo)
        perfil = p.copy(loSigo = !p.loSigo, seguidores = p.seguidores + if (p.loSigo) -1 else 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaScreen(id: String, onBack: () -> Unit, onPost: (String) -> Unit, onChat: (String) -> Unit, onDiario: (String, String) -> Unit) {
    val vm = rutaViewModel(key = "persona-$id") { PersonaViewModel(it, id) }
    val p = vm.perfil
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(p?.persona?.usuario?.let { "@$it" }.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { if (vm.cargado) Text(stringResource(R.string.red_sin_resultados)) }
            return@Scaffold
        }
        LazyVerticalGrid(
            GridCells.Fixed(3), Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item("cab", span = { GridItemSpan(3) }) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.rutaalacima.app.ui.components.AroMarca(96.dp) { Avatar(p.persona.nombre, p.persona.avatar, 96) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.persona.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        InsigniaCoach(p.persona.id, 20)
                    }
                    if (p.meSigue && !p.propio) Text(stringResource(R.string.red_te_sigue), style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 8.dp, vertical = 2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Cifra(p.publicaciones.size, R.string.red_publicaciones); Cifra(p.seguidores, R.string.red_seguidores); Cifra(p.siguiendo, R.string.red_siguiendo)
                    }
                    if (p.persona.bio.isNotBlank()) Text(p.persona.bio, style = MaterialTheme.typography.bodyMedium)
                    if (p.persona.cumbre.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(IconosRuta.Cima, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(p.persona.cumbre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    if (!p.propio) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (p.loSigo) OutlinedButton(onClick = vm::alternar) { Text(stringResource(R.string.red_siguiendo)) }
                        else Button(onClick = vm::alternar) { Text(stringResource(R.string.red_seguir)) }
                        FilledTonalButton(onClick = { vm.escribir(onChat) }) { Text(stringResource(R.string.red_mensaje)) }
                        FilledTonalButton(onClick = { onDiario(p.persona.id, p.persona.nombre) }) { Text(stringResource(R.string.red_diario)) }
                    }
                }
            }
            if (p.publicaciones.isEmpty()) item("vacio", span = { GridItemSpan(3) }) {
                Text(stringResource(R.string.red_sin_publicaciones), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(p.publicaciones, key = { it.id }) { post ->
                Box(Modifier.aspectRatio(0.8f).clickable { onPost(post.id) }.background(MaterialTheme.colorScheme.surfaceVariant)) {
                    if (post.foto.isNotBlank()) AsyncImage(post.foto, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Text(post.texto, Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun Cifra(n: Int, etiqueta: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(n.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(stringResource(etiqueta), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/* ====================================================================== Primeros pasos */

class BienvenidaViewModel(private val c: AppContainer) : ViewModel() {
    /** null = aún no se sabe; true = mostrar los primeros pasos. */
    var mostrar by mutableStateOf<Boolean?>(null)
        private set
    var ejes by mutableStateOf<Set<String>>(emptySet())
        private set
    var sugeridas by mutableStateOf<List<Persona>>(emptyList())
        private set
    var sigo by mutableStateOf<Set<String>>(emptySet())
        private set
    init {
        viewModelScope.launch {
            val r = c.respuestas.cargar("red")
            ejes = r["red:ejes"].orEmpty().split(",").filter { it.isNotBlank() }.toSet()
            mostrar = r["red:inicio"].isNullOrBlank()
            if (mostrar == true) { sigo = c.red.sigo(); sugeridas = c.red.sugeridas(ejes, 6) }
        }
    }
    fun alternarEje(e: String) = viewModelScope.launch {
        ejes = if (e in ejes) ejes - e else ejes + e
        c.respuestas.guardar("red", "red:ejes", ejes.joinToString(","))
        sugeridas = c.red.sugeridas(ejes, 6)
    }
    fun alternarSeguir(id: String) = viewModelScope.launch { val si = id !in sigo; c.red.seguir(id, si); sigo = if (si) sigo + id else sigo - id }
    var invitoOk by mutableStateOf<String?>(null)
        private set
    fun aceptarInvitacion(u: String) = viewModelScope.launch {
        val limpio = u.trim().removePrefix("@").lowercase()
        if (limpio.length >= 3 && c.red.aceptarInvitacion(limpio)) { invitoOk = limpio; sigo = c.red.sigo() }
    }
    fun terminar(onListo: () -> Unit) = viewModelScope.launch {
        c.respuestas.guardar("red", "red:inicio", "1")
        mostrar = false; onListo()
    }
}

/** La primera vez en Comunidad: elegir los ejes que mueven a la persona y seguir a algunas personas. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PrimerosPasosComunidad(vm: BienvenidaViewModel, padding: PaddingValues, onListo: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().fondoPapel(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 16.dp, bottom = padding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("t") {
            Text(stringResource(R.string.red_bienvenida_t), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.red_bienvenida), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item("ejes") {
            Column(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.red_tus_ejes), style = MaterialTheme.typography.titleMedium)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.rutaalacima.app.domain.model.Eje.entries.forEach { e ->
                        val si = e.codigo in vm.ejes
                        val color = Color(e.color)
                        androidx.compose.material3.FilterChip(
                            selected = si, onClick = { vm.alternarEje(e.codigo) },
                            label = { Text(e.texto()) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color, selectedLabelColor = Color.White, labelColor = color),
                        )
                    }
                }
            }
        }
        item("personas") {
            Column(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical = 8.dp)) {
                Text(stringResource(R.string.red_sugeridas), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
                vm.sugeridas.forEach { p -> FilaPersona(p, p.id in vm.sigo, onClick = { vm.alternarSeguir(p.id) }, onSeguir = { vm.alternarSeguir(p.id) }) }
                if (vm.sigo.isNotEmpty()) Text(stringResource(R.string.red_siguiendo_n, vm.sigo.size), Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item("invito") {
            var quien by rememberSaveable { mutableStateOf("") }
            Column(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(quien, { quien = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.red_invito)) },
                    trailingIcon = { if (quien.trim().length >= 3) androidx.compose.material3.TextButton(onClick = { vm.aceptarInvitacion(quien) }) { Text(stringResource(R.string.red_seguir)) } })
                vm.invitoOk?.let { Text(stringResource(R.string.red_invito_ok, it), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
            }
        }
        item("botones") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.terminar(onListo) }, Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.red_listo)) }
                androidx.compose.material3.TextButton(onClick = { vm.terminar(onListo) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.red_omitir)) }
            }
        }
    }
}

/* ====================================================================== Etiquetas y ejes */

class ListadoViewModel(private val c: AppContainer, private val clave: String) : ViewModel() {
    var posts by mutableStateOf<List<com.rutaalacima.app.data.social.Post>?>(null)
        private set
    init {
        viewModelScope.launch {
            posts = runCatching { if (clave.startsWith("#")) c.red.etiqueta(clave.drop(1)) else c.red.deEje(clave.removePrefix("eje:")) }.getOrDefault(emptyList())
        }
    }
}

/** Las publicaciones de una #etiqueta o de un eje, en cuadrícula. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoPublicacionesScreen(clave: String, onBack: () -> Unit, onPost: (String) -> Unit) {
    val vm = rutaViewModel(key = "listado-$clave") { ListadoViewModel(it, clave) }
    val eje = if (clave.startsWith("eje:")) com.rutaalacima.app.domain.model.Eje.entries.firstOrNull { it.codigo == clave.removePrefix("eje:") } else null
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(eje?.texto() ?: clave) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        val ps = vm.posts
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (ps != null && ps.isEmpty()) item("vacio", span = { GridItemSpan(3) }) {
                Text(stringResource(R.string.red_listado_vacio), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(ps.orEmpty(), key = { it.id }) { post ->
                Box(Modifier.aspectRatio(0.8f).clickable { onPost(post.id) }.background(MaterialTheme.colorScheme.surfaceVariant)) {
                    if (post.foto.isNotBlank()) AsyncImage(post.foto, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Text(post.texto, Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/* ====================================================================== Retos e insignias */

/** Quiénes son coaches verificados (se llena al abrir Comunidad); la insignia lo lee. */
object InsigniasRed { var verificados by mutableStateOf<Set<String>>(emptySet()) }

/** La insignia de coach de vida verificado junto al nombre. */
@Composable
fun InsigniaCoach(id: String, tamano: Int = 16) {
    if (id !in InsigniasRed.verificados) return
    Box(Modifier.padding(start = 4.dp).size(tamano.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Check, stringResource(R.string.red_coach_verificado), Modifier.size((tamano * 0.7f).dp), tint = MaterialTheme.colorScheme.onTertiary)
    }
}

/** El reto del mes: unirse, marcar el día y ver cuántos van. */
@Composable
fun RetoCard(r: com.rutaalacima.app.data.social.Reto, onUnirse: () -> Unit, onMarcar: () -> Unit, onSalir: () -> Unit) {
    val color = com.rutaalacima.app.domain.model.Eje.entries.firstOrNull { it.codigo == r.eje }?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.1f)).border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color), contentAlignment = Alignment.Center) {
                Icon(IconosRuta.Metas, null, Modifier.size(22.dp), tint = Color.White)
            }
            Column {
                Text(stringResource(R.string.red_reto), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(r.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        if (r.descripcion.isNotBlank()) Text(r.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.red_reto_en, r.participantes), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.red_reto_hoy, r.marcasHoy), style = MaterialTheme.typography.labelMedium)
            if (r.yoParticipo) Text(stringResource(R.string.red_reto_mio, r.misDias, r.dias), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        if (r.yoParticipo) androidx.compose.material3.LinearProgressIndicator(progress = { (r.misDias.toFloat() / r.dias).coerceIn(0f, 1f) }, Modifier.fillMaxWidth(), color = color)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                !r.yoParticipo -> Button(onClick = onUnirse) { Text(stringResource(R.string.red_unirme)) }
                r.hoyMarcado -> Text("✓ " + stringResource(R.string.red_hoy_cumplido), color = color, fontWeight = FontWeight.SemiBold)
                else -> Button(onClick = onMarcar) { Text(stringResource(R.string.red_marcar_hoy)) }
            }
            if (r.yoParticipo) androidx.compose.material3.TextButton(onClick = onSalir) { Text(stringResource(R.string.red_salir_reto)) }
        }
    }
}

/* ====================================================================== Comentarios con respuestas */

/**
 * Comentarios con sus respuestas debajo (sangradas), impulso de comentario, "Responder" y, en mis
 * publicaciones, "Ocultar". Se usa en la hoja de Cimas y en el detalle de la publicación.
 */
fun androidx.compose.foundation.lazy.LazyListScope.comentariosConRespuestas(
    lista: List<com.rutaalacima.app.data.social.Comentario>, esDueno: Boolean,
    onResponder: (com.rutaalacima.app.data.social.Comentario) -> Unit,
    onImpulsar: (com.rutaalacima.app.data.social.Comentario) -> Unit,
    onOcultar: (com.rutaalacima.app.data.social.Comentario) -> Unit,
) {
    val ids = lista.map { it.id }.toSet()
    val raiz = lista.filter { it.parentId == null || it.parentId !in ids }
    raiz.forEach { c ->
        item(key = "c${c.id}") { FilaComentario(c, false, esDueno, onResponder, onImpulsar, onOcultar) }
        lista.filter { it.parentId == c.id }.forEach { h ->
            item(key = "c${h.id}") { FilaComentario(h, true, esDueno, onResponder, onImpulsar, onOcultar) }
        }
    }
}

@Composable
private fun FilaComentario(
    c: com.rutaalacima.app.data.social.Comentario, respuesta: Boolean, esDueno: Boolean,
    onResponder: (com.rutaalacima.app.data.social.Comentario) -> Unit,
    onImpulsar: (com.rutaalacima.app.data.social.Comentario) -> Unit,
    onOcultar: (com.rutaalacima.app.data.social.Comentario) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(start = if (respuesta) 42.dp else 0.dp), verticalAlignment = Alignment.Top) {
        Avatar(c.autor, c.autorAvatar, if (respuesta) 26 else 32)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(c.autor, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextoEnlazado(c.texto, style = MaterialTheme.typography.bodyMedium)
            if (c.ocultoAutor) Text(stringResource(R.string.red_oculto), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(haceCuanto(c.creadoEn), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.red_responder), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onResponder(c) }.padding(vertical = 6.dp))
                if (esDueno && !c.propio) Text(stringResource(if (c.ocultoAutor) R.string.red_mostrar else R.string.red_ocultar), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onOcultar(c) }.padding(vertical = 6.dp))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onImpulsar(c) }.padding(6.dp)) {
            Icon(if (c.yoLike) IconosRuta.Banderin else IconosRuta.BanderinContorno, stringResource(R.string.impulsar), Modifier.size(20.dp),
                tint = if (c.yoLike) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            if (c.likes > 0) Text(c.likes.toString(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/* ====================================================================== Privacidad y seguridad */

class PrivacidadViewModel(private val c: AppContainer) : ViewModel() {
    var regla by mutableStateOf("TODOS"); private set
    var menor by mutableStateOf(false); private set
    var silenciados by mutableStateOf<List<Persona>>(emptyList()); private set
    init { viewModelScope.launch { val p = c.red.privacidad(); regla = p.first; menor = p.second; silenciados = c.red.silenciados() } }
    fun cambiarRegla(r: String) = viewModelScope.launch { regla = r; runCatching { c.red.reglaComentarios(r) } }
    fun cambiarMenor(si: Boolean) = viewModelScope.launch { menor = si; runCatching { c.red.soyMenor(si) } }
    fun desilenciar(p: Persona) = viewModelScope.launch { c.social.silenciar(p.id, false); silenciados = silenciados - p }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacidadScreen(onBack: () -> Unit) {
    val vm = rutaViewModel { PrivacidadViewModel(it) }
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.red_privacidad)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item("regla") {
                Column(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.red_quien_comenta), style = MaterialTheme.typography.titleMedium)
                    val ops = listOf("TODOS" to R.string.red_todos, "SIGUEN" to R.string.red_personas_sigo, "NADIE" to R.string.red_nadie)
                    androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ops.forEachIndexed { i, (k, n) ->
                            SegmentedButton(selected = vm.regla == k, onClick = { vm.cambiarRegla(k) },
                                shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(i, ops.size)) { Text(stringResource(n), maxLines = 1) }
                        }
                    }
                }
            }
            item("menor") {
                Row(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).clickable { vm.cambiarMenor(!vm.menor) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.red_menor), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.red_menor_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = vm.menor, onCheckedChange = vm::cambiarMenor)
                }
            }
            item("sil") {
                Column(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.red_silenciados), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
                    if (vm.silenciados.isEmpty()) Text(stringResource(R.string.red_sin_silenciados), Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    vm.silenciados.forEach { p ->
                        ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent), headlineContent = { Text(p.nombre) },
                            leadingContent = { Avatar(p.nombre, p.avatar, 36) },
                            trailingContent = { OutlinedButton(onClick = { vm.desilenciar(p) }) { Text(stringResource(R.string.red_dejar_silenciar)) } })
                    }
                }
            }
        }
    }
}
