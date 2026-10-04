package com.rutaalacima.app

import com.rutaalacima.app.domain.model.FraseLibro
import com.rutaalacima.app.domain.model.FrasesDelDia
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Las 365 frases del día: una por día, en los 12 idiomas, alineadas con el español. */
class FrasesTest {
    private fun cargar(l: String): List<FraseLibro> = Json.decodeFromString(
        ListSerializer(FraseLibro.serializer()),
        requireNotNull(javaClass.classLoader?.getResource("frases/$l.json")).readText(),
    )

    @Test fun unaFrasePorDia() {
        assertEquals(0, FrasesDelDia.indice(LocalDate.of(2026, 1, 1)))
        assertEquals(364, FrasesDelDia.indice(LocalDate.of(2026, 12, 31)))
        assertEquals(364, FrasesDelDia.indice(LocalDate.of(2028, 12, 31))) // año bisiesto
        assertEquals(276, FrasesDelDia.indice(LocalDate.of(2026, 10, 4)))
    }

    @Test fun docesIdiomasCon365Frases() {
        val es = cargar("es")
        assertEquals(365, es.size)
        assertEquals(365, es.map { it.t }.toSet().size)
        for (l in listOf("en", "pt", "fr", "de", "it", "zh", "ja", "ko", "ar", "hi", "ru")) {
            val f = cargar(l)
            assertEquals(l, 365, f.size)
            assertTrue(l, f.all { it.t.isNotBlank() })
            assertEquals(l, es.map { it.libro }, f.map { it.libro })
        }
    }
}
