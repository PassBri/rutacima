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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Check
import com.rutaalacima.app.data.local.avance
import androidx.compose.foundation.layout.width
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
    var eligiendo by remember { mutableStateOf(false) }
    if (eligiendo) HojaElegirMes(ym, anuales, metas, casillasV, mandala, vm, onQuitar = { anual, n -> quitar = anual to n }) { eligiendo = false }
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

        // El mes en dos tiempos (igual que la web): ELEGIR en una hoja aparte y TRABAJAR con tarjetas de lo elegido.
        // Los días del mes ya están en el calendario de arriba; aquí no se repiten.
        val delMes = metas.filter { mm -> mm.metaAnualId != null && anuales.any { it.id == mm.metaAnualId } }
        item(key = "foco-cab") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.foco_mis_metas, nombreMes(ym.monthValue).lowercase()), Modifier.weight(1f))
                if (anuales.isNotEmpty()) {
                    if (delMes.isEmpty()) androidx.compose.material3.FilledTonalButton(onClick = { eligiendo = true }) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.foco_elegir)) }
                    else androidx.compose.material3.OutlinedButton(onClick = { eligiendo = true }) {
                        Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.foco_cambiar)) }
                }
            }
        }
        item(key = "foco-resumen") {
            val nSubs = delMes.sumOf { mm -> pasosMes(mm, anuales, casillasV, mandala).second.size }
            Text(when {
                anuales.isEmpty() -> stringResource(R.string.foco_sin_metas, ym.year)
                delMes.isEmpty() -> stringResource(R.string.foco_vacio, nombreMes(ym.monthValue).lowercase(), ym.year)
                else -> stringResource(R.string.foco_resumen, delMes.size, nSubs, ym.year)
            }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(delMes, key = { "mf" + it.id }) { mm -> TarjetaFocoMes(mm, anuales, casillasV, mandala) }
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


/** Las sub-metas de una meta del mes: (casilla, todas las escritas, las que se ven este mes = elegidas o todas). */
private fun pasosMes(mm: MetaMensualEntity, anuales: List<com.rutaalacima.app.data.local.MetaAnualEntity>,
    casillas: List<com.rutaalacima.app.data.local.VisionCasillaEntity>, mandala: Map<String, String>): Triple<com.rutaalacima.app.data.local.VisionCasillaEntity?, List<Pair<Int, String>>, List<Int>> {
    val anual = anuales.firstOrNull { it.id == mm.metaAnualId }
    val cas = com.rutaalacima.app.data.vision.SincroBrujula.casillaDe(anual, anuales, casillas) ?: return Triple(null, emptyList(), emptyList())
    val todos = (0 until com.rutaalacima.app.domain.model.Mandala.PASOS).mapNotNull { p ->
        mandala[com.rutaalacima.app.domain.model.Mandala.clave(cas.id, p)].orEmpty().trim().takeIf { it.isNotEmpty() }?.let { p to it } }
    val elegidos = com.rutaalacima.app.data.vision.LibroMetas.pasosDelMes(mm.id).filter { e -> todos.any { it.first == e } }
    val vistos = todos.filter { elegidos.isEmpty() || it.first in elegidos }
    return Triple(cas, vistos, elegidos)
}

/** Una meta elegida para el mes: su nombre, su avance, sus sub-metas del mes (con jornadas) y los días con avance. */
@Composable
private fun TarjetaFocoMes(mm: MetaMensualEntity, anuales: List<com.rutaalacima.app.data.local.MetaAnualEntity>,
    casillas: List<com.rutaalacima.app.data.local.VisionCasillaEntity>, mandala: Map<String, String>) {
    val anual = anuales.firstOrNull { it.id == mm.metaAnualId }
    val (cas, vistos, _) = pasosMes(mm, anuales, casillas, mandala)
    val cs = MaterialTheme.colorScheme
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(com.rutaalacima.app.ui.vision.colorEje(anual?.eje ?: "CON")))
            Spacer(Modifier.width(10.dp))
            Text(anual?.titulo ?: mm.texto, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${(mm.avance() * 100).toInt()} %", style = MaterialTheme.typography.labelLarge, color = cs.primary, fontWeight = FontWeight.Bold)
        }
        if (cas == null || vistos.isEmpty()) Text(stringResource(R.string.foco_sin_pasos, mm.anio), style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        else vistos.forEachIndexed { i, (p, t) ->
            val x = com.rutaalacima.app.domain.model.MetodoCima.leer(cas.id, p, mandala)
            if (i > 0) androidx.compose.material3.HorizontalDivider(color = cs.outlineVariant)
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(18.dp).clip(CircleShape).background(if (x.cumplido) cs.primary else Color.Transparent)
                    .border(1.5.dp, if (x.cumplido) cs.primary else cs.outline, CircleShape), contentAlignment = Alignment.Center) {
                    if (x.cumplido) Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = cs.onPrimary)
                }
                Spacer(Modifier.width(10.dp))
                Text("${p + 1}. $t", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f), color = if (x.cumplido) cs.onSurfaceVariant else cs.onSurface,
                    textDecoration = if (x.cumplido) TextDecoration.LineThrough else null)
                Text(if (x.cumplido) stringResource(R.string.foco_ganada) else "${x.jornadas.size}/${x.requeridas ?: com.rutaalacima.app.domain.model.MetodoCima.DIFICULTAD_INICIAL}",
                    style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
        val n = mm.diasMarcados().size
        androidx.compose.material3.HorizontalDivider(color = cs.outlineVariant, modifier = Modifier.padding(top = 8.dp))
        Text(if (n > 0) stringResource(R.string.foco_dias, n) else stringResource(R.string.foco_sin_dias),
            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
    }
}

/** Elegir qué trabajar este mes: las metas del año por campamento y, de cada una, solo algunas sub-metas si se quiere. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun HojaElegirMes(ym: YearMonth, anuales: List<com.rutaalacima.app.data.local.MetaAnualEntity>, metas: List<MetaMensualEntity>,
    casillas: List<com.rutaalacima.app.data.local.VisionCasillaEntity>, mandala: Map<String, String>, vm: MesViewModel,
    onQuitar: (com.rutaalacima.app.data.local.MetaAnualEntity, Int) -> Unit, onCerrar: () -> Unit) {
    val estado = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cods = com.rutaalacima.app.domain.model.Mandala.CAMPAMENTOS_FIJOS
    val grupos = anuales.sortedBy { it.id }.groupBy { it.eje ?: "CON" }.toList().sortedBy { (c, _) -> cods.indexOf(c).let { if (it < 0) 99 else it } }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onCerrar, sheetState = estado) {
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            item {
                Text(stringResource(R.string.foco_elegir_titulo, nombreMes(ym.monthValue).lowercase()), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.foco_elegir_ayuda, ym.year), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            }
            grupos.forEach { (cod, lista) ->
                item(key = "g$cod") {
                    Row(Modifier.padding(top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(com.rutaalacima.app.ui.vision.colorEje(cod)))
                        Spacer(Modifier.width(8.dp))
                        Text(com.rutaalacima.app.ui.vision.nombreCodigo(cod), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(lista, key = { "e" + it.id }) { anual ->
                    val mm = metas.firstOrNull { it.metaAnualId == anual.id }
                    Column {
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                            if (mm != null && mm.diasMarcados().isNotEmpty()) onQuitar(anual, mm.diasMarcados().size) else vm.elegirAnual(anual, mm == null)
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = mm != null, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(anual.titulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
                                fontWeight = if (mm != null) FontWeight.SemiBold else FontWeight.Normal)
                        }
                        if (mm != null) {
                            val (_, todos0, elegidos) = pasosMes(mm, anuales, casillas, mandala).let { (c, _, e) ->
                                Triple(c, c?.let { c0 -> (0 until com.rutaalacima.app.domain.model.Mandala.PASOS).mapNotNull { p ->
                                    mandala[com.rutaalacima.app.domain.model.Mandala.clave(c0.id, p)].orEmpty().trim().takeIf { it.isNotEmpty() }?.let { p to it } } }.orEmpty(), e) }
                            if (todos0.isNotEmpty()) {
                                Text(if (elegidos.isEmpty()) stringResource(R.string.mes_completa) else stringResource(R.string.mes_pasos_n, elegidos.size, todos0.size),
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 40.dp))
                                androidx.compose.foundation.layout.FlowRow(Modifier.padding(start = 40.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    todos0.forEach { (p, t) ->
                                        androidx.compose.material3.FilterChip(selected = p in elegidos, onClick = { vm.alternarPasoMes(mm, p) },
                                            label = { Text("${p + 1}. $t", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp)) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    androidx.compose.material3.Button(onClick = onCerrar) { Text(stringResource(R.string.foco_listo)) }
                }
            }
        }
    }
}
