package com.rutaalacima.app.ui.planner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Finanzas
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Las finanzas de un mes, sumadas desde el control financiero de cada día (igual que en la web). */
class FinanzasMesViewModel(private val c: AppContainer, val ym: YearMonth) : ViewModel() {
    private val clave = Finanzas.clavePresupuesto(ym.year, ym.monthValue)

    val presupuesto: StateFlow<String> = c.respuestas.observar(Finanzas.WB).map { it[clave].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val resumen: StateFlow<Finanzas.Mes?> = combine(c.agenda.delMes(ym.year, ym.monthValue), presupuesto) { dias, p ->
        Finanzas.mes(
            dias.mapNotNull { d -> runCatching { LocalDate.parse(d.fecha).dayOfMonth }.getOrNull()?.let { it to Finanzas.dia(d.ingresos, d.gastos, d.ahorro, d.inversion) } }.toMap(),
            p,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var guardado: Job? = null
    fun guardarPresupuesto(v: String) {
        guardado?.cancel()
        guardado = c.appScope.launch { delay(500); c.respuestas.guardar(Finanzas.WB, clave, v.trim()) }
    }
}

@Composable
fun FinanzasMesCard(ym: YearMonth, modifier: Modifier = Modifier) {
    val vm = rutaViewModel(key = "finanzas-$ym") { FinanzasMesViewModel(it, ym) }
    val r by vm.resumen.collectAsStateWithLifecycle()
    val guardado by vm.presupuesto.collectAsStateWithLifecycle()
    var texto by remember(ym) { mutableStateOf(guardado) }
    var editado by remember(ym) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(guardado) { if (!editado) texto = guardado }
    val mesNombre = nombreMes(ym.monthValue)
    RutaCard(modifier) {
        Text(stringResource(R.string.fin_mes), style = MaterialTheme.typography.titleMedium)
        val m = r
        if (m == null || m.vacio) {
            Text(stringResource(R.string.fin_vacio, mesNombre), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(stringResource(R.string.fin_dias_registro, m.diasConRegistro, mesNombre), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            val t = m.total
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Cifra(stringResource(R.string.ingresos), t.ingresos, Modifier.weight(1f))
                Cifra(stringResource(R.string.gastos), t.gastos, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Cifra(stringResource(R.string.ahorro), t.ahorro, Modifier.weight(1f))
                Cifra(stringResource(R.string.inversion), t.inversion, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Cifra(stringResource(R.string.fin_disponible), t.disponible, Modifier.fillMaxWidth(), destacada = true)
            m.tasaAhorro?.let {
                Text(stringResource(R.string.fin_tasa, Math.round(it * 100).toInt()), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            }
        }
        OutlinedTextField(
            texto, { texto = it; editado = true; vm.guardarPresupuesto(it) },
            label = { Text(stringResource(R.string.fin_presupuesto)) }, placeholder = { Text(stringResource(R.string.fin_presupuesto_ej)) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        if (m != null && !m.vacio) {
            m.usoPresupuesto?.let { uso ->
                ProgressLine(uso.toFloat().coerceAtMost(1f), Modifier.padding(top = 10.dp),
                    color = if (uso > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.fin_gastaste, Finanzas.formato(m.total.gastos), Finanzas.formato(m.presupuesto)) +
                    if (uso > 1) " · " + stringResource(R.string.fin_te_pasaste, Finanzas.formato(m.total.gastos - m.presupuesto)) else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            if (m.gastosPorDia.isNotEmpty()) {
                Text(stringResource(R.string.fin_gastos_dia) + if (m.presupuesto > 0) " · " + stringResource(R.string.fin_linea) else "",
                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                GastosPorDia(ym.lengthOfMonth(), m.gastosPorDia, if (m.presupuesto > 0) m.presupuesto / ym.lengthOfMonth() else 0.0,
                    stringResource(R.string.fin_gastos_dia))
            }
        }
    }
}

@Composable
private fun Cifra(titulo: String, valor: Double, modifier: Modifier, destacada: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(12.dp))
        .background(if (destacada) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLow)
        .padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Finanzas.formato(valor), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            color = if (valor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

/** Barras a escala: una por día con gasto; la línea punteada es el presupuesto diario. */
@Composable
private fun GastosPorDia(dias: Int, gastos: Map<Int, Double>, diario: Double, descripcion: String) {
    val barra = MaterialTheme.colorScheme.primary
    val guia = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxWidth().height(80.dp).semantics { contentDescription = descripcion }) {
        val max = maxOf(gastos.values.maxOrNull() ?: 0.0, diario, 1.0)
        val ancho = size.width / dias
        gastos.forEach { (d, v) ->
            val h = (v / max * (size.height - 4)).toFloat().coerceAtLeast(2f)
            drawRoundRect(barra, Offset((d - 1) * ancho + 1, size.height - h), Size((ancho - 2).coerceAtLeast(1f), h), CornerRadius(3f, 3f))
        }
        if (diario > 0) {
            val y = (size.height - diario / max * (size.height - 4)).toFloat()
            drawLine(guia, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        }
    }
    Row(Modifier.fillMaxWidth()) {
        Text("1", style = MaterialTheme.typography.labelSmall, color = guia, modifier = Modifier.weight(1f))
        Text("$dias", style = MaterialTheme.typography.labelSmall, color = guia)
    }
}

/** "Disponible hoy" del control financiero del día. */
@Composable
fun DisponibleHoy(ingresos: String, gastos: String, ahorro: String, inversion: String) {
    val d = Finanzas.dia(ingresos, gastos, ahorro, inversion)
    if (d.vacio) return
    Row(Modifier.padding(top = 4.dp)) {
        Text(stringResource(R.string.fin_disponible_hoy) + " ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Finanzas.formato(d.disponible), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = if (d.disponible < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}
