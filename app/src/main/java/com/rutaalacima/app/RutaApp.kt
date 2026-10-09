package com.rutaalacima.app

import android.app.Application

class RutaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Reporte de errores anónimo: captura los cierres inesperados y envía los pendientes
        com.rutaalacima.app.diagnostico.Errores.instalar(this)
        container = AppContainer(this)
        com.rutaalacima.app.diagnostico.Errores.enviarPendientes(this, container.supabase)
        // Recordatorio diario del camino hacia los 120 años
        com.rutaalacima.app.notificaciones.Recordatorios.crearCanal(this)
        com.rutaalacima.app.notificaciones.Recordatorios.programar(this)
        // Avisos de mensajes, solicitudes y coach de vida (cada 15 minutos, si hay cuenta)
        com.rutaalacima.app.notificaciones.AvisosMensajes.programar(this)
    }
}
