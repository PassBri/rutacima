package com.rutaalacima.app

import com.rutaalacima.app.ui.components.Cordillera
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** La cordillera de la app debe ser la misma de la web (web/js/16-arte.js) para la misma semilla. */
class ArteTest {
    @Test fun laSemillaEsLaMismaDeLaWeb() {
        assertEquals(1657037486L, Cordillera.semillaDe("descubre").toUInt().toLong())
    }

    @Test fun laCrestaEsLaMismaDeLaWeb() {
        val (capas, astro) = Cordillera.crestas("descubre")
        val esperadas = doubleArrayOf(0.511754, 0.510914, 0.518332, 0.514953)
        esperadas.forEachIndexed { i, v -> assertEquals(v, capas[0][i], 1e-6) }
        assertEquals(0.858259, capas[4][64], 1e-6)
        assertEquals(0.571200, astro[0], 1e-6)
        assertEquals(0.954146, astro[1], 1e-6)
    }

    @Test fun losColoresDeLaCapaSonLosDeLaWeb() {
        assertArrayEquals(intArrayOf(147, 165, 179), Cordillera.colorCapa(Cordillera.MOMENTOS[1], 2))
    }
}
