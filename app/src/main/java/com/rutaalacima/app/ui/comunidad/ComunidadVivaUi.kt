package com.rutaalacima.app.ui.comunidad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.Compromiso
import com.rutaalacima.app.data.social.Post
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/* Comunidad viva en la app (igual que web/js/45-comunidad-viva.js). */

private fun fecha(iso: String): String = runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) }.getOrDefault(iso)
private fun diasHasta(iso: String): Long = runCatching { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(iso)) }.getOrDefault(0)

/** Carta al yo futuro: un sobre de papel con su lacre, hasta el día en que se abre. */
@Composable
fun CartaSellada(abre: String, modifier: Modifier = Modifier, conColumna: Boolean = true) {
    val tinta = Color(0xFF3A2A1E)
    Box(modifier.background(Brush.radialGradient(listOf(Color(0xFFF7EEDD), Color(0xFFE8D9BC)))), contentAlignment = Alignment.Center) {
        Column(if (conColumna) Modifier.padding(start = 24.dp, end = 88.dp, top = 80.dp, bottom = 180.dp) else Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth(0.62f).aspectRatio(1.5f).shadow(10.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)).background(Color(0xFFFBF5E8))) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val p = Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height * 0.62f); close() }
                    drawPath(p, Color(0xFFEFE3CC))
                }
                Image(painterResource(R.drawable.logo_sello), null, Modifier.align(Alignment.Center).padding(top = 24.dp).size(64.dp))
            }
            Text(stringResource(R.string.cv_carta_titulo), color = Color(0xFF5A1F14), style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold))
            val n = diasHasta(abre)
            Text(if (n > 0) stringResource(R.string.cv_se_abre_faltan, fecha(abre), n.toInt()) else stringResource(R.string.cv_se_abre, fecha(abre)),
                color = tinta, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Text(stringResource(R.string.cv_carta_ayuda), color = tinta.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ChipCv(icono: ImageVector, texto: String, sobreFoto: Boolean, fondo: Color? = null) {
    val c = if (sobreFoto) Color.White else MaterialTheme.colorScheme.onSurface
    Surface(shape = RoundedCornerShape(8.dp), color = fondo ?: if (sobreFoto) Color(0x6B0A0E18) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, if (sobreFoto) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = c, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(5.dp))
            Text(texto, color = c, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** El pie de la publicación: el camino detrás, el compromiso (y su cierre, si es mío y venció) y el sello propio. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsCamino(post: Post, sobreFoto: Boolean, onCerrarCompromiso: (String) -> Unit) {
    val hoy = LocalDate.now().toString()
    val k = post.compromiso; val cam = post.camino
    val propioSellado = post.propio && post.sello != null && hoy < post.sello
    if (cam == null && k == null && !propioSellado) return
    val burdeos = if (sobreFoto) Color(0x8C6E1E14) else null
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (cam != null && cam.racha > 0) ChipCv(Icons.Filled.Terrain, stringResource(R.string.cv_dia_constancia, cam.racha), sobreFoto, burdeos)
        if (cam != null && cam.ganados > 0) ChipCv(Icons.Filled.Flag, stringResource(R.string.cv_pasos, cam.ganados), sobreFoto, burdeos)
        if (k != null) when {
            k.estado == Compromiso.LOGRADO -> ChipCv(Icons.Filled.Check, stringResource(R.string.cv_compromiso_cumplido), sobreFoto, if (sobreFoto) Color(0x991E5A32) else null)
            k.estado == Compromiso.REPLANTEO -> ChipCv(Icons.Filled.Refresh, stringResource(R.string.cv_replanteo), sobreFoto)
            hoy <= k.hasta -> { val n = diasHasta(k.hasta).toInt()
                ChipCv(Icons.Filled.Flag, if (n > 0) stringResource(R.string.cv_compromiso_faltan, n) else stringResource(R.string.cv_compromiso_hoy), sobreFoto) }
            post.propio -> {
                ChipCv(Icons.Filled.Flag, stringResource(R.string.cv_vencio_propio), sobreFoto)
                FilterChip(selected = true, onClick = { onCerrarCompromiso(Compromiso.LOGRADO) }, label = { Text(stringResource(R.string.cv_lo_logre)) },
                    leadingIcon = { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) })
                FilterChip(selected = false, onClick = { onCerrarCompromiso(Compromiso.REPLANTEO) }, label = { Text(stringResource(R.string.cv_lo_replanteo), color = if (sobreFoto) Color.White else Color.Unspecified) },
                    leadingIcon = { Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp), tint = if (sobreFoto) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) })
            }
            else -> ChipCv(Icons.Filled.Schedule, stringResource(R.string.cv_vencio_esperando), sobreFoto)
        }
        if (propioSellado) ChipCv(Icons.Filled.Lock, stringResource(R.string.cv_sellada_hasta, fecha(post.sello!!)), sobreFoto)
    }
}

/** Lanzar una cuerda: comentar con una pregunta, un consejo o "yo también lo intento". */
val CUERDAS = listOf<Pair<String?, Int>>(null to R.string.cv_comentar, "PREGUNTA" to R.string.cv_pregunta, "CONSEJO" to R.string.cv_consejo, "YO_TAMBIEN" to R.string.cv_yo_tambien)

@Composable
fun ChipsCuerda(elegida: String?, onElegir: (String?) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CUERDAS.forEach { (k, n) ->
            FilterChip(selected = elegida == k, onClick = { onElegir(k) }, label = { Text(stringResource(n)) },
                leadingIcon = if (k != null) ({ Icon(Icons.Filled.Link, null, Modifier.size(16.dp)) }) else null)
        }
    }
}

@Composable
fun EtiquetaCuerda(cuerda: String?) {
    val n = CUERDAS.firstOrNull { it.first == cuerda && cuerda != null }?.second ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.cv_lanzo_cuerda, stringResource(n)), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun placeholderCuerda(k: String?): String? = when (k) {
    "PREGUNTA" -> stringResource(R.string.cv_ph_pregunta); "CONSEJO" -> stringResource(R.string.cv_ph_consejo); "YO_TAMBIEN" -> stringResource(R.string.cv_ph_yo_tambien); else -> null
}

/** Cómo funciona la comunidad: un botón junto a la lupa que abre la guía de todas las opciones. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BotonGuiaComunidad(tinte: Color = MaterialTheme.colorScheme.onSurface) {
    var abierta by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.material3.IconButton(onClick = { abierta = true }) {
        Icon(Icons.AutoMirrored.Filled.HelpOutline, stringResource(R.string.cv_guia_titulo), tint = tinte)
    }
    if (abierta) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { abierta = false }) {
        val lista = listOf(
            Icons.Filled.Terrain to (R.string.cv_g1_t to R.string.cv_g1_d), Icons.Filled.KeyboardArrowUp to (R.string.cv_g2_t to R.string.cv_g2_d),
            Icons.Filled.Favorite to (R.string.cv_g3_t to R.string.cv_g3_d), Icons.Filled.WbSunny to (R.string.cv_g4_t to R.string.cv_g4_d),
            Icons.Filled.Link to (R.string.cv_g5_t to R.string.cv_g5_d), Icons.Filled.Bookmark to (R.string.cv_g6_t to R.string.cv_g6_d),
            Icons.Filled.Hiking to (R.string.cv_g7_t to R.string.cv_g7_d), Icons.Filled.Flag to (R.string.cv_g8_t to R.string.cv_g8_d),
            Icons.Filled.Lock to (R.string.cv_g9_t to R.string.cv_g9_d), Icons.Filled.Diversity3 to (R.string.cv_g10_t to R.string.cv_g10_d),
            Icons.Filled.Landscape to (R.string.cv_g11_t to R.string.cv_g11_d), Icons.Filled.MoreHoriz to (R.string.cv_g12_t to R.string.cv_g12_d),
        )
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
            item {
                Text(stringResource(R.string.cv_guia_titulo), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.cv_guia_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            }
            items(lista.size) { i ->
                val (ic, td) = lista[i]
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)), contentAlignment = Alignment.Center) {
                        Icon(ic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(td.first), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(td.second), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (i < lista.lastIndex) androidx.compose.material3.HorizontalDivider()
            }
        }
    }
}
