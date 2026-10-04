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
import androidx.compose.ui.res.stringResource
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
import com.rutaalacima.app.R
import com.rutaalacima.app.ui.i18n.Textos
import com.rutaalacima.app.ui.i18n.texto

// ---------------------------------------------------------------- Propósitos

@Composable
internal fun PropositosTab(vm: PlannerViewModel, bottom: androidx.compose.ui.unit.Dp, onOpen: (Long) -> Unit, onNuevo: () -> Unit) {
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
                        stringResource(R.string.propositos_intro, propositos.size),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { leyenda = true }) { Icon(Icons.Outlined.Info, stringResource(R.string.prioridad_abcd)) }
                }
            }
            if (propositos.isEmpty()) {
                item {
                    EmptyState(stringResource(R.string.propositos_vacio_titulo), stringResource(R.string.propositos_vacio_texto))
                }
            }
            items(propositos, key = { it.id }) { p -> PropositoCard(p) { onOpen(p.id) } }
        }
        ExtendedFloatingActionButton(
            onClick = onNuevo,
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text(stringResource(R.string.nuevo_proposito)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottom + 16.dp),
        )
    }
    if (leyenda) {
        AlertDialog(
            onDismissRequest = { leyenda = false },
            confirmButton = { TextButton(onClick = { leyenda = false }) { Text(stringResource(R.string.entendido)) } },
            title = { Text(stringResource(R.string.prioridad_abcd)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Prioridad.entries.forEach { Text("${it.clave} · " + stringResource(Textos.nivel(it)) + ": " + stringResource(Textos.descripcion(it)), style = MaterialTheme.typography.bodyMedium) }
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
        etiqueta = { a -> stringResource(R.string.anio_n, anios.indexOf(a) + 1) + " · $a" },
        onSelect = onSelect,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MetasTab(vm: PlannerViewModel, bottom: androidx.compose.ui.unit.Dp, onAbrirMes: (Int, Int) -> Unit, onNueva: () -> Unit) {
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
                Text(stringResource(R.string.meses_del_anio_ayuda), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    (1..12).map { java.time.Month.of(it).getDisplayName(java.time.format.TextStyle.NARROW_STANDALONE, java.util.Locale.getDefault()) }.forEachIndexed { i, ini ->
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
                    Text(stringResource(R.string.metas_anio_intro), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { leyenda = true }) { Icon(Icons.Outlined.Info, stringResource(R.string.matriz_semaforo)) }
                }
            }
            if (metas.isEmpty()) {
                item { EmptyState(stringResource(R.string.metas_anio_vacio_titulo, anioSel), stringResource(R.string.metas_anio_vacio_texto)) }
            }
            items(metas, key = { it.id }) { m -> MetaCard(m, propositos) { editando = m } }
        }
        ExtendedFloatingActionButton(
            onClick = onNueva,
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text(stringResource(R.string.nueva_meta)) },
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
            confirmButton = { TextButton(onClick = { leyenda = false }) { Text(stringResource(R.string.entendido)) } },
            title = { Text(stringResource(R.string.matriz_semaforo)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Decision.entries.forEach { Text(stringResource(Textos.nombre(it)) + ": " + stringResource(Textos.descripcion(it)), style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.height(8.dp))
                    EstadoMeta.entries.forEach {
                        Text((it.porcentaje?.let { p -> "$p%" } ?: "—") + " " + stringResource(Textos.nombre(it)) + ": " + stringResource(Textos.significado(it)), style = MaterialTheme.typography.bodyMedium)
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
            Text(stringResource(R.string.proposito_x, it.titulo), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(estado.texto() + " · ${m.avance}% · " + Decision.from(m.decision).texto(), style = MaterialTheme.typography.labelLarge)
        }
        ProgressLine(m.avance / 100f, Modifier.padding(top = 6.dp), color = estado.color.asColor())
        if (m.obstaculo.isNotBlank()) Text(stringResource(R.string.obstaculo_x, m.obstaculo), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        if (m.proximaAccion.isNotBlank()) Text(stringResource(R.string.proxima_accion_x, m.proximaAccion), style = MaterialTheme.typography.bodyMedium)
        if (m.fechaInicio != null || m.fechaFin != null) {
            Text("${m.fechaInicio?.let { formatoFechaUtc(it) } ?: "—"}  →  ${m.fechaFin?.let { formatoFechaUtc(it) } ?: "—"}",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        m.fechaRevision?.let { Text(stringResource(R.string.revision_x, formatoFechaUtc(it)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        Text(stringResource(if (inicial.id == 0L) R.string.nueva_meta else R.string.editar_meta) + " · ${inicial.anio}", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(m.titulo, { m = m.copy(titulo = it) }, label = { Text(stringResource(R.string.actividad_meta)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(m.subMetas, { m = m.copy(subMetas = it) }, label = { Text(stringResource(R.string.submetas_ayuda)) },
            modifier = Modifier.fillMaxWidth(), minLines = 3)
        if (propositos.isNotEmpty()) {
            ChipSelector(
                stringResource(R.string.proposito_aporta),
                listOf<PropositoEntity?>(null) + propositos,
                propositos.firstOrNull { it.id == m.propositoId },
                { it?.titulo?.take(28) ?: stringResource(R.string.ninguno) },
                { m = m.copy(propositoId = it?.id) },
            )
        }
        ChipSelector(stringResource(R.string.matriz_decision), Decision.entries, Decision.from(m.decision), { it.texto() }, { m = m.copy(decision = it.name) })
        ChipSelector(stringResource(R.string.prioridad), Prioridad.entries, Prioridad.from(m.prioridad), { "${it.clave} · " + it.texto() },
            { m = m.copy(prioridad = it.name) }, color = { it.color.asColor() })
        Text(stringResource(R.string.avance_pct, m.avance), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = m.avance.toFloat(),
            onValueChange = {
                val v = (it / 5).toInt() * 5
                val estado = EstadoMeta.from(m.estado)
                m = m.copy(avance = v, estado = if (estado == EstadoMeta.EN_PAUSA) estado.name else EstadoMeta.desdePorcentaje(v).name)
            },
            valueRange = 0f..100f,
        )
        ChipSelector(stringResource(R.string.estado_semaforo), EstadoMeta.entries, EstadoMeta.from(m.estado), { it.texto() },
            { m = m.copy(estado = it.name, avance = it.porcentaje ?: m.avance) }, color = { it.color.asColor() })
        OutlinedTextField(m.obstaculo, { m = m.copy(obstaculo = it) }, label = { Text(stringResource(R.string.obstaculo_principal)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(m.proximaAccion, { m = m.copy(proximaAccion = it) }, label = { Text(stringResource(R.string.proxima_accion)) }, modifier = Modifier.fillMaxWidth())
        FechaField(stringResource(R.string.inicio), m.fechaInicio, { m = m.copy(fechaInicio = it) })
        FechaField(stringResource(R.string.fin), m.fechaFin, { m = m.copy(fechaFin = it) })
        FechaField(stringResource(R.string.fecha_revision), m.fechaRevision, { m = m.copy(fechaRevision = it) })
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            onEliminar?.let { OutlinedButton(onClick = it) { Text(stringResource(R.string.eliminar)) } }
            Spacer(Modifier.weight(1f))
            Button(onClick = { onGuardar(m.copy(titulo = m.titulo.trim())) }, enabled = m.titulo.isNotBlank()) { Text(stringResource(R.string.guardar)) }
        }
    }
}

// ---------------------------------------------------------------- Balance anual

@Composable
internal fun BalanceTab(vm: PlannerViewModel, bottom: androidx.compose.ui.unit.Dp, onOpenWorkbook: (String) -> Unit) {
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
                Text(stringResource(R.string.anio_inicio_plan, perfil.anioInicioPlan), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.cambiarAnioInicio(perfil.anioInicioPlan - 1) }) { Icon(Icons.Outlined.Remove, stringResource(R.string.anio_anterior)) }
                IconButton(onClick = { vm.cambiarAnioInicio(perfil.anioInicioPlan + 1) }) { Icon(Icons.Filled.Add, stringResource(R.string.anio_siguiente)) }
            }
            SelectorAnio(anios, anioSel) { anio = it }
        }
        item {
            Text(stringResource(R.string.balance_anual_x, anioSel), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.balance_intro),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { CampoBalance(stringResource(R.string.bal_como_me_senti), b.comoMeSenti) { b = b.copy(comoMeSenti = it) } }
        item { CampoBalance(stringResource(R.string.bal_positivo), b.positivo) { b = b.copy(positivo = it) } }
        item { CampoBalance(stringResource(R.string.bal_negativo), b.negativo) { b = b.copy(negativo = it) } }
        item { CampoBalance(stringResource(R.string.bal_lo_mejor), b.loMejor) { b = b.copy(loMejor = it) } }
        item { CampoBalance(stringResource(R.string.bal_objetivos), b.objetivosProximo) { b = b.copy(objetivosProximo = it) } }
        item {
            Button(onClick = { vm.guardarBalance(b.copy(anio = anioSel)) }, modifier = Modifier.fillMaxWidth(), enabled = b != guardado) {
                Text(stringResource(if (b == guardado) R.string.balance_guardado else R.string.guardar_balance))
            }
        }
        item {
            RutaCard(onClick = { onOpenWorkbook("planificador_cierre") }) {
                Text(stringResource(R.string.cierre_5_anios), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.cierre_5_anios_sub), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CampoBalance(titulo: String, valor: String, onChange: (String) -> Unit) {
    OutlinedTextField(valor, onChange, label = { Text(titulo) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
}
