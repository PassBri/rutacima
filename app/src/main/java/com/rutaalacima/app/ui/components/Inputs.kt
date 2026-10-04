package com.rutaalacima.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FMT_UTC = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "CO"))

/** Las fechas del DatePicker se guardan como medianoche UTC; se formatean en UTC para no correr el día. */
fun formatoFechaUtc(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(FMT_UTC)

/** Botón que muestra una fecha y abre el selector de fecha de Material 3. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FechaField(
    etiqueta: String,
    millis: Long?,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var abierto by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { abierto = true }, modifier = modifier) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
        Text("  $etiqueta: " + (millis?.let { formatoFechaUtc(it) } ?: "sin fecha"))
    }
    if (abierto) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = { onChange(state.selectedDateMillis); abierto = false }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { onChange(null); abierto = false }) { Text("Quitar fecha") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Selector de una opción entre varias, con fichas. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <T> ChipSelector(
    titulo: String?,
    opciones: List<T>,
    seleccionado: T?,
    etiqueta: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    color: ((T) -> Color)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        titulo?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { op ->
                val c = color?.invoke(op)
                FilterChip(
                    selected = op == seleccionado,
                    onClick = { onSelect(op) },
                    label = { Text(etiqueta(op)) },
                    colors = if (c != null) FilterChipDefaults.filterChipColors(
                        selectedContainerColor = c.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    ) else FilterChipDefaults.filterChipColors(),
                )
            }
        }
    }
}
