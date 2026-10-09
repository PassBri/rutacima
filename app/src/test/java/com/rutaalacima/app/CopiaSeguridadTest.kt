package com.rutaalacima.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * La copia de seguridad de Android nunca debe llevarse la sesión de la cuenta ni la clave del sello
 * (ver res/xml/backup_rules.xml y data_extraction_rules.xml). Si alguien renombra uno de esos archivos
 * de preferencias en el código o olvida excluirlo en alguna de las reglas, esta prueba falla.
 */
class CopiaSeguridadTest {
    private val res = File("src/main/res/xml").takeIf { it.isDirectory } ?: File("app/src/main/res/xml")
    private val codigo = File("src/main/java").takeIf { it.isDirectory } ?: File("app/src/main/java")

    /** Lo que nunca sale del teléfono, ni a la nube ni a un teléfono nuevo. */
    private val siempreFuera = listOf("rutacima_sesion", "seguridad", "ruta_web", "avisos_mensajes", "audiolibros_descargas")

    private fun excluidos(archivo: String, seccion: String?): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, archivo))
        val raiz: Element = if (seccion == null) doc.documentElement else doc.getElementsByTagName(seccion).item(0) as Element
        val nodos = raiz.getElementsByTagName("exclude")
        return (0 until nodos.length).map { nodos.item(it) as Element }.map { it.getAttribute("domain") + ":" + it.getAttribute("path") }.toSet()
    }

    private val nube = excluidos("data_extraction_rules.xml", "cloud-backup")
    private val traspaso = excluidos("data_extraction_rules.xml", "device-transfer")
    private val antiguo = excluidos("backup_rules.xml", null)

    @Test fun lasPreferenciasSensiblesNoViajanNunca() {
        siempreFuera.forEach { p ->
            val clave = "sharedpref:$p.xml"
            assertTrue("$p debe quedar fuera de la copia en la nube", clave in nube)
            assertTrue("$p debe quedar fuera del paso a un teléfono nuevo", clave in traspaso)
            assertTrue("$p debe quedar fuera de la copia en Android 11 o anterior", clave in antiguo)
        }
    }

    @Test fun lasReglasDeAndroid11YDe12CoincidenEnLaNube() = assertEquals(antiguo, nube)

    @Test fun losAudiosNoViajanYLasFotosSoloAlTelefonoNuevo() {
        assertTrue("file:audios/" in nube && "file:audios/" in traspaso)
        assertTrue("las fotos no van a la nube (límite de 25 MB)", "file:media/" in nube)
        assertFalse("las fotos sí pasan a un teléfono nuevo", "file:media/" in traspaso)
    }

    @Test fun losNombresExistenEnElCodigo() {
        val fuente = codigo.walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        siempreFuera.forEach { p -> assertTrue("No se encontró la preferencia \"$p\" en el código: ¿cambió de nombre?", "\"$p\"" in fuente) }
    }
}
