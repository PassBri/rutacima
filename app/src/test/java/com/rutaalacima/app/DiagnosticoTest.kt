package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Diagnostico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticoTest {
    private fun error(mensaje: String, causa: Throwable? = null): Throwable =
        IllegalStateException(mensaje, causa).apply {
            stackTrace = arrayOf(
                StackTraceElement("android.view.View", "draw", "View.java", 10),
                StackTraceElement("com.rutaalacima.app.ui.hoy.HoyScreen", "pintar", "HoyScreen.kt", 42),
            )
        }

    @Test fun laFirmaApuntaALaPrimeraLineaDeLaApp() =
        assertEquals("IllegalStateException en HoyScreen.pintar(HoyScreen.kt:42)", Diagnostico.firma(error("x")))

    @Test fun laFirmaUsaLaCausaRaiz() {
        val raiz = NullPointerException("nulo").apply { stackTrace = arrayOf(StackTraceElement("com.rutaalacima.app.data.X", "leer", "X.kt", 7)) }
        assertEquals("NullPointerException en X.leer(X.kt:7)", Diagnostico.firma(error("envoltura", raiz)))
    }

    @Test fun nuncaIncluyeLosMensajes() {
        val privado = "Mi meta secreta: dejar a mi pareja"
        val t = error(privado, RuntimeException(privado))
        assertFalse(Diagnostico.rastro(t).contains("secreta"))
        assertFalse(Diagnostico.firma(t).contains("secreta"))
        assertTrue(Diagnostico.rastro(t).contains("Causado por: java.lang.RuntimeException"))
    }

    @Test fun respetaLosLimites() {
        val largo = RuntimeException().apply { stackTrace = Array(5000) { StackTraceElement("com.rutaalacima.A$it", "m", "A.kt", it) } }
        assertTrue(Diagnostico.rastro(largo, lineasPorCausa = 5000).length <= Diagnostico.MAX_RASTRO)
        assertTrue(Diagnostico.firma(largo).length <= Diagnostico.MAX_FIRMA)
    }

    @Test fun soportaCausasCirculares() {
        val a = RuntimeException("a"); val b = RuntimeException("b", a); a.initCause(b)
        assertTrue(Diagnostico.rastro(a).isNotEmpty()); assertTrue(Diagnostico.firma(a).isNotEmpty())
    }
}
