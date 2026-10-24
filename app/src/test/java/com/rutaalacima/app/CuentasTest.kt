package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Cuentas
import com.rutaalacima.app.domain.model.Cuentas.Mochila
import com.rutaalacima.app.domain.model.Cuentas.Mov
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CuentasTest {
    private val dia = listOf(
        Mov("1", "ing", 2_400_000.0, "trabajo"),
        Mov("2", "gas", 85_000.0, "alimentacion"),
        Mov("3", "gas", 32_000.0, "restaurantes"),
        Mov("4", "aho", 300_000.0, "emergencia"),
        Mov("5", "inv", 120_000.0, "educacion"),
    )

    @Test fun sumaElDiaYLasMochilas() {
        val s = Cuentas.sumar(dia)
        assertEquals(1_863_000.0, s.disponible, 0.0)
        assertEquals(420_000.0, s.construido, 0.0)
        assertEquals(85_000.0, s.mochilas[Mochila.VERDE]!!, 0.0)
        assertEquals(32_000.0, s.mochilas[Mochila.AMARILLA]!!, 0.0)
        assertEquals(420_000.0, s.mochilas[Mochila.ROJA]!!, 0.0)
        assertEquals(0.175, s.tasa!!, 1e-9)
    }

    @Test fun guardaYLeeIgualQueLaWeb() {
        val txt = Cuentas.escribir(dia)
        assertEquals(dia, Cuentas.leer(txt))
        // El mismo JSON que escribe la web
        val web = """[{"id":"m1","t":"gas","m":12.5,"c":"hormiga","n":"Café"}]"""
        assertEquals(listOf(Mov("m1", "gas", 12.5, "hormiga", "Café")), Cuentas.leer(web))
        assertEquals(emptyList<Mov>(), Cuentas.leer("no es json"))
    }

    @Test fun losMontosDeAntesCuentanPeroNoSeGuardanDosVeces() {
        val viejos = Cuentas.deAgenda("$ 100.000", "20.000", "", "")
        assertEquals(2, viejos.size)
        assertEquals("ingresos", viejos[0].agenda)
        assertEquals("", Cuentas.escribir(viejos))
        assertEquals(80_000.0, Cuentas.sumar(viejos).disponible, 0.0)
    }

    @Test fun sinIngresosNoHayTasa() = assertNull(Cuentas.sumar(listOf(Mov("1", "gas", 5.0))).tasa)

    @Test fun monedaPorPaisEIdioma() {
        assertEquals("COP", Cuentas.moneda(null, Locale("es", "CO")))
        assertEquals("MXN", Cuentas.moneda(null, Locale("es", "MX")))
        assertEquals("EUR", Cuentas.moneda(null, Locale("de", "DE")))
        assertEquals("JPY", Cuentas.moneda(null, Locale("ja")))
        assertEquals("USD", Cuentas.moneda("USD", Locale("es", "CO")))
    }

    @Test fun formatoSinDecimalesCuandoEsEntero() {
        val co = Cuentas.formato(1_250_000.0, "COP", Locale("es", "CO"))
        assertTrue(co, co.contains("1.250.000") && !co.contains(","))
        val us = Cuentas.formato(12.5, "USD", Locale("en", "US"))
        assertEquals("$12.50", us)
    }
}
