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
    val fechaInicio: Long? = null,
    val fechaFin: Long? = null,
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

/**
 * Página del Planificador diario. fecha = yyyy-MM-dd.
 * [horario] guarda las franjas "Planifique su día" como JSON {"4": "…", "5": "…"} (hora 0-23).
 */
@Entity(tableName = "agenda_diaria")
data class AgendaDiaEntity(
    @PrimaryKey val fecha: String,
    val intencion: String = "",
    val prioridad: String = "",
    val horario: String = "{}",
    val metas: String = "",
    val pendientes: String = "",
    val victorias: String = "",
    val aprendizaje: String = "",
    val gratitud: String = "",
    val ingresos: String = "",
    val gastos: String = "",
    val ahorro: String = "",
    val inversion: String = "",
    /** Energía 0-5 (○ ○ ○ ○ ○). */
    val energia: Int = 0,
    /** Vasos de agua 0-8. */
    val agua: Int = 0,
    val notas: String = "",
)

/** Página mensual del Plan anual: balance, actividades y notas. clave = yyyy-MM. */
@Entity(tableName = "meses", indices = [Index("anio")])
data class MesEntity(
    @PrimaryKey val clave: String,
    val anio: Int,
    val mes: Int,
    val comoEstuvo: String = "",
    val agradecido: String = "",
    val mejorar: String = "",
    val logros: String = "",
    val desafios: String = "",
    val objetivosProximo: String = "",
    val actividades: String = "",
    val notas: String = "",
)

/** Meta mensual con registro de cumplimiento por día (dias = "1,2,15"). */
@Entity(tableName = "metas_mensuales", indices = [Index(value = ["anio", "mes"])])
data class MetaMensualEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val anio: Int,
    val mes: Int,
    val orden: Int = 0,
    val texto: String,
    val dias: String = "",
    val cumplida: Boolean = false,
)

fun MetaMensualEntity.diasMarcados(): Set<Int> = dias.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
