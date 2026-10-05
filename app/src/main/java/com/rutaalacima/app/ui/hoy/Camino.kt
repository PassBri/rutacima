package com.rutaalacima.app.ui.hoy

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.ChecklistDiario
import com.rutaalacima.app.domain.model.Constancia
import com.rutaalacima.app.domain.model.PrimerosPasos
import com.rutaalacima.app.domain.model.PrimerosPasos.Paso
import com.rutaalacima.app.domain.model.Regreso
import com.rutaalacima.app.domain.model.Revision
import com.rutaalacima.app.ui.Rutas
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.i18n.Textos
import com.rutaalacima.app.ui.metas.NivelMeta
import com.rutaalacima.app.ui.theme.fondoPapel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Dónde vive el "acompañamiento" del camino: primeros pasos, regreso, revisiones y constancia. */
class CaminoViewModel(private val c: AppContainer) : ViewModel() {
    private val hoy = LocalDate.now()
    private val prefs = c.contexto.getSharedPreferences("camino", Context.MODE_PRIVATE)

    /** Días con hábitos desde hace algo más de un año (para la racha y el mapa). */
    val checks: StateFlow<Map<LocalDate, Set<String>>> = c.checklist.desde(hoy.minusDays(400))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val actividad = combine(
        c.ejes.ultima, c.planificador.propositos, c.planAnual.metasMes(hoy.year, hoy.monthValue),
        c.respuestas.conteos, c.vision.casillas,
    ) { ejes, propositos, metasMes, conteos, vision ->
        listOf(ejes != null, propositos.isNotEmpty(), metasMes.isNotEmpty(), conteos.any { it.key != REVISION && it.value > 0 }, vision.isNotEmpty())
    }

    val pasos: StateFlow<PrimerosPasos.Estado?> = combine(
        c.perfil.perfil, actividad, checks, c.db.socialDao().observePublicaciones(),
    ) { perfil, a, checks, posts ->
        PrimerosPasos.Estado(
            cumbre = perfil.cumbreFrase.isNotBlank(), nacimiento = perfil.anioNacimiento != null,
            ejes = a[0], proposito = a[1], metaMes = a[2], habitos = checks.values.any { it.isNotEmpty() },
            guia = a[3], vision = a[4], comunidad = posts.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Revisiones ya hechas ("2026-W40", "2026-10"…). */
    val revisionesHechas: StateFlow<Set<String>> = c.respuestas.observar(REVISION)
        .map { m -> m.keys.filter { it.endsWith("#hecha") }.map { it.removePrefix("rev-").removeSuffix("#hecha") }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    var pasosCerrados by mutableStateOf(prefs.getBoolean("pasos_cerrados", false))
        private set
    var diasFuera by mutableStateOf(prefs.getInt("regreso_dias", 0))
        private set

    fun cerrarPasos() { pasosCerrados = true; prefs.edit().putBoolean("pasos_cerrados", true).apply() }
    fun cerrarRegreso() { diasFuera = 0; prefs.edit().putInt("regreso_dias", 0).apply() }

    // ---------- Revisión

    fun respuestas(clave: String) = c.respuestas.observar(REVISION).map { m ->
        (1..3).map { m["rev-$clave#q$it"].orEmpty() }
    }

    fun guardar(clave: String, n: Int, texto: String) = c.appScope.launch { c.respuestas.guardar(REVISION, "rev-$clave#q$n", texto) }
    fun terminar(clave: String) = c.appScope.launch { c.respuestas.guardar(REVISION, "rev-$clave#hecha", LocalDate.now().toString()) }

    companion object {
        const val REVISION = "revision"

        /** Se llama al abrir la app: si estuvo 5 días o más fuera, recibe la bienvenida de regreso. */
        fun registrarApertura(ctx: Context) {
            val p = ctx.getSharedPreferences("camino", Context.MODE_PRIVATE)
            val hoy = LocalDate.now()
            val ultima = p.getString("ultima_apertura", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val e = p.edit().putString("ultima_apertura", hoy.toString())
            if (Regreso.debeRecibir(ultima, hoy)) e.putInt("regreso_dias", Regreso.diasFuera(ultima, hoy))
            e.apply()
        }
    }
}

// ====================================================================== Tarjetas para Hoy

private data class InfoPaso(val titulo: Int, val texto: Int, val boton: Int, val ruta: String)

private fun info(p: Paso) = when (p) {
    Paso.CUMBRE -> InfoPaso(R.string.paso_cumbre, R.string.paso_cumbre_texto, R.string.paso_cumbre_boton, Rutas.AJUSTES)
    Paso.NACIMIENTO -> InfoPaso(R.string.paso_nacimiento, R.string.paso_nacimiento_texto, R.string.paso_nacimiento_boton, Rutas.AJUSTES)
    Paso.EJES -> InfoPaso(R.string.paso_ejes, R.string.paso_ejes_texto, R.string.paso_ejes_boton, Rutas.EJES)
    Paso.PROPOSITO -> InfoPaso(R.string.paso_proposito, R.string.paso_proposito_texto, R.string.paso_proposito_boton, Rutas.nuevaMeta(NivelMeta.CINCO_ANIOS))
    Paso.META_MES -> InfoPaso(R.string.paso_meta_mes, R.string.paso_meta_mes_texto, R.string.paso_meta_mes_boton, Rutas.nuevaMeta(NivelMeta.MES))
    Paso.HABITOS -> InfoPaso(R.string.paso_habitos, R.string.paso_habitos_texto, R.string.paso_habitos_boton, Rutas.KIT)
    Paso.GUIA -> InfoPaso(R.string.paso_guia, R.string.paso_guia_texto, R.string.paso_guia_boton, Rutas.workbook("descubre"))
    Paso.VISION -> InfoPaso(R.string.paso_vision, R.string.paso_vision_texto, R.string.paso_vision_boton, Rutas.VISION)
    Paso.COMUNIDAD -> InfoPaso(R.string.paso_comunidad, R.string.paso_comunidad_texto, R.string.paso_comunidad_boton, Rutas.publicar("META"))
}

/** "Tu paso de hoy": el siguiente paso pendiente del comienzo, uno solo, con su botón. */
@Composable
fun PrimerPasoCard(onIrA: (String) -> Unit) {
    val vm = rutaViewModel { CaminoViewModel(it) }
    val estado by vm.pasos.collectAsStateWithLifecycle()
    val e = estado ?: return
    val paso = PrimerosPasos.siguiente(e)
    if (paso == null || vm.pasosCerrados) return
    val i = info(paso)
    val hechos = PrimerosPasos.hechos(e)
    val total = Paso.entries.size
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Flag, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.paso_de_hoy, hechos + 1, total), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
            IconButton(onClick = vm::cerrarPasos) { Icon(Icons.Filled.Close, stringResource(R.string.paso_ocultar)) }
        }
        Text(stringResource(i.titulo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(i.texto), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        ProgressLine(hechos / total.toFloat())
        Spacer(Modifier.height(10.dp))
        Button(onClick = { onIrA(i.ruta) }) { Text(stringResource(i.boton)) }
    }
}

/** Regreso sin culpa: después de 5 días o más sin entrar. */
@Composable
fun RegresoCard(onIrA: (String) -> Unit, onSeccion: (String, Int) -> Unit) {
    val vm = rutaViewModel { CaminoViewModel(it) }
    if (vm.diasFuera <= 0) return
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WbTwilight, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.regreso_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = vm::cerrarRegreso) { Icon(Icons.Filled.Close, stringResource(R.string.paso_ocultar)) }
        }
        Text(stringResource(R.string.regreso_texto, vm.diasFuera), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.cerrarRegreso(); onIrA(Rutas.KIT) }) { Text(stringResource(R.string.regreso_un_habito)) }
            OutlinedButton(onClick = { vm.cerrarRegreso(); onIrA(Rutas.workbook("bono_anti_abandono")) }) { Text(stringResource(R.string.regreso_guia)) }
        }
    }
}

/** La revisión de la semana (domingo y lunes) o del mes (los últimos y primeros días). */
@Composable
fun RevisionCard(onIrA: (String) -> Unit) {
    val vm = rutaViewModel { CaminoViewModel(it) }
    val hechas by vm.revisionesHechas.collectAsStateWithLifecycle()
    val (tipo, dia) = Revision.pendiente(LocalDate.now(), hechas) ?: return
    RutaCard(onClick = { onIrA(Rutas.revision(tipo, dia)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EventRepeat, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (tipo == Revision.Tipo.MES) R.string.revision_mes else R.string.revision_semana), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.revision_invitacion), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Racha de días con hábitos y las últimas semanas en un mapa (toca para ver el año). */
@Composable
fun ConstanciaCard(onIrA: (String) -> Unit) {
    val vm = rutaViewModel { CaminoViewModel(it) }
    val checks by vm.checks.collectAsStateWithLifecycle()
    val hoy = LocalDate.now()
    val dias = checks.filterValues { it.isNotEmpty() }.keys
    val racha = Constancia.racha(dias, hoy)
    val mejor = Constancia.mejorRacha(dias)
    val semanas = Constancia.mapaDelAnio(hoy.year, checks.mapValues { it.value.size }, hoy)
    val hastaHoy = semanas.indexOfLast { s -> s.any { it != null } }
    val ultimas = semanas.subList((hastaHoy - 17).coerceAtLeast(0), hastaHoy + 1)
    RutaCard(onClick = { onIrA(Rutas.CONSTANCIA) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = if (racha > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(if (racha > 0) stringResource(R.string.racha_dias, racha) else stringResource(R.string.racha_cero), style = MaterialTheme.typography.titleMedium)
                if (mejor > 0) Text(stringResource(R.string.racha_mejor, mejor), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        MapaConstancia(ultimas, Modifier.fillMaxWidth().aspectRatio(ultimas.size.coerceAtLeast(1) / 7f))
    }
}

/** Cuadrícula semanas × días; el color crece con los hábitos marcados ese día. */
@Composable
fun MapaConstancia(semanas: List<List<Int?>>, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val oro = MaterialTheme.colorScheme.secondary
    val descripcion = stringResource(R.string.constancia_mapa_desc)
    Canvas(modifier.semantics { contentDescription = descripcion }) {
        if (semanas.isEmpty()) return@Canvas
        val celda = minOf(size.width / semanas.size, size.height / 7f)
        val hueco = celda * 0.16f
        semanas.forEachIndexed { x, semana ->
            semana.forEachIndexed { y, v ->
                if (v == null) return@forEachIndexed
                val nivel = Constancia.nivel(v)
                val color = if (nivel == 0) base else oro.copy(alpha = 0.25f + 0.1875f * nivel)
                drawRoundRect(color, Offset(x * celda + hueco / 2, y * celda + hueco / 2), Size(celda - hueco, celda - hueco), CornerRadius(celda * 0.22f))
            }
        }
    }
}

// ====================================================================== Pantallas

/** El año entero: racha, mejor racha y el mapa de días con hábitos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstanciaScreen(onBack: () -> Unit) {
    val vm = rutaViewModel { CaminoViewModel(it) }
    val checks by vm.checks.collectAsStateWithLifecycle()
    val hoy = LocalDate.now()
    val dias = checks.filterValues { it.isNotEmpty() }.keys
    val anio = checks.filterKeys { it.year == hoy.year }
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.constancia_titulo)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dato(stringResource(R.string.constancia_racha), "${Constancia.racha(dias, hoy)}", Modifier.weight(1f))
                    Dato(stringResource(R.string.constancia_mejor), "${Constancia.mejorRacha(dias)}", Modifier.weight(1f))
                    Dato(stringResource(R.string.constancia_dias_anio), "${anio.count { it.value.isNotEmpty() }}", Modifier.weight(1f))
                }
            }
            item {
                RutaCard {
                    Text("${hoy.year}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    // El año partido en dos filas de medio año para que se lea bien en el teléfono
                    val semanas = Constancia.mapaDelAnio(hoy.year, checks.mapValues { it.value.size }, hoy)
                    val mitad = (semanas.size + 1) / 2
                    MapaConstancia(semanas.take(mitad), Modifier.fillMaxWidth().aspectRatio(mitad / 7f))
                    Spacer(Modifier.height(8.dp))
                    MapaConstancia(semanas.drop(mitad), Modifier.fillMaxWidth().aspectRatio(mitad / 7f))
                    Text(stringResource(R.string.constancia_leyenda), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            }
            // Los hábitos más constantes del año
            val conteo = anio.values.flatten().groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5)
            if (conteo.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.constancia_habitos_fuertes)) }
                conteo.forEach { (id, n) ->
                    item(key = id) {
                        RutaCard {
                            Text(stringResource(Textos.habito(id)), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.constancia_veces, n), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dato(titulo: String, valor: String, modifier: Modifier = Modifier) {
    RutaCard(modifier) {
        Text(valor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Revisión semanal o mensual: resumen automático, tres preguntas y compartir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionScreen(tipo: Revision.Tipo, dia: LocalDate, onBack: () -> Unit) {
    val vm = rutaViewModel { CaminoViewModel(it) }
    val checks by vm.checks.collectAsStateWithLifecycle()
    val clave = Revision.clave(tipo, dia)
    val guardadas by remember(clave) { vm.respuestas(clave) }.collectAsStateWithLifecycle(initialValue = listOf("", "", ""))
    val r = Revision.resumen(tipo, dia, checks, LocalDate.now())
    val (desde, hasta) = Revision.periodo(tipo, dia)
    val formato = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val ctx = LocalContext.current
    val preguntas = listOf(R.string.revision_p1, R.string.revision_p2, R.string.revision_p3)
    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text(stringResource(if (tipo == Revision.Tipo.MES) R.string.revision_mes else R.string.revision_semana))
                        Text("${desde.format(formato)} – ${hasta.format(formato)}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RutaCard {
                    Text(stringResource(R.string.revision_resumen), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.revision_dias_habitos, r.diasConHabitos, r.dias), style = MaterialTheme.typography.bodyLarge)
                    ProgressLine(if (r.dias == 0) 0f else r.diasConHabitos / r.dias.toFloat(), Modifier.padding(vertical = 6.dp))
                    Text(stringResource(R.string.revision_promedio, r.promedio, ChecklistDiario.TOTAL), style = MaterialTheme.typography.bodyMedium)
                    r.mejorDia?.let { Text(stringResource(R.string.revision_mejor_dia, it.format(formato), r.mejorDiaHabitos), style = MaterialTheme.typography.bodyMedium) }
                    r.habitoFuerte?.let { (id, n) ->
                        Text(stringResource(R.string.revision_habito_fuerte, stringResource(Textos.habito(id)), n), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            preguntas.forEachIndexed { i, p ->
                item(key = "p$i") {
                    // Lo escrito manda; lo guardado solo llena el campo al abrir
                    var texto by remember(clave) { mutableStateOf<String?>(null) }
                    OutlinedTextField(
                        texto ?: guardadas[i], { nuevo -> texto = nuevo.take(1500); vm.guardar(clave, i + 1, nuevo.take(1500)) },
                        label = { Text(stringResource(p)) }, modifier = Modifier.fillMaxWidth(), minLines = 3,
                    )
                }
            }
            item {
                val txtCompartir = stringResource(R.string.revision_compartir_texto, r.diasConHabitos, r.dias)
                val titulo = stringResource(if (tipo == Revision.Tipo.MES) R.string.revision_mes else R.string.revision_semana)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.terminar(clave); onBack() }) { Text(stringResource(R.string.revision_terminar)) }
                    TextButton(onClick = {
                        val texto = listOf(titulo, txtCompartir, guardadas[0], "— Rutaalacima").filter { it.isNotBlank() }.joinToString("\n\n")
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), null))
                    }) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.revision_compartir))
                    }
                }
            }
        }
    }
}
