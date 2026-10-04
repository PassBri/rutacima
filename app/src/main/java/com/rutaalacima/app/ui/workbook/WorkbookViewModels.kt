package com.rutaalacima.app.ui.workbook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.data.content.InputBlock
import com.rutaalacima.app.data.content.InputTableBlock
import com.rutaalacima.app.data.content.Section
import com.rutaalacima.app.data.content.Workbook
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ¿El campo tiene respuesta? (para tablas: al menos una celda). */
fun InputBlock.respondido(respuestas: Map<String, String>): Boolean = when (this) {
    is InputTableBlock -> respuestas.keys.any { it.startsWith("$id#") && !it.endsWith("#n") && respuestas[it].orEmpty().isNotBlank() }
    else -> respuestas[id].orEmpty().isNotBlank()
}

fun Section.progreso(respuestas: Map<String, String>): Float =
    if (fields.isEmpty()) -1f else fields.count { it.respondido(respuestas) }.toFloat() / fields.size

fun Workbook.progreso(respuestas: Map<String, String>): Float =
    if (fields.isEmpty()) 0f else fields.count { it.respondido(respuestas) }.toFloat() / fields.size

/** Índice del workbook: solo lectura de respuestas para mostrar el progreso por sección. */
class WorkbookIndexViewModel(c: AppContainer, workbookId: String) : ViewModel() {
    var workbook by mutableStateOf<Workbook?>(null)
        private set

    val respuestas: StateFlow<Map<String, String>> =
        c.respuestas.observar(workbookId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch { workbook = c.contenido.workbook(workbookId) }
    }
}

/**
 * Edición de un workbook. Mantiene las respuestas en memoria para que escribir sea fluido
 * y las persiste en Room con un pequeño retardo. Los guardados corren en el scope de la
 * aplicación para no perder lo último escrito al salir de la pantalla.
 */
class WorkbookEditorViewModel(private val c: AppContainer, val workbookId: String) : ViewModel() {
    var workbook by mutableStateOf<Workbook?>(null)
        private set
    var cargado by mutableStateOf(false)
        private set

    val respuestas = mutableStateMapOf<String, String>()
    private val pendientes = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            workbook = c.contenido.workbook(workbookId)
            respuestas.putAll(c.respuestas.cargar(workbookId))
            cargado = true
        }
    }

    fun valor(clave: String): String = respuestas[clave].orEmpty()

    /** Guarda con retardo (texto). */
    fun escribir(clave: String, valor: String) {
        actualizarLocal(clave, valor)
        pendientes[clave]?.cancel()
        pendientes[clave] = c.appScope.launch {
            delay(400)
            c.respuestas.guardar(workbookId, clave, valor)
        }
    }

    /** Guarda de inmediato (escalas, casillas, opciones). */
    fun fijar(clave: String, valor: String) {
        actualizarLocal(clave, valor)
        pendientes[clave]?.cancel()
        c.appScope.launch { c.respuestas.guardar(workbookId, clave, valor) }
    }

    var ejesGuardados by mutableStateOf(false)
        private set

    /** Guarda el resultado de una evaluación de los 6 ejes en "Mis Ejes" (radar e historial). */
    fun guardarEnEjes(v: List<Int>) {
        if (v.size != 6) return
        c.appScope.launch {
            c.ejes.guardar(
                com.rutaalacima.app.data.local.EvaluacionEjesEntity(
                    voluntad = v[0], maestria = v[1], voz = v[2], valor = v[3], evolucion = v[4], trascendencia = v[5],
                    origen = workbookId,
                ),
            )
        }
        ejesGuardados = true
    }

    private fun actualizarLocal(clave: String, valor: String) {
        if (valor.isEmpty()) respuestas.remove(clave) else respuestas[clave] = valor
    }
}
