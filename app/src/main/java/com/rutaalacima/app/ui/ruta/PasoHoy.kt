package com.rutaalacima.app.ui.ruta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.VisionCasillaEntity
import com.rutaalacima.app.data.local.avance
import com.rutaalacima.app.data.local.diasMarcados
import com.rutaalacima.app.data.vision.LibroMetas
import com.rutaalacima.app.data.vision.SincroBrujula
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.domain.model.MetodoCima
import com.rutaalacima.app.ui.components.RutaCard
import java.time.LocalDate

/** Paso de hoy: una fila de chips, uno por meta del mes (punto de color y %). Un toque marca que hoy le diste un paso;
 *  si la meta tiene sub-metas, primero se escoge a cuál (suma su jornada del Método Cima). Tocar una marcada la desmarca. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PasoDeHoy(fecha: LocalDate, metas: List<MetaMensualEntity>, anuales: List<MetaAnualEntity>, casillas: List<VisionCasillaEntity>,
    mandala: Map<String, String>, onMarcar: (MetaMensualEntity, Long?, Int?) -> Unit, onDeshacer: (MetaMensualEntity) -> Unit) {
    val futuro = fecha.isAfter(LocalDate.now())
    var abierta by remember(fecha) { mutableStateOf<Long?>(null) }
    val cs = MaterialTheme.colorScheme
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.paso_hoy_titulo), style = MaterialTheme.typography.titleMedium)
                if (metas.isNotEmpty()) Text(stringResource(R.string.paso_hoy_ayuda), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            if (metas.isNotEmpty()) Text("${metas.count { fecha.dayOfMonth in it.diasMarcados() }}/${metas.size}", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(cs.surfaceContainerHigh).padding(horizontal = 10.dp, vertical = 4.dp))
        }
        if (metas.isEmpty()) {
            Text(stringResource(R.string.paso_hoy_vacio), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            return@RutaCard
        }
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            metas.forEach { m ->
                val anual = anuales.firstOrNull { it.id == m.metaAnualId }
                val si = fecha.dayOfMonth in m.diasMarcados()
                FilterChip(selected = si, enabled = !futuro, onClick = {
                    if (si) { onDeshacer(m); abierta = null }
                    else if (pasosDelMes(m, anual, anuales, casillas, mandala).second.isNotEmpty()) abierta = if (abierta == m.id) null else m.id
                    else onMarcar(m, null, null)
                }, leadingIcon = {
                    if (si) Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                    else Box(Modifier.size(10.dp).clip(CircleShape).background(com.rutaalacima.app.ui.vision.colorEje(anual?.eje ?: "CON")))
                }, label = {
                    Text((anual?.titulo ?: m.texto), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
                    Text("  ${(m.avance() * 100).toInt()} %", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                })
            }
        }
        val m = metas.firstOrNull { it.id == abierta }
        if (m != null) {
            val anual = anuales.firstOrNull { it.id == m.metaAnualId }
            val (cas, ps) = pasosDelMes(m, anual, anuales, casillas, mandala)
            Column(Modifier.padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).background(cs.surfaceContainerHigh).padding(10.dp)) {
                Text(stringResource(R.string.paso_hoy_cual), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ps.forEach { (p, t) ->
                        AssistChip(onClick = { onMarcar(m, cas?.id, p); abierta = null },
                            label = { Text("${p + 1}. $t", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp)) })
                    }
                    AssistChip(onClick = { onMarcar(m, null, null); abierta = null }, label = { Text(stringResource(R.string.paso_hoy_general)) })
                    AssistChip(onClick = { abierta = null }, label = { Text(stringResource(R.string.cancelar)) },
                        leadingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(16.dp)) })
                }
            }
        }
    }
}

/** Sub-metas aún no ganadas que se trabajan este mes (las elegidas o, si no se eligió ninguna, todas). */
private fun pasosDelMes(m: MetaMensualEntity, anual: MetaAnualEntity?, anuales: List<MetaAnualEntity>, casillas: List<VisionCasillaEntity>,
    mandala: Map<String, String>): Pair<VisionCasillaEntity?, List<Pair<Int, String>>> {
    val cas = SincroBrujula.casillaDe(anual, anuales, casillas) ?: return null to emptyList()
    val todos = (0 until Mandala.PASOS).mapNotNull { p -> mandala[Mandala.clave(cas.id, p)].orEmpty().trim().takeIf { it.isNotEmpty() }?.let { p to it } }
        .filter { (p, _) -> !MetodoCima.leer(cas.id, p, mandala).cumplido }
    val el = LibroMetas.pasosDelMes(m.id).filter { e -> todos.any { it.first == e } }
    return cas to todos.filter { el.isEmpty() || it.first in el }
}
