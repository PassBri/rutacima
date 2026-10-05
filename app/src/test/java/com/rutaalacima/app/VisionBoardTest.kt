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
    )

    @Test fun proponeCumbrePropositosMetasYEjesQueFaltan() {
        val r = VisionBoard.proponer(
            "Abrir mi escuela de montaña",
            listOf(VisionBoard.Proposito(1, "Escuela de montaña propia", "TRA", 2035)),
            listOf(VisionBoard.Meta(5, "Certificarme como guía", "MAE", 2026)),
            ejeMasDebil = "VOZ", t = t,
        )
        assertEquals("cumbre", r[0].origen)
        assertEquals("Propósito · 2035", r[1].titulo)
        assertEquals("meta:5", r[2].origen)
        assertEquals("eje:VOZ", r[3].origen)                      // primero el eje más débil
        assertEquals(setOf("VOL", "VOZ", "VAL", "EVO"), r.drop(3).mapNotNull { it.eje }.toSet())
        assertTrue(r[1].sugerencia.endsWith("Relacionada con: Escuela de montaña propia"))
        assertTrue(r.size <= VisionBoard.MAXIMO)
    }

    @Test fun sinDatosProponeLosSeisEjes() {
        val r = VisionBoard.proponer("", emptyList(), emptyList(), null, t)
        assertEquals(VisionBoard.CODIGOS, r.map { it.eje })
    }

    @Test fun nuncaPasaDeNueve() {
        val ps = (1..5).map { VisionBoard.Proposito(it.toLong(), "P$it", null, 2030) }
        val ms = (1..5).map { VisionBoard.Meta(it.toLong(), "M$it", null, 2026) }
        assertEquals(VisionBoard.MAXIMO, VisionBoard.proponer("Cumbre", ps, ms, null, t).size)
    }

    @Test fun leeLaRespuestaDeLaIa() {
        val texto = """Claro, aquí está:
            [{"titulo":"Mi escuela","afirmacion":"Guío a 12 personas a su primera cumbre","eje":"tra","sugerencia":"Tu grupo en la montaña","busqueda":"grupo montaña"},
             {"titulo":"Guía","afirmacion":"Tengo mi certificado de guía","eje":"MAE","sugerencia":"Tu certificado","busqueda":"certificado"},
             {"afirmacion":"Vivo de lo que amo","eje":"XYZ"}]"""
        val r = VisionBoard.desdeIa(texto)!!
        assertEquals(3, r.size)
        assertEquals("TRA", r[0].eje)
        assertNull(r[2].eje)                                       // eje inventado → sin eje
        assertEquals("Vivo de lo que amo", r[2].titulo)
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
