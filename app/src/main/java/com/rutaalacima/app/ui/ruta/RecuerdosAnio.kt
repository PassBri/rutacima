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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
    onPublicar: suspend (Uri, Visibilidad, String) -> Unit) {
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
    if (abierto) HistoriaRecuerdos(anio, fotos, fraseCumbre, metas, onPublicar) { abierto = false }
}

@Composable
private fun HistoriaRecuerdos(anio: Int, fotos: List<Post>, cumbre: String, metas: List<MetaAnualEntity>,
    onPublicar: suspend (Uri, Visibilidad, String) -> Unit, onCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    var indice by remember { mutableIntStateOf(0) }
    var progreso by remember { mutableFloatStateOf(0f) }
    var pausa by remember { mutableStateOf(false) }
    var audiencia by remember { mutableStateOf(false) }
    var ocupado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    val reducir = !ValueAnimator.areAnimatorsEnabled()
    val frase = cumbre.ifBlank { stringResource(R.string.rec_frase) }
    val titulo = stringResource(R.string.rec_mi_anio, anio)
    val marca = stringResource(R.string.rec_marca)
    val exito = stringResource(R.string.rec_exito)
    val error = stringResource(R.string.rec_error)
    val meses = remember(fotos) { fotos.map { fechaRecuerdo(it).monthValue }.distinct().size }
    val delAnio = remember(metas, anio) { metas.filter { it.anio == anio } }
    val logradas = delAnio.count { it.estado == "COMPLETADA" || it.avance >= 100 }
    LaunchedEffect(indice) { progreso = 0f }
    LaunchedEffect(indice, pausa, reducir) {
        if (!pausa && !reducir && indice < fotos.size) {
            var anterior = android.os.SystemClock.elapsedRealtime()
            while (progreso < 1f) {
                delay(16)
                val ahora = android.os.SystemClock.elapsedRealtime()
                progreso = (progreso + (ahora - anterior) / 4500f).coerceAtMost(1f)
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
                val archivo = mosaicoRecuerdos(contexto, anio, fotos, frase, marca)
                if (visibilidad == null) guardarRecuerdosGaleria(contexto, archivo)
                else onPublicar(Uri.fromFile(archivo), visibilidad, titulo)
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
            val lado = minOf(maxWidth, maxHeight, 720.dp)
            Box(Modifier.size(lado).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1D120D))) {
                if (indice < fotos.size) {
                    val p = fotos[indice]
                    val fecha = fechaRecuerdo(p)
                    var sellado by remember(indice) { mutableStateOf(false) }
                    LaunchedEffect(indice) { if (!reducir) delay(250); sellado = true }
                    val sello by animateFloatAsState(if (sellado || reducir) 1f else .6f,
                        if (reducir) snap() else tween(450), label = "selloRecuerdo")
                    AsyncImage(solicitudFotoVision(p.foto, lado), null,
                        Modifier.fillMaxSize().graphicsLayer {
                            val zoom = if (reducir) 1f else 1.02f + progreso * .1f
                            scaleX = zoom; scaleY = zoom
                            translationX = if (reducir) 0f else -size.width * progreso * .015f
                        }, contentScale = ContentScale.Crop)
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .8f)))).padding(20.dp)) {
                        Box(Modifier.size(56.dp).graphicsLayer { scaleX = sello; scaleY = sello; rotationZ = -8f }
                            .background(Brush.radialGradient(listOf(Color(0xFFA34352), Color(0xFF681C36))), CircleShape), contentAlignment = Alignment.Center) {
                            Text(fecha.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())).uppercase(Locale.getDefault()), color = Color.White,
                                style = MaterialTheme.typography.labelMedium)
                        }
                        val texto = p.metaTitulo.ifBlank { p.texto }.trim().take(140)
                        if (texto.isNotBlank()) Text("«$texto»", color = Color.White, style = MaterialTheme.typography.headlineSmall, maxLines = 4)
                        Text(fecha.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)), color = Color.White)
                    }
                    Box(Modifier.fillMaxSize().padding(top = 64.dp).pointerInput(indice) {
                        detectTapGestures(onLongPress = {}, onPress = { pausa = true; try { tryAwaitRelease() } finally { pausa = false } },
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
                        Text(stringResource(R.string.rec_cifras, fotos.size, meses, logradas, delAnio.size), color = Color.White)
                        Button(onClick = { audiencia = !audiencia }, enabled = !ocupado) { Text(stringResource(R.string.rec_publicar)) }
                        Row {
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
                        IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, stringResource(R.string.cerrar_brujula), tint = Color.White) }
                    }
                }
            }
        }
    }
}

/** Renderiza una sola imagen exportable; cada original se decodifica con tamaño acotado. */
private suspend fun mosaicoRecuerdos(context: Context, anio: Int, fotos: List<Post>, frase: String, marca: String): File = withContext(Dispatchers.IO) {
    val bitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb(29, 18, 13))
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val loader = ImageLoader(context)
    try {
        val seleccion = fotos.takeLast(9)
        val n = if (seleccion.size <= 1) 1 else if (seleccion.size <= 4) 2 else 3
        val lado = (1080 - 8 * (n + 1)) / n
        seleccion.forEachIndexed { i, post ->
            val resultado = loader.execute(ImageRequest.Builder(context)
                .data(if (post.foto.startsWith("http") || post.foto.startsWith("content:")) post.foto else File(post.foto))
                .size(lado, lado).allowHardware(false).build())
            check(resultado is SuccessResult)
            val foto = resultado.drawable.toBitmap()
            val s = minOf(foto.width, foto.height)
            val x = 8 + i % n * (lado + 8); val y = 8 + i / n * (lado + 8)
            canvas.drawBitmap(foto, Rect((foto.width - s) / 2, (foto.height - s) / 2, (foto.width + s) / 2, (foto.height + s) / 2),
                RectF(x.toFloat(), y.toFloat(), (x + lado).toFloat(), (y + lado).toFloat()), paint)
        }
        paint.shader = LinearGradient(0f, 550f, 0f, 1080f, intArrayOf(0x001D120D, 0xF51D120D.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 550f, 1080f, 1080f, paint)
        paint.shader = null; paint.color = android.graphics.Color.WHITE
        paint.typeface = Typeface.create("serif", Typeface.BOLD); paint.textSize = 150f
        canvas.drawText(anio.toString(), 56f, 930f, paint)
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL); paint.textSize = 38f
        val texto = android.text.TextUtils.ellipsize(frase, android.text.TextPaint(paint), 960f, android.text.TextUtils.TruncateAt.END).toString()
        canvas.drawText(texto, 60f, 988f, paint)
        paint.textSize = 26f; canvas.drawText(marca, 60f, 1034f, paint)
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
