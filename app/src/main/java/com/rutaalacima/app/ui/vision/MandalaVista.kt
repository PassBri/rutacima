package com.rutaalacima.app.ui.vision

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import com.rutaalacima.app.domain.model.Travesia
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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

private val ORO = Color(0xFFC9973B)
private val NIEVE = Color(0xFFFFF8EC)

/** Color de cada campamento fijo: el de su eje, y uno propio para Confluencia y Campamento Base. */
internal fun colorCampamento(codigo: String): Color = when (codigo) {
    "CON" -> Color(0xFF7A4A6B)
    "CAM" -> Color(0xFF4A5E7A)
    else -> colorEje(codigo)
}

/** Nombre de un campamento fijo en el idioma de la app. */
@Composable
internal fun nombreCodigo(codigo: String): String = stringResource(
    when (codigo) {
        "VOL" -> R.string.eje_vol; "MAE" -> R.string.eje_mae; "VOZ" -> R.string.eje_voz
        "VAL" -> R.string.eje_val; "EVO" -> R.string.eje_evo; "TRA" -> R.string.eje_tra
        "CON" -> R.string.brujula_con; else -> R.string.brujula_cam
    }
)

/** Nombre de la fase del Viaje Transformativo que corresponde al paso [p] (0..7). */
@Composable
internal fun nombreFase(p: Int): String = stringResource(
    listOf(R.string.fase_ori, R.string.fase_pre, R.string.fase_trv, R.string.fase_asc,
        R.string.fase_cim, R.string.fase_cnt, R.string.fase_des, R.string.fase_leg)[p.coerceIn(0, 7)]
)

/**
 * Lo que la Brújula de la Cima necesita saber: la cumbre, los 8 campamentos fijos (casillas del
 * vision board, null si falta alguno), los pasos y las fotos de evidencia (id de publicación → foto).
 */
class DatosMandala(
    val cumbre: CasillaVista?,
    val cumbreTexto: String,
    val campamentos: List<CasillaVista?>,
    val respuestas: Map<String, String>,
    val fotos: Map<String, String> = emptyMap(),
) {
    fun casilla(i: Int): CasillaVista? = campamentos.getOrNull(i)
    fun codigo(i: Int): String = Mandala.CAMPAMENTOS_FIJOS[i]
    fun color(i: Int): Color = colorCampamento(codigo(i))
    fun paso(i: Int, p: Int): String = casilla(i)?.let { respuestas[Mandala.clave(it.casilla.id, p)] }.orEmpty()
    fun estado(i: Int, p: Int) = Mandala.estado(casilla(i)?.casilla?.id, p, respuestas)
    /** Foto de evidencia del paso, si ya la tiene. */
    fun fotoPaso(i: Int, p: Int): String? = casilla(i)?.let { respuestas[Mandala.claveFoto(it.casilla.id, p)] }?.let { fotos[it] }?.takeIf { it.isNotBlank() }
    fun soltar(i: Int, p: Int): String = casilla(i)?.let { respuestas[Mandala.claveSoltar(it.casilla.id, p)] }.orEmpty()
    fun llevar(i: Int, p: Int): String = casilla(i)?.let { respuestas[Mandala.claveLlevar(it.casilla.id, p)] }.orEmpty()
    private val ids = campamentos.map { it?.casilla?.id }
    val progreso get() = Mandala.progreso(ids, respuestas)
    /** Los 64 pasos con sus jornadas y el ritmo del año (Método Cima 9×52). */
    val pasos: List<MetodoCima.Paso> = MetodoCima.pasosDe(ids, respuestas)
    val ritmo: MetodoCima.Estado = MetodoCima.calcular(pasos, LocalDate.now())
    fun pasoCima(i: Int, p: Int): MetodoCima.Paso = pasos[i * Mandala.PASOS + p]
    val tituloCumbre: String get() = cumbre?.casilla?.afirmacion?.ifBlank { null } ?: cumbreTexto
    /** Niebla, caída y confluencia de cada paso (Travesia). */
    fun niebla(i: Int, p: Int): Boolean = Travesia.enNiebla(pasoCima(i, p), LocalDate.now())
    fun diasQuieto(i: Int, p: Int): Int? = Travesia.diasQuieto(pasoCima(i, p), LocalDate.now())
    fun caida(i: Int, p: Int) = casilla(i)?.let { Travesia.caida(respuestas[Travesia.claveCaida(it.casilla.id, p)]) }
    fun enlaces(i: Int, p: Int): List<String> = casilla(i)?.let { Travesia.enlaces(respuestas[Travesia.claveEnlaces(it.casilla.id, p)], codigo(i)) }.orEmpty()
    val confluencias: Int get() = Travesia.confluencias(ids, respuestas)
    val idsCampamentos: List<Long?> get() = ids
    val enNiebla: Int get() = (0 until Mandala.CAMPAMENTOS).sumOf { i -> (0 until Mandala.PASOS).count { niebla(i, it) } }
    fun leccion(anio: Int, n: Int): String = respuestas[Travesia.claveLeccion(anio, n)].orEmpty()
    fun cierre(anio: Int): String? = respuestas[Travesia.claveCierre(anio)]?.takeIf { it.isNotBlank() }
    val evidencias: Int get() = (0 until Mandala.CAMPAMENTOS).sumOf { i -> (0 until Mandala.PASOS).count { fotoPaso(i, it) != null } }
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
    onAgregarCampamento: (codigo: String) -> Unit,
    onEvidencia: (casilla: VisionCasillaEntity, paso: Int) -> Unit,
    onPortal: (casilla: VisionCasillaEntity, paso: Int, soltar: String, llevar: String) -> Unit,
    onCaida: (casilla: VisionCasillaEntity, paso: Int, tipo: String) -> Unit,
    onEnlaces: (casilla: VisionCasillaEntity, paso: Int, enlaces: String) -> Unit,
    onGuia: (String) -> Unit,
    onCierre: (lecciones: List<String>, alDiario: Boolean) -> Unit,
    onNuevaMontana: () -> Unit,
    onCompartir: (Visibilidad) -> Unit,
    compartiendo: Boolean,
) {
    var bloque by rememberSaveable { mutableStateOf(4) }
    var editando by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var preguntarCompartir by remember { mutableStateOf(false) }
    val pr = datos.progreso
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.brujula_titulo), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.mandala_intro), style = MaterialTheme.typography.bodyMedium)
        Cuadricula(datos, bloque) { bloque = it }
        Text(stringResource(R.string.mandala_progreso, pr.escritos, pr.hechos), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(progress = { pr.fraccion }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.brujula_evidencias, datos.evidencias), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (datos.confluencias > 0) Text(stringResource(R.string.brujula_confluencias, datos.confluencias), style = MaterialTheme.typography.labelMedium,
            color = colorCampamento("CON"))
        if (datos.enNiebla > 0) Text(stringResource(R.string.brujula_en_niebla, datos.enNiebla, Travesia.NIEBLA_DIAS), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error)
        Button(onClick = { preguntarCompartir = true }, enabled = !compartiendo && datos.campamentos.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(stringResource(if (compartiendo) R.string.mandala_compartiendo else R.string.mandala_compartir))
        }
        RitmoCima(datos.ritmo)
        Text(stringResource(R.string.mandala_toca_bloque), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Bloque(datos, bloque, onElegirBloque = { bloque = it }, onPaso = { i, p -> editando = i to p },
            onSugerir = onSugerir, onAgregarCampamento = onAgregarCampamento)
        CierreAnio(datos, onCierre = onCierre, onNuevaMontana = onNuevaMontana, onGuia = onGuia, ocupado = compartiendo)
    }
    editando?.let { (i, p) ->
        datos.casilla(i)?.let { cv -> EditarPaso(
            numero = p + 1, fase = nombreFase(p), campamento = nombreCampamento(cv), texto = datos.paso(i, p),
            paso = datos.pasoCima(i, p), dificultad = datos.ritmo.dificultad,
            foto = datos.fotoPaso(i, p), soltar = datos.soltar(i, p), llevar = datos.llevar(i, p),
            codigo = datos.codigo(i), enlaces = datos.enlaces(i, p), diasQuieto = datos.diasQuieto(i, p),
            niebla = datos.niebla(i, p), caida = datos.caida(i, p)?.first,
            onEvidencia = { onEvidencia(cv.casilla, p) },
            onCaida = { tipo -> onCaida(cv.casilla, p, tipo) },
            onGuia = onGuia,
            onGuardar = { t, so, ll, en ->
                if (t != datos.paso(i, p)) onPaso(cv.casilla, p, t)
                if (so != datos.soltar(i, p) || ll != datos.llevar(i, p)) onPortal(cv.casilla, p, so, ll)
                if (en != datos.enlaces(i, p)) onEnlaces(cv.casilla, p, en.joinToString(","))
                editando = null
            },
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
            // Líneas de confluencia: del paso al centro de cada campamento que también activa
            val c = size.width / Mandala.LADO
            val con = colorCampamento("CON")
            Mandala.CELDAS.filter { it.tipo == Mandala.Tipo.PASO }.forEach { cel ->
                datos.enlaces(cel.campamento, cel.paso).forEach { cod ->
                    val b = Mandala.bloqueDe(Mandala.CAMPAMENTOS_FIJOS.indexOf(cod))
                    val desde = Offset((cel.col + 0.5f) * c, (cel.fila + 0.5f) * c)
                    val hasta = Offset(((b % 3) * 3 + 1.5f) * c, ((b / 3) * 3 + 1.5f) * c)
                    val ganado = datos.estado(cel.campamento, cel.paso) == Mandala.EstadoPaso.HECHO
                    drawLine(con.copy(alpha = if (ganado) 0.85f else 0.35f), desde, hasta, strokeWidth = (if (ganado) 2.5f else 1.5f).dp.toPx(),
                        pathEffect = if (ganado) null else androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                }
            }
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
                Text(cv?.let { nombreCampamento(it) } ?: ("+ " + nombreCodigo(datos.codigo(cel.campamento))), fontSize = tam, lineHeight = tam, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
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
            val evidencia = datos.fotoPaso(cel.campamento, cel.paso)
            Box(Modifier.fillMaxSize().background(fondo), contentAlignment = Alignment.Center) {
                // Paso ganado con foto: la evidencia ocupa la casilla (de la visión a la evidencia)
                if (evidencia != null) {
                    AsyncImage(modeloFoto(evidencia), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (!chica) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC1D120D)))))
                }
                if (!chica) Column(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("${cel.paso + 1} · ${nombreFase(cel.paso)}", fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold,
                        color = if (evidencia != null) Color.White else color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(datos.paso(cel.campamento, cel.paso), fontSize = tam, lineHeight = tam, textAlign = TextAlign.Center,
                        maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(),
                        color = if (evidencia != null) Color.White else MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(1.dp))
                }
                // Niebla: un paso empezado que lleva dos semanas quieto
                if (datos.niebla(cel.campamento, cel.paso)) Box(
                    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xE6F2F0EC), Color(0x99E8E6E1)))),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Cloud, stringResource(R.string.brujula_niebla), tint = Color(0xFF8A8580), modifier = Modifier.size(if (chica) 12.dp else 28.dp)) }
                // Confluencia: el paso también activa otros campamentos
                if (datos.enlaces(cel.campamento, cel.paso).isNotEmpty()) Icon(Icons.Filled.Hub, null, tint = colorCampamento("CON"),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(1.dp).size(if (chica) 7.dp else 14.dp))
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
    onSugerir: (VisionCasillaEntity) -> Unit, onAgregarCampamento: (String) -> Unit,
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
        if (campamento >= 0) Text(
            stringResource(R.string.brujula_campamento_de, nombreCodigo(datos.codigo(campamento)), Mandala.RUMBOS[campamento]),
            style = MaterialTheme.typography.labelLarge, color = datos.color(campamento),
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
                                { if (datos.casilla(cel.campamento) != null) onElegirBloque(Mandala.bloqueDe(cel.campamento)) else onAgregarCampamento(datos.codigo(cel.campamento)) }
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
            if (campamento >= 0 && cv == null) OutlinedButton(onClick = { onAgregarCampamento(datos.codigo(campamento)) }) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditarPaso(
    numero: Int, fase: String, campamento: String, texto: String, paso: MetodoCima.Paso, dificultad: Int,
    foto: String?, soltar: String, llevar: String,
    codigo: String, enlaces: List<String>, diasQuieto: Int?, niebla: Boolean, caida: String?,
    onEvidencia: () -> Unit, onCaida: (String) -> Unit, onGuia: (String) -> Unit,
    onGuardar: (String, String, String, List<String>) -> Unit, onAvanzar: (String) -> Unit, onCancelar: () -> Unit,
) {
    var t by remember { mutableStateOf(texto) }
    var en by remember { mutableStateOf(enlaces) }
    var eligiendoCaida by remember { mutableStateOf(false) }
    var so by remember { mutableStateOf(soltar) }
    var ll by remember { mutableStateOf(llevar) }
    val requeridas = paso.requeridas ?: dificultad
    val hoyListo = LocalDate.now() in paso.jornadas
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.brujula_paso_fase, numero, fase)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(campamento, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(t, { t = it }, minLines = 2, modifier = Modifier.fillMaxWidth(), enabled = !paso.cumplido)
                // Confluencia: los otros campamentos que también mueve este paso
                Text(stringResource(R.string.brujula_tambien_activa), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Mandala.CAMPAMENTOS_FIJOS.filter { it != codigo }.forEach { cod ->
                        FilterChip(selected = cod in en, onClick = { en = if (cod in en) en - cod else en + cod },
                            label = { Text(nombreCodigo(cod)) })
                    }
                }
                if (paso.cumplido) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.metodo_ganado), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    // Kit de Evidencias: la foto real reemplaza a la visión en el borde del 9×9
                    if (foto != null) AsyncImage(modeloFoto(foto), null, Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop)
                    OutlinedButton(onClick = onEvidencia, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.AddAPhoto, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (foto == null) R.string.brujula_evidencia_agregar else R.string.brujula_evidencia_cambiar))
                    }
                    // Portal (Portales y Transiciones): qué suelto y qué llevo antes del siguiente paso
                    Text(stringResource(R.string.brujula_portal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(so, { so = it }, label = { Text(stringResource(R.string.brujula_soltar)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(ll, { ll = it }, label = { Text(stringResource(R.string.brujula_llevar)) }, modifier = Modifier.fillMaxWidth())
                } else {
                    // Niebla y caídas: protocolos de los libros, sin culpas
                    if (niebla || caida != null) Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x1F8A8580)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (caida != null) {
                            Text(stringResource(R.string.brujula_caida_registrada, nombreCaida(caida), requeridas), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { onGuia("bono_anti_abandono") }) { Text(stringResource(R.string.brujula_anti_abandono)) }
                        } else {
                            Text(stringResource(R.string.brujula_niebla_texto, diasQuieto ?: Travesia.NIEBLA_DIAS), style = MaterialTheme.typography.bodySmall)
                            Row {
                                TextButton(onClick = { onGuia("niebla") }) { Text(stringResource(R.string.brujula_protocolos)) }
                                TextButton(onClick = { eligiendoCaida = !eligiendoCaida }) { Text(stringResource(R.string.brujula_me_cai)) }
                            }
                            if (eligiendoCaida) {
                                Text(stringResource(R.string.brujula_que_caida), style = MaterialTheme.typography.labelLarge)
                                Travesia.CAIDAS.forEach { tipo ->
                                    OutlinedButton(onClick = { onCaida(tipo); eligiendoCaida = false }, modifier = Modifier.fillMaxWidth()) { Text(nombreCaida(tipo)) }
                                }
                                TextButton(onClick = { onGuia("caidas") }) { Text(stringResource(R.string.brujula_guia_caidas)) }
                            }
                        }
                    }
                    Text(stringResource(R.string.metodo_jornadas, paso.jornadas.size, requeridas), style = MaterialTheme.typography.labelLarge)
                    LinearProgressIndicator(progress = { paso.jornadas.size.toFloat() / requeridas }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onAvanzar(t) }, enabled = t.isNotBlank() && !hoyListo, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (hoyListo) R.string.metodo_ya_hoy else R.string.metodo_avance_hoy))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(t, so.trim(), ll.trim(), en) }) { Text(stringResource(R.string.guardar)) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
    )
}

/** Nombre de un tipo de caída (Guía de Caídas). */
@Composable
private fun nombreCaida(tipo: String): String = stringResource(
    when (tipo) {
        "FIN" -> R.string.caida_fin; "EMO" -> R.string.caida_emo; "DEC" -> R.string.caida_dec
        "CAR" -> R.string.caida_car; "IDE" -> R.string.caida_ide; else -> R.string.caida_cir
    }
)

/**
 * Cierre del año (Desde la Cima): las 7 lecciones, guardar la Brújula del año en el diario de vida y
 * empezar una nueva montaña con los pasos que quedaron a medio camino.
 */
@Composable
private fun CierreAnio(datos: DatosMandala, onCierre: (List<String>, Boolean) -> Unit, onNuevaMontana: () -> Unit, onGuia: (String) -> Unit, ocupado: Boolean) {
    val anio = LocalDate.now().year
    var abierto by rememberSaveable { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }
    val lecciones = remember(abierto) { mutableStateListOf<String>().apply { addAll(List(Travesia.LECCIONES) { datos.leccion(anio, it) }) } }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.brujula_cierre_titulo, anio), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer)
        Text(
            if (datos.cierre(anio) != null) stringResource(R.string.brujula_cierre_hecho, anio) else stringResource(R.string.brujula_cierre_texto),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        if (!abierto) OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.brujula_cierre_abrir)) }
        else {
            lecciones.indices.forEach { n ->
                OutlinedTextField(lecciones[n], { lecciones[n] = it }, label = { Text(stringResource(R.string.brujula_leccion, n + 1)) },
                    modifier = Modifier.fillMaxWidth())
            }
            TextButton(onClick = { onGuia("desde_cima") }) { Text(stringResource(R.string.brujula_guia_desde_cima)) }
            Button(onClick = { onCierre(lecciones.toList(), true) }, enabled = !ocupado, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (ocupado) R.string.mandala_compartiendo else R.string.brujula_cierre_diario))
            }
            OutlinedButton(onClick = { confirmar = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.brujula_nueva_montana)) }
        }
    }
    if (confirmar) AlertDialog(
        onDismissRequest = { confirmar = false },
        title = { Text(stringResource(R.string.brujula_nueva_montana)) },
        text = { Text(stringResource(R.string.brujula_nueva_montana_ayuda)) },
        confirmButton = { TextButton(onClick = { confirmar = false; onNuevaMontana() }) { Text(stringResource(R.string.brujula_nueva_montana_si)) } },
        dismissButton = { TextButton(onClick = { confirmar = false }) { Text(stringResource(R.string.cancelar)) } },
    )
}
