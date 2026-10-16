package com.rutaalacima.app.domain.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * El cielo real de este momento: dónde están el sol, la luna y los planetas visibles, y qué fase
 * tiene la luna. Es la misma matemática de Rutaalacima Web (web/js/10-cielo.js) para que el cielo
 * del teléfono y el del computador coincidan: SunCalc (Vladimir Agafonkin, BSD) para sol y luna, y
 * los elementos keplerianos aproximados de JPL (Standish, válidos 1800–2050) para los planetas.
 *
 * Sin ubicación exacta: la latitud es la de Colombia y la longitud sale de la zona horaria
 * (no se pide permiso de ubicación). Ángulos en radianes; el azimut se mide desde el sur, positivo al oeste.
 */
object Astronomia {
    data class Posicion(val azimut: Double, val altura: Double) {
        val alturaGrados: Double get() = altura / RAD
    }

    /** [fraccion] iluminada (0..1) y [fase] (0 nueva, 0.25 creciente, 0.5 llena, 0.75 menguante). */
    data class Iluminacion(val fraccion: Double, val fase: Double)

    /** Planetas a simple vista y la altura máxima del sol (en grados) con la que ya se ven. */
    enum class Planeta(val umbralSol: Double) { VENUS(-3.0), JUPITER(-5.0), MARTE(-8.0), SATURNO(-8.0) }

    const val LATITUD = 7.1
    private const val RAD = PI / 180
    private const val DIA_MS = 864e5
    private const val J1970 = 2440588.0
    private const val J2000 = 2451545.0
    private const val E = RAD * 23.4397

    /** Longitud aproximada a partir del desfase de la zona horaria (15° por hora). */
    fun longitudPorZona(desfaseMs: Long): Double = desfaseMs / 3_600_000.0 * 15.0

    private fun dias(ms: Long) = ms / DIA_MS - 0.5 + J1970 - J2000
    private fun ascension(l: Double, b: Double) = atan2(sin(l) * cos(E) - tan(b) * sin(E), cos(l))
    private fun declinacion(l: Double, b: Double) = asin(sin(b) * cos(E) + cos(b) * sin(E) * sin(l))
    private fun azimut(h: Double, phi: Double, dec: Double) = atan2(sin(h), cos(h) * sin(phi) - tan(dec) * cos(phi))
    private fun altura(h: Double, phi: Double, dec: Double) = asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(h))
    private fun sideral(d: Double, lw: Double) = RAD * (280.16 + 360.9856235 * d) - lw

    private class Ecuatorial(val ra: Double, val dec: Double, val dist: Double = 0.0)

    private fun coordSol(d: Double): Ecuatorial {
        val m = RAD * (357.5291 + 0.98560028 * d)
        val l = m + RAD * (1.9148 * sin(m) + 0.02 * sin(2 * m) + 0.0003 * sin(3 * m)) + RAD * 102.9372 + PI
        return Ecuatorial(ascension(l, 0.0), declinacion(l, 0.0))
    }

    private fun coordLuna(d: Double): Ecuatorial {
        val l0 = RAD * (218.316 + 13.176396 * d)
        val m = RAD * (134.963 + 13.064993 * d)
        val f = RAD * (93.272 + 13.22935 * d)
        val l = l0 + RAD * 6.289 * sin(m)
        val b = RAD * 5.128 * sin(f)
        return Ecuatorial(ascension(l, b), declinacion(l, b), 385001 - 20905 * cos(m))
    }

    // a, e, I, L, ϖ, Ω y sus tasas por siglo (JPL, Standish, tabla 1)
    private val ELEMENTOS = mapOf(
        "tierra" to doubleArrayOf(1.00000261, .01671123, -.00001531, 100.46457166, 102.93768193, 0.0, .00000562, -.00004392, -.01294668, 35999.37244981, .32327364, 0.0),
        Planeta.VENUS.name to doubleArrayOf(.72333566, .00677672, 3.39467605, 181.9790995, 131.60246718, 76.67984255, .0000039, -.00004107, -.0007889, 58517.81538729, .00268329, -.27769418),
        Planeta.MARTE.name to doubleArrayOf(1.52371034, .0933941, 1.84969142, -4.55343205, -23.94362959, 49.55953891, .00001847, .00007882, -.00813131, 19140.30268499, .44441088, -.29257343),
        Planeta.JUPITER.name to doubleArrayOf(5.202887, .04838624, 1.30439695, 34.39644051, 14.72847983, 100.47390909, -.00011607, -.00013253, -.00183714, 3034.74612775, .21252668, .20469106),
        Planeta.SATURNO.name to doubleArrayOf(9.53667594, .05386179, 2.48599187, 49.95424423, 92.59887831, 113.66242448, -.0012506, -.00050991, .00193609, 1222.49362201, -.41897216, -.28867794),
    )

    private fun helio(k: String, d: Double): DoubleArray {
        val t = d / 36525
        val el = ELEMENTOS.getValue(k)
        fun v(i: Int) = el[i] + el[i + 6] * t
        val a = v(0); val ex = v(1); val inc = v(2) * RAD; val nodo = v(5) * RAD
        val m = ((v(3) - v(4)) % 360) * RAD
        val om = (v(4) - v(5)) * RAD
        var ea = m + ex * sin(m)
        repeat(6) { ea -= (ea - ex * sin(ea) - m) / (1 - ex * cos(ea)) }
        val xp = a * (cos(ea) - ex)
        val yp = a * sqrt(1 - ex * ex) * sin(ea)
        val co = cos(om); val so = sin(om); val cO = cos(nodo); val sO = sin(nodo); val cI = cos(inc); val sI = sin(inc)
        return doubleArrayOf(
            (co * cO - so * sO * cI) * xp + (-so * cO - co * sO * cI) * yp,
            (co * sO + so * cO * cI) * xp + (-so * sO + co * cO * cI) * yp,
            so * sI * xp + co * sI * yp,
        )
    }

    private fun coordPlaneta(p: Planeta, d: Double): Ecuatorial {
        val q = helio(p.name, d); val t = helio("tierra", d)
        val x = q[0] - t[0]; val y = q[1] - t[1]; val z = q[2] - t[2]
        val l = atan2(y, x); val b = atan2(z, hypot(x, y))
        return Ecuatorial(ascension(l, b), declinacion(l, b))
    }

    private fun posicion(c: (Double) -> Ecuatorial, ms: Long, lat: Double, lng: Double): Posicion {
        val lw = RAD * -lng; val phi = RAD * lat; val d = dias(ms); val e = c(d)
        val h = sideral(d, lw) - e.ra
        return Posicion(azimut(h, phi, e.dec), altura(h, phi, e.dec))
    }

    fun sol(ms: Long, lat: Double = LATITUD, lng: Double): Posicion = posicion(::coordSol, ms, lat, lng)
    fun luna(ms: Long, lat: Double = LATITUD, lng: Double): Posicion = posicion(::coordLuna, ms, lat, lng)
    fun planeta(p: Planeta, ms: Long, lat: Double = LATITUD, lng: Double): Posicion = posicion({ coordPlaneta(p, it) }, ms, lat, lng)

    /** Un astro fijo (estrella, radiante de una lluvia) dado en grados de ascensión recta y declinación. */
    fun fijo(raGrados: Double, decGrados: Double, ms: Long, lat: Double = LATITUD, lng: Double): Posicion =
        posicion({ Ecuatorial(raGrados * RAD, decGrados * RAD) }, ms, lat, lng)

    fun iluminacion(ms: Long): Iluminacion {
        val d = dias(ms); val s = coordSol(d); val m = coordLuna(d); val sd = 149598000.0
        val phi = kotlin.math.acos(sin(s.dec) * sin(m.dec) + cos(s.dec) * cos(m.dec) * cos(s.ra - m.ra))
        val inc = atan2(sd * sin(phi), m.dist - sd * cos(phi))
        val ang = atan2(cos(s.dec) * sin(s.ra - m.ra), sin(s.dec) * cos(m.dec) - cos(s.dec) * sin(m.dec) * cos(s.ra - m.ra))
        return Iluminacion((1 + cos(inc)) / 2, 0.5 + 0.5 * inc * (if (ang < 0) -1 else 1) / PI)
    }

    /** Ancho (relativo al radio) de la elipse del terminador de la luna. */
    fun anchoTerminador(fase: Double): Double = abs(cos(2 * PI * fase))
}
