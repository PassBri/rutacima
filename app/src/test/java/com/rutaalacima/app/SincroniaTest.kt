package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Sincronia
import com.rutaalacima.app.domain.model.Sincronia.huella
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SincroniaTest {
    private val f = Sincronia::fusionar

    @Test fun canonicoIgnoraOrdenYEspacios() {
        assertEquals(huella("""{"b":1,"a":[1,2]}"""), huella("""{ "a": [1, 2], "b": 1 }"""))
    }

    @Test fun primeraVezSubeTodo() {
        val p = Sincronia.planear(mapOf("perfil/yo" to """{"n":"Laura"}"""), emptyMap(), emptyMap(), fusionar = f)
        assertEquals(setOf("perfil/yo"), p.subir.keys)
        assertTrue(p.aplicar.isEmpty())
    }

    @Test fun sinCambiosNoHaceNada() {
        val doc = """{"marcados":["VOL1"]}"""
        val p = Sincronia.planear(mapOf("checklist/2026-10-04" to doc), mapOf("checklist/2026-10-04" to doc),
            mapOf("checklist/2026-10-04" to huella(doc)), fusionar = f)
        assertTrue(p.vacio)
    }

    @Test fun laWebMarcaUnHabitoYElTelefonoLoRecibe() {
        val antes = """{"marcados":["VOL1"]}"""
        val web = """{"marcados":["MAE2","VOL1"]}"""
        val p = Sincronia.planear(mapOf("checklist/d" to antes), mapOf("checklist/d" to web), mapOf("checklist/d" to huella(antes)), fusionar = f)
        assertEquals(web, p.aplicar["checklist/d"])
        assertTrue(p.subir.isEmpty())
    }

    @Test fun ambosCambianYSeFusiona() {
        val antes = """{"marcados":["VOL1"]}"""
        val tel = """{"marcados":["VOL1","VOZ1"]}"""
        val web = """{"marcados":["MAE2","VOL1"]}"""
        val p = Sincronia.planear(mapOf("checklist/d" to tel), mapOf("checklist/d" to web), mapOf("checklist/d" to huella(antes)), fusionar = f)
        val esperado = """{"marcados":["MAE2","VOL1","VOZ1"]}"""
        assertEquals(esperado, p.aplicar["checklist/d"])
        assertEquals(esperado, p.subir["checklist/d"])
        assertEquals(huella(esperado), p.huellas["checklist/d"])
    }

    @Test fun laWebCambiaElPerfilYElTelefonoLoRecibe() {
        val antes = """{"nombre":"Laura"}"""
        val web = """{"nombre":"Laura M."}"""
        val p = Sincronia.planear(mapOf("perfil/1" to antes), mapOf("perfil/1" to web), mapOf("perfil/1" to huella(antes)), fusionar = f)
        assertEquals(web, p.aplicar["perfil/1"])
        assertTrue(p.subir.isEmpty())
    }

    @Test fun borradoEnLaWebSeBorraEnElTelefono() {
        val doc = """{"titulo":"Meta"}"""
        val p = Sincronia.planear(mapOf("meta_anio/7" to doc), emptyMap(), mapOf("meta_anio/7" to huella(doc)), fusionar = f)
        assertEquals(setOf("meta_anio/7"), p.borrarLocal)
        assertTrue(p.subir.isEmpty() && p.borrar.isEmpty())
    }

    @Test fun nubeVaciaNoBorraNadaEnElTelefono() {
        val doc = """{"titulo":"Meta"}"""
        val huellas = Sincronia.huellasConfiables(emptyMap(), mapOf("meta_anio/7" to huella(doc)))
        val p = Sincronia.planear(mapOf("meta_anio/7" to doc), emptyMap(), huellas, fusionar = f)
        assertTrue(p.borrarLocal.isEmpty())
        assertEquals(setOf("meta_anio/7"), p.subir.keys)
    }

    @Test fun borradoEnElTelefonoSeBorraEnLaNube() {
        val doc = """{"t":"Meta"}"""
        val p = Sincronia.planear(emptyMap(), mapOf("meta_anio/5" to doc), mapOf("meta_anio/5" to huella(doc)), fusionar = f)
        assertEquals(setOf("meta_anio/5"), p.borrar)
        assertEquals(null, p.huellas["meta_anio/5"])
    }

    @Test fun reinstalarRecuperaTodoDesdeLaNube() {
        val p = Sincronia.planear(emptyMap(),
            mapOf("checklist/d" to """{"marcados":"VOL1"}""", "proposito/1" to """{"titulo":"x"}"""), emptyMap(), fusionar = f)
        assertEquals(setOf("checklist/d", "proposito/1"), p.aplicar.keys)
        assertTrue(p.borrar.isEmpty() && p.subir.isEmpty())
    }

    @Test fun fusionConListasComoTexto() {
        assertEquals("""{"fecha":"d","marcados":"MAE2,VOL1,VOZ1"}""",
            Sincronia.fusionar("checklist/d", """{"fecha":"d","marcados":"VOL1,VOZ1"}""", """{"fecha":"d","marcados":"MAE2,VOL1"}"""))
        assertEquals("""{"dias":"1,2,3,10","texto":"Correr"}""",
            Sincronia.fusionar("meta_mes/1", """{"dias":"1,3","texto":"Correr"}""", """{"dias":"3,10,2","texto":"x"}"""))
    }

    @Test fun fueraDeLaVentanaNoSeToca() {
        val p = Sincronia.planear(emptyMap(), emptyMap(), mapOf("checklist/2020-01-01" to "abc"),
            enVentana = { !it.startsWith("checklist/2020") }, fusionar = f)
        assertTrue(p.vacio)
        assertTrue(p.huellas.isEmpty())
    }

    @Test fun codigoDeVinculo() {
        assertEquals("K7P4QX9M", Sincronia.codigoDeVinculo("rutacima://vincular?codigo=K7P4QX9M"))
        assertEquals("K7P4QX9M", Sincronia.codigoDeVinculo(" k7p4-qx9m "))
        assertEquals(null, Sincronia.codigoDeVinculo("K7P4QX9"))       // corto
        assertEquals(null, Sincronia.codigoDeVinculo("K7P4QX90"))      // el 0 no existe en los códigos
        assertEquals(null, Sincronia.codigoDeVinculo("https://ejemplo.com"))
        assertEquals("K7P4-QX9M", Sincronia.codigoLegible("K7P4QX9M"))
    }

    @Test fun fusionMetaMesUneDiasYCumplida() {
        val l = """{"texto":"Correr","dias":[1,3],"cumplida":false}"""
        val r = """{"texto":"cambiado","dias":[3,10,2],"cumplida":true}"""
        assertEquals("""{"cumplida":true,"dias":[1,2,3,10],"texto":"Correr"}""", Sincronia.fusionar("meta_mes/1", l, r))
    }
}
