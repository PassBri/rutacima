package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Avisos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvisosTest {
    private fun conv(id: String, en: Long, noLeidos: Int = 1, solicitud: Boolean = false, mio: Boolean = false) =
        Avisos.Conv(id, "Persona $id", "hola", en, noLeidos, solicitud, mio)

    @Test fun avisaUnaVezPorMensajeNuevoYNoDeLoQueYoEscribi() {
        val convs = listOf(conv("a", 100), conv("b", 200, solicitud = true), conv("c", 300, mio = true), conv("d", 50, noLeidos = 0))
        val r = Avisos.nuevos(convs, emptyMap())
        assertEquals(listOf("m:a", "s:b"), r.map { it.clave })
        val marcados = Avisos.marcar(convs, emptyMap())
        assertTrue("no repite", Avisos.nuevos(convs, marcados).isEmpty())
        // Llega otro mensaje en "a"
        assertEquals(listOf("m:a"), Avisos.nuevos(listOf(conv("a", 150, noLeidos = 2)), marcados).map { it.clave })
    }

    @Test fun noAvisaDeLaConversacionAbiertaYAvisaCoachUnaVez() {
        val r = Avisos.nuevos(listOf(conv("a", 100)), emptyMap(), listOf("x" to "Ana"), setOf(), conversacionAbierta = "a")
        assertEquals(listOf("c:x"), r.map { it.clave })
        assertTrue(Avisos.nuevos(emptyList(), emptyMap(), listOf("x" to "Ana"), setOf("x")).isEmpty())
    }
}
