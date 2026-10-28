package com.rutaalacima.app.ui.ruta

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.core.graphics.drawable.toBitmap
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.data.social.Visibilidad
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.vision.solicitudFotoVision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun fechaRecuerdo(p: Post) = Instant.ofEpochMilli(p.creadoEn).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun RecuerdosAnio(anio: Int, posts: List<Post>, fraseCumbre: String, metas: List<MetaAnualEntity>,
    onPublicar: suspend (Uri, Visibilidad, String, List<Post>) -> Unit) {
    val fotos = remember(posts, anio) { posts.filter { it.foto.isNotBlank() && it.anio == anio }.sortedBy { it.creadoEn } }
    var abierto by remember(anio) { mutableStateOf(false) }
    RutaCard {
        Text(stringResource(R.string.rec_titulo, anio), style = MaterialTheme.typography.titleLarge)
        if (fotos.isEmpty()) Text(stringResource(R.string.rec_vacio, anio))
        else {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { abierto = true }) {
                val ancho = maxWidth
                val lado = minOf(84.dp, ancho / 4)
                val tira = fotos.takeLast(6)
                Box(Modifier.fillMaxWidth().height(lado + 12.dp)) {
                    tira.forEachIndexed { i, p ->
                        val x = (ancho - lado) * (i.toFloat() / (tira.size - 1).coerceAtLeast(1))
                        AsyncImage(solicitudFotoVision(p.foto, lado), null, Modifier
                            .offset(x = x, y = 6.dp).size(lado).graphicsLayer { rotationZ = (i - 2.5f) * 2f }
                            .clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    }
                }
            }
            Button(onClick = { abierto = true }) { Text(stringResource(R.string.rec_ver)) }
        }
    }
    if (abierto) HistoriaRecuerdos(anio, fotos, fraseCumbre, metas, onPublicar, onCerrar = { abierto = false })
}

@Composable
fun HistoriaRecuerdos(anio: Int, fotos: List<Post>, cumbre: String, metas: List<MetaAnualEntity>,
    onPublicar: suspend (Uri, Visibilidad, String, List<Post>) -> Unit, onCerrar: () -> Unit,
    /** null = mis recuerdos; si no, el nombre de quien los publicó (sin publicar ni descargar). */
    autorAjeno: String? = null, metasTexto: String? = null) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    var indice by remember { mutableIntStateOf(0) }
    var progreso by remember { mutableFloatStateOf(0f) }
    var pausaBoton by remember { mutableStateOf(false) }
    var pausaDedo by remember { mutableStateOf(false) }
    val pausa = pausaBoton || pausaDedo
    val destello = remember { Animatable(0f) }
    var audiencia by remember { mutableStateOf(false) }
    var ocupado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    val reducir = !ValueAnimator.areAnimatorsEnabled()
    val frase = cumbre.ifBlank { stringResource(R.string.rec_frase) }
    val titulo = if (autorAjeno != null) stringResource(R.string.rec_de_autor, anio, autorAjeno) else stringResource(R.string.rec_mi_anio, anio)
    val marca = stringResource(R.string.rec_marca)
    val exito = stringResource(R.string.rec_exito)
    val error = stringResource(R.string.rec_error)
    val meses = remember(fotos) { fotos.map { fechaRecuerdo(it).monthValue }.distinct().size }
    val delAnio = remember(metas, anio) { metas.filter { it.anio == anio } }
    val privadas = fotos.count { it.visibilidad == Visibilidad.PRIVADA }
    val logradas = delAnio.count { it.estado == "COMPLETADA" || it.avance >= 100 }
    // Flashback: cada foto entra con un destello breve
    LaunchedEffect(indice) { progreso = 0f; if (!reducir) { destello.snapTo(.75f); destello.animateTo(0f, tween(320)) } }
    LaunchedEffect(indice, pausa, reducir) {
        if (!pausa && !reducir && indice < fotos.size) {
            var anterior = android.os.SystemClock.elapsedRealtime()
            while (progreso < 1f) {
                delay(16)
                val ahora = android.os.SystemClock.elapsedRealtime()
                progreso = (progreso + (ahora - anterior) / 1700f).coerceAtMost(1f)
                anterior = ahora
            }
            indice++
        }
    }
    fun preparar(visibilidad: Visibilidad?) {
        if (ocupado) return
        ocupado = true
        scope.launch {
            try {
                val usar = if (visibilidad == null) fotos else fotos.filter { it.visibilidad != Visibilidad.PRIVADA }
                check(usar.isNotEmpty())
                val archivo = mosaicoRecuerdos(contexto, anio, usar, frase, marca)
                if (visibilidad == null) guardarRecuerdosGaleria(contexto, archivo)
                else onPublicar(Uri.fromFile(archivo), visibilidad, titulo, usar)
                mensaje = exito
                audiencia = false
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { mensaje = error }
            finally { ocupado = false }
        }
    }
    val permisoGaleria = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) preparar(null) else mensaje = error
    }
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding(), contentAlignment = Alignment.Center) {
            // Vertical 3:4, como se ve y se publica en el celular
            val lado = minOf(maxWidth, maxHeight * 3f / 4f, 600.dp)
            Box(Modifier.width(lado).aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1D120D))) {
                if (indice < fotos.size) {
                    val p = fotos[indice]
                    val fecha = fechaRecuerdo(p)
                    AsyncImage(solicitudFotoVision(p.foto, lado), null,
                        Modifier.fillMaxSize().graphicsLayer {
                            val zoom = if (reducir) 1f else 1.14f - progreso * .12f
                            scaleX = zoom; scaleY = zoom
                            translationX = if (reducir) 0f else -size.width * progreso * .015f
                        }, contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().graphicsLayer { alpha = destello.value }.background(Color(0xFFFFF7E8)))
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .8f)))).padding(20.dp)) {
                        // El sello del mes (OCT…) se quitó: la fecha ya va abajo
                        val texto = p.metaTitulo.ifBlank { p.texto }.trim().take(140)
                        if (texto.isNotBlank()) Text("«$texto»", color = Color.White, style = MaterialTheme.typography.headlineSmall, maxLines = 4)
                        Text(fecha.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)), color = Color.White)
                    }
                    Box(Modifier.fillMaxSize().padding(top = 64.dp).pointerInput(indice) {
                        detectTapGestures(onLongPress = {}, onPress = { pausaDedo = true; try { tryAwaitRelease() } finally { pausaDedo = false } },
                            onTap = { punto -> indice = if (punto.x < size.width * .4f) (indice - 1).coerceAtLeast(0) else (indice + 1).coerceAtMost(fotos.size) })
                    })
                } else {
                    Column(Modifier.fillMaxSize()) {
                        fotos.takeLast(9).chunked(3).forEach { fila -> Row(Modifier.weight(1f)) {
                            fila.forEach { p -> AsyncImage(solicitudFotoVision(p.foto, lado / 3), null,
                                Modifier.weight(1f).fillMaxSize(), contentScale = ContentScale.Crop) }
                        } }
                    }
                    Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .65f)).padding(top = 64.dp, start = 20.dp, end = 20.dp, bottom = 12.dp)
                        .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(titulo, color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Text(frase, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        if (metasTexto != null) Text(stringResource(R.string.rec_cifras_ajenas, fotos.size, meses), color = Color.White)
                        else Text(stringResource(R.string.rec_cifras, fotos.size, meses, logradas, delAnio.size), color = Color.White)
                        if (autorAjeno == null) Button(onClick = { audiencia = !audiencia }, enabled = !ocupado) { Text(stringResource(R.string.rec_publicar)) }
                        if (autorAjeno == null && audiencia && privadas > 0) Text(stringResource(R.string.rec_privadas, privadas), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        if (autorAjeno != null) TextButton(onClick = { indice = 0 }) { Text(stringResource(R.string.rec_repetir), color = Color.White) }
                        else Row {
                            TextButton(onClick = {
                                if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(contexto, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                                    permisoGaleria.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                else preparar(null)
                            }, enabled = !ocupado) { Text(stringResource(R.string.rec_descargar), color = Color.White) }
                            TextButton(onClick = { indice = 0 }) { Text(stringResource(R.string.rec_repetir), color = Color.White) }
                        }
                        if (audiencia) {
                            Button(onClick = { preparar(Visibilidad.SEGUIDORES) }, enabled = !ocupado) { Text(stringResource(R.string.mandala_compartir_seguidores)) }
                            Button(onClick = { preparar(Visibilidad.PUBLICA) }, enabled = !ocupado) { Text(stringResource(R.string.rec_todos)) }
                        }
                        if (mensaje.isNotEmpty()) Text(mensaje, color = Color.White)
                    }
                }
                Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(fotos.size + 1) { i -> LinearProgressIndicator(
                            progress = { if (i < indice || indice == fotos.size) 1f else if (i == indice) progreso else 0f },
                            modifier = Modifier.weight(1f), color = Color.White, trackColor = Color.White.copy(alpha = .3f)) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(anio.toString(), color = Color.White)
                        Row {
                            IconButton(onClick = { pausaBoton = !pausaBoton }) {
                                Icon(if (pausaBoton) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    stringResource(if (pausaBoton) R.string.rec_seguir else R.string.rec_detener), tint = Color.White)
                            }
                            IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, stringResource(R.string.cerrar_brujula), tint = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

/** Renderiza una sola imagen exportable; cada original se decodifica con tamaño acotado. */
private suspend fun mosaicoRecuerdos(context: Context, anio: Int, fotos: List<Post>, frase: String, marca: String): File = withContext(Dispatchers.IO) {
    // Vertical 3:4 (1080×1440): el formato del celular cuando se publica
    val W = 1080; val H = 1440
    val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb(29, 18, 13))
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val loader = ImageLoader(context)
    try {
        val seleccion = fotos.takeLast(9)
        val n = if (seleccion.size <= 1) 1 else if (seleccion.size <= 4) 2 else 3
        val cw = (W - 8 * (n + 1)) / n; val ch = (H - 8 * (n + 1)) / n
        seleccion.take(n * n).forEachIndexed { i, post ->
            val resultado = loader.execute(ImageRequest.Builder(context)
                .data(if (post.foto.startsWith("http") || post.foto.startsWith("content:")) post.foto else File(post.foto))
                .size(cw, ch).allowHardware(false).build())
            check(resultado is SuccessResult)
            val foto = resultado.drawable.toBitmap()
            // Recorte tipo "cover" a la proporción de la celda
            val r = cw.toFloat() / ch
            val sw = minOf(foto.width.toFloat(), foto.height * r); val sh = sw / r
            val sx = ((foto.width - sw) / 2).toInt(); val sy = ((foto.height - sh) / 2).toInt()
            val x = 8 + i % n * (cw + 8); val y = 8 + i / n * (ch + 8)
            canvas.drawBitmap(foto, Rect(sx, sy, sx + sw.toInt(), sy + sh.toInt()),
                RectF(x.toFloat(), y.toFloat(), (x + cw).toFloat(), (y + ch).toFloat()), paint)
        }
        paint.shader = LinearGradient(0f, H * .6f, 0f, H.toFloat(), intArrayOf(0x001D120D, 0xF51D120D.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, H * .6f, W.toFloat(), H.toFloat(), paint)
        paint.shader = null; paint.color = android.graphics.Color.WHITE
        paint.typeface = Typeface.create("serif", Typeface.BOLD); paint.textSize = 150f
        canvas.drawText(anio.toString(), 56f, H - 150f, paint)
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL); paint.textSize = 38f
        val texto = android.text.TextUtils.ellipsize(frase, android.text.TextPaint(paint), 960f, android.text.TextUtils.TruncateAt.END).toString()
        canvas.drawText(texto, 60f, H - 92f, paint)
        paint.textSize = 26f; canvas.drawText(marca, 60f, H - 46f, paint)
        File(context.cacheDir, "suroute-recuerdos-$anio.jpg").also { archivo ->
            archivo.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        }
    } finally { bitmap.recycle(); loader.shutdown() }
}

private suspend fun guardarRecuerdosGaleria(context: Context, archivo: File) = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val valores = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, archivo.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SuRoute")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores))
        try {
            checkNotNull(resolver.openOutputStream(uri)).use { salida -> archivo.inputStream().use { it.copyTo(salida) } }
            valores.clear(); valores.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, valores, null, null)
        } catch (e: Exception) { resolver.delete(uri, null, null); throw e }
    } else {
        val destino = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SuRoute/${archivo.name}")
        check(destino.parentFile?.mkdirs() == true || destino.parentFile?.isDirectory == true)
        archivo.copyTo(destino, overwrite = true)
        MediaScannerConnection.scanFile(context, arrayOf(destino.absolutePath), arrayOf("image/jpeg"), null)
    }
}


/** Recuerdos publicados por alguien: el JSON de la publicación (v 99, tipo RECUERDOS) con sus fotos. */
object RecuerdosCompartidos {
    data class Datos(val anio: Int, val frase: String, val metas: String, val fotos: List<Triple<String, String, Long>>)
    fun leer(json: String): Datos? = runCatching {
        if (json.isBlank()) return null
        val o = kotlinx.serialization.json.Json.parseToJsonElement(json) as? kotlinx.serialization.json.JsonObject ?: return null
        fun txt(k: String) = (o[k] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
        if (txt("tipo") != "RECUERDOS") return null
        val fotos = (o["fotos"] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { e ->
            val f = e as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            val url = (f["f"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
            if (url.isBlank()) null else Triple(url, (f["t"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(),
                (f["c"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()?.toLong() ?: 0L)
        }
        if (fotos.isEmpty()) null else Datos(txt("anio").toIntOrNull() ?: 0, txt("frase"), txt("metas"), fotos)
    }.getOrNull()

    /** El JSON que se publica: las fotos ya subidas (direcciones públicas). */
    fun json(anio: Int, frase: String, metas: String, fotos: List<Triple<String, String, Long>>): String =
        kotlinx.serialization.json.buildJsonObject {
            put("v", kotlinx.serialization.json.JsonPrimitive(99)); put("tipo", kotlinx.serialization.json.JsonPrimitive("RECUERDOS"))
            put("anio", kotlinx.serialization.json.JsonPrimitive(anio)); put("frase", kotlinx.serialization.json.JsonPrimitive(frase.take(200)))
            put("metas", kotlinx.serialization.json.JsonPrimitive(metas))
            put("fotos", kotlinx.serialization.json.JsonArray(fotos.takeLast(40).map { (f, t, c) ->
                kotlinx.serialization.json.buildJsonObject {
                    put("f", kotlinx.serialization.json.JsonPrimitive(f)); put("t", kotlinx.serialization.json.JsonPrimitive(t.take(140)))
                    put("c", kotlinx.serialization.json.JsonPrimitive(c))
                }
            }))
        }.toString()
}

/** En el muro: "Ver los recuerdos de 2026" abre el flashback de quien los publicó. */
@Composable
fun BotonRecuerdosPost(post: Post, modifier: Modifier = Modifier) {
    val datos = remember(post.brujula) { RecuerdosCompartidos.leer(post.brujula) } ?: return
    var abierto by remember(post.id) { mutableStateOf(false) }
    Button(onClick = { abierto = true }, modifier = modifier) {
        Icon(Icons.Filled.PlayArrow, null); Text(stringResource(R.string.rec_ver_de, datos.anio))
    }
    if (abierto) {
        val fotos = remember(datos) {
            datos.fotos.mapIndexed { i, (f, t, c) ->
                Post(id = "${post.id}-$i", autorId = post.autorId, autorNombre = post.autorNombre, autorUsuario = post.autorUsuario,
                    tipo = post.tipo, texto = t, foto = f, anio = datos.anio, creadoEn = if (c > 0) c else post.creadoEn)
            }
        }
        HistoriaRecuerdos(datos.anio, fotos, datos.frase, emptyList(), { _, _, _, _ -> }, { abierto = false },
            autorAjeno = post.autorNombre.ifBlank { "SuRoute" }, metasTexto = datos.metas)
    }
}
