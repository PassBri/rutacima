package com.rutaalacima.app.data.bancos

import android.content.Context
import com.rutaalacima.app.data.content.ContentJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Bancos de los bonos de Ruta a la Cima, convertidos en catálogos que la app usa
 * donde corresponde (generados con tools/bancos.py):
 *  - Bono 1 · Indicadores por eje → al definir el indicador de éxito de una meta.
 *  - Bono 2 · Metas por área (con evidencia observable y plazo) → plantillas al crear metas.
 *  - Bono 3 · Proyectos de confluencia → plantillas de propósitos/metas multi-eje.
 *  - Bono 5 · Acciones multi-eje → sugerencias para el plan de acción.
 */
@Serializable
data class Indicador(val eje: String, val texto: String)

@Serializable
data class MetaPlantilla(val area: String, val meta: String, val observable: String = "", val plazo: String = "")

@Serializable
data class AccionMultiEje(val ejes: List<String>, val texto: String)

@Serializable
data class ProyectoConfluencia(val nombre: String, val descripcion: String = "", val ejes: Map<String, String> = emptyMap())

data class Bancos(
    val indicadores: List<Indicador>,
    val metas: List<MetaPlantilla>,
    val acciones: List<AccionMultiEje>,
    val proyectos: List<ProyectoConfluencia>,
) {
    val areas: List<String> get() = metas.map { it.area }.distinct()

    fun indicadoresDe(eje: String?): List<Indicador> =
        if (eje == null) indicadores else indicadores.filter { it.eje == eje }

    /** Acciones que activan el eje (las de más ejes primero: más confluencia). */
    fun accionesDe(eje: String?): List<AccionMultiEje> =
        (if (eje == null) acciones else acciones.filter { eje in it.ejes }).sortedByDescending { it.ejes.size }

    /** Área del banco de metas más afín a cada eje (para sugerir plantillas). */
    fun areasSugeridas(eje: String?): List<String> = when (eje) {
        "VOL" -> listOf("Hábitos y disciplina", "Salud y bienestar")
        "MAE" -> listOf("Estudio y educación", "Carrera y desarrollo profesional")
        "VOZ" -> listOf("Escritura y creación", "Creatividad y arte")
        "VAL" -> listOf("Dinero y finanzas", "Carrera y desarrollo profesional")
        "EVO" -> listOf("Hábitos y disciplina", "Salud y bienestar")
        "TRA" -> listOf("Relaciones y familia", "Creatividad y arte")
        else -> areas
    }
}

class BancosRepository(private val context: Context) {
    private var cache: Bancos? = null
    private val mutex = Mutex()

    suspend fun bancos(): Bancos = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) {
            fun leer(n: String) = context.assets.open("bancos/$n.json").bufferedReader().use { it.readText() }
            Bancos(
                indicadores = ContentJson.decodeFromString(leer("indicadores")),
                metas = ContentJson.decodeFromString(leer("metas")),
                acciones = ContentJson.decodeFromString(leer("acciones")),
                proyectos = ContentJson.decodeFromString(leer("proyectos")),
            )
        }.also { cache = it }
    }
}
