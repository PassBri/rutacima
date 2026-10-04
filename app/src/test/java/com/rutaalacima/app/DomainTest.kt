package com.rutaalacima.app

import com.rutaalacima.app.domain.model.ChecklistDiario
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.domain.model.EstadoMeta
import com.rutaalacima.app.domain.model.Fase
import com.rutaalacima.app.domain.model.MatrizDecisiones
import com.rutaalacima.app.domain.model.MatrizDecisiones.Respuesta
import com.rutaalacima.app.domain.model.aniosDelPlan
import com.rutaalacima.app.domain.model.interpretarTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    @Test
    fun modeloSieteFasesPorSeisEjes() {
        assertEquals(7, Fase.entries.size)
        assertEquals(6, Eje.entries.size)
        assertEquals(Fase.ORIENTACION, Fase.DESCENSO.siguiente)
    }

    @Test
    fun checklistTiene18HabitosTresPorEje() {
        assertEquals(ChecklistDiario.TOTAL, ChecklistDiario.habitos.size)
        Eje.entries.forEach { e -> assertEquals(3, ChecklistDiario.habitos.count { it.eje == e }) }
        assertEquals(18, ChecklistDiario.habitos.map { it.id }.toSet().size)
    }

    @Test
    fun interpretacionDelTotal() {
        assertTrue(interpretarTotal(50).startsWith("Estás cerca"))
        assertTrue(interpretarTotal(35).startsWith("Estás en proceso"))
        assertTrue(interpretarTotal(10).startsWith("Estás al inicio"))
    }

    @Test
    fun semaforoDesdePorcentaje() {
        assertEquals(EstadoMeta.NO_INICIADA, EstadoMeta.desdePorcentaje(0))
        assertEquals(EstadoMeta.INICIADA, EstadoMeta.desdePorcentaje(30))
        assertEquals(EstadoMeta.EN_CURSO, EstadoMeta.desdePorcentaje(50))
        assertEquals(EstadoMeta.AVANZADA, EstadoMeta.desdePorcentaje(80))
        assertEquals(EstadoMeta.CUMPLIDA, EstadoMeta.desdePorcentaje(100))
    }

    @Test
    fun matrizDecisionesCuentaSoloSi() {
        assertEquals(2, MatrizDecisiones.puntaje(listOf(Respuesta.SI, Respuesta.NO, Respuesta.SI, Respuesta.NEUTRO, null)))
    }

    @Test
    fun planDeCincoAnios() {
        assertEquals(listOf(2026, 2027, 2028, 2029, 2030), aniosDelPlan(2026))
    }
}
