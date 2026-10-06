package com.rutaalacima.app.ui.metas

import com.rutaalacima.app.ui.theme.fondoPapel
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.bancos.Bancos
import com.rutaalacima.app.data.bancos.MetaPlantilla
import com.rutaalacima.app.data.bancos.ProyectoConfluencia
import com.rutaalacima.app.data.local.AccionEntity
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.PropositoEntity
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.Prioridad
import com.rutaalacima.app.domain.model.Vida
import com.rutaalacima.app.ui.components.ChipSelector
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SelectorHorizonte
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.i18n.texto
import com.rutaalacima.app.ui.theme.asColor
import com.rutaalacima.app.util.hoy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Nivel de la cascada en el que se crea la meta. */
enum class NivelMeta(@StringRes val nombre: Int, @StringRes val corto: Int, @StringRes val ayuda: Int, val icono: ImageVector) {
    CINCO_ANIOS(R.string.nivel_5_anios_largo, R.string.nivel_5_anios, R.string.nivel_5_anios_ayuda, Icons.Filled.Terrain),
    ANIO(R.string.nivel_anio_largo, R.string.nivel_anio, R.string.nivel_anio_ayuda, Icons.Filled.EmojiEvents),
    MES(R.string.nivel_mes_largo, R.string.nivel_mes, R.string.nivel_mes_ayuda, Icons.Filled.CalendarMonth),
}

class NuevaMetaViewModel(private val c: AppContainer, inicial: NivelMeta) : ViewModel() {
    var paso by mutableStateOf(0)
    var nivel by mutableStateOf(inicial)
    var titulo by mutableStateOf("")
    var eje by mutableStateOf<Eje?>(null)
    var prioridad by mutableStateOf(Prioridad.B)
    var indicador by mutableStateOf("")
    var observable by mutableStateOf("")
    var padreId by mutableStateOf<Long?>(null)
    var objetivoDias by mutableStateOf(20)
    /** Horizonte del propósito a largo plazo: los años que elija la persona (Vida.HORIZONTE_MIN a HORIZONTE_MAX). */
    var horizonte by mutableStateOf(5)
    val acciones = mutableStateListOf<String>()

    var bancos by mutableStateOf<Bancos?>(null)
        private set
    var propositos by mutableStateOf<List<PropositoEntity>>(emptyList())
        private set
    var metasAnio by mutableStateOf<List<MetaAnualEntity>>(emptyList())
        private set

    var iaCargando by mutableStateOf(false)
        private set
    var iaSugerencia by mutableStateOf<String?>(null)
        private set
    val conIA: Boolean get() = c.coach.conIA
    var guardada by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            bancos = c.bancos.bancos()
            propositos = c.planificador.propositos.first()
            metasAnio = c.planificador.metas(hoy().year).first()
        }
    }

    fun usarPlantilla(m: MetaPlantilla) {
        titulo = m.meta
        observable = listOf(m.observable, m.plazo).filter { it.isNotBlank() }.joinToString(" · ")
        paso = 2
    }

    fun usarProyecto(p: ProyectoConfluencia) {
        titulo = p.nombre
        observable = p.descripcion
        eje = p.ejes.keys.firstOrNull()?.let { Eje.fromCodigo(it) }
        acciones.clear()
        acciones.addAll(p.ejes.map { (cod, txt) -> "${Eje.fromCodigo(cod)?.codigo ?: cod}: $txt" })
        paso = 2
    }

    /** Pide a la IA que convierta la meta en ORSE (observable, relevante, específica, con evidencia). */
    fun mejorarConIA(idioma: String) {
        if (titulo.isBlank()) return
        iaCargando = true
        viewModelScope.launch {
            iaSugerencia = c.coach.consultar(
                "Reescribe esta meta en formato ORSE para el nivel \"${nivel.name}\" de la cascada " +
                    "(máximo 25 palabras, solo la meta, sin explicación). Responde en el idioma \"$idioma\". Meta: $titulo",
            )?.trim()?.trim('"')
            iaCargando = false
        }
    }

    fun aceptarSugerencia() {
        iaSugerencia?.let { titulo = it }
        iaSugerencia = null
    }

    fun guardar() {
        if (titulo.isBlank()) return
        viewModelScope.launch {
            val ahora = hoy()
            when (nivel) {
                NivelMeta.CINCO_ANIOS -> {
                    val id = c.planificador.guardarProposito(
                        PropositoEntity(
                            titulo = titulo.trim(), prioridad = prioridad.name, eje = eje?.codigo,
                            indicadorExito = indicador.trim(), descripcion = observable.trim(),
                            horizonte = horizonte,
                        ),
                    )
                    acciones.forEachIndexed { i, a -> c.planificador.guardarAccion(AccionEntity(propositoId = id, orden = i + 1, texto = a)) }
                }
                NivelMeta.ANIO -> c.planificador.guardarMeta(
                    MetaAnualEntity(
                        anio = ahora.year, propositoId = padreId, titulo = titulo.trim(), subMetas = acciones.joinToString("\n"),
                        prioridad = prioridad.name, eje = eje?.codigo, indicador = indicador.trim(), observable = observable.trim(),
                    ),
                )
                NivelMeta.MES -> c.planAnual.guardarMetaMes(
                    MetaMensualEntity(
                        anio = ahora.year, mes = ahora.monthValue, texto = titulo.trim(), metaAnualId = padreId,
                        eje = eje?.codigo, indicador = indicador.trim(), objetivoDias = objetivoDias,
                    ),
                )
            }
            guardada = true
        }
    }
}

/** Asistente para crear metas en la cascada, con los bancos de Ruta a la Cima y la IA. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaMetaScreen(nivelInicial: NivelMeta, onBack: () -> Unit, onCompartir: () -> Unit) {
    val vm = rutaViewModel(key = "nueva-${nivelInicial.name}") { NuevaMetaViewModel(it, nivelInicial) }
    val pasos = listOf(R.string.paso_nivel, R.string.paso_inspiracion, R.string.paso_detalles, R.string.paso_plan)
    val ultimo = if (vm.nivel == NivelMeta.MES) 2 else 3

    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                title = { Text(if (vm.guardada) stringResource(R.string.meta_creada) else stringResource(pasos[vm.paso.coerceAtMost(3)])) },
                navigationIcon = {
                    IconButton(onClick = { if (vm.paso > 0 && !vm.guardada) vm.paso-- else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver))
                    }
                },
            )
        },
        bottomBar = {
            if (!vm.guardada) {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                        LinearProgressIndicator(progress = { (vm.paso + 1f) / (ultimo + 1) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.paso_de, vm.paso + 1, ultimo + 1), style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f))
                            if (vm.paso == 1) TextButton(onClick = { vm.paso = 2 }) { Text(stringResource(R.string.escribir_la_mia)) }
                            if (vm.paso < ultimo) {
                                Button(onClick = { vm.paso++ }, enabled = vm.paso != 2 || vm.titulo.isNotBlank()) { Text(stringResource(R.string.siguiente)) }
                            } else {
                                Button(onClick = vm::guardar, enabled = vm.titulo.isNotBlank()) { Text(stringResource(R.string.crear_meta)) }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        AnimatedContent(targetState = if (vm.guardada) -1 else vm.paso, label = "paso") { paso ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (paso) {
                    -1 -> listo(vm, onBack, onCompartir)
                    0 -> pasoNivel(vm)
                    1 -> pasoInspiracion(vm)
                    2 -> pasoDetalles(vm)
                    else -> pasoPlan(vm)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.pasoNivel(vm: NuevaMetaViewModel) {
    item {
        Text(stringResource(R.string.que_nivel), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.que_nivel_ayuda), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(NivelMeta.entries) { n ->
        RutaCard(onClick = { vm.nivel = n; vm.paso = 1 }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(n.icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(n.nombre), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(n.ayuda), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (vm.nivel == n) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.pasoInspiracion(vm: NuevaMetaViewModel) {
    val b = vm.bancos ?: return
    item {
        Text(stringResource(R.string.inspiracion_titulo), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.inspiracion_ayuda), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    item {
        ChipSelector(stringResource(R.string.eje_que_quieres_activar), listOf<Eje?>(null) + Eje.entries, vm.eje,
            { it?.let { e -> e.codigo } ?: "—" }, { vm.eje = it }, color = { it?.color?.asColor() ?: androidx.compose.ui.graphics.Color.Gray })
    }
    if (vm.nivel != NivelMeta.MES) {
        item { SectionTitle(stringResource(R.string.proyectos_confluencia)) }
        items(b.proyectos.filter { p -> vm.eje == null || vm.eje!!.codigo in p.ejes }.take(5)) { p ->
            RutaCard(onClick = { vm.usarProyecto(p) }) {
                Text(p.nombre, style = MaterialTheme.typography.titleSmall)
                Text(p.descripcion, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.activa_n_ejes, p.ejes.size) + " · " + p.ejes.keys.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
    item { SectionTitle(stringResource(R.string.banco_metas)) }
    val areas = b.areasSugeridas(vm.eje?.codigo)
    items(b.metas.filter { it.area in areas }.take(14)) { m ->
        RutaCard(onClick = { vm.usarPlantilla(m) }) {
            Text(m.area.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(m.meta, style = MaterialTheme.typography.titleSmall)
            if (m.observable.isNotBlank()) Text("✓ ${m.observable}", style = MaterialTheme.typography.bodySmall)
            if (m.plazo.isNotBlank()) Text("⏱ ${m.plazo}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.pasoDetalles(vm: NuevaMetaViewModel) {
    item {
        Text(stringResource(vm.nivel.nombre), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        OutlinedTextField(vm.titulo, { vm.titulo = it }, label = { Text(stringResource(R.string.tu_meta)) },
            modifier = Modifier.fillMaxWidth(), minLines = 2, textStyle = MaterialTheme.typography.titleMedium)
        val idioma = java.util.Locale.getDefault().language
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.mejorarConIA(idioma) }, enabled = vm.titulo.isNotBlank() && vm.conIA && !vm.iaCargando) {
                Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.mejorar_con_ia))
            }
            if (vm.iaCargando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        if (!vm.conIA) {
            Text(stringResource(R.string.orse_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        vm.iaSugerencia?.let { s ->
            RutaCard {
                Text(stringResource(R.string.sugerencia_ia), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(s, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = vm::aceptarSugerencia) { Text(stringResource(R.string.usar_sugerencia)) }
            }
        }
    }
    item {
        ChipSelector(stringResource(R.string.eje), Eje.entries, vm.eje, { it.texto() }, { vm.eje = it }, color = { it.color.asColor() })
    }
    if (vm.nivel != NivelMeta.MES) {
        item {
            ChipSelector(stringResource(R.string.prioridad), Prioridad.entries, vm.prioridad, { "${it.clave} · ${it.texto()}" },
                { vm.prioridad = it }, color = { it.color.asColor() })
        }
    }
    item {
        var abrirBanco by remember { mutableStateOf(false) }
        OutlinedTextField(vm.indicador, { vm.indicador = it }, label = { Text(stringResource(R.string.indicador_exito)) },
            modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { abrirBanco = true }) {
            Icon(Icons.Filled.Insights, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.elegir_banco_indicadores))
        }
        if (abrirBanco) {
            ModalBottomSheet(onDismissRequest = { abrirBanco = false }) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { Text(stringResource(R.string.banco_indicadores), style = MaterialTheme.typography.titleLarge) }
                    items(vm.bancos?.indicadoresDe(vm.eje?.codigo).orEmpty()) { ind ->
                        RutaCard(onClick = { vm.indicador = ind.texto; abrirBanco = false }) {
                            Text(ind.eje, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Text(ind.texto, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
    if (vm.nivel != NivelMeta.MES) {
        item {
            OutlinedTextField(vm.observable, { vm.observable = it }, label = { Text(stringResource(R.string.evidencia_observable)) },
                modifier = Modifier.fillMaxWidth(), minLines = 2)
        }
    }
    when (vm.nivel) {
        NivelMeta.CINCO_ANIOS -> item {
            SelectorHorizonte(vm.horizonte, { vm.horizonte = it })
            Text(stringResource(R.string.horizonte_ayuda, hoy().year + vm.horizonte), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        NivelMeta.ANIO -> if (vm.propositos.isNotEmpty()) item {
            ChipSelector(stringResource(R.string.conectar_con_proposito), listOf<PropositoEntity?>(null) + vm.propositos,
                vm.propositos.firstOrNull { it.id == vm.padreId }, { it?.let { p -> "${p.horizonte}a · " + p.titulo.take(30) } ?: stringResource(R.string.ninguno) },
                { vm.padreId = it?.id })
        }
        NivelMeta.MES -> {
            if (vm.metasAnio.isNotEmpty()) item {
                ChipSelector(stringResource(R.string.conectar_con_meta_anual), listOf<MetaAnualEntity?>(null) + vm.metasAnio,
                    vm.metasAnio.firstOrNull { it.id == vm.padreId }, { it?.titulo?.take(30) ?: stringResource(R.string.ninguno) },
                    { vm.padreId = it?.id })
            }
            item {
                Text(stringResource(R.string.dias_objetivo, vm.objetivoDias), style = MaterialTheme.typography.labelLarge)
                Slider(value = vm.objetivoDias.toFloat(), onValueChange = { vm.objetivoDias = kotlin.math.round(it).toInt() }, valueRange = 1f..31f, steps = 29)
                Text(stringResource(R.string.dias_objetivo_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.pasoPlan(vm: NuevaMetaViewModel) {
    val b = vm.bancos ?: return
    item {
        Text(stringResource(R.string.plan_titulo), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.plan_ayuda), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    item {
        var nueva by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(nueva, { nueva = it }, label = { Text(stringResource(R.string.nueva_accion)) }, modifier = Modifier.weight(1f), singleLine = true)
            TextButton(onClick = { if (nueva.isNotBlank()) { vm.acciones.add(nueva.trim()); nueva = "" } }) { Text(stringResource(R.string.agregar)) }
        }
    }
    items(vm.acciones.toList()) { a ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = true, onCheckedChange = { vm.acciones.remove(a) })
            Text(a, style = MaterialTheme.typography.bodyLarge)
        }
    }
    item { SectionTitle(stringResource(R.string.banco_acciones)) }
    items(b.accionesDe(vm.eje?.codigo).filter { it.texto !in vm.acciones }.take(15)) { a ->
        RutaCard(onClick = { vm.acciones.add(a.texto) }) {
            Text(a.ejes.joinToString(" + "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(a.texto, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.listo(vm: NuevaMetaViewModel, onBack: () -> Unit, onCompartir: () -> Unit) {
    item {
        Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.meta_creada), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(vm.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            Text(stringResource(R.string.meta_creada_texto), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onCompartir, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.compartir_en_comunidad)) }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.listo)) }
        }
    }
}

