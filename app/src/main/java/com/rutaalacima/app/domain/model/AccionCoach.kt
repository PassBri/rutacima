package com.rutaalacima.app.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Una meta que el coach de IA propone y la persona puede crear con un toque. El coach la escribe al
 * final de su respuesta como `[[ACCION {"tipo":"meta_mes","texto":"…","eje":"VOL","dias":20}]]`;
 * la app la quita del texto y muestra un botón. (Sin Android: se prueba con JUnit; la web hace lo mismo.)
 */
data class AccionCoach(val tipo: Tipo, val texto: String, val eje: String?, val dias: Int) {
    enum class Tipo { META_MES, META_ANIO }

    companion object {
        private val MARCA = Regex("""\[\[ACCION(.*?)]]""", RegexOption.DOT_MATCHES_ALL)
        private val EJES = setOf("VOL", "MAE", "VOZ", "VAL", "EVO", "TRA")

        /** Separa el texto que se muestra de la acción propuesta (si hay una válida). */
        fun separar(respuesta: String): Pair<String, AccionCoach?> {
            val m = MARCA.find(respuesta) ?: return respuesta.trim() to null
            val limpio = respuesta.replace(MARCA, "").trim()
            val crudo = m.groupValues[1]
            val o = runCatching { Json.parseToJsonElement(crudo.substring(crudo.indexOf('{'), crudo.lastIndexOf('}') + 1)) as JsonObject }
                .getOrNull() ?: return limpio to null
            fun s(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            val tipo = when (s("tipo")) { "meta_mes" -> Tipo.META_MES; "meta_anio" -> Tipo.META_ANIO; else -> return limpio to null }
            val texto = s("texto").ifBlank { s("titulo") }.take(300)
            if (texto.isBlank()) return limpio to null
            val eje = s("eje").uppercase().takeIf { it in EJES }
            val dias = ((o["dias"] as? JsonPrimitive)?.intOrNull ?: 20).coerceIn(1, 31)
            return limpio to AccionCoach(tipo, texto, eje, dias)
        }
    }
}
