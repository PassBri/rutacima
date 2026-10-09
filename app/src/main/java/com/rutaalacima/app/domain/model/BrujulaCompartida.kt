package com.rutaalacima.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * La Brújula de la Cima tal como estaba el día que se compartió: viaja con la publicación (columna
 * "brujula" en Supabase) para que la comunidad la explore con el mismo zoom 1 · 9 · 81, en solo
 * lectura. Cada publicación es una foto del momento: compartir otra vez meses después muestra el avance.
 *
 * La persona elige qué sale: los 64 pasos son opcionales y las fotos de evidencia también (por
 * defecto no, porque en la app son privadas).
 */
object BrujulaCompartida {
    const val VERSION = 1

    @Serializable
    data class Paso(val t: String = "", val e: String = "V", val f: String = "")

    @Serializable
    data class Campamento(val c: String, val t: String = "", val f: String = "", val p: List<Paso> = emptyList())

    @Serializable
    data class Instantanea(
        val v: Int = VERSION,
        val cumbre: String = "",
        val fotoCumbre: String = "",
        val camps: List<Campamento> = emptyList(),
        val mm: Long = 0,
        val ganados: Int = 0,
        val escritos: Int = 0,
        val evidencias: Int = 0,
        val fecha: String = "",
        val conPasos: Boolean = true,
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun codificar(i: Instantanea): String = json.encodeToString(Instantanea.serializer(), i)

    fun leer(texto: String?): Instantanea? =
        texto?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(Instantanea.serializer(), it) }.getOrNull() }
            ?.takeIf { it.v <= VERSION && it.camps.size <= Mandala.CAMPAMENTOS }

    /** Estado de un paso como letra: V vacío, E escrito, H ganado. */
    fun letra(e: Mandala.EstadoPaso) = when (e) { Mandala.EstadoPaso.VACIO -> "V"; Mandala.EstadoPaso.ESCRITO -> "E"; Mandala.EstadoPaso.HECHO -> "H" }

    /**
     * Respuestas equivalentes a la instantánea (las mismas claves que usa la Brújula de la persona),
     * para dibujarla con las mismas piezas. El id de cada campamento es su posición + 1; las fotos de
     * evidencia van como "ev-<campamento>-<paso>" en el mapa de fotos.
     */
    fun comoRespuestas(i: Instantanea): Pair<Map<String, String>, Map<String, String>> {
        val r = mutableMapOf<String, String>()
        val fotos = mutableMapOf<String, String>()
        i.camps.forEachIndexed { k, c ->
            val id = (Mandala.CAMPAMENTOS_FIJOS.indexOf(c.c).takeIf { it >= 0 } ?: k) + 1L
            c.p.take(Mandala.PASOS).forEachIndexed { n, p ->
                if (p.t.isNotBlank() || p.e != "V") r[Mandala.clave(id, n)] = p.t.ifBlank { "·" }
                if (p.e == "H") r[Mandala.claveHecho(id, n)] = "1"
                if (p.f.isNotBlank()) { val clave = "ev-$id-$n"; r[Mandala.claveFoto(id, n)] = clave; fotos[clave] = p.f }
            }
        }
        return r to fotos
    }
}
