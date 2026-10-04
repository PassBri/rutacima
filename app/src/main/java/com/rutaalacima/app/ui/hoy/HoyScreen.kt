package com.rutaalacima.app.ui.hoy

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.Cascada
import com.rutaalacima.app.data.content.ContentRepository
import com.rutaalacima.app.data.content.WorkbookSummary
import com.rutaalacima.app.data.local.AgendaDiaEntity
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.PerfilEntity
import com.rutaalacima.app.data.local.RespuestaEntity
import com.rutaalacima.app.data.local.avance
import com.rutaalacima.app.data.local.diasMarcados
import com.rutaalacima.app.domain.model.ChecklistDiario
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.Fase
import com.rutaalacima.app.ui.Rutas
import com.rutaalacima.app.ui.components.MontanaArte
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.i18n.Textos
import com.rutaalacima.app.ui.i18n.texto
import com.rutaalacima.app.ui.metas.NivelMeta
import com.rutaalacima.app.ui.theme.asColor
import com.rutaalacima.app.ui.workbook.QuoteView
import com.rutaalacima.app.util.formatoDia
import com.rutaalacima.app.util.hoy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HoyViewModel(private val c: AppContainer) : ViewModel() {
    private fun <T> estado(f: kotlinx.coroutines.flow.Flow<T>, inicial: T): StateFlow<T> =
        f.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), inicial)

    val perfil: StateFlow<PerfilEntity> = estado(c.perfil.perfil, PerfilEntity())
    val cascada: StateFlow<Cascada?> = estado(c.cascada.cascada, null)
    val checksHoy: StateFlow<Set<String>> = estado(c.checklist.dia(hoy()), emptySet())
    val metasMes: StateFlow<List<MetaMensualEntity>> = estado(c.planAnual.metasMes(hoy().year, hoy().monthValue), emptyList())
    val ultimaRespuesta: StateFlow<RespuestaEntity?> = estado(c.respuestas.ultimaRespuesta, null)
    val indice: StateFlow<List<WorkbookSummary>> = estado(flow { emit(c.contenido.index()) }, emptyList())

    var agenda by mutableStateOf<AgendaDiaEntity?>(null)
        private set
    private var guardado: Job? = null

    init {
        viewModelScope.launch { agenda = c.agenda.dia(hoy()) }
    }

    fun editarAgenda(t: (AgendaDiaEntity) -> AgendaDiaEntity) {
        val nuevo = t(agenda ?: return)
        agenda = nuevo
        guardado?.cancel()
        guardado = c.appScope.launch { delay(500); c.agenda.guardar(nuevo) }
    }

    fun alternarHoy(m: MetaMensualEntity) = viewModelScope.launch { c.planAnual.alternarDia(m, hoy().dayOfMonth) }
}

/** Pantalla "Hoy": todo lo del día en un solo lugar, tan simple como un chat. */
@Composable
fun HoyScreen(
    contentPadding: PaddingValues,
    onAbrirWorkbook: (String) -> Unit,
    onAbrirSeccion: (String, Int) -> Unit,
    onIrA: (String) -> Unit,
) {
    val vm = rutaViewModel { HoyViewModel(it) }
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val cascada by vm.cascada.collectAsStateWithLifecycle()
    val checks by vm.checksHoy.collectAsStateWithLifecycle()
    val metasMes by vm.metasMes.collectAsStateWithLifecycle()
    val ultima by vm.ultimaRespuesta.collectAsStateWithLifecycle()
    val indice by vm.indice.collectAsStateWithLifecycle()
    val diaHoy = hoy().dayOfMonth

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                if (perfil.nombre.isBlank()) stringResource(R.string.hola_senderista)
                else stringResource(R.string.hola_nombre, perfil.nombre.substringBefore(' ')),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(formatoDia(hoy()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Cumbre + avance global
        item {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().clickable { onIrA(Rutas.METAS) }) {
                MontanaArte(semilla = "cumbre-${perfil.nombre}", paleta = 1, modifier = Modifier.fillMaxWidth().height(190.dp)) {
                    Box(
                        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent, Color(0xCC000000)))),
                    )
                    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.mi_cumbre).uppercase(), style = MaterialTheme.typography.labelLarge, color = Color(0xFFF5C451))
                            Text(
                                perfil.cumbreFrase.ifBlank { stringResource(R.string.cumbre_vacia) },
                                style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Fase.fromName(perfil.faseActual)?.let {
                                Text(stringResource(R.string.fase_n, it.numero, it.texto()), color = Color.White,
                                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            } ?: Spacer(Modifier.weight(1f))
                            val g = cascada?.avanceGlobal ?: 0f
                            Text("${(g * 100).toInt()}%", color = Color(0xFFF5C451), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // Cascada: 5 años · año · mes
        item {
            val c = cascada
            val cinco = c?.propositos?.map { it.avance }?.average()?.toFloat()?.takeIf { !it.isNaN() } ?: 0f
            val anuales = (c?.propositos?.flatMap { it.anios }.orEmpty() + c?.aniosSueltos.orEmpty()).filter { it.meta.anio == hoy().year }
            val anio = anuales.map { it.avance }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
            val mes = metasMes.map { it.avance() }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
            RutaCard(onClick = { onIrA(Rutas.METAS) }) {
                Text(stringResource(R.string.tu_cascada), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Anillo(cinco, stringResource(R.string.nivel_5_anios))
                    Anillo(anio, stringResource(R.string.nivel_anio))
                    Anillo(mes, stringResource(R.string.nivel_mes))
                    Anillo(checks.size / ChecklistDiario.TOTAL.toFloat(), stringResource(R.string.nivel_hoy))
                }
            }
        }

        // Intención y prioridad de hoy
        item {
            val a = vm.agenda
            if (a != null) {
                RutaCard {
                    SectionTitle(stringResource(R.string.hoy_mi_dia))
                    OutlinedTextField(a.intencion, { v -> vm.editarAgenda { it.copy(intencion = v) } },
                        label = { Text(stringResource(R.string.intencion_dia)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(a.prioridad, { v -> vm.editarAgenda { it.copy(prioridad = v) } },
                        label = { Text(stringResource(R.string.prioridad_1)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    TextButton(onClick = { onIrA(Rutas.METAS) }) { Text(stringResource(R.string.abrir_planificador_dia)) }
                }
            }
        }

        // Metas del mes: check de hoy
        item { SectionTitle(stringResource(R.string.metas_mes_hoy)) }
        if (metasMes.isEmpty()) {
            item {
                RutaCard(onClick = { onIrA(Rutas.nuevaMeta(NivelMeta.MES)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.crear_primera_meta_mes), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        items(metasMes.filter { !it.cumplida }, key = { it.id }) { m ->
            val hecho = diaHoy in m.diasMarcados()
            val color = Eje.fromCodigo(m.eje)?.color?.asColor() ?: MaterialTheme.colorScheme.secondary
            RutaCard(onClick = { vm.alternarHoy(m) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (hecho) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                        tint = if (hecho) color else MaterialTheme.colorScheme.outline, modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.texto, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        ProgressLine(m.avance(), color = color)
                        Text(stringResource(R.string.dias_de, m.diasMarcados().size, m.objetivoDias),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Hábitos por eje
        item {
            RutaCard(onClick = { onIrA(Rutas.KIT) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.checklist_hoy), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.checklist_resumen, checks.size, ChecklistDiario.TOTAL),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { checks.size / ChecklistDiario.TOTAL.toFloat() }, modifier = Modifier.size(46.dp),
                            color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeCap = StrokeCap.Round)
                        Text("${checks.size}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Rescate
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.rescate_titulo), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                    Text(stringResource(R.string.rescate_texto), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.height(10.dp))
                    // Botones de rescate: ocupan el ancho y bajan de línea si el idioma es largo.
                    val rescates = listOf(
                        Triple(R.string.me_perdi, Icons.Filled.Cloud) { onAbrirWorkbook(ContentRepository.NIEBLA) },
                        Triple(R.string.dia_malo, Icons.Filled.Shield) { onAbrirWorkbook("bono_anti_abandono") },
                        Triple(R.string.kit, Icons.Filled.LocalHospital) { onIrA(Rutas.KIT) },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rescates.forEach { (texto, icono, accion) ->
                            Surface(
                                onClick = accion, shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icono, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(texto), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Coach y comunidad
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { onIrA(Rutas.COACH) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.hablar_coach), maxLines = 1)
                }
                FilledTonalButton(onClick = { onIrA(Rutas.publicar("LOGRO")) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.AddAPhoto, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.compartir_avance), maxLines = 1)
                }
            }
        }

        // Continuar aprendiendo
        item {
            val w = indice.firstOrNull { it.id == ultima?.workbookId }
            if (w != null) {
                RutaCard(onClick = { onAbrirWorkbook(w.id) }) {
                    Text(stringResource(R.string.continua_donde_quedaste), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(w.title, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                RutaCard(onClick = { onAbrirWorkbook(ContentRepository.DESCUBRE) }) {
                    Text(stringResource(R.string.empieza_ascenso), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.empieza_ascenso_texto), style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Recordatorio del día
        item {
            val eje = Eje.entries[hoy().dayOfYear % Eje.entries.size]
            Column {
                Text(stringResource(R.string.recordatorio_dia, eje.texto()), style = MaterialTheme.typography.labelLarge, color = eje.color.asColor())
                QuoteView(stringResource(Textos.tarjeta(eje)))
            }
        }
    }
}

@Composable
private fun Anillo(valor: Float, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { valor.coerceIn(0f, 1f) }, modifier = Modifier.size(58.dp), strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeCap = StrokeCap.Round,
            )
            Text("${(valor * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}
