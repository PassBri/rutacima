package com.rutaalacima.app.ui.mensajes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.Acompanamiento
import com.rutaalacima.app.data.social.CoachFicha
import com.rutaalacima.app.data.social.MiFichaCoach
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.ResumenCoach
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.comunidad.Avatar
import com.rutaalacima.app.ui.theme.fondoPapel
import kotlinx.coroutines.launch

class CoachVidaViewModel(c: AppContainer) : ViewModel() {
    val repo = c.mensajes
    var directorio by mutableStateOf<List<CoachFicha>>(emptyList())
    var acompanamientos by mutableStateOf<List<Acompanamiento>>(emptyList())
    var miFicha by mutableStateOf<MiFichaCoach?>(null)
    var aviso by mutableStateOf<String?>(null)
    var trabajando by mutableStateOf(false)

    val miCoach get() = acompanamientos.firstOrNull { it.rol == "usuario" }
    val acompanados get() = acompanamientos.filter { it.rol == "coach" }

    fun cargar() = viewModelScope.launch {
        if (!repo.disponible) return@launch
        runCatching {
            acompanamientos = repo.acompanamientos()
            directorio = repo.directorio()
            miFicha = repo.miFicha()
        }
    }

    private fun accion(error: String, bloque: suspend () -> Unit) = viewModelScope.launch {
        trabajando = true
        runCatching { bloque() }.onFailure { aviso = it.message ?: error }
        trabajando = false
        cargar()
    }

    fun solicitar(coach: String, error: String) = accion(error) { repo.solicitarCoach(coach) }
    fun responder(id: String, si: Boolean, error: String) = accion(error) { repo.responderAcompanamiento(id, si) }
    fun terminar(id: String, error: String) = accion(error) { repo.terminarAcompanamiento(id) }
    fun compartir(id: String, si: Boolean, error: String) = accion(error) { repo.compartirAvance(id, si) }
    fun postular(bio: String, esp: String, error: String) = accion(error) { repo.postularme(bio, esp) }
    fun abrirChat(otro: String, onAbrir: (String) -> Unit, error: String) = viewModelScope.launch {
        runCatching { repo.abrirCon(otro) }.onSuccess(onAbrir).onFailure { aviso = error }
    }
}

/**
 * Coach de vida: una persona real que te acompaña. Directorio de coaches verificados, tu coach
 * (con el permiso para ver tu avance), y si eres coach, las personas que acompañas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachVidaScreen(onBack: () -> Unit, onChat: (String) -> Unit, onAcompanado: (String, String) -> Unit, onCuenta: () -> Unit) {
    val vm = rutaViewModel { CoachVidaViewModel(it) }
    LaunchedEffect(Unit) { vm.cargar() }
    val error = stringResource(R.string.mensajes_error)
    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.coach_vida)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text(stringResource(R.string.coach_vida_intro), style = MaterialTheme.typography.bodyMedium) }
            if (!vm.repo.disponible) {
                item {
                    RutaCard {
                        Text(stringResource(R.string.mensajes_sin_cuenta), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onCuenta) { Text(stringResource(R.string.web_ir_cuenta)) }
                    }
                }
                return@LazyColumn
            }
            vm.aviso?.let { item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }

            // ---------- Mi coach
            val mio = vm.miCoach
            if (mio != null) {
                item { SectionTitle(stringResource(R.string.coach_vida_mi_coach)) }
                item {
                    RutaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(mio.otroNombre, mio.otroAvatar, tamano = 48)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(mio.otroNombre, style = MaterialTheme.typography.titleMedium)
                                if (mio.especialidad.isNotBlank()) Text(mio.especialidad, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (mio.estado == "solicitado") {
                            Text(stringResource(R.string.coach_vida_esperando), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        } else {
                            Button(onClick = { vm.abrirChat(mio.otroId, onChat, error) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.coach_vida_escribir))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.coach_vida_compartir), style = MaterialTheme.typography.bodyMedium)
                                    Text(stringResource(R.string.coach_vida_compartir_ayuda), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = mio.comparteAvance, onCheckedChange = { vm.compartir(mio.id, it, error) }, enabled = !vm.trabajando)
                            }
                        }
                        TextButton(onClick = { vm.terminar(mio.id, error) }) {
                            Text(stringResource(if (mio.estado == "solicitado") R.string.coach_vida_cancelar else R.string.coach_vida_terminar),
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // ---------- Personas que acompaño (si soy coach)
            if (vm.acompanados.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.coach_vida_acompanas)) }
                items(vm.acompanados, key = { it.id }) { a ->
                    RutaCard(onClick = if (a.estado == "activo") ({ onAcompanado(a.otroId, a.otroNombre) }) else null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(a.otroNombre, a.otroAvatar, tamano = 40)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.otroNombre, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    stringResource(when {
                                        a.estado == "solicitado" -> R.string.coach_vida_te_pide
                                        a.comparteAvance -> R.string.coach_vida_ver_avance
                                        else -> R.string.coach_vida_no_comparte
                                    }),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (a.estado == "solicitado") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                Button(onClick = { vm.responder(a.id, true, error) }, enabled = !vm.trabajando) { Text(stringResource(R.string.mensajes_aceptar)) }
                                OutlinedButton(onClick = { vm.responder(a.id, false, error) }, enabled = !vm.trabajando) { Text(stringResource(R.string.mensajes_rechazar)) }
                            }
                        }
                    }
                }
            }

            // ---------- Directorio
            if (mio == null) {
                item { SectionTitle(stringResource(R.string.coach_vida_directorio)) }
                if (vm.directorio.isEmpty()) {
                    item { Text(stringResource(R.string.coach_vida_sin_coaches), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(vm.directorio, key = { it.userId }) { f ->
                    RutaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(f.nombre, f.avatar, tamano = 48)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.nombre, style = MaterialTheme.typography.titleMedium)
                                if (f.especialidad.isNotBlank()) Text(f.especialidad, style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary)
                                Text(stringResource(R.string.coach_vida_n_acompanados, f.acompanados), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (f.bio.isNotBlank()) Text(f.bio, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Button(onClick = { vm.solicitar(f.userId, error) }, enabled = !vm.trabajando) { Text(stringResource(R.string.coach_vida_solicitar)) }
                            OutlinedButton(onClick = { vm.abrirChat(f.userId, onChat, error) }) { Text(stringResource(R.string.coach_vida_preguntar)) }
                        }
                    }
                }
            }

            // ---------- Ser coach
            item { SectionTitle(stringResource(R.string.coach_vida_ser_coach)) }
            item {
                val ficha = vm.miFicha
                RutaCard {
                    when {
                        ficha?.verificado == true -> Text(stringResource(R.string.coach_vida_eres_coach), style = MaterialTheme.typography.bodyMedium)
                        ficha != null -> Text(stringResource(R.string.coach_vida_en_revision), style = MaterialTheme.typography.bodyMedium)
                        else -> {
                            var bio by androidx.compose.runtime.remember { mutableStateOf("") }
                            var esp by androidx.compose.runtime.remember { mutableStateOf("") }
                            Text(stringResource(R.string.coach_vida_ser_coach_texto), style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(esp, { esp = it.take(120) }, label = { Text(stringResource(R.string.coach_vida_especialidad)) },
                                singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                            OutlinedTextField(bio, { bio = it.take(1200) }, label = { Text(stringResource(R.string.coach_vida_bio)) },
                                minLines = 3, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                            Button(onClick = { vm.postular(bio, esp, error) }, enabled = bio.isNotBlank() && esp.isNotBlank() && !vm.trabajando,
                                modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.coach_vida_postular)) }
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================== Avance de quien acompaño

class AcompanadoViewModel(c: AppContainer) : ViewModel() {
    val repo = c.mensajes
    var resumen by mutableStateOf<ResumenCoach?>(null)
    var error by mutableStateOf(false)
    fun cargar(id: String) = viewModelScope.launch {
        runCatching { repo.avanceDe(id) }.onSuccess { resumen = it }.onFailure { error = true }
    }
    fun abrirChat(otro: String, onAbrir: (String) -> Unit) = viewModelScope.launch { runCatching { repo.abrirCon(otro) }.onSuccess(onAbrir) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcompanadoScreen(id: String, nombre: String, onBack: () -> Unit, onChat: (String) -> Unit) {
    val vm = rutaViewModel { AcompanadoViewModel(it) }
    LaunchedEffect(id) { vm.cargar(id) }
    val r = vm.resumen
    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(nombre) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = { IconButton(onClick = { vm.abrirChat(id, onChat) }) { Icon(Icons.AutoMirrored.Filled.Chat, stringResource(R.string.coach_vida_escribir)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (r == null) {
                item { Text(stringResource(if (vm.error) R.string.mensajes_error else R.string.cargando), style = MaterialTheme.typography.bodyMedium) }
                return@LazyColumn
            }
            item { Text(stringResource(R.string.coach_vida_privacidad), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (r.cumbre.isNotBlank()) item {
                RutaCard {
                    Text(stringResource(R.string.mi_cumbre), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(r.cumbre, style = MaterialTheme.typography.titleMedium)
                }
            }
            item {
                RutaCard {
                    Text(stringResource(R.string.coach_vida_habitos7), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Row(Modifier.fillMaxWidth().height(70.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                        r.habitos7.forEach { n ->
                            Box(Modifier.weight(1f).fillMaxHeight((n / 18f).coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(if (n >= 10) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
                        }
                    }
                    Text(stringResource(R.string.coach_vida_promedio, r.promedioHabitos), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
            fun bloque(titulo: Int, xs: List<ResumenCoach.Item>) {
                if (xs.isEmpty()) return
                item {
                    RutaCard {
                        Text(stringResource(titulo), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        xs.forEach { x ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                Text(x.titulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("${x.avance}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            LinearProgressIndicator(progress = { x.avance / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                        }
                    }
                }
            }
            bloque(R.string.coach_vida_propositos, r.propositos)
            bloque(R.string.coach_vida_metas_anio, r.metasAnio)
            bloque(R.string.coach_vida_metas_mes, r.metasMes)
            r.ejes?.let { ejes ->
                item {
                    RutaCard {
                        Text(stringResource(R.string.perfil_tab_ejes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        Eje.entries.zip(ejes).forEach { (e, v) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                                Text(stringResource(com.rutaalacima.app.ui.i18n.Textos.nombre(e)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(120.dp))
                                LinearProgressIndicator(progress = { v / 10f }, modifier = Modifier.weight(1f))
                                Text("  $v", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
            if (r.visionTotal > 0) item {
                RutaCard { Text(stringResource(R.string.vision_progreso, r.visionConFoto, r.visionTotal), style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
