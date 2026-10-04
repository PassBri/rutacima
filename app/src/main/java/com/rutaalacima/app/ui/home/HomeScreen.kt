package com.rutaalacima.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.content.ContentRepository
import com.rutaalacima.app.data.content.WorkbookSummary
import com.rutaalacima.app.data.local.EvaluacionEjesEntity
import com.rutaalacima.app.data.local.PerfilEntity
import com.rutaalacima.app.data.local.RespuestaEntity
import com.rutaalacima.app.data.local.puntajes
import com.rutaalacima.app.data.local.total
import com.rutaalacima.app.domain.model.ChecklistDiario
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.Fase
import com.rutaalacima.app.domain.model.Tarjetas
import com.rutaalacima.app.domain.model.interpretarTotal
import com.rutaalacima.app.ui.components.ChipSelector
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.RadarEjes
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.asColor
import com.rutaalacima.app.ui.workbook.QuoteView
import com.rutaalacima.app.util.hoy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private fun <T> estado(f: kotlinx.coroutines.flow.Flow<T>, inicial: T): StateFlow<T> =
        f.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), inicial)

    val perfil: StateFlow<PerfilEntity> = estado(c.perfil.perfil, PerfilEntity())
    val ultima: StateFlow<EvaluacionEjesEntity?> = estado(c.ejes.ultima, null)
    val checksHoy: StateFlow<Set<String>> = estado(c.checklist.dia(hoy()), emptySet())
    val ultimaRespuesta: StateFlow<RespuestaEntity?> = estado(c.respuestas.ultimaRespuesta, null)
    val indice: StateFlow<List<WorkbookSummary>> = estado(flow { emit(c.contenido.index()) }, emptyList())

    fun guardarCumbre(frase: String) = viewModelScope.launch { c.perfil.actualizar { it.copy(cumbreFrase = frase.trim()) } }
    fun guardarFase(fase: Fase) = viewModelScope.launch { c.perfil.actualizar { it.copy(faseActual = fase.name) } }
}

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenWorkbook: (String) -> Unit,
    onGoToAxes: () -> Unit,
    onGoToKit: () -> Unit,
    onGoToPlan: () -> Unit = {},
) {
    val vm = rutaViewModel { HomeViewModel(it) }
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val ultima by vm.ultima.collectAsStateWithLifecycle()
    val checks by vm.checksHoy.collectAsStateWithLifecycle()
    val ultimaRespuesta by vm.ultimaRespuesta.collectAsStateWithLifecycle()
    val indice by vm.indice.collectAsStateWithLifecycle()
    var editarCumbre by remember { mutableStateOf(false) }
    val fase = Fase.fromName(perfil.faseActual)

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.sello_ruta), contentDescription = "Sello Ruta a la Cima", modifier = Modifier.size(56.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(if (perfil.nombre.isBlank()) "Hola, senderista" else "Hola, ${perfil.nombre.substringBefore(' ')}",
                        style = MaterialTheme.typography.headlineSmall)
                    Text("Tu montaña te espera.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Cumbre en una frase
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("MI CUMBRE PERSONAL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = { editarCumbre = true }) {
                            Icon(Icons.Outlined.Edit, "Editar", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Text(
                        perfil.cumbreFrase.ifBlank { "Define tu cumbre en una declaración poderosa de máximo 25 palabras." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        // Fase actual
        item {
            RutaCard {
                Text("¿En qué fase estás HOY?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                ChipSelector(null, Fase.entries, fase, { "${it.numero}. ${it.nombre}" }, { vm.guardarFase(it) })
                fase?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it.autodiagnostico, style = MaterialTheme.typography.bodyMedium)
                    Text("Eje que se activa: ${it.ejeActivado.nombre} · Próximo portal: ${it.nombre} → ${it.siguiente.nombre}",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onOpenWorkbook(ContentRepository.PORTALES) }) { Text("Ver rituales del portal") }
                }
            }
        }

        // Ejes
        item {
            RutaCard(onClick = onGoToAxes) {
                Text("Tus 6 ejes", style = MaterialTheme.typography.titleMedium)
                val u = ultima
                if (u == null) {
                    Text("Aún no te has evaluado. Toca para medir dónde estás hoy.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    RadarEjes(u.puntajes())
                    Text("${u.total()}/60 · ${interpretarTotal(u.total())}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Checklist de hoy
        item {
            RutaCard(onClick = onGoToKit) {
                Text("Checklist de hoy", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                ProgressLine(checks.size / ChecklistDiario.TOTAL.toFloat())
                Text("${checks.size}/${ChecklistDiario.TOTAL} · ${ChecklistDiario.lectura(checks.size)}",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Planificador diario
        item {
            RutaCard(onClick = onGoToPlan) {
                Text("Planificador de hoy", style = MaterialTheme.typography.titleMedium)
                Text("Intención, prioridad #1, horario, victorias y gratitud.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Continuar
        item {
            val wid = ultimaRespuesta?.workbookId
            val w = indice.firstOrNull { it.id == wid }
            if (w != null) {
                RutaCard(onClick = { onOpenWorkbook(w.id) }) {
                    Text("Continúa donde quedaste", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(w.title, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Empieza tu ascenso")
                    RutaCard(onClick = { onOpenWorkbook(ContentRepository.DESCUBRE) }) {
                        Text("1 · Descubre tu Cumbre Personal", style = MaterialTheme.typography.titleMedium)
                        Text("El modelo 7 Fases × 6 Ejes y tu plan de 90 días.", style = MaterialTheme.typography.bodyMedium)
                    }
                    RutaCard(onClick = { onOpenWorkbook(ContentRepository.DIAGNOSTICO) }) {
                        Text("2 · Diagnóstico Personal", style = MaterialTheme.typography.titleMedium)
                        Text("Antes de ascender, conócete a ti mismo.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Tarjeta del día
        item {
            val eje = Eje.entries[hoy().dayOfYear % Eje.entries.size]
            Column {
                Text("Recordatorio del día · ${eje.nombre}", style = MaterialTheme.typography.labelLarge, color = eje.color.asColor())
                QuoteView(Tarjetas.frases.getValue(eje))
            }
        }
    }

    if (editarCumbre) {
        var texto by remember { mutableStateOf(perfil.cumbreFrase) }
        val palabras = texto.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        AlertDialog(
            onDismissRequest = { editarCumbre = false },
            title = { Text("Mi Cumbre Personal es…") },
            text = {
                Column {
                    OutlinedTextField(texto, { texto = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Text("$palabras/25 palabras", style = MaterialTheme.typography.labelSmall,
                        color = if (palabras > 25) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { vm.guardarCumbre(texto); editarCumbre = false }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { editarCumbre = false }) { Text("Cancelar") } },
        )
    }
}
