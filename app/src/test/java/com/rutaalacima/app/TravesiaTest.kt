package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.domain.model.MetodoCima
import com.rutaalacima.app.domain.model.MetodoCima.Paso
import com.rutaalacima.app.domain.model.Travesia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TravesiaTest {
    private val hoy = LocalDate.of(2026, 10, 9)

    @Test fun unPasoQuietoDosSemanasSeCubreDeNiebla() {
        val quieto = Paso(escrito = true, jornadas = setOf(hoy.minusDays(14)), requeridas = 3)
        val activo = Paso(escrito = true, jornadas = setOf(hoy.minusDays(13)), requeridas = 3)
        assertTrue(Travesia.enNiebla(quieto, hoy))
        assertFalse(Travesia.enNiebla(activo, hoy))
        assertFalse(Travesia.enNiebla(Paso(escrito = true), hoy))                      // sin empezar no hay niebla
        assertFalse(Travesia.enNiebla(quieto.copy(cumplido = true), hoy))             // ganado tampoco
        assertEquals(14, Travesia.diasQuieto(quieto, hoy))
        assertNull(Travesia.diasQuieto(Paso(escrito = true), hoy))
    }

    @Test fun trasUnaCaidaElPasoPideMenos() {
        val p = Paso(escrito = true, jornadas = setOf(hoy.minusDays(20)), requeridas = 12)
        assertEquals(1, Travesia.requeridasTrasCaida(p, 1))
        assertEquals(5, Travesia.requeridasTrasCaida(p.copy(requeridas = 5), 8))     // nunca sube
        assertEquals("EMO" to hoy, Travesia.caida("EMO|$hoy"))
        assertNull(Travesia.caida("XXX|$hoy"))
    }

    @Test fun pasosDeConfluencia() {
        assertEquals(listOf("VOZ", "TRA"), Travesia.enlaces("voz, TRA,VOL,xx", "VOL"))
        assertEquals("MAE,VOZ", Travesia.enlacesDeAccion(listOf("VOL", "MAE", "VOZ"), "VOL"))
        val vol = Mandala.CAMPAMENTOS_FIJOS.indexOf("VOL")
        val ids = MutableList<Long?>(8) { null }.also { it[vol] = 7L }
        val r = mapOf(
            Mandala.clave(7, 0) to "Podcast", Mandala.claveHecho(7, 0) to hoy.toString(), Travesia.claveEnlaces(7, 0) to "VOZ,TRA",
            Mandala.clave(7, 1) to "Correr", Mandala.claveHecho(7, 1) to hoy.toString(),
            Mandala.clave(7, 2) to "Leer", Travesia.claveEnlaces(7, 2) to "MAE",            // no ganado
        )
        assertEquals(1, Travesia.confluencias(ids, r))
    }

    @Test fun nuevaMontanaLiberaSoloLosGanados() {
        val r = mapOf(
            Mandala.clave(1, 0) to "Correr", Mandala.claveHecho(1, 0) to "2026-03-01", MetodoCima.claveJornadas(1, 0) to "2026-03-01",
            Mandala.claveFoto(1, 0) to "post-1",
            Mandala.clave(1, 1) to "Leer", MetodoCima.claveJornadas(1, 1) to "2026-10-01",
        )
        val c = Travesia.clavesALiberar(listOf(1L, null), r).toSet()
        assertEquals(setOf(Mandala.clave(1, 0), Mandala.claveHecho(1, 0), MetodoCima.claveJornadas(1, 0), Mandala.claveFoto(1, 0)), c)
    }
}
