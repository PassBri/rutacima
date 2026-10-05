package com.rutaalacima.app.domain.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.time.LocalDate

/**
 * Lo que ve un coach de vida de la persona que acompaña (con su permiso): cumbre, propósitos,
 * metas del año y del mes con su avance, hábitos de los últimos 7 días, ejes y vision board.
 * Se arma a partir de los mismos documentos que se sincronizan con la web ("tipo/clave" → JSON).
 */
data class ResumenCoach(
    val nombre: String,
    val cumbre: String,
    val propositos: List<Item>,
    val metasAnio: List<Item>,
    val metasMes: List<Item>,
    /** Hábitos marcados (de 18) en cada uno de los últimos 7 días, del más antiguo a hoy. */
    val habitos7: List<Int>,
    /** Puntaje 1-10 de cada eje (VOL, MAE, VOZ, VAL, EVO, TRA) o null si no ha evaluado. */
    val ejes: List<Int>?,
    val visionConFoto: Int,
    val visionTotal: Int,
) {
    data class Item(val titulo: String, val avance: Int)

    val promedioHabitos: Float get() = if (habitos7.isEmpty()) 0f else habitos7.average().toFloat()

    companion object {
        private fun JsonObject.s(k: String) = (this[k] as? JsonPrimitive)?.contentOrNull.orEmpty()
        private fun JsonObject.i(k: String) = (this[k] as? JsonPrimitive)?.intOrNull
        private fun JsonObject.l(k: String) = (this[k] as? JsonPrimitive)?.longOrNull
        private fun JsonObject.b(k: String) = (this[k] as? JsonPrimitive)?.booleanOrNull ?: false
        private fun lista(v: String) = v.split(',').map { it.trim() }.filter { it.isNotEmpty() }

        fun desde(docs: Map<String, JsonObject>, hoy: LocalDate): ResumenCoach {
            fun de(tipo: String) = docs.filterKeys { it.substringBefore('/') == tipo }.values
            val perfil = de("perfil").firstOrNull()

            // Avance de cada meta del mes: cumplida = 100 %; si no, días marcados / días objetivo
            val mensuales = de("meta_mes").toList()
            fun avanceMes(m: JsonObject): Float =
                if (m.b("cumplida")) 1f else (lista(m.s("dias")).size.toFloat() / (m.i("objetivoDias") ?: 20).coerceAtLeast(1)).coerceIn(0f, 1f)

            // Cascada: la meta del año toma el promedio de sus metas del mes; el propósito, el de sus metas del año
            val anuales = de("meta_anio").toList()
            fun avanceAnio(a: JsonObject): Float {
                val hijos = mensuales.filter { it.l("metaAnualId") != null && it.l("metaAnualId") == a.l("id") }
                return if (hijos.isNotEmpty()) hijos.map(::avanceMes).average().toFloat() else (a.i("avance") ?: 0) / 100f
            }
            val propositos = de("proposito").map { p ->
                val hijos = anuales.filter { it.l("propositoId") != null && it.l("propositoId") == p.l("id") }
                val av = if (hijos.isNotEmpty()) hijos.map(::avanceAnio).average().toFloat() else (p.i("progreso") ?: 0) / 100f
                Item(p.s("titulo"), (av * 100).toInt())
            }

            val habitos = (6 downTo 0).map { k ->
                val fecha = hoy.minusDays(k.toLong()).toString()
                docs["checklist/$fecha"]?.let { lista(it.s("marcados")).size } ?: 0
            }
            val ultima = de("ejes").maxByOrNull { it.l("fecha") ?: 0L }
            val ejes = ultima?.let { e ->
                listOf("voluntad", "maestria", "voz", "valor", "evolucion", "trascendencia").map { e.i(it) ?: 0 }
            }
            val vision = de("vision").toList()

            return ResumenCoach(
                nombre = perfil?.s("nombre").orEmpty(),
                cumbre = perfil?.s("cumbreFrase").orEmpty(),
                propositos = propositos,
                metasAnio = anuales.filter { it.i("anio") == hoy.year }.map { Item(it.s("titulo"), (avanceAnio(it) * 100).toInt()) },
                metasMes = mensuales.filter { it.i("anio") == hoy.year && it.i("mes") == hoy.monthValue }
                    .map { Item(it.s("texto"), (avanceMes(it) * 100).toInt()) },
                habitos7 = habitos,
                ejes = ejes,
                visionConFoto = vision.count { it.s("publicacionId").isNotEmpty() },
                visionTotal = vision.size,
            )
        }
    }
}
