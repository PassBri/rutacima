package com.rutaalacima.app.data.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Modelo del contenido de los workbooks. Los JSON de assets/content se generan
 * desde los .docx originales con tools/convert.py.
 */

@Serializable
data class WorkbookSummary(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val category: String = "ruta",
    val order: Int = 0,
    val sections: Int = 0,
    val inputs: Int = 0,
)

@Serializable
data class Workbook(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val category: String = "ruta",
    val order: Int = 0,
    val sections: List<Section> = emptyList(),
) {
    /** Todos los campos editables del workbook (incluye los anidados en recuadros). */
    val fields: List<InputBlock> by lazy { sections.flatMap { it.fields } }

    fun sectionIndexStartingWith(prefix: String): Int =
        sections.indexOfFirst { it.title.startsWith(prefix, ignoreCase = true) }
}

@Serializable
data class Section(
    val id: String,
    val title: String,
    val blocks: List<Block> = emptyList(),
) {
    val fields: List<InputBlock> by lazy { blocks.flatMap { it.inputs() } }
}

/** Categorías de la biblioteca. */
enum class Categoria(val clave: String, val titulo: String) {
    RUTA("ruta", "La Ruta"),
    HERRAMIENTAS("herramientas", "Herramientas"),
    BONOS("bonos", "Bonos"),
    FACILITADOR("facilitador", "Facilitadores");

    companion object {
        fun from(clave: String) = entries.firstOrNull { it.clave == clave } ?: RUTA
    }
}

@Serializable
sealed interface Block

/** Bloques que guardan una respuesta del usuario. */
sealed interface InputBlock : Block {
    val id: String
}

@Serializable
@SerialName("heading")
data class HeadingBlock(val text: String, val level: Int = 2) : Block

@Serializable
@SerialName("paragraph")
data class ParagraphBlock(val text: String) : Block

@Serializable
@SerialName("bullet")
data class BulletBlock(val text: String, val mark: String = "•") : Block

@Serializable
@SerialName("quote")
data class QuoteBlock(val text: String) : Block

@Serializable
@SerialName("callout")
data class CalloutBlock(val blocks: List<Block> = emptyList()) : Block

@Serializable
@SerialName("table")
data class TableBlock(
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
) : Block

@Serializable
@SerialName("prompt")
data class PromptBlock(
    override val id: String,
    val label: String = "",
    val title: String? = null,
    val number: String? = null,
) : InputBlock

@Serializable
@SerialName("scale")
data class ScaleBlock(
    override val id: String,
    val label: String,
    val min: Int = 1,
    val max: Int = 10,
) : InputBlock

@Serializable
@SerialName("check")
data class CheckBlock(override val id: String, val label: String) : InputBlock

@Serializable
@SerialName("choice")
data class ChoiceBlock(
    override val id: String,
    val label: String = "",
    val options: List<String> = emptyList(),
) : InputBlock

@Serializable
@SerialName("inputTable")
data class InputTableBlock(
    override val id: String,
    val headers: List<String> = emptyList(),
    val rowLabels: List<String> = emptyList(),
    val rows: Int = 3,
) : InputBlock {
    /** Columnas editables: si hay etiquetas de fila, la primera columna es la etiqueta. */
    val editableColumns: List<String>
        get() = if (rowLabels.isNotEmpty()) headers.drop(1) else headers

    fun cellKey(row: Int, col: Int) = "$id#${row}_$col"
}

fun Block.inputs(): List<InputBlock> = when (this) {
    is InputBlock -> listOf(this)
    is CalloutBlock -> blocks.flatMap { it.inputs() }
    else -> emptyList()
}

/** Json compartido para leer el contenido. */
val ContentJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "type"
    isLenient = true
}
