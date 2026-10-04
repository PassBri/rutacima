package com.rutaalacima.app.data.frases

import android.content.Context
import com.rutaalacima.app.domain.model.FraseLibro
import com.rutaalacima.app.domain.model.FrasesDelDia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.Locale

/**
 * Las 365 frases de los libros (assets/frases/<idioma>.json) y el registro de las que la
 * persona ya desbloqueó, por año.
 */
class FrasesRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("frases_dia", Context.MODE_PRIVATE)
    private var cache: Pair<String, List<FraseLibro>>? = null

    private val _desbloqueadas = MutableStateFlow(leer(LocalDate.now().year))
    /** Índices (0..364) desbloqueados en el año en curso. */
    val desbloqueadas: StateFlow<Set<Int>> = _desbloqueadas

    suspend fun todas(): List<FraseLibro> = withContext(Dispatchers.IO) {
        val idioma = Locale.getDefault().language.let { if (it in IDIOMAS) it else "es" }
        cache?.takeIf { it.first == idioma }?.second ?: run {
            val texto = context.assets.open("frases/$idioma.json").bufferedReader().use { it.readText() }
            Json.decodeFromString(ListSerializer(FraseLibro.serializer()), texto).also { cache = idioma to it }
        }
    }

    suspend fun delDia(fecha: LocalDate = LocalDate.now()): FraseLibro = todas()[FrasesDelDia.indice(fecha)]

    fun desbloqueada(fecha: LocalDate = LocalDate.now()): Boolean = FrasesDelDia.indice(fecha) in leer(fecha.year)

    fun desbloquear(fecha: LocalDate = LocalDate.now()) {
        val nuevo = leer(fecha.year) + FrasesDelDia.indice(fecha)
        prefs.edit().putStringSet("anio_${fecha.year}", nuevo.map { it.toString() }.toSet()).apply()
        if (fecha.year == LocalDate.now().year) _desbloqueadas.value = nuevo
    }

    /** Días (0..364) abiertos en [anio]; lo usa la sincronización con RutaCima Web. */
    fun abiertasDe(anio: Int): Set<Int> = leer(anio)

    /** Guarda los días abiertos de [anio] (sincronización: une lo abierto en la web). */
    fun guardarAbiertas(anio: Int, dias: Set<Int>) {
        prefs.edit().putStringSet("anio_$anio", dias.map { it.toString() }.toSet()).apply()
        if (anio == LocalDate.now().year) _desbloqueadas.value = dias
    }

    private fun leer(anio: Int): Set<Int> =
        prefs.getStringSet("anio_$anio", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    companion object {
        val IDIOMAS = setOf("es", "en", "pt", "fr", "de", "it", "zh", "ja", "ko", "ar", "hi", "ru")
    }
}
