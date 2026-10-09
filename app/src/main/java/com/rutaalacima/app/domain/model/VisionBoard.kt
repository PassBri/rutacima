package com.rutaalacima.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Vision board armado con los datos de la persona: la IA (o estas reglas, sin conexión)
 * propone las casillas del tablero —una por la cumbre, por cada propósito, por las metas del
 * año y por los ejes que faltan— y la persona las llena con SUS fotos.
 */
object VisionBoard {
    const val MAXIMO = 9

    /** Una casilla propuesta (todavía sin foto). */
    @Serializable
    data class Propuesta(
        val titulo: String,
        /** Frase en presente y primera persona que acompaña la imagen. */
        val afirmacion: String,
        val eje: String? = null,
        /** Qué foto buscar o tomar. */
        val sugerencia: String,
        /** Palabras para buscar ideas de imágenes. */
        val busqueda: String,
        /** De dónde sale: cumbre, proposito:ID, meta:ID o eje:COD. */
        val origen: String,
    )

    data class Proposito(val id: Long, val titulo: String, val eje: String?, val anioFin: Int)
    data class Meta(val id: Long, val titulo: String, val eje: String?, val anio: Int)

    /** Textos en el idioma de la app (vienen de strings.xml). */
    data class Textos(
        val tituloCumbre: String,
        /** "Propósito · %1$d" */
        val tituloProposito: String,
        /** "Meta %1$d" */
        val tituloMeta: String,
        val sugerenciaCumbre: String,
        /** "%1$s Relacionada con: %2$s" */
        val sugerenciaRelacionada: String,
        /** Nombre, afirmación y sugerencia de foto de cada eje (VOL, MAE, VOZ, VAL, EVO, TRA). */
        val ejes: Map<String, Triple<String, String, String>>,
        /** Lo mismo para Confluencia (CON) y Campamento Base (CAM). */
        val campamentos: Map<String, Triple<String, String, String>> = emptyMap(),
        /** Afirmación de la cumbre cuando todavía no la ha escrito. */
        val cumbreVacia: String = "",
    )

    /** Las 9 casillas del vision board: la cumbre y los 8 campamentos de la Brújula de la Cima. */
    val ORIGENES: List<String> = listOf("cumbre") + Mandala.CAMPAMENTOS_FIJOS.map { Mandala.origenCampamento(it) }

    val CODIGOS = listOf("VOL", "MAE", "VOZ", "VAL", "EVO", "TRA")

    /**
     * Propuesta sin IA: la cumbre y los 8 campamentos fijos. Cada eje toma, si existe, el propósito o
     * la meta del año de ese eje (así la frase es de la persona); Confluencia toma un propósito sin eje
     * (los que tocan varios); lo demás usa la afirmación del método.
     */
    @Suppress("UNUSED_PARAMETER")
    fun proponer(
        cumbre: String,
        propositos: List<Proposito>,
        metasDelAnio: List<Meta>,
        ejeMasDebil: String?,
        t: Textos,
    ): List<Propuesta> {
        val r = mutableListOf<Propuesta>()
        val c = cumbre.trim()
        r += Propuesta(t.tituloCumbre, c.ifBlank { t.cumbreVacia.ifBlank { t.tituloCumbre } }, null, t.sugerenciaCumbre,
            palabras(c.ifBlank { t.sugerenciaCumbre }), "cumbre")
        for (cod in Mandala.CAMPAMENTOS_FIJOS) {
            val (nombre, afirmacion, foto) = t.ejes[cod] ?: t.campamentos[cod] ?: continue
            val propio = when (cod) {
                "CON" -> propositos.firstOrNull { it.eje == null }?.titulo
                "CAM" -> null
                else -> propositos.firstOrNull { it.eje == cod }?.titulo ?: metasDelAnio.firstOrNull { it.eje == cod }?.titulo
            }?.trim()?.takeIf { it.isNotEmpty() }
            r += Propuesta(
                titulo = nombre, afirmacion = propio ?: afirmacion, eje = cod.takeIf { it in CODIGOS },
                sugerencia = if (propio != null) t.sugerenciaRelacionada.format(foto, propio) else foto,
                busqueda = palabras(propio ?: foto), origen = Mandala.origenCampamento(cod),
            )
        }
        return r
    }

    /** Lo que se le pide a la IA (el coach ya conoce la cumbre, los propósitos, las metas y los ejes). */
    fun instruccionIa(): String = """
        Arma mi vision board con lo que sabes de mí (mi cumbre, mis propósitos, mis metas y mis ejes).
        Son exactamente $MAXIMO casillas, una por cada campamento de mi Brújula de la Cima: CUMBRE (mi cumbre
        personal), VOL, MAE, VOZ, VAL, EVO, TRA (mis 6 ejes), CON (Confluencia: un proyecto que activa varios
        ejes) y CAM (Campamento Base: mentor, cordada y red de apoyo).
        Responde SOLO con un arreglo JSON, sin texto antes ni después. Cada casilla:
        {"campamento": "CUMBRE|VOL|MAE|VOZ|VAL|EVO|TRA|CON|CAM",
         "titulo": "rótulo corto, máx. 4 palabras",
         "afirmacion": "frase en presente y primera persona, máx. 14 palabras, sobre algo concreto de mi vida",
         "eje": "VOL|MAE|VOZ|VAL|EVO|TRA o null",
         "sugerencia": "qué foto MÍA buscar o tomar para esta casilla (no imágenes genéricas)",
         "busqueda": "2 a 4 palabras para buscar ideas de imágenes"}
        Usa mis propósitos y metas en el campamento de su eje. Escribe en mi idioma.
    """.trimIndent()

    /** Lee la respuesta de la IA; null si no trae un arreglo válido. */
    fun desdeIa(texto: String): List<Propuesta>? {
        val ini = texto.indexOf('[')
        val fin = texto.lastIndexOf(']')
        if (ini < 0 || fin <= ini) return null
        val arreglo = runCatching { Json.parseToJsonElement(texto.substring(ini, fin + 1)) as? JsonArray }.getOrNull() ?: return null
        val r = arreglo.mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            fun s(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
            val afirmacion = s("afirmacion") ?: return@mapNotNull null
            val eje = s("eje")?.uppercase()?.takeIf { it in CODIGOS }
            val camp = s("campamento")?.uppercase()
            val origen = when {
                camp == "CUMBRE" -> "cumbre"
                camp != null && camp in Mandala.CAMPAMENTOS_FIJOS -> Mandala.origenCampamento(camp)
                eje != null -> Mandala.origenCampamento(eje)
                else -> "ia"
            }
            Propuesta(
                titulo = (s("titulo") ?: afirmacion).take(40),
                afirmacion = afirmacion.take(160),
                eje = eje ?: camp?.takeIf { it in CODIGOS },
                sugerencia = (s("sugerencia") ?: "").take(200),
                busqueda = (s("busqueda") ?: palabras(afirmacion)).take(60),
                origen = origen,
            )
        }.take(MAXIMO)
        return r.takeIf { it.size >= 3 }
    }

    private val VACIAS = setOf(
        "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "a", "en", "y", "o", "con", "por", "para",
        "mi", "mis", "tu", "tus", "su", "sus", "que", "se", "lo", "es", "son", "como", "más", "the", "and", "of", "to", "my",
        "en", "una", "sin", "sobre", "hasta", "desde", "cada", "todo", "toda",
    )

    /** Hasta 4 palabras con sentido para buscar imágenes. */
    fun palabras(texto: String): String =
        texto.lowercase().split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 2 && it !in VACIAS && it.toIntOrNull() == null }
            .distinct().take(4).joinToString(" ")

    /** Página de imágenes libres (Pexels) con esa búsqueda. */
    fun urlIdeas(busqueda: String): String =
        "https://www.pexels.com/search/" + java.net.URLEncoder.encode(busqueda.ifBlank { "mountain summit" }, "UTF-8").replace("+", "%20") + "/"
}
