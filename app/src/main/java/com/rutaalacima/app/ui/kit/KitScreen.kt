package com.rutaalacima.app.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.rutaalacima.app.data.content.ContentRepository
import com.rutaalacima.app.data.content.Workbook
import com.rutaalacima.app.domain.model.ChecklistDiario
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.MatrizDecisiones
import com.rutaalacima.app.domain.model.MatrizDecisiones.Respuesta
import com.rutaalacima.app.domain.model.Niebla
import java.util.Locale
import com.rutaalacima.app.ui.components.ChipSelector
import com.rutaalacima.app.ui.components.EjeChip
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.asColor
import com.rutaalacima.app.util.formatoDia
import com.rutaalacima.app.util.hoy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class KitViewModel(private val c: AppContainer) : ViewModel() {
    var kit by mutableStateOf<Workbook?>(null)
        private set
    var niebla by mutableStateOf<Workbook?>(null)
        private set

    /** Checks de los últimos 7 días (para el tracker). */
    val semana: StateFlow<Map<LocalDate, Set<String>>> =
        c.checklist.desde(hoy().minusDays(6)).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch {
            kit = c.contenido.workbook(ContentRepository.KIT)
            niebla = c.contenido.workbook(ContentRepository.NIEBLA)
        }
    }

    fun dia(fecha: LocalDate): Flow<Set<String>> = c.checklist.dia(fecha)
    fun alternar(fecha: LocalDate, id: String) = viewModelScope.launch { c.checklist.alternar(fecha, id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitScreen(
    onBack: () -> Unit,
    onOpenSection: (workbookId: String, index: Int) -> Unit,
    onOpenWorkbook: (String) -> Unit,
) {
    val vm = rutaViewModel { KitViewModel(it) }
    var fecha by rememberSaveable { mutableStateOf(hoy().toString()) }
    val dia = LocalDate.parse(fecha)
    val marcados by remember(fecha) { vm.dia(dia) }.collectAsState(initial = emptySet())
    val semana by vm.semana.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.kit_titulo)) },
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
            Text(stringResource(R.string.kit_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // ----- Niebla: acceso rápido
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.niebla_titulo), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                    Text(stringResource(R.string.niebla_texto), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Niebla.protocolos) { p ->
                    val idx = vm.niebla?.sectionIndexStartingWith("Protocolo ${p.numero}") ?: -1
                    Card(
                        Modifier.width(170.dp).height(118.dp).clickable(enabled = idx >= 0) { onOpenSection(ContentRepository.NIEBLA, idx) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.protocolo_n, p.numero), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(Textos.protocolo(p.numero)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("“" + stringResource(Textos.protocoloCuando(p.numero)) + "”", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // ----- Herramienta 2: checklist diario
        item { SectionTitle(stringResource(R.string.checklist_diario)) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { fecha = dia.minusDays(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.dia_anterior))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (dia == hoy()) stringResource(R.string.hoy) else formatoDia(dia), style = MaterialTheme.typography.titleMedium)
                    Text("${marcados.size} / ${ChecklistDiario.TOTAL} · " + stringResource(Textos.lecturaChecklist(marcados.size)),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { fecha = dia.plusDays(1).toString() }, enabled = dia < hoy()) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.dia_siguiente))
                }
            }
        }
        Eje.entries.forEach { eje ->
            item(key = "habitos-${eje.name}") {
                RutaCard {
                    EjeChip(eje)
                    ChecklistDiario.habitos.filter { it.eje == eje }.forEach { h ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { vm.alternar(dia, h.id) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = h.id in marcados, onCheckedChange = { vm.alternar(dia, h.id) })
                            Text(stringResource(Textos.habito(h.id)), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // ----- Herramienta 6: tracker de la semana
        item {
            RutaCard {
                Text(stringResource(R.string.ultimos_7_dias), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                    (6 downTo 0).forEach { d ->
                        val f = hoy().minusDays(d.toLong())
                        val n = semana[f]?.size ?: 0
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text("$n", style = MaterialTheme.typography.labelSmall)
                            Box(
                                Modifier.fillMaxWidth().fillMaxHeight((n / 18f).coerceIn(0.04f, 0.75f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (n >= 10) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
                            )
                            Text(f.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // ----- Herramienta 4: matriz de decisiones
        item { SectionTitle(stringResource(R.string.matriz_titulo)) }
        item { MatrizDecisionesCard() }

        // ----- Herramienta 3: tarjetas
        item { SectionTitle(stringResource(R.string.tarjetas_titulo)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Eje.entries) { eje ->
                    Card(
                        Modifier.width(220.dp).height(150.dp),
                        colors = CardDefaults.cardColors(containerColor = eje.color.asColor().copy(alpha = 0.12f)),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(eje.texto().uppercase(), style = MaterialTheme.typography.labelLarge, color = eje.color.asColor())
                            Spacer(Modifier.height(6.dp))
                            Text("“" + stringResource(Textos.tarjeta(eje)) + "”", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        // ----- Resto de herramientas (formularios del workbook)
        item { SectionTitle(stringResource(R.string.mas_herramientas)) }
        val accesos = listOf(
            Triple(R.string.kit_revision_mensual, R.string.kit_revision_mensual_sub, "Herramienta 1"),
            Triple(R.string.kit_plan_semanal, R.string.kit_plan_semanal_sub, "Herramienta 5"),
            Triple(R.string.kit_reorientacion, R.string.kit_reorientacion_sub, "Herramienta 7"),
        )
        items(accesos) { (titulo, sub, prefijo) ->
            val idx = vm.kit?.sectionIndexStartingWith(prefijo) ?: -1
            RutaCard(onClick = { if (idx >= 0) onOpenSection(ContentRepository.KIT, idx) else onOpenWorkbook(ContentRepository.KIT) }) {
                Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            RutaCard(onClick = { onOpenWorkbook(ContentRepository.CIERRE_MENSUAL) }) {
                Text(stringResource(R.string.kit_cierre_mensual), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.kit_cierre_mensual_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    }
}

@Composable
private fun MatrizDecisionesCard() {
    val defA = stringResource(R.string.opcion_a)
    val defB = stringResource(R.string.opcion_b)
    var decision by rememberSaveable { mutableStateOf("") }
    var nombreA by rememberSaveable { mutableStateOf(defA) }
    var nombreB by rememberSaveable { mutableStateOf(defB) }
    val a = remember { mutableStateListOf<Respuesta?>(null, null, null, null, null) }
    val b = remember { mutableStateListOf<Respuesta?>(null, null, null, null, null) }

    RutaCard {
        OutlinedTextField(decision, { decision = it }, label = { Text(stringResource(R.string.decision_a_tomar)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(nombreA, { nombreA = it }, label = { Text(defA) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(nombreB, { nombreB = it }, label = { Text(defB) }, modifier = Modifier.weight(1f), singleLine = true)
        }
        MatrizDecisiones.preguntas.indices.forEach { i ->
            Spacer(Modifier.height(12.dp))
            Text(stringResource(Textos.pregunta(i)), style = MaterialTheme.typography.titleSmall)
            val etiqueta: @Composable (Respuesta) -> String = { r ->
                when (r) {
                    Respuesta.SI -> stringResource(if (i == 4) R.string.energia else R.string.si)
                    Respuesta.NO -> stringResource(if (i == 4) R.string.drena else R.string.no)
                    Respuesta.NEUTRO -> stringResource(R.string.neutro)
                }
            }
            ChipSelector(nombreA, Respuesta.entries, a[i], etiqueta, { a[i] = if (a[i] == it) null else it })
            ChipSelector(nombreB, Respuesta.entries, b[i], etiqueta, { b[i] = if (b[i] == it) null else it })
        }
        Spacer(Modifier.height(12.dp))
        val pa = MatrizDecisiones.puntaje(a)
        val pb = MatrizDecisiones.puntaje(b)
        Text("$nombreA: $pa/5   ·   $nombreB: $pb/5", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        if (a.any { it != null } || b.any { it != null }) {
            val texto = when {
                pa > pb -> stringResource(R.string.matriz_gana, nombreA, pa, pb)
                pb > pa -> stringResource(R.string.matriz_gana, nombreB, pb, pa)
                else -> stringResource(R.string.matriz_empate, pa, pb)
            }
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
        Text(stringResource(R.string.matriz_nota), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
