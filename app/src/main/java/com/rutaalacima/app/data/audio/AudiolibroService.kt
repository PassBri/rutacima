package com.rutaalacima.app.data.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.rutaalacima.app.MainActivity
import com.rutaalacima.app.R
import com.rutaalacima.app.RutaApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Mantiene sonando el audiolibro con la pantalla apagada o la app en segundo plano, con una
 * notificación para pausar, seguir o cerrar. Se va sola cuando el audiolibro se cierra.
 */
class AudiolibroService : Service() {
    private val scope = MainScope()
    private val reproductor get() = (application as RutaApp).container.audiolibro
    private var enPrimerPlano = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        corriendo = true
        crearCanal(this)
        mostrar(reproductor.estado.value)
        scope.launch {
            reproductor.estado.collect { e ->
                if (!e.activo) { detenerServicio(); return@collect }
                mostrar(e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACCION_ALTERNAR -> reproductor.alternar()
            ACCION_SIGUIENTE -> reproductor.capitulo(+1)
            ACCION_CERRAR -> reproductor.detener()
        }
        if (!reproductor.estado.value.activo) detenerServicio()
        else { enPrimerPlano = false; mostrar(reproductor.estado.value) }   // cada inicio pide estar en primer plano
        return START_NOT_STICKY
    }

    /** Si cierras la app desde recientes, el audiolibro también se cierra. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        reproductor.detener()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        corriendo = false
        scope.cancel()
        super.onDestroy()
    }

    private fun detenerServicio() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        enPrimerPlano = false
        stopSelf()
    }

    private fun accion(accion: String, codigo: Int) = PendingIntent.getService(
        this, codigo, Intent(this, AudiolibroService::class.java).setAction(accion),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun mostrar(e: EstadoAudiolibro) {
        val abrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(e.tituloSeccion.ifBlank { getString(R.string.audio_escuchar) })
            .setContentText(
                listOf(e.guia, getString(if (e.fuente == FuenteAudio.AUTOR) R.string.audio_voz_autor else R.string.audio_voz_app))
                    .filter { it.isNotBlank() }.joinToString(" · ")
            )
            .setContentIntent(abrir)
            .setOngoing(e.reproduciendo)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(100, (e.avance * 100).toInt(), e.cargando)
            .addAction(
                if (e.reproduciendo) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                getString(if (e.reproduciendo) R.string.audio_pausar else R.string.audio_reanudar),
                accion(ACCION_ALTERNAR, 1),
            )
            .addAction(android.R.drawable.ic_media_next, getString(R.string.audio_siguiente_capitulo), accion(ACCION_SIGUIENTE, 2))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.audio_cerrar), accion(ACCION_CERRAR, 3))
            .build()
        if (!enPrimerPlano) {
            val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            runCatching { ServiceCompat.startForeground(this, ID, n, tipo); enPrimerPlano = true }
        } else if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            runCatching { NotificationManagerCompat.from(this).notify(ID, n) }
        }
    }

    companion object {
        const val CANAL = "audiolibro"
        private const val ID = 4021
        private const val ACCION_ALTERNAR = "com.rutaalacima.app.audio.ALTERNAR"
        private const val ACCION_SIGUIENTE = "com.rutaalacima.app.audio.SIGUIENTE"
        private const val ACCION_CERRAR = "com.rutaalacima.app.audio.CERRAR"

        fun crearCanal(c: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canal = NotificationChannel(CANAL, c.getString(R.string.audio_canal), NotificationManager.IMPORTANCE_LOW)
                c.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
            }
        }

        @Volatile private var corriendo = false

        /** Se llama al empezar a sonar desde la app abierta; si ya está corriendo, no hace nada. */
        fun iniciar(c: Context) {
            if (corriendo) return
            runCatching { ContextCompat.startForegroundService(c, Intent(c, AudiolibroService::class.java)) }
        }
    }
}
