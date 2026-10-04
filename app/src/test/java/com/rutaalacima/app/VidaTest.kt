package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Vida
import com.rutaalacima.app.domain.model.Vida.EstadoMes
import com.rutaalacima.app.domain.model.aniosDelPlan
import org.junit.Assert.assertEquals
import org.junit.Test

/** Calendario de vida y horizontes de 5 a 20 años. */
class VidaTest {
    @Test fun mesesVividos() {
        assertEquals(0, Vida.mesesVividos(2026, 10, 2026, 10))
        assertEquals(13, Vida.mesesVividos(1987, 3, 1988, 4))
        assertEquals(0, Vida.mesesVividos(2030, 1, 2026, 10))
    }

    @Test fun estadoDeCadaMes() {
        assertEquals(EstadoMes.ANTES_DE_NACER, Vida.estado(1987, 2, 1987, 3, 2026, 10))
        assertEquals(EstadoMes.VIVIDO, Vida.estado(1987, 3, 1987, 3, 2026, 10))
        assertEquals(EstadoMes.ACTUAL, Vida.estado(2026, 10, 1987, 3, 2026, 10))
        assertEquals(EstadoMes.POR_VIVIR, Vida.estado(2026, 11, 1987, 3, 2026, 10))
    }

    @Test fun aniosDeVidaYEsperanza() {
        assertEquals(78, Vida.anios(1987, 77).size)
        assertEquals(77, Vida.esperanzaPais("co"))
        assertEquals(Vida.ESPERANZA_MUNDIAL, Vida.esperanzaPais("??"))
    }

    @Test fun planSeExtiendeAlHorizonteMasLargo() {
        assertEquals(5, aniosDelPlan(2026, 5).size)
        assertEquals(20, aniosDelPlan(2026, 20).size)
        assertEquals(2045, aniosDelPlan(2026, 20).last())
        assertEquals(5, aniosDelPlan(2026, 2).size)
    }

    @Test fun recordatorioDelDia() {
        val r = com.rutaalacima.app.domain.model.RecordatorioVida.calcular(1987, 3, 120, java.time.LocalDate.of(2026, 10, 4))
        assertEquals(39, r.edad)
        assertEquals(java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.of(1987, 3, 1), java.time.LocalDate.of(2026, 10, 4)) + 1, r.diaDeVida)
        assertEquals(r.diasTotales - r.diaDeVida, r.diasRestantes)
        assert(r.frase in 0 until com.rutaalacima.app.domain.model.RecordatorioVida.TOTAL_FRASES)
    }
}
