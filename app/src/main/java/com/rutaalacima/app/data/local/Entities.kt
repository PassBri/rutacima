package com.rutaalacima.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Perfil del senderista (una sola fila, id = 1). */
@Entity(tableName = "perfil")
data class PerfilEntity(
    @PrimaryKey val id: Int = 1,
    val nombre: String = "",
    /** "Mi Cumbre Personal en una frase" (máx. 25 palabras). */
    val cumbreFrase: String = "",
    /** Nombre del enum Fase. */
    val faseActual: String? = null,
    val anioInicioPlan: Int = 2026,
    val compromisoFirmadoEn: Long? = null,
    val onboardingCompleto: Boolean = false,
)

/** Respuesta a un campo de un workbook. clave = id del bloque (o id#fila_col en tablas). */
@Entity(tableName = "respuestas", indices = [Index("workbookId")])
data class RespuestaEntity(
    @PrimaryKey val clave: String,
    val workbookId: String,
    val valor: String,
    val actualizadoEn: Long = System.currentTimeMillis(),
)

/** Evaluación de los 6 ejes (1-10 cada uno). */
@Entity(tableName = "evaluaciones_ejes")
data class EvaluacionEjesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: Long = System.currentTimeMillis(),
    val voluntad: Int,
    val maestria: Int,
    val voz: Int,
    val valor: Int,
    val evolucion: Int,
    val trascendencia: Int,
    /** Origen: "rapida", "revision_mensual", "cierre_mensual"… */
    val origen: String = "rapida",
    val nota: String = "",
)

fun EvaluacionEjesEntity.puntajes(): List<Int> = listOf(voluntad, maestria, voz, valor, evolucion, trascendencia)
fun EvaluacionEjesEntity.total(): Int = puntajes().sum()

/** Checklist diario por eje. fecha = yyyy-MM-dd; marcados = ids separados por coma. */
@Entity(tableName = "checklist_diario")
data class ChecklistDiarioEntity(
    @PrimaryKey val fecha: String,
    val marcados: String = "",
)

fun ChecklistDiarioEntity?.ids(): Set<String> =
    this?.marcados?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

/** Propósito a 5 años (hasta 10). */
@Entity(tableName = "propositos")
data class PropositoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orden: Int = 0,
    val titulo: String,
    /** Metas específicas dentro de este propósito, una por línea. */
    val metasEspecificas: String = "",
    val prioridad: String = "B",
    /** Código del eje principal que activa (VOL, MAE…), opcional. */
    val eje: String? = null,
    val descripcion: String = "",
    val indicadorExito: String = "",
    val visualizacion: String = "",
    val porQueImporta: String = "",
    val impacto: String = "",
    val reflexionFinal: String = "",
    val progreso: Int = 0,
    val creadoEn: Long = System.currentTimeMillis(),
)

/** Paso del plan de acción de un propósito (hasta 10 por propósito). */
@Entity(
    tableName = "acciones",
    foreignKeys = [ForeignKey(
        entity = PropositoEntity::class,
        parentColumns = ["id"],
        childColumns = ["propositoId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("propositoId")],
)
data class AccionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propositoId: Long,
    val orden: Int = 0,
    val texto: String,
    val fechaInicio: Long? = null,
    val fechaFin: Long? = null,
    val hecha: Boolean = false,
)

/** Meta anual (campamento base) con matriz de decisión, prioridad y semáforo. */
@Entity(tableName = "metas_anuales", indices = [Index("anio")])
data class MetaAnualEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val anio: Int,
    val propositoId: Long? = null,
    val titulo: String,
    /** Sub-metas a, b, c, d… una por línea. */
    val subMetas: String = "",
    val decision: String = "CONTINUAR",
    val prioridad: String = "B",
    val avance: Int = 0,
    val estado: String = "NO_INICIADA",
    val obstaculo: String = "",
    val proximaAccion: String = "",
    val fechaRevision: Long? = null,
)

/** Balance anual: "¿Cómo me sentí?", reflexión positiva/negativa, lo mejor y objetivos. */
@Entity(tableName = "balances_anuales")
data class BalanceAnualEntity(
    @PrimaryKey val anio: Int,
    val comoMeSenti: String = "",
    val positivo: String = "",
    val negativo: String = "",
    val loMejor: String = "",
    val objetivosProximo: String = "",
)
