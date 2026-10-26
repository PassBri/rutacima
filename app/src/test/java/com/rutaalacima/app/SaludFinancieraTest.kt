package com.rutaalacima.app

import com.rutaalacima.app.domain.model.Cuentas
import com.rutaalacima.app.domain.model.Cuentas.Mov
import com.rutaalacima.app.domain.model.Deudas
import com.rutaalacima.app.domain.model.Patrimonio
import com.rutaalacima.app.domain.model.Salud
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaludFinancieraTest {
    /** Seis meses como la hoja de ejemplo de Brian. */
    private val dias = (1..6).associate { k ->
        "2026-%02d-05".format(k) to listOf(
            Mov("i$k", "ing", 15_312_993.0, "trabajo"), Mov("p$k", "ing", 87_000.0, "pasivo"),
            Mov("v$k", "gas", 9_163_107.0, "vivienda"), Mov("a$k", "gas", 2_168_300.0, "entretenimiento"),
            Mov("s$k", "aho", 483_290.0, "emergencia"),
        )
    }
    private val datos = Deudas.Datos("bola", 500_000.0, listOf(
        Deudas.Deuda("d1", "Tarjeta", 8_000_000.0, 28.0, 400_000.0), Deudas.Deuda("d2", "Hipoteca", 239_131_130.0, 12.0, 4_592_653.0, "buena")))

    @Test fun indicadoresComoLaHoja() {
        val r = Salud.de(dias, 2026, Deudas.hoy(datos, emptyMap(), "2026-12-31"), listOf(Patrimonio.Activo("a", "Casa", "bienes", 350_000_000.0)), anioActual = 2026)
        fun v(k: String) = r.indicadores.first { it.clave == k }.valor
        assertEquals(1.30, v("liquidez"), 0.01)
        assertEquals(0.0314, v("ahorro"), 0.0005)
        assertEquals(0.324, v("dti"), 0.001)
        assertEquals(0.2328, v("flujo"), 0.0005)
        assertEquals(247_131_130.0, r.pasivos, 0.5)
        assertTrue(r.isf!! in 40..70)
        assertEquals(Salud.Estado.OK, r.indicadores.first { it.clave == "buenas" }.estado)
    }

    @Test fun planDeDeudas() {
        val p = Deudas.plan(Deudas.hoy(datos, emptyMap(), "2026-12-31"), "bola", 500_000.0)
        assertTrue(p.meses!! in 50..80)
        assertTrue(p.fin.getValue("d1")!! < p.fin.getValue("d2")!!)
        // Sin extra y mínimo menor que el interés: nunca se paga
        val nunca = Deudas.plan(listOf(Deudas.Hoy(Deudas.Deuda("x", "X", 1_000_000.0, 60.0, 10_000.0), 0.0)), "avalancha", 0.0)
        assertNull(nunca.meses)
    }

    @Test fun losPagosBajanElSaldo() {
        val h = Deudas.hoy(datos, mapOf("2026-05-01" to listOf(Mov("m", "gas", 400_000.0, "deudas", d = "d1"))), "2026-12-31")
        assertEquals(7_600_000.0, h.first { it.deuda.id == "d1" }.actual, 0.0)
    }

    @Test fun losPagosViejosNoBajanUnSaldoNuevoYDanLaCuota() {
        // El saldo se anotó el 26 de octubre: los pagos de antes ya estaban en el extracto
        val d = Deudas.Datos(lista = listOf(Deudas.Deuda("b", "LIBRANZA BBVA", 82_885_315.0, desde = "2026-10-26", desdeT = 5)))
        val dias = (1..10).associate { m -> "2026-%02d-01".format(m) to listOf(Mov("fx-f1", "gas", 1_508_279.0, "deudas", d = "b", fx = "f1")) } +
            mapOf("2026-10-27" to listOf(Mov("m9", "gas", 100_000.0, "deudas", d = "b")))
        val h = Deudas.hoy(d, dias, "2026-10-31").single()
        assertEquals(82_785_315.0, h.actual, 0.0)
        assertEquals(1_608_279.0, h.deuda.minimo, 0.0)   // el último mes con pagos
        assertTrue(h.cuotaAuto)
    }

    @Test fun categoriasPropias() {
        val p = mapOf("gas" to listOf(Cuentas.Propia("u_mascotas", "Mascotas", "verde")))
        assertEquals("u_mascotas", Cuentas.codigoPropia("Mascotas"))
        assertEquals("u_gimnasio_y_spa", Cuentas.codigoPropia("Gimnasio y Spá"))
        assertEquals(Cuentas.Mochila.VERDE, Cuentas.mochila(Mov("1", "gas", 1.0, "u_mascotas"), p))
        assertEquals(p, Cuentas.leerPropias(Cuentas.escribirPropias(p)))
        // Los recibos y la deuda viajan en el JSON igual que en la web
        val x = Mov("1", "gas", 5.0, "deudas", "", listOf(Cuentas.Recibo("r1", "f.pdf", "application/pdf", "u/r1.pdf")), "d1")
        assertEquals(listOf(x), Cuentas.leer(Cuentas.escribir(listOf(x))))
    }
}
