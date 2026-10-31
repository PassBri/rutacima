package com.rutaalacima.app.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rutaalacima.app.MainActivity
import com.rutaalacima.app.R
import com.rutaalacima.app.RutaApp
import com.rutaalacima.app.ui.tramites.Plazos
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Avisos de Trámites y plazos: una notificación propia (canal de alarma) cuando un trámite vence hoy o en los días de
 * aviso que eligió la persona. Se revisa cada mañana a las 8, al abrir la app y al guardar un trámite; cada trámite
 * avisa una sola vez por día. Independiente del recordatorio diario, para que un plazo nunca pase en silencio.
 */
object AvisosTramites {
    private const val CANAL = "tramites_alarma"
    private const val TRABAJO = "tramites_diario"
    private const val YA = "tramites_avisados"

    fun crearCanal(c: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, c.getString(R.string.tr_titulo), NotificationManager.IMPORTANCE_HIGH)
            c.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    /** Cada mañana a las 8 (se mantiene si ya estaba programado). */
    fun programar(c: Context) {
        val ahora = LocalDateTime.now()
        var proxima = ahora.toLocalDate().atTime(8, 0)
        if (!proxima.isAfter(ahora)) proxima = proxima.plusDays(1)
        val pedido = PeriodicWorkRequestBuilder<TramitesWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(ahora, proxima).toMinutes(), TimeUnit.MINUTES).build()
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, pedido)
    }

    /** Revisar ya (al abrir la app o al guardar un trámite). */
    fun revisar(c: Context) {
        WorkManager.getInstance(c).enqueueUniqueWork("tramites_ahora", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<TramitesWorker>().build())
    }

    suspend fun avisar(c: Context) {
        if (!Recordatorios.puedeNotificar(c)) return
        crearCanal(c)
        val r = (c.applicationContext as RutaApp).container.respuestas.cargar(Plazos.WB)
        val hoy = LocalDate.now()
        val prefs = c.getSharedPreferences(YA, Context.MODE_PRIVATE)
        val abrir = PendingIntent.getActivity(c, 2, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        Plazos.leer(r).filter { !it.listo && Plazos.avisarHoy(it, hoy) }.forEach { t ->
            val clave = "${t.id}_$hoy"
            if (prefs.getBoolean(clave, false)) return@forEach
            val p = Plazos.proximo(t, hoy) ?: return@forEach
            val q = ChronoUnit.DAYS.between(hoy, p).toInt()
            val texto = when { t.mensual && q == 0 -> c.getString(R.string.tr_notif_pago, t.n); q == 0 -> c.getString(R.string.tr_notif_hoy, t.n); else -> c.getString(R.string.tr_notif_en, t.n, q) }
            val n = NotificationCompat.Builder(c, CANAL)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(c.getString(R.string.tr_titulo))
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto + if (t.nota.isNotBlank()) "\n" + t.nota else ""))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(abrir)
                .setAutoCancel(true)
                .build()
            try { NotificationManagerCompat.from(c).notify(("tramite" + t.id).hashCode(), n); prefs.edit().putBoolean(clave, true).apply() } catch (e: SecurityException) { }
        }
    }
}

class TramitesWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        AvisosTramites.avisar(applicationContext)
        return Result.success()
    }
}
