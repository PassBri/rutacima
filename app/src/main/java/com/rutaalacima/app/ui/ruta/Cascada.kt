package com.rutaalacima.app.ui.ruta

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.PropositoEntity
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.planner.nombreMes
import com.rutaalacima.app.util.hoy
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/*
 * La cascada completa, igual que en la web: cumbres personales (propósitos que abarcan varios años) → años → meses → días.
 * Cada nivel muestra de dónde viene y qué lo sostiene abajo.
 */

/** Los años que abarca una cumbre personal: desde que se creó (o su primera meta) hasta su horizonte. */
fun aniosDeCumbre(p: PropositoEntity, anuales: List<MetaAnualEntity>): IntRange {
    val metas = anuales.filter { it.propositoId == p.id }.map { it.anio }
    val creado = Instant.ofEpochMilli(p.creadoEn).atZone(ZoneId.systemDefault()).year
    val ini = (metas + creado).min()
    val fin = (metas + (ini + p.horizonte.coerceAtLeast(1) - 1)).max()
    return ini..fin
}

private fun colorEje(codigo: String?): Color = Eje.entries.firstOrNull { it.codigo == codigo }?.color?.let { Color(it) } ?: Color(0xFF7A4A6B)

/** Avance de una cumbre: el promedio de sus metas anuales. */
private fun avanceCumbre(p: PropositoEntity, anuales: List<MetaAnualEntity>): Int =
    anuales.filter { it.propositoId == p.id }.map { it.avance }.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: p.progreso

/** Año: las cumbres personales en curso, en qué año de su camino van y sus metas de este año. */
@Composable
fun CumbresAnio(
    anio: Int,
    propositos: List<PropositoEntity>,
    anuales: List<MetaAnualEntity>,
    onProposito: (Long) -> Unit,
    onNuevaMeta: (PropositoEntity) -> Unit,
) {
    val ps = propositos.filter { anio in aniosDeCumbre(it, anuales) }
    if (ps.isEmpty()) return
    RutaCard {
        Text(stringResource(R.string.cumbres_en_anio, anio), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.cumbres_en_anio_ayuda, anio), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ps.forEach { p ->
            val r = aniosDeCumbre(p, anuales)
            val ms = anuales.filter { it.propositoId == p.id && it.anio == anio }
            Column(Modifier.padding(top = 12.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onProposito(p.id) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.size(10.dp).clip(CircleShape).background(colorEje(p.eje)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.cumbre_anio_n, anio - r.first + 1, r.last - r.first + 1, r.first, r.last),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${avanceCumbre(p, anuales)}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (ms.isEmpty()) Text(stringResource(R.string.sin_metas_anio, anio), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp))
                ms.forEach { m ->
                    Row(Modifier.padding(start = 20.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.titulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("${m.avance}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = { onNuevaMeta(p) }, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.meta_anio_cumbre, anio))
                }
            }
        }
    }
}

/** "↑ meta del año · cumbre": de dónde viene una meta del mes. */
@Composable
fun CadenaMeta(m: MetaMensualEntity, anuales: List<MetaAnualEntity>, propositos: List<PropositoEntity>, modifier: Modifier = Modifier) {
    val a = anuales.firstOrNull { it.id == m.metaAnualId } ?: return
    val p = propositos.firstOrNull { it.id == a.propositoId }
    Text("↑ " + a.titulo + (p?.let { " · " + it.titulo } ?: ""), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/** Mes: metas del año que todavía no tienen un paso este mes, para bajar la meta grande a este mes con un toque. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SinPasoMes(ym: YearMonth, anuales: List<MetaAnualEntity>, mensuales: List<MetaMensualEntity>, onCrear: (MetaAnualEntity) -> Unit) {
    val delMes = mensuales.filter { it.anio == ym.year && it.mes == ym.monthValue }
    val faltan = anuales.filter { it.anio == ym.year && it.estado != "CUMPLIDA" && delMes.none { m -> m.metaAnualId == it.id } }
    if (faltan.isEmpty()) return
    Column {
        Text(stringResource(R.string.sin_paso_mes, ym.year, nombreMes(ym.monthValue)), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            faltan.forEach { a ->
                AssistChip(onClick = { onCrear(a) }, label = { Text(a.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp)) },
                    leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) })
            }
        }
    }
}

/** La cinta de años de una cumbre personal, con las metas de cada año. Toca un año para abrirlo en Mi ruta. */
@Composable
fun CintaCumbre(p: PropositoEntity, anuales: List<MetaAnualEntity>, onAnio: (Int) -> Unit) {
    val r = aniosDeCumbre(p, anuales)
    val actual = hoy().year
    val desc = stringResource(R.string.anios_cumbre)
    LazyRow(Modifier.fillMaxWidth().semantics { contentDescription = desc }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(r.toList()) { a ->
            val n = anuales.count { it.propositoId == p.id && it.anio == a }
            val fondo = when {
                a == actual -> MaterialTheme.colorScheme.primary
                a < actual -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> MaterialTheme.colorScheme.surfaceContainerLowest
            }
            val tinta = if (a == actual) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Column(Modifier.clip(RoundedCornerShape(12.dp)).background(fondo).clickable { onAnio(a) }.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$a", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = tinta)
                Text(if (n > 0) stringResource(R.string.ruta_n_metas, n) else stringResource(R.string.sin_metas),
                    style = MaterialTheme.typography.labelSmall, color = tinta.copy(alpha = 0.8f))
            }
        }
    }
}
