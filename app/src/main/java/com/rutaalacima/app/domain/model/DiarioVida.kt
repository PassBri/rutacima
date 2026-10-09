package com.rutaalacima.app.domain.model

/**
 * Diario de vida: el álbum de cada año (fotos, recuerdos y metas cumplidas) y quién puede verlo.
 * La visibilidad de cada año se guarda como una respuesta más ("diario-2004#visibilidad"), así viaja
 * con la sincronización a la web y con las copias de seguridad sin tablas nuevas en el teléfono.
 */
object DiarioVida {
    const val WORKBOOK = "diario"
    val VISIBILIDADES = listOf("PRIVADA", "SEGUIDORES", "PUBLICA")

    fun clave(anio: Int) = "diario-$anio#visibilidad"

    /** Años con su visibilidad elegida, a partir de las respuestas del workbook "diario". */
    fun visibilidades(respuestas: Map<String, String>): Map<Int, String> =
        respuestas.mapNotNull { (k, v) ->
            val anio = k.removePrefix("diario-").substringBefore('#').toIntOrNull()
            if (k.startsWith("diario-") && k.endsWith("#visibilidad") && anio != null && v in VISIBILIDADES) anio to v else null
        }.toMap()

    /** Años del diario, del más reciente al más antiguo: los que tienen recuerdos o metas cumplidas (hasta hoy). */
    fun anios(conRecuerdos: Set<Int>, conMetas: Set<Int>, hoy: Int): List<Int> =
        (conRecuerdos + conMetas).filter { it <= hoy }.sortedDescending()
}
