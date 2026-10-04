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

/** Inyección de dependencias manual (suficiente para el tamaño actual del proyecto). */
class AppContainer(context: Context) {
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
    /** RutaCima Web: vincular un computador y compartir la ruta con la cuenta. */
    val web = com.rutaalacima.app.data.web.RutaWebRepository(context, db, supabase, frases, social)

    init {
        web.iniciarAutomatica(appScope)
    }
}
