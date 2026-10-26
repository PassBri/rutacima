package com.rutaalacima.app.ui.finanzas

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Cuentas
import com.rutaalacima.app.domain.model.Cuentas.Mov
import com.rutaalacima.app.domain.model.Cuentas.Tipo
import com.rutaalacima.app.domain.model.Deudas
import com.rutaalacima.app.domain.model.Finanzas
import com.rutaalacima.app.domain.model.Patrimonio
import com.rutaalacima.app.domain.model.Salud
import com.rutaalacima.app.seguridad.Bloqueo
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.util.hoy
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

// ====================================================================== Bloqueo

/** El módulo cerrado: se abre con huella, rostro o el PIN del teléfono (o la clave propia de la app). */
@Composable
fun PantallaBloqueo(modifier: Modifier = Modifier, onAbrir: () -> Unit) {
    val ctx = LocalContext.current
    val bio = remember { Bloqueo.biometriaDisponible(ctx) }
    val clave = remember { Bloqueo.tieneClave(ctx) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val titulo = stringResource(R.string.cu_bloqueado_titulo); val sub = stringResource(R.string.cu_bloqueado_sub)
    val abrirBio = {
        Bloqueo.actividad(ctx)?.let { a -> Bloqueo.pedirBiometria(a, titulo, sub, onExito = onAbrir, onError = { error = it }) } ?: onAbrir()
    }
    LaunchedEffect(Unit) { if (bio) abrirBio() }
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Lock, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        when {
            bio -> Button(onClick = abrirBio) { Icon(Icons.Filled.Fingerprint, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.cu_abrir_huella)) }
            clave -> {
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text(stringResource(R.string.cu_clave)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                val mal = stringResource(R.string.cu_clave_mal)
                Button(onClick = { if (Bloqueo.verificarClave(ctx, pin)) onAbrir() else { error = mal; pin = "" } }, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.cu_abrir)) }
            }
            else -> {
                Text(stringResource(R.string.cu_sin_bloqueo_tel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(onClick = onAbrir, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.cu_abrir)) }
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
    }
}

// ====================================================================== Salud financiera

private fun pct(v: Double) = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 1 }.format(v * 100) + " %"
private fun num(v: Double) = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 2 }.format(v)

@Composable
private fun nombreIndicador(k: String): Pair<String, String> {
    val ctx = LocalContext.current
    fun r(n: String) = ctx.resources.getIdentifier(n, "string", ctx.packageName).let { if (it != 0) ctx.getString(it) else k }
    return r("cu_ind_$k") to r("cu_ind_${k}_d")
}

@Composable
private fun valorIndicador(i: Salud.Indicador): String = when (i.clave) {
    "liquidez", "libertad", "netoing" -> num(i.valor)
    "fondo" -> stringResource(R.string.cu_meses_x, num(i.valor))
    "endeuda" -> if (i.valor.isInfinite()) stringResource(R.string.cu_sin_activos) else if (i.valor == 0.0) stringResource(R.string.cu_sin_deudas) else pct(i.valor)
    else -> pct(i.valor)
}

@Composable
fun colorEstado(e: Salud.Estado) = when (e) {
    Salud.Estado.OK -> colorTipo(Tipo.ING)
    Salud.Estado.MEDIO -> colorMochila(Cuentas.Mochila.AMARILLA)
    Salud.Estado.BAJO -> MaterialTheme.colorScheme.error
    Salud.Estado.NA -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Salud financiera: el Índice (ISF) con su escala, cada peso que entra, los medidores, lo que tienes y debes, mes a mes y las cifras. */
@Composable
fun SaludCard(r: Salud.Resultado, anio: Int?, libro: Libro, plata: (Double) -> String) {
    if (r.suma.n == 0) return
    val ctx = LocalContext.current
    var cifras by remember { mutableStateOf(false) }
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cu_salud), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (libro.publica) {
                val texto = textoCompartir(r, anio, libro)
                IconButton(onClick = { (ctx.applicationContext as com.rutaalacima.app.RutaApp).container.navegacionPendiente.value = com.rutaalacima.app.ui.Rutas.publicar("LOGRO", texto = texto) }) {
                    Icon(Icons.Filled.Share, stringResource(R.string.cu_compartir))
                }
            }
        }
        val periodo = anio?.let { "$it" } ?: stringResource(R.string.cu_ultimos_12).lowercase()
        r.isf?.let { isf ->
            val (nom, desc, est) = when (r.nivel!!) {
                Salud.Nivel.EXCELENTE -> Triple(R.string.cu_nivel_excelente, R.string.cu_nivel_excelente_d, Salud.Estado.OK)
                Salud.Nivel.BUENA -> Triple(R.string.cu_nivel_buena, R.string.cu_nivel_buena_d, Salud.Estado.OK)
                Salud.Nivel.MODERADA -> Triple(R.string.cu_nivel_moderada, R.string.cu_nivel_moderada_d, Salud.Estado.MEDIO)
                Salud.Nivel.RIESGO -> Triple(R.string.cu_nivel_riesgo, R.string.cu_nivel_riesgo_d, Salud.Estado.BAJO)
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    val pista = MaterialTheme.colorScheme.surfaceContainerHighest; val color = MaterialTheme.colorScheme.primary
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val g = 9.dp.toPx()
                        drawArc(pista, 0f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(g))
                        drawArc(color, -90f, 360f * isf / 100f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(g, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$isf", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("ISF %", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(nom), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp)); ChipEstado(est)
                    }
                    Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    EscalaIsf(isf)
                }
            }
            Text(stringResource(R.string.cu_isf_periodo, periodo), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
        BarraPeso(r, plata)
        r.indicadores.forEach { i -> val (n, d) = nombreIndicador(i.clave); Medidor(i, n, d, valorIndicador(i)) }
        if (r.actual) BalanceBarras(r, plata)
        LineaMeses(anio, libro)
        VerCifras(cifras) { cifras = !cifras }
        if (cifras) {
            val filas = listOf(
                Triple(R.string.cu_c_ing_act, r.ingActivos, false), Triple(R.string.cu_c_ing_pas, r.ingPasivos, false), Triple(R.string.cu_c_ing_tot, r.suma.ing, true),
                Triple(R.string.cu_c_fijos, r.fijos, false), Triple(R.string.cu_c_variables, r.variables, false), Triple(R.string.cu_aho, r.suma.aho, false),
                Triple(R.string.cu_inv, r.suma.inv, false), Triple(R.string.cu_c_egresos, r.egresos, true), Triple(R.string.cu_c_flujo, r.flujoNeto, true),
            )
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
                FilaEstado(stringResource(R.string.cu_c_concepto), stringResource(R.string.cu_c_mensual), anio?.let { "$it" } ?: stringResource(R.string.cu_12_meses), cabecera = true)
                filas.forEach { (n, v, fuerte) -> FilaEstado(stringResource(n), plata(Math.round(r.mensual(v)).toDouble()), plata(v), fuerte = fuerte, negativo = v < 0) }
                FilaEstado(stringResource(R.string.cu_c_cuotas), plata(r.deudaMes), "")
                if (r.actual) {
                    FilaEstado(stringResource(R.string.cu_c_activos), "", plata(r.activos), fuerte = true)
                    FilaEstado(stringResource(R.string.cu_c_pasivos), "", plata(r.pasivos))
                    FilaEstado(stringResource(R.string.cu_c_neto), "", plata(r.neto), fuerte = true, negativo = r.neto < 0)
                }
            }
        }
    }
}

@Composable
private fun FilaEstado(etq: String, mensual: String, periodo: String, cabecera: Boolean = false, fuerte: Boolean = false, negativo: Boolean = false) {
    val estilo = if (cabecera) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    val color = when { cabecera -> MaterialTheme.colorScheme.onSurfaceVariant; negativo -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurface }
    val peso = if (fuerte) FontWeight.SemiBold else FontWeight.Normal
    Row(Modifier.fillMaxWidth().background(if (fuerte || cabecera) MaterialTheme.colorScheme.surfaceContainer else androidx.compose.ui.graphics.Color.Transparent)
        .heightIn(min = 36.dp).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etq, Modifier.weight(1.4f), style = estilo, fontWeight = peso, color = if (cabecera) color else MaterialTheme.colorScheme.onSurface)
        Text(mensual, Modifier.weight(1f), style = estilo, fontWeight = peso, color = color, textAlign = TextAlign.End, maxLines = 1)
        Text(periodo, Modifier.weight(1f), style = estilo, fontWeight = peso, color = color, textAlign = TextAlign.End, maxLines = 1)
    }
}

/** El avance para la comunidad: solo porcentajes y meses, nunca montos. */
@Composable
private fun textoCompartir(r: Salud.Resultado, anio: Int?, libro: Libro): String {
    val ctx = LocalContext.current
    val lineas = mutableListOf<String>()
    r.isf?.let { lineas += ctx.getString(R.string.cu_compartir_isf, it) }
    r.indicadores.filter { it.clave in setOf("flujo", "construye", "fondo", "libertad") }.forEach { i ->
        lineas += nombreIndicador(i.clave).first + ": " + valorIndicador(i)
    }
    val plan = Deudas.plan(libro.deudas, libro.deudasDatos.metodo, libro.deudasDatos.extra)
    if (libro.deudas.isNotEmpty()) lineas += if (libro.deudas.all { it.actual <= 0 }) ctx.getString(R.string.cu_compartir_sin_deudas)
        else plan.meses?.let { ctx.getString(R.string.cu_compartir_deudas, it) } ?: ""
    return (lineas.filter { it.isNotBlank() } + "#AscensoFinanciero").joinToString("\n")
}

// ====================================================================== Deudas

/** En el balance: cuánto debes y cuándo quedas libre. */
@Composable
fun DeudasResumen(libro: Libro, plata: (Double) -> String, onAbrir: () -> Unit) {
    val l = libro.deudas
    val debe = l.sumOf { it.actual }
    val plan = Deudas.plan(l, libro.deudasDatos.metodo, libro.deudasDatos.extra)
    RutaCard(onClick = onAbrir) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CreditCard, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.cu_deudas), style = MaterialTheme.typography.titleSmall)
                Text(when {
                    l.isEmpty() -> stringResource(R.string.cu_deudas_vacio)
                    debe <= 0 -> stringResource(R.string.cu_sin_deudas_ya)
                    plan.meses != null -> stringResource(R.string.cu_debes_libre, plata(debe), mesesDespues(plan.meses))
                    else -> stringResource(R.string.cu_no_baja)
                }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

private fun mesesDespues(n: Int): String {
    val ym = YearMonth.from(hoy()).plusMonths(n.toLong())
    return com.rutaalacima.app.ui.ruta.mesEnFrase(ym.monthValue) + " " + ym.year
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.deudasNivel(libro: Libro, plata: (Double) -> String, vm: CuentasViewModel, avisos: SnackbarHostState) {
    val d = libro.deudasDatos; val l = libro.deudas
    val plan = Deudas.plan(l, d.metodo, d.extra); val otro = Deudas.plan(l, if (d.metodo == "bola") "avalancha" else "bola", d.extra)
    val orden = l.filter { it.actual > 0 }.sortedBy { plan.fin[it.deuda.id] ?: Int.MAX_VALUE }
    if (l.isNotEmpty()) item(key = "d-cifras") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Cifra(stringResource(R.string.cu_debes_hoy), plata(l.sumOf { it.actual }), Modifier.weight(1f))
            Cifra(stringResource(R.string.cu_ya_pagaste), plata(l.sumOf { it.pagado }), Modifier.weight(1f))
            Cifra(stringResource(R.string.cu_libre_en), when { l.all { it.actual <= 0 } -> "✓"; plan.meses != null -> stringResource(R.string.cu_meses_x, "${plan.meses}"); else -> "—" }, Modifier.weight(1f), destacada = true)
        }
    }
    // Lo que las deudas le hacen a la salud financiera: el ISF hoy y el mismo cálculo sin deudas
    if (l.isNotEmpty()) item(key = "d-efecto") { EfectoDeudasSalud(libro, plan, plata) }
    item(key = "d-metodo") {
        var extra by remember(d.extra) { mutableStateOf(if (d.extra > 0) Cuentas.numero(d.extra) else "") }
        val planes = remember(l, d.extra) { Deudas.METODOS.associateWith { Deudas.plan(l, it, d.extra) } }
        val base = remember(l) { Deudas.plan(l, "minimos", 0.0) }
        val conFecha = planes.values.filter { it.meses != null }
        val masRapido = conFecha.minOfOrNull { it.meses!! }; val menosInteres = conFecha.minOfOrNull { it.interes }
        val vivas = l.any { it.actual > 0 }
        RutaCard {
            Text(stringResource(R.string.cu_elige_metodo), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.cu_metodo_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(extra, { extra = it; vm.deudaExtra(kotlin.math.abs(Finanzas.monto(it))) }, label = { Text(stringResource(R.string.cu_extra)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp))
            // Cada método es una tarjeta de opción (M3): su idea, cuánto tarda y cuánto cuesta
            val nombres = mapOf("avalancha" to (R.string.cu_avalancha to R.string.cu_avalancha_d), "bola" to (R.string.cu_bola to R.string.cu_bola_d),
                "flujo" to (R.string.cu_flujo to R.string.cu_flujo_d), "hibrido" to (R.string.cu_hibrido to R.string.cu_hibrido_d), "propio" to (R.string.cu_propio to R.string.cu_propio_d))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Deudas.METODOS.forEach { m ->
                    val (n, idea) = nombres.getValue(m); val p = planes.getValue(m); val sel = d.metodo == m
                    androidx.compose.material3.OutlinedCard(
                        modifier = Modifier.fillMaxWidth().selectable(sel, role = androidx.compose.ui.semantics.Role.RadioButton) { vm.deudaMetodo(m) },
                        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(if (sel) 2.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(n), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                if (sel) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(stringResource(idea), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (vivas) Text(if (p.meses != null) stringResource(R.string.cu_metodo_res, stringResource(R.string.cu_meses_x, "${p.meses}"), plata(Math.round(p.interes).toDouble()))
                                else stringResource(R.string.cu_no_baja_corto), style = MaterialTheme.typography.labelLarge)
                            val sellos = listOfNotNull(R.string.cu_mas_rapido.takeIf { vivas && p.meses != null && p.meses == masRapido },
                                R.string.cu_menos_interes.takeIf { vivas && p.meses != null && menosInteres != null && kotlin.math.abs(p.interes - menosInteres) < 1 })
                            if (sellos.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                sellos.forEach { s -> Text(stringResource(s), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) }
                            }
                        }
                    }
                }
            }
            if (vivas) Column(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp)) {
                if (plan.meses != null) {
                    Text(stringResource(R.string.cu_plan_libre, mesesDespues(plan.meses), plan.meses, plata(Math.round(plan.interes).toDouble())), style = MaterialTheme.typography.bodyMedium)
                    when {
                        base.meses == null -> Text(stringResource(R.string.cu_minimos_nunca), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        base.meses > plan.meses -> Text(stringResource(R.string.cu_vs_minimos, stringResource(R.string.cu_meses_x, "${base.meses}"), plata(Math.round(base.interes).toDouble()),
                            plata(Math.round(base.interes - plan.interes).toDouble()), stringResource(R.string.cu_meses_x, "${base.meses - plan.meses}")),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                } else Text(stringResource(R.string.cu_no_baja), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            if (vivas && plan.meses != null) GraficaDeudas(plan, base, l.filter { it.actual > 0 }, plata)
        }
    }
    if (l.any { it.actual > 0 } && plan.meses != null) item(key = "d-aplica") { AplicarMetodo(d, plan, l.filter { it.actual > 0 }, plata) }
    item(key = "d-tit") { SectionTitle(stringResource(R.string.cu_tus_deudas)) }
    if (l.isEmpty()) item(key = "d-vacio") { Text(stringResource(R.string.cu_deudas_ayuda), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    items(l, key = { "deuda-${it.deuda.id}" }) { h -> FilaDeuda(h, orden.indexOf(h), plan.fin[h.deuda.id], plata, vm, avisos, plan = plan,
        mover = if (d.metodo == "propio") ({ paso: Int -> vm.deudaMover(h.deuda.id, paso) }) else null) }
    item(key = "d-nueva") { NuevaDeuda(plata) { n, s, t, m, b -> vm.deudaAgregar(n, s, t, m, b) } }
    item(key = "d-nota") { Text(stringResource(R.string.cu_deudas_nota), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun Cifra(t: String, v: String, modifier: Modifier, destacada: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(if (destacada) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp)) {
        Text(t, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(v, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FilaDeuda(h: Deudas.Hoy, orden: Int, fin: Int?, plata: (Double) -> String, vm: CuentasViewModel, avisos: SnackbarHostState, plan: Deudas.Plan? = null, mover: ((Int) -> Unit)? = null) {
    val x = h.deuda
    var pagar by remember { mutableStateOf(false) }
    var editar by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()
    val quitada = stringResource(R.string.cu_deuda_quitada, x.nombre); val deshacer = stringResource(R.string.cu_deshacer)
    RutaCard(onClick = { editar = true }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(if (h.actual <= 0) colorTipo(Tipo.ING).copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                if (h.actual <= 0) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = colorTipo(Tipo.ING))
                else Text("${orden + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(x.nombre + if (x.tipo == "buena") " · " + stringResource(R.string.cu_buena) else "", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.cu_deuda_det, num(x.tasa), plata(x.minimo)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(plata(h.actual), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(when { h.actual <= 0 -> stringResource(R.string.cu_pagada); fin != null -> mesesDespues(fin); else -> "—" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        com.rutaalacima.app.ui.components.ProgressLine(if (x.saldo > 0) (h.pagado / x.saldo).toFloat() else 1f, Modifier.padding(top = 8.dp), color = colorTipo(Tipo.ING))
        // Cuánto le pagas al mes y el impacto de tu plan frente a pagarle solo su mínimo
        if (plan != null && h.actual > 0) {
            val pago = plan.pagoMes[x.id] ?: 0.0; val extraMes = (pago - x.minimo).coerceAtLeast(0.0)
            val sola = remember(h.actual, x.tasa, x.minimo) { Deudas.plan(listOf(h), "minimos", 0.0) }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(10.dp)) {
                    Text(stringResource(R.string.cu_pagas_mes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(plata(Math.round(pago).toDouble()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(if (extraMes > 0.5) stringResource(R.string.cu_pago_desglose, plata(x.minimo), plata(Math.round(extraMes).toDouble())) else stringResource(R.string.cu_su_minimo),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(10.dp)) {
                    Text(stringResource(R.string.cu_la_eliminas), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(fin?.let { mesesDespues(it) } ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (fin != null && plan.meses != null) com.rutaalacima.app.ui.components.ProgressLine(fin / plan.meses.toFloat(), Modifier.padding(top = 6.dp))
                }
            }
            val impacto = when {
                fin == null -> null
                sola.meses == null -> stringResource(R.string.cu_impacto_nunca)
                sola.meses > fin -> stringResource(R.string.cu_impacto, stringResource(R.string.cu_meses_x, "${sola.meses - fin}"),
                    plata(Math.round((sola.interes - (plan.interesDe[x.id] ?: 0.0)).coerceAtLeast(0.0)).toDouble()))
                else -> stringResource(R.string.cu_impacto_igual, stringResource(R.string.cu_meses_x, "${sola.meses}"))
            }
            if (impacto != null) Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colorTipo(Tipo.ING).copy(alpha = 0.10f)).padding(10.dp),
                verticalAlignment = Alignment.Top) {
                Icon(Icons.AutoMirrored.Filled.TrendingDown, null, Modifier.size(18.dp), tint = colorTipo(Tipo.ING)); Spacer(Modifier.width(8.dp))
                Text(impacto, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (mover != null) {
                IconButton(onClick = { mover(-1) }) { Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.cu_subir)) }
                IconButton(onClick = { mover(1) }) { Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.cu_bajar)) }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                vm.deudaQuitar(x.id)
                alcance.launch { if (avisos.showSnackbar(quitada, deshacer, withDismissAction = true) == SnackbarResult.ActionPerformed) vm.deudaRestaurar(x) }
            }) { Icon(Icons.Filled.Delete, stringResource(R.string.cu_quitar_deuda), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (h.actual > 0) FilledTonalButton(onClick = { pagar = true }) { Text(stringResource(R.string.cu_pagar)) }
        }
    }
    if (pagar) {
        var monto by remember { mutableStateOf(Cuentas.numero(minOf(h.actual, x.minimo.takeIf { it > 0 } ?: h.actual))) }
        val ctx = LocalContext.current
        val v = minOf(h.actual, kotlin.math.abs(Finanzas.monto(monto)))
        AlertDialog(onDismissRequest = { pagar = false }, title = { Text(stringResource(R.string.cu_pagar_x, x.nombre)) },
            text = { Column {
                Text(stringResource(R.string.cu_pagar_ayuda, plata(h.actual)), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(monto, { monto = it }, label = { Text(stringResource(R.string.cu_monto)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text(plata(v)) }, modifier = Modifier.padding(top = 8.dp))
            } },
            confirmButton = { TextButton(enabled = v > 0, onClick = {
                vm.deudaPagar(x, v, ctx.getString(R.string.cu_pago_de, x.nombre)); pagar = false
                com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(if (v >= h.actual - 0.005) ctx.getString(R.string.cu_quedo_pagada, x.nombre) else ctx.getString(R.string.cu_pago_anotado, plata(v)))
            }) { Text(stringResource(R.string.cu_pagar)) } },
            dismissButton = { TextButton(onClick = { pagar = false }) { Text(stringResource(R.string.cancelar)) } })
    }
    if (editar) {
        var tasa by remember { mutableStateOf(Cuentas.numero(x.tasa)) }
        var minimo by remember { mutableStateOf(Cuentas.numero(x.minimo)) }
        var saldo by remember { mutableStateOf(Cuentas.numero(x.saldo)) }
        var buena by remember { mutableStateOf(x.tipo == "buena") }
        AlertDialog(onDismissRequest = { editar = false }, title = { Text(x.nombre) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(saldo, { saldo = it }, label = { Text(stringResource(R.string.cu_saldo_inicial)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(tasa, { tasa = it }, label = { Text(stringResource(R.string.cu_interes_anual)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(minimo, { minimo = it }, label = { Text(stringResource(R.string.cu_minimo)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.cu_deuda_buena), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Switch(buena, { buena = it }) }
            } },
            confirmButton = { TextButton(onClick = {
                vm.deudaCambiar(x.id) { it.copy(saldo = kotlin.math.abs(Finanzas.monto(saldo)), tasa = kotlin.math.abs(Finanzas.monto(tasa)), minimo = kotlin.math.abs(Finanzas.monto(minimo)), tipo = if (buena) "buena" else "mala") }
                editar = false }) { Text(stringResource(R.string.guardar)) } },
            dismissButton = { TextButton(onClick = { editar = false }) { Text(stringResource(R.string.cancelar)) } })
    }
}

@Composable
private fun NuevaDeuda(plata: (Double) -> String, onAgregar: (String, Double, Double, Double, Boolean) -> Unit) {
    var nombre by remember { mutableStateOf("") }; var saldo by remember { mutableStateOf("") }
    var tasa by remember { mutableStateOf("") }; var minimo by remember { mutableStateOf("") }; var buena by remember { mutableStateOf(false) }
    val s = kotlin.math.abs(Finanzas.monto(saldo))
    RutaCard {
        Text(stringResource(R.string.cu_nueva_deuda), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(nombre, { nombre = it.take(60) }, label = { Text(stringResource(R.string.cu_deuda)) }, placeholder = { Text(stringResource(R.string.cu_deuda_ej)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            OutlinedTextField(saldo, { saldo = it }, label = { Text(stringResource(R.string.cu_debes_hoy)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            OutlinedTextField(tasa, { tasa = it }, label = { Text(stringResource(R.string.cu_interes_anual)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.8f))
        }
        OutlinedTextField(minimo, { minimo = it }, label = { Text(stringResource(R.string.cu_minimo)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.cu_deuda_buena), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Switch(buena, { buena = it }) }
        Button(enabled = nombre.isNotBlank() && s > 0, onClick = {
            onAgregar(nombre, s, kotlin.math.abs(Finanzas.monto(tasa)), kotlin.math.abs(Finanzas.monto(minimo)), buena)
            nombre = ""; saldo = ""; tasa = ""; minimo = ""; buena = false
        }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(stringResource(R.string.cu_agregar)) }
    }
}

// ====================================================================== Patrimonio

@Composable
private fun nombreTipoActivo(t: String) = stringResource(when (t) {
    "liquidez" -> R.string.cu_act_liquidez; "inversion" -> R.string.cu_act_inversion; "bienes" -> R.string.cu_act_bienes; "negocio" -> R.string.cu_act_negocio; else -> R.string.cu_act_otro })

fun LazyListScope.patrimonioNivel(libro: Libro, plata: (Double) -> String, ir: (NivelCuentas) -> Unit, vm: CuentasViewModel) {
    val led = libro.total.construido
    val act = libro.activos.sumOf { it.valor } + led; val pas = libro.deudas.sumOf { it.actual }
    item(key = "p-cifras") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Cifra(stringResource(R.string.cu_c_activos), plata(act), Modifier.weight(1f))
            Cifra(stringResource(R.string.cu_c_pasivos), plata(pas), Modifier.weight(1f))
            Cifra(stringResource(R.string.cu_c_neto), plata(act - pas), Modifier.weight(1f), destacada = true)
        }
    }
    item(key = "p-tit") {
        Column {
            SectionTitle(stringResource(R.string.cu_lo_que_tienes))
            Text(stringResource(R.string.cu_activos_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    item(key = "p-libro") {
        RutaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.cu_activo_libro), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.cu_se_suma_solo), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(plata(led), fontWeight = FontWeight.SemiBold)
            }
        }
    }
    items(libro.activos, key = { "act-${it.id}" }) { a -> FilaActivo(a, plata, vm) }
    item(key = "p-nuevo") { NuevoActivo { n, t, v -> vm.activoAgregar(n, t, v) } }
    item(key = "p-pas") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.cu_lo_que_debes), Modifier.weight(1f))
            TextButton(onClick = { ir(NivelCuentas.DeudasN) }) { Text(stringResource(R.string.cu_deudas)) }
        }
    }
    if (libro.deudas.isEmpty()) item(key = "p-pas-vacio") { Text(stringResource(R.string.cu_sin_deudas_anotadas), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    items(libro.deudas, key = { "pas-${it.deuda.id}" }) { h ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(h.deuda.nombre, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(if (h.deuda.tipo == "buena") R.string.cu_buena_d else R.string.cu_mala_d), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(plata(h.actual), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FilaActivo(a: Patrimonio.Activo, plata: (Double) -> String, vm: CuentasViewModel) {
    var editar by remember { mutableStateOf(false) }
    RutaCard(onClick = { editar = true }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.nombre, style = MaterialTheme.typography.bodyMedium)
                Text(nombreTipoActivo(a.tipo), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(plata(a.valor), fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { vm.activoQuitar(a.id) }) { Icon(Icons.Filled.Delete, stringResource(R.string.cu_quitar_activo), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    if (editar) {
        var nombre by remember { mutableStateOf(a.nombre) }; var valor by remember { mutableStateOf(Cuentas.numero(a.valor)) }; var tipo by remember { mutableStateOf(a.tipo) }
        AlertDialog(onDismissRequest = { editar = false }, title = { Text(stringResource(R.string.cu_editar_activo)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nombre, { nombre = it.take(60) }, label = { Text(stringResource(R.string.cu_activo)) }, singleLine = true)
                OutlinedTextField(valor, { valor = it }, label = { Text(stringResource(R.string.cu_valor_hoy)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                SelectorTipoActivo(tipo) { tipo = it }
            } },
            confirmButton = { TextButton(onClick = { vm.activoCambiar(a.copy(nombre = nombre.trim().ifBlank { a.nombre }, valor = kotlin.math.abs(Finanzas.monto(valor)), tipo = tipo)); editar = false }) { Text(stringResource(R.string.guardar)) } },
            dismissButton = { TextButton(onClick = { editar = false }) { Text(stringResource(R.string.cancelar)) } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorTipoActivo(actual: String, onElegir: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    androidx.compose.material3.ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(nombreTipoActivo(actual), {}, readOnly = true, singleLine = true, label = { Text(stringResource(R.string.cu_tipo)) },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(abierto) },
            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable))
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Patrimonio.TIPOS.forEach { t -> androidx.compose.material3.DropdownMenuItem(text = { Text(nombreTipoActivo(t)) }, onClick = { onElegir(t); abierto = false }) }
        }
    }
}

@Composable
private fun NuevoActivo(onAgregar: (String, String, Double) -> Unit) {
    var nombre by remember { mutableStateOf("") }; var valor by remember { mutableStateOf("") }; var tipo by remember { mutableStateOf("bienes") }
    val v = kotlin.math.abs(Finanzas.monto(valor))
    RutaCard {
        OutlinedTextField(nombre, { nombre = it.take(60) }, label = { Text(stringResource(R.string.cu_activo)) }, placeholder = { Text(stringResource(R.string.cu_activo_ej)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            OutlinedTextField(valor, { valor = it }, label = { Text(stringResource(R.string.cu_valor_hoy)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            Box(Modifier.weight(1.2f)) { SelectorTipoActivo(tipo) { tipo = it } }
        }
        Button(enabled = nombre.isNotBlank() && v > 0, onClick = { onAgregar(nombre, tipo, v); nombre = ""; valor = "" }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(stringResource(R.string.cu_agregar)) }
    }
}

// ====================================================================== Privacidad

/** Privada (con capas de seguridad) o pública (solo se comparte el avance en porcentajes). */
@Composable
fun PrivacidadCard(libro: Libro, vm: CuentasViewModel) {
    val ctx = LocalContext.current
    RutaCard {
        Text(stringResource(R.string.cu_privacidad), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.cu_privacidad_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !libro.publica, onClick = { vm.visibilidad(false) }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.cu_privada)) }
            SegmentedButton(selected = libro.publica, onClick = { vm.visibilidad(true) }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.cu_publica)) }
        }
        if (libro.publica) Text(stringResource(R.string.cu_publica_d), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
        else {
            Capa(Icons.Filled.VerifiedUser, R.string.cu_capa_cuenta, R.string.cu_capa_cuenta_d)
            val sinBloqueo = stringResource(R.string.cu_sin_bloqueo_tel)
            Capa(Icons.Filled.Fingerprint, R.string.cu_capa_bloqueo, R.string.cu_capa_bloqueo_d) {
                Switch(libro.bloqueo, { si ->
                    if (si && !Bloqueo.biometriaDisponible(ctx) && !Bloqueo.tieneClave(ctx)) com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(sinBloqueo)
                    else { vm.bloqueo(si); if (si) vm.abiertoHasta = System.currentTimeMillis() + 5 * 60_000 }
                })
            }
            Capa(Icons.Filled.VisibilityOff, R.string.cu_capa_ocultar, R.string.cu_capa_ocultar_d) { Switch(libro.respuestas["fin#ocultar"] == "1", { vm.ocultar(it) }) }
            Capa(Icons.Filled.AttachFile, R.string.cu_capa_recibos, R.string.cu_capa_recibos_d)
        }
    }
}

@Composable
private fun Capa(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: Int, desc: Int, control: (@Composable () -> Unit)? = null) {
    Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = colorTipo(Tipo.ING))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(titulo), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        control?.let { Spacer(Modifier.width(8.dp)); it() }
    }
}

// ====================================================================== Categorías propias

@Composable
fun BotonCrearCategoria() {
    var abierto by remember { mutableStateOf(false) }
    if (LocalCuentas.current == null) return
    TextButton(onClick = { abierto = true }) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.cu_crear_categoria)) }
    if (abierto) NuevaCategoriaDialogo(Tipo.GAS, onCerrar = { abierto = false }) { abierto = false }
}

@Composable
fun NuevaCategoriaDialogo(tipoInicial: Tipo, onCerrar: () -> Unit, onCreada: (String) -> Unit) {
    val vm = LocalCuentas.current ?: return
    val ctx = LocalContext.current
    var tipo by remember { mutableStateOf(tipoInicial) }
    var nombre by remember { mutableStateOf("") }
    var verde by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onCerrar, title = { Text(stringResource(R.string.cu_nueva_categoria)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Tipo.entries.forEachIndexed { i, t -> SegmentedButton(selected = tipo == t, onClick = { tipo = t }, shape = SegmentedButtonDefaults.itemShape(i, Tipo.entries.size), icon = {}) {
                    Text(nombreTipo(t, plural = false), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) } }
            }
            OutlinedTextField(nombre, { nombre = it.take(40) }, label = { Text(stringResource(R.string.cu_nombre)) }, placeholder = { Text(stringResource(R.string.cu_cat_ej)) }, singleLine = true)
            if (tipo == Tipo.GAS) Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cu_es_esencial), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Switch(verde, { verde = it })
            }
        } },
        confirmButton = { TextButton(enabled = nombre.isNotBlank(), onClick = {
            vm.crearCategoria(tipo, nombre, verde)?.let { c -> com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(ctx.getString(R.string.cu_categoria_creada, nombre.trim())); onCreada(c) }
        }) { Text(stringResource(R.string.cu_crear)) } },
        dismissButton = { TextButton(onClick = onCerrar) { Text(stringResource(R.string.cancelar)) } })
}

// ====================================================================== Recibos

/** Los recibos de un movimiento: verlos, agregar una foto o un PDF y quitarlos. */
@Composable
fun RecibosMovimiento(x: Mov, fecha: String, vm: CuentasViewModel) {
    val ctx = LocalContext.current
    var subiendo by remember { mutableStateOf(false) }
    var viendo by remember { mutableStateOf<Cuentas.Recibo?>(null) }
    val guardado = stringResource(R.string.cu_recibo_guardado); val fallo = stringResource(R.string.cu_recibo_error)
    val elegir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { subiendo = true; vm.agregarRecibo(fecha, actual, uri) { ok -> subiendo = false; com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(if (ok) guardado else fallo) } }
    }
    // Un fijo del mes (virtual) que recibe un recibo pasa a ser movimiento propio de ese mes: se busca por su fijo
    val movs = vm.libro.value.movs(fecha)
    val actual = movs.firstOrNull { it.id == x.id } ?: movs.firstOrNull { x.fx != null && it.fx == x.fx && !it.virtual } ?: x
    Column {
        Text(stringResource(R.string.cu_recibos), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
        actual.r.forEach { r ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { viendo = r }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (r.tipo == "application/pdf") Icons.Filled.PictureAsPdf else Icons.Filled.Image, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(r.nom, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = { vm.quitarRecibo(fecha, actual, r) }) { Icon(Icons.Filled.Delete, stringResource(R.string.cu_quitar_recibo), Modifier.size(20.dp)) }
            }
        }
        if (subiendo) CircularProgressIndicator(Modifier.size(24.dp).padding(4.dp))
        else TextButton(onClick = { elegir.launch(arrayOf("image/*", "application/pdf")) }) {
            Icon(Icons.Filled.AttachFile, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.cu_agregar_recibo))
        }
        Text(stringResource(R.string.cu_recibos_privados), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    viendo?.let { r -> VisorRecibo(r, vm) { viendo = null } }
}

/** Visor del recibo: la foto o las páginas del PDF, dentro de la app (nada sale del teléfono). */
@Composable
private fun VisorRecibo(r: Cuentas.Recibo, vm: CuentasViewModel, onCerrar: () -> Unit) {
    var paginas by remember { mutableStateOf<List<Bitmap>?>(null) }
    var archivo by remember { mutableStateOf<java.io.File?>(null) }
    var error by remember { mutableStateOf(false) }
    LaunchedEffect(r.id) {
        val f = vm.archivoRecibo(r); if (f == null) { error = true; return@LaunchedEffect }
        archivo = f
        if (r.tipo == "application/pdf") paginas = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                PdfRenderer(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)).use { pdf ->
                    (0 until minOf(pdf.pageCount, 20)).map { i -> pdf.openPage(i).use { p ->
                        Bitmap.createBitmap(p.width * 2, p.height * 2, Bitmap.Config.ARGB_8888).also { b -> b.eraseColor(android.graphics.Color.WHITE); p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) } } }
                }
            }.getOrNull()
        }
    }
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(r.nom, Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = onCerrar) { Text(stringResource(R.string.cu_listo)) }
            }
            when {
                error -> Text(stringResource(R.string.cu_recibo_no_abre), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                r.tipo == "application/pdf" -> paginas?.let { l ->
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(l) { b -> Image(b.asImageBitmap(), null, Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant), contentScale = ContentScale.FillWidth) }
                    }
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { com.rutaalacima.app.ui.components.CargandoMarca() }
                else -> archivo?.let { coil.compose.AsyncImage(it, r.nom, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                    ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { com.rutaalacima.app.ui.components.CargandoMarca() }
            }
        }
    }
}


/** Medidor semicircular de M3 del Índice de Salud Financiera. */
@Composable
private fun MedidorIsf(valor: Int?, etiqueta: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val pista = MaterialTheme.colorScheme.surfaceContainerHighest
    val fraccion by androidx.compose.animation.core.animateFloatAsState(((valor ?: 0) / 100f).coerceIn(0f, 1f),
        androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "isf")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 150.dp, height = 86.dp), contentAlignment = Alignment.BottomCenter) {
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                val grosor = 10.dp.toPx(); val lado = size.width - grosor
                val tl = androidx.compose.ui.geometry.Offset(grosor / 2, grosor / 2)
                val sz = androidx.compose.ui.geometry.Size(lado, lado)
                val trazo = androidx.compose.ui.graphics.drawscope.Stroke(grosor, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawArc(pista, 180f, 180f, false, tl, sz, style = trazo)
                drawArc(color, 180f, 180f * fraccion, false, tl, sz, style = trazo)
            }
            Text(valor?.toString() ?: "—", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** Lo que tus deudas le hacen a tu salud financiera: el ISF hoy y libre de deudas, y los indicadores que cambian. */
@Composable
private fun EfectoDeudasSalud(libro: Libro, plan: Deudas.Plan, plata: (Double) -> String) {
    val hoyS = remember(libro) { libro.salud(null) }; val sinS = libro.saludSinDeudas
    val verde = colorTipo(Tipo.ING)
    RutaCard {
        Text(stringResource(R.string.cu_efecto_titulo), style = MaterialTheme.typography.titleMedium)
        if (hoyS.isf == null || sinS.isf == null) {
            Text(stringResource(R.string.cu_efecto_sin_ing), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@RutaCard
        }
        val pts = sinS.isf - hoyS.isf
        Text(if (pts > 0) stringResource(R.string.cu_efecto_puntos, pts) else stringResource(R.string.cu_efecto_poco),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            val colorHoy = when (hoyS.nivel) { Salud.Nivel.EXCELENTE, Salud.Nivel.BUENA -> verde; Salud.Nivel.MODERADA -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.error }
            MedidorIsf(hoyS.isf, stringResource(R.string.cu_hoy_con_deudas), colorHoy, Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            MedidorIsf(sinS.isf, stringResource(R.string.cu_libre_de_deudas) + (plan.meses?.let { " · " + mesesDespues(it) } ?: ""), verde, Modifier.weight(1f))
        }
        fun pct(v: Double?) = if (v == null || v.isInfinite()) "—" else "${Math.round(v * 100)} %"
        fun ind(clave: String, r: Salud.Resultado) = r.indicadores.firstOrNull { it.clave == clave }?.valor
        Column(Modifier.padding(top = 10.dp)) {
            listOf(
                Triple(R.string.cu_ind_dti, pct(ind("dti", hoyS)), pct(ind("dti", sinS) ?: 0.0)),
                Triple(R.string.cu_ind_endeuda, pct(ind("endeuda", hoyS)), pct(ind("endeuda", sinS) ?: 0.0)),
                Triple(R.string.cu_c_cuotas, plata(hoyS.deudaMes), plata(0.0)),
                Triple(R.string.cu_c_neto, plata(hoyS.neto), plata(sinS.neto)),
            ).forEach { (t, a, b) ->
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(t), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(a, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(96.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text(b, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = verde, modifier = Modifier.width(96.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
    }
}

/** Así baja tu deuda: el método elegido (área) frente a solo mínimos (punteada), con cada deuda pagada marcada. */
@Composable
private fun GraficaDeudas(plan: Deudas.Plan, base: Deudas.Plan, vivas: List<Deudas.Hoy>, plata: (Double) -> String) {
    val linea = MaterialTheme.colorScheme.primary; val gris = MaterialTheme.colorScheme.onSurfaceVariant; val oro = MaterialTheme.colorScheme.tertiary
    val fondo = MaterialTheme.colorScheme.surface
    val n = maxOf(plan.serie.size, minOf(base.serie.size, (plan.serie.size * 2.2).toInt(), 120))
    val alto = (plan.serie + base.serie.take(n)).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    Column(Modifier.padding(top = 12.dp)) {
        Text(stringResource(R.string.cu_asi_baja), style = MaterialTheme.typography.titleSmall)
        Text(plata(alto), style = MaterialTheme.typography.labelSmall, color = gris)
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(160.dp).padding(vertical = 6.dp)) {
            fun x(k: Int) = k / (n - 1).coerceAtLeast(1).toFloat() * size.width
            fun y(v: Double) = ((1 - v / alto) * size.height).toFloat()
            fun camino(s: List<Double>) = androidx.compose.ui.graphics.Path().apply { s.take(n).forEachIndexed { k, v -> if (k == 0) moveTo(x(k), y(v)) else lineTo(x(k), y(v)) } }
            val area = camino(plan.serie).apply { lineTo(x(plan.serie.size - 1), size.height); lineTo(0f, size.height); close() }
            drawPath(area, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(linea.copy(alpha = 0.3f), linea.copy(alpha = 0.03f))))
            drawLine(gris.copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1.dp.toPx())
            drawPath(camino(base.serie), gris.copy(alpha = 0.6f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
            drawPath(camino(plan.serie), linea, style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
            vivas.mapNotNull { plan.fin[it.deuda.id] }.forEach { k ->
                val c = androidx.compose.ui.geometry.Offset(x(k), y(plan.serie.getOrElse(k) { 0.0 }))
                drawCircle(fondo, 6.dp.toPx(), c); drawCircle(oro, 4.dp.toPx(), c)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Leyenda(linea, stringResource(R.string.cu_ley_metodo)); Leyenda(gris, stringResource(R.string.cu_ley_minimos)); Leyenda(oro, stringResource(R.string.cu_ley_pagada))
        }
    }
}

@Composable
private fun Leyenda(color: androidx.compose.ui.graphics.Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


/**
 * Así se aplica el método: el orden en que atacas tus deudas (y por qué), qué pagas este mes a cada una
 * y el calendario mes a mes con lo que recibe cada deuda (igual que la web).
 */
@Composable
private fun AplicarMetodo(d: Deudas.Datos, plan: Deudas.Plan, vivas: List<Deudas.Hoy>, plata: (Double) -> String) {
    val nombre = stringResource(when (d.metodo) { "bola" -> R.string.cu_bola; "flujo" -> R.string.cu_flujo; "hibrido" -> R.string.cu_hibrido; "propio" -> R.string.cu_propio; else -> R.string.cu_avalancha })
    val orden = vivas.sortedBy { plan.fin[it.deuda.id] ?: Int.MAX_VALUE }
    val mes1 = plan.pagos.firstOrNull()?.first.orEmpty()
    val foco = orden.firstOrNull { (mes1[it.deuda.id] ?: 0.0) > it.deuda.minimo + 0.5 }
    var abierto by remember { mutableStateOf(false) }; var todo by remember { mutableStateOf(false) }
    fun num1(v: Double) = java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(v)
    @Composable fun porque(h: Deudas.Hoy): String { val x = h.deuda
        return when (d.metodo) {
            "bola" -> stringResource(R.string.cu_porque_saldo, plata(h.actual))
            "flujo" -> if (x.minimo > 0) stringResource(R.string.cu_porque_flujo, num1(h.actual / x.minimo)) else "—"
            "hibrido" -> if (h.actual <= 3 * (x.minimo + d.extra)) stringResource(R.string.cu_porque_rapida) else stringResource(R.string.cu_porque_interes, num1(x.tasa))
            "propio" -> stringResource(R.string.cu_porque_orden)
            else -> stringResource(R.string.cu_porque_interes, num1(x.tasa))
        } }
    RutaCard {
        Text(stringResource(R.string.cu_asi_aplica, nombre), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.cu_aplica_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.cu_orden_ataque), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        orden.forEachIndexed { i, h ->
            Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(h.deuda.nombre, style = MaterialTheme.typography.titleSmall)
                    Text(porque(h) + " · " + stringResource(R.string.cu_eliminas_en, plan.fin[h.deuda.id]?.let { mesesDespues(it) } ?: "—"),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(stringResource(R.string.cu_este_mes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
        androidx.compose.material3.OutlinedCard(Modifier.fillMaxWidth()) {
            orden.forEachIndexed { i, h ->
                val v = mes1[h.deuda.id] ?: 0.0; val extra = v - h.deuda.minimo > 0.5
                if (i > 0) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().background(if (extra) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(h.deuda.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (extra) Text(stringResource(R.string.cu_recibe_extra), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(end = 8.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 2.dp))
                    Text(plata(Math.round(v).toDouble()), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(stringResource(R.string.cu_total_mes), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(plata(Math.round(orden.sumOf { mes1[it.deuda.id] ?: 0.0 }).toDouble()), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
        if (foco != null) Text(stringResource(R.string.cu_extra_a, foco.deuda.nombre), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        // Calendario mes a mes (plegable)
        androidx.compose.material3.TextButton(onClick = { abierto = !abierto }, modifier = Modifier.padding(top = 4.dp)) {
            Icon(if (abierto) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.cu_mes_a_mes))
        }
        if (abierto) {
            val filas = plan.pagos.take(if (todo) 120 else 12)
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                Row(Modifier.padding(vertical = 6.dp)) {
                    Text(stringResource(R.string.cu_mes), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(76.dp))
                    orden.forEach { Text(it.deuda.nombre, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.width(92.dp)) }
                    Text(stringResource(R.string.cu_debes_final), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.width(104.dp))
                }
                filas.forEachIndexed { k, (por, saldo) ->
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.width((76 + 92 * orden.size + 104).dp))
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(mesesDespues(k + 1), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(76.dp))
                        orden.forEach { h -> val v = por[h.deuda.id] ?: 0.0; val fin = plan.fin[h.deuda.id] == k + 1; val extra = v - h.deuda.minimo > 0.5
                            Text((if (v > 0.5) plata(Math.round(v).toDouble()) else "—") + if (fin) " ✓" else "", style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (extra || fin) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.End,
                                color = if (fin) colorTipo(Tipo.ING) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(92.dp).background(if (extra && !fin) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent).padding(horizontal = 4.dp)) }
                        Text(plata(Math.round(saldo).toDouble()), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.width(104.dp))
                    }
                }
            }
            if (plan.pagos.size > 12) androidx.compose.material3.TextButton(onClick = { todo = !todo }) {
                Text(if (todo) stringResource(R.string.cu_ver_12) else stringResource(R.string.cu_ver_meses, minOf(plan.pagos.size, 120)))
            }
        }
    }
}
