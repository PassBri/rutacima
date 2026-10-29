package com.rutaalacima.app.ui.components

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Astronomia
import com.rutaalacima.app.domain.model.Astronomia.Planeta
import com.rutaalacima.app.domain.model.CieloDatos
import com.rutaalacima.app.domain.model.CieloEventos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

private const val G = PI / 180

/**
 * Mirar el cielo: una ventana al cielo real según hacia dónde apuntas el teléfono o la tableta.
 * Con el sensor de rotación (brújula + giroscopio) se ve la parte del cielo que está detrás de la
 * pantalla, corregida al norte verdadero con la declinación magnética. Sin sensor, se arrastra.
 * Proyección estereográfica y horizonte siempre nivelado. Es la misma ventana de la web (10-cielo.js).
 */
@Composable
fun MirarCieloScreen(onCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val datos by produceState(DatosCielo.cache, contexto) {
        if (value == null) value = withContext(Dispatchers.IO) { DatosCielo.de(contexto) }
    }
    // Mirada inicial: hacia el sur, a 35° (la mitad del cielo que mejor se ve desde Colombia)
    var metaRumbo by remember { mutableFloatStateOf(180f) }
    var metaAlt by remember { mutableFloatStateOf(35f) }
    var rumbo by remember { mutableFloatStateOf(180f) }
    var alt by remember { mutableFloatStateOf(35f) }
    var conSensor by remember { mutableStateOf(false) }
    var tiempo by remember { mutableLongStateOf(0L) }
    // Ajuste fino de la brújula (se recuerda en el teléfono) y modo noche en rojo (igual que la web)
    val prefs = remember { contexto.getSharedPreferences("suroute_ui", android.content.Context.MODE_PRIVATE) }
    var ajusteA by remember { mutableFloatStateOf(prefs.getFloat("cielo_dA", 0f)) }
    var ajusteH by remember { mutableFloatStateOf(prefs.getFloat("cielo_dh", 0f)) }
    var crudoRumbo by remember { mutableFloatStateOf(180f) }
    var crudoAlt by remember { mutableFloatStateOf(35f) }
    var ajustando by remember { mutableStateOf(false) }
    // Calibrar con un toque: donde la persona ve la Luna (o el Sol, o el planeta más alto) de verdad, ahí debe quedar el dibujo
    var calibrado by remember { mutableStateOf(false) }
    val ultimoFov = remember { floatArrayOf(70f) }
    var noche by remember { mutableStateOf(prefs.getBoolean("cielo_noche", false)) }
    fun guardarAjuste() { prefs.edit().putFloat("cielo_dA", ajusteA).putFloat("cielo_dh", ajusteH).apply() }
    fun aplicar() { metaRumbo = (crudoRumbo + ajusteA + 360f) % 360f; metaAlt = (crudoAlt + ajusteH).coerceIn(-20f, 89f) }

    // Sensor de rotación: dirección de la parte de atrás del aparato en Este-Norte-Arriba
    DisposableEffect(Unit) {
        val sm = contexto.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val declinacion = GeomagneticField(Astronomia.LATITUD.toFloat(), -73.1f, 1000f, System.currentTimeMillis()).declination
        val r = FloatArray(9)
        val oyente = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(r, e.values)
                val este = -r[2]; val norte = -r[5]; val arriba = -r[8]
                crudoRumbo = ((atan2(este, norte) / G).toFloat() + declinacion + 360f) % 360f
                crudoAlt = (asin(arriba.coerceIn(-1f, 1f)) / G).toFloat()
                aplicar()
                conSensor = true
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm?.registerListener(oyente, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(oyente) }
    }
    // Suavizado por cuadro (el giro más corto) para que el cielo no tiemble
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { t ->
                val d = ((metaRumbo - rumbo + 540f) % 360f) - 180f
                rumbo = (rumbo + d * 0.18f + 360f) % 360f
                alt += (metaAlt - alt) * 0.18f
                tiempo = t / 1_000_000
            }
        }
    }
    var minuto by remember { mutableLongStateOf(System.currentTimeMillis() / 60_000) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(15_000); minuto = System.currentTimeMillis() / 60_000 } }
    val idioma = remember { java.util.Locale.getDefault().language }
    val nombres = mapOf(
        Planeta.VENUS to stringResource(R.string.planeta_venus), Planeta.JUPITER to stringResource(R.string.planeta_jupiter),
        Planeta.MARTE to stringResource(R.string.planeta_marte), Planeta.SATURNO to stringResource(R.string.planeta_saturno),
    )
    val tSol = stringResource(R.string.cielo_sol); val tLuna = stringResource(R.string.planeta_luna); val tLluvia = stringResource(R.string.cielo_lluvia_corto)
    val cardinales = stringResource(R.string.cardinales).split(",")
    val cielo = remember(minuto, datos) { CieloDelMinuto.de(minuto * 60_000, datos, idioma, nombres) }
    val medidor = rememberTextMeasurer()
    val sinMovimiento = remember {
        android.provider.Settings.Global.getFloat(contexto.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val rumboTexto = stringResource(R.string.mirar_rumbo, cardinales[((rumbo / 45f).roundToInt()) % 8], rumbo.roundToInt(), alt.roundToInt())

    // Cámara: la imagen real de fondo y las guías encima. Se pide el permiso solo al tocar el botón.
    var camara by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var lente by remember { mutableStateOf<Pair<Double, Double>?>(null) }   // tan(medio ángulo) del lado corto y del largo
    val permiso = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok -> camara = ok }
    fun alternarCamara() {
        if (camara) { camara = false; return }
        if (androidx.core.content.ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) camara = true
        else permiso.launch(android.Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF060A12))
        // Modo noche: todo pasa a rojo tenue (también la cámara) para conservar la visión nocturna
        .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            if (noche) { drawRect(Color(0xFFFF2A2A), blendMode = androidx.compose.ui.graphics.BlendMode.Multiply); drawRect(Color.Black.copy(alpha = 0.35f)) }
        }) {
        if (camara) CamaraDeFondo(onLente = { lente = it })
        Canvas(
            Modifier.fillMaxSize().pointerInput(conSensor, ajustando) {
                if (!conSensor) detectDragGestures { _, d ->
                    val k = 70f / size.width
                    metaRumbo = (metaRumbo - d.x * k + 360f) % 360f
                    metaAlt = (metaAlt + d.y * k).coerceIn(-20f, 89f)
                } else if (ajustando) detectDragGestures(onDragEnd = { guardarAjuste() }) { _, d ->
                    // Con sensores, arrastrar corre el dibujo hasta que calce con el cielo real
                    val k = 70f / size.width
                    ajusteA = (((ajusteA - d.x * k) + 540f) % 360f) - 180f
                    ajusteH = (ajusteH + d.y * k).coerceIn(-30f, 30f)
                    aplicar()
                }
            }.pointerInput(ajustando, calibrado, cielo, camara) {
                if (ajustando) detectTapGestures(
                    onTap = { p ->
                        val ref = referenciaCielo(cielo, tSol, tLuna)
                        if (!calibrado && ref != null) {
                            val (aT, hT) = desproyectar(p.x, p.y, size.width.toFloat(), size.height.toFloat(), rumbo, alt, ultimoFov[0], camara)
                            ajusteA = ((ajusteA + (ref.second - aT).toFloat() + 540f) % 360f) - 180f
                            ajusteH = (ajusteH + (ref.third - hT).toFloat()).coerceIn(-30f, 30f)
                            guardarAjuste(); aplicar(); calibrado = true
                        }
                    },
                    onDoubleTap = { ajusteA = 0f; ajusteH = 0f; calibrado = false; guardarAjuste(); aplicar() },
                )
            },
        ) {
            val fov = if (camara) fovDeCamara(lente, size.width, size.height) else if (size.width < 700.dp.toPx()) 70f else 90f
            ultimoFov[0] = fov
            val marca = if (ajustando && !calibrado) referenciaCielo(cielo, tSol, tLuna)?.let { it.second to it.third } else null
            dibujarVentana(cielo, rumbo, alt, fov, tiempo, sinMovimiento, medidor, cardinales, tSol, tLuna, tLluvia, camara = camara, marca = marca)
        }
        Row(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0x8C060A10), Color.Transparent))).safeDrawingPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(rumboTexto.uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Light, letterSpacing = 1.8.sp,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite })
            val cerrar = stringResource(R.string.cerrar_cielo)
            Surface(onClick = onCerrar, shape = CircleShape, color = Color.White.copy(alpha = 0.1f), contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), modifier = Modifier.size(48.dp).semantics { contentDescription = cerrar }) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, null) }
            }
        }
        androidx.compose.foundation.layout.Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().safeDrawingPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                if (conSensor) BotonCielo(stringResource(if (ajustando) R.string.mirar_listo else R.string.mirar_ajustar), ajustando) {
                    ajustando = !ajustando; calibrado = false; if (!ajustando) guardarAjuste()
                }
                BotonCielo(stringResource(R.string.mirar_noche), noche) { noche = !noche; prefs.edit().putBoolean("cielo_noche", noche).apply() }
            }
            if (conSensor) {
                val etq = stringResource(if (camara) R.string.mirar_camara_quitar else R.string.mirar_camara)
                Surface(onClick = { alternarCamara() }, shape = CircleShape, color = if (camara) Color.White else Color.White.copy(alpha = 0.12f),
                    contentColor = if (camara) Color(0xFF060A12) else Color.White, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(bottom = 10.dp).semantics { contentDescription = etq }) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (camara) Icons.Filled.Close else Icons.Filled.PhotoCamera, null, Modifier.size(18.dp))
                        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                        Text(etq, fontSize = 14.sp)
                    }
                }
            }
            Text(
                referenciaCielo(cielo, tSol, tLuna)?.first.let { ref -> when {
                    ajustando && calibrado && ref != null -> stringResource(R.string.mirar_calibrado, ref)
                    ajustando && ref != null -> stringResource(R.string.mirar_ayuda_calibrar, ref)
                    ajustando -> stringResource(R.string.mirar_ayuda_ajustar)
                    camara && conSensor && ajusteA == 0f && ajusteH == 0f && ref != null -> stringResource(R.string.mirar_sugerir_calibrar, ref)
                    camara -> stringResource(R.string.mirar_ayuda_camara)
                    conSensor -> stringResource(R.string.mirar_ayuda_sensor)
                    else -> stringResource(R.string.mirar_ayuda_arrastre)
                } },
                color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.7f), blurRadius = 4f)),
            )
        }
    }
}

/** El astro para calibrar: nombre, rumbo y altura. La Luna o el Sol sobre el horizonte; si no, el planeta más alto. */
private fun referenciaCielo(c: CieloDelMinuto, tSol: String, tLuna: String): Triple<String, Double, Double>? = when {
    c.luna.second > 2 -> Triple(tLuna, c.luna.first, c.luna.second)
    c.sol.second > 2 -> Triple(tSol, c.sol.first, c.sol.second)
    else -> c.planetas.filter { it.third > 5 }.maxByOrNull { it.third }
}

/** Inverso de la proyección de dibujarVentana: el rumbo y la altura que hay detrás de un punto de la pantalla. */
private fun desproyectar(x: Float, y: Float, w: Float, h: Float, rumbo: Float, alt: Float, fov: Float, camara: Boolean): Pair<Double, Double> {
    val a0 = rumbo * G; val h0 = alt * G
    val f = doubleArrayOf(cos(h0) * sin(a0), cos(h0) * cos(a0), sin(h0))
    val r = doubleArrayOf(cos(a0), -sin(a0), 0.0)
    val u = doubleArrayOf(-sin(a0) * sin(h0), -cos(a0) * sin(h0), cos(h0))
    val escala = if (camara) (w / 2) / tan(fov * G / 2) else (w / 2) / (2 * tan(fov * G / 4))
    val px = (x - w / 2) / escala; val py = (h / 2 - y) / escala
    val v = if (camara) DoubleArray(3) { f[it] + px * r[it] + py * u[it] } else {
        val rho = kotlin.math.hypot(px, py); val c = 2 * kotlin.math.atan(rho / 2); val s = if (rho > 0) sin(c) / rho else 0.0
        DoubleArray(3) { cos(c) * f[it] + s * (px * r[it] + py * u[it]) }
    }
    val n = kotlin.math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    return ((atan2(v[0], v[1]) / G + 360) % 360) to (asin((v[2] / n).coerceIn(-1.0, 1.0)) / G)
}

/**
 * Ángulo horizontal que se ve de la cámara en la pantalla. La vista previa (4:3) llena la pantalla y
 * recorta lo que sobra (FILL_CENTER); con el tamaño del sensor y la focal del lente se sabe cuánto cielo
 * cabe de lado a lado. Sin datos del lente, 60° (lo típico de la cámara principal en vertical).
 */
private fun fovDeCamara(lente: Pair<Double, Double>?, w: Float, h: Float): Float {
    val (tCorto, tLargo) = lente ?: return 60f
    val vertical = h >= w
    // Imagen en el sentido de la pantalla: en vertical, el lado corto del sensor va de lado a lado
    val imgW = if (vertical) 3.0 else 4.0; val imgH = if (vertical) 4.0 else 3.0
    val escala = maxOf(w / imgW, h / imgH)
    val fraccion = (w / escala) / imgW
    val tanMedio = fraccion * (if (vertical) tCorto else tLargo)
    return (2 * kotlin.math.atan(tanMedio) / G).toFloat().coerceIn(20f, 120f)
}

/** La cámara de atrás, a pantalla completa, detrás de las guías. Entrega las tangentes del medio ángulo del lente. */
@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
@Composable
private fun CamaraDeFondo(onLente: (Pair<Double, Double>) -> Unit) {
    val contexto = LocalContext.current
    val ciclo = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val vista = remember {
        androidx.camera.view.PreviewView(contexto).apply {
            implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
            scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
        }
    }
    DisposableEffect(Unit) {
        val futuro = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(contexto)
        var proveedor: androidx.camera.lifecycle.ProcessCameraProvider? = null
        futuro.addListener({
            runCatching {
                val p = futuro.get(); proveedor = p
                val previa = androidx.camera.core.Preview.Builder().build().also { it.setSurfaceProvider(vista.surfaceProvider) }
                p.unbindAll()
                val cam = p.bindToLifecycle(ciclo, androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA, previa)
                val info = androidx.camera.camera2.interop.Camera2CameraInfo.from(cam.cameraInfo)
                val sensor = info.getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val focal = info.getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
                if (sensor != null && focal != null && focal > 0f) {
                    val corto = minOf(sensor.width, sensor.height); val largo = maxOf(sensor.width, sensor.height)
                    onLente((corto / 2.0 / focal) to (largo / 2.0 / focal))
                }
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(contexto))
        onDispose { runCatching { proveedor?.unbindAll() } }
    }
    androidx.compose.ui.viewinterop.AndroidView({ vista }, Modifier.fillMaxSize())
}

/** Todo lo que hay en el cielo en un minuto: rumbo (desde el norte) y altura, en grados. */
private class CieloDelMinuto(
    val sol: Pair<Double, Double>, val luna: Pair<Double, Double>, val il: Astronomia.Iluminacion,
    val planetas: List<Triple<String, Double, Double>>,
    val estrellas: List<Triple<Double, Double, Double>>,
    val lineas: List<List<List<Pair<Double, Double>>>>,
    val nombres: List<Triple<String, Double, Double>>,
    val eclipseLuna: Float, val eclipseSol: Float,
    val radiante: Pair<Double, Double>?, val tasa: Double,
) {
    companion object {
        fun de(ms: Long, datos: CieloDatos?, idioma: String, nombresPlanetas: Map<Planeta, String>): CieloDelMinuto {
            val lng = Astronomia.longitudPorZona(TimeZone.getDefault().getOffset(ms).toLong())
            fun hz(p: Astronomia.Posicion) = ((p.azimut / G + 180 + 360) % 360) to p.alturaGrados
            fun fijo(ra: Double, dec: Double) = hz(Astronomia.fijo(ra, dec, ms, lng = lng))
            val s = Astronomia.sol(ms, lng = lng)
            val actual = datos?.let { CieloEventos.eventoDe(it, ms) }
            val cub = actual?.evento?.takeIf { it.tipo.startsWith("eclipse") }?.let { CieloEventos.cubrimiento(it, ms) } ?: 0.0
            val ll = datos?.let { CieloEventos.lluviaDe(it, ms) }?.takeIf { s.alturaGrados < -10 }
            return CieloDelMinuto(
                sol = hz(s), luna = hz(Astronomia.luna(ms, lng = lng)), il = Astronomia.iluminacion(ms),
                planetas = Planeta.entries.map { p -> hz(Astronomia.planeta(p, ms, lng = lng)).let { (a, h) -> Triple(nombresPlanetas.getValue(p), a, h) } },
                estrellas = datos?.estrellas?.map { (ra, dec, mag) -> fijo(ra, dec).let { (a, h) -> Triple(a, h, mag) } }.orEmpty(),
                lineas = datos?.lineas?.map { l -> l.segmentos.map { seg -> seg.map { (ra, dec) -> fijo(ra, dec) } } }.orEmpty(),
                nombres = datos?.nombres?.values?.map { n -> fijo(n.ra, n.dec).let { (a, h) -> Triple(n.en(idioma), a, h) } }.orEmpty(),
                eclipseLuna = if (actual?.evento?.tipo == "eclipse_luna") cub.toFloat() else 0f,
                eclipseSol = if (actual?.evento?.tipo == "eclipse_sol") cub.toFloat() else 0f,
                radiante = ll?.let { fijo(it.lluvia.ra, it.lluvia.dec) }, tasa = ll?.tasa ?: 0.0,
            )
        }
    }
}

private fun DrawScope.dibujarVentana(
    c: CieloDelMinuto, rumbo: Float, alt: Float, fov: Float, tiempo: Long, sinMovimiento: Boolean,
    medidor: androidx.compose.ui.text.TextMeasurer, cardinales: List<String>, tSol: String, tLuna: String, tLluvia: String,
    /** Sobre la cámara: sin cielo ni montañas pintados, proyección del lente (rectilínea) y guías siempre visibles. */
    camara: Boolean = false,
    /** Mientras se calibra: el astro de referencia, con un aro dorado que late para compararlo con el de verdad. */
    marca: Pair<Double, Double>? = null,
) {
    val w = size.width; val h = size.height
    val a0 = rumbo * G; val h0 = alt * G
    val f = doubleArrayOf(cos(h0) * sin(a0), cos(h0) * cos(a0), sin(h0))
    val r = doubleArrayOf(cos(a0), -sin(a0), 0.0)
    val u = doubleArrayOf(-sin(a0) * sin(h0), -cos(a0) * sin(h0), cos(h0))
    val escala = if (camara) (w / 2) / tan(fov * G / 2) else (w / 2) / (2 * tan(fov * G / 4))
    fun proy(az: Double, al: Double): Offset? {
        val a = az * G; val e = al * G
        val vx = cos(e) * sin(a); val vy = cos(e) * cos(a); val vz = sin(e)
        val pz = vx * f[0] + vy * f[1] + vz * f[2]
        if (pz < (if (camara) 0.15 else -0.2)) return null
        val k = if (camara) 1 / pz else 2 / (1 + pz)
        return Offset((w / 2 + k * (vx * r[0] + vy * r[1]) * escala).toFloat(), (h / 2 - k * (vx * u[0] + vy * u[1] + vz * u[2]) * escala).toFloat())
    }
    val altS = c.sol.second
    val noche = if (camara) 1f else ((-altS - 2) / 10).coerceIn(0.0, 1.0).toFloat()
    val dia = ((altS + 4) / 14).coerceIn(0.0, 1.0).toFloat()
    val (arriba, medio, abajo) = coloresCielo(altS)
    val yHor = proy(rumbo.toDouble(), 0.0)?.y ?: h
    val yCenit = proy(rumbo.toDouble(), 89.9)?.y ?: -h
    if (camara) drawRect(Color(0x26060A12))   // un velo apenas, para que las guías se lean sobre la imagen
    else drawRect(Brush.verticalGradient(0f to arriba, 0.62f to medio, 1f to abajo, startY = min(yCenit, yHor - 1), endY = yHor))
    if (c.eclipseSol > 0.6f) drawRect(Color(0xFF060A12).copy(alpha = ((c.eclipseSol - 0.6f) * 1.4f).coerceAtMost(1f)))
    val etiqueta = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, letterSpacing = 1.4.sp)
    fun rotulo(o: Offset, t: String, radio: Float) {
        val m = medidor.measure(t.uppercase(), etiqueta)
        drawText(m, topLeft = Offset(o.x + radio + 6.dp.toPx(), o.y - m.size.height / 2f))
    }
    // Constelaciones y estrellas
    if (noche > 0.05f) {
        val trazo = Color.White.copy(alpha = if (camara) 0.6f else 0.22f * noche)
        c.lineas.forEach { segs -> segs.forEach { seg -> seg.zipWithNext { p, q ->
            if (p.second > -1 && q.second > -1) { val a = proy(p.first, p.second); val b = proy(q.first, q.second); if (a != null && b != null) drawLine(trazo, a, b, 1.dp.toPx()) }
        } } }
        c.estrellas.forEach { (az, al, mag) -> if (al > -1) proy(az, al)?.let {
            if (camara) drawCircle(Color.White.copy(alpha = 0.7f), (2.5f + (4f - mag.toFloat()) * 0.8f).dp.toPx(), it, style = Stroke(1.dp.toPx()))   // aro: la estrella real va adentro
            else drawCircle(Color.White.copy(alpha = noche), (0.6f + (4f - mag.toFloat()) * 0.4f).dp.toPx(), it)
        } }
        val est = etiqueta.copy(color = Color.White.copy(alpha = if (camara) 0.9f else 0.55f * noche), fontSize = if (camara) 12.sp else 10.sp, letterSpacing = 2.sp,
            shadow = if (camara) androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.7f), blurRadius = 4f) else null)
        c.nombres.forEach { (n, az, al) -> if (al > 3) proy(az, al)?.let { o -> val m = medidor.measure(n.uppercase(), est); drawText(m, topLeft = Offset(o.x - m.size.width / 2f, o.y - m.size.height / 2f)) } }
    }
    // Planetas
    c.planetas.forEach { (n, az, al) -> if (al > 0) proy(az, al)?.let { o ->
        drawCircle(Color.White, 2.6.dp.toPx(), o); drawCircle(Color.White.copy(alpha = 0.35f), 9.dp.toPx(), o, style = Stroke(1.dp.toPx())); rotulo(o, n, 9.dp.toPx())
    } }
    // Luna con su fase (cobre en un eclipse)
    if (c.luna.second > -2) proy(c.luna.first, c.luna.second)?.let { o ->
        val rr = 14.dp.toPx()
        drawCircle(Color(0x99181E2C), rr, o)
        drawPath(faseLuna(c.il.fase, rr, o), lerp(Color(0xFFEEF1F4), Color(0xFFA8502C), c.eclipseLuna))
        drawCircle(Color.White.copy(alpha = 0.32f), rr + 12.dp.toPx(), o, style = Stroke(1.dp.toPx()))
        rotulo(o, "$tLuna ${(c.il.fraccion * 100).roundToInt()} %", rr + 12.dp.toPx())
    }
    // Sol (tapado en la fracción real si hay eclipse)
    if (altS > -4) proy(c.sol.first, altS)?.let { o ->
        val rr = 16.dp.toPx()
        drawCircle(Brush.radialGradient(listOf(Color(0x8CFFE8B4), Color.Transparent), o, 60.dp.toPx()), 60.dp.toPx(), o)
        drawCircle(Color(0xFFFFF4D8), rr, o)
        if (c.eclipseSol > 0f) drawCircle(Color(0xFF0E1620), rr, o + Offset((1 - c.eclipseSol) * 2 * rr, 0f))
        drawCircle(Color.White.copy(alpha = 0.45f), rr + 14.dp.toPx(), o, style = Stroke(1.dp.toPx()))
        rotulo(o, "$tSol ${altS.roundToInt()}°", rr + 14.dp.toPx())
    }
    // Lluvia de meteoros: estelas que salen del radiante
    val rad = c.radiante
    if (rad != null && rad.second > 0 && !sinMovimiento) proy(rad.first, rad.second)?.let { qr ->
        val lunaArriba = if (c.luna.second > 0) c.il.fraccion else 0.0
        val efectiva = c.tasa * sin(rad.second * G) * (1 - 0.6 * lunaArriba)
        val n = if (efectiva >= 1) (efectiva / 8).roundToInt().coerceIn(1, 6) else 0
        for (k in 0 until n) {
            val ciclo = 12_000L; val desfase = tiempo + k * ciclo / n
            val t = (desfase % ciclo) / 700f; if (t > 1f) continue
            val sem = (desfase / ciclo) * 7.13 + k
            val ang = abs((sin(sem) * 43758.5453) % 1) * 2 * PI
            val dist = (40 + abs(sin(sem * 3.1)) * 140).dp.toPx()
            val o0 = qr + Offset((cos(ang) * dist).toFloat(), (sin(ang) * dist).toFloat())
            val o1 = o0 + Offset((cos(ang) * 120.dp.toPx() * t).toFloat(), (sin(ang) * 120.dp.toPx() * t).toFloat())
            val alfa = if (t < 0.3f) t / 0.3f else 1f - (t - 0.3f) / 0.7f
            drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = alfa)), o0, o1), o0, o1, 1.5.dp.toPx())
        }
    }
    // Suelo: la cordillera alrededor y el mar de nubes, con perspectiva atmosférica (no sobre la cámara: el suelo es el real)
    val monte = lerp(lerp(abajo, Color(0xFF141C28), 0.55f), Color(0xFF06090E), noche * 0.6f)
    if (!camara) for ((capa, alto, alfa) in listOf(Triple(0, 6.0, 0.55f), Triple(1, 3.4, 1f))) {
        val camino = Path(); var empezado = false
        var az = rumbo - fov.toDouble()
        while (az <= rumbo + fov) {
            val cresta = alto * (0.55 + 0.45 * sin(az * G * (if (capa == 1) 9 else 6) + capa * 2) * cos(az * G * (if (capa == 1) 4 else 3) + 1))
            proy((az + 360) % 360, cresta)?.let { o -> if (!empezado) { camino.moveTo(o.x, h + 10); empezado = true }; camino.lineTo(o.x, o.y) }
            az += 1.5
        }
        if (empezado) { camino.lineTo(w + 10, h + 10); camino.close(); drawPath(camino, monte.copy(alpha = alfa)) }
    }
    if (!camara) proy(rumbo.toDouble(), 0.6)?.let { o ->
        if (o.y < h) drawRect(
            Brush.verticalGradient(0f to Color.White.copy(alpha = 0.14f + 0.5f * dia), 0.12f to lerp(monte, Color.White, 0.06f + 0.4f * dia), 1f to lerp(monte, Color.Black, 0.35f), startY = o.y, endY = max(o.y + 1, h)),
            topLeft = Offset(0f, o.y), size = androidx.compose.ui.geometry.Size(w, h - o.y),
        )
    }
    // Horizonte con marcas cada 15° y puntos cardinales
    val linea = Color.White.copy(alpha = 0.3f + 0.2f * noche)
    for (az in 0 until 360 step 15) {
        val o = proy(az.toDouble(), 0.0) ?: continue
        if (o.x < -20 || o.x > w + 20) continue
        val card = az % 45 == 0
        drawLine(linea, o, o - Offset(0f, (if (card) 10 else 5).dp.toPx()), 1.dp.toPx())
        if (card) { val m = medidor.measure(cardinales[az / 45], etiqueta); drawText(m, topLeft = Offset(o.x - m.size.width / 2f, o.y - 16.dp.toPx() - m.size.height)) }
    }
    // Guías en el borde: hacia dónde girar para ver el sol, la luna o el radiante de la lluvia
    marca?.let { (az, al) -> proy(az, al)?.let { o ->
        val pulso = if (sinMovimiento) 0f else ((sin(tiempo / 260.0) + 1) * 4).toFloat()
        drawCircle(Color(0xFFC9A24A), radius = 34.dp.toPx() + pulso, center = o, style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))
    } }
    fun guia(az: Double, al: Double, t: String) {
        proy(az, al)?.let { if (it.x in 30f..(w - 30f) && it.y in 70.dp.toPx()..(h - 60.dp.toPx())) return }
        val dA = ((az - rumbo + 540) % 360) - 180; val dH = al - alt
        val ang = atan2(-dH, dA).toFloat()
        val mx = w / 2 - 34.dp.toPx(); val my = h / 2 - 70.dp.toPx()
        val k = min(mx / max(abs(cos(ang)), 1e-6f), my / max(abs(sin(ang)), 1e-6f))
        val p = Offset(w / 2 + cos(ang) * k, h / 2 + 8 + sin(ang) * k)
        translate(p.x, p.y) {
            rotate(ang * 180f / PI.toFloat(), Offset.Zero) {
                drawPath(Path().apply { moveTo(9.dp.toPx(), 0f); lineTo(-5.dp.toPx(), -6.dp.toPx()); lineTo(-5.dp.toPx(), 6.dp.toPx()); close() }, Color.White.copy(alpha = 0.85f))
            }
        }
        val m = medidor.measure(t.uppercase(), etiqueta.copy(fontSize = 10.sp))
        drawText(m, topLeft = Offset(p.x - cos(ang) * 22.dp.toPx() - m.size.width / 2f, p.y - sin(ang) * 18.dp.toPx() - m.size.height / 2f))
    }
    if (altS > -4) guia(c.sol.first, altS, tSol)
    if (c.luna.second > -2) guia(c.luna.first, c.luna.second, tLuna)
    if (rad != null && rad.second > 0) guia(rad.first, rad.second, tLluvia)
}


/** Botón píldora de la ventana del cielo (Ajustar, Modo noche). */
@Composable
private fun BotonCielo(texto: String, activo: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (activo) Color.White else Color.White.copy(alpha = 0.12f),
        contentColor = if (activo) Color(0xFF060A12) else Color.White, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
        modifier = Modifier.semantics { contentDescription = texto }) {
        Text(texto, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
    }
}
