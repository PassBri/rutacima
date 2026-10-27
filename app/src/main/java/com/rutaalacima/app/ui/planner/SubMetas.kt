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
import androidx.compose.ui.focus.onFocusChanged
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

/** Un paso del plan de acción de una meta: texto y si ya se hizo. */
internal data class PasoPlan(val t: String, val h: Boolean)

/** Plan de acción de cada meta del año (igual que la web): respuesta "plan:a:<id>" del libro "metas", con [{t, h}]. */
internal object PlanMeta {
    const val WB = "metas"
    fun clave(id: Long) = "plan:a:$id"
    fun leer(v: String?): List<PasoPlan> = runCatching {
        val a = org.json.JSONArray(v.orEmpty().ifBlank { "[]" })
        (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { o -> o.optString("t").takeIf { it.isNotBlank() }?.let { PasoPlan(it, o.optBoolean("h")) } } }
    }.getOrDefault(emptyList())
    fun escribir(ps: List<PasoPlan>): String = if (ps.isEmpty()) "" else org.json.JSONArray().apply {
        ps.forEach { put(org.json.JSONObject().put("t", it.t).put("h", it.h)) }
    }.toString()
}

/** El plan de acción de una meta: los pasos que la persona quiera, cada uno con su casilla, editar y quitar. */
@Composable
internal fun PlanMetaEditor(pasos: List<PasoPlan>, onCambio: (List<PasoPlan>) -> Unit) {
    var nuevo by remember { mutableStateOf("") }
    val hechos = pasos.count { it.h }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.plan_accion) + if (pasos.isNotEmpty()) " · " + stringResource(R.string.plan_hechos, hechos, pasos.size) else "",
            style = MaterialTheme.typography.labelLarge)
        if (pasos.isNotEmpty()) androidx.compose.material3.LinearProgressIndicator(progress = { hechos.toFloat() / pasos.size }, modifier = Modifier.fillMaxWidth())
        else Text(stringResource(R.string.plan_vacio), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        pasos.forEachIndexed { i, p ->
            Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = if (p.h) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledIconToggleButton(checked = p.h, onCheckedChange = { si -> onCambio(pasos.toMutableList().also { it[i] = p.copy(h = si) }) }) {
                        Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                    }
                    // El texto se edita aquí y se guarda al salir del campo (o con Listo), sin saltos del cursor
                    var t by remember(i, p.t) { mutableStateOf(p.t) }
                    fun guardarTexto() { if (t.trim() != p.t) onCambio(pasos.toMutableList().also { l -> if (t.isBlank()) l.removeAt(i) else l[i] = p.copy(t = t.trim()) }) }
                    TextField(t, { t = it }, Modifier.weight(1f).onFocusChanged { if (!it.isFocused) guardarTexto() }, singleLine = true,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { guardarTexto() }),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent))
                    IconButton(onClick = { onCambio(pasos.toMutableList().also { it.removeAt(i) }) }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.eliminar_paso), Modifier.size(18.dp))
                    }
                }
            }
        }
        fun agregar() { val t = nuevo.trim(); if (t.isNotEmpty()) { onCambio(pasos + PasoPlan(t.take(200), false)); nuevo = "" } }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(nuevo, { nuevo = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text(stringResource(R.string.nuevo_paso)) },
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { agregar() }),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done))
            FilledTonalButton(onClick = { agregar() }, enabled = nuevo.isNotBlank()) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Text(stringResource(R.string.submeta_agregar), Modifier.padding(start = 4.dp))
            }
        }
    }
}
