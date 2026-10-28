package com.rutaalacima.app.ui.planner

import androidx.compose.foundation.layout.widthIn
import com.rutaalacima.app.ui.components.MontanaArte
import com.rutaalacima.app.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.layout.heightIn
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


fun nombreMes(mes: Int): String =
    java.time.Month.of(mes).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }

/** Portada de montaña del mes (11 ilustraciones del Planificador Anual, en ciclo). */

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

    /** Las metas del año de este mes: aquí se escoge cuáles se trabajan (orden de mayor a menor, igual que la web). */
    val anuales: StateFlow<List<com.rutaalacima.app.data.local.MetaAnualEntity>> =
        kotlinx.coroutines.flow.combine(c.planificador.todasLasMetas, actual) { ms, ym -> ms.filter { it.anio == ym.year }.sortedWith(compareBy({ it.prioridad }, { it.id })) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Para escoger solo algunas sub-metas de una meta en el mes (igual que la web: "mes:pasos:<id>"). */
    val casillas: StateFlow<List<com.rutaalacima.app.data.local.VisionCasillaEntity>> = c.db.visionDao().observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val mandala: StateFlow<Map<String, String>> = c.respuestas.observar(com.rutaalacima.app.domain.model.Mandala.WORKBOOK).stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    fun alternarPasoMes(m: MetaMensualEntity, p: Int) = viewModelScope.launch {
        val s = com.rutaalacima.app.data.vision.LibroMetas.pasosDelMes(m.id).toMutableSet(); if (!s.add(p)) s.remove(p)
        c.respuestas.guardar("metas", "mes:pasos:${m.id}", s.sorted().joinToString(","))
    }

    /** Escoger (o quitar) una meta del año para este mes: crea (o quita) su meta del mes. */
    fun elegirAnual(anual: com.rutaalacima.app.data.local.MetaAnualEntity, si: Boolean) = viewModelScope.launch {
        val ym = periodo
        val actualMes = metas.value.firstOrNull { it.metaAnualId == anual.id }
        if (si && actualMes == null) c.planAnual.guardarMetaMes(
            MetaMensualEntity(anio = ym.year, mes = ym.monthValue, orden = metas.value.size + 1, texto = anual.titulo, metaAnualId = anual.id))
        else if (!si && actualMes != null) c.planAnual.eliminarMetaMes(actualMes)
    }

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
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MonthPlannerTab(vm: MesViewModel, bottom: Dp, onAbrirDia: (LocalDate) -> Unit) {
    val metas by vm.metas.collectAsStateWithLifecycle()
    val diasAgenda by vm.diasConAgenda.collectAsStateWithLifecycle()
    val ym = vm.periodo
    val m = vm.mes
    val anuales by vm.anuales.collectAsStateWithLifecycle()
    val casillasV by vm.casillas.collectAsStateWithLifecycle()
    val mandala by vm.mandala.collectAsStateWithLifecycle()
    var quitar by remember { mutableStateOf<Pair<com.rutaalacima.app.data.local.MetaAnualEntity, Int>?>(null) }
    quitar?.let { (anual, n) ->
        androidx.compose.material3.AlertDialog(onDismissRequest = { quitar = null },
            text = { Text(stringResource(R.string.foco_quitar_q, anual.titulo, n)) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { vm.elegirAnual(anual, false); quitar = null }) { Text(stringResource(R.string.foco_quitar)) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { quitar = null }) { Text(stringResource(R.string.cancelar)) } })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.ir(ym.minusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.mes_anterior)) }
                Text("${nombreMes(ym.monthValue)} ${ym.year}", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { vm.ir(ym.plusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.mes_siguiente)) }
            }
        }
        item { MontanaArte("mes-${ym.monthValue}", Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(20.dp)), paleta = ym.monthValue) }
        item { Calendario(ym, diasAgenda, onAbrirDia) }

        // Metas mensuales con registro de cumplimiento
        item { SectionTitle(stringResource(R.string.foco_mes_titulo, ym.year, nombreMes(ym.monthValue).lowercase())) }
        item {
            Text(stringResource(R.string.foco_mes_ayuda), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Las metas del año que se trabajan este mes: se marcan aquí; cada una lleva su registro de días
        if (anuales.isEmpty()) item {
            Text(stringResource(R.string.foco_sin_metas, ym.year), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(anuales, key = { "a" + it.id }) { anual ->
            val delMes = metas.firstOrNull { it.metaAnualId == anual.id }
            Column {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = delMes != null, onCheckedChange = { si ->
                        if (!si && delMes != null && delMes.diasMarcados().isNotEmpty()) quitar = anual to delMes.diasMarcados().size
                        else vm.elegirAnual(anual, si)
                    })
                    Text(anual.titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                        fontWeight = if (delMes != null) FontWeight.SemiBold else FontWeight.Normal)
                }
                if (delMes != null) {
                    // Completa o solo algunas sub-metas este mes
                    val cas = com.rutaalacima.app.data.vision.SincroBrujula.casillaDe(anual, anuales, casillasV)
                    val pasos = cas?.let { c0 -> (0 until com.rutaalacima.app.domain.model.Mandala.PASOS).mapNotNull { p ->
                        mandala[com.rutaalacima.app.domain.model.Mandala.clave(c0.id, p)].orEmpty().trim().takeIf { it.isNotEmpty() }?.let { p to it } } }.orEmpty()
                    if (pasos.isNotEmpty()) {
                        val elegidos = com.rutaalacima.app.data.vision.LibroMetas.pasosDelMes(delMes.id)
                        Text(if (elegidos.isEmpty()) stringResource(R.string.mes_completa) else stringResource(R.string.mes_pasos_n, elegidos.size, pasos.size),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp))
                        androidx.compose.foundation.layout.FlowRow(Modifier.padding(start = 12.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pasos.forEach { (p, t) ->
                                androidx.compose.material3.FilterChip(selected = p in elegidos, onClick = { vm.alternarPasoMes(delMes, p) },
                                    label = { Text("${p + 1}. $t", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp)) })
                            }
                        }
                    }
                    MetaMensualCard(delMes, ym, vm)
                }
            }
        }

        item(key = "bienestar-$ym") { com.rutaalacima.app.ui.bienestar.BienestarMesCard(ym, onDia = onAbrirDia) }
        if (m != null) {
            item { SectionTitle(stringResource(R.string.actividades_notas)) }
            item { CampoMes(stringResource(R.string.actividades), m.actividades) { v -> vm.editar { it.copy(actividades = v) } } }
            item { CampoMes(stringResource(R.string.notas_importantes), m.notas) { v -> vm.editar { it.copy(notas = v) } } }
            item { SectionTitle(stringResource(R.string.balance_mensual)) }
            item { CampoMes(stringResource(R.string.bal_como_mes), m.comoEstuvo) { v -> vm.editar { it.copy(comoEstuvo = v) } } }
            item { CampoMes(stringResource(R.string.bal_agradecido), m.agradecido) { v -> vm.editar { it.copy(agradecido = v) } } }
            item { CampoMes(stringResource(R.string.bal_mejorar), m.mejorar) { v -> vm.editar { it.copy(mejorar = v) } } }
            item { CampoMes(stringResource(R.string.logros), m.logros) { v -> vm.editar { it.copy(logros = v) } } }
            item { CampoMes(stringResource(R.string.bal_desafios), m.desafios) { v -> vm.editar { it.copy(desafios = v) } } }
            item { CampoMes(stringResource(R.string.bal_objetivos_mes), m.objetivosProximo) { v -> vm.editar { it.copy(objetivosProximo = v) } } }
        }
        // Finanzas del mes: la suma del control financiero de cada día, con presupuesto (igual que en la web)
        item(key = "finanzas-$ym") { com.rutaalacima.app.ui.finanzas.ResumenCuentasMes(ym) }
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
            (0..6).map { java.time.DayOfWeek.SUNDAY.plus(it.toLong()).getDisplayName(TextStyle.NARROW_STANDALONE, Locale.getDefault()) }.forEach {
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
        Text(stringResource(R.string.toca_dia), style = MaterialTheme.typography.labelSmall,
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
                com.rutaalacima.app.ui.components.ApodoLista("m", meta.id)
                Text(stringResource(R.string.n_de_dias, marcados.size, dias), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { vm.eliminarMeta(meta) }) { Icon(Icons.Outlined.Delete, stringResource(R.string.eliminar_meta)) }
        }
        com.rutaalacima.app.ui.components.ApodoField("m", meta.id)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (d in 1..dias) {
                val hecho = d in marcados
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(if (hecho) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .border(1.dp, if (hecho) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { vm.alternarDia(meta, d) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$d", fontSize = 10.sp, fontWeight = if (hecho) FontWeight.Bold else FontWeight.Normal,
                        color = if (hecho) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
