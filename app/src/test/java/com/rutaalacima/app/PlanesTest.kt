package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Planes
import com.rutaalacima.app.domain.model.Planes.Beneficio
import com.rutaalacima.app.domain.model.Planes.Plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanesTest {
    @Test fun loEsencialNuncaSeCobra() {
        // Promesa de la app y de la política de privacidad: el método, la comunidad, la web (iPhone) y tus datos
        listOf(Beneficio.METODO, Beneficio.BRUJULA, Beneficio.COMUNIDAD, Beneficio.WEB, Beneficio.DATOS).forEach {
            assertTrue("$it debe ser esencial", it.esencial)
            assertTrue("$it debe estar en el plan gratuito", Planes.incluido(it, Plan.GRATIS))
        }
    }

    @Test fun cumbreLoIncluyeTodo() = Beneficio.entries.forEach { assertTrue(Planes.incluido(it, Plan.CUMBRE)) }

    @Test fun losExtrasSonSoloDeCumbre() {
        assertFalse(Planes.incluido(Beneficio.AUDIOLIBROS, Plan.GRATIS))
        assertFalse(Planes.incluido(Beneficio.CIERRE_GUIADO, Plan.GRATIS))
        assertTrue("el coach sigue en el gratuito, con menos mensajes", Planes.incluido(Beneficio.COACH, Plan.GRATIS))
    }

    @Test fun leeLaRespuestaDelServidor() {
        val e = Planes.leer("cumbre", "2027-01-15T10:00:00+00:00", "piloto", false)
        assertEquals(Plan.CUMBRE, e.plan); assertEquals(Planes.Origen.PILOTO, e.origen); assertTrue(e.esCumbre)
        val g = Planes.leer("gratis", null, null, true)
        assertEquals(Plan.GRATIS, g.plan); assertTrue(g.avisame); assertNull(g.hasta)
    }

    @Test fun sinFechaValidaEsGratis() {
        assertEquals(Plan.GRATIS, Planes.leer("cumbre", null, "regalo", false).plan)
        assertEquals(Plan.GRATIS, Planes.leer("cumbre", "no-es-fecha", "regalo", false).plan)
        assertEquals(Plan.GRATIS, Planes.leer(null, null, null, false).plan)
        assertNull(Planes.leer("cumbre", "2027-01-15T10:00:00+00:00", "desconocido", false).origen)
    }
}
