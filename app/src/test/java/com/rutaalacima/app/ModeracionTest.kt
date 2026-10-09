package com.rutaalacima.app

import com.rutaalacima.app.data.social.ModeracionRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeracionTest {
    // Respuesta real de moderacion_pendientes() en la base de prueba
    private val respuesta = """[
      {"tipo":"post","objetivo":"b7","reportes":3,"motivos":["ofensivo","spam"],"primero":"2026-10-09T18:00:00+00:00","ultimo":"2026-10-09T18:05:00+00:00",
       "autor":{"id":"a2","usuario":"autor7","nombre":"","avatar":"","suspendido_hasta":null,"sanciones":2},
       "texto":"post dudoso","imagen":"https://x/y.jpg","oculto":true,"mensajes":null},
      {"tipo":"conversacion","objetivo":"c1","reportes":1,"motivos":[],"ultimo":"2026-10-09T18:06:00+00:00",
       "autor":{"id":"a3","usuario":"otro","nombre":"Otro","avatar":"","suspendido_hasta":"infinity","sanciones":0},
       "texto":"","imagen":"","oculto":false,"mensajes":[{"texto":"hola","creado":"x"},{"texto":"","creado":"y"}]},
      {"tipo":"desconocido","objetivo":"z"}
    ]"""

    private val casos = Json.parseToJsonElement(respuesta).jsonArray.mapNotNull { ModeracionRepository.leerCaso(it) }

    @Test fun ignoraTiposDesconocidos() = assertEquals(2, casos.size)

    @Test fun leePublicacionReportada() {
        val p = casos[0]
        assertEquals(ModeracionRepository.Tipo.POST, p.tipo)
        assertEquals(3, p.reportes)
        assertEquals(listOf("ofensivo", "spam"), p.motivos)
        assertTrue(p.oculto)
        assertEquals("autor7", p.autor?.usuario)
        assertEquals(2, p.autor?.sanciones)
        assertNull(p.autor?.suspendidoHasta)
        assertTrue(p.mensajes.isEmpty())
    }

    @Test fun leeConversacionConMensajesYSuspension() {
        val c = casos[1]
        assertEquals(ModeracionRepository.Tipo.CONVERSACION, c.tipo)
        assertEquals(listOf("hola"), c.mensajes) // los vacíos no se muestran
        assertEquals("infinity", c.autor?.suspendidoHasta)
        assertFalse(c.oculto)
        assertTrue(c.motivos.isEmpty())
    }
}
