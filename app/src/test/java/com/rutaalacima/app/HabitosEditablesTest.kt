package com.rutaalacima.app

import com.rutaalacima.app.domain.model.ChecklistDiario
import com.rutaalacima.app.domain.model.Eje
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitosEditablesTest {
    @Test fun jsonIdaYVuelta() {
        var l = ChecklistDiario.renombrar(ChecklistDiario.base, "VOL1", "Leer mi cumbre")
        l = ChecklistDiario.agregar(l, Eje.VOZ, "Grabar un audio")
        l = ChecklistDiario.quitar(l, "MAE3")
        val vuelta = ChecklistDiario.desdeJson(ChecklistDiario.aJson(l))!!
        assertEquals(l, vuelta)
        assertEquals("Leer mi cumbre", vuelta.first { it.id == "VOL1" }.texto)
        assertEquals(4, vuelta.count { it.eje == Eje.VOZ })
        assertEquals(2, vuelta.count { it.eje == Eje.MAESTRIA })
    }

    @Test fun cadaEjeConservaAlMenosUno() {
        var l = ChecklistDiario.base
        listOf("TRA1", "TRA2", "TRA3").forEach { l = ChecklistDiario.quitar(l, it) }
        assertEquals(1, l.count { it.eje == Eje.TRASCENDENCIA })
        // Una lista guardada sin un eje recupera los de fábrica de ese eje
        val sinVoz = ChecklistDiario.desdeJson(ChecklistDiario.aJson(ChecklistDiario.base.filterNot { it.eje == Eje.VOZ }))!!
        assertEquals(3, sinVoz.count { it.eje == Eje.VOZ })
    }

    @Test fun maximoPorEjeYTextoVacio() {
        var l = ChecklistDiario.base
        repeat(10) { l = ChecklistDiario.agregar(l, Eje.VALOR, "Ahorrar $it") }
        assertEquals(ChecklistDiario.MAX_POR_EJE, l.count { it.eje == Eje.VALOR })
        assertEquals(l, ChecklistDiario.agregar(l, Eje.EVOLUCION, "  "))
        assertTrue(l.map { it.id }.toSet().size == l.size)
    }

    @Test fun jsonInvalidoDaNull() { assertNull(ChecklistDiario.desdeJson("no es json")); assertNull(ChecklistDiario.desdeJson("")) }
}
