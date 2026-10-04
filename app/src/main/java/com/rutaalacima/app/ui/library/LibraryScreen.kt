package com.rutaalacima.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.data.content.Categoria
import com.rutaalacima.app.data.content.WorkbookSummary
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class WorkbookItem(val resumen: WorkbookSummary, val progreso: Float)

class LibraryViewModel(c: AppContainer) : ViewModel() {
    val items: StateFlow<Map<Categoria, List<WorkbookItem>>> =
        combine(flow { emit(c.contenido.index()) }, c.respuestas.conteos) { index, conteos ->
            index.map { w ->
                val p = if (w.inputs == 0) 0f else (conteos[w.id] ?: 0).toFloat() / w.inputs
                WorkbookItem(w, p.coerceAtMost(1f))
            }.groupBy { Categoria.from(it.resumen.category) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

@Composable
fun LibraryScreen(contentPadding: PaddingValues, onOpen: (String) -> Unit) {
    val vm = rutaViewModel { LibraryViewModel(it) }
    val grupos by vm.items.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Tu Ruta", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Los workbooks de Ruta a la Cima. Todo lo que escribas se guarda en tu teléfono.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Categoria.entries.forEach { cat ->
            val lista = grupos[cat].orEmpty()
            if (lista.isNotEmpty()) {
                item(key = "cat-${cat.clave}") { SectionTitle(cat.titulo, Modifier.padding(top = 12.dp)) }
                items(lista, key = { it.resumen.id }) { item -> WorkbookRow(item) { onOpen(item.resumen.id) } }
            }
        }
    }
}

@Composable
private fun WorkbookRow(item: WorkbookItem, onClick: () -> Unit) {
    RutaCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    item.resumen.order.toString().padStart(2, '0'),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.resumen.title, style = MaterialTheme.typography.titleMedium)
                Text(item.resumen.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.progreso > 0f) {
                    Spacer(Modifier.height(8.dp))
                    ProgressLine(item.progreso)
                }
            }
        }
    }
}
