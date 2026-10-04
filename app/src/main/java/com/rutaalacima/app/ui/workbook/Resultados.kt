package com.rutaalacima.app.ui.workbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.RadarEjes
import com.rutaalacima.app.ui.i18n.Textos
import com.rutaalacima.app.ui.i18n.texto
import com.rutaalacima.app.ui.theme.asColor
import com.rutaalacima.app.data.content.ResultadoSeccion

/** Tarjeta de resultados al final de una sección con escalas. */
@Composable
fun ResultadosCard(r: ResultadoSeccion, onGuardarEjes: ((List<Int>) -> Unit)?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Insights, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.tus_resultados), style = MaterialTheme.typography.titleMedium)
            }
            if (r.respondidas == 0) {
                Text(stringResource(R.string.resultados_pendientes), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${r.suma}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Text(" / ${r.respondidas * 10}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.weight(1f))
                Text("${(r.pct * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(stringResource(Textos.interpretacion(r.pct)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (r.respondidas < r.total) {
                Text(stringResource(R.string.respondidas_de, r.respondidas, r.total), style = MaterialTheme.typography.labelSmall)
            }
            if (r.grupos.size > 1) {
                r.grupos.filter { it.respondidas > 0 }.sortedBy { it.pct }.forEach { g ->
                    val color = g.eje?.color?.asColor() ?: MaterialTheme.colorScheme.secondary
                    Column {
                        Row {
                            Text(g.eje?.texto() ?: g.titulo.orEmpty(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("%.1f/10".format(g.promedio), style = MaterialTheme.typography.labelLarge, color = color)
                        }
                        ProgressLine(g.pct, color = color)
                    }
                }
                val debil = r.grupos.filter { it.respondidas > 0 }.minByOrNull { it.pct }
                debil?.let {
                    Text(stringResource(R.string.foco_sugerido, it.eje?.texto() ?: it.titulo.orEmpty()), style = MaterialTheme.typography.bodyMedium)
                }
            }
            r.ejes?.let { valores ->
                Spacer(Modifier.height(4.dp))
                RadarEjes(valores)
                if (onGuardarEjes != null) {
                    Button(onClick = { onGuardarEjes(valores) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.guardar_en_mis_ejes)) }
                }
            }
        }
    }
}
