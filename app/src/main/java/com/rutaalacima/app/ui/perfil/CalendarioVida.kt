package com.rutaalacima.app.ui.perfil

import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.PerfilEntity
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.domain.model.Vida
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
 * "Mi vida": la línea de la vida en puntos. Cada punto es un año, 10 por fila, desde el
 * nacimiento hasta la esperanza de vida: los vividos se llenan, el actual lleva un anillo dorado
 * y los años con recuerdos brillan en dorado. Al tocar un punto, ese año se abre mes a mes.
 */
fun LazyListScope.calendarioVida(
    perfil: PerfilEntity,
    posts: List<Post>,
    anioSel: Int?,
    mesSel: Int?,
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
    val vividos = Vida.mesesVividos(anioNac, mesNac, h.year, h.monthValue).coerceAtMost(esperanza * 12)
    val edad = vividos / 12
    val porAnio = posts.groupBy { it.anio }
    // Años de edad 0..esperanza-1: un punto por cada uno
    val puntos = (0 until esperanza).toList()

    item {
        Column(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.vida_anios_de, edad, esperanza), style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black,
            )
            Text(stringResource(R.string.vida_resumen, vividos, (esperanza * 12 - vividos).coerceAtLeast(0)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            val vivido = MaterialTheme.colorScheme.primary
            val recuerdo = MaterialTheme.colorScheme.secondary
            val futuro = MaterialTheme.colorScheme.surfaceVariant
            val borde = MaterialTheme.colorScheme.outlineVariant
            puntos.chunked(10).forEach { fila ->
                Row(Modifier.fillMaxWidth()) {
                    fila.forEach { e ->
                        val anio = anioNac + e
                        val conRecuerdos = porAnio[anio].orEmpty().isNotEmpty()
                        val actual = e == edad
                        val pasado = e < edad
                        val sel = anio == anioSel
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).padding(3.dp).clip(CircleShape)
                                .clickable { onSeleccion(anio, null) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.fillMaxSize(if (sel) 1f else 0.86f).clip(CircleShape)
                                    .background(
                                        when {
                                            conRecuerdos -> Brush.linearGradient(listOf(recuerdo, recuerdo.copy(alpha = 0.7f)))
                                            pasado -> Brush.linearGradient(listOf(vivido, vivido.copy(alpha = 0.75f)))
                                            else -> Brush.linearGradient(listOf(futuro, futuro))
                                        },
                                    )
                                    .then(
                                        when {
                                            actual -> Modifier.border(2.5.dp, recuerdo, CircleShape)
                                            sel -> Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            !pasado -> Modifier.border(1.dp, borde, CircleShape)
                                            else -> Modifier
                                        },
                                    ),
                            )
                        }
                    }
                    repeat(10 - fila.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Leyenda(vivido, stringResource(R.string.vida_vivido))
                Leyenda(recuerdo, stringResource(R.string.vida_con_registro))
                Leyenda(futuro, stringResource(R.string.vida_por_vivir))
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.vida_punto_ayuda), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }

    val anio = anioSel
    if (anio != null && anio in anioNac until anioNac + esperanza) {
        item(key = "vida-detalle-$anio") {
            DetalleAnio(anio, anio - anioNac, mesSel, porAnio[anio].orEmpty(), onSeleccion, onAbrir, onPublicar, esFuturo = anio > h.year)
        }
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
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
