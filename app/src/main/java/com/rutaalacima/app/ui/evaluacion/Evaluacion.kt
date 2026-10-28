package com.rutaalacima.app.ui.evaluacion

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
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
import com.rutaalacima.app.data.local.BalanceAnualEntity
import com.rutaalacima.app.data.local.MesEntity
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.PropositoEntity
import com.rutaalacima.app.data.local.VisionCasillaEntity
import com.rutaalacima.app.data.local.avance
import com.rutaalacima.app.data.local.diasMarcados
import com.rutaalacima.app.data.vision.LibroMetas
import com.rutaalacima.app.data.vision.SincroBrujula
import com.rutaalacima.app.domain.model.Bienestar
import com.rutaalacima.app.domain.model.ChecklistDiario
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.domain.model.MetodoCima
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.i18n.texto
import com.rutaalacima.app.ui.planner.nombreMes
import com.rutaalacima.app.ui.planner.horarioMapa
import com.rutaalacima.app.ui.theme.fondoPapel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/*
 * Evaluación (igual que la web, 44-evaluacion.js): mirar hacia atrás y medir.
 * Mes: puntaje (metas + hábitos + bienestar), cumplimiento de metas, hábitos por eje, bienestar, balance y finanzas.
 * Año: puntaje mes a mes, Brújula del año, bienestar, balance anual y finanzas.
 * Propósitos: avance frente al tiempo recorrido, años y una reflexión.
 * Informes PDF (Imprimir › Guardar como PDF): de un mes o de un año.
 */

class EvaluacionViewModel(private val c: AppContainer) : ViewModel() {
    private fun <T> e(f: kotlinx.coroutines.flow.Flow<T>, i: T) = f.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), i)
    val anuales: StateFlow<List<MetaAnualEntity>> = e(c.planificador.todasLasMetas, emptyList())
    val mensuales: StateFlow<List<MetaMensualEntity>> = e(c.planAnual.todasMetasMes, emptyList())
    val propositos: StateFlow<List<PropositoEntity>> = e(c.planificador.propositos, emptyList())
    val casillas: StateFlow<List<VisionCasillaEntity>> = e(c.db.visionDao().observe(), emptyList())
    val mandala: StateFlow<Map<String, String>> = e(c.respuestas.observar(Mandala.WORKBOOK), emptyMap())
    val metasResp: StateFlow<Map<String, String>> = e(c.respuestas.observar("metas"), emptyMap())
    val checks: StateFlow<Map<LocalDate, Set<String>>> = e(c.checklist.desde(LocalDate.now().minusYears(2).withDayOfYear(1)), emptyMap())
    val perfil: StateFlow<com.rutaalacima.app.data.local.PerfilEntity> = e(c.perfil.perfil, com.rutaalacima.app.data.local.PerfilEntity())
    val vida: StateFlow<Map<String, String>> = e(c.respuestas.observar(com.rutaalacima.app.ui.ruta.WB_VIDA), emptyMap())
    fun anhelo(clave: String, v: String) = viewModelScope.launch { c.respuestas.guardar(com.rutaalacima.app.ui.ruta.WB_VIDA, clave, v) }
    val diaResp: StateFlow<Map<String, String>> = e(c.respuestas.observar(com.rutaalacima.app.ui.planner.WB_DIA), emptyMap())
    suspend fun agenda(f: LocalDate) = c.agenda.dia(f)
    fun guardarAgenda(d: com.rutaalacima.app.data.local.AgendaDiaEntity) { guardando?.cancel(); guardando = viewModelScope.launch { delay(500); c.agenda.guardar(d) } }
    fun balance(anio: Int) = c.planificador.balance(anio)
    suspend fun mes(a: Int, m: Int): MesEntity = c.planAnual.mes(a, m)
    private var guardando: Job? = null
    fun guardarMes(m: MesEntity) { guardando?.cancel(); guardando = viewModelScope.launch { delay(500); c.planAnual.guardarMes(m) } }
    fun guardarBalance(b: BalanceAnualEntity) { guardando?.cancel(); guardando = viewModelScope.launch { delay(500); c.planificador.guardarBalance(b) } }
    fun reflexion(clave: String, v: String) = viewModelScope.launch { c.respuestas.guardar("metas", clave, v) }
}

// ---------------------------------------------------------------- Cálculos (los mismos de la web)

private fun prom(l: List<Double?>): Double? = l.filterNotNull().takeIf { it.isNotEmpty() }?.average()

/** Avance de una meta del año: el promedio de sus metas del mes o, si no tiene, su avance manual. */
private fun nodoAnio(a: MetaAnualEntity, mensuales: List<MetaMensualEntity>): Double =
    mensuales.filter { it.metaAnualId == a.id }.map { it.avance().toDouble() }.takeIf { it.isNotEmpty() }?.average() ?: (a.avance / 100.0)

private fun fechasHasta(ym: YearMonth): List<LocalDate> { val hoy = LocalDate.now(); return (1..ym.lengthOfMonth()).map { ym.atDay(it) }.filter { !it.isAfter(hoy) } }

data class HabEval(val p: Double?, val porEje: Map<Eje, Pair<Int, Int>>)
private fun habitosEval(fechas: List<LocalDate>, checks: Map<LocalDate, Set<String>>): HabEval {
    val hs = ChecklistDiario.habitos
    val porEje = Eje.entries.associateWith { e -> val ids = hs.filter { it.eje == e }.map { it.id }
        fechas.sumOf { f -> ids.count { it in checks[f].orEmpty() } } to ids.size * fechas.size }
    val si = porEje.values.sumOf { it.first }; val de = porEje.values.sumOf { it.second }
    return HabEval(if (de > 0) si.toDouble() / de else null, porEje)
}

/** Una sub-meta evaluada: ganada (true), en camino con jornadas (null) o pendiente (false). */
data class PasoEval(val n: Int, val t: String, val ok: Boolean?, val jornadas: Int, val req: Int)
data class MetaEval(val mm: MetaMensualEntity, val titulo: String, val eje: String?, val p: Double, val dias: Int, val ganadas: Int, val total: Int, val pasos: List<PasoEval> = emptyList())
private fun pasoEval(casillaId: Long, p: Int, t: String, mandala: Map<String, String>): PasoEval {
    val x = MetodoCima.leer(casillaId, p, mandala)
    return PasoEval(p, t, if (x.cumplido) true else if (x.jornadas.isNotEmpty()) null else false, x.jornadas.size, x.requeridas ?: MetodoCima.DIFICULTAD_INICIAL)
}
private fun metasEval(ym: YearMonth, anuales: List<MetaAnualEntity>, mensuales: List<MetaMensualEntity>, casillas: List<VisionCasillaEntity>, mandala: Map<String, String>): List<MetaEval> =
    mensuales.filter { it.anio == ym.year && it.mes == ym.monthValue && it.metaAnualId != null }.mapNotNull { mm ->
        val a = anuales.firstOrNull { it.id == mm.metaAnualId } ?: return@mapNotNull null
        val cas = SincroBrujula.casillaDe(a, anuales, casillas)
        val todos = cas?.let { c0 -> (0 until Mandala.PASOS).filter { mandala[Mandala.clave(c0.id, it)].orEmpty().isNotBlank() } }.orEmpty()
        val el = LibroMetas.pasosDelMes(mm.id).filter { it in todos }; val vis = el.ifEmpty { todos }
        val ganadas = cas?.let { c0 -> vis.count { mandala[Mandala.claveHecho(c0.id, it)].orEmpty().startsWith(ym.toString()) } } ?: 0
        MetaEval(mm, a.titulo, a.eje, mm.avance().toDouble(), mm.diasMarcados().size, ganadas, vis.size,
            cas?.let { c0 -> vis.map { p -> pasoEval(c0.id, p, mandala[Mandala.clave(c0.id, p)].orEmpty().trim(), mandala) } }.orEmpty())
    }

data class CampEval(val cod: String, val meta: MetaAnualEntity, val p: Double, val ganadas: Int, val total: Int, val pasos: List<PasoEval> = emptyList())
private fun campsEval(anio: Int, anuales: List<MetaAnualEntity>, mensuales: List<MetaMensualEntity>, casillas: List<VisionCasillaEntity>, mandala: Map<String, String>): List<CampEval> =
    Mandala.CAMPAMENTOS_FIJOS.mapNotNull { cod ->
        val meta = anuales.filter { it.anio == anio && (it.eje ?: "CON") == cod }.minByOrNull { it.id } ?: return@mapNotNull null
        val cas = SincroBrujula.casillaDe(meta, anuales, casillas)
        val pasos = cas?.let { c0 -> (0 until Mandala.PASOS).filter { mandala[Mandala.clave(c0.id, it)].orEmpty().isNotBlank() } }.orEmpty()
        CampEval(cod, meta, nodoAnio(meta, mensuales), cas?.let { c0 -> pasos.count { MetodoCima.leer(c0.id, it, mandala).cumplido } } ?: 0, pasos.size,
            cas?.let { c0 -> pasos.map { p -> pasoEval(c0.id, p, mandala[Mandala.clave(c0.id, p)].orEmpty().trim(), mandala) } }.orEmpty())
    }

private fun pct(p: Double?) = p?.let { "${Math.round(it * 100)} %" } ?: "—"
@Composable private fun lectura(p: Double?) = stringResource(when { p == null -> R.string.ev_sin_datos; p >= .8 -> R.string.ev_lect_4; p >= .6 -> R.string.ev_lect_3; p >= .4 -> R.string.ev_lect_2; else -> R.string.ev_lect_1 })
private fun colorEje(cod: String?): Color = Eje.entries.firstOrNull { it.codigo == cod }?.color?.let { Color(it) } ?: Color(0xFF7A4A6B)

// ---------------------------------------------------------------- Pantalla

/** nivel: "m:2026-10", "a:2026" o "p". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluacionScreen(inicial: String?, onBack: () -> Unit, onRuta: (String) -> Unit) {
    val vm = rutaViewModel { EvaluacionViewModel(it) }
    val anuales by vm.anuales.collectAsStateWithLifecycle()
    val mensuales by vm.mensuales.collectAsStateWithLifecycle()
    val propositos by vm.propositos.collectAsStateWithLifecycle()
    val casillas by vm.casillas.collectAsStateWithLifecycle()
    val mandala by vm.mandala.collectAsStateWithLifecycle()
    val metasResp by vm.metasResp.collectAsStateWithLifecycle()
    val checks by vm.checks.collectAsStateWithLifecycle()
    val hoy = LocalDate.now()
    var tab by rememberSaveable { mutableIntStateOf(when { inicial?.startsWith("d:") == true -> 0; inicial?.startsWith("a:") == true -> 2; inicial == "p" -> 3; else -> 1 }) }
    var diaTxt by rememberSaveable { mutableStateOf(inicial?.takeIf { it.startsWith("d:") }?.removePrefix("d:") ?: hoy.toString()) }
    val dia = runCatching { LocalDate.parse(diaTxt) }.getOrDefault(hoy)
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val diaResp by vm.diaResp.collectAsStateWithLifecycle()
    val vidaResp by vm.vida.collectAsStateWithLifecycle()
    var ymTxt by rememberSaveable { mutableStateOf(inicial?.takeIf { it.startsWith("m:") }?.removePrefix("m:") ?: YearMonth.from(hoy).toString()) }
    var anio by rememberSaveable { mutableIntStateOf(inicial?.takeIf { it.startsWith("a:") }?.removePrefix("a:")?.toIntOrNull() ?: hoy.year) }
    val ym = runCatching { YearMonth.parse(ymTxt) }.getOrDefault(YearMonth.from(hoy))
    val ctx = LocalContext.current
    // Bienestar de los meses que se ven (el índice entra en el puntaje)
    val mesesBien = if (tab == 0) listOf(YearMonth.from(dia)) else if (tab == 2) (1..12).map { YearMonth.of(anio, it) }.filter { !it.atDay(1).isAfter(hoy) } else listOf(ym)
    val bvm = rutaViewModel(key = "ev-bien-${mesesBien.joinToString()}") { com.rutaalacima.app.ui.bienestar.BienestarRangoViewModel(it, mesesBien) }
    val datosBien by bvm.datos.collectAsState()
    val bienDe: (List<LocalDate>) -> Double? = { fs -> Bienestar.acumulado(fs, datosBien.dias, datosBien.agua).indice?.let { it / 100.0 } }

    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.ev_titulo)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = {
                    if (tab == 1 || tab == 2) IconButton(onClick = {
                        val html = if (tab == 1) InformeRuta.mes(ctx, ym, metasEval(ym, anuales, mensuales, casillas, mandala), habitosEval(fechasHasta(ym), checks), bienDe(fechasHasta(ym)))
                        else InformeRuta.anio(ctx, anio, campsEval(anio, anuales, mensuales, casillas, mandala),
                            (1..12).map { m -> YearMonth.of(anio, m) }.filter { !it.atDay(1).isAfter(hoy) }.map { y -> y to puntajeMes(y, anuales, mensuales, casillas, mandala, checks, bienDe) })
                        InformeRuta.imprimir(ctx, html, if (tab == 1) "SuRoute-$ym" else "SuRoute-$anio")
                    }) { Icon(Icons.Filled.Download, stringResource(R.string.ev_pdf)) }
                })
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            SecondaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                listOf(R.string.ev_tab_dia, R.string.ev_tab_mes, R.string.ev_tab_anio, R.string.ev_tab_prop).forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(t)) }) }
            }
            // La misma barra de vida de Mi ruta: se elige el año, el mes o el día que se quiere evaluar
            if (tab != 3) Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                val nivel = when (tab) { 0 -> com.rutaalacima.app.ui.ruta.Nivel.Dia(dia); 2 -> com.rutaalacima.app.ui.ruta.Nivel.Anio(anio); else -> com.rutaalacima.app.ui.ruta.Nivel.Mes(ym) }
                com.rutaalacima.app.ui.ruta.BarraVida(nivel, perfil, anuales) { n ->
                    when (n) {
                        is com.rutaalacima.app.ui.ruta.Nivel.Dia -> { diaTxt = n.fecha.toString(); tab = 0 }
                        is com.rutaalacima.app.ui.ruta.Nivel.Mes -> { ymTxt = n.ym.toString(); tab = 1 }
                        is com.rutaalacima.app.ui.ruta.Nivel.Anio -> { anio = n.anio; tab = 2 }
                        else -> {}
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (tab) {
                    0 -> diaItems(dia, vm, diaResp, checks, datosBien, onRuta)
                    1 -> mesItems(ym, vm, metasEval(ym, anuales, mensuales, casillas, mandala), habitosEval(fechasHasta(ym), checks), bienDe(fechasHasta(ym)), onRuta)
                    2 -> anioItems(anio, vm, vidaResp, anuales, mensuales, casillas, mandala, checks, bienDe, onRuta) { ymTxt = it.toString(); tab = 1 }
                    else -> propItems(propositos, anuales, mensuales, metasResp, vm, onRuta, casillas, mandala)
                }
            }
        }
    }
}

private fun puntajeMes(ym: YearMonth, anuales: List<MetaAnualEntity>, mensuales: List<MetaMensualEntity>, casillas: List<VisionCasillaEntity>,
    mandala: Map<String, String>, checks: Map<LocalDate, Set<String>>, bienDe: (List<LocalDate>) -> Double?): Double? {
    val f = fechasHasta(ym)
    return prom(listOf(prom(metasEval(ym, anuales, mensuales, casillas, mandala).map { it.p }), habitosEval(f, checks).p, bienDe(f)))
}

@Composable
private fun Puntaje(p: Double?, titulo: String, nota: String) {
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { (p ?: 0.0).toFloat() }, modifier = Modifier.fillMaxSize(), strokeWidth = 7.dp, strokeCap = StrokeCap.Round,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, gapSize = 0.dp)
                Text(p?.let { "${Math.round(it * 100)}" } ?: "—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(lectura(p), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(nota, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun Kpis(items: List<Triple<String, Double?, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (n, p, sub) ->
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp)) {
                Text(n, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(pct(p), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(progress = { (p ?: 0.0).toFloat() }, Modifier.fillMaxWidth().padding(vertical = 4.dp), strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {})
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

@Composable
private fun FilaAvance(color: Color, titulo: String, p: Double, sub: String?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(10.dp))
            Text(titulo, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(pct(p), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(progress = { p.toFloat().coerceIn(0f, 1f) }, Modifier.fillMaxWidth().padding(start = 20.dp, top = 6.dp), color = color,
            strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {})
        sub?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, top = 4.dp)) }
    }
}

/** Un ítem evaluado: ✓ cumplido, ✕ pendiente, – sin dato o en camino. */
@Composable
private fun ResultadoEv(ok: Boolean?, texto: String, sub: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        val (bg, fg) = when (ok) { true -> Color(0xFF2F6B4A) to Color.White; false -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary; null -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant }
        Box(Modifier.size(22.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            if (ok == null) Text("–", color = fg, style = MaterialTheme.typography.labelMedium)
            else Icon(if (ok) Icons.Filled.Check else Icons.Filled.Close, null, Modifier.size(14.dp), tint = fg)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(texto, style = MaterialTheme.typography.bodyMedium)
            sub?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** Separador de bloque: «Análisis» y «Preguntas de balance». */
@Composable
private fun Separador(titulo: String, sub: String) = Column(Modifier.padding(top = 10.dp)) {
    Text(titulo, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Campo(etq: String, v: String, onCambio: (String) -> Unit) =
    OutlinedTextField(v, onCambio, label = { Text(etq) }, modifier = Modifier.fillMaxWidth(), minLines = 2)

/** El día en versión evaluación: plan del día, actividades, checklist de hábitos y bienestar con su resultado, y el cierre. */
private fun androidx.compose.foundation.lazy.LazyListScope.diaItems(f: LocalDate, vm: EvaluacionViewModel, r: Map<String, String>, checks: Map<LocalDate, Set<String>>,
    bien: com.rutaalacima.app.ui.bienestar.DatosBienestar, onRuta: (String) -> Unit) {
    val futuro = f.isAfter(LocalDate.now())
    item(key = "dia-$f") {
        var ag by remember(f) { mutableStateOf<com.rutaalacima.app.data.local.AgendaDiaEntity?>(null) }
        LaunchedEffect(f) { ag = vm.agenda(f) }
        val d = ag ?: return@item
        val tops = com.rutaalacima.app.ui.planner.leerTops(r["dia#$f#top"]).filter { it.t.isNotBlank() }
        val hor = with(com.rutaalacima.app.ui.planner.HORAS_DEL_DIA) { val m = d.horarioMapa(); mapNotNull { h -> m[h]?.takeIf { it.isNotBlank() }?.let { h to it } } }
        val hecho: (Int) -> Boolean = { h -> r["dia#$f#$h#hecho"] == "1" }
        val marc = checks[f].orEmpty(); val hs = ChecklistDiario.habitos
        val bd = bien.dias[f] ?: Bienestar.Dia(); val pil = Bienestar.pilares(bd, bien.agua[f] ?: d.agua)
        val pTop = if (tops.isEmpty()) null else tops.count { it.hecho }.toDouble() / tops.size
        val pAct = if (hor.isEmpty()) null else hor.count { hecho(it.first) }.toDouble() / hor.size
        val pHab = if (hs.isEmpty()) null else hs.count { it.id in marc }.toDouble() / hs.size
        val pv = pil.values.filterNotNull(); val pBien = if (pv.isEmpty()) null else pv.sum() / Bienestar.PILARES.size
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Puntaje(if (futuro) null else prom(listOf(pTop, pAct, pHab, pBien)), stringResource(R.string.ev_puntaje_dia), stringResource(R.string.ev_nota_dia))
            @Composable fun Resultado(ok: Boolean?, texto: String, sub: String? = null) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val (bg, fg) = when (ok) { true -> Color(0xFF2F6B4A) to Color.White; false -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary; null -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant }
                    Box(Modifier.size(22.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                        Icon(if (ok == true) Icons.Filled.Check else Icons.Filled.Close, null, Modifier.size(14.dp), tint = fg)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(texto, style = MaterialTheme.typography.bodyMedium)
                        sub?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            @Composable fun Seccion(titulo: String, sub: String, p: Double?, contenido: @Composable () -> Unit) = RutaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(titulo, style = MaterialTheme.typography.titleMedium)
                        Text(sub, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(pct(p), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 10.dp, vertical = 4.dp))
                }
                contenido()
            }
            Seccion(stringResource(R.string.plan_del_dia_ev), if (d.intencion.isNotBlank()) "«${d.intencion}»" else stringResource(R.string.ev_sin_intencion), pTop) {
                if (tops.isEmpty()) Text(stringResource(R.string.ev_sin_cumbres), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                tops.forEachIndexed { i, t -> Resultado(t.hecho, "${i + 1}. ${t.t}") }
            }
            Seccion(stringResource(R.string.actividades_dia), stringResource(R.string.ev_n_hechas, hor.count { hecho(it.first) }, hor.size), pAct) {
                hor.forEach { (h, t) -> Resultado(hecho(h), t, "%02d:00".format(h)) }
            }
            Seccion(stringResource(R.string.checklist_hoy), stringResource(R.string.ev_n_hechas, hs.count { it.id in marc }, hs.size), pHab) {
                Eje.entries.forEach { e -> val del = hs.filter { it.eje == e }
                    if (del.isNotEmpty()) {
                        Text("${e.texto()} · ${del.count { it.id in marc }}/${del.size}", style = MaterialTheme.typography.labelLarge, color = Color(e.color), modifier = Modifier.padding(top = 10.dp))
                        del.forEach { h -> Resultado(h.id in marc, h.texto.ifBlank { h.id }) }
                    }
                }
            }
            val ctxC = LocalContext.current
            val climaTxt = bd.clima?.let { c0 -> ctxC.resources.getIdentifier("bi_clima_$c0", "string", ctxC.packageName).takeIf { it != 0 }?.let { ctxC.getString(it) } ?: c0 }
            Seccion(stringResource(R.string.ev_bienestar), climaTxt?.let { stringResource(R.string.ev_clima_x, it) } ?: stringResource(R.string.ev_sin_clima), pBien) {
                Bienestar.PILARES.forEach { k -> val v = pil[k]; Resultado(v?.let { it >= 0.99 }, stringResource(when (k) {
                    "sueno" -> R.string.bi_pil_sueno; "mov" -> R.string.bi_pil_mov; "fyv" -> R.string.bi_pil_fyv; "agua" -> R.string.bi_pil_agua; "aire" -> R.string.bi_pil_aire; else -> R.string.bi_pil_pant })) }
            }
            if (!futuro) RutaCard {
                Text(stringResource(R.string.cierre_dia), style = MaterialTheme.typography.titleMedium)
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cambia: (com.rutaalacima.app.data.local.AgendaDiaEntity) -> Unit = { n -> ag = n; vm.guardarAgenda(n) }
                    Campo(stringResource(R.string.victorias_dia), d.victorias) { cambia(d.copy(victorias = it)) }
                    Campo(stringResource(R.string.aprendizaje), d.aprendizaje) { cambia(d.copy(aprendizaje = it)) }
                    Campo(stringResource(R.string.gratitud), d.gratitud) { cambia(d.copy(gratitud = it)) }
                    Campo(stringResource(R.string.notas_importantes), d.notas) { cambia(d.copy(notas = it)) }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.mesItems(ym: YearMonth, vm: EvaluacionViewModel, metas: List<MetaEval>, hab: HabEval, pBien: Double?, onRuta: (String) -> Unit) {
    val pMetas = prom(metas.map { it.p })
    // Primero la misma estructura del mes de Mi ruta: cada meta elegida con sus sub-metas evaluadas
    item(key = "metas") {
        RutaCard {
            Text(stringResource(R.string.ev_metas_mes_como, nombreMes(ym.monthValue)), style = MaterialTheme.typography.titleMedium)
            if (metas.isEmpty()) Text(stringResource(R.string.ev_sin_metas_mes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            metas.forEachIndexed { i, m ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 6.dp))
                FilaAvance(colorEje(m.eje), m.titulo, m.p, stringResource(R.string.ev_dias_avance, m.dias) + if (m.total > 0) " · " + stringResource(R.string.ev_submetas_mes, m.ganadas, m.total) else "") { onRuta("m:$ym") }
                m.pasos.forEach { p -> ResultadoEv(p.ok, "${p.n + 1}. ${p.t}", if (p.ok == true) null else stringResource(R.string.ev_jornadas, p.jornadas, p.req)) }
            }
        }
    }
    item(key = "sep-an") { Separador(stringResource(R.string.ev_analisis), stringResource(R.string.ev_analisis_mes)) }
    item(key = "punt") { Puntaje(prom(listOf(pMetas, hab.p, pBien)), stringResource(R.string.ev_puntaje_mes), stringResource(R.string.ev_nota_puntaje)) }
    item(key = "kpis") {
        Kpis(listOf(Triple(stringResource(R.string.ev_metas), pMetas, stringResource(R.string.ev_n_metas, metas.size)),
            Triple(stringResource(R.string.ev_habitos), hab.p, ""), Triple(stringResource(R.string.ev_bienestar), pBien, "")))
    }
    item(key = "ejes") {
        RutaCard {
            Text(stringResource(R.string.ev_habitos_eje), style = MaterialTheme.typography.titleMedium)
            hab.porEje.forEach { (e, v) -> if (v.second > 0) Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(e.texto(), Modifier.width(112.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                LinearProgressIndicator(progress = { v.first.toFloat() / v.second }, Modifier.weight(1f), color = Color(e.color), strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {})
                Text(pct(v.first.toDouble() / v.second), Modifier.width(56.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelLarge)
            } }
        }
    }
    item(key = "bien-$ym") { com.rutaalacima.app.ui.bienestar.BienestarMesCard(ym) }
    item(key = "fin-$ym") { com.rutaalacima.app.ui.finanzas.ResumenCuentasMes(ym) }
    item(key = "sep-pr") { Separador(stringResource(R.string.ev_preguntas), stringResource(R.string.ev_preguntas_mes)) }
    item(key = "bal-$ym") {
        var m by remember(ym) { mutableStateOf<MesEntity?>(null) }
        LaunchedEffect(ym) { m = vm.mes(ym.year, ym.monthValue) }
        m?.let { b ->
            RutaCard {
                Text(stringResource(R.string.ev_balance_mes, nombreMes(ym.monthValue)), style = MaterialTheme.typography.titleMedium)
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cambia: (MesEntity) -> Unit = { n -> m = n; vm.guardarMes(n) }
                    Campo(stringResource(R.string.bal_como_mes), b.comoEstuvo) { cambia(b.copy(comoEstuvo = it)) }
                    Campo(stringResource(R.string.logros), b.logros) { cambia(b.copy(logros = it)) }
                    Campo(stringResource(R.string.bal_desafios), b.desafios) { cambia(b.copy(desafios = it)) }
                    Campo(stringResource(R.string.bal_agradecido), b.agradecido) { cambia(b.copy(agradecido = it)) }
                    Campo(stringResource(R.string.bal_mejorar), b.mejorar) { cambia(b.copy(mejorar = it)) }
                    Campo(stringResource(R.string.bal_objetivos_mes), b.objetivosProximo) { cambia(b.copy(objetivosProximo = it)) }
                    Campo(stringResource(R.string.notas_importantes), b.notas) { cambia(b.copy(notas = it)) }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.anioItems(anio: Int, vm: EvaluacionViewModel, vida: Map<String, String>, anuales: List<MetaAnualEntity>, mensuales: List<MetaMensualEntity>,
    casillas: List<VisionCasillaEntity>, mandala: Map<String, String>, checks: Map<LocalDate, Set<String>>, bienDe: (List<LocalDate>) -> Double?,
    onRuta: (String) -> Unit, onMes: (YearMonth) -> Unit) {
    val hoy = LocalDate.now()
    val meses = (1..12).map { YearMonth.of(anio, it) }
    val pMeses = meses.map { if (it.atDay(1).isAfter(hoy)) null else puntajeMes(it, anuales, mensuales, casillas, mandala, checks, bienDe) }
    val delAnio = anuales.filter { it.anio == anio }
    val pMetas = prom(delAnio.map { nodoAnio(it, mensuales) })
    val todas = meses.filter { !it.atDay(1).isAfter(hoy) }.flatMap { fechasHasta(it) }
    val hab = habitosEval(todas, checks); val pBien = bienDe(todas)
    // Primero la misma estructura del año de Mi ruta: tu año (palabra y anhelos, ¿se cumplieron?) y su Brújula
    item(key = "corazon-a-$anio") {
        val anh = vida[com.rutaalacima.app.ui.ruta.claveVida(anio, "anhelos")].orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
        val palabra = vida[com.rutaalacima.app.ui.ruta.claveVida(anio, "palabra")].orEmpty()
        RutaCard {
            Text(stringResource(R.string.ev_tu_anio, anio), style = MaterialTheme.typography.titleMedium)
            if (palabra.isNotBlank()) Text(stringResource(R.string.ev_palabra, palabra), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (anh.isEmpty()) Text(stringResource(R.string.ev_sin_anhelos), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            anh.forEachIndexed { i, t ->
                val k = "eval:anh:$anio:$i"; val v = vida[k]; val ok = when (v) { "1" -> true; "0" -> false; else -> null }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { ResultadoEv(ok, t) }
                    androidx.compose.material3.FilterChip(selected = ok == true, onClick = { vm.anhelo(k, if (v == "1") "" else "1") }, label = { Text(stringResource(R.string.si)) })
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.FilterChip(selected = ok == false, onClick = { vm.anhelo(k, if (v == "0") "" else "0") }, label = { Text(stringResource(R.string.no)) })
                }
            }
        }
    }
    item(key = "brujula-a") {
        val camps = campsEval(anio, anuales, mensuales, casillas, mandala)
        RutaCard {
            Text(stringResource(R.string.ev_brujula_anio, anio), style = MaterialTheme.typography.titleMedium)
            if (camps.isEmpty()) Text(stringResource(R.string.ev_sin_brujula), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            camps.forEachIndexed { i, cmp ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 6.dp))
                FilaAvance(colorEje(cmp.cod), cmp.meta.titulo, cmp.p, com.rutaalacima.app.ui.vision.nombreCodigo(cmp.cod) + if (cmp.total > 0) " · " + stringResource(R.string.ev_submetas_ganadas, cmp.ganadas, cmp.total) else "") { onRuta("a:$anio") }
                cmp.pasos.forEach { p -> ResultadoEv(p.ok, p.t, if (p.ok == true) null else stringResource(R.string.ev_jornadas, p.jornadas, p.req)) }
            }
        }
    }
    item(key = "sep-an-a") { Separador(stringResource(R.string.ev_analisis), stringResource(R.string.ev_analisis_anio, anio)) }
    item(key = "punt-a") { Puntaje(prom(listOf(pMetas, hab.p, pBien)), stringResource(R.string.ev_puntaje_anio, anio), stringResource(R.string.ev_toca_mes)) }
    item(key = "kpis-a") { Kpis(listOf(Triple(stringResource(R.string.ev_metas), pMetas, stringResource(R.string.ev_n_metas, delAnio.size)), Triple(stringResource(R.string.ev_habitos), hab.p, ""), Triple(stringResource(R.string.ev_bienestar), pBien, ""))) }
    item(key = "meses-a") {
        RutaCard {
            Text(stringResource(R.string.ev_mes_a_mes), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().height(130.dp).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                meses.forEachIndexed { i, y -> val p = pMeses[i]
                    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).clickable(enabled = p != null) { onMes(y) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.weight(1f).width(18.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight(((p ?: 0.0).toFloat()).coerceIn(0.03f, 1f).takeIf { p != null } ?: 0f).background(MaterialTheme.colorScheme.primary))
                        }
                        Text(p?.let { "${Math.round(it * 100)}" } ?: "", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(nombreMes(y.monthValue).take(3), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
    item(key = "bien-a-$anio") { com.rutaalacima.app.ui.bienestar.BienestarAnioCard(anio) { onMes(it) } }
    item(key = "sep-pr-a") { Separador(stringResource(R.string.ev_preguntas), stringResource(R.string.ev_preguntas_anio)) }
    item(key = "bal-a-$anio") {
        val guardado by remember(anio) { vm.balance(anio) }.collectAsState(initial = null)
        var b by remember(anio, guardado) { mutableStateOf(guardado ?: BalanceAnualEntity(anio = anio)) }
        val cambia: (BalanceAnualEntity) -> Unit = { n -> b = n; vm.guardarBalance(n) }
        RutaCard {
            Text(stringResource(R.string.ev_balance_anio, anio), style = MaterialTheme.typography.titleMedium)
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo(stringResource(R.string.bal_como_me_senti), b.comoMeSenti) { cambia(b.copy(comoMeSenti = it)) }
                Campo(stringResource(R.string.bal_positivo), b.positivo) { cambia(b.copy(positivo = it)) }
                Campo(stringResource(R.string.bal_negativo), b.negativo) { cambia(b.copy(negativo = it)) }
                Campo(stringResource(R.string.bal_lo_mejor), b.loMejor) { cambia(b.copy(loMejor = it)) }
                Campo(stringResource(R.string.bal_objetivos), b.objetivosProximo) { cambia(b.copy(objetivosProximo = it)) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.propItems(propositos: List<PropositoEntity>, anuales: List<MetaAnualEntity>, mensuales: List<MetaMensualEntity>,
    resp: Map<String, String>, vm: EvaluacionViewModel, onRuta: (String) -> Unit,
    casillas: List<VisionCasillaEntity> = emptyList(), mandala: Map<String, String> = emptyMap()) {
    // Las casillas de la Brújula general: cada propósito es un campamento y sus metas específicas son sus pasos
    val generales = Mandala.repartir(casillas.filter { Mandala.anioDeOrigen(it.origen) == null }, { it.origen }, { it.eje }).second
    if (propositos.isEmpty()) item { Text(stringResource(R.string.ev_sin_propositos), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    items(propositos, key = { "p${it.id}" }) { p ->
        val rango = com.rutaalacima.app.ui.ruta.aniosDeCumbre(p, anuales)
        val hoyA = LocalDate.now().year
        val metasP = anuales.filter { it.propositoId == p.id }
        val avance = metasP.map { nodoAnio(it, mensuales) }.takeIf { it.isNotEmpty() }?.average() ?: (p.progreso / 100.0)
        val tiempo = ((hoyA - rango.first + 1).toDouble() / (rango.last - rango.first + 1).coerceAtLeast(1)).coerceIn(0.0, 1.0)
        RutaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(colorEje(p.eje))); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.ev_prop_sub, rango.first, rango.last, Math.round(tiempo * 100).toInt()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { avance.toFloat() }, modifier = Modifier.fillMaxSize(), strokeWidth = 5.dp, strokeCap = StrokeCap.Round, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, gapSize = 0.dp)
                    Text("${Math.round(avance * 100)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
            // La misma estructura del propósito: para qué, indicador de éxito y metas específicas (¿se están cumpliendo?)
            listOf(R.string.ev_prop_desc to p.descripcion, R.string.ev_prop_porque to p.porQueImporta, R.string.ev_prop_indicador to p.indicadorExito).forEach { (etq, v) ->
                if (v.isNotBlank()) {
                    Text(stringResource(etq), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                    Text(v, style = MaterialTheme.typography.bodyMedium)
                }
            }
            val cas = generales.getOrNull(Mandala.CAMPAMENTOS_FIJOS.indexOf(p.eje ?: "CON"))
            val especificas = p.metasEspecificas.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (especificas.isNotEmpty()) {
                Text(stringResource(R.string.ev_prop_especificas), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                especificas.forEachIndexed { i, t -> val pe = cas?.let { pasoEval(it.id, i, t, mandala) }
                    ResultadoEv(pe?.ok, t, pe?.takeIf { it.ok != true && it.jornadas > 0 }?.let { stringResource(R.string.ev_jornadas, it.jornadas, it.req) }) }
            }
            Separador(stringResource(R.string.ev_analisis), stringResource(R.string.ev_prop_analisis))
            Text(stringResource(if (avance >= tiempo) R.string.ev_prop_ritmo else R.string.ev_prop_atras), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rango.forEach { y ->
                    val ms = metasP.filter { it.anio == y }; val py = ms.map { nodoAnio(it, mensuales) }.takeIf { it.isNotEmpty() }?.average()
                    Column(Modifier.width(96.dp).clip(RoundedCornerShape(12.dp)).background(if (y == hoyA) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onRuta("a:$y") }.padding(10.dp)) {
                        Text("$y", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(progress = { (py ?: 0.0).toFloat() }, Modifier.fillMaxWidth().padding(vertical = 4.dp), strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {})
                        Text(if (ms.isEmpty()) stringResource(R.string.ev_sin_metas_corto) else "${ms.size} · ${pct(py)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            val k = "eval:p:${p.id}"
            var bien by remember(p.id) { mutableStateOf(resp["$k:bien"].orEmpty()) }
            var ajustar by remember(p.id) { mutableStateOf(resp["$k:ajustar"].orEmpty()) }
            Separador(stringResource(R.string.ev_preguntas), stringResource(R.string.ev_preguntas_prop))
            Spacer(Modifier.height(8.dp))
            Campo(stringResource(R.string.ev_funciona), bien) { bien = it; vm.reflexion("$k:bien", it) }
            Spacer(Modifier.height(8.dp))
            Campo(stringResource(R.string.ev_ajustar), ajustar) { ajustar = it; vm.reflexion("$k:ajustar", it) }
        }
    }
}

// ---------------------------------------------------------------- Informes PDF (Imprimir › Guardar como PDF)

object InformeRuta {
    private var vista: WebView? = null
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    private const val CSS = "*{box-sizing:border-box}body{font:14px/1.5 sans-serif;color:#2B2522;margin:24px}h1{font:700 26px/1.2 serif;color:#6B2A1A;margin:0}" +
        "h2{font:600 17px/1.3 serif;color:#6B2A1A;margin:24px 0 8px;padding-bottom:6px;border-bottom:1px solid #E6DDD3}.marca{font:600 11px sans-serif;letter-spacing:.12em;color:#B08A3E}" +
        ".kpis{display:flex;gap:10px;margin:14px 0}.kpis div{flex:1;border:1px solid #E6DDD3;border-radius:12px;padding:10px}.kpis span{display:block;color:#7A6E66;font-size:12px}.kpis b{font-size:20px}" +
        "table{width:100%;border-collapse:collapse}th,td{text-align:left;padding:7px 6px;border-bottom:1px solid #EFE8E0}th{font-size:12px;color:#7A6E66}td.n{text-align:right}" +
        ".barra{height:6px;background:#EFE8E0;border-radius:6px;overflow:hidden;min-width:70px}.barra i{display:block;height:100%;background:#6B2A1A}.txt{white-space:pre-wrap}.pie{margin-top:30px;color:#9A8F87;font-size:12px}"
    private fun barra(p: Double?) = "<div class=\"barra\"><i style=\"width:${((p ?: 0.0) * 100).coerceIn(0.0, 100.0)}%\"></i></div>"
    private fun doc(ctx: Context, titulo: String, cuerpo: String) = "<!doctype html><html><head><meta charset=\"utf-8\"><style>$CSS</style></head><body>" +
        "<div class=\"marca\">SUROUTE · RUTA A LA CIMA</div><h1>${esc(titulo)}</h1>$cuerpo<p class=\"pie\">${esc(ctx.getString(R.string.ev_pdf_pie, LocalDate.now().toString()))}</p></body></html>"

    fun mes(ctx: Context, ym: YearMonth, metas: List<MetaEval>, hab: HabEval, pBien: Double?): String {
        val pMetas = prom(metas.map { it.p }); val punt = prom(listOf(pMetas, hab.p, pBien))
        val k = "<div class=\"kpis\"><div><span>${ctx.getString(R.string.ev_puntaje)}</span><b>${pct(punt)}</b></div><div><span>${ctx.getString(R.string.ev_metas)}</span><b>${pct(pMetas)}</b></div>" +
            "<div><span>${ctx.getString(R.string.ev_habitos)}</span><b>${pct(hab.p)}</b></div><div><span>${ctx.getString(R.string.ev_bienestar)}</span><b>${pct(pBien)}</b></div></div>"
        val t = "<h2>${ctx.getString(R.string.ev_cumplimiento)}</h2><table>" + metas.joinToString("") { m ->
            "<tr><td>${esc(m.titulo)}</td><td>${barra(m.p)}</td><td class=\"n\">${pct(m.p)}</td><td class=\"n\">${m.dias}</td><td class=\"n\">${if (m.total > 0) "${m.ganadas}/${m.total}" else "—"}</td></tr>" } + "</table>"
        val e = "<h2>${ctx.getString(R.string.ev_habitos_eje)}</h2><table>" + hab.porEje.entries.joinToString("") { (eje, v) ->
            if (v.second == 0) "" else "<tr><td>${esc(ctx.getString(com.rutaalacima.app.ui.i18n.Textos.nombre(eje)))}</td><td>${barra(v.first.toDouble() / v.second)}</td><td class=\"n\">${pct(v.first.toDouble() / v.second)}</td></tr>" } + "</table>"
        return doc(ctx, ctx.getString(R.string.ev_informe_de, "${nombreMesTxt(ym.monthValue)} ${ym.year}"), k + t + e)
    }
    fun anio(ctx: Context, anio: Int, camps: List<CampEval>, meses: List<Pair<YearMonth, Double?>>): String {
        val m = "<h2>${ctx.getString(R.string.ev_mes_a_mes)}</h2><table>" + meses.joinToString("") { (y, p) -> "<tr><td>${nombreMesTxt(y.monthValue)}</td><td>${barra(p)}</td><td class=\"n\">${pct(p)}</td></tr>" } + "</table>"
        val c = "<h2>${ctx.getString(R.string.ev_brujula_anio, anio)}</h2><table>" + camps.joinToString("") { x ->
            "<tr><td>${esc(x.meta.titulo)}</td><td>${barra(x.p)}</td><td class=\"n\">${pct(x.p)}</td><td class=\"n\">${if (x.total > 0) "${x.ganadas}/${x.total}" else "—"}</td></tr>" } + "</table>"
        return doc(ctx, ctx.getString(R.string.ev_informe_de, "$anio"), m + c)
    }
    /** Un día: intención, 3 cumbres, actividades por hora y cierre. */
    fun dia(ctx: Context, f: LocalDate, intencion: String, tops: List<Pair<String, Boolean>>, acts: List<Triple<Int, String, Boolean>>,
        cierre: List<Pair<String, String>>, energia: Int, agua: Int): String {
        val fecha = f.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL))
        val hecha = ctx.getString(R.string.hecha)
        val c = (if (intencion.isNotBlank()) "<p><b>${esc(intencion)}</b></p>" else "") +
            "<h2>${ctx.getString(R.string.ev_pdf_cumbres)}</h2><table>" + tops.mapIndexed { i, (t, h) -> "<tr><td>${i + 1}. ${esc(t)}</td><td class=\"n\">${if (h) hecha else "—"}</td></tr>" }.joinToString("") + "</table>" +
            "<h2>${ctx.getString(R.string.actividades_dia)}</h2><table>" + acts.joinToString("") { (h, t, ok) -> "<tr><td>${"%02d".format(h)}:00</td><td>${esc(t)}</td><td class=\"n\">${if (ok) hecha else "—"}</td></tr>" } + "</table>" +
            "<h2>${ctx.getString(R.string.cierre_dia)}</h2>" + cierre.filter { it.second.isNotBlank() }.joinToString("") { (k, v) -> "<p><b>${esc(k)}</b></p><p class=\"txt\">${esc(v)}</p>" } +
            "<p>${esc(ctx.getString(R.string.energia))}: $energia/5 · ${esc(ctx.getString(R.string.agua_vasos))}: $agua/8</p>"
        return doc(ctx, fecha.replaceFirstChar { it.uppercase() }, c)
    }
    private fun nombreMesTxt(m: Int) = java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, java.util.Locale.getDefault()).replaceFirstChar { it.uppercase() }

    fun imprimir(ctx: Context, html: String, nombre: String) {
        val w = WebView(ctx); vista = w
        w.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
                pm.print(nombre, view.createPrintDocumentAdapter(nombre), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
            }
        }
        w.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }
}


/** Acceso desde Mi ruta: el balance, el bienestar y las finanzas del mes o del año viven en Evaluación. */
@Composable
fun EvaluarCard(titulo: String, sub: String, nivel: String) {
    val app = LocalContext.current.applicationContext as com.rutaalacima.app.RutaApp
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Insights, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        androidx.compose.material3.Button(onClick = { app.container.navegacionPendiente.value = com.rutaalacima.app.ui.Rutas.evaluacion(nivel) },
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth()) { Text(stringResource(R.string.ev_evaluar)) }
    }
}
