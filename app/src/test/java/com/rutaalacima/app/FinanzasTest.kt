package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Finanzas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinanzasTest {
    @Test fun leeMontosEscritosAMano() {
        assertEquals(1_250_000.0, Finanzas.monto("$ 1.250.000"), 0.0)
        assertEquals(50_000.0, Finanzas.monto("50.000"), 0.0)
        assertEquals(12.5, Finanzas.monto("12,5"), 0.0)
        assertEquals(12.5, Finanzas.monto("12.50"), 0.0)
        assertEquals(1_250.0, Finanzas.monto("1,250"), 0.0)
        assertEquals(20_000.0, Finanzas.monto("20 000"), 0.0)
        assertEquals(-3_000.0, Finanzas.monto("-3.000"), 0.0)
        assertEquals(0.0, Finanzas.monto("almuerzo"), 0.0)
        assertEquals(0.0, Finanzas.monto(""), 0.0)
    }

    @Test fun sumaElMes() {
        val m = Finanzas.mes(mapOf(
            1 to Finanzas.dia("100.000", "30.000", "10.000", ""),
            2 to Finanzas.dia("", "20.000", "", "5.000"),
            3 to Finanzas.dia("", "", "", ""),
        ), "200.000")
        assertEquals(100_000.0, m.total.ingresos, 0.0)
        assertEquals(50_000.0, m.total.gastos, 0.0)
        assertEquals(35_000.0, m.total.disponible, 0.0)
        assertEquals(2, m.diasConRegistro)
        assertEquals(0.15, m.tasaAhorro!!, 1e-9)
        assertEquals(0.25, m.usoPresupuesto!!, 1e-9)
        assertEquals(mapOf(1 to 30_000.0, 2 to 20_000.0), m.gastosPorDia)
    }

    @Test fun sinIngresosNoHayTasa() {
        assertNull(Finanzas.mes(mapOf(1 to Finanzas.dia("", "1.000", "", "")), null).tasaAhorro)
        assertNull(Finanzas.mes(emptyMap(), "").usoPresupuesto)
    }
}
