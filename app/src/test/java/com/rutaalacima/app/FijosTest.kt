package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Cuentas
import com.rutaalacima.app.domain.model.Fijos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FijosTest {
    private val hoy = LocalDate.of(2026, 10, 24)
    private val arriendo = Fijos.Fijo("f1", "gas", 1_200_000.0, "vivienda", "Arriendo", 31, "2026-07")

    @Test fun apareceCadaMesHastaHoy() {
        val v = Fijos.virtuales(listOf(arriendo), hoy)
        // julio 31, agosto 31, septiembre 30 (mes corto); octubre 31 aún no llega
        assertEquals(listOf("2026-07-31", "2026-08-31", "2026-09-30"), v.keys.sorted())
        assertTrue(v.values.flatten().all { it.virtual && it.fx == "f1" })
    }

    @Test fun cambiadoAManoNoSeDuplica() {
        val dias = mapOf("2026-08-31" to listOf(Cuentas.Mov("m1", "gas", 1_300_000.0, "vivienda", "Arriendo", fx = "f1")))
        val u = Fijos.unir(dias, listOf(arriendo), hoy)
        assertEquals(1, u["2026-08-31"]!!.size)
        assertEquals(1_300_000.0, u["2026-08-31"]!!.single().m, 0.0)
    }

    @Test fun saltarYMontoDesdeEsteMes() {
        val l = Fijos.saltar(listOf(arriendo), "f1", "2026-08", true)
        assertEquals(listOf("2026-07-31", "2026-09-30"), Fijos.virtuales(l, hoy).keys.sorted())
        val c = Fijos.cambiarMonto(listOf(arriendo.copy(dia = 5)), "f1", 1_500_000.0, hoy, "f2")
        val v = Fijos.virtuales(c, hoy)
        assertEquals(1_200_000.0, v["2026-09-05"]!!.single().m, 0.0)
        assertEquals(1_500_000.0, v["2026-10-05"]!!.single().m, 0.0)
        assertEquals("", Cuentas.escribir(v.values.flatten()))   // los virtuales no se guardan
    }
}
