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

    @Query("SELECT * FROM metas_mensuales ORDER BY anio, mes, orden, id")
    fun observeTodasMetasMes(): Flow<List<MetaMensualEntity>>

    @Query("SELECT * FROM metas_mensuales WHERE anio = :anio AND mes = :mes ORDER BY orden, id")
    fun observeMetasMes(anio: Int, mes: Int): Flow<List<MetaMensualEntity>>

    @Upsert
    suspend fun upsertMetaMes(m: MetaMensualEntity)

    @Delete
    suspend fun deleteMetaMes(m: MetaMensualEntity)
}

@Dao
interface SocialDao {
    @Query("SELECT * FROM publicaciones ORDER BY creadaEn DESC")
    fun observePublicaciones(): Flow<List<PublicacionEntity>>

    @Query("SELECT * FROM publicaciones ORDER BY creadaEn DESC")
    suspend fun todas(): List<PublicacionEntity>

    @Query("SELECT * FROM publicaciones WHERE id = :id")
    suspend fun get(id: String): PublicacionEntity?

    @Upsert
    suspend fun upsert(p: PublicacionEntity)

    @Query("DELETE FROM publicaciones WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM comentarios_locales WHERE publicacionId = :id ORDER BY creadoEn")
    suspend fun comentarios(id: String): List<ComentarioLocalEntity>

    @Insert
    suspend fun comentar(c: ComentarioLocalEntity)

    @Query("SELECT COUNT(*) FROM comentarios_locales WHERE publicacionId = :id")
    suspend fun contarComentarios(id: String): Int
}

@Dao
interface CoachDao {
    @Query("SELECT * FROM coach_mensajes ORDER BY creadoEn, id")
    fun observe(): Flow<List<CoachMensajeEntity>>

    @Query("SELECT * FROM coach_mensajes ORDER BY creadoEn DESC, id DESC LIMIT :n")
    suspend fun ultimos(n: Int): List<CoachMensajeEntity>

    @Insert
    suspend fun insert(m: CoachMensajeEntity)

    @Query("DELETE FROM coach_mensajes")
    suspend fun borrarTodo()
}

/** Lecturas y escrituras en bloque para sincronizar con Rutaalacima Web. */
@Dao
interface SincroniaDao {
    @Query("SELECT * FROM perfil") suspend fun perfiles(): List<PerfilEntity>
    @Query("SELECT * FROM propositos") suspend fun propositos(): List<PropositoEntity>
    @Query("SELECT * FROM acciones") suspend fun acciones(): List<AccionEntity>
    @Query("SELECT * FROM metas_anuales") suspend fun metasAnuales(): List<MetaAnualEntity>
    @Query("SELECT * FROM metas_mensuales") suspend fun metasMensuales(): List<MetaMensualEntity>
    @Query("SELECT * FROM balances_anuales") suspend fun balances(): List<BalanceAnualEntity>
    @Query("SELECT * FROM agenda_diaria WHERE fecha >= :desde") suspend fun agenda(desde: String): List<AgendaDiaEntity>
    @Query("SELECT * FROM meses") suspend fun meses(): List<MesEntity>
    @Query("SELECT * FROM checklist_diario WHERE fecha >= :desde") suspend fun checklist(desde: String): List<ChecklistDiarioEntity>
    @Query("SELECT * FROM evaluaciones_ejes") suspend fun evaluaciones(): List<EvaluacionEjesEntity>
    @Query("SELECT * FROM respuestas") suspend fun respuestas(): List<RespuestaEntity>
    @Query("SELECT * FROM coach_mensajes") suspend fun coach(): List<CoachMensajeEntity>
    @Query("SELECT * FROM vision_casillas") suspend fun vision(): List<VisionCasillaEntity>

    @Upsert suspend fun guardar(e: PerfilEntity)
    @Upsert suspend fun guardar(e: PropositoEntity)
    @Upsert suspend fun guardar(e: AccionEntity)
    @Upsert suspend fun guardar(e: MetaAnualEntity)
    @Upsert suspend fun guardar(e: MetaMensualEntity)
    @Upsert suspend fun guardar(e: BalanceAnualEntity)
    @Upsert suspend fun guardar(e: AgendaDiaEntity)
    @Upsert suspend fun guardar(e: MesEntity)
    @Upsert suspend fun guardar(e: ChecklistDiarioEntity)
    @Upsert suspend fun guardar(e: EvaluacionEjesEntity)
    @Upsert suspend fun guardar(e: RespuestaEntity)
    @Upsert suspend fun guardar(e: CoachMensajeEntity)
    @Upsert suspend fun guardar(e: VisionCasillaEntity)

    @Query("DELETE FROM propositos WHERE id = :id") suspend fun borrarProposito(id: Long)
    @Query("DELETE FROM acciones WHERE id = :id") suspend fun borrarAccion(id: Long)
    @Query("DELETE FROM metas_anuales WHERE id = :id") suspend fun borrarMetaAnual(id: Long)
    @Query("DELETE FROM metas_mensuales WHERE id = :id") suspend fun borrarMetaMensual(id: Long)
    @Query("DELETE FROM balances_anuales WHERE anio = :anio") suspend fun borrarBalance(anio: Int)
    @Query("DELETE FROM agenda_diaria WHERE fecha = :fecha") suspend fun borrarAgenda(fecha: String)
    @Query("DELETE FROM meses WHERE clave = :clave") suspend fun borrarMes(clave: String)
    @Query("DELETE FROM checklist_diario WHERE fecha = :fecha") suspend fun borrarChecklist(fecha: String)
    @Query("DELETE FROM evaluaciones_ejes WHERE id = :id") suspend fun borrarEvaluacion(id: Long)
    @Query("DELETE FROM respuestas WHERE clave = :clave") suspend fun borrarRespuesta(clave: String)
    @Query("DELETE FROM coach_mensajes WHERE id = :id") suspend fun borrarCoach(id: Long)
    @Query("DELETE FROM vision_casillas WHERE id = :id") suspend fun borrarVision(id: Long)
}

@Dao
interface VisionDao {
    @Query("SELECT * FROM vision_casillas ORDER BY orden, id")
    fun observe(): Flow<List<VisionCasillaEntity>>

    @Query("SELECT * FROM vision_casillas ORDER BY orden, id")
    suspend fun todas(): List<VisionCasillaEntity>

    @Upsert
    suspend fun guardar(c: VisionCasillaEntity): Long

    @Query("DELETE FROM vision_casillas WHERE id = :id")
    suspend fun borrar(id: Long)
}
