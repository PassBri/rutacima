package com.rutaalacima.app.ui.cordadas

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.Companero
import com.rutaalacima.app.data.social.Cordada
import com.rutaalacima.app.data.social.NotaCordada
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.comunidad.Avatar
import com.rutaalacima.app.ui.i18n.Textos
import com.rutaalacima.app.ui.theme.fondoPapel
import com.rutaalacima.app.util.haceCuanto
import kotlinx.coroutines.launch
import java.time.LocalDate

class CordadasViewModel(c: AppContainer) : ViewModel() {
    val repo = c.cordadas
    var lista by mutableStateOf<List<Cordada>?>(null)
        private set
    var aviso by mutableStateOf<String?>(null)
    var trabajando by mutableStateOf(false)
        private set

    fun cargar() = viewModelScope.launch {
        if (!repo.disponible) { lista = emptyList(); return@launch }
        runCatching { repo.mias() }.onSuccess { lista = it }.onFailure { aviso = it.message; if (lista == null) lista = emptyList() }
    }

    private fun accion(bloque: suspend () -> String, alTerminar: (String) -> Unit) = viewModelScope.launch {
        trabajando = true
        runCatching { bloque() }.onSuccess { id -> cargar(); if (id.isNotBlank()) alTerminar(id) }.onFailure { aviso = it.message }
        trabajando = false
    }

    fun crear(nombre: String, reto: String, eje: String?, dias: Int, abrir: (String) -> Unit) = accion({ repo.crear(nombre, reto, eje, dias) }, abrir)
    fun unirse(codigo: String, abrir: (String) -> Unit) = accion({ repo.unirse(codigo) }, abrir)
    fun marcarHoy(c: Cordada) = viewModelScope.launch { runCatching { repo.marcar(c.id, LocalDate.now(), !c.marqueHoy) }; cargar() }
}

/** Mis cordadas: los retos en grupo, crear uno nuevo o unirme con un código. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CordadasScreen(onBack: () -> Unit, onAbrir: (String) -> Unit, onCuenta: () -> Unit) {
    val vm = rutaViewModel { CordadasViewModel(it) }
    LaunchedEffect(Unit) { vm.cargar() }
    var creando by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.cordadas)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text(stringResource(R.string.cordadas_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (!vm.repo.disponible) {
                item {
                    RutaCard {
                        Text(stringResource(R.string.cordadas_sin_cuenta), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onCuenta) { Text(stringResource(R.string.web_ir_cuenta)) }
                    }
                }
                return@LazyColumn
            }
            vm.aviso?.let { item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }
            val lista = vm.lista
            if (lista == null) item { Text(stringResource(R.string.cargando)) }
            else {
                lista.forEach { c ->
                    item(key = c.id) { TarjetaCordada(c, onAbrir = { onAbrir(c.id) }, onMarcar = { vm.marcarHoy(c) }) }
                }
                if (lista.isEmpty()) item {
                    com.rutaalacima.app.ui.components.EstadoVacio(
                        semilla = "cordada", titulo = stringResource(R.string.vacio_cordadas_titulo), texto = stringResource(R.string.cordadas_vacio),
                    )
                }
            }
            item { SectionTitle(stringResource(R.string.cordadas_nueva)) }
            item {
                if (!creando) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { creando = true }) { Text(stringResource(R.string.cordadas_crear)) }
                } else FormularioCordada(vm.trabajando, onCrear = { n, r, e, d -> vm.crear(n, r, e, d) { creando = false; onAbrir(it) } }, onCancelar = { creando = false })
            }
            item {
                var codigo by remember { mutableStateOf("") }
                RutaCard {
                    Text(stringResource(R.string.cordadas_unirme), style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(codigo, { codigo = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(6) },
                            label = { Text(stringResource(R.string.cordadas_codigo)) }, singleLine = true, modifier = Modifier.weight(1f))
                        Button(onClick = { vm.unirse(codigo, onAbrir) }, enabled = codigo.length == 6 && !vm.trabajando) { Text(stringResource(R.string.cordadas_entrar)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaCordada(c: Cordada, onAbrir: () -> Unit, onMarcar: () -> Unit) {
    RutaCard(onClick = onAbrir) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(c.reto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (c.activa()) IconButton(onClick = onMarcar) {
                Icon(if (c.marqueHoy) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    stringResource(R.string.cordadas_marcar_hoy), tint = if (c.marqueHoy) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (c.activa()) stringResource(R.string.cordadas_dia_de, c.diaDelReto(), c.dias) else stringResource(R.string.cordadas_terminada),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary,
        )
        ProgressLine((c.misDias / c.dias.toFloat()).coerceIn(0f, 1f), Modifier.padding(vertical = 4.dp))
        Text(stringResource(R.string.cordadas_resumen, c.misDias, c.miembros, c.diasGrupo), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FormularioCordada(ocupado: Boolean, onCrear: (String, String, String?, Int) -> Unit, onCancelar: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var reto by remember { mutableStateOf("") }
    var eje by remember { mutableStateOf<String?>(null) }
    var dias by remember { mutableStateOf(30) }
    RutaCard {
        OutlinedTextField(nombre, { nombre = it.take(60) }, label = { Text(stringResource(R.string.cordadas_nombre)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(reto, { reto = it.take(200) }, label = { Text(stringResource(R.string.cordadas_reto)) },
            placeholder = { Text(stringResource(R.string.cordadas_reto_ejemplo)) }, minLines = 2, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Text(stringResource(R.string.cordadas_duracion), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(7, 21, 30, 60, 90).forEach { d -> FilterChip(selected = dias == d, onClick = { dias = d }, label = { Text(stringResource(R.string.cordadas_n_dias, d)) }) }
        }
        Text(stringResource(R.string.cordadas_eje), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Eje.entries.forEach { e ->
                FilterChip(selected = eje == e.codigo, onClick = { eje = if (eje == e.codigo) null else e.codigo }, label = { Text(stringResource(Textos.nombre(e))) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { onCrear(nombre, reto, eje, dias) }, enabled = nombre.trim().length >= 2 && reto.trim().length >= 3 && !ocupado) {
                Text(stringResource(R.string.cordadas_crear))
            }
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    }
}

// ====================================================================== Una cordada

class CordadaViewModel(c: AppContainer) : ViewModel() {
    val repo = c.cordadas
    var cordada by mutableStateOf<Cordada?>(null)
        private set
    var companeros by mutableStateOf<List<Companero>>(emptyList())
        private set
    var notas by mutableStateOf<List<NotaCordada>>(emptyList())
        private set
    var texto by mutableStateOf("")
    var aviso by mutableStateOf<String?>(null)

    fun cargar(id: String) = viewModelScope.launch {
        runCatching {
            cordada = repo.mias().firstOrNull { it.id == id }
            companeros = repo.companeros(id)
            notas = repo.notas(id)
        }.onFailure { aviso = it.message }
    }

    fun marcar(id: String, dia: LocalDate, si: Boolean) = viewModelScope.launch {
        // Al instante en pantalla; luego se confirma con el servidor
        companeros = companeros.map { if (it.soyYo) it.copy(dias = if (si) it.dias + dia else it.dias - dia) else it }
        runCatching { repo.marcar(id, dia, si) }.onFailure { aviso = it.message }
        cargar(id)
    }

    fun escribir(id: String) = viewModelScope.launch {
        val t = texto.trim(); if (t.isEmpty()) return@launch
        texto = ""
        runCatching { repo.escribir(id, t) }.onFailure { texto = t; aviso = it.message }
        notas = runCatching { repo.notas(id) }.getOrDefault(notas)
    }

    fun borrarNota(id: String, nota: String) = viewModelScope.launch { runCatching { repo.borrarNota(nota) }; notas = runCatching { repo.notas(id) }.getOrDefault(notas) }
    fun salir(id: String, alTerminar: () -> Unit) = viewModelScope.launch { runCatching { repo.salir(id) }.onSuccess { alTerminar() }.onFailure { aviso = it.message } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CordadaScreen(id: String, onBack: () -> Unit) {
    val vm = rutaViewModel(key = "cordada-$id") { CordadaViewModel(it) }
    LaunchedEffect(id) { vm.cargar(id) }
    val c = vm.cordada
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val hoy = LocalDate.now()
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(c?.nombre.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.mas_opciones)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.cordadas_salir), color = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; vm.salir(id, onBack) })
                        }
                    }
                },
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().imePadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(vm.texto, { vm.texto = it.take(500) }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.cordadas_animo)) },
                    maxLines = 3, shape = RoundedCornerShape(20.dp))
                IconButton(onClick = { vm.escribir(id) }, enabled = vm.texto.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.enviar), tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            vm.aviso?.let { item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }
            if (c == null) { item { Text(stringResource(R.string.cargando)) }; return@LazyColumn }
            item {
                RutaCard {
                    Text(c.reto, style = MaterialTheme.typography.titleMedium)
                    Text(if (c.activa()) stringResource(R.string.cordadas_dia_de, c.diaDelReto(), c.dias) else stringResource(R.string.cordadas_terminada),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    val yo = vm.companeros.firstOrNull { it.soyYo }
                    if (c.activa() && yo != null) {
                        val marcado = hoy in yo.dias
                        Button(onClick = { vm.marcar(id, hoy, !marcado) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Icon(if (marcado) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (marcado) R.string.cordadas_hoy_listo else R.string.cordadas_marcar_hoy))
                        }
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.cordadas_companeros, vm.companeros.size)) }
            item { CuadriculaCordada(c, vm.companeros, hoy) }
            if (c.activa() && vm.companeros.size < 6) item {
                RutaCard {
                    Text(stringResource(R.string.cordadas_invitar), style = MaterialTheme.typography.bodyMedium)
                    Text(c.codigo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { contentDescription = c.codigo.toList().joinToString(" ") })
                    val texto = stringResource(R.string.cordadas_invitacion, c.reto, c.codigo)
                    OutlinedButton(onClick = {
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), null))
                    }) { Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.revision_compartir)) }
                }
            }
            item { SectionTitle(stringResource(R.string.cordadas_muro)) }
            if (vm.notas.isEmpty()) item { Text(stringResource(R.string.cordadas_muro_vacio), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(vm.notas, key = { it.id }) { n ->
                Row(verticalAlignment = Alignment.Top) {
                    Avatar(n.autor, tamano = 32)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(n.autor, style = MaterialTheme.typography.labelLarge)
                        Text(n.texto, style = MaterialTheme.typography.bodyMedium)
                        Text(haceCuanto(n.creada), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (n.mia) TextButton(onClick = { vm.borrarNota(id, n.id) }) { Text(stringResource(R.string.eliminar), style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

/** Compañeros × días del reto: un punto dorado por cada día cumplido (los más recientes a la derecha). */
@Composable
private fun CuadriculaCordada(c: Cordada, companeros: List<Companero>, hoy: LocalDate) {
    val hasta = minOf(c.fin, hoy)
    val dias = generateSequence(c.inicio) { it.plusDays(1) }.takeWhile { !it.isAfter(hasta) }.toList()
    RutaCard {
        companeros.forEach { p ->
          androidx.compose.runtime.key(p.userId) {
            val desplazar = rememberScrollState(Int.MAX_VALUE)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Avatar(p.nombre, p.avatar, tamano = 30)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.width(92.dp)) {
                    Text(if (p.soyYo) stringResource(R.string.cordadas_yo) else p.nombre, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.cordadas_n_de, p.dias.size, c.dias), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.weight(1f).horizontalScroll(desplazar), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    dias.forEach { d ->
                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (d in p.dias) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant))
                    }
                }
            }
          }
        }
    }
}
