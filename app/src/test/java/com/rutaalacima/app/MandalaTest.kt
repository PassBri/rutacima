package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Mandala
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MandalaTest {
    @Test fun laCuadriculaTieneUnaCumbre16CampamentosY64Pasos() {
        val porTipo = Mandala.CELDAS.groupingBy { it.tipo }.eachCount()
        assertEquals(1, porTipo[Mandala.Tipo.CUMBRE])
        assertEquals(16, porTipo[Mandala.Tipo.CAMPAMENTO])   // 8 en el centro + 8 en el centro de su bloque
        assertEquals(64, porTipo[Mandala.Tipo.PASO])
        assertEquals(Mandala.Tipo.CUMBRE, Mandala.celda(4, 4).tipo)
    }

    @Test fun cadaCampamentoSeRepiteEnElCentroYEnSuBloque() {
        // Arriba a la izquierda del bloque central (3,3) es el campamento 0, que vive en el bloque 0 (centro 1,1)
        val copia = Mandala.celda(3, 3)
        assertEquals(0, copia.campamento); assertTrue(copia.copia)
        val original = Mandala.celda(1, 1)
        assertEquals(Mandala.Tipo.CAMPAMENTO, original.tipo); assertEquals(0, original.campamento)
        // Abajo a la derecha: campamento 7 en (5,5) y en (7,7)
        assertEquals(7, Mandala.celda(5, 5).campamento); assertEquals(7, Mandala.celda(7, 7).campamento)
        // Un paso del bloque 7 (abajo a la derecha)
        val p = Mandala.celda(8, 8)
        assertEquals(Mandala.Tipo.PASO, p.tipo); assertEquals(7, p.campamento); assertEquals(7, p.paso)
        // Cada campamento tiene exactamente 8 pasos
        (0 until 8).forEach { c -> assertEquals(8, Mandala.CELDAS.count { it.tipo == Mandala.Tipo.PASO && it.campamento == c }) }
    }


    @Test fun progresoYEstados() {
        val r = mapOf(
            Mandala.clave(10, 0) to "Correr", Mandala.claveHecho(10, 0) to "1",
            Mandala.clave(10, 1) to "Leer", Mandala.clave(11, 3) to "Ahorrar",
            Mandala.claveHecho(11, 4) to "1", // marcado pero sin texto: no cuenta
        )
        val p = Mandala.progreso(listOf(10L, 11L), r)
        assertEquals(3, p.escritos); assertEquals(1, p.hechos)
        assertEquals(Mandala.EstadoPaso.HECHO, Mandala.estado(10, 0, r))
        assertEquals(Mandala.EstadoPaso.ESCRITO, Mandala.estado(10, 1, r))
        assertEquals(Mandala.EstadoPaso.VACIO, Mandala.estado(null, 1, r))
    }

    @Test fun repartirPoneLaCumbreAlCentro() {
        val cs = listOf("eje:VOL", "cumbre", "proposito:1", "meta:2")
        val (c, camp) = Mandala.repartir(cs) { it }
        assertEquals("cumbre", c); assertEquals(listOf("eje:VOL", "proposito:1", "meta:2"), camp)
        assertNull(Mandala.repartir(listOf("a")) { it }.first)
    }

    @Test fun completarSoloLlenaLoVacioSinRepetir() {
        val r = Mandala.completar(listOf("Leer", "", "", "", "", "", "", ""), listOf("leer", "Correr", "Meditar"))
        assertEquals(listOf("Leer", "Correr", "Meditar", "", "", "", "", ""), r)
    }
}
