package com.rutaalacima.app

import com.rutaalacima.app.domain.model.CieloDatos
import com.rutaalacima.app.domain.model.CieloEventos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/**
 * Los eventos del cielo de la app deben ser los mismos de SuRoute Web. Los valores esperados
 * salen de web/js/10-cielo.js con los mismos datos (tools/cielo/generar.js).
 */
class CieloEventosTest {
    private val datos = CieloDatos.leer(File("src/main/assets/cielo/datos.json").readText())
    private fun ms(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test fun losDatosTraenEstrellasConstelacionesYEventos() {
        assertTrue(datos.estrellas.size > 400)
        assertTrue(datos.lineas.isNotEmpty())
        // Los nombres de las constelaciones vienen en los 12 idiomas de la app
        val ori = datos.nombres.getValue("Ori")
        listOf("es", "en", "pt", "fr", "de", "it", "zh", "ja", "ko", "ar", "hi", "ru").forEach { assertTrue(it, ori.textos.containsKey(it)) }
        assertEquals("Orión", ori.en("es"))
    }

    @Test fun lluviaDeLasOrionidasEnSuPico() {
        val e = CieloEventos.eventoDe(datos, ms("2026-10-21T09:00:00Z"))
        assertNotNull(e); assertEquals("lluvia", e!!.tipo); assertEquals("ORI", e.lluvia!!.lluvia.id)
        assertEquals(19.3847, e.lluvia.tasa, 1e-4)
    }

    @Test fun eclipseTotalDeLunaDeJunioDe2029() {
        val total = CieloEventos.eventoDe(datos, ms("2029-06-26T03:22:06Z"))!!.evento!!
        assertEquals("eclipse_luna", total.tipo); assertEquals("total", total.clase)
        assertEquals(1.0, CieloEventos.cubrimiento(total, ms("2029-06-26T03:22:06Z")), 1e-9)
        assertEquals(0.4756, CieloEventos.cubrimiento(total, ms("2029-06-26T02:00:00Z")), 1e-4)
    }

    @Test fun eclipseParcialDeSolDeEnero2028() {
        val e = CieloEventos.eventoDe(datos, ms("2028-01-26T13:00:00Z"))!!.evento!!
        assertEquals("eclipse_sol", e.tipo)
        assertEquals(0.1929, CieloEventos.cubrimiento(e, ms("2028-01-26T13:00:00Z")), 1e-4)
    }

    @Test fun conjuncionMasCercanaYNochesSinEvento() {
        val c = CieloEventos.eventoDe(datos, ms("2026-11-03T00:00:00Z"))!!.evento!!
        assertEquals(listOf("luna", "jupiter"), c.cuerpos)
        assertNull(CieloEventos.eventoDe(datos, ms("2026-10-13T02:30:00Z")))
        assertEquals(2.6098, CieloEventos.lluviaDe(datos, ms("2026-10-13T02:30:00Z"))!!.tasa, 1e-4)
    }

    @Test fun lasCuadrantidasCruzanElAnio() {
        val e = CieloEventos.eventoDe(datos, ms("2027-01-03T12:00:00Z"))!!
        assertEquals("QUA", e.lluvia!!.lluvia.id); assertEquals(72.5165, e.lluvia.tasa, 1e-4)
    }
}
