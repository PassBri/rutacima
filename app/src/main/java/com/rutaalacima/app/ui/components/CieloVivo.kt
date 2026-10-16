package com.rutaalacima.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Astronomia
import com.rutaalacima.app.domain.model.Astronomia.Planeta
import com.rutaalacima.app.domain.model.CieloDatos
import com.rutaalacima.app.domain.model.CieloEventos
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import kotlinx.coroutines.delay
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * El cielo vivo como un instrumento de altura (estilo Altitud): el degradado real del cielo de este
 * momento, estrellas de noche, la línea del horizonte con marcas cada 15°, el arco que recorre el sol
 * hoy, el sol y la luna (con su fase) en su lugar real con un anillo fino, los planetas visibles con
 * su nombre, y abajo la cordillera saliendo de un mar de nubes. Se actualiza cada minuto.
 * Es el mismo cielo de Rutaalacima Web (web/js/10-cielo.js).
 *
 * [horizonte] es la altura del horizonte (0..1 desde arriba): más arriba si va texto encima.
 * [lectura] muestra la hora y la altura del sol arriba a la izquierda.
 */
@Composable
fun CieloVivo(
    modifier: Modifier = Modifier,
    horizonte: Float = 0.6f,
    lectura: Boolean = true,
    contenido: @Composable BoxScope.() -> Unit = {},
) {
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000 - ahora % 60_000)
            ahora = System.currentTimeMillis()
        }
    }
    val lng = remember(ahora / 3_600_000) { Astronomia.longitudPorZona(TimeZone.getDefault().getOffset(ahora).toLong()) }
    val sol = Astronomia.sol(ahora, lng = lng)
    val luna = Astronomia.luna(ahora, lng = lng)
    val il = Astronomia.iluminacion(ahora)
    val altS = sol.alturaGrados
    val nombres = mapOf(
        Planeta.VENUS to stringResource(R.string.planeta_venus), Planeta.JUPITER to stringResource(R.string.planeta_jupiter),
        Planeta.MARTE to stringResource(R.string.planeta_marte), Planeta.SATURNO to stringResource(R.string.planeta_saturno),
    )
    val planetas = Planeta.entries.mapNotNull { p ->
        val pos = Astronomia.planeta(p, ahora, lng = lng)
        if (pos.alturaGrados < 2 || altS > p.umbralSol) null else nombres.getValue(p) to pos
    }
    // El arco del sol de hoy, cada 20 minutos (se recalcula una vez por día)
    val dia = remember(ahora / 86_400_000) {
        val inicio = ahora - (ahora + TimeZone.getDefault().getOffset(ahora)) % 86_400_000
        (0..72).map { Astronomia.sol(inicio + it * 1_200_000L, lng = lng) }.filter { it.alturaGrados > -8 }
    }
    val medidor = rememberTextMeasurer()
    val etiqueta = TextStyle(color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp, letterSpacing = 1.4.sp)
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val datos by androidx.compose.runtime.produceState<CieloDatos?>(DatosCielo.cache, contexto) {
        if (value == null) value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { DatosCielo.de(contexto) }
    }
    val idioma = remember { java.util.Locale.getDefault().language }
    // Constelaciones reales: altura y azimut de cada estrella y vértice (una vez por minuto, no por cuadro)
    val cielo = remember(ahora / 60_000, datos) {
        val d = datos ?: return@remember null
        if (altS > -4) return@remember null
        val ver = { p: Astronomia.Posicion -> p.altura > 0.02 && kotlin.math.cos(p.azimut) > 0.05 }
        Constelaciones(
            estrellas = d.estrellas.mapNotNull { (ra, dec, mag) -> Astronomia.fijo(ra, dec, ahora, lng = lng).takeIf(ver)?.let { it to mag } },
            lineas = d.lineas.map { l -> l.rango to l.segmentos.map { seg -> seg.map { (ra, dec) -> Astronomia.fijo(ra, dec, ahora, lng = lng).takeIf(ver) } } },
            nombres = d.nombres.values.mapNotNull { n -> Astronomia.fijo(n.ra, n.dec, ahora, lng = lng).takeIf { ver(it) && it.alturaGrados > 8 }?.let { n.en(idioma) to it } },
        )
    }
    val actual = datos?.let { CieloEventos.eventoDe(it, ahora) }
    val cub = actual?.evento?.takeIf { it.tipo.startsWith("eclipse") }?.let { CieloEventos.cubrimiento(it, ahora) } ?: 0.0
    val eclipseLuna = if (actual?.evento?.tipo == "eclipse_luna") cub.toFloat() else 0f
    val eclipseSol = if (actual?.evento?.tipo == "eclipse_sol") cub.toFloat() else 0f
    // Lluvia de meteoros visible: de noche, con el radiante arriba y menos estelas si hay luna
    val estelas = datos?.let { d -> CieloEventos.lluviaDe(d, ahora) }?.takeIf { altS < -10 }?.let { ll ->
        val r = Astronomia.fijo(ll.lluvia.ra, ll.lluvia.dec, ahora, lng = lng)
        val lunaArriba = if (luna.alturaGrados > 0) il.fraccion else 0.0
        val efectiva = ll.tasa * max(0.0, sin(r.altura)) * (1 - 0.6 * lunaArriba)
        if (efectiva >= 1) Lluvia(r, (efectiva / 8).roundToInt().coerceIn(1, 6), ahora / 60_000) else null
    }
    val descripcion = stringResource(R.string.cielo_desc, altS.roundToInt(), (il.fraccion * 100).roundToInt())

    Box(modifier.semantics { contentDescription = descripcion }) {
        Canvas(Modifier.fillMaxSize()) {
            val (arriba, medio, abajo) = coloresCielo(altS)
            val noche = ((-altS - 2) / 10).coerceIn(0.0, 1.0).toFloat()
            val luz = ((altS + 4) / 14).coerceIn(0.0, 1.0).toFloat()
            val h0 = size.height * horizonte
            fun x(az: Double) = size.width * (0.5f + sin(az).toFloat() * 0.46f)
            fun y(alt: Double) = h0 - (max(-12.0, alt / (Math.PI / 180)) / 90.0).toFloat() * (h0 - size.height * 0.16f)

            drawRect(Brush.verticalGradient(0f to arriba, 0.62f to medio, 1f to abajo))
            val c = cielo
            if (c != null && noche > 0.05f) {
                // En el teléfono: solo las constelaciones mayores y sin nombres (el cielo es pequeño)
                val amplio = size.width >= 560.dp.toPx()
                val trazo = Color.White.copy(alpha = 0.22f * noche)
                c.lineas.forEach { (rango, segs) ->
                    if (amplio || rango == 1) segs.forEach { seg ->
                        seg.zipWithNext { a, b -> if (a != null && b != null) drawLine(trazo, Offset(x(a.azimut), y(a.altura)), Offset(x(b.azimut), y(b.altura)), 1.dp.toPx()) }
                    }
                }
                c.estrellas.forEach { (p, mag) ->
                    drawCircle(Color.White.copy(alpha = noche), (0.5f + (4f - mag.toFloat()) * 0.32f).dp.toPx(), Offset(x(p.azimut), y(p.altura)))
                }
                if (amplio) c.nombres.forEach { (nombre, p) ->
                    val o = Offset(x(p.azimut), y(p.altura))
                    if (lectura && o.y < size.height * 0.34f && (o.x < size.width * 0.36f || o.x > size.width * 0.5f)) return@forEach
                    val t = medidor.measure(nombre.uppercase(), etiqueta.copy(color = Color.White.copy(alpha = 0.55f * noche), fontSize = 9.sp))
                    drawText(t, topLeft = Offset(o.x - t.size.width / 2f, o.y - t.size.height / 2f))
                }
            } else if (noche > 0f) ESTRELLAS.forEach { (ex, ey, r) ->
                drawCircle(Color.White.copy(alpha = noche * 0.85f), r.dp.toPx() * 0.6f, Offset(ex * size.width, ey * h0))
            }
            // Instrumento: horizonte, marcas y el arco del sol
            val linea = Color.White.copy(alpha = 0.22f + 0.2f * noche)
            val fino = 1.dp.toPx()
            drawLine(linea, Offset(size.width * 0.02f, h0), Offset(size.width * 0.98f, h0), fino)
            for (a in -75..75 step 15) {
                val xx = x(a * Math.PI / 180)
                drawLine(linea, Offset(xx, h0), Offset(xx, h0 - (if (a % 45 == 0) 7 else 4).dp.toPx()), fino)
            }
            if (dia.size > 1) {
                val arco = Path().apply { dia.forEachIndexed { i, p -> if (i == 0) moveTo(x(p.azimut), y(p.altura)) else lineTo(x(p.azimut), y(p.altura)) } }
                drawPath(arco, linea, style = Stroke(fino, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))))
            }
            // Sol
            if (altS > -6) {
                val c = Offset(x(sol.azimut), y(sol.altura)); val a = min(1.0, (altS + 6) / 6).toFloat()
                drawCircle(Brush.radialGradient(listOf(Color(0x66FFE8B4), Color.Transparent), c, 30.dp.toPx()), 30.dp.toPx(), c, alpha = a)
                drawCircle(Brush.radialGradient(0f to Color(0xFFFFF8E6), 0.6f to Color(0xFFFFE2A6), 1f to Color(0x00FFE2A6), center = c, radius = 13.dp.toPx()), 13.dp.toPx(), c, alpha = a)
                if (eclipseSol > 0f) drawCircle(Color(0xFF0E1620), 13.dp.toPx(), c + Offset((1 - eclipseSol) * 26.dp.toPx(), 0f))
                drawCircle(Color.White.copy(alpha = 0.45f * a), 27.dp.toPx(), c, style = Stroke(fino))
                rotulo(medidor, "${altS.roundToInt()}°", c, 27.dp.toPx(), etiqueta)
            }
            // Luna con su fase
            if (luna.alturaGrados > -3 && il.fraccion > 0.04) {
                val c = Offset(x(luna.azimut), y(luna.altura)); val r = 10.dp.toPx(); val a = 0.4f + 0.6f * noche
                drawCircle(Color(0xFF181E2C).copy(alpha = 0.55f * a), r, c)
                drawPath(faseLuna(il.fase, r, c), lerp(Color(0xFFEEF1F4), Color(0xFFA8502C), eclipseLuna).copy(alpha = a * (1 - 0.45f * eclipseLuna)))
                drawCircle(Color.White.copy(alpha = 0.32f * a), 21.dp.toPx(), c, style = Stroke(fino))
                rotulo(medidor, "${(il.fraccion * 100).roundToInt()} %", c, 21.dp.toPx(), etiqueta)
            }
            // Planetas visibles
            planetas.forEach { (nombre, p) ->
                val c = Offset(x(p.azimut), y(p.altura))
                drawCircle(Color.White.copy(alpha = 0.35f), 7.dp.toPx(), c, style = Stroke(fino))
                drawCircle(Color.White, 2.dp.toPx(), c)
                val bajoLectura = lectura && c.x < size.width * 0.36f && c.y < size.height * 0.34f
                if (!bajoLectura) rotulo(medidor, nombre.uppercase(), c, 7.dp.toPx(), etiqueta)
            }
            if (eclipseSol > 0.6f) drawRect(Color(0xFF060A12).copy(alpha = ((eclipseSol - 0.6f) * 1.4f).coerceAtMost(1f)))
            // Cordillera saliendo de un mar de nubes
            val monte = lerp(lerp(abajo, Color(0xFF141C28), 0.55f), Color(0xFF06090E), noche * 0.6f)
            val alto = size.height - h0 + size.height * 0.06f
            montes(MONTES_LEJOS, alto, monte.copy(alpha = 0.55f))
            montes(MONTES_CERCA, alto, monte)
            val nubes = Path().apply {
                val sx = size.width / 400f; val sy = alto / 60f; val top = size.height - alto
                moveTo(0f, size.height); lineTo(0f, top + 48 * sy)
                var px = 0f
                listOf(80f to 47f, 160f to 48f, 248f to 49f, 336f to 48f, 400f to 47f).forEach { (nx, ny) ->
                    cubicTo((px + 30) * sx, top + 44 * sy, (nx - 28) * sx, top + 50 * sy, nx * sx, top + ny * sy); px = nx
                }
                lineTo(size.width, size.height); close()
            }
            drawPath(nubes, Color.White.copy(alpha = 0.16f + 0.62f * luz))
        }
        estelas?.let { EstelasDeMeteoros(it, horizonte) }
        if (lectura) {
            Column(Modifier.padding(start = 18.dp, top = 14.dp)) {
                Text(
                    android.text.format.DateFormat.getTimeFormat(androidx.compose.ui.platform.LocalContext.current).format(java.util.Date(ahora)),
                    color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp,
                )
                Text(
                    stringResource(R.string.cielo_lectura, altS.roundToInt(), (il.fraccion * 100).roundToInt()).uppercase(),
                    color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, letterSpacing = 1.6.sp,
                )
            }
        }
        contenido()
        if (lectura && actual != null && datos != null) ChipEvento(actual, datos!!, idioma, Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(top = 12.dp, end = 12.dp))
    }
}

/** Los datos del cielo (assets/cielo/datos.json, generados por tools/cielo/generar.js), leídos una sola vez. */
object DatosCielo {
    @Volatile var cache: CieloDatos? = null
        private set
    fun de(contexto: android.content.Context): CieloDatos? = cache ?: runCatching {
        CieloDatos.leer(contexto.assets.open("cielo/datos.json").bufferedReader().use { it.readText() })
    }.getOrNull()?.also { cache = it }
}

private class Constelaciones(
    val estrellas: List<Pair<Astronomia.Posicion, Double>>,
    val lineas: List<Pair<Int, List<List<Astronomia.Posicion?>>>>,
    val nombres: List<Pair<String, Astronomia.Posicion>>,
)

private class Lluvia(val radiante: Astronomia.Posicion, val cuantas: Int, val semilla: Long)

/**
 * Estelas finas que nacen del radiante: cada una cruza el cielo en menos de un segundo dentro de un
 * ciclo de 60 s. Con "Reducir movimiento" (animaciones del sistema apagadas) queda una estela quieta.
 */
@Composable
private fun EstelasDeMeteoros(ll: Lluvia, horizonte: Float) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val sinMovimiento = remember {
        android.provider.Settings.Global.getFloat(contexto.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val avance = if (sinMovimiento) remember { androidx.compose.runtime.mutableFloatStateOf(0.002f) }.floatValue else
        androidx.compose.animation.core.rememberInfiniteTransition(label = "estelas").animateFloat(
            0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(60_000, easing = androidx.compose.animation.core.LinearEasing)), label = "ciclo",
        ).value
    Canvas(Modifier.fillMaxSize()) {
        val h0 = size.height * horizonte
        val rx = size.width * (0.5f + sin(ll.radiante.azimut).toFloat() * 0.46f)
        val ry = h0 - (max(-12.0, ll.radiante.alturaGrados) / 90.0).toFloat() * (h0 - size.height * 0.16f)
        for (k in 0 until (if (sinMovimiento) 1 else ll.cuantas)) {
            val azar = kotlin.math.abs((sin(ll.semilla * 12.9898 + k * 78.233) * 43758.5453) % 1).toFloat()
            val azar2 = kotlin.math.abs((azar * 7.31f) % 1f)
            val inicio = Offset(size.width * (0.08f + azar * 0.84f), size.height * 0.08f + azar2 * (h0 - size.height * 0.22f))
            val dir = (inicio - Offset(rx, ry)).let { val l = it.getDistance().coerceAtLeast(1f); Offset(it.x / l, it.y / l) }
            val t = ((avance * 60f - (k * 60f / ll.cuantas + azar2 * 6f)) % 60f + 60f) % 60f / 0.84f   // 0..1 mientras la estela cruza
            if (t !in 0f..1f) continue
            val cabeza = inicio + dir * (90.dp.toPx() * t)
            val cola = cabeza - dir * (70.dp.toPx() * (0.2f + 0.8f * t))
            val alfa = if (t < 0.3f) t / 0.3f else 1f - (t - 0.3f) / 0.7f
            drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = alfa)), cola, cabeza), cola, cabeza, 1.5.dp.toPx())
        }
    }
}

/** El evento del cielo de hoy: una línea discreta; al tocarla se explica en dos frases. */
@Composable
private fun ChipEvento(a: CieloEventos.Actual, datos: CieloDatos, idioma: String, modifier: Modifier) {
    val (linea, detalle, explica) = textoEvento(a, datos, idioma)
    var abierto by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val desc = stringResource(R.string.cielo_evento_desc, linea)
    androidx.compose.material3.Surface(
        onClick = { abierto = true },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = Color(0x52060A10), contentColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
        modifier = modifier.widthIn(max = 220.dp).heightIn(min = 32.dp).semantics { contentDescription = desc },
    ) {
        androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.secondary, androidx.compose.foundation.shape.CircleShape))
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Text(linea, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
    if (abierto) androidx.compose.material3.AlertDialog(
        onDismissRequest = { abierto = false },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.entendido)) } },
        title = { Text(linea) },
        text = {
            Column {
                Text(explica)
                if (detalle.isNotBlank()) Text(detalle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            }
        },
    )
}

/** Nombre corto, detalle (fecha y horas) y explicación del evento, en el idioma del teléfono. */
@Composable
private fun textoEvento(a: CieloEventos.Actual, datos: CieloDatos, idioma: String): Triple<String, String, String> {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val horaF = android.text.format.DateFormat.getTimeFormat(ctx)
    fun h(ms: Long?) = ms?.let { horaF.format(java.util.Date(it)) } ?: ""
    val patron = android.text.format.DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), "dMMMM")
    fun f(ms: Long) = java.text.SimpleDateFormat(patron, java.util.Locale.getDefault()).format(java.util.Date(ms))
    val e = a.evento
    val ll = a.lluvia
    if (ll != null) {
        val cons = datos.nombres[ll.lluvia.constelacion]?.en(idioma) ?: ll.lluvia.constelacion
        return Triple(stringResource(R.string.cielo_lluvia, cons), stringResource(R.string.cielo_lluvia_pico, f(ll.pico)), stringResource(R.string.cielo_exp_lluvia, cons))
    }
    e ?: return Triple("", "", "")
    val astro = mapOf(
        "luna" to stringResource(R.string.planeta_luna), "venus" to stringResource(R.string.planeta_venus), "marte" to stringResource(R.string.planeta_marte),
        "jupiter" to stringResource(R.string.planeta_jupiter), "saturno" to stringResource(R.string.planeta_saturno),
    )
    val numero = java.text.NumberFormat.getInstance()
    return when (e.tipo) {
        "eclipse_luna" -> Triple(
            stringResource(when (e.clase) { "total" -> R.string.cielo_eclipse_luna_total; "partial" -> R.string.cielo_eclipse_luna_parcial; else -> R.string.cielo_eclipse_luna_penumbral }),
            "${f(e.max)} · ${h(e.parcialInicio ?: e.inicio)}–${h(e.parcialFin ?: e.fin)}",
            stringResource(when (e.clase) { "total" -> R.string.cielo_exp_luna_total; "partial" -> R.string.cielo_exp_luna_parcial; else -> R.string.cielo_exp_luna_penumbral }),
        )
        "eclipse_sol" -> Triple(
            stringResource(when (e.clase) { "total" -> R.string.cielo_eclipse_sol_total; "annular" -> R.string.cielo_eclipse_sol_anular; else -> R.string.cielo_eclipse_sol_parcial }),
            "${f(e.max)} · ${h(e.inicio)}–${h(e.fin)}",
            stringResource(R.string.cielo_exp_sol, (e.oscurecimiento * 100).roundToInt()),
        )
        "conjuncion" -> {
            val (x1, x2) = e.cuerpos.map { astro[it] ?: it }
            val grados = numero.format(e.sep)
            Triple(stringResource(R.string.cielo_conjuncion, x1, x2), "${f(e.max)} · $grados°", stringResource(R.string.cielo_exp_conjuncion, x1, x2, grados))
        }
        "superluna" -> Triple(stringResource(R.string.cielo_superluna), f(e.max), stringResource(R.string.cielo_exp_superluna, numero.format(e.km)))
        else -> Triple(
            stringResource(when (e.clase) { "equinoccio_marzo" -> R.string.cielo_equinoccio_marzo; "equinoccio_septiembre" -> R.string.cielo_equinoccio_septiembre
                "solsticio_junio" -> R.string.cielo_solsticio_junio; else -> R.string.cielo_solsticio_diciembre }),
            f(e.max),
            stringResource(when (e.clase) { "solsticio_junio" -> R.string.cielo_exp_solsticio_junio; "solsticio_diciembre" -> R.string.cielo_exp_solsticio_diciembre; else -> R.string.cielo_exp_equinoccio }),
        )
    }
}

/** Colores del cielo (arriba, medio, horizonte) según la altura del sol: noche, alba y día con bruma clara. */
private fun coloresCielo(alt: Double): Triple<Color, Color, Color> {
    val noche = Triple(Color(0xFF060A14), Color(0xFF101828), Color(0xFF1E283C))
    val alba = Triple(Color(0xFF2C3A62), Color(0xFF7E748C), Color(0xFFEEAC7A))
    val diaC = Triple(Color(0xFF4E84BA), Color(0xFF96BEDE), Color(0xFFDEE9EE))
    fun mx(a: Triple<Color, Color, Color>, b: Triple<Color, Color, Color>, t: Float) =
        Triple(lerp(a.first, b.first, t), lerp(a.second, b.second, t), lerp(a.third, b.third, t))
    return when {
        alt <= -10 -> noche
        alt <= 2 -> mx(noche, alba, ((alt + 10) / 12).toFloat())
        alt <= 14 -> mx(alba, diaC, ((alt - 2) / 12).toFloat())
        else -> diaC
    }
}

/** La parte iluminada de la luna: medio disco del lado de la luz y la elipse del terminador. */
private fun faseLuna(fase: Double, r: Float, c: Offset): Path {
    val crece = fase < 0.5
    val giba = fase > 0.25 && fase < 0.75
    val rx = (Astronomia.anchoTerminador(fase) * r).toFloat()
    val disco = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
    val elipse = Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)
    return Path().apply {
        moveTo(c.x, c.y - r)
        // Borde iluminado: a la derecha al crecer, a la izquierda al menguar
        arcTo(disco, -90f, if (crece) 180f else -180f, false)
        // Terminador de abajo hacia arriba: hacia el lado oscuro si está gibosa, hacia la luz si es creciente/menguante
        val haciaIzquierda = crece == giba
        arcTo(elipse, 90f, if (haciaIzquierda) 180f else -180f, false)
        close()
    }
}

private fun DrawScope.rotulo(medidor: androidx.compose.ui.text.TextMeasurer, texto: String, c: Offset, radio: Float, estilo: TextStyle) {
    val t = medidor.measure(texto, estilo)
    val izq = c.x > size.width * 0.78f
    val x = if (izq) c.x - radio - 6.dp.toPx() - t.size.width else c.x + radio + 6.dp.toPx()
    drawText(t, topLeft = Offset(x, c.y - t.size.height / 2f))
}

private fun DrawScope.montes(puntos: List<Pair<Float, Float>>, alto: Float, color: Color) {
    val sx = size.width / 400f; val sy = alto / 60f; val top = size.height - alto
    drawPath(Path().apply {
        moveTo(0f, size.height)
        puntos.forEach { (px, py) -> lineTo(px * sx, top + py * sy) }
        lineTo(size.width, size.height); close()
    }, color)
}

private val MONTES_LEJOS = listOf(0f to 40f, 30f to 30f, 52f to 36f, 84f to 18f, 104f to 26f, 128f to 14f, 150f to 24f, 176f to 8f, 198f to 22f,
    226f to 16f, 252f to 28f, 280f to 12f, 306f to 26f, 334f to 20f, 360f to 30f, 384f to 22f, 400f to 28f)
private val MONTES_CERCA = listOf(0f to 46f, 40f to 34f, 70f to 42f, 112f to 24f, 140f to 38f, 170f to 30f, 214f to 42f, 246f to 28f,
    276f to 40f, 318f to 30f, 352f to 40f, 400f to 34f)
private val ESTRELLAS = List(70) { i -> Triple(((i * 97.3) % 100).toFloat() / 100f, ((i * 37.7) % 70).toFloat() / 70f * 0.95f, 0.5f + (i % 3) * 0.35f) }
