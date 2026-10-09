package com.rutaalacima.app.domain.model

import kotlin.math.abs
import kotlin.math.max

/**
 * Mandala 9×9 de Rutaalacima, adaptado del Mandala Chart (Mandal-Art) de Hiroaki Matsumura, el
 * método que Takashi Harada enseñaba y con el que Shohei Ohtani planeó su carrera a los 16 años:
 * una meta en el centro, 8 metas de apoyo alrededor y 8 acciones para cada una (64 acciones).
 *
 * En Rutaalacima se mezcla con el vision board:
 *  - Centro: la cumbre (la casilla "Mi cumbre" del vision board o la frase de la cumbre).
 *  - 8 campamentos: las otras casillas del vision board, con su foto. Cada campamento se repite en
 *    el bloque central y en el centro de su propio bloque, igual que en el método original.
 *  - 64 pasos: 8 por campamento, cada uno se escribe y se marca cuando se cumple.
 *
 * En 3D la cuadrícula se vuelve montaña: cuanto más cerca del centro, más alto; los pasos crecen
 * cuando se escriben y cuando se cumplen, así la montaña sube con el avance.
 *
 * Los pasos se guardan como respuestas ("mandala#<casilla>#<n>" y "...#hecho"), así viajan a la
 * web y a las copias de seguridad sin tablas nuevas.
 */
object Mandala {
    const val WORKBOOK = "mandala"
    const val LADO = 9
    const val CAMPAMENTOS = 8
    const val PASOS = 8

    /** Posiciones (0..8, fila por fila) del anillo alrededor del centro de un bloque de 3×3. */
    val ANILLO = listOf(0, 1, 2, 3, 5, 6, 7, 8)

    fun clave(casillaId: Long, paso: Int) = "mandala#$casillaId#$paso"
    fun claveHecho(casillaId: Long, paso: Int) = clave(casillaId, paso) + "#hecho"

    enum class Tipo { CUMBRE, CAMPAMENTO, PASO }

    /**
     * Qué hay en la celda [fila],[col] de la cuadrícula 9×9.
     * [campamento]: 0..7 (−1 en la cumbre); [paso]: 0..7 solo en las celdas de tipo PASO.
     * [copia]: el campamento repetido en el bloque central (el original está en el centro de su bloque).
     */
    data class Celda(val fila: Int, val col: Int, val tipo: Tipo, val campamento: Int, val paso: Int = -1, val copia: Boolean = false)

    fun celda(fila: Int, col: Int): Celda {
        require(fila in 0 until LADO && col in 0 until LADO)
        val bloque = (fila / 3) * 3 + col / 3
        val dentro = (fila % 3) * 3 + col % 3
        return when {
            bloque == 4 && dentro == 4 -> Celda(fila, col, Tipo.CUMBRE, -1)
            bloque == 4 -> Celda(fila, col, Tipo.CAMPAMENTO, ANILLO.indexOf(dentro), copia = true)
            dentro == 4 -> Celda(fila, col, Tipo.CAMPAMENTO, ANILLO.indexOf(bloque))
            else -> Celda(fila, col, Tipo.PASO, ANILLO.indexOf(bloque), ANILLO.indexOf(dentro))
        }
    }

    val CELDAS: List<Celda> = (0 until LADO).flatMap { f -> (0 until LADO).map { c -> celda(f, c) } }

    /** Bloque (0..8) donde vive el campamento [i] con sus pasos. */
    fun bloqueDe(campamento: Int) = ANILLO[campamento]

    /** Distancia al centro en anillos (0 en la cumbre, 4 en el borde). */
    fun anillo(fila: Int, col: Int) = max(abs(fila - 4), abs(col - 4))

    /** Estado de un paso para dibujar la montaña. */
    enum class EstadoPaso { VACIO, ESCRITO, HECHO }

    /**
     * Altura de la celda en la montaña 3D (en "pisos"). La base escalonada va de 1 (borde) a 5
     * (cumbre); los campamentos sobresalen un poco y los pasos crecen al escribirse y al cumplirse.
     */
    fun altura(c: Celda, estado: EstadoPaso = EstadoPaso.VACIO): Float {
        val base = (5 - anillo(c.fila, c.col)).toFloat()
        return when (c.tipo) {
            Tipo.CUMBRE -> base + 1.2f
            Tipo.CAMPAMENTO -> base + 0.6f
            Tipo.PASO -> base * when (estado) { EstadoPaso.VACIO -> 0.35f; EstadoPaso.ESCRITO -> 0.7f; EstadoPaso.HECHO -> 1f }
        }
    }

    data class Progreso(val escritos: Int, val hechos: Int) {
        val total get() = CAMPAMENTOS * PASOS
        val fraccion: Float get() = hechos.toFloat() / total
    }

    /** Pasos escritos y cumplidos de los campamentos [casillas] (ids del vision board, hasta 8). */
    fun progreso(casillas: List<Long>, respuestas: Map<String, String>): Progreso {
        var escritos = 0; var hechos = 0
        casillas.take(CAMPAMENTOS).forEach { id ->
            repeat(PASOS) { p ->
                if (!respuestas[clave(id, p)].isNullOrBlank()) {
                    escritos++
                    if (respuestas[claveHecho(id, p)] == "1") hechos++
                }
            }
        }
        return Progreso(escritos, hechos)
    }

    fun estado(casillaId: Long?, paso: Int, respuestas: Map<String, String>): EstadoPaso = when {
        casillaId == null || respuestas[clave(casillaId, paso)].isNullOrBlank() -> EstadoPaso.VACIO
        respuestas[claveHecho(casillaId, paso)] == "1" -> EstadoPaso.HECHO
        else -> EstadoPaso.ESCRITO
    }

    /**
     * Ordena las casillas del vision board para la mandala: la de origen "cumbre" va al centro y las
     * demás (por su orden) son los campamentos. Devuelve (cumbre o null, campamentos hasta 8).
     */
    fun <T> repartir(casillas: List<T>, origen: (T) -> String): Pair<T?, List<T>> {
        val cumbre = casillas.firstOrNull { origen(it) == "cumbre" }
        return cumbre to casillas.filter { it !== cumbre }.take(CAMPAMENTOS)
    }

    /** Llena solo los pasos vacíos con las sugerencias, sin repetir lo que ya está escrito. */
    fun completar(actuales: List<String>, sugerencias: List<String>): List<String> {
        val usados = actuales.filter { it.isNotBlank() }.map { it.trim().lowercase() }.toMutableSet()
        val cola = sugerencias.map { it.trim() }.filter { it.isNotBlank() && usados.add(it.lowercase()) }.iterator()
        return List(PASOS) { i -> actuales.getOrNull(i)?.takeIf { it.isNotBlank() } ?: if (cola.hasNext()) cola.next() else "" }
    }
}
