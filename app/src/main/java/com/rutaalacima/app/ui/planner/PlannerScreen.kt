package com.rutaalacima.app.ui.planner

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rutaalacima.app.data.local.BalanceAnualEntity
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.PropositoEntity
import com.rutaalacima.app.domain.model.Decision
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.EstadoMeta
import com.rutaalacima.app.domain.model.Prioridad
import com.rutaalacima.app.domain.model.aniosDelPlan
import com.rutaalacima.app.ui.components.ChipSelector
import com.rutaalacima.app.ui.components.EjeChip
import com.rutaalacima.app.ui.components.EmptyState
import com.rutaalacima.app.ui.components.FechaField
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.formatoFechaUtc
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.asColor

@Composable
fun PlannerScreen(
    contentPadding: PaddingValues,
    onOpenProposito: (Long) -> Unit,
    onOpenWorkbook: (String) -> Unit,
) {
    val vm = rutaViewModel { PlannerViewModel(it) }
    val agenda = rutaViewModel { AgendaViewModel(it) }
    val mes = rutaViewModel { MesViewModel(it) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val titulos = listOf("Hoy", "Mes", "Año", "5 años", "Balance")

    Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text("Planificador", style = MaterialTheme.typography.headlineMedium)
            Text("Del día a tu cumbre de 5 años: cada paso cuenta.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ScrollableTabRow(selectedTabIndex = tab, containerColor = Color.Transparent, edgePadding = 8.dp,
            modifier = Modifier.padding(top = 8.dp)) {
            titulos.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        val bottom = contentPadding.calculateBottomPadding()
        when (tab) {
            0 -> DayPlannerTab(agenda, bottom)
            1 -> MonthPlannerTab(mes, bottom) { fecha -> agenda.abrir(fecha); tab = 0 }
            2 -> MetasTab(vm, bottom) { anioMes, numMes -> mes.ir(java.time.YearMonth.of(anioMes, numMes)); tab = 1 }
            3 -> PropositosTab(vm, bottom, onOpenProposito)
            else -> BalanceTab(vm, bottom, onOpenWorkbook)
        }
    }
}

// ---------------------------------------------------------------- Propósitos

@Composable
private fun PropositosTab(vm: PlannerViewModel, bottom: androidx.compose.ui.unit.Dp, onOpen: (Long) -> Unit) {
    val propositos by vm.propositos.collectAsStateWithLifecycle()
    var leyenda by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottom + 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Haz una lista de 10 metas que quieres lograr en los próximos 5 años (${propositos.size}/10).",
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { leyenda = true }) { Icon(Icons.Outlined.Info, "Sistema ABCD") }
                }
            }
            if (propositos.isEmpty()) {
                item {
                    EmptyState(
                        "Tu cumbre de 5 años: tu destino",
                        "Imagina tu vida ideal dentro de 5 años. ¿Qué logros te harían sentir orgulloso? Escribe aquí las metas que transformarán tu vida.",
                    )
                }
            }
            items(propositos, key = { it.id }) { p -> PropositoCard(p) { onOpen(p.id) } }
        }
        ExtendedFloatingActionButton(
            onClick = { onOpen(0L) },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Nuevo propósito") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottom + 16.dp),
        )
    }
    if (leyenda) {
        AlertDialog(
            onDismissRequest = { leyenda = false },
            confirmButton = { TextButton(onClick = { leyenda = false }) { Text("Entendido") } },
            title = { Text("Prioridad: sistema ABCD") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Prioridad.entries.forEach { Text("${it.clave} · ${it.nivel}: ${it.descripcion}", style = MaterialTheme.typography.bodyMedium) }
                }
            },
        )
    }
}

@Composable
private fun PropositoCard(p: PropositoEntity, onClick: () -> Unit) {
    val prioridad = Prioridad.from(p.prioridad)
    RutaCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.orden.toString().padStart(2, '0'), style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.titulo, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrioridadBadge(prioridad)
                    Eje.fromCodigo(p.eje)?.let { EjeChip(it) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        ProgressLine(p.progreso / 100f)
        Text("${p.progreso}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PrioridadBadge(p: Prioridad) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(p.color.asColor()).padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(p.clave, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- Metas anuales

@Composable
private fun SelectorAnio(anios: List<Int>, actual: Int, onSelect: (Int) -> Unit) {
    ChipSelector(
        titulo = null,
        opciones = anios,
        seleccionado = actual,
        etiqueta = { a -> "Año ${anios.indexOf(a) + 1} · $a" },
        onSelect = onSelect,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetasTab(vm: PlannerViewModel, bottom: androidx.compose.ui.unit.Dp, onAbrirMes: (Int, Int) -> Unit) {
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val todas by vm.metas.collectAsStateWithLifecycle()
    val propositos by vm.propositos.collectAsStateWithLifecycle()
    val anios = aniosDelPlan(perfil.anioInicioPlan)
    var anio by rememberSaveable { mutableIntStateOf(-1) }
    val anioSel = if (anio in anios) anio else anios.first()
    var editando by remember { mutableStateOf<MetaAnualEntity?>(null) }
    var leyenda by remember { mutableStateOf(false) }
    val metas = todas.filter { it.anio == anioSel }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottom + 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SelectorAnio(anios, anioSel) { anio = it }
                val conBalance by remember(anioSel) { vm.mesesConBalance(anioSel) }.collectAsState(initial = emptySet())
                Text("Meses del año (● = balance mensual hecho). Toca uno para abrirlo.", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D").forEachIndexed { i, ini ->
                        val hecho = (i + 1) in conBalance
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onAbrirMes(anioSel, i + 1) }.padding(2.dp)) {
                            Text(ini, style = MaterialTheme.typography.labelSmall)
                            Box(Modifier.size(16.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (hecho) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Construye tu año extraordinario: tus 10 metas del año (campamentos base).", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { leyenda = true }) { Icon(Icons.Outlined.Info, "Matriz y semáforo") }
                }
            }
            if (metas.isEmpty()) {
                item { EmptyState("Metas ($anioSel)", "Agrega las metas de este año: actividad, matriz de decisión, prioridad, avance, obstáculo, próxima acción y fecha de revisión.") }
            }
            items(metas, key = { it.id }) { m -> MetaCard(m, propositos) { editando = m } }
        }
        ExtendedFloatingActionButton(
            onClick = { editando = MetaAnualEntity(anio = anioSel, titulo = "") },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Nueva meta") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottom + 16.dp),
        )
    }

    editando?.let { meta ->
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { editando = null }, sheetState = sheet) {
            MetaEditor(
                inicial = meta,
                propositos = propositos,
                onGuardar = { vm.guardarMeta(it); editando = null },
                onEliminar = if (meta.id != 0L) ({ vm.eliminarMeta(meta); editando = null }) else null,
            )
        }
    }
    if (leyenda) {
        AlertDialog(
            onDismissRequest = { leyenda = false },
            confirmButton = { TextButton(onClick = { leyenda = false }) { Text("Entendido") } },
            title = { Text("Matriz de decisión y semáforo") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Decision.entries.forEach { Text("${it.nombre}: ${it.descripcion}", style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.height(8.dp))
                    EstadoMeta.entries.forEach {
                        Text("${it.porcentaje?.let { p -> "$p%" } ?: "—"} ${it.nombre}: ${it.significado}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
        )
    }
}

@Composable
private fun MetaCard(m: MetaAnualEntity, propositos: List<PropositoEntity>, onClick: () -> Unit) {
    val estado = EstadoMeta.from(m.estado)
    RutaCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrioridadBadge(Prioridad.from(m.prioridad))
            Spacer(Modifier.width(8.dp))
            Text(m.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        propositos.firstOrNull { it.id == m.propositoId }?.let {
            Text("Propósito: ${it.titulo}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (m.subMetas.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            m.subMetas.lines().filter { it.isNotBlank() }.forEachIndexed { i, s ->
                Text("${'a' + i}. $s", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(estado.color.asColor()))
            Spacer(Modifier.width(6.dp))
            Text("${estado.nombre} · ${m.avance}% · ${Decision.from(m.decision).nombre}", style = MaterialTheme.typography.labelLarge)
        }
        ProgressLine(m.avance / 100f, Modifier.padding(top = 6.dp), color = estado.color.asColor())
        if (m.obstaculo.isNotBlank()) Text("Obstáculo: ${m.obstaculo}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        if (m.proximaAccion.isNotBlank()) Text("Próxima acción: ${m.proximaAccion}", style = MaterialTheme.typography.bodyMedium)
        if (m.fechaInicio != null || m.fechaFin != null) {
            Text("${m.fechaInicio?.let { formatoFechaUtc(it) } ?: "—"}  →  ${m.fechaFin?.let { formatoFechaUtc(it) } ?: "—"}",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        m.fechaRevision?.let { Text("Revisión: ${formatoFechaUtc(it)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun MetaEditor(
    inicial: MetaAnualEntity,
    propositos: List<PropositoEntity>,
    onGuardar: (MetaAnualEntity) -> Unit,
    onEliminar: (() -> Unit)?,
) {
    var m by remember(inicial.id) { mutableStateOf(inicial) }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (inicial.id == 0L) "Nueva meta · ${inicial.anio}" else "Editar meta · ${inicial.anio}", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(m.titulo, { m = m.copy(titulo = it) }, label = { Text("Actividad / meta") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(m.subMetas, { m = m.copy(subMetas = it) }, label = { Text("Sub-metas (una por línea: a, b, c, d…)") },
            modifier = Modifier.fillMaxWidth(), minLines = 3)
        if (propositos.isNotEmpty()) {
            ChipSelector(
                "Propósito al que aporta",
                listOf<PropositoEntity?>(null) + propositos,
                propositos.firstOrNull { it.id == m.propositoId },
                { it?.titulo?.take(28) ?: "Ninguno" },
                { m = m.copy(propositoId = it?.id) },
            )
        }
        ChipSelector("Matriz de decisión", Decision.entries, Decision.from(m.decision), { it.nombre }, { m = m.copy(decision = it.name) })
        ChipSelector("Prioridad", Prioridad.entries, Prioridad.from(m.prioridad), { "${it.clave} · ${it.nivel}" },
            { m = m.copy(prioridad = it.name) }, color = { it.color.asColor() })
        Text("Avance: ${m.avance}%", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = m.avance.toFloat(),
            onValueChange = {
                val v = (it / 5).toInt() * 5
                val estado = EstadoMeta.from(m.estado)
                m = m.copy(avance = v, estado = if (estado == EstadoMeta.EN_PAUSA) estado.name else EstadoMeta.desdePorcentaje(v).name)
            },
            valueRange = 0f..100f,
        )
        ChipSelector("Estado (semáforo)", EstadoMeta.entries, EstadoMeta.from(m.estado), { it.nombre },
            { m = m.copy(estado = it.name, avance = it.porcentaje ?: m.avance) }, color = { it.color.asColor() })
        OutlinedTextField(m.obstaculo, { m = m.copy(obstaculo = it) }, label = { Text("Obstáculo principal") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(m.proximaAccion, { m = m.copy(proximaAccion = it) }, label = { Text("Próxima acción") }, modifier = Modifier.fillMaxWidth())
        FechaField("Inicio", m.fechaInicio, { m = m.copy(fechaInicio = it) })
        FechaField("Fin", m.fechaFin, { m = m.copy(fechaFin = it) })
        FechaField("Fecha de revisión", m.fechaRevision, { m = m.copy(fechaRevision = it) })
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            onEliminar?.let { OutlinedButton(onClick = it) { Text("Eliminar") } }
            Spacer(Modifier.weight(1f))
            Button(onClick = { onGuardar(m.copy(titulo = m.titulo.trim())) }, enabled = m.titulo.isNotBlank()) { Text("Guardar") }
        }
    }
}

// ---------------------------------------------------------------- Balance anual

@Composable
private fun BalanceTab(vm: PlannerViewModel, bottom: androidx.compose.ui.unit.Dp, onOpenWorkbook: (String) -> Unit) {
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val anios = aniosDelPlan(perfil.anioInicioPlan)
    var anio by rememberSaveable { mutableIntStateOf(-1) }
    val anioSel = if (anio in anios) anio else anios.first()
    val guardado by remember(anioSel) { vm.balance(anioSel) }.collectAsState(initial = null)
    var b by remember(anioSel, guardado) { mutableStateOf(guardado ?: BalanceAnualEntity(anio = anioSel)) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Año de inicio del plan: ${perfil.anioInicioPlan}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.cambiarAnioInicio(perfil.anioInicioPlan - 1) }) { Icon(Icons.Outlined.Remove, "Año anterior") }
                IconButton(onClick = { vm.cambiarAnioInicio(perfil.anioInicioPlan + 1) }) { Icon(Icons.Filled.Add, "Año siguiente") }
            }
            SelectorAnio(anios, anioSel) { anio = it }
        }
        item {
            Text("Balance anual ($anioSel)", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("No es un juicio: es una herramienta de aprendizaje para ajustar tu ruta, celebrar victorias y extraer lecciones.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { CampoBalance("¿Cómo me sentí?", b.comoMeSenti) { b = b.copy(comoMeSenti = it) } }
        item { CampoBalance("Reflexión del año · Lo positivo", b.positivo) { b = b.copy(positivo = it) } }
        item { CampoBalance("Reflexión del año · Lo negativo", b.negativo) { b = b.copy(negativo = it) } }
        item { CampoBalance("Lo mejor del año", b.loMejor) { b = b.copy(loMejor = it) } }
        item { CampoBalance("Objetivos para el próximo año", b.objetivosProximo) { b = b.copy(objetivosProximo = it) } }
        item {
            Button(onClick = { vm.guardarBalance(b.copy(anio = anioSel)) }, modifier = Modifier.fillMaxWidth(), enabled = b != guardado) {
                Text(if (b == guardado) "Balance guardado" else "Guardar balance")
            }
        }
        item {
            RutaCard(onClick = { onOpenWorkbook("planificador_cierre") }) {
                Text("Cierre de los 5 años", style = MaterialTheme.typography.titleMedium)
                Text("Evaluación de propósitos, reflexiones y \"¿Quién soy ahora?\".", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CampoBalance(titulo: String, valor: String, onChange: (String) -> Unit) {
    OutlinedTextField(valor, onChange, label = { Text(titulo) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
}
