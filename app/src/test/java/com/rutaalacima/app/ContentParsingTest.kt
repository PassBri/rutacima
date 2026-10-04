package com.rutaalacima.app

import com.rutaalacima.app.data.content.ContentJson
import com.rutaalacima.app.data.content.ImageBlock
import com.rutaalacima.app.data.content.ScaleBlock
import com.rutaalacima.app.data.content.Workbook
import com.rutaalacima.app.data.content.WorkbookSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Valida que todo el contenido de assets/content se pueda leer y sea consistente. */
class ContentParsingTest {

    private fun leer(nombre: String): String =
        requireNotNull(javaClass.classLoader?.getResource("content/$nombre")) { "No existe content/$nombre" }.readText()

    private val indice: List<WorkbookSummary> by lazy { ContentJson.decodeFromString(leer("index.json")) }

    @Test
    fun indiceTieneTodosLosWorkbooks() {
        assertEquals(24, indice.size)
        assertEquals(indice.size, indice.map { it.id }.toSet().size)
    }

    @Test
    fun cadaWorkbookSeDecodificaYTieneCampos() {
        for (resumen in indice) {
            val wb: Workbook = ContentJson.decodeFromString(leer("${resumen.id}.json"))
            assertEquals(resumen.id, wb.id)
            assertTrue("${wb.id} sin secciones", wb.sections.isNotEmpty())
            val ids = wb.fields.map { it.id }
            assertEquals("IDs duplicados en ${wb.id}", ids.size, ids.toSet().size)
            assertTrue("IDs de ${wb.id} deben empezar por el id del workbook", ids.all { it.startsWith("${wb.id}.") })
        }
    }

    @Test
    fun todasLasImagenesExisten() {
        fun existe(ruta: String) = javaClass.classLoader?.getResource(ruta) != null
        for (resumen in indice) {
            val wb: Workbook = ContentJson.decodeFromString(leer("${resumen.id}.json"))
            wb.cover?.let { assertTrue("Falta portada $it", existe("content/img/$it")) }
            assertEquals(wb.cover, resumen.cover)
            wb.sections.flatMap { it.blocks }.filterIsInstance<ImageBlock>().forEach {
                assertTrue("Falta imagen ${it.src}", existe("content/img/${it.src}"))
            }
        }
        (1..11).forEach { assertTrue(existe("plan/mes_%02d.jpg".format(it))) }
    }

    @Test
    fun instrumentoDeLaTeoriaTieneEscalas() {
        val teoria: Workbook = ContentJson.decodeFromString(leer("teoria.json"))
        val i = teoria.sectionIndexStartingWith("Instrumento")
        assertTrue(i >= 0)
        assertEquals(23, teoria.sections[i].fields.filterIsInstance<ScaleBlock>().size)
    }

    @Test
    fun protocolosDeNieblaSonLocalizables() {
        val niebla: Workbook = ContentJson.decodeFromString(leer("niebla.json"))
        listOf(1, 2, 3, 4, 5, 8).forEach { n ->
            assertTrue("Falta Protocolo $n", niebla.sectionIndexStartingWith("Protocolo $n") >= 0)
        }
    }

    @Test
    fun herramientasDelKitSonLocalizables() {
        val kit: Workbook = ContentJson.decodeFromString(leer("kit.json"))
        listOf(1, 5, 7).forEach { n -> assertTrue(kit.sectionIndexStartingWith("Herramienta $n") >= 0) }
    }
}
