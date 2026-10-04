package com.rutaalacima.app.data

import com.rutaalacima.app.data.local.AccionEntity
import com.rutaalacima.app.data.local.AgendaDiaEntity
import com.rutaalacima.app.data.local.MesEntity
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.BalanceAnualEntity
import com.rutaalacima.app.data.local.ChecklistDiarioEntity
import com.rutaalacima.app.data.local.EvaluacionEjesEntity
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.PerfilEntity
import com.rutaalacima.app.data.local.PropositoEntity
import com.rutaalacima.app.data.local.RespuestaEntity
import com.rutaalacima.app.data.local.RutaDatabase
import com.rutaalacima.app.data.local.ids
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class PerfilRepository(private val db: RutaDatabase) {
    val perfil: Flow<PerfilEntity> = db.perfilDao().observe().map { it ?: PerfilEntity() }

    suspend fun actualizar(transform: (PerfilEntity) -> PerfilEntity) {
        val actual = db.perfilDao().get() ?: PerfilEntity()
        db.perfilDao().upsert(transform(actual))
    }
}

class RespuestasRepository(private val db: RutaDatabase) {
    fun observar(workbookId: String): Flow<Map<String, String>> =
        db.respuestaDao().observeWorkbook(workbookId).map { list -> list.associate { it.clave to it.valor } }

    suspend fun cargar(workbookId: String): Map<String, String> =
        db.respuestaDao().getWorkbook(workbookId).associate { it.clave to it.valor }

    /** Número de campos respondidos por workbook. */
    val conteos: Flow<Map<String, Int>> =
        db.respuestaDao().observeConteos().map { list -> list.associate { it.workbookId to it.respondidas } }

    val ultimaRespuesta: Flow<RespuestaEntity?> = db.respuestaDao().observeUltima()

    /** workbookId -> índice de la última sección abierta. */
    val ultimasSecciones: Flow<Map<String, Int>> = db.respuestaDao().observeUltimasSecciones().map { list ->
        list.associate { it.workbookId to (it.valor.toIntOrNull() ?: 0) }
    }

    suspend fun guardar(workbookId: String, clave: String, valor: String) {
        if (valor.isEmpty()) db.respuestaDao().delete(clave)
        else db.respuestaDao().upsert(RespuestaEntity(clave = clave, workbookId = workbookId, valor = valor))
    }
}

class EjesRepository(private val db: RutaDatabase) {
    val historial: Flow<List<EvaluacionEjesEntity>> = db.evaluacionDao().observeTodas()
    val ultima: Flow<EvaluacionEjesEntity?> = db.evaluacionDao().observeUltima()

    suspend fun guardar(evaluacion: EvaluacionEjesEntity) = db.evaluacionDao().upsert(evaluacion)
    suspend fun eliminar(evaluacion: EvaluacionEjesEntity) = db.evaluacionDao().delete(evaluacion)
}

class ChecklistRepository(private val db: RutaDatabase) {
    fun dia(fecha: LocalDate): Flow<Set<String>> =
        db.checklistDao().observe(fecha.toString()).map { it.ids() }

    /** Días desde la fecha indicada (para racha y tracker semanal). */
    fun desde(fecha: LocalDate): Flow<Map<LocalDate, Set<String>>> =
        db.checklistDao().observeDesde(fecha.toString()).map { list ->
            list.associate { LocalDate.parse(it.fecha) to it.ids() }
        }

    suspend fun alternar(fecha: LocalDate, habitoId: String) {
        val actual = db.checklistDao().get(fecha.toString()).ids()
        val nuevo = if (habitoId in actual) actual - habitoId else actual + habitoId
        db.checklistDao().upsert(ChecklistDiarioEntity(fecha.toString(), nuevo.sorted().joinToString(",")))
    }
}

class PlanificadorRepository(private val db: RutaDatabase) {
    private val dao = db.planificadorDao()

    val propositos: Flow<List<PropositoEntity>> = dao.observePropositos()
    fun proposito(id: Long): Flow<PropositoEntity?> = dao.observeProposito(id)
    fun acciones(propositoId: Long): Flow<List<AccionEntity>> = dao.observeAcciones(propositoId)
    fun metas(anio: Int): Flow<List<MetaAnualEntity>> = dao.observeMetas(anio)
    val todasLasMetas: Flow<List<MetaAnualEntity>> = dao.observeTodasLasMetas()
    fun balance(anio: Int): Flow<BalanceAnualEntity?> = dao.observeBalance(anio)

    suspend fun guardarProposito(p: PropositoEntity): Long =
        if (p.id == 0L) dao.insertProposito(p.copy(orden = dao.contarPropositos() + 1))
        else { dao.updateProposito(p); p.id }

    suspend fun eliminarProposito(p: PropositoEntity) = dao.deleteProposito(p)
    suspend fun guardarAccion(a: AccionEntity) = dao.upsertAccion(a)
    suspend fun eliminarAccion(a: AccionEntity) = dao.deleteAccion(a)
    suspend fun guardarMeta(m: MetaAnualEntity) = dao.upsertMeta(m)
    suspend fun eliminarMeta(m: MetaAnualEntity) = dao.deleteMeta(m)
    suspend fun guardarBalance(b: BalanceAnualEntity) = dao.upsertBalance(b)
}

class AgendaRepository(private val db: RutaDatabase) {
    suspend fun dia(fecha: LocalDate): AgendaDiaEntity =
        db.agendaDao().get(fecha.toString()) ?: AgendaDiaEntity(fecha = fecha.toString())

    suspend fun guardar(dia: AgendaDiaEntity) = db.agendaDao().upsert(dia)

    /** Días del mes que tienen agenda diligenciada. */
    fun diasConAgenda(anio: Int, mes: Int): Flow<Set<Int>> =
        db.agendaDao().observeFechasDelMes("%04d-%02d".format(anio, mes)).map { fechas ->
            fechas.mapNotNull { runCatching { LocalDate.parse(it).dayOfMonth }.getOrNull() }.toSet()
        }
}

class PlanAnualRepository(private val db: RutaDatabase) {
    private val dao = db.planAnualDao()

    suspend fun mes(anio: Int, mes: Int): MesEntity =
        dao.getMes(clave(anio, mes)) ?: MesEntity(clave = clave(anio, mes), anio = anio, mes = mes)

    suspend fun guardarMes(m: MesEntity) = dao.upsertMes(m)
    fun mesesConBalance(anio: Int): Flow<Set<Int>> = dao.observeMesesConBalance(anio).map { it.toSet() }
    fun metasMes(anio: Int, mes: Int): Flow<List<MetaMensualEntity>> = dao.observeMetasMes(anio, mes)
    suspend fun guardarMetaMes(m: MetaMensualEntity) = dao.upsertMetaMes(m)
    suspend fun eliminarMetaMes(m: MetaMensualEntity) = dao.deleteMetaMes(m)

    companion object {
        fun clave(anio: Int, mes: Int) = "%04d-%02d".format(anio, mes)
    }
}
