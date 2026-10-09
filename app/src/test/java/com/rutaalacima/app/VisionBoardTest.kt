package com.rutaalacima.app

import com.rutaalacima.app.domain.model.VisionBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionBoardTest {
    private val t = VisionBoard.Textos(
        tituloCumbre = "Mi cumbre", tituloProposito = "Propósito · %1\$d", tituloMeta = "Meta %1\$d",
        sugerenciaCumbre = "Una foto tuya en un lugar que te inspire.", sugerenciaRelacionada = "%1\$s Relacionada con: %2\$s",
        ejes = VisionBoard.CODIGOS.associateWith { Triple("Eje $it", "Afirmación $it", "Foto de $it") },
        campamentos = listOf("CON", "CAM").associateWith { Triple("Camp $it", "Afirmación $it", "Foto de $it") },
        cumbreVacia = "Escribe tu cumbre",
    )

    @Test fun proponeLaCumbreYLosOchoCampamentos() {
        val r = VisionBoard.proponer(
            "Abrir mi escuela de montaña",
            listOf(VisionBoard.Proposito(1, "Escuela de montaña propia", "TRA", 2035), VisionBoard.Proposito(2, "Proyecto que une todo", null, 2030)),
            listOf(VisionBoard.Meta(5, "Certificarme como guía", "MAE", 2026)),
            ejeMasDebil = "VOZ", t = t,
        )
        assertEquals(VisionBoard.ORIGENES, r.map { it.origen })
        assertEquals("Abrir mi escuela de montaña", r[0].afirmacion)
        fun de(cod: String) = r.first { it.origen == "campamento:$cod" }
        assertEquals("Escuela de montaña propia", de("TRA").afirmacion)       // su propósito
        assertEquals("Certificarme como guía", de("MAE").afirmacion)          // su meta del año
        assertEquals("Proyecto que une todo", de("CON").afirmacion)           // propósito sin eje = confluencia
        assertEquals("Afirmación VOL", de("VOL").afirmacion)
        assertEquals("Afirmación CAM", de("CAM").afirmacion)
        assertNull(de("CAM").eje); assertEquals("VOL", de("VOL").eje)
        assertTrue(de("TRA").sugerencia.endsWith("Relacionada con: Escuela de montaña propia"))
    }

    @Test fun sinDatosIgualArmaLasNueve() {
        val r = VisionBoard.proponer("", emptyList(), emptyList(), null, t)
        assertEquals(VisionBoard.MAXIMO, r.size)
        assertEquals("Escribe tu cumbre", r[0].afirmacion)
    }

    @Test fun leeLaRespuestaDeLaIa() {
        val texto = """Claro, aquí está:
            [{"titulo":"Mi escuela","afirmacion":"Guío a 12 personas a su primera cumbre","eje":"tra","sugerencia":"Tu grupo en la montaña","busqueda":"grupo montaña"},
             {"titulo":"Guía","afirmacion":"Tengo mi certificado de guía","eje":"MAE","sugerencia":"Tu certificado","busqueda":"certificado"},
             {"afirmacion":"Vivo de lo que amo","eje":"XYZ"}]"""
        val r = VisionBoard.desdeIa(texto)!!
        assertEquals(3, r.size)
        assertEquals("TRA", r[0].eje)
        assertEquals("campamento:TRA", r[0].origen)                // sin "campamento", lo ubica por su eje
        assertNull(r[2].eje)                                       // eje inventado → sin eje
        assertEquals("ia", r[2].origen)
        assertEquals("Vivo de lo que amo", r[2].titulo)
    }

    @Test fun laIaPuedeNombrarElCampamento() {
        val r = VisionBoard.desdeIa("""[{"campamento":"CUMBRE","afirmacion":"Vivo en la montaña"},{"campamento":"cam","afirmacion":"Mi cordada me sostiene"},{"campamento":"CON","afirmacion":"Mi podcast une mis ejes"}]""")!!
        assertEquals(listOf("cumbre", "campamento:CAM", "campamento:CON"), r.map { it.origen })
    }

    @Test fun respuestaInvalidaDevuelveNull() {
        assertNull(VisionBoard.desdeIa("No puedo ayudarte con eso"))
        assertNull(VisionBoard.desdeIa("""[{"titulo":"solo una"}]"""))
    }

    @Test fun palabrasParaBuscar() {
        assertEquals("abrir escuela montaña", VisionBoard.palabras("Abrir mi escuela de montaña en 2030"))
        assertTrue(VisionBoard.urlIdeas("grupo montaña").startsWith("https://www.pexels.com/search/grupo%20monta"))
    }
}
