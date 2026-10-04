package com.rutaalacima.app

import android.content.Context
import com.rutaalacima.app.data.ChecklistRepository
import com.rutaalacima.app.data.EjesRepository
import com.rutaalacima.app.data.PerfilRepository
import com.rutaalacima.app.data.PlanificadorRepository
import com.rutaalacima.app.data.RespuestasRepository
import com.rutaalacima.app.data.content.ContentRepository
import com.rutaalacima.app.data.local.RutaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Inyección de dependencias manual (suficiente para el tamaño actual del proyecto). */
class AppContainer(context: Context) {
    private val db = RutaDatabase.build(context)

    /** Scope de la aplicación: para guardados que deben terminar aunque se cierre la pantalla. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val contenido = ContentRepository(context)
    val perfil = PerfilRepository(db)
    val respuestas = RespuestasRepository(db)
    val ejes = EjesRepository(db)
    val checklist = ChecklistRepository(db)
    val planificador = PlanificadorRepository(db)
}
