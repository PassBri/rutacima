package com.rutaalacima.app.ui.workbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.data.content.Block
import com.rutaalacima.app.data.content.BulletBlock
import com.rutaalacima.app.data.content.CalloutBlock
import com.rutaalacima.app.data.content.CheckBlock
import com.rutaalacima.app.data.content.ChoiceBlock
import com.rutaalacima.app.data.content.HeadingBlock
import com.rutaalacima.app.data.content.InputTableBlock
import com.rutaalacima.app.data.content.ParagraphBlock
import com.rutaalacima.app.data.content.PromptBlock
import com.rutaalacima.app.data.content.QuoteBlock
import com.rutaalacima.app.data.content.ScaleBlock
import com.rutaalacima.app.data.content.TableBlock
import com.rutaalacima.app.ui.components.ScaleSelector
import com.rutaalacima.app.ui.theme.EstiloCita

/** Acceso a las respuestas para el renderizador (lo implementa el ViewModel de edición). */
interface Respuestas {
    fun valor(clave: String): String
    fun escribir(clave: String, valor: String)
    fun fijar(clave: String, valor: String)
}

fun WorkbookEditorViewModel.comoRespuestas(): Respuestas = object : Respuestas {
    override fun valor(clave: String) = this@comoRespuestas.valor(clave)
    override fun escribir(clave: String, valor: String) = this@comoRespuestas.escribir(clave, valor)
    override fun fijar(clave: String, valor: String) = this@comoRespuestas.fijar(clave, valor)
}

@Composable
fun BlockView(block: Block, r: Respuestas, modifier: Modifier = Modifier) {
    when (block) {
        is HeadingBlock -> HeadingView(block, modifier)
        is ParagraphBlock -> Text(block.text, style = MaterialTheme.typography.bodyLarge, modifier = modifier.fillMaxWidth())
        is BulletBlock -> BulletView(block, modifier)
        is QuoteBlock -> QuoteView(block.text, modifier)
        is CalloutBlock -> CalloutView(block, r, modifier)
        is TableBlock -> TableView(block, modifier)
        is PromptBlock -> PromptView(block, r, modifier)
        is ScaleBlock -> ScaleView(block, r, modifier)
        is CheckBlock -> CheckView(block, r, modifier)
        is ChoiceBlock -> ChoiceView(block, r, modifier)
        is InputTableBlock -> InputTableView(block, r, modifier)
    }
}

@Composable
private fun HeadingView(b: HeadingBlock, modifier: Modifier) {
    val style = when (b.level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.titleMedium
    }
    Text(
        b.text,
        style = style,
        color = if (b.level <= 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

@Composable
private fun BulletView(b: BulletBlock, modifier: Modifier) {
    val (mark, color) = when (b.mark) {
        "✓" -> "✓" to Color(0xFF2E7D32)
        "✗" -> "✗" to Color(0xFFB3261E)
        "→" -> "→" to MaterialTheme.colorScheme.secondary
        else -> "•" to MaterialTheme.colorScheme.secondary
    }
    Row(modifier.fillMaxWidth()) {
        Text(mark, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
        Text(b.text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun QuoteView(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 4.dp)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(12.dp))
        Text("“$text”", style = EstiloCita, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CalloutView(b: CalloutBlock, r: Respuestas, modifier: Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            b.blocks.forEach { BlockView(it, r) }
        }
    }
}

@Composable
private fun TableView(b: TableBlock, modifier: Modifier) {
    val cols = maxOf(b.headers.size, b.rows.maxOfOrNull { it.size } ?: 0)
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(12.dp)) {
            if (cols <= 3) {
                if (b.headers.any { it.isNotBlank() }) {
                    TableRow(b.headers, cols, header = true)
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.secondary)
                }
                b.rows.forEachIndexed { i, row ->
                    TableRow(row, cols, header = false)
                    if (i < b.rows.lastIndex) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            } else {
                // Muchas columnas: cada fila como ficha "encabezado: valor".
                b.rows.forEachIndexed { i, row ->
                    row.forEachIndexed { j, cell ->
                        if (cell.isNotBlank()) {
                            Text(
                                (b.headers.getOrNull(j)?.takeIf { it.isNotBlank() }?.let { "$it: " } ?: "") + cell,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (j == 0) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                    if (i < b.rows.lastIndex) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, cols: Int, header: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (j in 0 until cols) {
            val w = if (cols == 3 && j == 0) 0.6f else if (cols == 3 && j == 1) 1f else 1f
            Text(
                cells.getOrNull(j).orEmpty(),
                modifier = Modifier.weight(w),
                style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = if (header || j == 0) FontWeight.Bold else FontWeight.Normal,
                color = if (header) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PromptView(b: PromptBlock, r: Respuestas, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            b.number?.let { n ->
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(n, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                b.title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                if (b.label.isNotBlank()) {
                    Text(
                        b.label,
                        style = if (b.title == null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        color = if (b.title == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        OutlinedTextField(
            value = r.valor(b.id),
            onValueChange = { r.escribir(b.id, it) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("Escribe aquí…") },
            textStyle = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ScaleView(b: ScaleBlock, r: Respuestas, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(b.label, style = MaterialTheme.typography.titleMedium)
        ScaleSelector(
            value = r.valor(b.id).toIntOrNull(),
            onChange = { r.fijar(b.id, it?.toString().orEmpty()) },
            min = b.min,
            max = b.max,
        )
    }
}

@Composable
private fun CheckView(b: CheckBlock, r: Respuestas, modifier: Modifier) {
    val checked = r.valor(b.id) == "1"
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { r.fijar(b.id, if (checked) "" else "1") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { r.fijar(b.id, if (it) "1" else "") })
        Text(b.label, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceView(b: ChoiceBlock, r: Respuestas, modifier: Modifier) {
    val actual = r.valor(b.id)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (b.label.isNotBlank()) Text(b.label, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            b.options.forEach { opt ->
                FilterChip(
                    selected = actual == opt,
                    onClick = { r.fijar(b.id, if (actual == opt) "" else opt) },
                    label = { Text(opt) },
                )
            }
        }
    }
}

@Composable
private fun InputTableView(b: InputTableBlock, r: Respuestas, modifier: Modifier) {
    val claveFilas = "${b.id}#n"
    val conEtiquetas = b.rowLabels.isNotEmpty()
    val filas = if (conEtiquetas) b.rowLabels.size else (r.valor(claveFilas).toIntOrNull() ?: b.rows)
    val columnas = b.editableColumns.ifEmpty { listOf("") }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (fila in 0 until filas) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (conEtiquetas) b.rowLabels[fila] else "${b.headers.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Fila"} ${fila + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    columnas.forEachIndexed { col, titulo ->
                        val clave = b.cellKey(fila, col)
                        OutlinedTextField(
                            value = r.valor(clave),
                            onValueChange = { r.escribir(clave, it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = titulo.takeIf { it.isNotBlank() }?.let { t -> @Composable { Text(t) } },
                            minLines = 1,
                        )
                    }
                }
            }
        }
        if (!conEtiquetas) {
            TextButton(onClick = { r.fijar(claveFilas, (filas + 1).toString()) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Agregar fila", textAlign = TextAlign.Center)
            }
        }
    }
}
