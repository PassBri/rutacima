package com.rutaalacima.app.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.data.local.MesEntity
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.diasMarcados
import com.rutaalacima.app.ui.components.AssetImage
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.util.hoy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val LOCALE_CO = Locale("es", "CO")

fun nombreMes(mes: Int): String =
    java.time.Month.of(mes).getDisplayName(TextStyle.FULL_STANDALONE, LOCALE_CO).replaceFirstChar { it.uppercase() }

/** Portada de montaña del mes (11 ilustraciones del Planificador Anual, en ciclo). */
fun portadaMes(mes: Int): String = "plan/mes_%02d.jpg".format(((mes - 1) % 11) + 1)

@OptIn(ExperimentalCoroutinesApi::class)
class MesViewModel(private val c: AppContainer) : ViewModel() {
    private val actual = MutableStateFlow(YearMonth.from(hoy()))
    var periodo by mutableStateOf(actual.value)
        private set
    var mes by mutableStateOf<MesEntity?>(null)
        private set
    private var carga: Job? = null
    private var guardado: Job? = null

    val metas: StateFlow<List<MetaMensualEntity>> = actual
        .flatMapLatest { c.planAnual.metasMes(it.year, it.monthValue) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val diasConAgenda: StateFlow<Set<Int>> = actual
        .flatMapLatest { c.agenda.diasConAgenda(it.year, it.monthValue) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init { cargar() }

    fun ir(ym: YearMonth) {
        if (ym == periodo && mes != null) return
        guardarYa()
        periodo = ym
        actual.value = ym
        cargar()
    }

    private fun cargar() {
        mes = null
        carga?.cancel()
        val ym = periodo
        carga = viewModelScope.launch { mes = c.planAnual.mes(ym.year, ym.monthValue) }
    }

    fun editar(transform: (MesEntity) -> MesEntity) {
        val nuevo = transform(mes ?: return)
        mes = nuevo
        guardado?.cancel()
        guardado = c.appScope.launch { delay(500); c.planAnual.guardarMes(nuevo) }
    }

    fun agregarMeta(texto: String) {
        if (texto.isBlank()) return
        val ym = periodo
        viewModelScope.launch {
            c.planAnual.guardarMetaMes(
                MetaMensualEntity(anio = ym.year, mes = ym.monthValue, orden = metas.value.size + 1, texto = texto.trim()),
            )
        }
    }

    fun alternarDia(m: MetaMensualEntity, dia: Int) = viewModelScope.launch {
        val dias = m.diasMarcados().let { if (dia in it) it - dia else it + dia }
        c.planAnual.guardarMetaMes(m.copy(dias = dias.sorted().joinToString(",")))
    }

    fun alternarCumplida(m: MetaMensualEntity) = viewModelScope.launch { c.planAnual.guardarMetaMes(m.copy(cumplida = !m.cumplida)) }
    fun eliminarMeta(m: MetaMensualEntity) = viewModelScope.launch { c.planAnual.eliminarMetaMes(m) }

    private fun guardarYa() {
        val m = mes ?: return
        if (guardado?.isActive == true) {
            guardado?.cancel()
            c.appScope.launch { c.planAnual.guardarMes(m) }
        }
    }

    override fun onCleared() = guardarYa()
}

/** Página mensual del Planificador Anual. */
@Composable
fun MonthPlannerTab(vm: MesViewModel, bottom: Dp, onAbrirDia: (LocalDate) -> Unit) {
    val metas by vm.metas.collectAsStateWithLifecycle()
    val diasAgenda by vm.diasConAgenda.collectAsStateWithLifecycle()
    val ym = vm.periodo
    val m = vm.mes
    var nuevaMeta by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.ir(ym.minusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior") }
                Text("${nombreMes(ym.monthValue)} ${ym.year}", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { vm.ir(ym.plusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente") }
            }
        }
        item { AssetImage(portadaMes(ym.monthValue), 900f / 630f, contentDescription = "Portada de ${nombreMes(ym.monthValue)}") }
        item { Calendario(ym, diasAgenda, onAbrirDia) }

        // Metas mensuales con registro de cumplimiento
        item { SectionTitle("Metas mensuales (${metas.size}/10)") }
        item {
            Text("Registra el cumplimiento de cada meta tocando los días.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(metas, key = { it.id }) { meta -> MetaMensualCard(meta, ym, vm) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(nuevaMeta, { nuevaMeta = it }, label = { Text("Nueva meta del mes") },
                    modifier = Modifier.weight(1f), singleLine = true)
                IconButton(onClick = { vm.agregarMeta(nuevaMeta); nuevaMeta = "" }, enabled = nuevaMeta.isNotBlank()) {
                    Icon(Icons.Filled.Add, "Agregar meta")
                }
            }
        }

        if (m != null) {
            item { SectionTitle("Actividades y notas") }
            item { CampoMes("Actividades", m.actividades) { v -> vm.editar { it.copy(actividades = v) } } }
            item { CampoMes("Notas importantes", m.notas) { v -> vm.editar { it.copy(notas = v) } } }
            item { SectionTitle("Balance mensual") }
            item { CampoMes("¿Cómo estuvo mi mes?", m.comoEstuvo) { v -> vm.editar { it.copy(comoEstuvo = v) } } }
            item { CampoMes("Me siento agradecido por", m.agradecido) { v -> vm.editar { it.copy(agradecido = v) } } }
            item { CampoMes("Lo que necesito mejorar", m.mejorar) { v -> vm.editar { it.copy(mejorar = v) } } }
            item { CampoMes("Logros", m.logros) { v -> vm.editar { it.copy(logros = v) } } }
            item { CampoMes("Desafíos y lecciones", m.desafios) { v -> vm.editar { it.copy(desafios = v) } } }
            item { CampoMes("Objetivos para el próximo mes", m.objetivosProximo) { v -> vm.editar { it.copy(objetivosProximo = v) } } }
        }
    }
}

@Composable
private fun CampoMes(titulo: String, valor: String, onChange: (String) -> Unit) {
    OutlinedTextField(valor, onChange, label = { Text(titulo) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
}

/** Calendario del mes (domingo a sábado). Un punto marca los días con planificador diario. */
@Composable
private fun Calendario(ym: YearMonth, diasAgenda: Set<Int>, onAbrirDia: (LocalDate) -> Unit) {
    val primero = ym.atDay(1)
    val desfase = primero.dayOfWeek.value % 7 // domingo = 0
    val dias = ym.lengthOfMonth()
    val hoyFecha = hoy()
    RutaCard {
        Row(Modifier.fillMaxWidth()) {
            listOf("D", "L", "M", "M", "J", "V", "S").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(4.dp))
        val celdas = desfase + dias
        val filas = (celdas + 6) / 7
        for (f in 0 until filas) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dia = f * 7 + col - desfase + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (dia in 1..dias) {
                            val fecha = ym.atDay(dia)
                            val esHoy = fecha == hoyFecha
                            Column(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                    .background(if (esHoy) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onAbrirDia(fecha) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text("$dia", style = MaterialTheme.typography.bodyMedium,
                                    color = if (esHoy) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                if (dia in diasAgenda) {
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                                }
                            }
                        }
                    }
                }
            }
        }
        Text("Toca un día para abrir su planificador diario.", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetaMensualCard(meta: MetaMensualEntity, ym: YearMonth, vm: MesViewModel) {
    val marcados = meta.diasMarcados()
    val dias = ym.lengthOfMonth()
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = meta.cumplida, onCheckedChange = { vm.alternarCumplida(meta) })
            Column(Modifier.weight(1f)) {
                Text(meta.texto, style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (meta.cumplida) TextDecoration.LineThrough else null)
                Text("${marcados.size}/$dias días", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { vm.eliminarMeta(meta) }) { Icon(Icons.Outlined.Delete, "Eliminar meta") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (d in 1..dias) {
                val hecho = d in marcados
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(if (hecho) MaterialTheme.colorScheme.secondary else Color.Transparent)
                        .border(1.dp, if (hecho) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { vm.alternarDia(meta, d) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$d", fontSize = 10.sp, fontWeight = if (hecho) FontWeight.Bold else FontWeight.Normal,
                        color = if (hecho) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
