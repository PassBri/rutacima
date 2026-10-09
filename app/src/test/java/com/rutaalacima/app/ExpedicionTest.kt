package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Expedicion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpedicionTest {
    @Test fun aRitmoConstanteLaCumbreLlegaAlFinalDelAnio() {
        val semanal = 500_000L
        val e = Expedicion.calcular(List(52) { semanal }, 0, LocalDate.of(2026, 12, 31))
        // Los primeros tramos suben al tope (4/52) mientras la dificultad alcanza el ritmo real
        assertTrue(e.cumbre)
        // Desde que la dificultad alcanza el ritmo real, cada tramo sube 1/52 de la montaña
        val d = Expedicion.dificultades(List(10) { semanal })
        assertEquals(semanal, d.last())
        assertEquals(Expedicion.ALTURA_MM / 52.0, semanal * Expedicion.mmPorPunto(d.last()), 0.001)
    }

    @Test fun conMillonesDePersonasCadaAporteValeFraccionesDeMilimetro() {
        val millones = 20_000_000L
        val e = Expedicion.calcular(List(20) { millones }, Expedicion.puntos(1, 0), LocalDate.of(2026, 5, 1))
        assertTrue(e.mmPorPublicacion < 0.1)
        assertTrue(e.mm < Expedicion.ALTURA_MM)
    }

    @Test fun laDificultadNoCambiaMasDeCuatroVeces() {
        val d = Expedicion.dificultades(listOf(1_000_000, 100_000_000, 100_000_000, 10))
        assertEquals(100_000L, d[0])
        assertEquals(400_000L, d[1])        // 1.000.000 > 4 × 100.000
        assertEquals(1_600_000L, d[2])      // 100.000.000 > 4 × 400.000
        assertEquals(6_400_000L, d[3])
        val baja = Expedicion.dificultades(listOf(100_000_000, 100_000_000, 100_000_000, 100_000_000, 100_000_000, 10, 10))
        assertEquals(baja[5] / 4, baja[6])  // nunca cae de golpe
    }

    @Test fun unaAvalanchaNoLaSubeAntesDeTreceSemanas() {
        val e = Expedicion.calcular(List(12) { 1_000_000_000L }, 0, LocalDate.of(2026, 3, 20))
        assertEquals(11, e.tramo)
        assertTrue(!e.cumbre)
        assertTrue(!Expedicion.calcular(List(12) { Long.MAX_VALUE / 1_000_000 }, 0, LocalDate.of(2026, 3, 20)).cumbre)
    }

    @Test fun unaComunidadPequenaNoLaSubeEnUnaSemana() {
        val e = Expedicion.calcular(listOf(671), 0, LocalDate.of(2026, 1, 3))
        assertEquals(Expedicion.DIFICULTAD_MIN, e.dificultad)
        assertTrue(e.mm < Expedicion.ALTURA_MM / 52.0)
        assertTrue(e.mmPorPublicacion < 20)   // milímetros, no metros
    }

    @Test fun tramosYAjuste() {
        assertEquals(0, Expedicion.tramoDe(LocalDate.of(2026, 1, 7)))
        assertEquals(1, Expedicion.tramoDe(LocalDate.of(2026, 1, 8)))
        assertEquals(51, Expedicion.tramoDe(LocalDate.of(2026, 12, 31)))
        assertEquals(1, Expedicion.calcular(emptyList(), 0, LocalDate.of(2026, 1, 7)).diasParaAjuste)
        assertEquals(1, Expedicion.calcular(emptyList(), 0, LocalDate.of(2026, 12, 31)).diasParaAjuste)
        val pt = Expedicion.porTramo(listOf(LocalDate.of(2026, 1, 2) to 10L, LocalDate.of(2025, 1, 2) to 10L, LocalDate.of(2026, 1, 9) to 1L), 2026)
        assertEquals(10L, pt[0]); assertEquals(1L, pt[1])
    }
}
