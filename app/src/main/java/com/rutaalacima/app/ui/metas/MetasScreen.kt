package com.rutaalacima.app.ui.metas

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.Cascada
import com.rutaalacima.app.data.NodoAnio
import com.rutaalacima.app.data.NodoMes
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.ui.components.EjeChip
import com.rutaalacima.app.ui.components.MontanaArte
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.planner.AgendaViewModel
import com.rutaalacima.app.ui.planner.BalanceTab
import com.rutaalacima.app.ui.planner.DayPlannerTab
import com.rutaalacima.app.ui.planner.MesViewModel
import com.rutaalacima.app.ui.planner.MetasTab
import com.rutaalacima.app.ui.planner.MonthPlannerTab
import com.rutaalacima.app.ui.planner.PlannerViewModel
import com.rutaalacima.app.ui.planner.PropositosTab
import com.rutaalacima.app.ui.planner.nombreMes
import com.rutaalacima.app.ui.theme.asColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

class CascadaViewModel(c: AppContainer) : ViewModel() {
    val cascada: StateFlow<Cascada?> = c.cascada.cascada.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Metas: la cascada completa y los planificadores de día, mes, año, 5 años y balance. */
@Composable
fun MetasScreen(
    contentPadding: PaddingValues,
    onNuevaMeta: (NivelMeta) -> Unit,
    onOpenProposito: (Long) -> Unit,
    onOpenWorkbook: (String) -> Unit,
    tabInicial: Int = 0,
    fechaInicial: java.time.LocalDate? = null,
) {
    val vm = rutaViewModel { PlannerViewModel(it) }
    val agenda = rutaViewModel { AgendaViewModel(it) }
    val mes = rutaViewModel { MesViewModel(it) }
    val cascadaVm = rutaViewModel { CascadaViewModel(it) }
    var tab by rememberSaveable { mutableIntStateOf(tabInicial) }
    // Al llegar desde la cascada de la vida: abrir el día o el mes elegido.
    androidx.compose.runtime.LaunchedEffect(fechaInicial) {
        fechaInicial?.let { agenda.abrir(it); mes.ir(YearMonth.from(it)) }
    }
    val titulos = listOf(
        R.string.metas_tab_cascada, R.string.metas_tab_dia, R.string.metas_tab_mes,
        R.string.metas_tab_anio, R.string.metas_tab_5anios, R.string.metas_tab_balance,
    )
    Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        ScrollableTabRow(selectedTabIndex = tab, containerColor = Color.Transparent, edgePadding = 8.dp) {
            titulos.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(t)) }) }
        }
        val bottom = contentPadding.calculateBottomPadding()
        when (tab) {
            0 -> CascadaTab(
                cascadaVm, bottom, onNuevaMeta, onOpenProposito,
                onAbrirAnio = { tab = 3 },
                onAbrirMes = { ym -> mes.ir(ym); tab = 2 },
            )
            1 -> DayPlannerTab(agenda, bottom)
            2 -> MonthPlannerTab(mes, bottom) { fecha -> agenda.abrir(fecha); tab = 1 }
            3 -> MetasTab(vm, bottom, onAbrirMes = { a, m -> mes.ir(YearMonth.of(a, m)); tab = 2 }, onNueva = { onNuevaMeta(NivelMeta.ANIO) })
            4 -> PropositosTab(vm, bottom, onOpenProposito, onNuevo = { onNuevaMeta(NivelMeta.CINCO_ANIOS) })
            else -> BalanceTab(vm, bottom, onOpenWorkbook)
        }
    }
}

@Composable
private fun CascadaTab(
    vm: CascadaViewModel,
    bottom: Dp,
    onNuevaMeta: (NivelMeta) -> Unit,
    onOpenProposito: (Long) -> Unit,
    onAbrirAnio: () -> Unit,
    onAbrirMes: (YearMonth) -> Unit,
) {
    val cascada by vm.cascada.collectAsStateWithLifecycle()
    val c = cascada
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MontanaArte("cascada", Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp)), paleta = 0) {
                Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                    Text(stringResource(R.string.cascada_titulo), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.cascada_texto), style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NivelMeta.entries.forEach { n ->
                    FilledTonalButton(onClick = { onNuevaMeta(n) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(6.dp)) {
                        Icon(Icons.Filled.Add, null)
                        Text(stringResource(n.corto), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (c == null) return@LazyColumn
        if (c.propositos.isEmpty() && c.aniosSueltos.isEmpty() && c.mesesSueltos.isEmpty()) {
            item {
                RutaCard(onClick = { onNuevaMeta(NivelMeta.CINCO_ANIOS) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.cascada_vacia_titulo), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.cascada_vacia_texto), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        items(c.propositos, key = { "p${it.proposito.id}" }) { n ->
            RutaCard(onClick = { onOpenProposito(n.proposito.id) }) {
                Encabezado(
                    nivel = stringResource(R.string.proposito_n_anios, n.proposito.horizonte), titulo = n.proposito.titulo,
                    eje = Eje.fromCodigo(n.proposito.eje), avance = n.avance, automatico = n.automatico,
                )
                if (n.anios.isEmpty()) {
                    Text(stringResource(R.string.cascada_sin_anuales), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
                n.anios.forEach { a -> Rama { NodoAnioView(a, onAbrirAnio, onAbrirMes) } }
            }
        }
        if (c.aniosSueltos.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.cascada_anuales_sueltas)) }
            items(c.aniosSueltos, key = { "a${it.meta.id}" }) { a -> RutaCard { NodoAnioView(a, onAbrirAnio, onAbrirMes) } }
        }
        if (c.mesesSueltos.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.cascada_mensuales_sueltas)) }
            items(c.mesesSueltos, key = { "m${it.meta.id}" }) { m -> RutaCard { NodoMesView(m, onAbrirMes) } }
        }
    }
}

/** Rama de la cascada: línea vertical dorada a la izquierda. */
@Composable
private fun Rama(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(top = 8.dp)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun NodoAnioView(a: NodoAnio, onAbrirAnio: () -> Unit, onAbrirMes: (YearMonth) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            Column {
                Encabezado(
                    nivel = stringResource(R.string.nivel_anio_n, a.meta.anio), titulo = a.meta.titulo,
                    eje = Eje.fromCodigo(a.meta.eje), avance = a.avance, automatico = a.automatico, onClick = onAbrirAnio,
                )
            }
        }
        a.meses.forEach { m -> Rama { NodoMesView(m, onAbrirMes) } }
    }
}

@Composable
private fun NodoMesView(m: NodoMes, onAbrirMes: (YearMonth) -> Unit) {
    Encabezado(
        nivel = "${nombreMes(m.meta.mes)} ${m.meta.anio}", titulo = m.meta.texto,
        eje = Eje.fromCodigo(m.meta.eje), avance = m.avance, automatico = true,
        onClick = { onAbrirMes(YearMonth.of(m.meta.anio, m.meta.mes)) },
    )
}

@Composable
private fun Encabezado(nivel: String, titulo: String, eje: Eje?, avance: Float, automatico: Boolean, onClick: (() -> Unit)? = null) {
    val color = eje?.color?.asColor() ?: MaterialTheme.colorScheme.secondary
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(nivel.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f))
            Text("${(avance * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
        }
        Text(titulo, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        ProgressLine(avance, color = color)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            eje?.let { EjeChip(it) }
            if (automatico) {
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.avance_automatico), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Planificador completo como pantalla propia (se abre desde la Ruta de la vida). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlanificadorScreen(
    tabInicial: Int,
    fechaInicial: java.time.LocalDate?,
    onBack: () -> Unit,
    onNuevaMeta: (NivelMeta) -> Unit,
    onOpenProposito: (Long) -> Unit,
    onOpenWorkbook: (String) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.planificador)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver),
                        )
                    }
                },
            )
        },
    ) { padding ->
        MetasScreen(padding, onNuevaMeta, onOpenProposito, onOpenWorkbook, tabInicial, fechaInicial)
    }
}
