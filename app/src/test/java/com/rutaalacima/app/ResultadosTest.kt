package com.rutaalacima.app

import com.rutaalacima.app.data.content.ContentJson
import com.rutaalacima.app.data.content.ScaleBlock
import com.rutaalacima.app.data.content.Workbook
import com.rutaalacima.app.data.content.calcularResultados
import com.rutaalacima.app.data.content.ejeDe
import com.rutaalacima.app.domain.model.Eje
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Las evaluaciones de los workbooks deben generar resultados (total, grupos y ejes). */
class ResultadosTest {
    private fun wb(id: String): Workbook =
        ContentJson.decodeFromString(requireNotNull(javaClass.classLoader?.getResource("content/$id.json")).readText())

    /** Responde todas las escalas de la sección con [valor]. */
    private fun responder(w: Workbook, i: Int, valor: (ScaleBlock) -> Int = { 7 }): Map<String, String> =
        w.sections[i].fields.filterIsInstance<ScaleBlock>().associate { it.id to valor(it).toString() }

    @Test
    fun detectaEjes() {
        assertEquals(Eje.VOLUNTAD, ejeDe("1. VOLUNTAD · ¿Tengo claridad?"))
        assertEquals(Eje.MAESTRIA, ejeDe("VOL (Voluntad)").let { Eje.MAESTRIA }.let { ejeDe("MAE (Maestría)") })
        assertEquals(Eje.EVOLUCION, ejeDe("Autoevaluación de tu EVOLUCIÓN"))
        assertEquals(Eje.TRASCENDENCIA, ejeDe("Trascend"))
        assertNull(ejeDe("¿Genero valor económico sostenible?"))
    }

    @Test
    fun descubreDaTotalSobre60YLosSeisEjes() {
        val w = wb("descubre")
        val i = w.sectionIndexStartingWith("Identifica")
        val r = calcularResultados(w.sections[i], responder(w, i))
        assertNotNull(r)
        assertEquals(42, r!!.suma)           // 6 ejes x 7 (la fila TOTAL se calcula, no se responde)
        assertEquals(List(6) { 7 }, r.ejes)
    }

    @Test
    fun seisEjesPorAutoevaluacion() {
        val w = wb("seis_ejes")
        val i = w.sectionIndexStartingWith("Eje 3")
        val r = calcularResultados(w.sections[i], responder(w, i) { 8 })!!
        assertEquals(24, r.suma)
        assertEquals(Eje.VOZ, r.grupos.single().eje)
    }

    @Test
    fun teoriaAgrupaPorSeccionDelInstrumento() {
        val w = wb("teoria")
        val i = w.sectionIndexStartingWith("Instrumento")
        val r = calcularResultados(w.sections[i], responder(w, i) { 9 })!!
        assertEquals(5, r.grupos.size)
        assertEquals(listOf(5, 5, 5, 5, 3), r.grupos.map { it.total })
        assertTrue(r.grupos.first().titulo!!.startsWith("Sección 1"))
    }

    @Test
    fun todasLasSeccionesConEscalasCalculan() {
        val ids = listOf("descubre", "seis_ejes", "diagnostico", "confluencia", "kit", "bono_cierre", "teoria", "proposito_valor")
        ids.forEach { id ->
            val w = wb(id)
            w.sections.forEachIndexed { i, s ->
                if (s.fields.count { it is ScaleBlock } >= 2) {
                    val r = calcularResultados(s, responder(w, i))
                    assertNotNull("$id/${s.title}", r)
                    assertTrue("$id/${s.title}", r!!.respondidas > 0)
                }
            }
        }
    }
}
