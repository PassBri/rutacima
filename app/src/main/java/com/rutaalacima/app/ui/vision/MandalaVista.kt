package com.rutaalacima.app.ui.vision

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.VisionCasillaEntity
import com.rutaalacima.app.domain.model.Mandala
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Colores para los campamentos sin eje. */
private val PALETA = listOf(
    Color(0xFF8E3B26), Color(0xFF5C4A8A), Color(0xFF2F6F7A), Color(0xFF8A6A1F),
    Color(0xFF3F7A4A), Color(0xFF6B2A1A), Color(0xFF7A4A6B), Color(0xFF4A5E7A),
)
private val ORO = Color(0xFFC9973B)
private val NIEVE = Color(0xFFFFF8EC)

/** Lo que la mandala necesita saber: la cumbre, los 8 campamentos (casillas del vision board) y los pasos. */
class DatosMandala(
    val cumbre: CasillaVista?,
    val cumbreTexto: String,
    val campamentos: List<CasillaVista>,
    val respuestas: Map<String, String>,
) {
    fun casilla(i: Int): CasillaVista? = campamentos.getOrNull(i)
    fun color(i: Int): Color = casilla(i)?.casilla?.eje?.let(::colorEje) ?: PALETA[i % PALETA.size]
    fun paso(i: Int, p: Int): String = casilla(i)?.let { respuestas[Mandala.clave(it.casilla.id, p)] }.orEmpty()
    fun estado(i: Int, p: Int) = Mandala.estado(casilla(i)?.casilla?.id, p, respuestas)
    val progreso get() = Mandala.progreso(campamentos.map { it.casilla.id }, respuestas)
    val tituloCumbre: String get() = cumbre?.casilla?.afirmacion?.ifBlank { null } ?: cumbreTexto
}

/**
 * La mandala 9×9 dentro del vision board: vista plana para escribir y marcar los pasos, y vista
 * 3D en la que la cuadrícula es una montaña escalonada que crece con el avance.
 */
@Composable
fun MandalaSeccion(
    datos: DatosMandala,
    onPaso: (casilla: VisionCasillaEntity, paso: Int, texto: String, hecho: Boolean) -> Unit,
    onSugerir: (VisionCasillaEntity) -> Unit,
    onAgregarCampamento: () -> Unit,
) {
    var en3d by rememberSaveable { mutableStateOf(false) }
    var bloque by rememberSaveable { mutableStateOf(4) }
    var editando by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val pr = datos.progreso
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.mandala_intro), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.mandala_progreso, pr.escritos, pr.hechos), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(progress = { pr.fraccion }, modifier = Modifier.fillMaxWidth())
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !en3d, onClick = { en3d = false }, shape = SegmentedButtonDefaults.itemShape(0, 2),
                icon = { Icon(Icons.Filled.GridView, null, Modifier.size(18.dp)) }) { Text(stringResource(R.string.mandala_2d)) }
            SegmentedButton(selected = en3d, onClick = { en3d = true }, shape = SegmentedButtonDefaults.itemShape(1, 2),
                icon = { Icon(Icons.Filled.Landscape, null, Modifier.size(18.dp)) }) { Text(stringResource(R.string.mandala_3d)) }
        }
        if (en3d) {
            Montana3D(datos)
            Text(stringResource(R.string.mandala_3d_ayuda), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Cuadricula(datos, bloque) { bloque = it }
            Text(stringResource(R.string.mandala_toca_bloque), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Bloque(datos, bloque, onElegirBloque = { bloque = it }, onPaso = { i, p -> editando = i to p },
                onSugerir = onSugerir, onAgregarCampamento = onAgregarCampamento)
        }
    }
    editando?.let { (i, p) ->
        datos.casilla(i)?.let { cv -> EditarPaso(
            numero = p + 1, campamento = cv.casilla.titulo, texto = datos.paso(i, p),
            hecho = datos.estado(i, p) == Mandala.EstadoPaso.HECHO,
            onGuardar = { t, h -> onPaso(cv.casilla, p, t, h); editando = null },
            onCancelar = { editando = null },
        ) }
    }
}

private fun modeloFoto(f: String): Any = if (f.startsWith("http")) f else File(f)

/** La cuadrícula completa de 9×9, con los bloques marcados. Tocar un bloque lo abre abajo. */
@Composable
private fun Cuadricula(datos: DatosMandala, elegido: Int, onBloque: (Int) -> Unit) {
    val linea = MaterialTheme.colorScheme.outline
    val vacio = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
        val lado: Dp = maxWidth / Mandala.LADO
        Column {
            for (f in 0 until Mandala.LADO) Row {
                for (col in 0 until Mandala.LADO) {
                    val cel = Mandala.celda(f, col)
                    val b = (f / 3) * 3 + col / 3
                    Box(
                        Modifier.size(lado).clickable { onBloque(b) }.border(0.5.dp, linea.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) { CeldaPlana(datos, cel, vacio, chica = true) }
                }
            }
        }
        // Bordes gruesos de los bloques y el bloque elegido
        Canvas(Modifier.fillMaxSize()) {
            val t = size.width / 3
            for (k in 0..3) {
                drawLine(linea, Offset(k * t, 0f), Offset(k * t, size.height), strokeWidth = 2.dp.toPx())
                drawLine(linea, Offset(0f, k * t), Offset(size.width, k * t), strokeWidth = 2.dp.toPx())
            }
            drawRect(ORO, topLeft = Offset((elegido % 3) * t, (elegido / 3) * t), size = androidx.compose.ui.geometry.Size(t, t),
                style = Stroke(width = 3.dp.toPx()))
        }
    }
}

/** El contenido de una celda: la cumbre, un campamento (con su foto) o un paso. */
@Composable
private fun CeldaPlana(datos: DatosMandala, cel: Mandala.Celda, vacio: Color, chica: Boolean) {
    val tam = if (chica) 7.sp else 12.sp
    val alto = if (chica) 3 else 5
    when (cel.tipo) {
        Mandala.Tipo.CUMBRE -> {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NIEVE, ORO))), contentAlignment = Alignment.Center) {
                datos.cumbre?.foto?.let { AsyncImage(modeloFoto(it), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                if (datos.cumbre?.foto != null) Box(Modifier.fillMaxSize().background(Color(0x66000000)))
                Text(datos.tituloCumbre.ifBlank { stringResource(R.string.mandala_cumbre) }, fontSize = tam, lineHeight = tam,
                    fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = alto, overflow = TextOverflow.Ellipsis,
                    color = if (datos.cumbre?.foto != null) Color.White else Color(0xFF3A1A10), modifier = Modifier.padding(2.dp))
            }
        }
        Mandala.Tipo.CAMPAMENTO -> {
            val cv = datos.casilla(cel.campamento)
            val color = datos.color(cel.campamento)
            Box(Modifier.fillMaxSize().background(if (cv != null) color else vacio), contentAlignment = Alignment.Center) {
                cv?.foto?.let {
                    AsyncImage(modeloFoto(it), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(color.copy(alpha = 0.55f)))
                }
                Text(cv?.casilla?.titulo ?: "+", fontSize = tam, lineHeight = tam, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    color = if (cv != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = alto, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(2.dp))
            }
        }
        Mandala.Tipo.PASO -> {
            val color = datos.color(cel.campamento)
            val estado = datos.estado(cel.campamento, cel.paso)
            val fondo = when (estado) {
                Mandala.EstadoPaso.VACIO -> vacio
                Mandala.EstadoPaso.ESCRITO -> color.copy(alpha = 0.14f)
                Mandala.EstadoPaso.HECHO -> color.copy(alpha = 0.42f)
            }
            Box(Modifier.fillMaxSize().background(fondo).padding(2.dp), contentAlignment = Alignment.Center) {
                Text(datos.paso(cel.campamento, cel.paso), fontSize = tam, lineHeight = tam, textAlign = TextAlign.Center,
                    maxLines = alto, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                if (estado == Mandala.EstadoPaso.HECHO) Icon(Icons.Filled.Check, null, tint = color,
                    modifier = Modifier.align(Alignment.TopEnd).size(if (chica) 8.dp else 16.dp))
            }
        }
    }
}

/** Un bloque de 3×3 en grande: el centro (cumbre y campamentos) o un campamento con sus 8 pasos. */
@Composable
private fun Bloque(
    datos: DatosMandala, bloque: Int,
    onElegirBloque: (Int) -> Unit, onPaso: (Int, Int) -> Unit,
    onSugerir: (VisionCasillaEntity) -> Unit, onAgregarCampamento: () -> Unit,
) {
    val vacio = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val campamento = Mandala.ANILLO.indexOf(bloque)          // −1 en el bloque central
    val cv = if (campamento >= 0) datos.casilla(campamento) else null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (campamento < 0) datos.tituloCumbre.ifBlank { stringResource(R.string.mandala_cumbre) }
            else cv?.casilla?.titulo ?: stringResource(R.string.mandala_campamento_vacio),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        )
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))) {
            val lado = maxWidth / 3
            Column {
                for (f in 0 until 3) Row {
                    for (col in 0 until 3) {
                        // Celda equivalente dentro de la cuadrícula completa
                        val cel = Mandala.celda((bloque / 3) * 3 + f, (bloque % 3) * 3 + col)
                        val accion: () -> Unit = when {
                            cel.tipo == Mandala.Tipo.PASO && cv != null -> { { onPaso(campamento, cel.paso) } }
                            cel.tipo == Mandala.Tipo.CAMPAMENTO && cel.copia -> {
                                { if (datos.casilla(cel.campamento) != null) onElegirBloque(Mandala.bloqueDe(cel.campamento)) else onAgregarCampamento() }
                            }
                            cel.tipo == Mandala.Tipo.CAMPAMENTO -> { { onElegirBloque(4) } }
                            else -> { {} }
                        }
                        Box(Modifier.size(lado).padding(2.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = accion), contentAlignment = Alignment.Center) {
                            CeldaPlana(datos, cel, vacio, chica = false)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cv != null) OutlinedButton(onClick = { onSugerir(cv.casilla) }) {
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.mandala_sugerir))
            }
            if (campamento >= 0 && cv == null) OutlinedButton(onClick = onAgregarCampamento) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.mandala_agregar_campamento))
            }
            if (campamento >= 0) TextButton(onClick = { onElegirBloque(4) }) { Text(stringResource(R.string.mandala_ver_centro)) }
        }
    }
}

@Composable
private fun EditarPaso(numero: Int, campamento: String, texto: String, hecho: Boolean, onGuardar: (String, Boolean) -> Unit, onCancelar: () -> Unit) {
    var t by remember { mutableStateOf(texto) }
    var h by remember { mutableStateOf(hecho) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.mandala_paso, numero)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(campamento, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(t, { t = it }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(enabled = t.isNotBlank()) { h = !h }) {
                    Checkbox(checked = h && t.isNotBlank(), onCheckedChange = { h = it }, enabled = t.isNotBlank())
                    Text(stringResource(R.string.mandala_paso_hecho))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(t, h) }) { Text(stringResource(R.string.guardar)) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/**
 * La mandala como montaña: 81 columnas en perspectiva, más altas hacia la cumbre. Se dibuja de
 * atrás hacia adelante (algoritmo del pintor) y gira sola hasta que la persona la arrastra.
 */
@Composable
private fun Montana3D(datos: DatosMandala) {
    var angulo by remember { mutableFloatStateOf((PI / 5).toFloat()) }
    var inclinacion by remember { mutableFloatStateOf(0.62f) }
    var girarSola by remember { mutableStateOf(true) }
    LaunchedEffect(girarSola) {
        var antes = 0L
        while (girarSola) withFrameNanos { t ->
            if (antes != 0L) angulo += (t - antes) / 1e9f * 0.25f
            antes = t
        }
    }
    val vacio = MaterialTheme.colorScheme.surfaceVariant
    val pr = datos.progreso
    val descripcion = stringResource(R.string.mandala_progreso, pr.escritos, pr.hechos)
    // Cada columna: celda, altura y color de la cara de arriba
    val columnas = Mandala.CELDAS.map { cel ->
        val estado = if (cel.tipo == Mandala.Tipo.PASO) datos.estado(cel.campamento, cel.paso) else Mandala.EstadoPaso.HECHO
        val color = when (cel.tipo) {
            Mandala.Tipo.CUMBRE -> NIEVE
            Mandala.Tipo.CAMPAMENTO -> if (datos.casilla(cel.campamento) != null) datos.color(cel.campamento) else vacio
            Mandala.Tipo.PASO -> when (estado) {
                Mandala.EstadoPaso.VACIO -> vacio
                Mandala.EstadoPaso.ESCRITO -> lerp(vacio, datos.color(cel.campamento), 0.35f)
                Mandala.EstadoPaso.HECHO -> lerp(datos.color(cel.campamento), ORO, 0.25f)
            }
        }
        Triple(cel, Mandala.altura(cel, estado), color)
    }
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFDDE7F0), Color(0xFFF6EBDD))))
            .semantics { contentDescription = descripcion }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { girarSola = false }) { cambio, d ->
                    cambio.consume()
                    angulo += d.x * 0.01f
                    inclinacion = (inclinacion - d.y * 0.004f).coerceIn(0.25f, 1.25f)
                }
            },
    ) {
        val s = size.width / 13.5f
        val cx = size.width / 2; val cy = size.height * 0.60f
        val ca = cos(angulo); val sa = sin(angulo)
        val st = sin(inclinacion); val ct = cos(inclinacion)
        val zEsc = 0.62f
        fun p(x: Float, y: Float, z: Float): Offset {
            val rx = x * ca - y * sa; val ry = x * sa + y * ca
            return Offset(cx + rx * s, cy + ry * s * st - z * zEsc * s * ct)
        }
        fun cara(pts: List<Offset>, color: Color) {
            val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close() }
            drawPath(path, color)
            drawPath(path, Color(0x22000000), style = Stroke(width = 0.6f))
        }
        val m = 0.47f
        columnas.sortedBy { (cel, _, _) -> val x = cel.col - 4f; val y = cel.fila - 4f; x * sa + y * ca }.forEach { (cel, alto, color) ->
            val x = cel.col - 4f; val y = cel.fila - 4f
            // Caras laterales que miran hacia quien observa (normal con componente hacia adelante)
            listOf(
                Triple(0f, 1f, listOf(x - m to y + m, x + m to y + m)),
                Triple(0f, -1f, listOf(x + m to y - m, x - m to y - m)),
                Triple(1f, 0f, listOf(x + m to y + m, x + m to y - m)),
                Triple(-1f, 0f, listOf(x - m to y - m, x - m to y + m)),
            ).forEach { (nx, ny, borde) ->
                val haciaAdelante = nx * sa + ny * ca
                if (haciaAdelante > 0f) {
                    val luz = 0.55f + 0.25f * (nx * ca - ny * sa).coerceIn(-1f, 1f)
                    val (a, b) = borde
                    cara(listOf(p(a.first, a.second, 0f), p(b.first, b.second, 0f), p(b.first, b.second, alto), p(a.first, a.second, alto)),
                        lerp(Color.Black, color, luz))
                }
            }
            cara(listOf(p(x - m, y - m, alto), p(x + m, y - m, alto), p(x + m, y + m, alto), p(x - m, y + m, alto)), color)
            if (cel.tipo == Mandala.Tipo.CUMBRE) {
                val c = p(x, y, alto)
                drawCircle(ORO, radius = s * 0.18f, center = c)
            }
        }
    }
}
