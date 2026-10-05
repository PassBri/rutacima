package com.rutaalacima.app

import com.rutaalacima.app.data.content.BulletBlock
import com.rutaalacima.app.data.content.CalloutBlock
import com.rutaalacima.app.data.content.ContentJson
import com.rutaalacima.app.data.content.ImageBlock
import com.rutaalacima.app.data.content.Locucion
import com.rutaalacima.app.data.content.ParagraphBlock
import com.rutaalacima.app.data.content.PromptBlock
import com.rutaalacima.app.data.content.Section
import com.rutaalacima.app.data.content.TableBlock
import com.rutaalacima.app.data.content.Workbook
import com.rutaalacima.app.data.content.WorkbookSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocucionTest {
    @Test fun quitaRayasDeLlenarYEspacios() {
        assertEquals("Total: /60", Locucion.limpiar("Total:   ____/60"))
        assertEquals("Me comprometo a dedicar horas/semana durante semanas/meses a este proyecto.",
            Locucion.limpiar("Me comprometo a dedicar _____ horas/semana durante _____ semanas/meses a este proyecto."))
        assertEquals("¿cuántos cumpliste?", Locucion.limpiar("¿cuántos cumpliste? ___/___  (%: ___)"))
    }

    @Test fun leeTituloBloquesRecuadrosYTablasSinImagenes() {
        val s = Section("x.sec0", "Parte 1: El inicio", listOf(
            ParagraphBlock("Primer párrafo del capítulo, con suficiente texto para no unirse con lo demás de este capítulo."),
            ImageBlock("foto.png"),
            CalloutBlock(listOf(BulletBlock("Una idea dentro del recuadro que también tiene bastante texto para quedar sola."))),
            TableBlock(listOf("Eje", "Pregunta"), listOf(listOf("Voluntad", "¿Qué me detiene cuando quiero empezar algo importante?"))),
            PromptBlock("x.p1", label = "Escribe tu respuesta: ______"),
        ))
        val f = Locucion.fragmentos(s)
        assertEquals("Parte 1: El inicio", f.first())
        assertTrue(f.any { it.startsWith("Una idea dentro del recuadro") })
        assertTrue(f.any { it == "Voluntad. Pregunta: ¿Qué me detiene cuando quiero empezar algo importante?" })
        assertTrue(f.last().endsWith("Escribe tu respuesta"))
        assertFalse(f.any { "foto.png" in it })
    }

    @Test fun parteTextosLargosSinPasarseDelLimite() {
        val largo = (1..400).joinToString(" ") { "Frase número $it." }
        val p = Locucion.partir(largo)
        assertTrue(p.size > 1)
        assertTrue(p.all { it.length <= Locucion.MAX_FRAGMENTO })
        assertEquals(largo, p.joinToString(" "))
    }

    /** Todas las guías se pueden escuchar: cada capítulo tiene voz y ningún fragmento se pasa del límite. */
    @Test fun todasLasGuiasSePuedenEscuchar() {
        fun leer(n: String) = requireNotNull(javaClass.classLoader?.getResource("content/$n")).readText()
        val indice = ContentJson.decodeFromString<List<WorkbookSummary>>(leer("index.json"))
        var minutos = 0
        indice.forEach { w ->
            val wb = ContentJson.decodeFromString<Workbook>(leer("${w.id}.json"))
            wb.sections.forEach { s ->
                val f = Locucion.fragmentos(s)
                assertTrue("${w.id}/${s.id} sin texto", f.isNotEmpty())
                assertTrue("${w.id}/${s.id} fragmento largo", f.all { it.length <= Locucion.MAX_FRAGMENTO })
                assertFalse("${w.id}/${s.id} con rayas", f.any { "__" in it })
                minutos += Locucion.minutos(f)
            }
        }
        println("Audiolibros: ${indice.size} guías, unos $minutos minutos de escucha")
    }
}
