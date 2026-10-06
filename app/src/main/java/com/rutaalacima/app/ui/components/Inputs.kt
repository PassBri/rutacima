package com.rutaalacima.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.rutaalacima.app.domain.model.Vida
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.rutaalacima.app.R
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FMT_UTC get() = DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(Locale.getDefault())

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
        Text("  $etiqueta: " + (millis?.let { formatoFechaUtc(it) } ?: stringResource(R.string.sin_fecha)))
    }
    if (abierto) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = { onChange(state.selectedDateMillis); abierto = false }) { Text(stringResource(R.string.aceptar)) }
            },
            dismissButton = {
                TextButton(onClick = { onChange(null); abierto = false }) { Text(stringResource(R.string.quitar_fecha)) }
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
    etiqueta: @Composable (T) -> String,
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

/**
 * Horizonte de un propósito: cualquier número de años entre [Vida.HORIZONTE_MIN] y [Vida.HORIZONTE_MAX].
 * Se escribe el número, se ajusta con − y +, o se toca un atajo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorHorizonte(anios: Int, onCambio: (Int) -> Unit, modifier: Modifier = Modifier) {
    // El texto se edita libre (puede quedar vacío mientras escribes); el valor se guarda ya acotado.
    var texto by remember(anios) { mutableStateOf(anios.toString()) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.horizonte), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(onClick = { onCambio(Vida.horizonte(anios - 1)) }, enabled = anios > Vida.HORIZONTE_MIN) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.horizonte_menos))
            }
            OutlinedTextField(
                value = texto,
                onValueChange = { v ->
                    val limpio = v.filter { it.isDigit() }.take(2)
                    texto = limpio
                    limpio.toIntOrNull()?.let { n -> if (n >= Vida.HORIZONTE_MIN) onCambio(Vida.horizonte(n)) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                modifier = Modifier.width(80.dp),
            )
            FilledTonalIconButton(onClick = { onCambio(Vida.horizonte(anios + 1)) }, enabled = anios < Vida.HORIZONTE_MAX) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.horizonte_mas))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Vida.HORIZONTES.forEach { h ->
                FilterChip(selected = h == anios, onClick = { onCambio(h) }, label = { Text(stringResource(R.string.n_anios, h)) })
            }
        }
        Text(stringResource(R.string.horizonte_elige, Vida.HORIZONTE_MIN, Vida.HORIZONTE_MAX), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
