package com.rutaalacima.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rutaalacima.app.domain.model.Sincronia
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.rutaalacima.app.ui.RutaRoot
import com.rutaalacima.app.ui.theme.RutaTheme

/** AppCompatActivity para que el cambio de idioma por app (12 idiomas) se aplique al instante. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla de inicio del sistema con el sello (debe ir antes de super.onCreate)
        installSplashScreen()
        com.rutaalacima.app.ui.theme.EstiloActual.cargar(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pedirPermisoNotificaciones()
        recibirEnlace(intent)
        // Rutaalacima Web: mientras la app está abierta, trae lo que se marcó en el computador cada minuto
        val web = (application as RutaApp).container.web
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    web.sincronizarSiActivo()
                    delay(60_000)
                }
            }
        }
        setContent {
            RutaTheme {
                RutaRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        recibirEnlace(intent)
    }

    /** Código QR de Rutaalacima Web escaneado con la cámara del teléfono: rutacima://vincular?codigo=… */
    private fun recibirEnlace(intent: Intent?) {
        val datos = intent?.data ?: return
        if (datos.scheme != "rutacima" || datos.host != "vincular") return
        Sincronia.codigoDeVinculo(datos.toString())?.let { (application as RutaApp).container.web.codigoPendiente.value = it }
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
