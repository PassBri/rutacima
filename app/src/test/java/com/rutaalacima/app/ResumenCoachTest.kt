package com.rutaalacima.app

import com.rutaalacima.app.domain.model.ResumenCoach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ResumenCoachTest {
    private val hoy = LocalDate.of(2026, 10, 5)
    private fun d(s: String) = Json.parseToJsonElement(s).jsonObject

    @Test fun armaElResumenConLaCascada() {
        val docs = mapOf(
            "perfil/1" to d("""{"id":1,"nombre":"Beto","cumbreFrase":"Correr en montaña"}"""),
            "proposito/1" to d("""{"id":1,"titulo":"Ultramaratón","progreso":0}"""),
            "meta_anio/10" to d("""{"id":10,"anio":2026,"titulo":"Media maratón","propositoId":1,"avance":0}"""),
            "meta_mes/20" to d("""{"id":20,"anio":2026,"mes":10,"texto":"Trotar","dias":"1,2,3,4,5","objetivoDias":10,"metaAnualId":10}"""),
            "meta_mes/21" to d("""{"id":21,"anio":2026,"mes":9,"texto":"Viejo","dias":"","objetivoDias":20,"metaAnualId":10,"cumplida":true}"""),
            "checklist/2026-10-05" to d("""{"fecha":"2026-10-05","marcados":"VOL1,MAE1,TRA2"}"""),
            "checklist/2026-10-03" to d("""{"fecha":"2026-10-03","marcados":"VOL1"}"""),
            "checklist/2026-09-01" to d("""{"fecha":"2026-09-01","marcados":"VOL1,VOL2"}"""),
            "ejes/1" to d("""{"id":1,"fecha":100,"voluntad":5,"maestria":5,"voz":5,"valor":5,"evolucion":5,"trascendencia":5}"""),
            "ejes/2" to d("""{"id":2,"fecha":200,"voluntad":8,"maestria":7,"voz":4,"valor":6,"evolucion":7,"trascendencia":9}"""),
            "vision/1" to d("""{"id":1,"publicacionId":"abc"}"""),
            "vision/2" to d("""{"id":2}"""),
        )
        val r = ResumenCoach.desde(docs, hoy)
        assertEquals("Beto", r.nombre)
        assertEquals(listOf(ResumenCoach.Item("Trotar", 50)), r.metasMes)            // 5 de 10 días
        assertEquals(75, r.metasAnio.single().avance)                                  // (50 % + 100 %) / 2
        assertEquals(75, r.propositos.single().avance)                                 // sube por la cascada
        assertEquals(listOf(0, 0, 0, 0, 1, 0, 3), r.habitos7)                          // últimos 7 días
        assertEquals(listOf(8, 7, 4, 6, 7, 9), r.ejes)                                 // la evaluación más reciente
        assertEquals(1, r.visionConFoto); assertEquals(2, r.visionTotal)
    }

    @Test fun sinDatosNoFalla() {
        val r = ResumenCoach.desde(emptyMap(), hoy)
        assertEquals("", r.nombre); assertNull(r.ejes); assertEquals(7, r.habitos7.size)
    }
}
