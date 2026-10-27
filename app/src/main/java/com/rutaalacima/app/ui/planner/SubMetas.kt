package com.rutaalacima.app.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.data.vision.SincroBrujula
import com.rutaalacima.app.domain.model.Mandala

/**
 * Sub-metas una por una (igual que la web): cada una con su número de paso en la Brújula, su casilla de ganado y quitar;
 * abajo, agregar. Con Brújula, las 8 primeras son los pasos 1 a 8; desde la 9 se quedan solo en Metas.
 */
@Composable
internal fun SubMetasEditor(
    valor: String,
    onCambio: (String) -> Unit,
    casillaId: Long?,
    mandala: Map<String, String>,
    onGanado: (Int) -> Unit,
) {
    val ls = SincroBrujula.lineas(valor)
    val enBrujula = casillaId != null
    var nueva by remember { mutableStateOf("") }
    fun poner(i: Int, t: String) {
        val x = ls.toMutableList(); while (x.size <= i) x.add("")
        x[i] = t
        if (t.isBlank() && (!enBrujula || i >= SincroBrujula.PASOS)) x.removeAt(i)
        onCambio(SincroBrujula.unir(x))
    }
    fun agregar() {
        val t = nueva.trim(); if (t.isEmpty()) return
        var i = if (enBrujula) ls.take(SincroBrujula.PASOS).indexOfFirst { it.isBlank() } else -1
        if (enBrujula && i < 0 && ls.size < SincroBrujula.PASOS) i = ls.size
        if (i < 0) i = maxOf(ls.size, if (enBrujula) SincroBrujula.PASOS else 0)
        poner(i, t); nueva = ""
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.submetas_titulo), style = MaterialTheme.typography.labelLarge)
        ls.forEachIndexed { i, t ->
            if (t.isBlank()) return@forEachIndexed
            val paso = enBrujula && i < SincroBrujula.PASOS
            val ganado = paso && casillaId != null && !mandala[Mandala.claveHecho(casillaId, i)].isNullOrBlank()
            Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = if (ganado) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (paso) FilledIconToggleButton(checked = ganado, onCheckedChange = { onGanado(i) }) {
                        Icon(Icons.Filled.Check, stringResource(R.string.submeta_ganado), Modifier.size(18.dp))
                    }
                    TextField(t, { poner(i, it) }, Modifier.weight(1f), singleLine = true,
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent))
                    Text(if (paso) stringResource(R.string.submeta_paso, i + 1) else if (enBrujula) stringResource(R.string.submeta_solo_metas) else "",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { poner(i, "") }) { Icon(Icons.Filled.Close, stringResource(R.string.submeta_quitar), Modifier.size(18.dp)) }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(nueva, { nueva = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text(stringResource(R.string.submeta_nueva)) },
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { agregar() }),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done))
            FilledTonalButton(onClick = { agregar() }, enabled = nueva.isNotBlank()) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Text(stringResource(R.string.submeta_agregar), Modifier.padding(start = 4.dp))
            }
        }
        if (enBrujula) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Explore, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.submeta_sincro), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
