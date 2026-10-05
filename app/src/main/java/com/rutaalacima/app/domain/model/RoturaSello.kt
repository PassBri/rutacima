package com.rutaalacima.app.domain.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Cómo se rompe el sello de cera de la frase del día, sin Android (se prueba con JUnit y la web
 * usa la misma receta en JavaScript).
 *
 * Todo está en unidades del sello: el centro es (0, 0) y el borde está a radio 1. El sello se
 * agrieta desde un punto de impacto cerca del centro: unas grietas lo parten en [piezas] trozos
 * (más un par de grietas finas que no lo parten), los trozos saltan hacia afuera, giran, se
 * voltean un poco y caen con gravedad, y suelta migas de cera. Con la misma [semilla] se rompe
 * igual; la app usa el día del año, así cada día el sello se rompe distinto.
 */
object RoturaSello {
    data class P(val x: Float, val y: Float)

    data class Fragmento(
        /** Contorno del trozo (se usa para recortar la imagen del sello; se sale del borde a propósito). */
        val contorno: List<P>,
        /** El trozo de cera tal como es (recortado al círculo del sello): para su sombra y su canto. */
        val cara: List<P>,
        /** Los dos filos por donde se partió (para marcar la fractura). */
        val filos: List<List<P>>,
        /** Hacia dónde sale disparado (vector unitario). */
        val direccion: P,
        val velocidad: Float,
        /** Grados que gira en todo el vuelo. */
        val giro: Float,
        /** Cuánto se voltea (0 = nada, 1 = media vuelta). */
        val volteo: Float,
        /** Punto sobre el que gira (centro aproximado del trozo). */
        val pivote: P,
    )

    data class Miga(val origen: P, val direccion: P, val velocidad: Float, val tamano: Float, val giro: Float)

    data class Rotura(val impacto: P, val grietas: List<List<P>>, val finas: List<List<P>>, val fragmentos: List<Fragmento>, val migas: List<Miga>)

    /** Posición de un trozo o una miga en un instante del vuelo. */
    data class Pose(val dx: Float, val dy: Float, val grados: Float, val escalaX: Float, val alfa: Float, val altura: Float)

    /** Primera parte de la animación: aparecen las grietas y el sello tiembla. */
    const val FIN_GRIETAS = 0.2f

    /** Gravedad en radios del sello: los trozos caen unos dos radios mientras vuelan. */
    private const val GRAVEDAD = 2.2f

    fun generar(semilla: Long, piezas: Int = 6, migas: Int = 18): Rotura {
        val r = java.util.Random(semilla)
        fun azar(a: Float, b: Float) = a + r.nextFloat() * (b - a)
        val impacto = P(azar(-0.18f, 0.18f), azar(-0.18f, 0.18f))

        // Ángulos de las grietas principales: repartidos, con variación (trozos de tamaños distintos)
        val base = azar(0f, (2 * PI).toFloat())
        val angulos = (0 until piezas).map { i -> base + (2 * PI / piezas).toFloat() * (i + azar(-0.32f, 0.32f)) }.sorted()

        // Cada grieta va del impacto hasta fuera del borde, quebrándose en zigzag
        fun grieta(angulo: Float, largo: Float, desde: P, quiebres: Int): List<P> {
            val pts = mutableListOf(desde)
            for (k in 1..quiebres) {
                val t = k.toFloat() / quiebres
                val a = angulo + if (k < quiebres) azar(-0.16f, 0.16f) else 0f
                pts += P(desde.x + cos(a) * largo * t, desde.y + sin(a) * largo * t)
            }
            return pts
        }
        val grietas = angulos.map { grieta(it, 1.7f, impacto, 5) }

        // Grietas finas: arrancan de una grieta principal y se pierden antes del borde
        val finas = (0 until 4).map {
            val g = grietas[r.nextInt(grietas.size)]
            val desde = g[1 + r.nextInt(2)]
            grieta(atan2(desde.y - impacto.y, desde.x - impacto.x) + azar(-0.9f, 0.9f), azar(0.18f, 0.38f), desde, 3)
        }

        val fragmentos = angulos.indices.map { i ->
            val a1 = angulos[i]
            val a2 = if (i + 1 < angulos.size) angulos[i + 1] else angulos[0] + (2 * PI).toFloat()
            val arco = (0..6).map { k -> val a = a1 + (a2 - a1) * k / 6f; P(impacto.x + cos(a) * 1.7f, impacto.y + sin(a) * 1.7f) }
            val siguiente = grietas[(i + 1) % grietas.size]
            val contorno = grietas[i] + arco + siguiente.reversed()
            val arcoCara = (0..14).map { k -> val a = a1 + (a2 - a1) * k / 14f; P(impacto.x + cos(a) * 1.7f, impacto.y + sin(a) * 1.7f) }
            val cara = (grietas[i] + arcoCara + siguiente.reversed()).map { recortar(it, 0.95f) }
            val filos = listOf(grietas[i], siguiente).map { g -> g.map { recortar(it, 0.95f) } }
            val medio = (a1 + a2) / 2
            val dir = P(cos(medio), sin(medio))
            // El trozo es más grande cuanto más ancho su ángulo: los grandes salen más lentos
            val ancho = (a2 - a1) / (2 * PI / piezas).toFloat()
            Fragmento(
                contorno = contorno, cara = cara, filos = filos, direccion = dir,
                velocidad = azar(0.75f, 1.15f) / ancho.coerceIn(0.6f, 1.6f),
                giro = azar(70f, 220f) * (if (r.nextBoolean()) 1 else -1),
                volteo = azar(0.15f, 0.55f),
                pivote = P(impacto.x + dir.x * 0.55f, impacto.y + dir.y * 0.55f),
            )
        }

        val listaMigas = (0 until migas).map {
            val a = azar(0f, (2 * PI).toFloat())
            val d = azar(0.05f, 0.75f)
            Miga(
                origen = P(impacto.x + cos(a) * d, impacto.y + sin(a) * d),
                direccion = P(cos(a), sin(a) - 0.35f),
                velocidad = azar(0.6f, 1.6f), tamano = azar(0.025f, 0.07f), giro = azar(-360f, 360f),
            )
        }
        return Rotura(impacto, grietas, finas, fragmentos, listaMigas)
    }

    /** Cuánto de las grietas se ve en el instante [t] (0 a 1 de toda la animación). */
    fun grietas(t: Float): Float = (t / FIN_GRIETAS).coerceIn(0f, 1f).let { 1f - (1f - it) * (1f - it) }

    /** Vuelo de 0 a 1 (empieza al terminar de agrietarse). */
    fun vuelo(t: Float): Float = ((t - FIN_GRIETAS) / (1f - FIN_GRIETAS)).coerceIn(0f, 1f)

    /** Temblor del sello mientras se agrieta, en radios. */
    fun temblor(t: Float): P {
        if (t <= 0f || t >= FIN_GRIETAS) return P(0f, 0f)
        val f = 1f - t / FIN_GRIETAS
        return P(sin(t * 210f) * 0.025f * f, cos(t * 170f) * 0.018f * f)
    }

    fun pose(f: Fragmento, u: Float): Pose {
        val salto = f.velocidad * (1f - (1f - u) * (1f - u)) * 0.8f
        return Pose(
            dx = f.direccion.x * salto,
            dy = f.direccion.y * salto + GRAVEDAD * u * u,
            grados = f.giro * u,
            escalaX = cos(f.volteo * PI.toFloat() * u),
            alfa = 1f - suave(0.62f, 1f, u),
            altura = sin(PI.toFloat() * u.coerceAtMost(0.5f)),   // se levanta del papel y no vuelve a bajar
        )
    }

    fun pose(m: Miga, u: Float): Pose {
        val salto = m.velocidad * (1f - (1f - u) * (1f - u)) * 0.9f
        return Pose(m.direccion.x * salto, m.direccion.y * salto + GRAVEDAD * 1.2f * u * u, m.giro * u, 1f, 1f - suave(0.5f, 0.95f, u), 0f)
    }

    private fun suave(a: Float, b: Float, x: Float): Float { val t = ((x - a) / (b - a)).coerceIn(0f, 1f); return t * t * (3 - 2 * t) }
    private fun atan2(y: Float, x: Float) = kotlin.math.atan2(y, x)

    /**
     * Crujido de la cera al partirse, como muestras de audio (16 bits, mono): un chasquido seco,
     * un golpe sordo de la cera y unos crujidos pequeños mientras se separan los trozos.
     */
    fun sonido(muestreo: Int = 44_100, semilla: Long = 7): ShortArray {
        val r = java.util.Random(semilla)
        val n = (muestreo * 0.32).toInt()
        val s = FloatArray(n)
        fun chasquido(inicio: Float, fuerza: Float, caida: Float) {
            val i0 = (inicio * muestreo).toInt()
            var previo = 0f
            for (i in i0 until n) {
                val t = (i - i0).toFloat() / muestreo
                val env = exp(-t / caida)
                if (env < 0.001f) break
                val ruido = r.nextFloat() * 2 - 1
                val agudo = ruido - previo * 0.85f       // filtro paso alto: suena seco, no sordo
                previo = ruido
                s[i] += agudo * env * fuerza
            }
        }
        // Golpe sordo de la cera (cuerpo del sonido)
        for (i in 0 until n) {
            val t = i.toFloat() / muestreo
            s[i] += sin(2 * PI.toFloat() * 165f * t) * exp(-t / 0.028f) * 0.35f
        }
        chasquido(0f, 0.95f, 0.004f)
        chasquido(0.006f, 0.55f, 0.007f)
        repeat(9) { chasquido(0.02f + r.nextFloat() * 0.2f, 0.12f + r.nextFloat() * 0.4f, 0.0015f + r.nextFloat() * 0.003f) }
        val pico = s.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        return ShortArray(n) { i ->
            val desvanecer = if (i > n - 600) (n - i) / 600f else 1f
            (s[i] / pico * 0.8f * desvanecer * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Distancia de un punto al centro del sello (útil para pruebas y para recortar). */
    fun radio(p: P) = hypot(p.x, p.y)

    /** Lleva el punto al borde si se sale del círculo de radio [r]. */
    fun recortar(p: P, r: Float): P { val d = radio(p); return if (d <= r) p else P(p.x / d * r, p.y / d * r) }
}
