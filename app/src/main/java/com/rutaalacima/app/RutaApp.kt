package com.rutaalacima.app

import android.app.Application

class RutaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Recordatorio diario del camino hacia los 120 años
        com.rutaalacima.app.notificaciones.Recordatorios.crearCanal(this)
        com.rutaalacima.app.notificaciones.Recordatorios.programar(this)
    }
}
