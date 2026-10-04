package com.rutaalacima.app.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.Prioridad
import com.rutaalacima.app.ui.components.ChipSelector
import com.rutaalacima.app.ui.components.FechaField
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.asColor

/** Ficha de un propósito: descripción, indicador de éxito, visualización, porqué y plan de acción. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropositoScreen(propositoId: Long, onBack: () -> Unit) {
    val vm = rutaViewModel(key = "proposito-$propositoId") { PropositoViewModel(it, propositoId) }
    val acciones by vm.acciones.collectAsStateWithLifecycle()
    val p = vm.proposito
    var confirmarBorrado by remember { mutableStateOf(false) }
    var nuevaAccion by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.id == 0L) "Nuevo propósito" else "Propósito") },
                navigationIcon = { IconButton(onClick = { if (p.titulo.isNotBlank() && !vm.guardado) vm.guardar(onBack) else onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Guardar y volver") } },
                actions = {
                    if (vm.id != 0L) IconButton(onClick = { confirmarBorrado = true }) { Icon(Icons.Outlined.Delete, "Eliminar") }
                },
            )
        },
    ) { padding ->
        if (!vm.cargado) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(p.titulo, { v -> vm.editar { it.copy(titulo = v) } }, label = { Text("Propósito (meta a 5 años)") },
                    modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.titleMedium)
            }
            item {
                ChipSelector("Prioridad (ABCD)", Prioridad.entries, Prioridad.from(p.prioridad), { "${it.clave} · ${it.nivel}" },
                    { sel -> vm.editar { it.copy(prioridad = sel.name) } }, color = { it.color.asColor() })
            }
            item {
                ChipSelector("Eje que activa principalmente", listOf<Eje?>(null) + Eje.entries, Eje.fromCodigo(p.eje), { it?.nombre ?: "—" },
                    { sel -> vm.editar { it.copy(eje = sel?.codigo) } }, color = { it?.color?.asColor() ?: Color.Gray })
            }
            item { Campo("Descripción", p.descripcion) { v -> vm.editar { it.copy(descripcion = v) } } }
            item { Campo("Indicador de éxito: \"Habré alcanzado esta meta si…\"", p.indicadorExito) { v -> vm.editar { it.copy(indicadorExito = v) } } }
            item { Campo("Visualización: \"Me visualizo…\"", p.visualizacion) { v -> vm.editar { it.copy(visualizacion = v) } } }
            item { Campo("¿Por qué es importante esta meta para mí?", p.porQueImporta) { v -> vm.editar { it.copy(porQueImporta = v) } } }
            item { Campo("Metas específicas dentro de este eje (una por línea)", p.metasEspecificas) { v -> vm.editar { it.copy(metasEspecificas = v) } } }
            item { Campo("Impacto", p.impacto) { v -> vm.editar { it.copy(impacto = v) } } }
            item { Campo("Reflexión final", p.reflexionFinal) { v -> vm.editar { it.copy(reflexionFinal = v) } } }
            item {
                Text("Progreso: ${p.progreso}%", style = MaterialTheme.typography.labelLarge)
                Slider(value = p.progreso.toFloat(), onValueChange = { v -> vm.editar { it.copy(progreso = (v / 5).toInt() * 5) } }, valueRange = 0f..100f)
                Button(onClick = { vm.guardar() }, enabled = p.titulo.isNotBlank() && !vm.guardado, modifier = Modifier.fillMaxWidth()) {
                    Text(if (vm.guardado) "Guardado" else "Guardar propósito")
                }
            }
            item { SectionTitle("Plan de acción (${acciones.size}/10)") }
            if (vm.id == 0L) {
                item { Text("Guarda el propósito para empezar su plan de acción.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(acciones, key = { it.id }) { a ->
                    RutaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = a.hecha, onCheckedChange = { vm.alternarAccion(a) })
                            Text(
                                a.texto, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (a.hecha) TextDecoration.LineThrough else null,
                            )
                            IconButton(onClick = { vm.eliminarAccion(a) }) { Icon(Icons.Outlined.Delete, "Eliminar paso") }
                        }
                        FechaField("Fecha límite", a.fechaFin, { vm.fechaAccion(a, it) })
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(nuevaAccion, { nuevaAccion = it }, label = { Text("Nuevo paso") }, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.agregarAccion(nuevaAccion); nuevaAccion = "" }, enabled = nuevaAccion.isNotBlank()) {
                            Icon(Icons.Filled.Add, "Agregar paso")
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Eliminar este propósito?") },
            text = { Text("Se borrará también su plan de acción.") },
            confirmButton = { TextButton(onClick = { confirmarBorrado = false; vm.eliminar(onBack) }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Campo(titulo: String, valor: String, onChange: (String) -> Unit) {
    OutlinedTextField(valor, onChange, label = { Text(titulo) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
}
