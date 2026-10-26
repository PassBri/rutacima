package com.rutaalacima.app.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest

/**
 * Reglas para sincronizar la ruta del teléfono con SuRoute Web (sin Android: se prueba con JUnit).
 *
 * Cada cosa (perfil, una meta, un día del checklist…) es un documento JSON con una clave
 * "tipo/clave". En cada sincronización se comparan tres versiones:
 *  - la local (lo que hay en el teléfono),
 *  - la remota (lo que hay en la nube),
 *  - la huella de lo último que se sincronizó.
 * Así se sabe quién cambió desde la última vez. La web es la misma app: puede crear, editar y
 * borrar lo mismo que el teléfono. Si los dos cambiaron lo mismo, se fusiona (ver [fusionar]).
 */
object Sincronia {

    /** Letras de los códigos de vinculación (sin O/0 ni I/1, igual que en supabase/schema.sql). */
    private const val ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /**
     * Saca el código de 8 letras de lo que se escaneó o escribió: "rutacima://vincular?codigo=K7P4QX9M",
     * "k7p4-qx9m" o "K7P4 QX9M". Devuelve null si no es un código válido.
     */
    fun codigoDeVinculo(texto: String): String? {
        Cordada.codigo(texto)?.let { return it }
        val crudo = texto.substringAfter("codigo=", texto).substringBefore('&')
        val c = crudo.uppercase().filter { it.isLetterOrDigit() }
        return c.takeIf { it.length == 8 && it.all { ch -> ch in ALFABETO } }
    }

    /** "SURABB8W" → "cóndor · nevado · 42"; los códigos antiguos, "K7P4-QX9M". */
    fun codigoLegible(c: String) = Cordada.palabras(c)?.joinToString(" · ") ?: if (c.length == 8) c.take(4) + "-" + c.drop(4) else c

    /** Todo lo que se sincroniza (igual que la lista de supabase/schema.sql). */
    val TIPOS = setOf(
        "perfil", "proposito", "accion", "meta_anio", "meta_mes", "balance", "agenda", "mes",
        "checklist", "ejes", "respuesta", "coach", "vision", "frases",
    )

    fun tipoDe(k: String) = k.substringBefore('/')

    /** Lo que se puede cambiar desde la web (hoy, todo lo que se sincroniza). */
    /** Lo que guarda una copia de seguridad: lo que se sincroniza más el diario de vida (publicaciones propias). */
    val TIPOS_RESPALDO = TIPOS + "publicacion"

    fun bidireccional(k: String) = tipoDe(k) in TIPOS

    /** JSON en forma canónica: llaves ordenadas y sin espacios (Postgres reordena los jsonb). */
    fun canonico(e: JsonElement): String = when (e) {
        is JsonObject -> e.entries.sortedBy { it.key }
            .joinToString(",", "{", "}") { (k, v) -> "${JsonPrimitive(k)}:${canonico(v)}" }
        is JsonArray -> e.joinToString(",", "[", "]") { canonico(it) }
        is JsonPrimitive, JsonNull -> e.toString()
    }

    fun canonico(texto: String): String = canonico(Json.parseToJsonElement(texto))

    // ------------------------------------------------------------------ Respaldo en un archivo

    const val FORMATO_RESPALDO = "rutaalacima-respaldo"

    /** Toda la ruta en un archivo JSON legible ({formato, version, fecha, documentos: {"tipo/clave": {...}}}). */
    fun respaldo(documentos: Map<String, String>, version: String, fecha: String): String {
        val docs = documentos.toSortedMap().entries.joinToString(",\n    ", "{\n    ", "\n  }") { (k, v) -> "${JsonPrimitive(k)}: ${canonico(v)}" }
        return "{\n  \"formato\": \"$FORMATO_RESPALDO\",\n  \"version\": ${JsonPrimitive(version)},\n  \"fecha\": ${JsonPrimitive(fecha)},\n  \"documentos\": $docs\n}\n"
    }

    /** Lee un respaldo; solo devuelve los tipos conocidos. Lanza IllegalArgumentException si no es un respaldo. */
    fun leerRespaldo(texto: String): Map<String, String> {
        val raiz = runCatching { Json.parseToJsonElement(texto) as JsonObject }.getOrNull()
            ?: throw IllegalArgumentException("no es JSON")
        require((raiz["formato"] as? JsonPrimitive)?.content == FORMATO_RESPALDO) { "no es un respaldo de SuRoute" }
        val docs = raiz["documentos"] as? JsonObject ?: throw IllegalArgumentException("sin documentos")
        return docs.filter { (k, v) -> tipoDe(k) in TIPOS_RESPALDO && k.contains('/') && v is JsonObject }.mapValues { canonico(it.value) }
    }

    /**
     * Combina dos documentos: toma del respaldo solo los campos con contenido (ni vacíos, ni nulos, ni falsos)
     * y deja el resto como está. Sirve para restaurar un archivo parcial sin borrar lo que ya había.
     */
    fun combinar(local: String, respaldo: String): String {
        val l = Json.parseToJsonElement(local) as JsonObject
        val r = Json.parseToJsonElement(respaldo) as JsonObject
        val util = r.filterValues { v ->
            v !is JsonNull && !(v is JsonPrimitive && v.isString && v.content.isBlank()) && !(v is JsonPrimitive && !v.isString && v.content == "false")
        }
        return canonico(JsonObject(l + util))
    }

    fun huella(texto: String): String =
        MessageDigest.getInstance("SHA-256").digest(canonico(texto).toByteArray())
            .take(12).joinToString("") { "%02x".format(it) }

    data class Plan(
        /** Documentos a subir a la nube. */
        val subir: Map<String, String>,
        /** Documentos a escribir en el teléfono (cambios hechos en la web). */
        val aplicar: Map<String, String>,
        /** Claves a borrar del teléfono (se borraron en la web). */
        val borrarLocal: Set<String>,
        /** Claves a borrar de la nube (se borraron en el teléfono). */
        val borrar: Set<String>,
        /** Huellas tras sincronizar (null = olvidar la clave). */
        val huellas: Map<String, String?>,
    ) {
        val vacio get() = subir.isEmpty() && aplicar.isEmpty() && borrar.isEmpty() && borrarLocal.isEmpty()
    }

    /**
     * @param locales documentos del teléfono dentro de la ventana de sincronización.
     * @param remotos documentos de la nube dentro de la misma ventana.
     * @param huellas huellas de la última sincronización (solo se miran las claves de la ventana).
     * @param enVentana si una clave está dentro de la ventana (las de fuera no se tocan).
     * @param fusionar une dos versiones de un documento bidireccional que cambiaron en ambos lados.
     */
    fun planear(
        locales: Map<String, String>,
        remotos: Map<String, String>,
        huellas: Map<String, String>,
        enVentana: (String) -> Boolean = { true },
        fusionar: (clave: String, local: String, remoto: String) -> String,
    ): Plan {
        val subir = mutableMapOf<String, String>()
        val aplicar = mutableMapOf<String, String>()
        val borrar = mutableSetOf<String>()
        val borrarLocal = mutableSetOf<String>()
        val nuevas = mutableMapOf<String, String?>()
        val claves = locales.keys + remotos.keys + huellas.keys.filter(enVentana)
        for (k in claves) {
            val l = locales[k]
            val r = remotos[k]
            val h = huellas[k]
            val hl = l?.let(::huella)
            val hr = r?.let(::huella)
            val cambioLocal = hl != h
            val cambioRemoto = hr != h
            when {
                hl == hr -> if (hl != h) nuevas[k] = hl                       // ya iguales en ambos lados
                cambioLocal && !cambioRemoto -> {                              // solo cambió el teléfono
                    if (l == null) borrar += k else subir[k] = l
                    nuevas[k] = hl
                }
                !cambioLocal && cambioRemoto -> when {                         // solo cambió la nube
                    r == null && l != null && bidireccional(k) -> { borrarLocal += k; nuevas[k] = null }  // se borró en la web
                    r == null && l != null -> { subir[k] = l; nuevas[k] = hl }
                    r == null -> nuevas[k] = null
                    bidireccional(k) -> { aplicar[k] = r; nuevas[k] = hr }
                    l != null -> { subir[k] = l; nuevas[k] = hl }              // el teléfono manda
                    else -> {}                                                  // dato del teléfono que ya no existe aquí
                }
                else -> when {                                                  // cambiaron los dos
                    l == null -> { borrar += k; nuevas[k] = null }
                    r == null -> { subir[k] = l; nuevas[k] = hl }
                    bidireccional(k) -> {
                        val f = fusionar(k, l, r)
                        val hf = huella(f)
                        if (hf != hl) aplicar[k] = f
                        if (hf != hr) subir[k] = f
                        nuevas[k] = hf
                    }
                    else -> { subir[k] = l; nuevas[k] = hl }
                }
            }
        }
        return Plan(subir, aplicar, borrarLocal = borrarLocal, borrar = borrar, huellas = nuevas)
    }

    /**
     * Seguro contra borrados masivos: si la nube llega vacía pero ya se había sincronizado
     * (la tabla se vació, o la consulta vino mal), no se borra nada en el teléfono; se vuelve
     * a subir todo como si fuera la primera vez.
     */
    fun huellasConfiables(remotos: Map<String, String>, huellas: Map<String, String>): Map<String, String> =
        if (remotos.isEmpty() && huellas.isNotEmpty()) emptyMap() else huellas

    /** Borrar más de la mitad de lo que hay en el teléfono de una vez se toma como un error. */
    fun borradoSospechoso(plan: Plan, locales: Int): Boolean =
        plan.borrarLocal.size > 10 && plan.borrarLocal.size * 2 > locales

    /**
     * Fusión de un documento que cambió en el teléfono y en la web a la vez: se unen las
     * listas de lo marcado ("marcados" del checklist, "dias" de metas y frases), "cumplida"
     * queda en true si lo está en alguno, y el resto de campos se toma del teléfono.
     */
    fun fusionar(clave: String, local: String, remoto: String): String {
        val l = Json.parseToJsonElement(local) as? JsonObject ?: return local
        val r = Json.parseToJsonElement(remoto) as? JsonObject ?: return local
        // Una respuesta (por ejemplo, los movimientos de un día en Ascenso financiero) es un solo texto:
        // gana la versión escrita más tarde, para que lo anotado en la web no se pierda con la copia del teléfono.
        if (tipoDe(clave) == "respuesta") {
            val tl = (l["actualizadoEn"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
            val tr = (r["actualizadoEn"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
            return canonico(if (tr > tl) r else l)
        }
        val res = l.toMutableMap()
        val orden = compareBy<String>({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it })
        for (campo in listOf("marcados", "dias")) {
            val vl = l[campo]
            val vr = r[campo]
            if (vl == null && vr == null) continue
            val union = (elementos(vl) + elementos(vr)).filter { it.isNotBlank() }.distinct().sortedWith(orden)
            // Se respeta el formato del teléfono: arreglo JSON (frases) o texto "1,2,3" (Room)
            res[campo] = if (vl is JsonArray || (vl == null && vr is JsonArray)) {
                JsonArray(union.map { v -> v.toIntOrNull()?.let { JsonPrimitive(it) } ?: JsonPrimitive(v) })
            } else JsonPrimitive(union.joinToString(","))
        }
        for (campo in listOf("cumplida", "hecha")) {
            val cl = (l[campo] as? JsonPrimitive)?.content == "true"
            val cr = (r[campo] as? JsonPrimitive)?.content == "true"
            if (l[campo] != null || r[campo] != null) res[campo] = JsonPrimitive(cl || cr)
        }
        return canonico(JsonObject(res))
    }

    private fun elementos(e: JsonElement?): List<String> = when (e) {
        null, JsonNull -> emptyList()
        is JsonArray -> e.mapNotNull { (it as? JsonPrimitive)?.content }
        is JsonPrimitive -> if (e.isString) e.content.split(',').map { it.trim() } else listOf(e.content)
        else -> emptyList()
    }
}

/**
 * Código de cordada: los códigos SUR + 5 caracteres guardan dos palabras de montaña y un número
 * («cóndor · nevado · 42»), igual que la web (09-arranque.js) y el servidor (2026-10-27-codigo-cordada.sql).
 */
object Cordada {
    private const val ALF = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val PALABRAS = listOf(
        "andes", "cumbre", "nevado", "cóndor", "glaciar", "sendero", "cordada", "brújula",
        "piolet", "crampón", "mochila", "refugio", "collado", "ladera", "valle", "cresta",
        "arista", "cornisa", "morrena", "páramo", "frailejón", "laguna", "cascada", "quebrada",
        "volcán", "cráter", "granito", "basalto", "cuarzo", "risco", "pico", "cerro",
        "loma", "colina", "meseta", "altiplano", "puna", "pradera", "bosque", "roble",
        "pino", "cedro", "aliso", "musgo", "liquen", "helecho", "orquídea", "puma",
        "llama", "alpaca", "guanaco", "águila", "halcón", "colibrí", "venado", "zorro",
        "oso", "tapir", "trucha", "río", "arroyo", "manantial", "fuente", "niebla",
        "nube", "lluvia", "nieve", "granizo", "viento", "brisa", "aurora", "alba",
        "ocaso", "luna", "estrella", "cometa", "sol", "cielo", "horizonte", "mapa",
        "ruta", "huella", "paso", "bastón", "linterna", "carpa", "fogata", "cantimplora",
        "cuerda", "mosquetón", "arnés", "casco", "bota", "polaina", "anclaje", "vivac",
        "campamento", "bandera", "hito", "baliza", "mirador", "puente", "portillo", "abra",
        "desfiladero", "garganta", "terraza", "sabana", "selva", "jaguar", "tundra", "escarcha",
        "hielo", "serac", "grieta", "avalancha", "ventisca", "tormenta", "trueno", "relámpago",
        "arcoíris", "rocío", "lirio", "romero", "tomillo", "salvia", "eucalipto", "picacho"
    )
    private fun norm(w: String) = java.text.Normalizer.normalize(w, java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()
    private val NORM = PALABRAS.map(::norm)

    fun palabras(c: String): List<String>? {
        if (!Regex("^SUR[A-Z2-9]{5}$").matches(c)) return null
        var v = 0L
        for (ch in c.drop(3)) { val i = ALF.indexOf(ch); if (i < 0) return null; v = v * 32 + i }
        val n = v % 90 + 10; val r = v / 90
        if (r / 128 >= 128) return null
        return listOf(PALABRAS[(r / 128).toInt()], PALABRAS[(r % 128).toInt()], n.toString())
    }

    fun codigo(texto: String): String? {
        val t = norm(texto).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        val w = t.filter { it in NORM }; val num = t.firstOrNull { Regex("^\\d{2}$").matches(it) } ?: return null
        if (w.size != 2) return null
        var v = (NORM.indexOf(w[0]) * 128L + NORM.indexOf(w[1])) * 90 + (num.toInt() - 10)
        val sb = StringBuilder()
        repeat(5) { sb.insert(0, ALF[(v % 32).toInt()]); v /= 32 }
        return "SUR$sb"
    }
}
