package com.rutaalacima.app.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rutaalacima.app.MainActivity
import com.rutaalacima.app.R
import com.rutaalacima.app.RutaApp
import com.rutaalacima.app.domain.model.RecordatorioVida
import com.rutaalacima.app.domain.model.Vida
import com.rutaalacima.app.ui.ruta.fraseRecordatorio
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Recordatorio del día del camino hacia los 120 años: una notificación diaria a la hora que
 * elija la persona con el día de vida que comienza, los días que quedan y una frase.
 */
object Recordatorios {
    private const val CANAL = "recordatorio_diario"
    private const val TRABAJO = "recordatorio_120"
    private const val PREFS = "ajustes_recordatorio"
    val HORAS = listOf(5, 6, 7, 8, 9, 12, 20, 21)

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun activo(c: Context): Boolean = prefs(c).getBoolean("activo", true)
    fun hora(c: Context): Int = prefs(c).getInt("hora", 7)

    fun configurar(c: Context, activo: Boolean, hora: Int) {
        prefs(c).edit().putBoolean("activo", activo).putInt("hora", hora).apply()
        if (activo) programar(c, reemplazar = true) else WorkManager.getInstance(c).cancelUniqueWork(TRABAJO)
    }

    /** Programa (o mantiene) el trabajo diario. Se llama al abrir la app y al cambiar los ajustes. */
    fun programar(c: Context, reemplazar: Boolean = false) {
        if (!activo(c)) return
        val ahora = LocalDateTime.now()
        var proxima = ahora.toLocalDate().atTime(hora(c), 0)
        if (!proxima.isAfter(ahora)) proxima = proxima.plusDays(1)
        val retraso = Duration.between(ahora, proxima).toMinutes()
        val pedido = PeriodicWorkRequestBuilder<RecordatorioWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(retraso, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(
            TRABAJO,
            if (reemplazar) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            pedido,
        )
    }

    fun crearCanal(c: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, c.getString(R.string.recordatorio_canal), NotificationManager.IMPORTANCE_DEFAULT)
            c.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    fun puedeNotificar(c: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Construye y muestra la notificación del día. */
    suspend fun mostrar(c: Context) {
        if (!puedeNotificar(c)) return
        crearCanal(c)
        val perfil = (c.applicationContext as RutaApp).container.perfil.perfil.first()
        val nf = NumberFormat.getIntegerInstance()
        val hoy = LocalDate.now()
        val (titulo, texto) = perfil.anioNacimiento?.let { anio ->
            val meta = (perfil.esperanzaVida ?: Vida.META_DEFECTO).coerceIn(1, Vida.META_MAXIMA)
            val r = RecordatorioVida.calcular(anio, perfil.mesNacimiento ?: 1, meta, hoy)
            c.getString(R.string.recordatorio_dia_n, nf.format(r.diaDeVida)) to
                c.getString(R.string.recordatorio_frase_sellada) + "\n" + c.getString(R.string.recordatorio_quedan, nf.format(r.diasRestantes), r.meta)
        } ?: (c.getString(R.string.recordatorio_titulo) to c.getString(fraseRecordatorio((hoy.toEpochDay() % RecordatorioVida.TOTAL_FRASES).toInt())))
        val abrir = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(c, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto.substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(c).notify(120, n) } catch (e: SecurityException) { }
    }
}

class RecordatorioWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Recordatorios.mostrar(applicationContext)
        return Result.success()
    }
}
