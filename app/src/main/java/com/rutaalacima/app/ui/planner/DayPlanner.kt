package com.rutaalacima.app.ui.planner

import com.rutaalacima.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.data.local.AgendaDiaEntity
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.util.formatoDia
import com.rutaalacima.app.util.hoy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** Franjas de "Planifique su día": de 4:00 am a 3:00 am. */
val HORAS_DEL_DIA: List<Int> = (4..23).toList() + (0..3).toList()

fun etiquetaHora(h: Int): String = when {
    h == 0 -> "12:00 am"
    h < 12 -> "$h:00 am"
    h == 12 -> "12:00 pm"
    else -> "${h - 12}:00 pm"
}

private val horarioSerializer = MapSerializer(String.serializer(), String.serializer())
private val json = Json { ignoreUnknownKeys = true }

fun AgendaDiaEntity.horarioMapa(): Map<Int, String> =
    runCatching { json.decodeFromString(horarioSerializer, horario) }.getOrDefault(emptyMap())
        .mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()

class AgendaViewModel(private val c: AppContainer) : ViewModel() {
    var fecha by mutableStateOf(hoy())
        private set
    var dia by mutableStateOf<AgendaDiaEntity?>(null)
        private set
    private var carga: Job? = null
    private var guardado: Job? = null

    init {
        carga = viewModelScope.launch { dia = c.agenda.dia(fecha) }
    }

    fun abrir(f: LocalDate) {
        if (f == fecha && dia != null) return
        guardarYa()
        fecha = f
        dia = null
        carga?.cancel()
        carga = viewModelScope.launch { dia = c.agenda.dia(f) }
    }

    fun editar(transform: (AgendaDiaEntity) -> AgendaDiaEntity) {
        val actual = dia ?: return
        val nuevo = transform(actual)
        dia = nuevo
        guardado?.cancel()
        guardado = c.appScope.launch {
            delay(500)
            c.agenda.guardar(nuevo)
        }
    }

    fun hora(h: Int, texto: String) = editar { d ->
        val mapa = d.horarioMapa().toMutableMap()
        if (texto.isBlank()) mapa.remove(h) else mapa[h] = texto
        d.copy(horario = json.encodeToString(horarioSerializer, mapa.mapKeys { it.key.toString() }))
    }

    private fun guardarYa() {
        val d = dia ?: return
        if (guardado?.isActive == true) {
            guardado?.cancel()
            c.appScope.launch { c.agenda.guardar(d) }
        }
    }

    override fun onCleared() {
        guardarYa()
    }
}

/** Planificador diario ("Serie Ruta a la Cima"). */
@Composable
fun DayPlannerTab(vm: AgendaViewModel, bottom: Dp) {
    val d = vm.dia
    var horarioCompleto by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.abrir(vm.fecha.minusDays(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.dia_anterior)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatoDia(vm.fecha), style = MaterialTheme.typography.titleMedium)
                    if (vm.fecha != hoy()) {
                        TextButton(onClick = { vm.abrir(hoy()) }) { Text(stringResource(R.string.ir_a_hoy)) }
                    }
                }
                IconButton(onClick = { vm.abrir(vm.fecha.plusDays(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.dia_siguiente)) }
            }
        }
        if (d == null) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            return@LazyColumn
        }
        item {
            RutaCard {
                CampoDia(stringResource(R.string.intencion_dia), d.intencion) { v -> vm.editar { it.copy(intencion = v) } }
                Spacer(Modifier.height(8.dp))
                CampoDia(stringResource(R.string.prioridad_1_hoy), d.prioridad) { v -> vm.editar { it.copy(prioridad = v) } }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.energia), style = MaterialTheme.typography.labelLarge)
                Circulos(total = 5, valor = d.energia, color = MaterialTheme.colorScheme.secondary) { n -> vm.editar { it.copy(energia = n) } }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.agua_vasos), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..8) {
                        val lleno = i <= d.agua
                        Icon(
                            Icons.Filled.WaterDrop,
                            contentDescription = stringResource(R.string.vaso_n, i),
                            tint = if (lleno) Color(0xFF2E7D9A) else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(30.dp).clip(CircleShape)
                                .clickable { vm.editar { it.copy(agua = if (it.agua == i) i - 1 else i) } },
                        )
                    }
                }
            }
        }
        item { SectionTitle(stringResource(R.string.planifique_dia)) }
        val horario = d.horarioMapa()
        val horas = if (horarioCompleto) HORAS_DEL_DIA else HORAS_DEL_DIA.filter { it in 5..22 || horario.containsKey(it) }
        items(horas, key = { "h$it" }) { h ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(etiquetaHora(h), modifier = Modifier.width(76.dp), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = horario[h].orEmpty(),
                    onValueChange = { vm.hora(h, it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            TextButton(onClick = { horarioCompleto = !horarioCompleto }) {
                Text(stringResource(if (horarioCompleto) R.string.ver_horario_corto else R.string.ver_24_horas))
            }
        }
        item { SectionTitle(stringResource(R.string.mi_dia)) }
        item { CampoDia(stringResource(R.string.metas), d.metas, 3) { v -> vm.editar { it.copy(metas = v) } } }
        item { CampoDia(stringResource(R.string.pendientes), d.pendientes, 3) { v -> vm.editar { it.copy(pendientes = v) } } }
        item { CampoDia(stringResource(R.string.victorias_dia), d.victorias, 2) { v -> vm.editar { it.copy(victorias = v) } } }
        item { CampoDia(stringResource(R.string.aprendizaje), d.aprendizaje, 2) { v -> vm.editar { it.copy(aprendizaje = v) } } }
        item { CampoDia(stringResource(R.string.gratitud), d.gratitud, 2) { v -> vm.editar { it.copy(gratitud = v) } } }
        item { SectionTitle(stringResource(R.string.control_financiero)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { CampoDia(stringResource(R.string.ingresos), d.ingresos) { v -> vm.editar { it.copy(ingresos = v) } } }
                Box(Modifier.weight(1f)) { CampoDia(stringResource(R.string.gastos), d.gastos) { v -> vm.editar { it.copy(gastos = v) } } }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { CampoDia(stringResource(R.string.ahorro), d.ahorro) { v -> vm.editar { it.copy(ahorro = v) } } }
                Box(Modifier.weight(1f)) { CampoDia(stringResource(R.string.inversion), d.inversion) { v -> vm.editar { it.copy(inversion = v) } } }
            }
        }
        item { CampoDia(stringResource(R.string.notas), d.notas, 3) { v -> vm.editar { it.copy(notas = v) } } }
    }
}

@Composable
private fun CampoDia(titulo: String, valor: String, lineas: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(titulo) },
        modifier = Modifier.fillMaxWidth(),
        minLines = lineas,
        singleLine = lineas == 1,
    )
}

/** Fila de círculos marcables (○ ○ ○ ○ ○). Tocar el último marcado lo desmarca. */
@Composable
fun Circulos(total: Int, valor: Int, color: Color, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..total) {
            val lleno = i <= valor
            Box(
                Modifier.size(26.dp).clip(CircleShape)
                    .background(if (lleno) color else Color.Transparent)
                    .border(1.5.dp, if (lleno) color else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onChange(if (valor == i) i - 1 else i) },
                contentAlignment = Alignment.Center,
            ) {
                Text("$i", style = MaterialTheme.typography.labelSmall,
                    color = if (lleno) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}
