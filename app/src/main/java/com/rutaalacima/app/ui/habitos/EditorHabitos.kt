package com.rutaalacima.app.ui.habitos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.RutaApp
import com.rutaalacima.app.domain.model.ChecklistDiario
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.HabitoDiario
import com.rutaalacima.app.ui.i18n.Textos
import com.rutaalacima.app.ui.i18n.texto
import kotlinx.coroutines.launch

/** El texto que se ve de un hábito: el que escribió la persona o el de fábrica, traducido. */
@Composable
fun HabitoDiario.textoVisible(): String = texto.ifBlank { stringResource(Textos.habito(id)) }

/**
 * Editar el checklist de hábitos: por eje se renombra, se agrega (hasta 6) y se quita (queda al menos 1).
 * Se guarda al instante y se sincroniza con la web; los días ya marcados se conservan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorHabitos(onCerrar: () -> Unit) {
    val c = (LocalContext.current.applicationContext as RutaApp).container
    val scope = rememberCoroutineScope()
    val lista = ChecklistDiario.habitos
    val nuevos = remember { mutableStateMapOf<Eje, String>() }
    var confirmarFabrica by remember { mutableStateOf(false) }
    fun guardar(l: List<HabitoDiario>) {
        ChecklistDiario.aplicar(ChecklistDiario.aJson(l))
        scope.launch { c.respuestas.guardar(ChecklistDiario.WORKBOOK, ChecklistDiario.CLAVE, ChecklistDiario.aJson(l)) }
    }
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = { WindowInsets.ime.union(WindowInsets.navigationBars) },
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.habitos_editar_titulo), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.habitos_editar_ayuda), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            Eje.entries.forEach { eje ->
                val delEje = lista.filter { it.eje == eje }
                Text(eje.texto(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                delEje.forEach { h ->
                    val visible = h.textoVisible()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = visible,
                            onValueChange = { t -> guardar(ChecklistDiario.renombrar(ChecklistDiario.habitos, h.id, t)) },
                            modifier = Modifier.weight(1f), singleLine = false, maxLines = 3,
                        )
                        IconButton(onClick = { guardar(ChecklistDiario.quitar(ChecklistDiario.habitos, h.id)) }, enabled = delEje.size > 1) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.habitos_quitar))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (delEje.size < ChecklistDiario.MAX_POR_EJE) Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nuevos[eje].orEmpty(), onValueChange = { nuevos[eje] = it.take(120) },
                        placeholder = { Text(stringResource(R.string.habitos_nuevo)) }, modifier = Modifier.weight(1f), singleLine = true,
                    )
                    IconButton(onClick = {
                        val t = nuevos[eje].orEmpty(); if (t.isNotBlank()) { guardar(ChecklistDiario.agregar(ChecklistDiario.habitos, eje, t)); nuevos[eje] = "" }
                    }, enabled = !nuevos[eje].isNullOrBlank()) { Icon(Icons.Filled.Add, stringResource(R.string.habitos_agregar)) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (confirmarFabrica) TextButton(onClick = { guardar(ChecklistDiario.base); confirmarFabrica = false }) { Text(stringResource(R.string.habitos_fabrica_si)) }
                else TextButton(onClick = { confirmarFabrica = true }) { Text(stringResource(R.string.habitos_fabrica)) }
                TextButton(onClick = onCerrar) { Text(stringResource(R.string.listo)) }
            }
        }
    }
}
