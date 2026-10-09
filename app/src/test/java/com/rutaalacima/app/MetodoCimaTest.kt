package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.domain.model.MetodoCima
import com.rutaalacima.app.domain.model.MetodoCima.Paso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MetodoCimaTest {
    private val hoy = LocalDate.of(2026, 10, 9)

    @Test fun unPasoSeGanaConJornadasEnDiasDistintos() {
        var p = Paso(escrito = true)
        val a1 = MetodoCima.avanzar(p, hoy, 3)
        assertEquals(3, a1.requeridas); assertNull(a1.cumplidoEn); assertTrue(a1.nueva)
        p = p.copy(jornadas = a1.jornadas, requeridas = a1.requeridas)
        // El mismo día no cuenta dos veces
        val repetida = MetodoCima.avanzar(p, hoy, 3)
        assertFalse(repetida.nueva); assertEquals(1, repetida.jornadas.size)
        // La dificultad queda fija desde la primera jornada aunque cambie la del tramo
        val a2 = MetodoCima.avanzar(p, hoy.plusDays(1), 10)
        assertEquals(3, a2.requeridas)
        p = p.copy(jornadas = a2.jornadas)
        val a3 = MetodoCima.avanzar(p, hoy.plusDays(2), 10)
        assertEquals(hoy.plusDays(2), a3.cumplidoEn)
    }

    @Test fun siVasMuyRapidoLaDificultadSube() {
        // 64 pasos en 52 tramos ≈ 1,23 por tramo; ganar 5 por tramo en el primer ciclo sube la dificultad (tope 4×)
        val d = MetodoCima.dificultades(List(8) { 5 }, 64, 8)
        assertEquals(listOf(3, 3, 3, 3), d.take(4))   // dentro del ciclo no cambia
        assertEquals(12, d[4])                         // 3 × 20/4,9 ≈ 12
        assertTrue(d[8] > d[4])
        assertTrue(d.all { it <= MetodoCima.DIFICULTAD_MAX })
    }

    @Test fun siTeFrenasLaDificultadBajaParaQueVuelvas() {
        val d = MetodoCima.dificultades(List(8) { 0 }, 64, 8)
        assertEquals(listOf(3, 3, 3, 3, 1, 1, 1, 1, 1), d)  // nunca baja de 1 jornada
    }

    @Test fun aRitmoJustoLaDificultadSeMantiene() {
        val ritmo = List(16) { if (it % 4 == 0) 2 else 1 }   // ~1,25 por tramo
        val d = MetodoCima.dificultades(ritmo, 64, 16)
        assertTrue(d.all { it in 2..4 })
    }

    @Test fun estadoDelAnioConMilimetros() {
        val r = mutableMapOf<String, String>()
        // Campamento 1: un paso ganado esta semana y otro con 1 de 3 jornadas
        r[Mandala.clave(1, 0)] = "Correr"; r[Mandala.claveHecho(1, 0)] = hoy.toString()
        r[Mandala.clave(1, 1)] = "Leer"; r[MetodoCima.claveJornadas(1, 1)] = hoy.toString(); r[MetodoCima.claveRequeridas(1, 1)] = "3"
        // Un paso marcado con la regla vieja ("1"): cuenta como ganado antes de este año
        r[Mandala.clave(2, 0)] = "Ahorrar"; r[Mandala.claveHecho(2, 0)] = "1"
        val pasos = MetodoCima.pasosDe(listOf(1, 2), r)
        assertEquals(64, pasos.size)
        val e = MetodoCima.calcular(pasos, hoy)
        assertEquals(1, e.ganadosTramo); assertEquals(1, e.ganadosAnio)
        assertEquals(62, e.restantes)
        val esperadoMm = 2 * MetodoCima.MM_POR_PASO + (MetodoCima.MM_POR_PASO / 3.0).toLong()
        assertTrue(kotlin.math.abs(e.mm - esperadoMm) <= 2)
        assertTrue(e.adelanto < 0)   // en octubre con 1 paso, va atrás del ritmo
        assertEquals(27, e.diasParaAjuste)   // el ciclo siguiente empieza en el tramo 44 (5 de noviembre)
    }

}
