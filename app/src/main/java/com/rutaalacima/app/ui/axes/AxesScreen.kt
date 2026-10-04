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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.data.local.EvaluacionEjesEntity
import com.rutaalacima.app.data.local.puntajes
import com.rutaalacima.app.data.local.total
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.interpretarEje
import com.rutaalacima.app.domain.model.interpretarTotal
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

@Composable
fun AxesScreen(contentPadding: PaddingValues) {
    val vm = rutaViewModel { AxesViewModel(it) }
    val historial by vm.historial.collectAsStateWithLifecycle()
    val ultima = historial.firstOrNull()
    val anterior = historial.getOrNull(1)

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Tus 6 Ejes", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Atravesar las 7 fases × activar los 6 ejes = tu Cumbre Personal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            if (ultima == null) {
                EmptyState("Antes de ascender, debes saber dónde estás parado", "Evalúa cada eje del 1 al 10 según tu situación ACTUAL (no donde quieres estar, sino donde ESTÁS hoy).")
            } else {
                RutaCard {
                    Text("Última evaluación · ${formatoFecha(ultima.fecha)}", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    RadarEjes(ultima.puntajes(), comparacion = anterior?.puntajes())
                    Text("Total: ${ultima.total()}/60", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(interpretarTotal(ultima.total()), style = MaterialTheme.typography.bodyMedium)
                    if (anterior != null) {
                        Spacer(Modifier.height(4.dp))
                        Text("Línea punteada: evaluación anterior (${formatoFecha(anterior.fecha)}).",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    val debiles = Eje.entries.zip(ultima.puntajes()).sortedBy { it.second }.take(2)
                    val fuerte = Eje.entries.zip(ultima.puntajes()).maxBy { it.second }
                    Text("Ejes que necesitan atención: ${debiles.joinToString { "${it.first.nombre} (${it.second})" }}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Eje más fuerte: ${fuerte.first.nombre} (${fuerte.second})", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { SectionTitle("Nueva evaluación") }
        items(Eje.entries) { eje ->
            val i = eje.ordinal
            RutaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EjeChip(eje)
                    Spacer(Modifier.width(8.dp))
                    Text(eje.lema, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Text(eje.preguntaEvaluacion, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(10.dp))
                ScaleSelector(value = vm.borrador[i], onChange = { vm.borrador[i] = it }, color = eje.color.asColor())
                vm.borrador[i]?.let {
                    Text(interpretarEje(it), style = MaterialTheme.typography.labelMedium, color = eje.color.asColor(),
                        modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        item {
            OutlinedTextField(
                value = vm.nota, onValueChange = { vm.nota = it },
                label = { Text("Nota (opcional)") }, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            val suma = vm.borrador.sumOf { it ?: 0 }
            Button(onClick = vm::guardar, enabled = vm.completo, modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.completo) "Guardar evaluación ($suma/60)" else "Evalúa los 6 ejes para guardar")
            }
        }
        if (historial.isNotEmpty()) {
            item { SectionTitle("Historial") }
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
                        IconButton(onClick = { vm.eliminar(e) }) { Icon(Icons.Outlined.Delete, "Eliminar") }
                    }
                }
            }
        }
    }
}
