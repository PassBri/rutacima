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

    fun guardarMeta(m: MetaAnualEntity) = viewModelScope.launch { c.planificador.guardarMeta(m) }
    fun eliminarMeta(m: MetaAnualEntity) = viewModelScope.launch { c.planificador.eliminarMeta(m) }
    fun guardarBalance(b: BalanceAnualEntity) = viewModelScope.launch { c.planificador.guardarBalance(b) }
    fun mesesConBalance(anio: Int): Flow<Set<Int>> = c.planAnual.mesesConBalance(anio)
    fun cambiarAnioInicio(anio: Int) = viewModelScope.launch { c.perfil.actualizar { it.copy(anioInicioPlan = anio) } }
}

/** Edición de un propósito (id = 0 para uno nuevo) y su plan de acción. */
class PropositoViewModel(private val c: AppContainer, idInicial: Long) : ViewModel() {
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

    init {
        if (idInicial != 0L) {
            viewModelScope.launch {
                c.planificador.proposito(idInicial).first()?.let { proposito = it }
                cargado = true
            }
        }
    }

    fun editar(transform: (PropositoEntity) -> PropositoEntity) {
        proposito = transform(proposito)
        guardado = false
    }

    fun guardar(onDone: (() -> Unit)? = null) {
        if (proposito.titulo.isBlank()) return
        viewModelScope.launch {
            val nuevoId = c.planificador.guardarProposito(proposito)
            if (id == 0L) {
                id = nuevoId
                proposito = proposito.copy(id = nuevoId)
                idFlow.value = nuevoId
            }
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
