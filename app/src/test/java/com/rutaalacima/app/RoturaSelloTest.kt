package com.rutaalacima.app

import com.rutaalacima.app.domain.model.RoturaSello
import com.rutaalacima.app.domain.model.RoturaSello.P
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RoturaSelloTest {
    private fun dentro(p: P, poligono: List<P>): Boolean {
        var dentro = false
        var j = poligono.lastIndex
        for (i in poligono.indices) {
            val a = poligono[i]; val b = poligono[j]
            if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) dentro = !dentro
            j = i
        }
        return dentro
    }

    /** Los trozos cubren todo el sello sin encimarse: cada punto de la cera queda en un solo trozo. */
    @Test fun losTrozosCubrenElSelloSinEncimarse() {
        for (semilla in 0L until 60L) {
            val r = RoturaSello.generar(semilla)
            var fuera = 0
            for (i in -20..20) for (j in -20..20) {
                val p = P(i / 20f * 0.98f, j / 20f * 0.98f)
                if (RoturaSello.radio(p) > 0.98f) continue
                val n = r.fragmentos.count { dentro(p, it.contorno) }
                if (n != 1) fuera++
            }
            // Un punto justo sobre una grieta puede caer en el borde: se tolera un puñado de casos
            assertTrue("semilla $semilla: $fuera puntos mal cubiertos", fuera <= 3)
        }
    }

    @Test fun cadaDiaSeRompeDistintoPeroSiempreIgualElMismoDia() {
        assertEquals(RoturaSello.generar(42), RoturaSello.generar(42))
        assertTrue(RoturaSello.generar(42).fragmentos.first().contorno != RoturaSello.generar(43).fragmentos.first().contorno)
    }

    @Test fun losTrozosSaltanCaenYDesaparecen() {
        val f = RoturaSello.generar(5).fragmentos.first()
        val inicio = RoturaSello.pose(f, 0f)
        val fin = RoturaSello.pose(f, 1f)
        assertEquals(0f, inicio.dx, 1e-6f); assertEquals(1f, inicio.alfa, 1e-6f)
        assertTrue("cae", fin.dy > 1f)
        assertEquals(0f, fin.alfa, 1e-6f)
        assertEquals(0f, RoturaSello.vuelo(RoturaSello.FIN_GRIETAS), 1e-6f)
        assertEquals(1f, RoturaSello.grietas(RoturaSello.FIN_GRIETAS), 1e-6f)
    }

    @Test fun elCrujidoEsCortoYNoSatura() {
        val s = RoturaSello.sonido(44_100)
        assertTrue(s.size in 10_000..20_000)
        val pico = s.maxOf { abs(it.toInt()) }
        assertTrue(pico in 20_000..26_300)
        assertTrue("termina en silencio", abs(s.last().toInt()) < 200)
    }
}
