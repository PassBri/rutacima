package com.rutaalacima.app.ui.cordadas

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.diasMarcados
import com.rutaalacima.app.data.social.Cordada
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/*
 * Cordadas unidas a metas del mes (igual que en la web): la respuesta "cordada#<id>#meta" (workbook "cordada")
 * guarda el id de la meta del mes; al marcar el día en la cordada, ese día también cuenta en la meta.
 */
const val WB_CORDADA = "cordada"
fun claveMetaCordada(id: String) = "cordada#$id#meta"

/** Marca el día en la meta del mes unida a la cordada. Devuelve true si había una meta de ese mes. */
suspend fun marcarMetaDeCordada(c: AppContainer, id: String, dia: LocalDate): Boolean {
    val metaId = c.respuestas.cargar(WB_CORDADA)[claveMetaCordada(id)]?.toLongOrNull() ?: return false
    val m = c.planAnual.todasMetasMes.first().firstOrNull { it.id == metaId && it.anio == dia.year && it.mes == dia.monthValue } ?: return false
    if (dia.dayOfMonth !in m.diasMarcados()) c.planAnual.alternarDia(m, dia.dayOfMonth)
    return true
}

/** Selector de la meta del mes que mueve una cordada (bandera rellena si ya tiene una). */
@Composable
fun SelectorMetaCordada(actual: String, metas: List<MetaMensualEntity>, onElegir: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val elegida = metas.firstOrNull { it.id.toString() == actual }
    Column {
        Box {
            TextButton(onClick = { abierto = true }) {
                Icon(if (elegida != null) Icons.Filled.Flag else Icons.Outlined.Flag, null, Modifier.size(18.dp))
                Text("  " + (elegida?.texto ?: stringResource(R.string.meta_que_mueve)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.sin_meta)) }, onClick = { onElegir(""); abierto = false })
                metas.forEach { m -> DropdownMenuItem(text = { Text(m.texto, maxLines = 2) }, onClick = { onElegir(m.id.toString()); abierto = false }) }
            }
        }
        Text(stringResource(R.string.cord_meta_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp))
    }
}

class CordadasHoyViewModel(private val c: AppContainer) : ViewModel() {
    var lista by mutableStateOf<List<Cordada>>(emptyList())
        private set

    fun cargar() = viewModelScope.launch {
        if (!c.cordadas.disponible) return@launch
        runCatching { c.cordadas.mias() }.onSuccess { l -> lista = l.filter { it.activa() } }
    }

    /** Marca o desmarca hoy; al marcar, también la meta del mes unida. Devuelve por [alTerminar] si movió una meta. */
    fun marcar(k: Cordada, alTerminar: (Boolean) -> Unit) = viewModelScope.launch {
        val si = !k.marqueHoy
        lista = lista.map { if (it.id == k.id) it.copy(marqueHoy = si) else it }
        runCatching { c.cordadas.marcar(k.id, LocalDate.now(), si) }
        val movio = si && runCatching { marcarMetaDeCordada(c, k.id, LocalDate.now()) }.getOrDefault(false)
        if (si) alTerminar(movio)
        cargar()
    }
}

/** Tus cordadas activas, para marcar el día sin salir de Hoy (igual que en la web). No se muestra si no hay ninguna. */
@Composable
fun CordadasHoy(onVerTodas: () -> Unit, modifier: Modifier = Modifier) {
    val vm = rutaViewModel { CordadasHoyViewModel(it) }
    LaunchedEffect(Unit) { vm.cargar() }
    val ctx = LocalContext.current
    if (vm.lista.isEmpty()) return
    RutaCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cord_hoy_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onVerTodas) { Text(stringResource(R.string.cord_ver_todas)) }
        }
        Text(stringResource(R.string.cord_hoy_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        vm.lista.forEach { k ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(k.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(k.reto + " · " + stringResource(R.string.cordadas_dia_de, k.diaDelReto(), k.dias), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                val listo = stringResource(R.string.cordadas_hoy_listo)
                val conMeta = stringResource(R.string.cord_marcado_meta)
                IconButton(onClick = { vm.marcar(k) { movio -> Toast.makeText(ctx, if (movio) conMeta else listo, Toast.LENGTH_SHORT).show() } }) {
                    Icon(if (k.marqueHoy) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, stringResource(R.string.cordadas_marcar_hoy),
                        tint = if (k.marqueHoy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}
