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

class GraficaAscensoTest {
    @Test fun velasYSaldo() {
        val g = com.rutaalacima.app.domain.model.GraficaAscenso
        val dias = mapOf(
            "2026-09-03" to listOf(Cuentas.Mov("1", "ing", 1000.0, "trabajo")),
            "2026-09-20" to listOf(Cuentas.Mov("2", "gas", 300.0, "vivienda")),
            "2026-10-05" to listOf(Cuentas.Mov("3", "gas", 900.0, "vivienda"), Cuentas.Mov("4", "aho", 50.0, "emergencia")),
        )
        val p = g.de(dias, g.Gran.M, LocalDate.of(2026, 10, 24))
        assertEquals(2, p.size)
        assertEquals(0.0, p[0].o, 0.0); assertEquals(1000.0, p[0].h, 0.0); assertEquals(700.0, p[0].c, 0.0)
        assertEquals(700.0, p[1].o, 0.0); assertEquals(-250.0, p[1].c, 0.0); assertEquals(-250.0, p[1].l, 0.0)
        assertEquals(50.0, p[1].patr, 0.0)
        assertEquals(0.7, p[0].flujo!!, 1e-9)
        assertEquals(listOf(0.0, 200.0, 400.0, 600.0, 800.0, 1000.0), g.marcas(0.0, 1000.0))
    }
}
