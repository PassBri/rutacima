package com.rutaalacima.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.rutaalacima.app.ui.RutaRoot
import com.rutaalacima.app.ui.theme.RutaTheme

/** AppCompatActivity para que el cambio de idioma por app (12 idiomas) se aplique al instante. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pedirPermisoNotificaciones()
        setContent {
            RutaTheme {
                RutaRoot()
            }
        }
    }

    /** Android 13+: pide una sola vez el permiso para el recordatorio diario de los 120 años. */
    private val permiso = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    private fun pedirPermisoNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        val prefs = getSharedPreferences("ajustes_recordatorio", MODE_PRIVATE)
        if (prefs.getBoolean("permiso_pedido", false) || com.rutaalacima.app.notificaciones.Recordatorios.puedeNotificar(this)) return
        prefs.edit().putBoolean("permiso_pedido", true).apply()
        permiso.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}
