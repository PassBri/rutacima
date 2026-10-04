package com.rutaalacima.app.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R

/**
 * Teclado numérico para la clave propia (4 a 6 dígitos). Botones de 64 dp (más que el mínimo
 * de 48 dp de Material). Los dígitos se muestran como puntos, nunca en claro.
 */
@Composable
fun PinPad(
    titulo: String,
    valor: String,
    onCambio: (String) -> Unit,
    onConfirmar: () -> Unit,
    error: String? = null,
    max: Int = 6,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.semantics { contentDescription = "${valor.length} / $max" },
        ) {
            repeat(max) { i ->
                Box(
                    Modifier.size(14.dp).clip(CircleShape)
                        .background(if (i < valor.length) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge) }
        val filas = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("<", "0", "ok"))
        filas.forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                fila.forEach { tecla ->
                    FilledTonalIconButton(
                        onClick = {
                            when (tecla) {
                                "<" -> onCambio(valor.dropLast(1))
                                "ok" -> if (valor.length >= 4) onConfirmar()
                                else -> if (valor.length < max) onCambio(valor + tecla)
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        enabled = tecla != "ok" || valor.length >= 4,
                    ) {
                        when (tecla) {
                            "<" -> Icon(Icons.AutoMirrored.Filled.Backspace, stringResource(R.string.borrar))
                            "ok" -> Icon(Icons.Filled.Check, stringResource(R.string.aceptar))
                            else -> Text(tecla, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}
