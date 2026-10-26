package com.rutaalacima.app.ui.finanzas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Cuentas
import com.rutaalacima.app.domain.model.Finanzas
import com.rutaalacima.app.domain.model.MetasFin
import com.rutaalacima.app.domain.model.Cuentas.Tipo
import com.rutaalacima.app.ui.components.RutaCard
import java.time.YearMonth

/** Metas financieras: lo que la persona quiere lograr con su dinero (igual que la web). */
private fun iconoMeta(t: String): ImageVector = when (t) {
    "emergencia" -> Icons.Filled.Shield; "viaje" -> Icons.Filled.Flight; "vivienda" -> Icons.Filled.Home; "educacion" -> Icons.Filled.School
    "vehiculo" -> Icons.Filled.DirectionsCar; "retiro" -> Icons.Filled.BeachAccess; "compra" -> Icons.Filled.ShoppingBag; else -> Icons.Filled.Flag
}
@Composable
private fun nombreTipoMeta(t: String) = stringResource(when (t) {
    "emergencia" -> R.string.mf_t_emergencia; "viaje" -> R.string.mf_t_viaje; "vivienda" -> R.string.mf_t_vivienda; "educacion" -> R.string.mf_t_educacion
    "vehiculo" -> R.string.mf_t_vehiculo; "retiro" -> R.string.mf_t_retiro; "compra" -> R.string.mf_t_compra; else -> R.string.mf_t_otra
})
private fun mesAnio(ym: String) = runCatching { YearMonth.parse(ym).let { com.rutaalacima.app.ui.ruta.mesEnFrase(it.monthValue) + " " + it.year } }.getOrDefault(ym)

/** En el balance: cuántas metas y cuánto llevan. */
@Composable
fun MetasFinResumen(libro: Libro, plata: (Double) -> String, onAbrir: () -> Unit) {
    val l = libro.metas
    val obj = l.sumOf { it.meta.objetivo }; val aho = l.sumOf { minOf(it.ahorrado, it.meta.objetivo) }
    RutaCard(onClick = onAbrir) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Savings, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.mf_titulo), style = MaterialTheme.typography.titleSmall)
                Text(if (l.isEmpty()) stringResource(R.string.mf_vacio_corto) else stringResource(R.string.mf_resumen, l.size, plata(aho), plata(obj)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun LazyListScope.metasFinNivel(libro: Libro, plata: (Double) -> String, vm: CuentasViewModel) {
    val l = libro.metas
    if (l.isNotEmpty()) item(key = "mf-cifras") {
        val obj = l.sumOf { it.meta.objetivo }; val aho = l.sumOf { minOf(it.ahorrado, it.meta.objetivo) }
        val mes = l.filter { it.estado != MetasFin.Estado.LOGRADA }.sumOf { it.aporteMes }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Cifra(stringResource(R.string.mf_ahorrado), plata(aho), Modifier.weight(1f))
                Cifra(stringResource(R.string.mf_reunir), plata(obj), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Cifra(stringResource(R.string.mf_falta), plata((obj - aho).coerceAtLeast(0.0)), Modifier.weight(1f))
                Cifra(stringResource(R.string.mf_aporta_mes), plata(Math.ceil(mes)), Modifier.weight(1f), destacada = true)
            }
        }
    }
    // Sugerencia: el fondo de emergencia primero (de 3 a 6 meses de gastos fijos)
    val r = libro.salud(null); val fijosMes = r.fijos / r.meses.coerceAtLeast(1)
    if (l.none { it.meta.tipo == "emergencia" } && fijosMes > 0) item(key = "mf-fondo") {
        RutaCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.mf_fondo_titulo), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.mf_fondo_d, plata(Math.round(fijosMes).toDouble()), plata(Math.round(fijosMes * 3).toDouble()), plata(Math.round(fijosMes * 6).toDouble())),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val nombre = stringResource(R.string.mf_t_emergencia)
            Button(onClick = { vm.metaAgregar("emergencia", nombre, Math.round(fijosMes * 6).toDouble(), YearMonth.now().plusMonths(11).toString(), 0.0) },
                modifier = Modifier.padding(top = 10.dp)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.mf_crear_fondo)) }
        }
    }
    if (l.isEmpty()) item(key = "mf-vacio") {
        RutaCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Savings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Text(stringResource(R.string.mf_vacio_t), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.mf_vacio_d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    items(l, key = { "mf-${it.meta.id}" }) { h -> TarjetaMeta(h, plata, vm) }
    item(key = "mf-nueva") { NuevaMeta(vm) }
    item(key = "mf-nota") { Text(stringResource(R.string.mf_nota), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun TarjetaMeta(h: MetasFin.Hoy, plata: (Double) -> String, vm: CuentasViewModel) {
    val x = h.meta
    var editar by remember { mutableStateOf(false) }
    var mover by remember { mutableStateOf<Boolean?>(null) }   // true: retirar, false: aportar
    val (estTxt, estColor) = when (h.estado) {
        MetasFin.Estado.LOGRADA -> R.string.mf_e_lograda to colorTipo(Tipo.ING); MetasFin.Estado.A_TIEMPO -> R.string.mf_e_tiempo to colorTipo(Tipo.ING)
        MetasFin.Estado.ATRASADA -> R.string.mf_e_atrasada to colorMochila(Cuentas.Mochila.AMARILLA); MetasFin.Estado.VENCIDA -> R.string.mf_e_vencida to MaterialTheme.colorScheme.error
        MetasFin.Estado.SIN_FECHA -> R.string.mf_e_sinfecha to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val quitada = stringResource(R.string.cu_quitado_n, x.nombre); val deshacer = stringResource(R.string.cu_deshacer)
    RutaCard(onClick = { editar = true }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colorTipo(Tipo.ING).copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(iconoMeta(x.tipo), null, tint = colorTipo(Tipo.ING))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(x.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(nombreTipoMeta(x.tipo), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(estTxt), Modifier.clip(RoundedCornerShape(8.dp)).background(estColor.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = estColor)
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { h.pct.toFloat() }, modifier = Modifier.size(64.dp), strokeWidth = 7.dp, color = colorTipo(Tipo.ING),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                Text("${Math.round(h.pct * 100)} %", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(stringResource(R.string.mf_de, plata(h.ahorrado), plata(x.objetivo)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(if (h.falta > 0) stringResource(R.string.mf_faltan, plata(h.falta)) else stringResource(R.string.mf_completa),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (h.estado != MetasFin.Estado.LOGRADA) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(10.dp)) {
                Text(stringResource(R.string.mf_aporta_mes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (h.meses != null && h.meses > 0) plata(Math.ceil(h.aporteMes)) else "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(when {
                    x.fecha.isEmpty() -> stringResource(R.string.mf_sin_fecha_d)
                    h.meses != null && h.meses > 0 -> stringResource(R.string.mf_meses_para, h.meses, mesAnio(x.fecha))
                    else -> stringResource(R.string.mf_fecha_paso)
                }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(10.dp)) {
                Text(stringResource(R.string.mf_ritmo), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (h.ritmo > 0) plata(Math.round(h.ritmo).toDouble()) else "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(h.llegaEn?.let { stringResource(R.string.mf_llegas, mesAnio(YearMonth.now().plusMonths((it - 1).toLong()).toString())) } ?: stringResource(R.string.mf_sin_aportes),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.metaQuitar(x.id); com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(quitada, deshacer) { vm.metaRestaurar(x) } }) {
                Icon(Icons.Filled.Delete, stringResource(R.string.mf_quitar), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            if (h.ahorrado > 0) OutlinedButton(onClick = { mover = true }) { Text(stringResource(R.string.mf_retirar)) }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = { mover = false }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.mf_aportar)) }
        }
    }
    mover?.let { retirar ->
        val ctx = LocalContext.current
        var monto by remember { mutableStateOf(Cuentas.numero(if (retirar) h.ahorrado else Math.ceil(h.aporteMes)).takeIf { it != "0" } ?: "") }
        val v = kotlin.math.abs(Finanzas.monto(monto)).let { if (retirar) minOf(it, h.ahorrado) else it }
        AlertDialog(onDismissRequest = { mover = null }, title = { Text(stringResource(if (retirar) R.string.mf_retirar_de else R.string.mf_aportar_a, x.nombre)) },
            text = { Column {
                Text(stringResource(R.string.mf_llevas, plata(h.ahorrado), plata(x.objetivo)), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(monto, { monto = it }, label = { Text(stringResource(R.string.cu_monto)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text(stringResource(if (retirar) R.string.mf_retiro_d else R.string.mf_aporte_d)) }, modifier = Modifier.padding(top = 8.dp))
            } },
            confirmButton = { TextButton(enabled = v > 0, onClick = {
                vm.metaMover(x, v, retirar, ctx.getString(if (retirar) R.string.mf_retiro_de else R.string.mf_aporte_a, x.nombre)); mover = null
                com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(ctx.getString(if (retirar) R.string.mf_retiraste else R.string.mf_aportaste, plata(v)))
            }) { Text(stringResource(if (retirar) R.string.mf_retirar else R.string.mf_aportar)) } },
            dismissButton = { TextButton(onClick = { mover = null }) { Text(stringResource(R.string.cancelar)) } })
    }
    if (editar) {
        var nombre by remember { mutableStateOf(x.nombre) }; var objetivo by remember { mutableStateOf(Cuentas.numero(x.objetivo)) }
        var fecha by remember { mutableStateOf(x.fecha) }
        AlertDialog(onDismissRequest = { editar = false }, title = { Text(stringResource(R.string.mf_editar)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nombre, { nombre = it.take(60) }, label = { Text(stringResource(R.string.mf_nombre)) }, singleLine = true)
                OutlinedTextField(objetivo, { objetivo = it }, label = { Text(stringResource(R.string.mf_cuanto)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(fecha, { fecha = it.take(7) }, label = { Text(stringResource(R.string.mf_cuando)) }, placeholder = { Text("2027-06") }, singleLine = true,
                    supportingText = { Text(stringResource(R.string.mf_cuando_d)) })
            } },
            confirmButton = { TextButton(onClick = {
                val o = kotlin.math.abs(Finanzas.monto(objetivo))
                vm.metaCambiar(x.id) { it.copy(nombre = nombre.trim().ifBlank { x.nombre }, objetivo = if (o > 0) o else it.objetivo, fecha = if (Regex("\\d{4}-\\d{2}").matches(fecha)) fecha else "") }
                editar = false }) { Text(stringResource(R.string.guardar)) } },
            dismissButton = { TextButton(onClick = { editar = false }) { Text(stringResource(R.string.cancelar)) } })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NuevaMeta(vm: CuentasViewModel) {
    var tipo by remember { mutableStateOf("viaje") }
    var nombre by remember { mutableStateOf("") }; var objetivo by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }; var inicial by remember { mutableStateOf("") }
    val o = kotlin.math.abs(Finanzas.monto(objetivo))
    val porDefecto = nombreTipoMeta(tipo); val creada = stringResource(R.string.mf_creada)
    RutaCard {
        Text(stringResource(R.string.mf_nueva), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.mf_nueva_d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetasFin.TIPOS.forEach { (t, _) ->
                val sel = tipo == t
                Row(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .background(if (sel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface).clickable { tipo = t }.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(iconoMeta(t), null, Modifier.size(18.dp), tint = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp)); Text(nombreTipoMeta(t), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        OutlinedTextField(nombre, { nombre = it.take(60) }, label = { Text(stringResource(R.string.mf_nombre)) }, placeholder = { Text(porDefecto) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            OutlinedTextField(objetivo, { objetivo = it }, label = { Text(stringResource(R.string.mf_cuanto)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            OutlinedTextField(fecha, { fecha = it.take(7) }, label = { Text(stringResource(R.string.mf_cuando)) }, placeholder = { Text("2027-06") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(inicial, { inicial = it }, label = { Text(stringResource(R.string.mf_ya_tengo)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Button(enabled = o > 0, onClick = {
            vm.metaAgregar(tipo, nombre.ifBlank { porDefecto }, o, if (Regex("\\d{4}-\\d{2}").matches(fecha)) fecha else "", kotlin.math.abs(Finanzas.monto(inicial)))
            nombre = ""; objetivo = ""; fecha = ""; inicial = ""; com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(creada)
        }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text(stringResource(R.string.mf_crear)) }
    }
}
