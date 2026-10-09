package com.rutaalacima.app

import com.rutaalacima.app.domain.model.BrujulaCompartida
import com.rutaalacima.app.domain.model.BrujulaCompartida.Campamento
import com.rutaalacima.app.domain.model.BrujulaCompartida.Instantanea
import com.rutaalacima.app.domain.model.BrujulaCompartida.Paso
import com.rutaalacima.app.domain.model.Mandala
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrujulaCompartidaTest {
    private val i = Instantanea(
        cumbre = "Abrir mi escuela de montaña", fotoCumbre = "https://x/c.jpg", mm = 276500, ganados = 2, escritos = 3,
        camps = listOf(Campamento("VOL", "Correr una media maratón", "", listOf(Paso("Correr 5 km", "H", "https://x/e.jpg"), Paso("Plan de 12 semanas", "E"), Paso("", "H")))),
    )

    @Test fun idaYVuelta() {
        val texto = BrujulaCompartida.codificar(i)
        assertEquals(i, BrujulaCompartida.leer(texto))
        assertNull(BrujulaCompartida.leer("no es json"))
        assertNull(BrujulaCompartida.leer(""))
    }

    @Test fun seDibujaConLasMismasClaves() {
        val (r, fotos) = BrujulaCompartida.comoRespuestas(i)
        val id = Mandala.CAMPAMENTOS_FIJOS.indexOf("VOL") + 1L
        assertEquals("Correr 5 km", r[Mandala.clave(id, 0)])
        assertEquals(Mandala.EstadoPaso.HECHO, Mandala.estado(id, 0, r))
        assertEquals(Mandala.EstadoPaso.ESCRITO, Mandala.estado(id, 1, r))
        assertEquals(Mandala.EstadoPaso.HECHO, Mandala.estado(id, 2, r))       // ganado aunque el texto no se compartió
        assertEquals("https://x/e.jpg", fotos[r[Mandala.claveFoto(id, 0)]])
    }
}
