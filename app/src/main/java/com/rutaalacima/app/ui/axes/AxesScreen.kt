package com.rutaalacima.app.ui.axes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.ui.i18n.Textos
import com.rutaalacima.app.ui.i18n.texto
import com.rutaalacima.app.data.local.EvaluacionEjesEntity
import com.rutaalacima.app.data.local.puntajes
import com.rutaalacima.app.data.local.total
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.ui.components.EjeChip
import com.rutaalacima.app.ui.components.EmptyState
import com.rutaalacima.app.ui.components.RadarEjes
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.ScaleSelector
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.asColor
import com.rutaalacima.app.util.formatoFecha
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AxesViewModel(private val c: AppContainer) : ViewModel() {
    val historial: StateFlow<List<EvaluacionEjesEntity>> =
        c.ejes.historial.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Borrador de la evaluación en curso (null = sin marcar). */
    val borrador = mutableStateListOf<Int?>(null, null, null, null, null, null)
    var nota by mutableStateOf("")

    val completo: Boolean get() = borrador.all { it != null }

    fun guardar() {
        if (!completo) return
        val v = borrador.map { it ?: 0 }
        viewModelScope.launch {
            c.ejes.guardar(
                EvaluacionEjesEntity(
                    voluntad = v[0], maestria = v[1], voz = v[2],
                    valor = v[3], evolucion = v[4], trascendencia = v[5],
                    nota = nota.trim(),
                ),
            )
            for (i in borrador.indices) borrador[i] = null
            nota = ""
        }
    }

    fun eliminar(e: EvaluacionEjesEntity) = viewModelScope.launch { c.ejes.eliminar(e) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxesScreen(onBack: () -> Unit) {
    val vm = rutaViewModel { AxesViewModel(it) }
    val historial by vm.historial.collectAsStateWithLifecycle()
    val ultima = historial.firstOrNull()
    val anterior = historial.getOrNull(1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mis_ejes)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.ejes_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            if (ultima == null) {
                EmptyState(stringResource(R.string.ejes_vacio_titulo), stringResource(R.string.ejes_vacio_texto))
            } else {
                RutaCard {
                    Text(stringResource(R.string.ultima_evaluacion, formatoFecha(ultima.fecha)), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    RadarEjes(ultima.puntajes(), comparacion = anterior?.puntajes())
                    Text(stringResource(R.string.total_de_60, ultima.total()), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(Textos.interpretacionTotal(ultima.total())), style = MaterialTheme.typography.bodyMedium)
                    if (anterior != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.linea_punteada, formatoFecha(anterior.fecha)),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    val pares = Eje.entries.zip(ultima.puntajes())
                    val debiles = pares.sortedBy { it.second }.take(2).map { "${it.first.texto()} (${it.second})" }
                    val fuerte = pares.maxBy { it.second }
                    Text(stringResource(R.string.ejes_atencion, debiles.joinToString()), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.eje_mas_fuerte, "${fuerte.first.texto()} (${fuerte.second})"), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { SectionTitle(stringResource(R.string.nueva_evaluacion)) }
        items(Eje.entries) { eje ->
            val i = eje.ordinal
            RutaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EjeChip(eje)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Textos.lema(eje)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Textos.pregunta(eje)), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(10.dp))
                ScaleSelector(value = vm.borrador[i], onChange = { vm.borrador[i] = it }, color = eje.color.asColor())
                vm.borrador[i]?.let {
                    Text(stringResource(Textos.interpretacionEje(it)), style = MaterialTheme.typography.labelMedium, color = eje.color.asColor(),
                        modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        item {
            OutlinedTextField(
                value = vm.nota, onValueChange = { vm.nota = it },
                label = { Text(stringResource(R.string.nota_opcional)) }, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            val suma = vm.borrador.sumOf { it ?: 0 }
            Button(onClick = vm::guardar, enabled = vm.completo, modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.completo) stringResource(R.string.guardar_evaluacion, suma) else stringResource(R.string.evalua_6_ejes))
            }
        }
        if (historial.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.historial)) }
            items(historial, key = { it.id }) { e ->
                RutaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(formatoFecha(e.fecha), style = MaterialTheme.typography.titleMedium)
                            Text(
                                Eje.entries.zip(e.puntajes()).joinToString("  ") { "${it.first.codigo} ${it.second}" },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (e.nota.isNotBlank()) Text(e.nota, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("${e.total()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { vm.eliminar(e) }) { Icon(Icons.Outlined.Delete, stringResource(R.string.eliminar)) }
                    }
                }
            }
        }
    }
    }
}
