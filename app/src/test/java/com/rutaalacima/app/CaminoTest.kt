package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Constancia
import com.rutaalacima.app.domain.model.PrimerosPasos
import com.rutaalacima.app.domain.model.Regreso
import com.rutaalacima.app.domain.model.Revision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CaminoTest {
    private fun d(s: String): LocalDate = LocalDate.parse(s)

    @Test fun primerosPasosVanEnOrdenYTerminan() {
        val vacio = PrimerosPasos.Estado()
        assertEquals(PrimerosPasos.Paso.CUMBRE, PrimerosPasos.siguiente(vacio))
        assertEquals(PrimerosPasos.Paso.EJES, PrimerosPasos.siguiente(vacio.copy(cumbre = true, nacimiento = true)))
        val todo = PrimerosPasos.Estado(true, true, true, true, true, true, true, true, true)
        assertNull(PrimerosPasos.siguiente(todo))
        assertEquals(9, PrimerosPasos.hechos(todo))
    }

    @Test fun regresoDespuesDeCincoDias() {
        assertFalse(Regreso.debeRecibir(null, d("2026-10-05")))
        assertFalse(Regreso.debeRecibir(d("2026-10-01"), d("2026-10-05")))
        assertTrue(Regreso.debeRecibir(d("2026-09-30"), d("2026-10-05")))
    }

    @Test fun revisionTocaElDomingoYElLunesYLaDelMesManda() {
        assertEquals("2026-W40", Revision.clave(Revision.Tipo.SEMANA, d("2026-10-04")))   // domingo
        assertEquals(Revision.Tipo.SEMANA to d("2026-10-04"), Revision.pendiente(d("2026-10-05"), emptySet()))   // lunes → semana anterior
        assertNull(Revision.pendiente(d("2026-10-05"), setOf("2026-W40")))
        assertNull(Revision.pendiente(d("2026-10-07"), emptySet()))                       // miércoles
        assertEquals(Revision.Tipo.MES to d("2026-10-30"), Revision.pendiente(d("2026-10-30"), emptySet()))
        assertEquals(Revision.Tipo.MES to d("2026-09-02"), Revision.pendiente(d("2026-10-02"), emptySet()))
    }

    @Test fun resumenDeLaSemana() {
        val checks = mapOf(d("2026-09-28") to setOf("VOL1", "MAE1"), d("2026-09-30") to setOf("VOL1"), d("2026-10-01") to setOf("VOL1", "VOZ1", "TRA1"))
        val r = Revision.resumen(Revision.Tipo.SEMANA, d("2026-10-01"), checks, d("2026-10-04"))
        assertEquals(7, r.dias); assertEquals(3, r.diasConHabitos)
        assertEquals(d("2026-10-01"), r.mejorDia); assertEquals("VOL1" to 3, r.habitoFuerte)
    }

    @Test fun rachaNoSeRompeAntesDeMarcarHoy() {
        val dias = setOf(d("2026-10-02"), d("2026-10-03"), d("2026-10-04"))
        assertEquals(3, Constancia.racha(dias, d("2026-10-05")))
        assertEquals(4, Constancia.racha(dias + d("2026-10-05"), d("2026-10-05")))
        assertEquals(0, Constancia.racha(dias, d("2026-10-07")))
        assertEquals(3, Constancia.mejorRacha(dias + d("2026-09-20")))
    }

    @Test fun mapaDelAnioTieneTodasLasSemanas() {
        val m = Constancia.mapaDelAnio(2026, mapOf(d("2026-01-01") to 5), d("2026-10-05"))
        assertTrue(m.size in 52..54)
        assertEquals(5, m[0][3])          // 1 de enero de 2026 es jueves
        assertNull(m[0][0])                // lunes 29 de diciembre de 2025: fuera del año
        assertNull(m.last().last())        // futuro
        assertEquals(2, Constancia.nivel(5))
    }
}
