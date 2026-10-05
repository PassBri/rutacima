package com.rutaalacima.app

import com.rutaalacima.app.domain.model.AccionCoach
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccionCoachTest {
    @Test fun separaLaMetaPropuestaDelTexto() {
        val (t, a) = AccionCoach.separar("Te propongo esto.\n\n[[ACCION {\"tipo\":\"meta_mes\",\"texto\":\"Correr 3 veces por semana\",\"eje\":\"vol\",\"dias\":12}]]")
        assertEquals("Te propongo esto.", t)
        assertEquals(AccionCoach(AccionCoach.Tipo.META_MES, "Correr 3 veces por semana", "VOL", 12), a)
    }

    @Test fun ignoraAccionesRarasYDejaElTexto() {
        assertNull(AccionCoach.separar("Hola [[ACCION {\"tipo\":\"borrar_todo\"}]]").second)
        assertEquals("Hola", AccionCoach.separar("Hola [[ACCION {roto]]").first)
        assertEquals("Sin acción", AccionCoach.separar("Sin acción").first)
        assertEquals(31, AccionCoach.separar("[[ACCION {\"tipo\":\"meta_anio\",\"titulo\":\"Libro\",\"dias\":99}]]").second!!.dias)
    }
}
