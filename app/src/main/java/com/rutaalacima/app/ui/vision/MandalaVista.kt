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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.rutaalacima.app.data.social.Visibilidad
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.domain.model.MetodoCima
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Lo que se lee en un campamento: la frase de la casilla (el rótulo suele ser genérico, como "Meta 2026"). */
internal fun nombreCampamento(cv: CasillaVista): String = cv.casilla.afirmacion.trim().ifBlank { cv.casilla.titulo }

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
    /** Los 64 pasos con sus jornadas y el ritmo del año (Método Cima 9×52). */
    val pasos: List<MetodoCima.Paso> = MetodoCima.pasosDe(campamentos.map { it.casilla.id }, respuestas)
    val ritmo: MetodoCima.Estado = MetodoCima.calcular(pasos, LocalDate.now())
    fun pasoCima(i: Int, p: Int): MetodoCima.Paso = pasos[i * Mandala.PASOS + p]
    val tituloCumbre: String get() = cumbre?.casilla?.afirmacion?.ifBlank { null } ?: cumbreTexto
}

/**
 * El 9×9 del vision board: el bloque central son las 9 casillas del tablero (la cumbre y los 8
 * campamentos) y cada campamento abre su bloque con 8 pasos. Se puede compartir como imagen.
 */
@Composable
fun MandalaSeccion(
    datos: DatosMandala,
    onPaso: (casilla: VisionCasillaEntity, paso: Int, texto: String) -> Unit,
    onAvanzar: (casilla: VisionCasillaEntity, paso: Int) -> Unit,
    onSugerir: (VisionCasillaEntity) -> Unit,
    onAgregarCampamento: () -> Unit,
    onCompartir: (Visibilidad) -> Unit,
    compartiendo: Boolean,
) {
    var bloque by rememberSaveable { mutableStateOf(4) }
    var editando by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var preguntarCompartir by remember { mutableStateOf(false) }
    val pr = datos.progreso
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.mandala_titulo), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.mandala_intro), style = MaterialTheme.typography.bodyMedium)
        Cuadricula(datos, bloque) { bloque = it }
        Text(stringResource(R.string.mandala_progreso, pr.escritos, pr.hechos), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(progress = { pr.fraccion }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { preguntarCompartir = true }, enabled = !compartiendo && datos.campamentos.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(stringResource(if (compartiendo) R.string.mandala_compartiendo else R.string.mandala_compartir))
        }
        RitmoCima(datos.ritmo)
        Text(stringResource(R.string.mandala_toca_bloque), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Bloque(datos, bloque, onElegirBloque = { bloque = it }, onPaso = { i, p -> editando = i to p },
            onSugerir = onSugerir, onAgregarCampamento = onAgregarCampamento)
    }
    editando?.let { (i, p) ->
        datos.casilla(i)?.let { cv -> EditarPaso(
            numero = p + 1, campamento = nombreCampamento(cv), texto = datos.paso(i, p),
            paso = datos.pasoCima(i, p), dificultad = datos.ritmo.dificultad,
            onGuardar = { t -> onPaso(cv.casilla, p, t); editando = null },
            onAvanzar = { t -> if (t != datos.paso(i, p)) onPaso(cv.casilla, p, t); onAvanzar(cv.casilla, p) },
            onCancelar = { editando = null },
        ) }
    }
    if (preguntarCompartir) AlertDialog(
        onDismissRequest = { preguntarCompartir = false },
        title = { Text(stringResource(R.string.mandala_compartir)) },
        text = { Text(stringResource(R.string.mandala_compartir_ayuda)) },
        confirmButton = {
            TextButton(onClick = { preguntarCompartir = false; onCompartir(Visibilidad.PUBLICA) }) { Text(stringResource(R.string.mandala_compartir_publico)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { preguntarCompartir = false }) { Text(stringResource(R.string.cancelar)) }
                TextButton(onClick = { preguntarCompartir = false; onCompartir(Visibilidad.SEGUIDORES) }) { Text(stringResource(R.string.mandala_compartir_seguidores)) }
            }
        },
    )
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
                Text(cv?.let { nombreCampamento(it) } ?: "+", fontSize = tam, lineHeight = tam, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
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
                if (!chica) Text(datos.paso(cel.campamento, cel.paso), fontSize = tam, lineHeight = tam, textAlign = TextAlign.Center,
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
            else cv?.let { nombreCampamento(it) } ?: stringResource(R.string.mandala_campamento_vacio),
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

/** El ritmo del año: tramo, dificultad que pide un paso nuevo, ajuste y altura en milímetros. */
@Composable
private fun RitmoCima(r: MetodoCima.Estado) {
    val nf = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.metodo_titulo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(stringResource(R.string.metodo_altura, NumberFormat.getIntegerInstance().format(r.mm)), style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(stringResource(R.string.metodo_dificultad, r.dificultad), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(stringResource(R.string.metodo_tramo, r.tramo + 1, r.ganadosTramo, nf.format(r.esperadoTramo), r.diasParaAjuste),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(
            if (r.adelanto >= 0) stringResource(R.string.metodo_ritmo_adelante, nf.format(r.adelanto))
            else stringResource(R.string.metodo_ritmo_atras, nf.format(-r.adelanto)),
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
            color = if (r.adelanto >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(stringResource(R.string.metodo_explica), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
    }
}

@Composable
private fun EditarPaso(
    numero: Int, campamento: String, texto: String, paso: MetodoCima.Paso, dificultad: Int,
    onGuardar: (String) -> Unit, onAvanzar: (String) -> Unit, onCancelar: () -> Unit,
) {
    var t by remember { mutableStateOf(texto) }
    val requeridas = paso.requeridas ?: dificultad
    val hoyListo = LocalDate.now() in paso.jornadas
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.mandala_paso, numero)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(campamento, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(t, { t = it }, minLines = 2, modifier = Modifier.fillMaxWidth(), enabled = !paso.cumplido)
                if (paso.cumplido) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.metodo_ganado), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text(stringResource(R.string.metodo_jornadas, paso.jornadas.size, requeridas), style = MaterialTheme.typography.labelLarge)
                    LinearProgressIndicator(progress = { paso.jornadas.size.toFloat() / requeridas }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onAvanzar(t) }, enabled = t.isNotBlank() && !hoyListo, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (hoyListo) R.string.metodo_ya_hoy else R.string.metodo_avance_hoy))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(t) }) { Text(stringResource(R.string.guardar)) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
    )
}
