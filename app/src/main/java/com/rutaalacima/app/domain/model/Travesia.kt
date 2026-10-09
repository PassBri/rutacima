package com.rutaalacima.app.domain.model

import java.time.LocalDate

/**
 * Lo que le pasa a la Brújula de la Cima a lo largo del año, tomado de los libros:
 *  - **Niebla** ("Cuando te pierdes en la niebla"): un paso empezado que lleva [NIEBLA_DIAS] días sin
 *    jornadas se cubre de niebla y ofrece los protocolos de reorientación.
 *  - **Caídas** (Guía de Caídas + Sistema Anti-Abandono): si la persona reconoce que se cayó, elige el
 *    tipo de caída; el paso conserva sus jornadas y su exigencia baja a la dificultad actual del ritmo
 *    (que ya baja sola cuando te frenas), para que volver sea fácil.
 *  - **Confluencia** (Bono 5, acciones multi-eje): un paso puede activar otros campamentos; se ve como
 *    una línea en el 9×9 y cuenta como paso de confluencia al ganarse.
 *  - **Cierre del año** (Desde la Cima): las 7 lecciones, la imagen del año al diario de vida y una
 *    nueva montaña: los pasos ganados se liberan para escribir los del año siguiente.
 */
object Travesia {
    const val NIEBLA_DIAS = 14
    /** Pasos de confluencia que se enlazan solos por campamento al sugerir (acciones de 3 ejes o más). */
    const val ENLACES_AUTO = 2
    val CAIDAS = listOf("FIN", "EMO", "DEC", "CAR", "IDE", "CIR")
    const val LECCIONES = 7

    fun claveCaida(casillaId: Long, paso: Int) = Mandala.clave(casillaId, paso) + "#caida"
    fun claveEnlaces(casillaId: Long, paso: Int) = Mandala.clave(casillaId, paso) + "#enlaces"
    fun claveLeccion(anio: Int, n: Int) = "brujula-$anio#leccion$n"
    fun claveCierre(anio: Int) = "brujula-$anio#cierre"

    /** Un paso empezado (con jornadas) que no se mueve hace [NIEBLA_DIAS] días o más. */
    fun enNiebla(p: MetodoCima.Paso, hoy: LocalDate): Boolean =
        !p.cumplido && p.jornadas.isNotEmpty() && p.jornadas.max().plusDays(NIEBLA_DIAS.toLong()) <= hoy

    /** Días que lleva quieto un paso empezado (null si no ha empezado o ya se ganó). */
    fun diasQuieto(p: MetodoCima.Paso, hoy: LocalDate): Int? =
        if (p.cumplido || p.jornadas.isEmpty()) null else (hoy.toEpochDay() - p.jornadas.max().toEpochDay()).toInt()

    /** Tras reconocer una caída, el paso pide como máximo la dificultad actual (nunca más de lo que pedía). */
    fun requeridasTrasCaida(p: MetodoCima.Paso, dificultadActual: Int): Int = minOf(p.requeridas ?: dificultadActual, dificultadActual)

    /** Lee "TIPO|fecha" de una caída registrada. */
    fun caida(valor: String?): Pair<String, LocalDate?>? {
        val v = valor?.takeIf { it.isNotBlank() } ?: return null
        val (tipo, fecha) = v.split('|').let { it[0] to it.getOrNull(1) }
        return if (tipo in CAIDAS) tipo to fecha?.let { runCatching { LocalDate.parse(it) }.getOrNull() } else null
    }

    /** Campamentos (códigos) que también activa un paso, sin repetir el suyo. */
    fun enlaces(valor: String?, propio: String): List<String> =
        valor.orEmpty().split(',').map { it.trim().uppercase() }.filter { it in Mandala.CAMPAMENTOS_FIJOS && it != propio }.distinct()

    /** Enlaces de una acción multi-eje del banco: los ejes que activa además del campamento. */
    fun enlacesDeAccion(ejesAccion: List<String>, propio: String): String =
        ejesAccion.map { it.uppercase() }.filter { it in Mandala.CAMPAMENTOS_FIJOS && it != propio }.distinct().joinToString(",")

    /** Pasos ganados que activan más de un campamento. */
    fun confluencias(ids: List<Long?>, r: Map<String, String>): Int =
        ids.withIndex().sumOf { (i, id) ->
            if (id == null) 0 else (0 until Mandala.PASOS).count { p ->
                !r[Mandala.claveHecho(id, p)].isNullOrBlank() && enlaces(r[claveEnlaces(id, p)], Mandala.CAMPAMENTOS_FIJOS[i]).isNotEmpty()
            }
        }

    /**
     * Nueva montaña: claves de los pasos ganados que se liberan (texto, jornadas, foto, portal…).
     * Los pasos a medio camino se quedan con sus jornadas: siguen en la ruta del año nuevo.
     */
    fun clavesALiberar(ids: List<Long?>, r: Map<String, String>): List<String> =
        ids.filterNotNull().flatMap { id ->
            (0 until Mandala.PASOS).filter { p -> !r[Mandala.claveHecho(id, p)].isNullOrBlank() }.flatMap { p ->
                listOf(
                    Mandala.clave(id, p), Mandala.claveHecho(id, p), MetodoCima.claveJornadas(id, p), MetodoCima.claveRequeridas(id, p),
                    Mandala.claveFoto(id, p), Mandala.claveSoltar(id, p), Mandala.claveLlevar(id, p), claveCaida(id, p), claveEnlaces(id, p),
                ).filter { r.containsKey(it) }
            }
        }

    /** Resumen del año que se guarda al cerrar: "ganados|escritos|mm|evidencias|confluencias". */
    fun resumen(ganados: Int, escritos: Int, mm: Long, evidencias: Int, confluencias: Int) = "$ganados|$escritos|$mm|$evidencias|$confluencias"
}
