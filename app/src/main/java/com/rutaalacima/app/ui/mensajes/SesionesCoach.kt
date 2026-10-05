package com.rutaalacima.app.ui.mensajes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.SesionCoach
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class SesionesViewModel(c: AppContainer) : ViewModel() {
    private val repo = c.cordadas
    var sesiones by mutableStateOf<List<SesionCoach>>(emptyList())
        private set
    var notas by mutableStateOf<String?>(null)
        private set
    var aviso by mutableStateOf<String?>(null)
    private var guardado: Job? = null

    fun cargar(acomp: String, conNotas: Boolean) = viewModelScope.launch {
        runCatching { repo.sesiones(acomp) }.onSuccess { sesiones = it }
        if (conNotas) notas = runCatching { repo.notasPrivadas(acomp) }.getOrDefault("")
    }

    fun agendar(acomp: String, dia: LocalDate, hora: LocalTime, minutos: Int, enlace: String, tema: String) = viewModelScope.launch {
        val inicio = dia.atTime(hora).atZone(ZoneId.systemDefault()).toInstant()
        runCatching { repo.agendar(acomp, inicio, minutos, enlace, tema) }.onFailure { aviso = it.message }
        cargar(acomp, false)
    }

    fun cancelar(acomp: String, id: String) = viewModelScope.launch { runCatching { repo.cancelarSesion(id) }; cargar(acomp, false) }

    /** Las notas privadas se guardan solas mientras escribes. */
    fun escribirNotas(acomp: String, texto: String) {
        notas = texto
        guardado?.cancel()
        guardado = viewModelScope.launch { delay(800); runCatching { repo.guardarNotasPrivadas(acomp, texto) }.onFailure { aviso = it.message } }
    }
}

/** Sesiones con el coach de vida: la próxima con su enlace, las siguientes y agendar una nueva. */
@Composable
fun SesionesCoach(acompId: String, esCoach: Boolean) {
    val vm = rutaViewModel(key = "sesiones-$acompId") { SesionesViewModel(it) }
    LaunchedEffect(acompId) { vm.cargar(acompId, esCoach) }
    val abrir = LocalUriHandler.current
    val formato = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT) }
    val ahora = System.currentTimeMillis()
    val proximas = vm.sesiones.filter { it.inicio + it.minutos * 60_000L > ahora }
    var agendando by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Event, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.sesiones_titulo), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (!agendando) TextButton(onClick = { agendando = true }) { Text(stringResource(R.string.sesiones_agendar)) }
        }
        if (proximas.isEmpty() && !agendando) Text(stringResource(R.string.sesiones_ninguna), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        proximas.forEach { s ->
            val cuando = Instant.ofEpochMilli(s.inicio).atZone(ZoneId.systemDefault()).format(formato)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(cuando, style = MaterialTheme.typography.labelLarge)
                    Text(listOf(stringResource(R.string.sesiones_minutos, s.minutos), s.tema).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (s.enlace.isNotBlank()) TextButton(onClick = { runCatching { abrir.openUri(s.enlace) } }) {
                    Icon(Icons.Filled.Videocam, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.sesiones_unirme))
                }
                TextButton(onClick = { vm.cancelar(acompId, s.id) }) { Text(stringResource(R.string.cancelar), color = MaterialTheme.colorScheme.error) }
            }
        }
        if (agendando) FormularioSesion(onAgendar = { d, h, m, e, t -> vm.agendar(acompId, d, h, m, e, t); agendando = false }, onCancelar = { agendando = false })
        vm.aviso?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioSesion(onAgendar: (LocalDate, LocalTime, Int, String, String) -> Unit, onCancelar: () -> Unit) {
    val hoy = LocalDate.now()
    var dia by remember { mutableStateOf(hoy.plusDays(1)) }
    var hora by remember { mutableStateOf(LocalTime.of(19, 0)) }
    var minutos by remember { mutableStateOf(45) }
    var enlace by remember { mutableStateOf("") }
    var tema by remember { mutableStateOf("") }
    val formatoDia = remember { DateTimeFormatter.ofPattern("EEE d") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.sesiones_dia), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0L..13L).map { hoy.plusDays(it) }.forEach { d -> FilterChip(selected = d == dia, onClick = { dia = d }, label = { Text(d.format(formatoDia)) }) }
        }
        Text(stringResource(R.string.sesiones_hora), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(6, 7, 8, 9, 12, 13, 17, 18, 19, 20, 21).map { LocalTime.of(it, 0) }.forEach { h ->
                FilterChip(selected = h == hora, onClick = { hora = h }, label = { Text("%02d:00".format(h.hour)) })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(30, 45, 60, 90).forEach { m -> FilterChip(selected = m == minutos, onClick = { minutos = m }, label = { Text(stringResource(R.string.sesiones_minutos, m)) }) }
        }
        OutlinedTextField(tema, { tema = it.take(200) }, label = { Text(stringResource(R.string.sesiones_tema)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(enlace, { enlace = it.trim().take(500) }, label = { Text(stringResource(R.string.sesiones_enlace)) },
            placeholder = { Text("https://meet.google.com/…") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            isError = enlace.isNotEmpty() && !enlace.startsWith("https://"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAgendar(dia, hora, minutos, enlace, tema) }, enabled = enlace.isEmpty() || enlace.startsWith("https://")) {
                Text(stringResource(R.string.sesiones_agendar))
            }
            OutlinedButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    }
}

/** Notas privadas del coach sobre la persona que acompaña (ella no las ve). */
@Composable
fun NotasPrivadasCoach(acompId: String) {
    val vm = rutaViewModel(key = "sesiones-$acompId") { SesionesViewModel(it) }
    LaunchedEffect(acompId) { vm.cargar(acompId, true) }
    RutaCard {
        Text(stringResource(R.string.notas_coach_titulo), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.notas_coach_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val notas = vm.notas
        if (notas == null) Text(stringResource(R.string.cargando), style = MaterialTheme.typography.bodySmall)
        else OutlinedTextField(notas, { vm.escribirNotas(acompId, it.take(8000)) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), minLines = 4)
    }
}
