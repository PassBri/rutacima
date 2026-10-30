package com.rutaalacima.app.ui.alto

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.ui.components.IconosRuta
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.fondoPapel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Alto en el camino (igual que la web): una vez al mes, 9 preguntas en tarjetas de papel, una por campamento y la novena
 * por tu cumbre; y cada noche, una sola pregunta. Las respuestas se guardan en el libro "alto" ("alto#<año-mes>#<n>" y
 * "noche#<fecha>"), las mismas claves que la web, para releerlas y ver cuánto has subido.
 */
object Alto {
    const val WB = "alto"
    val ORDEN = listOf("VOL", "MAE", "VOZ", "VAL", "EVO", "TRA", "CON", "CAM", "CUMBRE")
    private val IDIOMAS = setOf("es", "en", "pt", "fr", "de", "it", "zh", "ja", "ko", "ar", "hi", "ru")

    data class Banco(val intro: String, val cierre: String, val nombres: Map<String, String>, val preguntas: Map<String, List<String>>)
    data class Pregunta(val camp: String, val nombre: String, val texto: String)

    private var cache: Pair<String, Banco>? = null

    suspend fun banco(ctx: Context): Banco = withContext(Dispatchers.IO) {
        val idioma = Locale.getDefault().language.let { if (it in IDIOMAS) it else "es" }
        cache?.takeIf { it.first == idioma }?.second ?: run {
            val texto = runCatching { ctx.assets.open("preguntas/$idioma.json") }.getOrElse { ctx.assets.open("preguntas/es.json") }.bufferedReader().use { it.readText() }
            val o = Json.parseToJsonElement(texto).jsonObject
            fun mapa(k: String): JsonObject = o[k]!!.jsonObject
            Banco(
                o["intro"]!!.jsonPrimitive.content, o["cierre"]!!.jsonPrimitive.content,
                mapa("nombres").mapValues { it.value.jsonPrimitive.content },
                mapa("preguntas").mapValues { (_, v) -> v.jsonArray.map { it.jsonPrimitive.content } },
            ).also { cache = idioma to it }
        }
    }

    /** Las 9 del mes: una de cada campamento (cambian cada mes) y la de tu cumbre. */
    fun delMes(b: Banco, ym: YearMonth): List<Pregunta> {
        val k = ym.year * 12 + ym.monthValue
        return ORDEN.mapIndexed { i, c -> val xs = b.preguntas.getValue(c); Pregunta(c, b.nombres[c] ?: c, xs[(k + i) % xs.size]) }
    }

    /** La de la noche: recorre todo el banco, una distinta cada día (la misma que ve la web ese día). */
    fun deLaNoche(b: Banco, fecha: LocalDate): Pregunta {
        val todas = ORDEN.flatMap { c -> b.preguntas.getValue(c).map { Pregunta(c, b.nombres[c] ?: c, it) } }
        val n = todas.size
        return todas[(((fecha.toEpochDay() * 7) % n + n) % n).toInt()]
    }

    fun clave(ym: YearMonth, i: Int) = "alto#$ym#$i"
    fun claveNoche(f: LocalDate) = "noche#$f"
}

class AltoViewModel(private val c: AppContainer) : ViewModel() {
    val respuestas: StateFlow<Map<String, String>> = c.respuestas.observar(Alto.WB).stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    /** Lo escrito y aún no guardado (para que el campo no salte mientras se escribe). */
    val borrador = mutableStateMapOf<String, String>()
    private val guardados = mutableMapOf<String, Job>()
    fun texto(clave: String) = borrador[clave] ?: respuestas.value[clave].orEmpty()
    fun escribir(clave: String, v: String) {
        borrador[clave] = v.take(2000)
        guardados[clave]?.cancel()
        guardados[clave] = c.appScope.launch { delay(500); c.respuestas.guardar(Alto.WB, clave, borrador[clave].orEmpty()) }
    }
}

@Composable
private fun bancoAlto(): Alto.Banco? {
    val ctx = LocalContext.current
    val b by produceState<Alto.Banco?>(null) { value = Alto.banco(ctx) }
    return b
}

private val Serif = FontFamily.Serif

/** Pantalla del ritual: introducción, 9 tarjetas y el resumen de tus respuestas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AltoScreen(onBack: () -> Unit, mes: YearMonth = YearMonth.now()) {
    val vm = rutaViewModel { AltoViewModel(it) }
    val r by vm.respuestas.collectAsStateWithLifecycle()
    val b = bancoAlto()
    val ps = remember(b, mes) { b?.let { Alto.delMes(it, mes) }.orEmpty() }
    val respondidas = ps.indices.count { vm.texto(Alto.clave(mes, it)).isNotBlank() }
    val inicial = when { respondidas == 0 -> 0; respondidas >= 9 -> 10; else -> 1 + ps.indices.first { vm.texto(Alto.clave(mes, it)).isBlank() } }
    val pager = rememberPagerState(initialPage = inicial) { 11 }
    val alcance = rememberCoroutineScope()
    val nombreMes = mes.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, Locale.getDefault()).replaceFirstChar { it.titlecase() } + " ${mes.year}"
    Scaffold(
        modifier = Modifier.fondoPapel().imePadding(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.alto_titulo) + " · " + nombreMes, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.Close, stringResource(R.string.alto_cerrar)) } },
            )
        },
    ) { padding ->
        if (b == null) return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(pager, Modifier.weight(1f)) { p ->
                Surface(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                        when (p) {
                            0 -> {
                                Icon(IconosRuta.Cima, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(stringResource(R.string.alto_titulo), fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, textAlign = TextAlign.Center)
                                Text(b.intro, fontFamily = Serif, fontSize = 22.sp, lineHeight = 32.sp, textAlign = TextAlign.Center)
                                Text(stringResource(R.string.alto_n_preguntas, nombreMes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(onClick = { alcance.launch { pager.animateScrollToPage(1) } }) { Text(stringResource(R.string.alto_empezar)) }
                            }
                            10 -> {
                                Text(stringResource(R.string.alto_tu_alto, nombreMes), fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, textAlign = TextAlign.Center)
                                Text(b.cierre, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                ps.forEachIndexed { i, q ->
                                    val t = vm.texto(Alto.clave(mes, i))
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(q.nombre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        Text(q.texto, fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                        Text(t.ifBlank { stringResource(R.string.alto_sin_responder) }, style = MaterialTheme.typography.bodyMedium,
                                            color = if (t.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Button(onClick = onBack, Modifier.fillMaxWidth()) { Text(stringResource(R.string.alto_guardar)) }
                                OutlinedButton(onClick = { alcance.launch { pager.animateScrollToPage(1) } }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.alto_reiniciar)) }
                            }
                            else -> {
                                val i = p - 1; val q = ps[i]; val clave = Alto.clave(mes, i)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(stringResource(R.string.alto_pregunta, i + 1), fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                    Spacer(Modifier.size(6.dp))
                                    Text(stringResource(R.string.alto_de, ps.size), fontFamily = Serif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(q.nombre, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 4.dp))
                                Text(q.texto, fontFamily = Serif, fontSize = 23.sp, lineHeight = 33.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp))
                                OutlinedTextField(vm.texto(clave), { vm.escribir(clave, it) }, Modifier.fillMaxWidth().heightIn(min = 120.dp),
                                    placeholder = { Text(stringResource(R.string.alto_placeholder)) },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = Serif))
                            }
                        }
                    }
                }
            }
            // Abajo: anterior, los 9 puntos y siguiente
            if (pager.currentPage in 1..9) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { alcance.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, enabled = pager.currentPage > 1) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp)); Spacer(Modifier.size(4.dp)); Text(stringResource(R.string.alto_anterior))
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                    repeat(9) { k ->
                        val on = pager.currentPage == k + 1; val ok = vm.texto(Alto.clave(mes, k)).isNotBlank()
                        val e by animateFloatAsState(if (on) 1.3f else 1f, label = "punto")
                        Box(Modifier.padding(horizontal = 2.5.dp).size(7.dp).scale(e).clip(CircleShape).background(
                            if (on) MaterialTheme.colorScheme.primary else if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant))
                    }
                }
                Button(onClick = { alcance.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                    Text(stringResource(if (pager.currentPage == 9) R.string.alto_ver_respuestas else R.string.alto_siguiente))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

/** La invitación al alto del mes (en Hoy y en Mis frases). */
@Composable
fun AltoInvitacion(onAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rutaViewModel { AltoViewModel(it) }
    val r by vm.respuestas.collectAsStateWithLifecycle()
    val mes = YearMonth.now()
    val n = (0 until 9).count { r[Alto.clave(mes, it)].orEmpty().isNotBlank() }
    val nombreMes = mes.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, Locale.getDefault())
    Surface(onClick = onAbrir, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(IconosRuta.Cima, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.alto_titulo) + " · " + nombreMes, fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(when { n == 0 -> stringResource(R.string.alto_inv_d0); n >= 9 -> stringResource(R.string.alto_inv_lleno); else -> stringResource(R.string.alto_inv_dn, n) },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** La pregunta de la noche, con su respuesta privada (la misma que en la web ese día). */
@Composable
fun PreguntaNocheCard(modifier: Modifier = Modifier) {
    val vm = rutaViewModel { AltoViewModel(it) }
    val r by vm.respuestas.collectAsStateWithLifecycle()
    val b = bancoAlto() ?: return
    val hoy = LocalDate.now(); val q = remember(b, hoy) { Alto.deLaNoche(b, hoy) }; val clave = Alto.claveNoche(hoy)
    r.size // recompone al llegar lo guardado
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FormatQuote, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.noche_titulo) + " · " + q.nombre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Text(q.texto, fontFamily = Serif, fontSize = 19.sp, lineHeight = 27.sp)
            OutlinedTextField(vm.texto(clave), { vm.escribir(clave, it) }, Modifier.fillMaxWidth(), minLines = 2,
                placeholder = { Text(stringResource(R.string.noche_placeholder)) }, textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = Serif))
        }
    }
}
