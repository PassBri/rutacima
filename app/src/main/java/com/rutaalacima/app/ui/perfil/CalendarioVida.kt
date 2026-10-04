package com.rutaalacima.app.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.PerfilEntity
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.domain.model.Vida
import com.rutaalacima.app.domain.model.Vida.EstadoMes
import com.rutaalacima.app.ui.comunidad.PostImagen
import com.rutaalacima.app.ui.comunidad.TipoBadge
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.util.hoy
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Esperanza de vida efectiva: la elegida por la persona o la estimada para su país. */
fun PerfilEntity.esperanza(): Int = esperanzaVida ?: Vida.esperanzaPais(Locale.getDefault().country)

private fun Post.mes(): Int = Instant.ofEpochMilli(creadoEn).atZone(ZoneId.systemDefault()).monthValue

/**
 * "Mi vida": calendario de vida. Cada año de la vida, desde el nacimiento hasta la esperanza
 * de vida, se ve como una fila de 12 meses. Se elige una década y luego un año o un mes; el año
 * elegido se despliega como calendario mensual con lo que registraste en él.
 */
fun LazyListScope.calendarioVida(
    perfil: PerfilEntity,
    posts: List<Post>,
    decadaSel: Int?,
    anioSel: Int?,
    mesSel: Int?,
    onDecada: (Int) -> Unit,
    onSeleccion: (anio: Int, mes: Int?) -> Unit,
    onAbrir: (String) -> Unit,
    onPublicar: (String) -> Unit,
    onAjustes: () -> Unit,
) {
    val anioNac = perfil.anioNacimiento
    val mesNac = perfil.mesNacimiento ?: 1
    if (anioNac == null) {
        item {
            RutaCard(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Cake, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.vida_sin_nacimiento_titulo), style = MaterialTheme.typography.titleMedium)
                }
                Text(stringResource(R.string.vida_sin_nacimiento_texto), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onAjustes, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.configurar_nacimiento)) }
            }
        }
        return
    }

    val h = hoy()
    val esperanza = perfil.esperanza()
    val totalMeses = esperanza * 12
    val vividos = Vida.mesesVividos(anioNac, mesNac, h.year, h.monthValue).coerceAtMost(totalMeses)
    val edad = vividos / 12
    val porAnio = posts.groupBy { it.anio }
    val decadas = (0..esperanza / 10).toList()
    val decada = decadaSel ?: (edad / 10).coerceIn(decadas.first(), decadas.last())

    // Resumen
    item {
        RutaCard(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$edad", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vida_edad, esperanza), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
            }
            LinearProgressIndicator(
                progress = { vividos / totalMeses.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeCap = StrokeCap.Round,
            )
            Text(
                stringResource(R.string.vida_resumen, vividos, (totalMeses - vividos).coerceAtLeast(0)),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp),
            )
            Text(stringResource(R.string.vida_frase), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }

    // Selector de década + leyenda
    item {
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(decadas) { d ->
                FilterChip(selected = d == decada, onClick = { onDecada(d) },
                    label = { Text(stringResource(R.string.vida_decada, d * 10, d * 10 + 9)) })
            }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Leyenda(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), stringResource(R.string.vida_vivido))
            Leyenda(MaterialTheme.colorScheme.secondary, stringResource(R.string.vida_con_registro))
            Leyenda(MaterialTheme.colorScheme.surfaceVariant, stringResource(R.string.vida_por_vivir))
        }
        // Iniciales de los meses
        Row(Modifier.padding(start = 16.dp + 64.dp, end = 16.dp)) {
            (1..12).forEach { m ->
                Text(
                    java.time.Month.of(m).getDisplayName(TextStyle.NARROW_STANDALONE, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Filas: un año de vida = 12 meses
    val desdeEdad = decada * 10
    val hastaEdad = minOf(desdeEdad + 9, esperanza)
    for (e in desdeEdad..hastaEdad) {
        val anio = anioNac + e
        val delAnio = porAnio[anio].orEmpty()
        val mesesConRegistro = delAnio.map { it.mes() }.toSet()
        item(key = "vida-$anio") {
            val seleccionado = anio == anioSel
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (seleccionado) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else Color.Transparent)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(64.dp).clickable { onSeleccion(anio, null) }) {
                    Text("$anio", style = MaterialTheme.typography.labelLarge, fontWeight = if (anio == h.year) FontWeight.Black else FontWeight.Normal,
                        color = if (anio == h.year) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.vida_anios_edad, e), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                (1..12).forEach { m ->
                    val estado = Vida.estado(anio, m, anioNac, mesNac, h.year, h.monthValue)
                    val conRegistro = m in mesesConRegistro
                    val color = when {
                        estado == EstadoMes.ANTES_DE_NACER -> Color.Transparent
                        conRegistro -> MaterialTheme.colorScheme.secondary
                        estado == EstadoMes.VIVIDO -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        estado == EstadoMes.ACTUAL -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(RoundedCornerShape(5.dp)).background(color)
                            .then(
                                if (estado == EstadoMes.ACTUAL || (seleccionado && m == mesSel))
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(5.dp)) else Modifier,
                            )
                            .clickable(enabled = estado != EstadoMes.ANTES_DE_NACER) { onSeleccion(anio, m) },
                    )
                }
            }
        }
        if (anio == anioSel) {
            item(key = "vida-detalle-$anio") {
                DetalleAnio(anio, e, mesSel, delAnio, onSeleccion, onAbrir, onPublicar, esFuturo = anio > h.year)
            }
        }
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(texto, style = MaterialTheme.typography.labelSmall)
    }
}

/** El año elegido como calendario mensual: 12 meses con lo que registraste en cada uno. */
@Composable
private fun DetalleAnio(
    anio: Int,
    edad: Int,
    mesSel: Int?,
    posts: List<Post>,
    onSeleccion: (Int, Int?) -> Unit,
    onAbrir: (String) -> Unit,
    onPublicar: (String) -> Unit,
    esFuturo: Boolean,
) {
    val porMes = posts.groupBy { it.mes() }
    RutaCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(stringResource(R.string.vida_anio_titulo, anio, edad), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        (1..12).chunked(4).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fila.forEach { m ->
                    val n = porMes[m]?.size ?: 0
                    val sel = m == mesSel
                    Column(
                        Modifier.weight(1f).padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    sel -> MaterialTheme.colorScheme.primary
                                    n > 0 -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            )
                            .clickable { onSeleccion(anio, if (sel) null else m) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            java.time.Month.of(m).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()).replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                        Box(
                            Modifier.padding(top = 4.dp).size(22.dp).clip(CircleShape)
                                .background(if (n > 0) MaterialTheme.colorScheme.secondary else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (n > 0) Text("$n", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
            }
        }
        val visibles = if (mesSel == null) posts else porMes[mesSel].orEmpty()
        Spacer(Modifier.height(8.dp))
        if (visibles.isEmpty()) {
            Text(
                stringResource(if (esFuturo) R.string.vida_anio_futuro else R.string.vida_anio_vacio),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibles, key = { it.id }) { p ->
                    Column(Modifier.width(150.dp).clickable { onAbrir(p.id) }) {
                        PostImagen(p, Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)), conTexto = false)
                        Spacer(Modifier.height(4.dp))
                        TipoBadge(p.tipo)
                        Text(p.metaTitulo.ifBlank { p.texto }, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (esFuturo) {
            OutlinedButton(onClick = { onPublicar("VISION") }) { Text(stringResource(R.string.vida_sonar_anio)) }
        } else {
            OutlinedButton(onClick = { onPublicar("LOGRO") }) {
                Icon(Icons.Filled.AddAPhoto, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.vida_registrar))
            }
        }
    }
}
