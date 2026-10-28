package com.rutaalacima.app.ui.ruta

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.ui.components.RutaCard

/** Las metas del año que no son campamento de su Brújula (la primera de cada eje lo es; sin eje = CON), igual que la web. */
internal fun otrasMetasDelAnio(delAnio: List<MetaAnualEntity>): List<MetaAnualEntity> {
    val principales = delAnio.groupBy { it.eje ?: "CON" }.filterKeys { it in Mandala.CAMPAMENTOS_FIJOS }
        .values.map { g -> g.minByOrNull { it.id }!!.id }.toSet()
    return delAnio.filter { it.id !in principales }
}

/** Una "otra meta" del año: su nombre, y ahí mismo editar, copiar, borrar (con confirmación) y abrir. */
@Composable
internal fun FilaOtraMeta(m: MetaAnualEntity, onAbrir: () -> Unit, onRenombrar: (String) -> Unit, onCopiar: () -> Unit, onBorrar: () -> Unit) {
    var editar by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf(false) }
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(m.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 4.dp))
            IconButton(onClick = { editar = true }) { Icon(Icons.Outlined.Edit, stringResource(R.string.editar)) }
            IconButton(onClick = onCopiar) { Icon(Icons.Outlined.ContentCopy, stringResource(R.string.om_copiar)) }
            IconButton(onClick = { borrar = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.borrar), tint = MaterialTheme.colorScheme.error) }
            IconButton(onClick = onAbrir) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
        }
    }
    if (editar) {
        var t by remember { mutableStateOf(m.titulo) }
        AlertDialog(onDismissRequest = { editar = false },
            title = { Text(stringResource(R.string.editar)) },
            text = { OutlinedTextField(t, { t = it.take(120) }, singleLine = true) },
            confirmButton = { TextButton(enabled = t.isNotBlank(), onClick = { onRenombrar(t.trim()); editar = false }) { Text(stringResource(R.string.guardar)) } },
            dismissButton = { TextButton(onClick = { editar = false }) { Text(stringResource(R.string.cancelar)) } })
    }
    if (borrar) AlertDialog(onDismissRequest = { borrar = false },
        title = { Text(stringResource(R.string.om_borrar_titulo)) },
        text = { Text(stringResource(R.string.om_borrar_texto, m.titulo)) },
        confirmButton = { TextButton(onClick = { onBorrar(); borrar = false }) { Text(stringResource(R.string.borrar)) } },
        dismissButton = { TextButton(onClick = { borrar = false }) { Text(stringResource(R.string.cancelar)) } })
}
