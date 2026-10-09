package com.rutaalacima.app.data.social

import com.rutaalacima.app.data.remote.SupabaseClient
import com.rutaalacima.app.domain.model.Planes
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** Plan de la cuenta (mi_plan) y "Avísame cuando esté disponible" (avisame_plan). Sin cuenta: Gratis. */
class PlanRepository(private val supa: SupabaseClient) {
    val hayCuenta get() = supa.configurado && supa.sesion.value != null

    suspend fun miPlan(): Planes.Estado {
        if (!hayCuenta) return Planes.GRATIS
        val o = runCatching { supa.rpc("mi_plan") }.getOrNull() as? JsonObject ?: return Planes.GRATIS
        fun txt(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull
        return Planes.leer(txt("plan"), txt("hasta"), txt("origen"), (o["avisame"] as? JsonPrimitive)?.booleanOrNull == true)
    }

    suspend fun avisame() { supa.rpc("avisame_plan") }
}
