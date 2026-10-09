package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Comodines
import com.rutaalacima.app.domain.model.InvitacionFrase
import com.rutaalacima.app.domain.model.ResumenAnio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AscensoTest {
    private val d0 = LocalDate.of(2026, 3, 1)
    private fun dias(vararg rangos: IntRange) = rangos.flatMap { r -> r.map { d0.plusDays(it.toLong()) } }.toSet()

    @Test fun sinComodinUnDiaVacioCortaLaRacha() {
        val e = Comodines.calcular(dias(0..4, 6..8), d0.plusDays(8))
        assertEquals(3, e.racha); assertEquals(5, e.mejorRacha); assertTrue(e.usados.isEmpty())
    }

    @Test fun sieteDiasGananUnComodinQueCubreElHueco() {
        val e = Comodines.calcular(dias(0..6, 8..10), d0.plusDays(10))
        assertEquals(10, e.racha)                 // 7 + 3; el día 7 lo cubrió el comodín
        assertEquals(setOf(d0.plusDays(7)), e.usados)
        assertEquals(0, e.disponibles)
    }

    @Test fun noSeGuardanMasDeDos() {
        val e = Comodines.calcular(dias(0..29), d0.plusDays(29))
        assertEquals(2, e.disponibles); assertEquals(0, e.proximoEn)
    }

    @Test fun hoySinMarcarNoGastaComodin() {
        val e = Comodines.calcular(dias(0..6), d0.plusDays(7))
        assertEquals(7, e.racha); assertEquals(1, e.disponibles); assertTrue(e.usados.isEmpty())
    }

    @Test fun dosHuecosSeguidosConUnSoloComodinCortan() {
        val e = Comodines.calcular(dias(0..6, 9..9), d0.plusDays(9))
        assertEquals(1, e.racha); assertEquals(setOf(d0.plusDays(7)), e.usados)
    }

    @Test fun elResumenCuentaElAnio() {
        val checks = (0..9).associate { d0.plusDays(it.toLong()) to setOf("VOL1", "MAE2") } + (d0.plusDays(20) to setOf("MAE1"))
        val r = ResumenAnio.calcular(2026, checks, LocalDate.of(2026, 12, 10), 3, 1, 4, 120)
        assertEquals(11, r.diasConHabitos); assertEquals(21, r.habitos); assertEquals("MAE", r.ejeFuerte)
        assertEquals(3, r.mejorMes); assertEquals(10, r.mejorRacha); assertEquals(1, r.comodinesUsados)
        assertTrue(r.suficiente)
    }

    @Test fun elResumenSeOfreceEnDiciembreYPrincipiosDeEnero() {
        assertEquals(2026, ResumenAnio.anioParaOfrecer(LocalDate.of(2026, 12, 1)))
        assertEquals(2026, ResumenAnio.anioParaOfrecer(LocalDate.of(2027, 1, 15)))
        assertNull(ResumenAnio.anioParaOfrecer(LocalDate.of(2027, 1, 16)))
        assertNull(ResumenAnio.anioParaOfrecer(LocalDate.of(2026, 10, 8)))
        assertFalse(ResumenAnio.calcular(2026, emptyMap(), LocalDate.of(2026, 12, 1), 0, 0, 0, 0).suficiente)
    }

    @Test fun laPreguntaCambiaCadaDiaYSeRepiteCadaSemana() {
        val f = LocalDate.of(2026, 10, 8)
        assertEquals(InvitacionFrase.pregunta(f), InvitacionFrase.pregunta(f.plusDays(7)))
        assertEquals(7, (0L..6L).map { InvitacionFrase.pregunta(f.plusDays(it)) }.toSet().size)
        assertEquals("Frase del día 281", InvitacionFrase.etiqueta(280, "Frase del día %d"))
    }
}

class DiarioVidaTest {
    @org.junit.Test fun leeLaVisibilidadDeCadaAnio() {
        val r = mapOf("diario-2004#visibilidad" to "PUBLICA", "diario-2008#visibilidad" to "SEGUIDORES", "diario-x#visibilidad" to "PUBLICA", "otra" to "1",
            "diario-2010#visibilidad" to "CUALQUIERA")
        org.junit.Assert.assertEquals(mapOf(2004 to "PUBLICA", 2008 to "SEGUIDORES"), com.rutaalacima.app.domain.model.DiarioVida.visibilidades(r))
        org.junit.Assert.assertEquals("diario-2004#visibilidad", com.rutaalacima.app.domain.model.DiarioVida.clave(2004))
    }

    @org.junit.Test fun losAniosVanDelMasRecienteAlMasAntiguo() {
        org.junit.Assert.assertEquals(listOf(2026, 2008, 2004, 1997),
            com.rutaalacima.app.domain.model.DiarioVida.anios(setOf(2004, 2026, 2030), setOf(1997, 2008), 2026))
    }
}
