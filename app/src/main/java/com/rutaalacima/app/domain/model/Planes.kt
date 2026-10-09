package com.rutaalacima.app.domain.model

import java.time.Instant

/**
 * Plan Cumbre. El método completo sigue gratis; el plan suma lo que tiene costo real (sobre todo el
 * coach con IA) o es un extra. Lo esencial (tus datos, la seguridad, la comunidad, la web para quien no
 * tiene Android) nunca se cobra. Hoy los cobros no están activos: el plan solo se da como regalo, a
 * instituciones o a pilotos, y el límite real del coach lo decide el servidor.
 */
object Planes {
    enum class Plan { GRATIS, CUMBRE }
    enum class Origen { REGALO, INSTITUCION, PILOTO, PLAY, WEB }

    /** Qué incluye cada plan, en el orden en que se muestra. [esencial]: nunca se cobra. */
    enum class Beneficio(val enGratis: Boolean, val esencial: Boolean = false) {
        METODO(true, esencial = true),
        BRUJULA(true, esencial = true),
        COMUNIDAD(true, esencial = true),
        WEB(true, esencial = true),
        DATOS(true, esencial = true),
        COACH(true),          // gratis con pocos mensajes al día; Cumbre con muchos más
        AUDIOLIBROS(false),
        CIERRE_GUIADO(false),
    }

    data class Estado(val plan: Plan, val hasta: Instant?, val origen: Origen?, val avisame: Boolean) {
        val esCumbre get() = plan == Plan.CUMBRE
    }

    val GRATIS = Estado(Plan.GRATIS, null, null, false)

    /** Lee la respuesta de mi_plan(): {"plan","hasta","origen","avisame"}. Si algo no cuadra, es Gratis. */
    fun leer(plan: String?, hasta: String?, origen: String?, avisame: Boolean): Estado {
        val h = hasta?.let { runCatching { java.time.OffsetDateTime.parse(it).toInstant() }.getOrNull() }
        val p = if (plan == "cumbre" && h != null) Plan.CUMBRE else Plan.GRATIS
        val o = origen?.let { runCatching { Origen.valueOf(it.uppercase()) }.getOrNull() }
        return Estado(p, if (p == Plan.CUMBRE) h else null, if (p == Plan.CUMBRE) o else null, avisame)
    }

    /** Lo esencial nunca queda fuera del plan gratuito. */
    fun incluido(b: Beneficio, plan: Plan) = plan == Plan.CUMBRE || b.enGratis || b.esencial
}
