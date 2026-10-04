package com.rutaalacima.app.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.CoachMensajeEntity
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CoachViewModel(private val c: AppContainer) : ViewModel() {
    val mensajes: StateFlow<List<CoachMensajeEntity>> = c.coach.conversacion.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var texto by mutableStateOf("")
    var pensando by mutableStateOf(false)
        private set
    val conIA: Boolean get() = c.coach.conIA

    fun enviar(t: String = texto) {
        if (t.isBlank() || pensando) return
        texto = ""
        pensando = true
        viewModelScope.launch {
            runCatching { c.coach.enviar(t) }
            pensando = false
        }
    }

    fun borrar() = viewModelScope.launch { c.coach.borrar() }
}

/** Coach de IA: orienta a la persona con el método Ruta a la Cima y su información real. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(onBack: () -> Unit) {
    val vm = rutaViewModel { CoachViewModel(it) }
    val mensajes by vm.mensajes.collectAsStateWithLifecycle()
    val lista = rememberLazyListState()
    LaunchedEffect(mensajes.size, vm.pensando) { if (mensajes.isNotEmpty()) lista.animateScrollToItem(mensajes.size) }
    val sugerencias = listOf(
        R.string.coach_sug_cumbre, R.string.coach_sug_dividir, R.string.coach_sug_motivacion,
        R.string.coach_sug_semana, R.string.coach_sug_eje, R.string.coach_sug_habito,
    ).map { stringResource(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.size(8.dp))
                        Column {
                            Text(stringResource(R.string.coach))
                            Text(
                                stringResource(if (vm.conIA) R.string.coach_con_ia else R.string.coach_sin_ia),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = { IconButton(onClick = { vm.borrar() }) { Icon(Icons.Outlined.DeleteSweep, stringResource(R.string.borrar_conversacion)) } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(sugerencias) { s -> AssistChip(onClick = { vm.enviar(s) }, label = { Text(s, maxLines = 1) }) }
                    }
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            vm.texto, { vm.texto = it }, placeholder = { Text(stringResource(R.string.escribe_al_coach)) },
                            modifier = Modifier.weight(1f), maxLines = 5, shape = RoundedCornerShape(24.dp),
                        )
                        IconButton(onClick = { vm.enviar() }, enabled = vm.texto.isNotBlank() && !vm.pensando) {
                            Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.enviar), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = lista,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Burbuja(stringResource(R.string.coach_bienvenida), deCoach = true) }
            items(mensajes, key = { it.id }) { m -> Burbuja(m.texto, deCoach = m.rol == "assistant") }
            if (vm.pensando) item { Box(Modifier.padding(8.dp)) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) } }
        }
    }
}

@Composable
private fun Burbuja(texto: String, deCoach: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (deCoach) Alignment.CenterStart else Alignment.CenterEnd) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyLarge,
            color = if (deCoach) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    if (deCoach) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (deCoach) 4.dp else 18.dp, bottomEnd = if (deCoach) 18.dp else 4.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
