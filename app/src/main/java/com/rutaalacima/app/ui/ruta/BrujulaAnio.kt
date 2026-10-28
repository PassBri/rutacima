package com.rutaalacima.app.ui.ruta

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.VisionCasillaEntity
import com.rutaalacima.app.data.vision.LibroMetas
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.domain.model.MetodoCima
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.planner.PlanMeta
import com.rutaalacima.app.ui.vision.colorCampamento
import com.rutaalacima.app.ui.vision.nombreCodigo

/**
 * El año es su Brújula (igual que la web): la cumbre de ese año y sus 8 campamentos, cada uno con su meta,
 * sus sub-metas (los pasos, con su casilla de ganado) y cuántas actividades lleva su plan de acción.
 */
@Composable
internal fun BrujulaAnioCard(
    anio: Int, casillas: List<VisionCasillaEntity>, anuales: List<MetaAnualEntity>, mandala: Map<String, String>,
    onGanado: (Long, Int) -> Unit, onAbrirBrujula: () -> Unit, onMeta: (MetaAnualEntity) -> Unit,
) {
    val delAnio = casillas.filter { Mandala.anioDeOrigen(it.origen) == anio }
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.bja_titulo, anio), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                val (cumbre, _) = Mandala.repartir(delAnio, { it.origen }, { it.eje })
                Text(cumbre?.afirmacion?.ifBlank { cumbre.titulo } ?: stringResource(R.string.bja_sin_brujula, anio),
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            OutlinedButton(onClick = onAbrirBrujula) {
                Icon(Icons.Filled.Explore, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text(stringResource(if (delAnio.isEmpty()) R.string.bja_crear else R.string.bja_abrir))
            }
        }
        if (delAnio.isEmpty()) return@RutaCard
        val (_, camps) = Mandala.repartir(delAnio, { it.origen }, { it.eje })
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Mandala.CAMPAMENTOS_FIJOS.forEachIndexed { i, cod ->
                val c = camps.getOrNull(i) ?: return@forEachIndexed
                val color = colorCampamento(cod)
                val eje = if (cod == "CON") null else cod
                val meta = anuales.filter { it.anio == anio && it.eje == eje }.minByOrNull { it.id }
                val pasos = (0 until Mandala.PASOS).map { p -> p to mandala[Mandala.clave(c.id, p)].orEmpty().trim() }.filter { it.second.isNotEmpty() }
                val ganados = (0 until Mandala.PASOS).count { !mandala[Mandala.claveHecho(c.id, it)].isNullOrBlank() }
                Surface(shape = RoundedCornerShape(16.dp), color = color.copy(alpha = .06f), border = BorderStroke(1.dp, color.copy(alpha = .25f))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(nombreCodigo(cod), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("$ganados/8", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
                        }
                        Text(meta?.titulo ?: c.afirmacion.ifBlank { c.titulo }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = meta != null) { meta?.let(onMeta) })
                        LinearProgressIndicator(progress = { ganados / 8f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = color, trackColor = color.copy(alpha = .14f))
                        pasos.forEach { (p, t) ->
                            val ganado = !mandala[Mandala.claveHecho(c.id, p)].isNullOrBlank()
                            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(10.dp)).clickable { onGanado(c.id, p) },
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(24.dp).clip(CircleShape).background(if (ganado) color else Color.Transparent)
                                    .border(2.dp, if (ganado) color else color.copy(alpha = .5f), CircleShape), contentAlignment = Alignment.Center) {
                                    if (ganado) Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = Color.White)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(t, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                                    textDecoration = if (ganado) TextDecoration.LineThrough else null,
                                    color = if (ganado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                val x = MetodoCima.leer(c.id, p, mandala)
                                if (!ganado && x.jornadas.isNotEmpty()) Text("${x.jornadas.size}/${x.requeridas ?: MetodoCima.DIFICULTAD_INICIAL}",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (meta != null) {
                            val plan = PlanMeta.leer(LibroMetas.valores[PlanMeta.clave(meta.id)])
                            Text(stringResource(R.string.bja_plan, plan.count { it.h }, plan.size), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onMeta(meta) }.padding(vertical = 6.dp))
                        }
                    }
                }
            }
        }
    }
}
