package com.rutaalacima.app.ui.planner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.data.local.AccionEntity
import com.rutaalacima.app.data.local.BalanceAnualEntity
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.PerfilEntity
import com.rutaalacima.app.data.local.PropositoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlannerViewModel(private val c: AppContainer) : ViewModel() {
    val perfil: StateFlow<PerfilEntity> =
        c.perfil.perfil.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilEntity())
    val propositos: StateFlow<List<PropositoEntity>> =
        c.planificador.propositos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val metas: StateFlow<List<MetaAnualEntity>> =
        c.planificador.todasLasMetas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun balance(anio: Int): Flow<BalanceAnualEntity?> = c.planificador.balance(anio)

    /** Las sub-metas 1 a 8 de una meta con Brújula también escriben sus pasos. */
    fun guardarMeta(m: MetaAnualEntity) = viewModelScope.launch {
        val casilla = if (m.id != 0L) c.sincro.casillaDeMeta(m) else null
        c.planificador.guardarMeta(if (casilla != null) m.copy(subMetas = c.sincro.lineasEditadas(casilla, com.rutaalacima.app.data.vision.SincroBrujula.lineas(m.subMetas))) else m)
    }
    init { viewModelScope.launch { runCatching { c.sincro.reconciliar() } } }
    /** El campamento de la meta que se está editando (null si no tiene Brújula) y sus pasos ganados. */
    var vinculo by mutableStateOf<com.rutaalacima.app.data.local.VisionCasillaEntity?>(null)
        private set
    fun cargarVinculo(m: MetaAnualEntity) = viewModelScope.launch { vinculo = if (m.id != 0L) c.sincro.casillaDeMeta(m) else null }
    /** Planes de acción de las metas (libro "metas"), igual que la web. */
    val planes: StateFlow<Map<String, String>> =
        c.respuestas.observar(PlanMeta.WB).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    internal fun guardarPlan(metaId: Long, pasos: List<PasoPlan>) = viewModelScope.launch {
        c.respuestas.guardar(PlanMeta.WB, PlanMeta.clave(metaId), PlanMeta.escribir(pasos.filter { it.t.isNotBlank() }))
    }
    val mandala: StateFlow<Map<String, String>> =
        c.respuestas.observar(com.rutaalacima.app.domain.model.Mandala.WORKBOOK).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    fun alternarGanado(casillaId: Long, paso: Int) = viewModelScope.launch { alternarGanadoPaso(c, casillaId, paso) }
    fun eliminarMeta(m: MetaAnualEntity) = viewModelScope.launch { c.planificador.eliminarMeta(m) }
    fun guardarBalance(b: BalanceAnualEntity) = viewModelScope.launch { c.planificador.guardarBalance(b) }
    fun mesesConBalance(anio: Int): Flow<Set<Int>> = c.planAnual.mesesConBalance(anio)
    fun cambiarAnioInicio(anio: Int) = viewModelScope.launch { c.perfil.actualizar { it.copy(anioInicioPlan = anio) } }
}

/** Edición de un propósito (id = 0 para uno nuevo) y su plan de acción. */
class PropositoViewModel(private val c: AppContainer, idInicial: Long) : ViewModel() {
    var apodo by mutableStateOf("")
    var proposito by mutableStateOf(PropositoEntity(titulo = ""))
        private set
    var id by mutableStateOf(idInicial)
        private set
    var cargado by mutableStateOf(idInicial == 0L)
        private set
    var guardado by mutableStateOf(false)
        private set

    private val idFlow = MutableStateFlow(idInicial)

    @OptIn(ExperimentalCoroutinesApi::class)
    val acciones: StateFlow<List<AccionEntity>> = idFlow
        .flatMapLatest { pid -> if (pid == 0L) flowOf(emptyList()) else c.planificador.acciones(pid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Todas las metas anuales: para la cinta de años de esta cumbre personal. */
    val anuales: StateFlow<List<com.rutaalacima.app.data.local.MetaAnualEntity>> =
        c.planificador.todasLasMetas.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (idInicial != 0L) {
            viewModelScope.launch {
                c.planificador.proposito(idInicial).first()?.let { proposito = it }
                apodo = c.respuestas.cargar("apodo")["apodo:p:$idInicial"].orEmpty()
                cargado = true
            }
        }
    }

    fun editarApodo(texto: String) { apodo = texto.take(32); guardado = false }

    /** El campamento de este propósito en la Brújula general (null si no lo tiene). */
    var vinculo by mutableStateOf<com.rutaalacima.app.data.local.VisionCasillaEntity?>(null)
        private set
    fun cargarVinculo() = viewModelScope.launch { vinculo = if (proposito.id != 0L) c.sincro.casillaDeProposito(proposito) else null }
    val mandala: StateFlow<Map<String, String>> =
        c.respuestas.observar(com.rutaalacima.app.domain.model.Mandala.WORKBOOK).stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    fun alternarGanado(casillaId: Long, paso: Int) = viewModelScope.launch { alternarGanadoPaso(c, casillaId, paso) }

    fun editar(transform: (PropositoEntity) -> PropositoEntity) {
        proposito = transform(proposito)
        guardado = false
    }

    fun guardar(onDone: (() -> Unit)? = null) {
        if (proposito.titulo.isBlank()) return
        viewModelScope.launch {
            vinculo?.let { cas -> proposito = proposito.copy(metasEspecificas = c.sincro.lineasEditadas(cas, com.rutaalacima.app.data.vision.SincroBrujula.lineas(proposito.metasEspecificas))) }
            val nuevoId = c.planificador.guardarProposito(proposito)
            if (id == 0L) {
                id = nuevoId
                proposito = proposito.copy(id = nuevoId)
                idFlow.value = nuevoId
            }
            c.respuestas.guardar("apodo", "apodo:p:$id", apodo.trim())
            guardado = true
            onDone?.invoke()
        }
    }

    fun eliminar(onDone: () -> Unit) {
        if (id == 0L) { onDone(); return }
        viewModelScope.launch { c.planificador.eliminarProposito(proposito); onDone() }
    }

    fun agregarAccion(texto: String) {
        if (id == 0L || texto.isBlank()) return
        viewModelScope.launch {
            c.planificador.guardarAccion(AccionEntity(propositoId = id, orden = acciones.value.size + 1, texto = texto.trim()))
        }
    }

    fun alternarAccion(a: AccionEntity) = viewModelScope.launch { c.planificador.guardarAccion(a.copy(hecha = !a.hecha)) }
    fun fechaAccion(a: AccionEntity, millis: Long?) = viewModelScope.launch { c.planificador.guardarAccion(a.copy(fechaFin = millis)) }
    fun eliminarAccion(a: AccionEntity) = viewModelScope.launch { c.planificador.eliminarAccion(a) }
}

/** Ganar o quitar un paso desde Metas (igual que en la Brújula): al quitarlo, la última jornada sale. */
internal suspend fun alternarGanadoPaso(c: AppContainer, casillaId: Long, paso: Int) {
    val M = com.rutaalacima.app.domain.model.Mandala
    val r = c.respuestas.cargar(M.WORKBOOK)
    val x = com.rutaalacima.app.domain.model.MetodoCima.leer(casillaId, paso, r)
    if (!x.cumplido) { c.respuestas.guardar(M.WORKBOOK, M.claveHecho(casillaId, paso), java.time.LocalDate.now().toString()); return }
    c.respuestas.guardar(M.WORKBOOK, M.claveHecho(casillaId, paso), "")
    val req = x.requeridas ?: com.rutaalacima.app.domain.model.MetodoCima.DIFICULTAD_INICIAL
    if (x.jornadas.size >= req) c.respuestas.guardar(M.WORKBOOK, com.rutaalacima.app.domain.model.MetodoCima.claveJornadas(casillaId, paso), x.jornadas.sorted().take(req - 1).joinToString(","))
}
