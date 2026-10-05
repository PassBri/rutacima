@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.rutaalacima.app

import android.content.Context
import com.rutaalacima.app.data.AgendaRepository
import com.rutaalacima.app.data.ChecklistRepository
import com.rutaalacima.app.data.EjesRepository
import com.rutaalacima.app.data.PerfilRepository
import com.rutaalacima.app.data.PlanAnualRepository
import com.rutaalacima.app.data.PlanificadorRepository
import com.rutaalacima.app.data.RespuestasRepository
import com.rutaalacima.app.data.bancos.BancosRepository
import com.rutaalacima.app.data.coach.CoachRepository
import com.rutaalacima.app.data.content.ContentRepository
import com.rutaalacima.app.data.local.RutaDatabase
import com.rutaalacima.app.data.remote.SupabaseClient
import com.rutaalacima.app.data.social.SocialRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/** Inyección de dependencias manual (suficiente para el tamaño actual del proyecto). */
class AppContainer(context: Context) {
    /** Contexto de la aplicación (para preferencias de las pantallas). */
    val contexto: Context = context.applicationContext
    val db = RutaDatabase.build(context)

    /** Scope de la aplicación: para guardados que deben terminar aunque se cierre la pantalla. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val supabase = SupabaseClient(context)
    val contenido = ContentRepository(context)
    val bancos = BancosRepository(context)
    val perfil = PerfilRepository(db)
    val respuestas = RespuestasRepository(db)
    val ejes = EjesRepository(db)
    val checklist = ChecklistRepository(db)
    val planificador = PlanificadorRepository(db)
    val agenda = AgendaRepository(db)
    val planAnual = PlanAnualRepository(db)
    val cascada = com.rutaalacima.app.data.CascadaRepository(planificador, planAnual)
    val social = SocialRepository(context, db, supabase)
    val frases = com.rutaalacima.app.data.frases.FrasesRepository(context)
    val coach = CoachRepository(context, db, supabase, bancos)
    /** Vision board armado con IA y llenado con fotos propias. */
    val vision = com.rutaalacima.app.data.vision.VisionRepository(context, db, coach, social)
    /** Rutaalacima Web: vincular un computador y compartir la ruta con la cuenta. */
    val web = com.rutaalacima.app.data.web.RutaWebRepository(context, db, supabase, frases, social)

    /** Mensajes 1 a 1 y coaches de vida. */
    val mensajes = com.rutaalacima.app.data.social.MensajesRepository(supabase)
    /** Cordadas (grupos con reto compartido) y agenda del coach de vida. */
    val cordadas = com.rutaalacima.app.data.social.CordadasRepository(supabase)

    /** Audiolibros: grabaciones propias por capítulo y reproductor (voz del teléfono si no hay grabación). */
    val audios = com.rutaalacima.app.data.audio.AudiosRepository(context, supabase)
    val audiolibro = com.rutaalacima.app.data.audio.ReproductorAudiolibro(context, contenido, audios)

    /** Pantalla a abrir al tocar una notificación (por ejemplo "chat/…"). */
    val navegacionPendiente = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    init {
        web.iniciarAutomatica(appScope)
        // El widget se actualiza cuando marcas un hábito o abres la frase del día
        appScope.launch {
            kotlinx.coroutines.flow.combine(checklist.desde(java.time.LocalDate.now().minusDays(1)), frases.desbloqueadas) { a, b -> a to b }
                .debounce(1_500)
                .collect { com.rutaalacima.app.widget.WidgetRuta.actualizar(context.applicationContext) }
        }
    }
}
