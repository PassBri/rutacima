package com.rutaalacima.app.ui.finanzas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Cuentas
import com.rutaalacima.app.domain.model.Cuentas.Tipo
import com.rutaalacima.app.domain.model.Finanzas
import com.rutaalacima.app.domain.model.Fijos
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.planner.nombreMes
import com.rutaalacima.app.util.hoy
import java.time.YearMonth

/**
 * Fijos de cada mes (igual que la web): lo que se repite solo cada mes en su día. Tocar uno lo edita:
 * el concepto y el día cambian siempre; el monto rige desde este mes. La X deja de repetirlo.
 */
@Composable
fun FijosCard(libro: Libro, ym: YearMonth?, plata: (Double) -> String, vm: CuentasViewModel) {
    val base = (ym ?: YearMonth.from(hoy())).toString()
    val l = libro.fijos.filter { (it.hasta.isEmpty() || it.hasta >= base) && it.desde <= (ym?.toString() ?: "9999-12") }
    var editando by remember { mutableStateOf<Fijos.Fijo?>(null) }
    val ctx = LocalContext.current
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Repeat, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.cu_fijos_titulo), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.cu_fijos_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (l.isEmpty()) Text(stringResource(R.string.cu_fijos_vacio), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        else {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                l.forEach { x ->
                    val t = Tipo.de(x.t)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable(onClickLabel = stringResource(R.string.cu_fijo_editar)) { editando = x }.padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(nombreTipo(t, plural = false), Modifier.clip(RoundedCornerShape(50)).background(colorTipo(t).copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium, color = colorTipo(t))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(x.n.ifBlank { nombreCategoria(t, x.c) }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.cu_fijo_dia, x.dia) + " · " + stringResource(R.string.cu_fijo_desde, mesCorto(x.desde)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(plata(x.m), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        val parado = stringResource(R.string.cu_fijo_parado)
                        IconButton(onClick = { vm.fijoParar(x.id); com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(parado) }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.cu_fijo_parar), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            val porTipo = Tipo.entries.mapNotNull { t -> l.filter { it.t == t.codigo }.sumOf { it.m }.takeIf { it > 0 }?.let { nombreTipo(t) + " " + plata(it) } }
            Text(stringResource(R.string.cu_cada_mes) + ": " + porTipo.joinToString(" · "), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
            Text(stringResource(R.string.cu_fijos_nota), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
    editando?.let { x -> EditarFijo(x, plata, vm) { editando = null } }
}

private fun mesCorto(ym: String) = runCatching { nombreMes(ym.drop(5).toInt()).take(3) + " " + ym.take(4) }.getOrDefault(ym)

@Composable
private fun EditarFijo(x: Fijos.Fijo, plata: (Double) -> String, vm: CuentasViewModel, onCerrar: () -> Unit) {
    var monto by remember { mutableStateOf(Cuentas.numero(x.m)) }
    var nota by remember { mutableStateOf(x.n) }
    var dia by remember { mutableStateOf(x.dia.toString()) }
    val v = kotlin.math.abs(Finanzas.monto(monto)); val d = dia.toIntOrNull()
    val ok = v > 0 && d != null && d in 1..31
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Icon(Icons.Filled.Repeat, null) },
        title = { Text(stringResource(R.string.cu_fijo_editar)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nota, { nota = it.take(120) }, label = { Text(stringResource(R.string.cu_concepto)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(monto, { monto = it }, label = { Text(stringResource(R.string.cu_monto)) }, singleLine = true, isError = v == 0.0,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), supportingText = { Text(stringResource(R.string.cu_fijos_nota_monto)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(dia, { dia = it.filter(Char::isDigit).take(2) }, label = { Text(stringResource(R.string.cu_dia_mes)) }, singleLine = true, isError = !(d != null && d in 1..31),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = ok, onClick = {
                vm.fijoEditar(x.id, nota.trim(), d!!, v)
                if (v != x.m) { com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(ctx.getString(R.string.cu_fijo_monto_desde, nombreMes(hoy().monthValue), plata(v))) }
                onCerrar()
            }) { Text(stringResource(R.string.guardar)) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}
