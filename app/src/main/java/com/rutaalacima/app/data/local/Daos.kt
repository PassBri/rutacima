package com.rutaalacima.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilDao {
    @Query("SELECT * FROM perfil WHERE id = 1")
    fun observe(): Flow<PerfilEntity?>

    @Query("SELECT * FROM perfil WHERE id = 1")
    suspend fun get(): PerfilEntity?

    @Upsert
    suspend fun upsert(perfil: PerfilEntity)
}

@Dao
interface RespuestaDao {
    @Query("SELECT * FROM respuestas WHERE workbookId = :workbookId")
    fun observeWorkbook(workbookId: String): Flow<List<RespuestaEntity>>

    @Query("SELECT * FROM respuestas WHERE workbookId = :workbookId")
    suspend fun getWorkbook(workbookId: String): List<RespuestaEntity>

    @Query("SELECT workbookId, COUNT(*) AS respondidas FROM respuestas WHERE valor != '' AND clave NOT LIKE '%#%' GROUP BY workbookId")
    fun observeConteos(): Flow<List<ConteoRespuestas>>

    @Upsert
    suspend fun upsert(respuesta: RespuestaEntity)

    @Query("DELETE FROM respuestas WHERE clave = :clave")
    suspend fun delete(clave: String)

    @Query("SELECT * FROM respuestas ORDER BY actualizadoEn DESC LIMIT 1")
    fun observeUltima(): Flow<RespuestaEntity?>

    /** Última sección abierta de cada workbook (clave "<workbookId>#ultima"). */
    @Query("SELECT * FROM respuestas WHERE clave LIKE '%#ultima'")
    fun observeUltimasSecciones(): Flow<List<RespuestaEntity>>
}

data class ConteoRespuestas(val workbookId: String, val respondidas: Int)

@Dao
interface EvaluacionDao {
    @Query("SELECT * FROM evaluaciones_ejes ORDER BY fecha DESC")
    fun observeTodas(): Flow<List<EvaluacionEjesEntity>>

    @Query("SELECT * FROM evaluaciones_ejes ORDER BY fecha DESC LIMIT 1")
    fun observeUltima(): Flow<EvaluacionEjesEntity?>

    @Upsert
    suspend fun upsert(evaluacion: EvaluacionEjesEntity)

    @Delete
    suspend fun delete(evaluacion: EvaluacionEjesEntity)
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_diario WHERE fecha = :fecha")
    fun observe(fecha: String): Flow<ChecklistDiarioEntity?>

    @Query("SELECT * FROM checklist_diario WHERE fecha = :fecha")
    suspend fun get(fecha: String): ChecklistDiarioEntity?

    @Query("SELECT * FROM checklist_diario WHERE fecha >= :desde ORDER BY fecha")
    fun observeDesde(desde: String): Flow<List<ChecklistDiarioEntity>>

    @Upsert
    suspend fun upsert(dia: ChecklistDiarioEntity)
}

@Dao
interface PlanificadorDao {
    // Propósitos
    @Query("SELECT * FROM propositos ORDER BY orden, id")
    fun observePropositos(): Flow<List<PropositoEntity>>

    @Query("SELECT * FROM propositos WHERE id = :id")
    fun observeProposito(id: Long): Flow<PropositoEntity?>

    @Query("SELECT COUNT(*) FROM propositos")
    suspend fun contarPropositos(): Int

    @Insert
    suspend fun insertProposito(p: PropositoEntity): Long

    @Update
    suspend fun updateProposito(p: PropositoEntity)

    @Delete
    suspend fun deleteProposito(p: PropositoEntity)

    // Acciones
    @Query("SELECT * FROM acciones WHERE propositoId = :propositoId ORDER BY orden, id")
    fun observeAcciones(propositoId: Long): Flow<List<AccionEntity>>

    @Upsert
    suspend fun upsertAccion(a: AccionEntity)

    @Delete
    suspend fun deleteAccion(a: AccionEntity)

    // Metas anuales
    @Query("SELECT * FROM metas_anuales WHERE anio = :anio ORDER BY prioridad, id")
    fun observeMetas(anio: Int): Flow<List<MetaAnualEntity>>

    @Query("SELECT * FROM metas_anuales ORDER BY anio, prioridad, id")
    fun observeTodasLasMetas(): Flow<List<MetaAnualEntity>>

    @Upsert
    suspend fun upsertMeta(m: MetaAnualEntity)

    @Delete
    suspend fun deleteMeta(m: MetaAnualEntity)

    // Balance anual
    @Query("SELECT * FROM balances_anuales WHERE anio = :anio")
    fun observeBalance(anio: Int): Flow<BalanceAnualEntity?>

    @Upsert
    suspend fun upsertBalance(b: BalanceAnualEntity)
}

@Dao
interface AgendaDao {
    @Query("SELECT * FROM agenda_diaria WHERE fecha = :fecha")
    suspend fun get(fecha: String): AgendaDiaEntity?

    /** Fechas (yyyy-MM-dd) con agenda en un mes; prefijo = "yyyy-MM". */
    @Query("SELECT fecha FROM agenda_diaria WHERE fecha LIKE :prefijo || '%'")
    fun observeFechasDelMes(prefijo: String): Flow<List<String>>

    @Upsert
    suspend fun upsert(dia: AgendaDiaEntity)
}

@Dao
interface PlanAnualDao {
    @Query("SELECT * FROM meses WHERE clave = :clave")
    suspend fun getMes(clave: String): MesEntity?

    /** Meses del año que ya tienen balance (para los círculos Ene…Dic). */
    @Query("SELECT mes FROM meses WHERE anio = :anio AND (comoEstuvo != '' OR logros != '' OR objetivosProximo != '')")
    fun observeMesesConBalance(anio: Int): Flow<List<Int>>

    @Upsert
    suspend fun upsertMes(m: MesEntity)

    @Query("SELECT * FROM metas_mensuales WHERE anio = :anio AND mes = :mes ORDER BY orden, id")
    fun observeMetasMes(anio: Int, mes: Int): Flow<List<MetaMensualEntity>>

    @Upsert
    suspend fun upsertMetaMes(m: MetaMensualEntity)

    @Delete
    suspend fun deleteMetaMes(m: MetaMensualEntity)
}
