package com.rutaalacima.app.ui.hoy

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.avance
import com.rutaalacima.app.domain.model.Comodines
import com.rutaalacima.app.domain.model.Constancia
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.EstadoMeta
import com.rutaalacima.app.domain.model.FraseLibro
import com.rutaalacima.app.domain.model.FrasesDelDia
import com.rutaalacima.app.domain.model.InvitacionFrase
import com.rutaalacima.app.domain.model.ResumenAnio
import com.rutaalacima.app.ui.Rutas
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.i18n.texto
import com.rutaalacima.app.ui.theme.asColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Color de los días que protegió un comodín (hielo), igual en la tarjeta y en la pantalla del año. */
val ColorHielo = Color(0xFF6FA8C9)

// ====================================================================== Frase del día → publicar

class InvitacionViewModel(private val c: AppContainer) : ViewModel() {
    private val hoy = LocalDate.now()
    val indice = FrasesDelDia.indice(hoy)
    val abierta: StateFlow<Boolean> = c.frases.desbloqueadas.map { indice in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), c.frases.desbloqueada(hoy))
    var frase by mutableStateOf<FraseLibro?>(null)
        private set

    /** Publicaciones de hoy vinculadas a una frase (para saber si ya respondiste). */
    val etiquetasDeHoy: StateFlow<Set<String>> = c.db.socialDao().observePublicaciones().map { ps ->
        ps.filter { Instant.ofEpochMilli(it.creadaEn).atZone(ZoneId.systemDefault()).toLocalDate() == hoy }.map { it.metaTitulo }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init { viewModelScope.launch { frase = runCatching { c.frases.delDia(hoy) }.getOrNull() } }
}

private fun preguntaRes(i: Int): Int = when (i) {
    0 -> R.string.invita_0; 1 -> R.string.invita_1; 2 -> R.string.invita_2; 3 -> R.string.invita_3
    4 -> R.string.invita_4; 5 -> R.string.invita_5; else -> R.string.invita_6
}

/**
 * Cada día la frase trae una pregunta distinta para responder con una reflexión o una foto
 * (como BeReal, pero sin prisa ni presión: se responde cuando quieras durante el día).
 */
@Composable
fun InvitacionFraseCard(onIrA: (String) -> Unit) {
    val vm = rutaViewModel { InvitacionViewModel(it) }
    val abierta by vm.abierta.collectAsStateWithLifecycle()
    val hechas by vm.etiquetasDeHoy.collectAsStateWithLifecycle()
    val etiqueta = InvitacionFrase.etiqueta(vm.indice, stringResource(R.string.frase_etiqueta))
    val pregunta = stringResource(preguntaRes(InvitacionFrase.pregunta(LocalDate.now())))
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FormatQuote, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.invita_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        when {
            !abierta -> {
                Text(stringResource(R.string.invita_sellada), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                OutlinedButton(onClick = { onIrA(Rutas.SELLO) }, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.invita_abrir)) }
            }
            etiqueta in hechas -> Text(stringResource(R.string.invita_hecha), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp).clickable { onIrA(Rutas.COMUNIDAD) })
            else -> {
                vm.frase?.let {
                    Text("“${it.t}”", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 8.dp))
                }
                Text(pregunta, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
                val texto = vm.frase?.let { "“${it.t}”\n\n" }.orEmpty()
                Button(
                    onClick = { onIrA(Rutas.publicar("REFLEXION", etiqueta, texto)) },
                    modifier = Modifier.padding(top = 10.dp),
                ) { Text(stringResource(R.string.invita_boton)) }
            }
        }
    }
}

// ====================================================================== Comodines

/** Fila con los comodines de racha: cuántos tienes y cuándo llega el siguiente. */
@Composable
fun FilaComodines(e: Comodines.Estado, conAyuda: Boolean) {
    Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(Comodines.MAX) { i ->
                Icon(Icons.Filled.AcUnit, null, Modifier.size(18.dp), tint = if (i < e.disponibles) ColorHielo else MaterialTheme.colorScheme.outlineVariant)
            }
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.comodines_n, e.disponibles), style = MaterialTheme.typography.labelLarge)
            if (e.proximoEn > 0) {
                Spacer(Modifier.width(8.dp))
                Text("· " + stringResource(R.string.comodines_proximo, e.proximoEn), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (conAyuda) Text(stringResource(R.string.comodines_ayuda), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Conteos del mapa con los días protegidos marcados como -1 (se pintan de hielo). */
fun conteosConComodines(checks: Map<LocalDate, Set<String>>, usados: Set<LocalDate>): Map<LocalDate, Int> =
    checks.mapValues { it.value.size } + usados.associateWith { -1 }

// ====================================================================== Resumen del año

class ResumenViewModel(private val c: AppContainer, val anio: Int) : ViewModel() {
    var datos by mutableStateOf<ResumenAnio.Datos?>(null)
        private set
    var mapa by mutableStateOf<List<List<Int?>>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            val hoy = LocalDate.now()
            val checks = c.checklist.desde(LocalDate.of(anio, 1, 1)).first()
            val d = c.db.sincroniaDao()
            val mes = d.metasMensuales().count { it.anio == anio && (it.cumplida || it.avance() >= 1f) }
            val anual = d.metasAnuales().count { it.anio == anio && EstadoMeta.from(it.estado) == EstadoMeta.CUMPLIDA }
            val recuerdos = c.db.socialDao().todas().count { it.anio == anio }
            val r = ResumenAnio.calcular(anio, checks, hoy, mes, anual, recuerdos, c.frases.abiertasDe(anio).size)
            val fin = minOf(hoy, LocalDate.of(anio, 12, 31))
            val usados = Comodines.calcular(checks.filterValues { it.isNotEmpty() }.filterKeys { it.year == anio }.keys, fin, LocalDate.of(anio, 1, 1)).usados
            mapa = Constancia.mapaDelAnio(anio, conteosConComodines(checks, usados), fin)
            datos = r
        }
    }
}

/** En Hoy, del 1 de diciembre al 15 de enero: el resumen del año ya está listo. */
@Composable
fun ResumenCard(onIrA: (String) -> Unit) {
    val anio = ResumenAnio.anioParaOfrecer(LocalDate.now()) ?: return
    Surface(
        onClick = { onIrA(Rutas.resumen(anio)) }, shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.resumen_titulo, anio), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.resumen_listo), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private data class Historia(val fondo: Color, val tinta: Color)

/**
 * El año contado como historias (como el resumen anual de Strava): una cifra grande por pantalla,
 * se avanza tocando a la derecha o deslizando, y al final se comparte.
 */
@Composable
fun ResumenAnioScreen(anio: Int, onBack: () -> Unit, onPublicar: (String, String) -> Unit) {
    val vm = rutaViewModel(key = "resumen-$anio") { ResumenViewModel(it, anio) }
    val d = vm.datos
    val esquema = MaterialTheme.colorScheme
    val historias = listOf(
        Historia(esquema.primary, esquema.onPrimary), Historia(esquema.secondaryContainer, esquema.onSecondaryContainer),
        Historia(esquema.surfaceContainerHigh, esquema.onSurface), Historia(esquema.primaryContainer, esquema.onPrimaryContainer),
    )
    val titulo = stringResource(R.string.resumen_titulo, anio)
    // Sin datos suficientes queda una sola historia con la invitación a volver
    val paginas = if (d?.suficiente == false) 1 else 8
    val pager = rememberPagerState { paginas }
    val alcance = rememberCoroutineScope()
    val ctx = LocalContext.current
    Box(Modifier.fillMaxSize().background(esquema.surface)) {
        if (d == null) return@Box
        HorizontalPager(pager, Modifier.fillMaxSize()) { p ->
            val h = historias[p % historias.size]
            Box(
                Modifier.fillMaxSize().background(h.fondo)
                    // Tocar a la derecha avanza y a la izquierda retrocede, como en las historias
                    .clickable(remember { MutableInteractionSource() }, null) {
                        alcance.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(paginas - 1)) }
                    }
                    .statusBarsPadding().navigationBarsPadding().padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(Modifier.widthIn(max = 520.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!d.suficiente && p == 0) {
                        Text(titulo, style = MaterialTheme.typography.headlineMedium, color = h.tinta, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.resumen_vacio), style = MaterialTheme.typography.bodyLarge, color = h.tinta, textAlign = TextAlign.Center)
                        return@Column
                    }
                    when (p) {
                        0 -> {
                            Image(painterResource(R.drawable.logo_sello), null, Modifier.size(120.dp))
                            Text(titulo, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = h.tinta, textAlign = TextAlign.Center)
                        }
                        1 -> {
                            Cifra("${d.diasConHabitos}", stringResource(R.string.resumen_dias), h.tinta)
                            if (vm.mapa.isNotEmpty()) {
                                val mitad = (vm.mapa.size + 1) / 2
                                Column(Modifier.clip(RoundedCornerShape(12.dp)).background(esquema.surface).padding(10.dp)) {
                                    MapaConstancia(vm.mapa.take(mitad), Modifier.fillMaxWidth().aspectRatio(mitad / 7f))
                                    Spacer(Modifier.height(6.dp))
                                    MapaConstancia(vm.mapa.drop(mitad), Modifier.fillMaxWidth().aspectRatio(mitad / 7f))
                                }
                            }
                        }
                        2 -> Cifra("${d.habitos}", stringResource(R.string.resumen_habitos), h.tinta)
                        3 -> {
                            Cifra("${d.mejorRacha}", stringResource(R.string.resumen_racha), h.tinta)
                            if (d.comodinesUsados > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AcUnit, null, tint = ColorHielo); Spacer(Modifier.width(6.dp))
                                Text("${d.comodinesUsados} " + stringResource(R.string.resumen_comodines), color = h.tinta)
                            }
                        }
                        4 -> {
                            val eje = Eje.fromCodigo(d.ejeFuerte)
                            if (eje != null) {
                                Box(Modifier.size(72.dp).clip(CircleShape).background(eje.color.asColor()))
                                Text(eje.texto(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = h.tinta)
                                Text(stringResource(R.string.resumen_eje), style = MaterialTheme.typography.titleMedium, color = h.tinta)
                            }
                            d.mejorMes?.let { m ->
                                val nombre = java.time.Month.of(m).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).replaceFirstChar { it.titlecase() }
                                Spacer(Modifier.height(12.dp))
                                Text("$nombre " + stringResource(R.string.resumen_mejor_mes), style = MaterialTheme.typography.titleLarge, color = h.tinta, textAlign = TextAlign.Center)
                            }
                        }
                        5 -> Cifra("${d.metasMesCumplidas + d.metasAnioCumplidas}", stringResource(R.string.resumen_metas), h.tinta)
                        6 -> {
                            Cifra("${d.recuerdos}", stringResource(R.string.resumen_recuerdos), h.tinta)
                            Spacer(Modifier.height(16.dp))
                            Cifra("${d.frasesAbiertas}", stringResource(R.string.resumen_frases), h.tinta)
                        }
                        else -> {
                            Image(painterResource(R.drawable.logo_sello), null, Modifier.size(96.dp))
                            Text(stringResource(R.string.resumen_cierre), style = MaterialTheme.typography.headlineSmall, color = h.tinta, textAlign = TextAlign.Center)
                            val resumen = stringResource(R.string.resumen_texto, anio, d.diasConHabitos, d.habitos, d.mejorRacha, d.metasMesCumplidas + d.metasAnioCumplidas)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "$resumen\n\n— Rutaalacima"), null))
                            }, colors = ButtonDefaults.buttonColors(containerColor = h.tinta, contentColor = h.fondo)) {
                                Icon(Icons.Outlined.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.resumen_compartir))
                            }
                            OutlinedButton(onClick = { onPublicar(titulo, resumen) }) { Text(stringResource(R.string.resumen_publicar), color = h.tinta) }
                        }
                    }
                }
            }
        }
        // Barras de avance de las historias y botón de cerrar
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(paginas) { i ->
                    Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i <= pager.currentPage) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.35f)))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.Close, stringResource(R.string.volver), tint = historias[pager.currentPage % historias.size].tinta) }
            }
        }
    }
}

@Composable
private fun Cifra(valor: String, etiqueta: String, tinta: Color) {
    Text(valor, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = tinta)
    Text(etiqueta, style = MaterialTheme.typography.titleLarge, color = tinta, textAlign = TextAlign.Center)
}
