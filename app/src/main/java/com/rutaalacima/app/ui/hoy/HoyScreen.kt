package com.rutaalacima.app.ui.hoy

import com.rutaalacima.app.ui.ruta.RecordatorioDiaCard
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
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.remember
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
    fun alternarHabito(id: String) = viewModelScope.launch { c.checklist.alternar(hoy(), id) }
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
        // Bienvenida de regreso, el paso del comienzo y la revisión que toca (cada una solo si aplica)
        item { RegresoCard(onIrA = onIrA, onSeccion = onAbrirSeccion) }
        item { PrimerPasoCard(onIrA = onIrA) }
        item { RevisionCard(onIrA = onIrA) }
        // Diciembre y principios de enero: el resumen del año ya está listo
        item { ResumenCard(onIrA = onIrA) }
        // Recordatorio del día del camino hacia los 120 años
        item { RecordatorioDiaCard(perfil, onAjustes = { onIrA(Rutas.AJUSTES) }) }
        item {
            Text(
                if (perfil.nombre.isBlank()) stringResource(R.string.hola_senderista)
                else stringResource(R.string.hola_nombre, perfil.nombre.substringBefore(' ')),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(formatoDia(hoy()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Cumbre: el porqué del día, en una frase (toca para ver tu ruta)
        item {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().clickable { onIrA(Rutas.RUTA) }) {
                MontanaArte(semilla = "cumbre-${perfil.nombre}", paleta = 1, modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66000000), Color(0xB3000000)))))
                    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            perfil.cumbreFrase.ifBlank { stringResource(R.string.cumbre_vacia) },
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif),
                            color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                Fase.fromName(perfil.faseActual)?.let { stringResource(R.string.fase_n, it.numero, it.texto()) } ?: stringResource(R.string.mi_cumbre),
                                color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f),
                            )
                            Text("${((cascada?.avanceGlobal ?: 0f) * 100).toInt()}%", color = Color.White,
                                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // La acción principal del día: marcar los hábitos aquí mismo, un eje a la vez
        item { HabitosHoy(checks, onAlternar = { vm.alternarHabito(it) }, onVerKit = { onIrA(Rutas.KIT) }) }

        // Metas del mes: check de hoy
        // Racha y mapa de constancia (toca para ver el año)
        // La frase del día pregunta algo distinto cada día para responder en la comunidad
        item { InvitacionFraseCard(onIrA = onIrA) }
        item { ConstanciaCard(onIrA = onIrA) }
        item { Text(stringResource(R.string.metas_mes_hoy), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp)) }
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
            val color = MaterialTheme.colorScheme.secondary
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

        // Intención y prioridad de hoy
        item {
            val a = vm.agenda
            if (a != null) {
                RutaCard {
                    Text(stringResource(R.string.hoy_mi_dia), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(a.intencion, { v -> vm.editarAgenda { it.copy(intencion = v) } },
                        label = { Text(stringResource(R.string.intencion_dia)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(a.prioridad, { v -> vm.editarAgenda { it.copy(prioridad = v) } },
                        label = { Text(stringResource(R.string.prioridad_1)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    TextButton(onClick = { onIrA(Rutas.planificador(1, hoy())) }) { Text(stringResource(R.string.abrir_planificador_dia)) }
                }
            }
        }

        // Cascada: largo plazo · año · mes (líneas finas; se abre el planificador)
        item {
            val c = cascada
            val cinco = c?.propositos?.map { it.avance }?.average()?.toFloat()?.takeIf { !it.isNaN() } ?: 0f
            val anuales = (c?.propositos?.flatMap { it.anios }.orEmpty() + c?.aniosSueltos.orEmpty()).filter { it.meta.anio == hoy().year }
            val anio = anuales.map { it.avance }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
            val mes = metasMes.map { it.avance() }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
            RutaCard(onClick = { onIrA(Rutas.planificador(0)) }) {
                Text(stringResource(R.string.tu_cascada), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                NivelCascada(stringResource(R.string.nivel_5_anios), cinco)
                NivelCascada(stringResource(R.string.nivel_anio), anio)
                NivelCascada(stringResource(R.string.nivel_mes), mes)
            }
        }

        // Rescate
        item {
            RutaCard {
                Column {
                    Text(stringResource(R.string.rescate_titulo), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.rescate_texto), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
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
                Text(stringResource(R.string.recordatorio_dia, eje.texto()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                QuoteView(stringResource(Textos.tarjeta(eje)))
            }
        }
    }
}

@Composable
private fun NivelCascada(etiqueta: String, valor: Float) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        ProgressLine(valor, modifier = Modifier.weight(1f))
        Text("${(valor.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
    }
}

/**
 * Los 18 hábitos del día en Hoy, sin cambiar de pantalla: un eje abierto a la vez (el primero que falta).
 * Al completar los 3 de un eje, se abre solo el siguiente. Un toque marca; otro toque desmarca.
 */
@Composable
private fun HabitosHoy(marcados: Set<String>, onAlternar: (String) -> Unit, onVerKit: () -> Unit) {
    val pendiente = Eje.entries.firstOrNull { e -> ChecklistDiario.habitos.any { it.eje == e && it.id !in marcados } }
    var abierto by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    // Si el eje que la persona abrió se acaba de completar, se vuelve a seguir al siguiente que falta
    androidx.compose.runtime.LaunchedEffect(marcados) {
        val actual = Eje.entries.firstOrNull { it.name == abierto } ?: return@LaunchedEffect
        if (ChecklistDiario.habitos.filter { it.eje == actual }.all { it.id in marcados }) { delay(350); abierto = null }
    }
    val visible = abierto ?: pendiente?.name
    val haptico = androidx.compose.ui.platform.LocalHapticFeedback.current
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.checklist_hoy), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${marcados.size} / ${ChecklistDiario.TOTAL}", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        ProgressLine(marcados.size / ChecklistDiario.TOTAL.toFloat())
        Text(stringResource(Textos.lecturaChecklist(marcados.size)), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
        Eje.entries.forEach { eje ->
            val delEje = ChecklistDiario.habitos.filter { it.eje == eje }
            val hechos = delEje.count { it.id in marcados }
            val esteAbierto = visible == eje.name
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { abierto = if (esteAbierto) "" else eje.name }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(eje.texto(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (hechos == delEje.size) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                else Text("$hechos / ${delEje.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (esteAbierto) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp),
                )
            }
            androidx.compose.animation.AnimatedVisibility(esteAbierto) {
                Column {
                    delEje.forEach { h ->
                        val si = h.id in marcados
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                                .toggleable(si, role = androidx.compose.ui.semantics.Role.Checkbox) {
                                    if (!si) haptico.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    onAlternar(h.id)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = si, onCheckedChange = null, modifier = Modifier.padding(horizontal = 8.dp))
                            Text(stringResource(Textos.habito(h.id)), style = MaterialTheme.typography.bodyMedium,
                                color = if (si) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        TextButton(onClick = onVerKit) { Text(stringResource(R.string.kit)) }
    }
}
