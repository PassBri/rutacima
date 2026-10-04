package com.rutaalacima.app.ui.ruta

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.rutaalacima.app.data.local.AgendaDiaEntity
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.PerfilEntity
import com.rutaalacima.app.data.local.PropositoEntity
import com.rutaalacima.app.data.local.avance
import com.rutaalacima.app.data.local.diasMarcados
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.domain.model.ChecklistDiario
import com.rutaalacima.app.domain.model.RecordatorioVida
import com.rutaalacima.app.domain.model.Vida
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.metas.NivelMeta
import com.rutaalacima.app.ui.perfil.esperanza
import com.rutaalacima.app.ui.planner.nombreMes
import com.rutaalacima.app.util.hoy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Mes (1-12) en que se creó una publicación. */
fun Post.mesDelAnio(): Int = Instant.ofEpochMilli(creadoEn).atZone(ZoneId.systemDefault()).monthValue
fun Post.fecha(): LocalDate = Instant.ofEpochMilli(creadoEn).atZone(ZoneId.systemDefault()).toLocalDate()

/** Nivel de la cascada de la vida. Se guarda como texto para sobrevivir a la rotación. */
sealed class Nivel {
    data object Vida : Nivel()
    data class Anio(val anio: Int) : Nivel()
    data class Mes(val ym: YearMonth) : Nivel()
    data class Semana(val lunes: LocalDate) : Nivel()
    data class Dia(val fecha: LocalDate) : Nivel()

    fun codificar(): String = when (this) {
        Vida -> "vida"; is Anio -> "anio:$anio"; is Mes -> "mes:$ym"; is Semana -> "semana:$lunes"; is Dia -> "dia:$fecha"
    }

    /** Nivel superior (para el gesto Atrás). */
    fun arriba(): Nivel? = when (this) {
        Vida -> null
        is Anio -> Vida
        is Mes -> Anio(ym.year)
        is Semana -> Mes(YearMonth.from(lunes.plusDays(3)))
        is Dia -> Semana(fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }

    companion object {
        fun decodificar(s: String): Nivel = runCatching {
            val (t, v) = (s.split(':', limit = 2) + "").let { it[0] to it[1] }
            when (t) {
                "anio" -> Anio(v.toInt()); "mes" -> Mes(YearMonth.parse(v))
                "semana" -> Semana(LocalDate.parse(v)); "dia" -> Dia(LocalDate.parse(v)); else -> Vida
            }
        }.getOrDefault(Vida)
    }
}

class RutaViewModel(private val c: AppContainer) : ViewModel() {
    private fun <T> e(f: Flow<T>, i: T) = f.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), i)
    val perfil: StateFlow<PerfilEntity> = e(c.perfil.perfil, PerfilEntity())
    val posts: StateFlow<List<Post>> = e(c.social.misPublicaciones, emptyList())
    val propositos: StateFlow<List<PropositoEntity>> = e(c.planificador.propositos, emptyList())
    val anuales: StateFlow<List<MetaAnualEntity>> = e(c.planificador.todasLasMetas, emptyList())
    val mensuales: StateFlow<List<MetaMensualEntity>> = e(c.planAnual.todasMetasMes, emptyList())

    private val desde = MutableStateFlow(hoy().withDayOfMonth(1))

    @OptIn(ExperimentalCoroutinesApi::class)
    val checks: StateFlow<Map<LocalDate, Set<String>>> = e(desde.flatMapLatest { c.checklist.desde(it) }, emptyMap())

    /** Carga los hábitos desde la fecha indicada (el inicio de lo que se está viendo). */
    fun verDesde(fecha: LocalDate) { if (fecha != desde.value) desde.value = fecha }

    fun diasConAgenda(ym: YearMonth): Flow<Set<Int>> = c.agenda.diasConAgenda(ym.year, ym.monthValue)
    suspend fun agenda(fecha: LocalDate): AgendaDiaEntity = c.agenda.dia(fecha)
    fun alternarDia(m: MetaMensualEntity, dia: Int) = viewModelScope.launch { c.planAnual.alternarDia(m, dia) }
    fun cambiarMeta(anios: Int) = viewModelScope.launch { c.perfil.actualizar { it.copy(esperanzaVida = anios) } }
}

/**
 * Pantalla principal: la vida en 120 puntos y, tocando, la cascada Vida → Año → Mes → Semana → Día.
 * Cada nivel muestra sus metas, su avance y sus recuerdos, y enlaza con la herramienta completa.
 */
@Composable
fun RutaScreen(
    contentPadding: PaddingValues,
    onPlanificador: (tab: Int, fecha: LocalDate?) -> Unit,
    onNuevaMeta: (NivelMeta) -> Unit,
    onProposito: (Long) -> Unit,
    onPublicar: (String) -> Unit,
    onAbrirPost: (String) -> Unit,
    onKit: () -> Unit,
    onAjustes: () -> Unit,
) {
    val vm = rutaViewModel { RutaViewModel(it) }
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val posts by vm.posts.collectAsStateWithLifecycle()
    val propositos by vm.propositos.collectAsStateWithLifecycle()
    val anuales by vm.anuales.collectAsStateWithLifecycle()
    val mensuales by vm.mensuales.collectAsStateWithLifecycle()
    val checks by vm.checks.collectAsStateWithLifecycle()
    var codigo by rememberSaveable { mutableStateOf("vida") }
    val nivel = Nivel.decodificar(codigo)
    val ir: (Nivel) -> Unit = { codigo = it.codificar() }

    BackHandler(enabled = nivel != Nivel.Vida) { nivel.arriba()?.let(ir) }
    LaunchedEffect(codigo) {
        vm.verDesde(
            when (nivel) {
                is Nivel.Mes -> nivel.ym.atDay(1).minusDays(6)
                is Nivel.Semana -> nivel.lunes
                is Nivel.Dia -> nivel.fecha
                else -> hoy().withDayOfMonth(1)
            },
        )
    }

    Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Migas(nivel, perfil, ir)
        AnimatedContent(targetState = nivel, label = "nivel", transitionSpec = { fadeIn() togetherWith fadeOut() }) { n ->
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (n) {
                    Nivel.Vida -> nivelVida(perfil, posts, propositos, ir, { vm.cambiarMeta(it) }, onNuevaMeta, onProposito, onAjustes)
                    is Nivel.Anio -> nivelAnio(n.anio, perfil, anuales, mensuales, posts, ir, onNuevaMeta, onPlanificador)
                    is Nivel.Mes -> nivelMes(n.ym, mensuales, checks, posts, vm, ir, onNuevaMeta, onPlanificador)
                    is Nivel.Semana -> nivelSemana(n.lunes, mensuales, checks, ir, onPlanificador, onKit)
                    is Nivel.Dia -> nivelDia(n.fecha, perfil, mensuales, checks, vm, onPlanificador, onKit, onPublicar)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Migas de pan

@Composable
private fun Migas(nivel: Nivel, perfil: PerfilEntity, ir: (Nivel) -> Unit) {
    val meta = perfil.esperanza()
    val pasos = buildList<Pair<String, Nivel>> {
        add(stringResource(R.string.ruta_n_anios, meta) to Nivel.Vida)
        when (nivel) {
            Nivel.Vida -> Unit
            is Nivel.Anio -> add("${nivel.anio}" to nivel)
            is Nivel.Mes -> { add("${nivel.ym.year}" to Nivel.Anio(nivel.ym.year)); add(nombreMes(nivel.ym.monthValue) to nivel) }
            is Nivel.Semana -> {
                val ym = YearMonth.from(nivel.lunes.plusDays(3))
                add("${ym.year}" to Nivel.Anio(ym.year)); add(nombreMes(ym.monthValue) to Nivel.Mes(ym))
                add(stringResource(R.string.ruta_semana_n, nivel.lunes.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)) to nivel)
            }
            is Nivel.Dia -> {
                val ym = YearMonth.from(nivel.fecha)
                val lunes = nivel.fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                add("${ym.year}" to Nivel.Anio(ym.year)); add(nombreMes(ym.monthValue) to Nivel.Mes(ym))
                add(stringResource(R.string.ruta_semana_n, lunes.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)) to Nivel.Semana(lunes))
                add("${nivel.fecha.dayOfMonth}" to nivel)
            }
        }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        pasos.forEachIndexed { i, (texto, destino) ->
            item {
                val ultimo = i == pasos.lastIndex
                Text(
                    texto, style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (ultimo) FontWeight.Bold else FontWeight.Normal,
                    color = if (ultimo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(if (ultimo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { ir(destino) }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                if (!ultimo) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------- Recordatorio del día (120 años)

/** Tarjeta del recordatorio diario del camino hacia los N años. */
@Composable
fun RecordatorioDiaCard(perfil: PerfilEntity, onAjustes: () -> Unit, compacta: Boolean = false) {
    val anioNac = perfil.anioNacimiento
    val primary = MaterialTheme.colorScheme.primary
    val nf = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(primary, primary.copy(alpha = 0.8f))))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WbSunny, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.recordatorio_titulo), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
        if (anioNac == null) {
            Text(stringResource(R.string.recordatorio_sin_fecha), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(top = 8.dp))
            Text(
                stringResource(R.string.configurar_nacimiento), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f))
                    .clickable(onClick = onAjustes).padding(horizontal = 14.dp, vertical = 10.dp),
            )
            return@Column
        }
        val r = RecordatorioVida.calcular(anioNac, perfil.mesNacimiento ?: 1, perfil.esperanza(), hoy())
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.recordatorio_dia_n, nf.format(r.diaDeVida)), style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.recordatorio_quedan, nf.format(r.diasRestantes), r.meta), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { r.avance }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round,
        )
        if (!compacta) {
            Spacer(Modifier.height(10.dp))
            Text("“" + stringResource(fraseRecordatorio(r.frase)) + "”", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

fun fraseRecordatorio(i: Int): Int = when (i) {
    0 -> R.string.frase_120_1; 1 -> R.string.frase_120_2; 2 -> R.string.frase_120_3; 3 -> R.string.frase_120_4
    4 -> R.string.frase_120_5; 5 -> R.string.frase_120_6; 6 -> R.string.frase_120_7; 7 -> R.string.frase_120_8
    8 -> R.string.frase_120_9; 9 -> R.string.frase_120_10; 10 -> R.string.frase_120_11; 11 -> R.string.frase_120_12
    12 -> R.string.frase_120_13; else -> R.string.frase_120_14
}

// ---------------------------------------------------------------- Nivel 1: Vida (los puntos)

private fun androidx.compose.foundation.lazy.LazyListScope.nivelVida(
    perfil: PerfilEntity,
    posts: List<Post>,
    propositos: List<PropositoEntity>,
    ir: (Nivel) -> Unit,
    onMeta: (Int) -> Unit,
    onNuevaMeta: (NivelMeta) -> Unit,
    onProposito: (Long) -> Unit,
    onAjustes: () -> Unit,
) {
    item { RecordatorioDiaCard(perfil, onAjustes) }
    val anioNac = perfil.anioNacimiento
    if (anioNac != null) {
        val meta = perfil.esperanza()
        val h = hoy()
        val edad = java.time.Period.between(LocalDate.of(anioNac, perfil.mesNacimiento ?: 1, 1), h).years
        val aniosConRecuerdos = posts.map { it.anio }.toSet()
        // Año en que vence cada propósito (se marca con una bandera en su punto)
        val aniosMeta = propositos.map { java.time.Instant.ofEpochMilli(it.creadoEn).atZone(ZoneId.systemDefault()).year + it.horizonte }.toSet()
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.vida_camino, meta), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Vida.METAS.forEach { m ->
                        val sel = m == meta
                        Text(
                            "$m", style = MaterialTheme.typography.labelLarge,
                            color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onMeta(m) }.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.vida_anios_de, edad, meta), style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                PuntosVida(anioNac, edad, meta, aniosConRecuerdos, aniosMeta) { ir(Nivel.Anio(it)) }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.ruta_puntos_ayuda), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.ruta_cumbres), Modifier.weight(1f))
            OutlinedButton(onClick = { onNuevaMeta(NivelMeta.CINCO_ANIOS) }) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.nuevo_proposito))
            }
        }
    }
    if (propositos.isEmpty()) {
        item { Text(stringResource(R.string.cascada_vacia_texto), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    items(propositos, key = { "p${it.id}" }) { p ->
        val anioMeta = Instant.ofEpochMilli(p.creadoEn).atZone(ZoneId.systemDefault()).year + p.horizonte
        RutaCard(onClick = { onProposito(p.id) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Flag, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.ruta_meta_en, p.horizonte, anioMeta), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${p.progreso}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            ProgressLine(p.progreso / 100f, Modifier.padding(top = 8.dp))
        }
    }
}

/** La vida en puntos: uno por año, 10 por fila. Bandera dorada = año de una meta a largo plazo. */
@Composable
fun PuntosVida(anioNac: Int, edad: Int, meta: Int, aniosConRecuerdos: Set<Int>, aniosMeta: Set<Int>, onAnio: (Int) -> Unit) {
    val vivido = MaterialTheme.colorScheme.primary
    val recuerdo = MaterialTheme.colorScheme.secondary
    val futuro = MaterialTheme.colorScheme.surfaceVariant
    val borde = MaterialTheme.colorScheme.outlineVariant
    (0 until meta).chunked(10).forEach { fila ->
        Row(Modifier.fillMaxWidth()) {
            fila.forEach { e ->
                val anio = anioNac + e
                val pasado = e < edad
                val actual = e == edad
                Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp).clip(CircleShape).clickable { onAnio(anio) }, contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.fillMaxSize(0.86f).clip(CircleShape)
                            .background(
                                when {
                                    anio in aniosConRecuerdos -> recuerdo
                                    pasado -> vivido
                                    else -> futuro
                                },
                            )
                            .then(
                                when {
                                    actual -> Modifier.border(2.5.dp, recuerdo, CircleShape)
                                    !pasado -> Modifier.border(1.dp, borde, CircleShape)
                                    else -> Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (anio in aniosMeta) Icon(Icons.Filled.Flag, null, Modifier.fillMaxSize(0.6f), tint = recuerdo)
                    }
                }
            }
            repeat(10 - fila.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

// ---------------------------------------------------------------- Nivel 2: Año

private fun androidx.compose.foundation.lazy.LazyListScope.nivelAnio(
    anio: Int,
    perfil: PerfilEntity,
    anuales: List<MetaAnualEntity>,
    mensuales: List<MetaMensualEntity>,
    posts: List<Post>,
    ir: (Nivel) -> Unit,
    onNuevaMeta: (NivelMeta) -> Unit,
    onPlanificador: (Int, LocalDate?) -> Unit,
) {
    val delAnio = anuales.filter { it.anio == anio }
    val mesesDelAnio = mensuales.filter { it.anio == anio }
    val recuerdos = posts.filter { it.anio == anio }
    val avance = if (delAnio.isEmpty()) null else delAnio.map { it.avance }.average().toFloat() / 100f
    item {
        RutaCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$anio", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                perfil.anioNacimiento?.let {
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.vida_anios_edad, anio - it), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            Text(stringResource(R.string.ruta_anio_resumen, delAnio.size, mesesDelAnio.size, recuerdos.size),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            avance?.let { ProgressLine(it, Modifier.padding(top = 8.dp)) }
        }
    }
    item {
        // Los 12 meses del año como calendario
        (1..12).chunked(3).forEach { fila ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { m ->
                    val metasMes = mesesDelAnio.filter { it.mes == m }
                    val av = if (metasMes.isEmpty()) 0f else metasMes.map { it.avance() }.average().toFloat()
                    val nRec = recuerdos.count { it.mesDelAnio() == m }
                    val esActual = YearMonth.of(anio, m) == YearMonth.from(hoy())
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .background(if (esActual) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { ir(Nivel.Mes(YearMonth.of(anio, m))) }.padding(12.dp),
                    ) {
                        Text(nombreMes(m), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.ruta_n_metas, metasMes.size), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ProgressLine(av, Modifier.padding(vertical = 6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape)
                                .background(if (nRec > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant))
                            Spacer(Modifier.width(4.dp))
                            Text("$nRec", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
    item { SectionTitle(stringResource(R.string.ruta_metas_del_anio)) }
    if (delAnio.isEmpty()) item {
        Text(stringResource(R.string.metas_anio_vacio_texto), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(delAnio, key = { "a${it.id}" }) { m ->
        RutaCard(onClick = { onPlanificador(3, LocalDate.of(anio, 1, 1)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("${m.avance}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            ProgressLine(m.avance / 100f, Modifier.padding(top = 6.dp))
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onNuevaMeta(NivelMeta.ANIO) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.nueva_meta), maxLines = 1)
            }
            OutlinedButton(onClick = { onPlanificador(5, LocalDate.of(anio, 1, 1)) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.metas_tab_balance), maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------- Nivel 3: Mes (semanas y días)

private fun androidx.compose.foundation.lazy.LazyListScope.nivelMes(
    ym: YearMonth,
    mensuales: List<MetaMensualEntity>,
    checks: Map<LocalDate, Set<String>>,
    posts: List<Post>,
    vm: RutaViewModel,
    ir: (Nivel) -> Unit,
    onNuevaMeta: (NivelMeta) -> Unit,
    onPlanificador: (Int, LocalDate?) -> Unit,
) {
    val metas = mensuales.filter { it.anio == ym.year && it.mes == ym.monthValue }
    val diasRecuerdo = posts.map { it.fecha() }.filter { YearMonth.from(it) == ym }.map { it.dayOfMonth }.toSet()
    item {
        val agenda by remember(ym) { vm.diasConAgenda(ym) }.collectAsState(initial = emptySet())
        RutaCard {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(36.dp))
                (0..6).forEach { d ->
                    Text(DayOfWeek.MONDAY.plus(d.toLong()).getDisplayName(TextStyle.NARROW_STANDALONE, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            var lunes = ym.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val fin = ym.atEndOfMonth()
            while (!lunes.isAfter(fin)) {
                val semana = lunes
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Número de semana: toca para entrar a la semana
                    Text(
                        "S${semana.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp).clip(RoundedCornerShape(8.dp)).clickable { ir(Nivel.Semana(semana)) }.padding(vertical = 10.dp),
                    )
                    (0..6).forEach { d ->
                        val f = semana.plusDays(d.toLong())
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                            if (YearMonth.from(f) == ym) {
                                DiaCelda(f, checks[f]?.size ?: 0, f.dayOfMonth in agenda, f.dayOfMonth in diasRecuerdo) { ir(Nivel.Dia(f)) }
                            }
                        }
                    }
                }
                lunes = lunes.plusWeeks(1)
            }
        }
    }
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.metas_mensuales_n, metas.size), Modifier.weight(1f))
            OutlinedButton(onClick = { onNuevaMeta(NivelMeta.MES) }) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) }
        }
    }
    if (metas.isEmpty()) item {
        Text(stringResource(R.string.crear_primera_meta_mes), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(metas, key = { "m${it.id}" }) { m ->
        RutaCard {
            Text(m.texto, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.dias_de, m.diasMarcados().size, m.objetivoDias), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ProgressLine(m.avance(), Modifier.padding(top = 6.dp))
        }
    }
    item {
        OutlinedButton(onClick = { onPlanificador(2, ym.atDay(1)) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.CalendarMonth, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.ruta_planificador_mes))
        }
    }
}

/** Día del calendario: color por hábitos cumplidos, punto dorado si hay recuerdo, anillo si es hoy. */
@Composable
private fun DiaCelda(f: LocalDate, habitos: Int, conAgenda: Boolean, conRecuerdo: Boolean, onClick: () -> Unit) {
    val esHoy = f == hoy()
    val nivel = (habitos / ChecklistDiario.TOTAL.toFloat()).coerceIn(0f, 1f)
    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
            .background(
                if (habitos > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f + 0.6f * nivel)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .then(if (esHoy) Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp)) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("${f.dayOfMonth}", style = MaterialTheme.typography.labelMedium,
            color = if (nivel > 0.5f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (conAgenda) Box(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant))
            if (conRecuerdo) Box(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
        }
    }
}

// ---------------------------------------------------------------- Nivel 4: Semana

private fun androidx.compose.foundation.lazy.LazyListScope.nivelSemana(
    lunes: LocalDate,
    mensuales: List<MetaMensualEntity>,
    checks: Map<LocalDate, Set<String>>,
    ir: (Nivel) -> Unit,
    onPlanificador: (Int, LocalDate?) -> Unit,
    onKit: () -> Unit,
) {
    item {
        val total = (0..6).sumOf { checks[lunes.plusDays(it.toLong())]?.size ?: 0 }
        RutaCard {
            Text(stringResource(R.string.ruta_semana_n, lunes.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)),
                style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ruta_semana_resumen, total, ChecklistDiario.TOTAL * 7), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ProgressLine(total / (ChecklistDiario.TOTAL * 7f), Modifier.padding(top = 8.dp))
        }
    }
    items((0..6).map { lunes.plusDays(it.toLong()) }, key = { "d$it" }) { f ->
        val metasDia = mensuales.filter { it.anio == f.year && it.mes == f.monthValue }
        val cumplidas = metasDia.count { f.dayOfMonth in it.diasMarcados() }
        val habitos = checks[f]?.size ?: 0
        RutaCard(onClick = { ir(Nivel.Dia(f)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(f.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${f.dayOfMonth}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        color = if (f == hoy()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ruta_dia_metas, cumplidas, metasDia.size), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.checklist_resumen, habitos, ChecklistDiario.TOTAL), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onKit, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.LocalHospital, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.kit), maxLines = 1)
            }
            OutlinedButton(onClick = { onPlanificador(1, lunes) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.EditCalendar, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.metas_tab_dia), maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------- Nivel 5: Día

private fun androidx.compose.foundation.lazy.LazyListScope.nivelDia(
    fecha: LocalDate,
    perfil: PerfilEntity,
    mensuales: List<MetaMensualEntity>,
    checks: Map<LocalDate, Set<String>>,
    vm: RutaViewModel,
    onPlanificador: (Int, LocalDate?) -> Unit,
    onKit: () -> Unit,
    onPublicar: (String) -> Unit,
) {
    if (fecha == hoy()) item { RecordatorioDiaCard(perfil, onAjustes = {}, compacta = true) }
    item {
        val agenda by produceState<AgendaDiaEntity?>(null, fecha) { value = vm.agenda(fecha) }
        RutaCard {
            Text(
                fecha.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.titlecase() } +
                    " ${fecha.dayOfMonth} · " + nombreMes(fecha.monthValue) + " ${fecha.year}",
                style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
            )
            agenda?.let { a ->
                if (a.intencion.isNotBlank()) Text(stringResource(R.string.intencion_dia) + ": " + a.intencion, style = MaterialTheme.typography.bodyMedium)
                if (a.prioridad.isNotBlank()) Text(stringResource(R.string.prioridad_1_hoy) + ": " + a.prioridad, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onPlanificador(1, fecha) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.EditCalendar, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.abrir_planificador_dia))
            }
        }
    }
    val metas = mensuales.filter { it.anio == fecha.year && it.mes == fecha.monthValue }
    item { SectionTitle(stringResource(R.string.metas_mes_hoy)) }
    if (metas.isEmpty()) item {
        Text(stringResource(R.string.crear_primera_meta_mes), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(metas, key = { "md${it.id}" }) { m ->
        val hecho = fecha.dayOfMonth in m.diasMarcados()
        RutaCard(onClick = { vm.alternarDia(m, fecha.dayOfMonth) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hecho, onCheckedChange = { vm.alternarDia(m, fecha.dayOfMonth) })
                Column(Modifier.weight(1f)) {
                    Text(m.texto, style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.dias_de, m.diasMarcados().size, m.objetivoDias), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    item {
        val n = checks[fecha]?.size ?: 0
        RutaCard(onClick = onKit) {
            Text(stringResource(R.string.checklist_hoy), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.checklist_resumen, n, ChecklistDiario.TOTAL), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ProgressLine(n / ChecklistDiario.TOTAL.toFloat(), Modifier.padding(top = 6.dp))
        }
    }
    item {
        FilledTonalButton(onClick = { onPublicar("LOGRO") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.AddAPhoto, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.compartir_avance))
        }
    }
}
