package com.rutaalacima.app.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rutaalacima.app.MainActivity
import com.rutaalacima.app.R
import com.rutaalacima.app.RutaApp
import com.rutaalacima.app.domain.model.Avisos
import java.util.concurrent.TimeUnit

/**
 * Avisos de mensajes nuevos, solicitudes para escribirte y personas que te piden ser su coach.
 * Revisa en segundo plano cada 15 minutos (lo mínimo que permite Android) sin servicios extra.
 */
object AvisosMensajes {
    private const val CANAL = "mensajes"
    private const val TRABAJO = "avisos_mensajes"
    private const val PREFS = "avisos_mensajes"
    const val EXTRA_IR_A = "ir_a"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun activos(c: Context) = prefs(c).getBoolean("activos", true)

    fun configurar(c: Context, activos: Boolean) {
        prefs(c).edit().putBoolean("activos", activos).apply()
        if (activos) programar(c) else WorkManager.getInstance(c).cancelUniqueWork(TRABAJO)
    }

    fun programar(c: Context) {
        if (!activos(c)) return
        val pedido = PeriodicWorkRequestBuilder<AvisosWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, pedido)
    }

    /** Revisa ya (por ejemplo, al cerrar la app). */
    fun revisarAhora(c: Context) {
        if (!activos(c)) return
        WorkManager.getInstance(c).enqueueUniqueWork(
            "$TRABAJO-ahora", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<AvisosWorker>().setInitialDelay(1, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build(),
        )
    }

    private fun crearCanal(c: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, c.getString(R.string.mensajes), NotificationManager.IMPORTANCE_HIGH)
            c.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    suspend fun revisar(c: Context) {
        val cont = (c.applicationContext as RutaApp).container
        val repo = cont.mensajes
        if (!repo.disponible || !activos(c)) return
        val p = prefs(c)
        val avisados = p.getString("avisados", "").orEmpty().lines().mapNotNull { l ->
            l.split('\t').takeIf { it.size == 2 }?.let { it[0] to (it[1].toLongOrNull() ?: 0L) }
        }.toMap()
        val coachAvisados = p.getStringSet("coach_avisados", emptySet()).orEmpty()

        val convs = repo.conversaciones().map {
            Avisos.Conv(it.id, it.otroNombre, it.ultimoTexto, it.ultimoEn, it.noLeidos, it.solicitudParaMi, it.ultimoEsMio)
        }
        val pendientes = runCatching { repo.acompanamientos() }.getOrDefault(emptyList())
            .filter { it.rol == "coach" && it.estado == "solicitado" }.map { it.id to it.otroNombre }
        runCatching { avisarComunidad(c) }
        val nuevos = Avisos.nuevos(convs, avisados, pendientes, coachAvisados, repo.conversacionAbierta)

        // Lo primero que se guarda: aunque el sistema no deje notificar, no se repiten
        p.edit()
            .putString("avisados", Avisos.marcar(convs, avisados).entries.joinToString("\n") { "${it.key}\t${it.value}" })
            .putStringSet("coach_avisados", coachAvisados + pendientes.map { it.first })
            .apply()
        if (nuevos.isEmpty() || !Recordatorios.puedeNotificar(c)) return
        crearCanal(c)
        val nm = NotificationManagerCompat.from(c)
        nuevos.forEach { a ->
            val (titulo, texto, irA) = when (a) {
                is Avisos.Mensajes -> Triple(
                    if (a.cuantos > 1) c.getString(R.string.aviso_mensajes_n, a.nombre, a.cuantos) else a.nombre, a.texto, "chat/${a.conv}")
                is Avisos.Solicitud -> Triple(c.getString(R.string.aviso_solicitud, a.nombre), a.texto, "chat/${a.conv}")
                is Avisos.PideCoach -> Triple(c.getString(R.string.aviso_pide_coach, a.nombre), c.getString(R.string.aviso_pide_coach_texto), "coach_vida")
            }
            val abrir = PendingIntent.getActivity(
                c, a.clave.hashCode(),
                Intent(c, MainActivity::class.java).putExtra(EXTRA_IR_A, irA).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(c, CANAL)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(CANAL)
                .setAutoCancel(true)
                .setContentIntent(abrir)
                .build()
            runCatching { nm.notify(a.clave.hashCode(), n) }
        }
    }
}

/** Avisos de la comunidad (impulsos, comentarios, respuestas, seguidores, menciones): uno solo que los resume. */
private suspend fun avisarComunidad(c: Context) {
    val red = (c.applicationContext as RutaApp).container.red
    if (!red.disponible) return
    val p = c.getSharedPreferences("avisos_mensajes", Context.MODE_PRIVATE)
    val ultimo = p.getLong("ultimo_aviso_social", 0L)
    val nuevos = red.avisos().filter { !it.leida && it.id > ultimo }
    if (nuevos.isEmpty()) return
    p.edit().putLong("ultimo_aviso_social", nuevos.maxOf { it.id }).apply()
    if (!Recordatorios.puedeNotificar(c)) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        c.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("comunidad", c.getString(R.string.red_canal), NotificationManager.IMPORTANCE_DEFAULT))
    }
    val a = nuevos.first()
    val accion = c.getString(when (a.tipo) {
        "IMPULSO" -> R.string.red_a_impulso; "COMENTARIO" -> R.string.red_a_comentario; "RESPUESTA" -> R.string.red_a_respuesta
        "SEGUIDOR" -> R.string.red_a_seguidor; "LIKE_COMENTARIO" -> R.string.red_a_like_com; else -> R.string.red_a_mencion
    })
    val titulo = if (nuevos.size > 1) c.getString(R.string.red_n_nuevos, nuevos.size) else a.actorNombre
    val texto = if (nuevos.size > 1) "${a.actorNombre}: $accion" else if (a.texto.isNotBlank()) "$accion: ${a.texto}" else accion
    val abrir = PendingIntent.getActivity(
        c, "avisos".hashCode(),
        Intent(c, MainActivity::class.java).putExtra(AvisosMensajes.EXTRA_IR_A, "avisos").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val n = NotificationCompat.Builder(c, "comunidad")
        .setSmallIcon(R.drawable.ic_notificacion).setContentTitle(titulo).setContentText(texto)
        .setStyle(NotificationCompat.BigTextStyle().bigText(texto)).setCategory(NotificationCompat.CATEGORY_SOCIAL)
        .setAutoCancel(true).setContentIntent(abrir).build()
    runCatching { NotificationManagerCompat.from(c).notify("comunidad".hashCode(), n) }
}

class AvisosWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        runCatching { AvisosMensajes.revisar(applicationContext) }
        return Result.success()
    }
}
