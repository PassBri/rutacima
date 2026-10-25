package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Astronomia
import com.rutaalacima.app.domain.model.Astronomia.Planeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El cielo de la app debe ser el mismo de SuRoute Web. Los valores esperados salen de
 * web/js/10-cielo.js para el 12 de octubre de 2026 a las 20:30 en Bogotá (latitud 7.1, longitud -75).
 */
class AstronomiaTest {
    private val ms = 1_791_855_000_000L
    private val lng = -75.0
    private val tol = 1e-6

    private fun igual(az: Double, alt: Double, p: Astronomia.Posicion) {
        assertEquals(az, p.azimut, tol); assertEquals(alt, p.altura, tol)
    }

    @Test fun solYLunaComoEnLaWeb() {
        igual(1.503946, -0.719656, Astronomia.sol(ms, lng = lng))
        igual(1.201564, -0.268002, Astronomia.luna(ms, lng = lng))
        val il = Astronomia.iluminacion(ms)
        assertEquals(0.066920, il.fraccion, tol); assertEquals(0.083291, il.fase, tol)
    }

    @Test fun planetasComoEnLaWeb() {
        igual(1.226406, -0.489426, Astronomia.planeta(Planeta.VENUS, ms, lng = lng))
        igual(-2.933439, -1.176568, Astronomia.planeta(Planeta.JUPITER, ms, lng = lng))
        igual(-2.565956, -1.017556, Astronomia.planeta(Planeta.MARTE, ms, lng = lng))
        igual(-1.475026, 0.834541, Astronomia.planeta(Planeta.SATURNO, ms, lng = lng))
    }

    @Test fun deNocheElSolEstaBajoElHorizonteYAlMediodiaArriba() {
        assertTrue(Astronomia.sol(ms, lng = lng).alturaGrados < -10)
        val mediodia = ms - 8L * 3_600_000 - 30L * 60_000 // 12:00 en Bogotá
        assertTrue(Astronomia.sol(mediodia, lng = lng).alturaGrados > 60)
    }

    @Test fun longitudSaleDeLaZonaHoraria() {
        assertEquals(-75.0, Astronomia.longitudPorZona(-5L * 3_600_000), 1e-9)
    }
}
