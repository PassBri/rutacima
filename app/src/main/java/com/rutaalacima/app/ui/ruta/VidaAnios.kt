package com.rutaalacima.app.ui.ruta

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.data.social.TipoPost
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.vision.colorCampamento
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDate

/*
 * Mi vida como registro de toda la vida (igual que en la web): cada año guarda su palabra, sus anhelos y
 * sueños, sus metas, lo cumplido, sus recuerdos y una carta a ese yo, sellada con lacre si el año aún no llega.
 * Se guarda como respuestas del workbook "vida" ("vida#<año>#palabra|anhelos|carta") y se sincroniza.
 */
const val WB_VIDA = "vida"
fun claveVida(anio: Int, k: String) = "vida#$anio#$k"

private fun modelo(f: String): Any = if (f.startsWith("http")) f else File(f)

/** La línea de vida: una cinta de años de punta a punta, centrada en el que se mira. */
@Composable
fun LineaVida(
    nacimiento: Int, meta: Int, actual: Int, metas: List<MetaAnualEntity>, posts: List<Post>, vida: Map<String, String>, onAnio: (Int) -> Unit,
) {
    val hoy = LocalDate.now().year
    val anios = (nacimiento..nacimiento + meta).toList()
    val estado = rememberLazyListState()
    LaunchedEffect(actual) { estado.scrollToItem((anios.indexOf(actual) - 2).coerceAtLeast(0)) }
    val fotos = remember(posts) { posts.filter { it.foto.isNotBlank() }.groupBy { it.anio }.mapValues { it.value.first().foto } }
    LazyRow(state = estado, horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
        items(anios.size, key = { anios[it] }) { i ->
            val a = anios[i]; val edad = a - nacimiento
            val ejes = metas.filter { it.anio == a }.map { it.eje ?: "CON" }.distinct().take(4)
            val palabra = vida[claveVida(a, "palabra")].orEmpty()
            val sellada = a > hoy && !vida[claveVida(a, "carta")].isNullOrBlank()
            val foto = fotos[a]
            val esActual = a == actual
            val fondo = when { esActual -> MaterialTheme.colorScheme.primary; a < hoy -> MaterialTheme.colorScheme.surfaceContainer; else -> MaterialTheme.colorScheme.surface }
            val tinta = when { esActual -> MaterialTheme.colorScheme.onPrimary; foto != null -> Color.White; else -> MaterialTheme.colorScheme.onSurface }
            Surface(
                onClick = { onAnio(a) }, shape = RoundedCornerShape(14.dp), color = fondo,
                border = BorderStroke(if (a == hoy && !esActual) 2.dp else 1.dp, if (a == hoy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.width(80.dp).height(96.dp).semantics { contentDescription = "$a, $edad" + if (palabra.isNotBlank()) ", $palabra" else "" },
            ) {
                Box {
                    if (foto != null && !esActual) {
                        AsyncImage(modelo(foto), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x10000000), Color(0xC8060A12)))))
                    }
                    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Bottom) {
                        if (palabra.isNotBlank()) Text(palabra, fontFamily = FontFamily.Serif, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = tinta)
                        Text("$a", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = tinta)
                        Text(stringResource(R.string.vida_anios_edad, edad), fontSize = 10.sp, color = tinta.copy(alpha = 0.8f))
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 3.dp).heightIn(min = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ejes.forEach { e -> Box(Modifier.size(7.dp).background(colorCampamento(e), CircleShape)) }
                            if (sellada) Image(painterResource(R.drawable.logo_sello), null, Modifier.size(11.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Año anterior y siguiente (además de deslizar el dedo sobre la pantalla). */
@Composable
fun NavAnio(anio: Int, onAnio: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onAnio(anio - 1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null); Text("${anio - 1}") }
        Text(stringResource(R.string.vida_desliza), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { onAnio(anio + 1) }) { Text("${anio + 1}"); Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
    }
}

/** Campo de texto que guarda solo, un momento después de dejar de escribir. */
@Composable
private fun CampoVida(valor: String, etiqueta: String, ejemplo: String, lineas: Int, onGuardar: (String) -> Unit, max: Int = 2000) {
    var texto by remember(valor) { mutableStateOf(valor) }
    LaunchedEffect(texto) { if (texto != valor) { delay(600); onGuardar(texto) } }
    // Si el campo se va (por ejemplo, al sellar la carta), se guarda lo último escrito
    val ultimo by androidx.compose.runtime.rememberUpdatedState(texto)
    val original by androidx.compose.runtime.rememberUpdatedState(valor)
    val guardar by androidx.compose.runtime.rememberUpdatedState(onGuardar)
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { if (ultimo != original) guardar(ultimo) } }
    OutlinedTextField(
        value = texto, onValueChange = { texto = it.take(max) }, label = { Text(etiqueta) }, placeholder = { Text(ejemplo) },
        minLines = lineas, maxLines = maxOf(lineas, 8), singleLine = lineas == 1, modifier = Modifier.fillMaxWidth(),
    )
}

/** El corazón del año: su palabra y sus anhelos y sueños; un toque convierte un anhelo en meta de ese año. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CorazonAnio(anio: Int, vida: Map<String, String>, onGuardar: (String, String) -> Unit, onAnheloAMeta: (String) -> Unit) {
    val hoy = LocalDate.now().year; val futuro = anio > hoy
    val anhelos = vida[claveVida(anio, "anhelos")].orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
    RutaCard {
        Text(stringResource(when { futuro -> R.string.vida_suenas; anio == hoy -> R.string.vida_tu_anio; else -> R.string.vida_lo_que_fue }, anio), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(if (futuro) R.string.vida_anhelos_ayuda_futuro else R.string.vida_anhelos_ayuda), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
        CampoVida(vida[claveVida(anio, "palabra")].orEmpty(), stringResource(R.string.vida_palabra), stringResource(R.string.vida_palabra_ejemplo), 1,
            { onGuardar(claveVida(anio, "palabra"), it.trim()) }, max = 40)
        Spacer(Modifier.height(8.dp))
        CampoVida(vida[claveVida(anio, "anhelos")].orEmpty(), stringResource(if (futuro) R.string.vida_anhelos_suenos else R.string.vida_anhelos), stringResource(R.string.vida_anhelos_ejemplo), 3,
            { onGuardar(claveVida(anio, "anhelos"), it) })
        if (anhelos.isNotEmpty() && anio >= hoy) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                anhelos.take(8).forEach { t ->
                    AssistChip(onClick = { onAnheloAMeta(t) }, label = { Text(t, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) })
                }
            }
            Text(stringResource(R.string.vida_anhelo_a_meta, anio), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Lo cumplido del año: metas logradas y publicaciones de logro. */
@Composable
fun CumplidosAnio(anio: Int, metas: List<MetaAnualEntity>, posts: List<Post>, onPost: (String) -> Unit) {
    val logradas = metas.filter { it.anio == anio && (it.estado == "CUMPLIDA" || it.avance >= 100) }
    val logros = posts.filter { it.anio == anio && it.tipo == TipoPost.LOGRO }
    if (logradas.isEmpty() && logros.isEmpty()) return
    RutaCard {
        Text(stringResource(R.string.vida_cumplidos, anio), style = MaterialTheme.typography.titleMedium)
        logradas.forEach { m ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(com.rutaalacima.app.ui.components.IconosRuta.Metas, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(m.titulo, style = MaterialTheme.typography.bodyMedium)
            }
        }
        logros.take(6).forEach { p ->
            Text("· " + p.metaTitulo.ifBlank { p.texto }, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clickable { onPost(p.id) }.padding(vertical = 6.dp))
        }
    }
}

/** Carta a tu yo de ese año: si el año está por venir, queda sellada con lacre hasta el 1 de enero. */
@Composable
fun CartaAnio(anio: Int, vida: Map<String, String>, onGuardar: (String, String) -> Unit) {
    val hoy = LocalDate.now().year; val futuro = anio > hoy
    val texto = vida[claveVida(anio, "carta")].orEmpty()
    var editar by rememberSaveable(anio) { mutableStateOf(false) }
    if (!futuro && texto.isBlank() && anio < hoy) return
    RutaCard {
        when {
            futuro && texto.isNotBlank() && !editar -> Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.logo_sello), stringResource(R.string.vida_carta_sello), Modifier.size(64.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.vida_carta_titulo, anio), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.vida_carta_sellada, anio), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { editar = true }, modifier = Modifier.padding(top = 6.dp)) { Text(stringResource(R.string.vida_carta_reescribir)) }
                }
            }
            !futuro && texto.isNotBlank() && !editar -> {
                Text(stringResource(R.string.vida_carta_titulo, anio), style = MaterialTheme.typography.titleMedium)
                Text(texto, fontFamily = FontFamily.Serif, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.padding(vertical = 8.dp))
                TextButton(onClick = { editar = true }) { Text(stringResource(R.string.editar)) }
            }
            else -> {
                Text(stringResource(R.string.vida_carta_titulo, anio), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(if (futuro) R.string.vida_carta_ayuda_futuro else R.string.vida_carta_ayuda, anio), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                CampoVida(texto, stringResource(R.string.vida_carta_campo), stringResource(R.string.vida_carta_ejemplo, anio), 5, { onGuardar(claveVida(anio, "carta"), it) }, max = 5000)
                Button(onClick = { editar = false }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(if (futuro) R.string.vida_carta_sellar else R.string.listo))
                }
            }
        }
    }
}
