package com.rutaalacima.app.ui.moderacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.ModeracionRepository.Accion
import com.rutaalacima.app.data.social.ModeracionRepository.Caso
import com.rutaalacima.app.data.social.ModeracionRepository.Decision
import com.rutaalacima.app.data.social.ModeracionRepository.Tipo
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.fondoPapel
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ModeracionViewModel(private val c: AppContainer) : ViewModel() {
    var casos by mutableStateOf<List<Caso>>(emptyList()); private set
    var historial by mutableStateOf<List<Decision>>(emptyList()); private set
    var cargando by mutableStateOf(true); private set
    var error by mutableStateOf(false); private set
    /** Caso que se está decidiendo (para no tocar dos veces). */
    var ocupado by mutableStateOf<String?>(null); private set

    init { cargar() }

    fun cargar() = viewModelScope.launch {
        cargando = true
        runCatching { casos = c.moderacion.pendientes(); historial = c.moderacion.historial(); error = false }.onFailure { error = true }
        cargando = false
    }

    fun decidir(caso: Caso, accion: Accion, nota: String = "") = hacer(caso.objetivo) { c.moderacion.moderar(caso, accion, nota) }

    fun suspender(caso: Caso, dias: Int, motivo: String) {
        val autor = caso.autor ?: return
        hacer(caso.objetivo) { c.moderacion.suspender(autor.id, dias, motivo) }
    }

    private fun hacer(id: String, accion: suspend () -> Unit) {
        if (ocupado != null) return
        ocupado = id
        viewModelScope.launch {
            runCatching { accion() }.onSuccess { error = false; cargar().join() }.onFailure { error = true }
            ocupado = null
        }
    }
}

/** Cuándo, en el idioma del teléfono; "infinity" (suspensión permanente) devuelve null. */
internal fun fecha(iso: String): String? = runCatching {
    OffsetDateTime.parse(iso).toLocalDateTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeracionScreen(onBack: () -> Unit) {
    val vm = rutaViewModel { ModeracionViewModel(it) }
    var pestana by remember { mutableIntStateOf(0) }
    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.moderacion_titulo)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            TabRow(selectedTabIndex = pestana, containerColor = Color.Transparent) {
                Tab(pestana == 0, { pestana = 0 }, text = { Text(stringResource(R.string.mod_pendientes, vm.casos.size)) })
                Tab(pestana == 1, { pestana = 1 }, text = { Text(stringResource(R.string.mod_historial)) })
            }
            if (vm.error) Text(stringResource(R.string.mod_error), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            PullToRefreshBox(isRefreshing = vm.cargando, onRefresh = { vm.cargar() }, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (pestana == 0) {
                        if (vm.casos.isEmpty() && !vm.cargando) item { Vacio(stringResource(R.string.mod_vacio)) }
                        items(vm.casos, key = { it.tipo.clave + it.objetivo }) { caso -> TarjetaCaso(caso, vm) }
                    } else {
                        if (vm.historial.isEmpty() && !vm.cargando) item { Vacio(stringResource(R.string.mod_historial_vacio)) }
                        items(vm.historial) { d -> FilaDecision(d) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Vacio(texto: String) {
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Gavel, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.size(12.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun nombreTipo(t: Tipo) = stringResource(
    when (t) {
        Tipo.POST -> R.string.mod_tipo_post
        Tipo.COMENTARIO -> R.string.mod_tipo_comentario
        Tipo.CONVERSACION -> R.string.mod_tipo_conversacion
        Tipo.PERSONA -> R.string.mod_tipo_persona
    },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaCaso(caso: Caso, vm: ModeracionViewModel) {
    var confirmarEliminar by remember { mutableStateOf(false) }
    var suspendiendo by remember { mutableStateOf(false) }
    val ocupado = vm.ocupado == caso.objetivo
    val contenido = caso.tipo == Tipo.POST || caso.tipo == Tipo.COMENTARIO
    RutaCard {
        // Qué es y cuántas personas lo reportaron
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(nombreTipo(caso.tipo).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.mod_reportes, caso.reportes), style = MaterialTheme.typography.labelMedium,
                color = if (caso.reportes >= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        caso.autor?.let { a ->
            Text("@${a.usuario}" + (if (a.nombre.isNotBlank()) " · ${a.nombre}" else ""), style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp))
            val extras = buildList {
                if (a.sanciones > 0) add(stringResource(R.string.mod_sanciones, a.sanciones))
                if (a.suspendidoHasta != null) add(stringResource(R.string.mod_suspendida))
            }
            if (extras.isNotEmpty()) Text(extras.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
        // Lo reportado
        if (caso.imagen.isNotBlank()) {
            AsyncImage(caso.imagen, null, contentScale = ContentScale.Crop,
                modifier = Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(10.dp)))
        }
        if (caso.texto.isNotBlank()) Text(caso.texto, style = MaterialTheme.typography.bodyMedium, maxLines = 8, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp))
        if (caso.mensajes.isNotEmpty()) {
            Text(stringResource(R.string.mod_mensajes), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            caso.mensajes.takeLast(8).forEach { m ->
                Text("“$m”", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (contenido) Text(stringResource(if (caso.oculto) R.string.mod_oculto else R.string.mod_visible), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        // Por qué
        Column(Modifier.padding(top = 8.dp)) {
            (caso.motivos.ifEmpty { listOf(stringResource(R.string.mod_sin_motivo)) }).take(5).forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            fecha(caso.ultimo)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
        }
        // Decisiones
        if (ocupado) CircularProgressIndicator(Modifier.padding(top = 12.dp).size(24.dp))
        else FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { vm.decidir(caso, Accion.RESTAURAR) },
                label = { Text(stringResource(if (contenido) R.string.mod_restaurar else R.string.mod_descartar)) },
                leadingIcon = { Icon(Icons.Filled.Visibility, null, Modifier.size(18.dp)) })
            if (contenido) {
                if (!caso.oculto) AssistChip(onClick = { vm.decidir(caso, Accion.OCULTAR) }, label = { Text(stringResource(R.string.mod_ocultar)) },
                    leadingIcon = { Icon(Icons.Filled.VisibilityOff, null, Modifier.size(18.dp)) })
                AssistChip(onClick = { confirmarEliminar = true }, label = { Text(stringResource(R.string.mod_eliminar)) },
                    leadingIcon = { Icon(Icons.Filled.DeleteForever, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) })
            }
            if (caso.autor != null) AssistChip(onClick = { suspendiendo = true },
                label = { Text(stringResource(if (caso.autor.suspendidoHasta != null) R.string.mod_levantar else R.string.mod_suspender)) },
                leadingIcon = { Icon(Icons.Filled.Block, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) })
        }
    }

    if (confirmarEliminar) {
        var nota by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text(stringResource(R.string.mod_eliminar)) },
            text = {
                Column {
                    Text(stringResource(R.string.mod_eliminar_texto))
                    OutlinedTextField(nota, { nota = it.take(500) }, label = { Text(stringResource(R.string.mod_nota)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmarEliminar = false; vm.decidir(caso, Accion.ELIMINAR, nota) }) {
                    Text(stringResource(R.string.mod_eliminar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
    if (suspendiendo) DialogoSuspender(
        levantar = caso.autor?.suspendidoHasta != null,
        onConfirmar = { dias, motivo -> suspendiendo = false; vm.suspender(caso, dias, motivo) },
        onCancelar = { suspendiendo = false },
    )
}

/** Elegir la duración y escribir el motivo que verá la persona (o levantar la suspensión vigente). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoSuspender(levantar: Boolean, onConfirmar: (Int, String) -> Unit, onCancelar: () -> Unit) {
    val opciones = listOf(1, 7, 30, -1)
    var dias by remember { mutableIntStateOf(7) }
    var motivo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (levantar) R.string.mod_levantar else R.string.mod_suspender)) },
        text = {
            if (!levantar) Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    opciones.forEach { d ->
                        FilterChip(selected = dias == d, onClick = { dias = d },
                            label = { Text(if (d < 0) stringResource(R.string.mod_permanente) else stringResource(R.string.mod_dias, d)) })
                    }
                }
                OutlinedTextField(motivo, { motivo = it.take(500) }, label = { Text(stringResource(R.string.mod_motivo)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = levantar || motivo.isNotBlank(), onClick = { onConfirmar(if (levantar) 0 else dias, motivo.trim()) }) {
                Text(stringResource(R.string.mod_confirmar))
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
    )
}

@Composable
private fun FilaDecision(d: Decision) {
    val accion = stringResource(
        when (d.accion) {
            "restaurar" -> R.string.mod_acc_restaurar
            "ocultar" -> R.string.mod_acc_ocultar
            "eliminar" -> R.string.mod_acc_eliminar
            "suspender" -> R.string.mod_acc_suspender
            "levantar" -> R.string.mod_acc_levantar
            else -> R.string.mod_acc_descartar
        },
    )
    val tipo = Tipo.entries.firstOrNull { it.clave == d.tipo }?.let { nombreTipo(it) }.orEmpty()
    Column(Modifier.fillMaxWidth()) {
        Text(buildString {
            append("@").append(d.moderador.ifBlank { "—" }).append(" · ").append(accion)
            if (tipo.isNotBlank() && d.accion !in listOf("suspender", "levantar")) append(" · ").append(tipo.lowercase())
            if (d.usuario.isNotBlank()) append(" · @").append(d.usuario)
        }, style = MaterialTheme.typography.bodyMedium)
        if (d.nota.isNotBlank()) Text(d.nota, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        fecha(d.creado)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
    }
}

/** Aviso para quien tiene la cuenta suspendida: hasta cuándo, por qué y cómo pedir revisión. */
@Composable
fun AvisoSuspension(modifier: Modifier = Modifier) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val c = remember { (ctx.applicationContext as com.rutaalacima.app.RutaApp).container }
    val sesion by c.supabase.sesion.collectAsStateWithLifecycle()
    var s by remember { mutableStateOf<com.rutaalacima.app.data.social.ModeracionRepository.Suspension?>(null) }
    androidx.compose.runtime.LaunchedEffect(sesion?.userId) { s = c.moderacion.miSuspension() }
    val susp = s ?: return
    val abrir = androidx.compose.ui.platform.LocalUriHandler.current
    RutaCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Block, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.suspension_titulo), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        }
        val hasta = fecha(susp.hasta)
        Text(
            if (susp.permanente || hasta == null) stringResource(R.string.suspension_permanente) else stringResource(R.string.suspension_hasta, hasta),
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp),
        )
        if (susp.motivo.isNotBlank()) Text(stringResource(R.string.suspension_motivo, susp.motivo), style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(R.string.suspension_apelar), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp))
        OutlinedButton(onClick = {
            runCatching { abrir.openUri(com.rutaalacima.app.ui.legal.Legal.url(com.rutaalacima.app.ui.legal.Legal.Doc.PRIVACIDAD) + "#procedimiento") }
        }, modifier = Modifier.padding(top = 6.dp)) { Text(stringResource(R.string.suspension_apelar_boton)) }
    }
}
