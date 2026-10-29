package com.rutaalacima.app.ui.comunidad

import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import com.rutaalacima.app.ui.theme.fondoPapel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.avance
import com.rutaalacima.app.data.social.TipoPost
import com.rutaalacima.app.data.social.Visibilidad
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.ui.components.ChipSelector
import com.rutaalacima.app.ui.components.MontanaArte
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.i18n.texto
import com.rutaalacima.app.ui.theme.asColor
import com.rutaalacima.app.util.hoy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PublicarViewModel(private val c: AppContainer, tipoInicial: String) : ViewModel() {
    var tipo by mutableStateOf(runCatching { TipoPost.valueOf(tipoInicial) }.getOrDefault(TipoPost.LOGRO))
    var texto by mutableStateOf("")
    var foto by mutableStateOf<Uri?>(null)
    /** Varias fotos o un video (carrusel). */
    var medios by mutableStateOf<List<Uri>>(emptyList())
    var eje by mutableStateOf<Eje?>(null)
    var visibilidad by mutableStateOf(Visibilidad.PUBLICA)
    var meta by mutableStateOf("")
    /** Año y mes del recuerdo (null = hoy). */
    var anio by mutableStateOf<Int?>(null)
    var mes by mutableStateOf<Int?>(null)
    var metas by mutableStateOf<List<String>>(emptyList())
        private set
    var publicando by mutableStateOf(false)
        private set
    /** Comunidad viva: el camino que se mostrará, el compromiso (días) y la carta sellada (meses). */
    var camino by mutableStateOf(com.rutaalacima.app.data.social.Camino(0, 0))
        private set
    var compromisoDias by mutableStateOf(0)
    var selloMeses by mutableStateOf(0)

    init {
        viewModelScope.launch { camino = com.rutaalacima.app.data.social.ComunidadViva.caminoActual(c) }
        // Metas activas para vincular la publicación (del mes, del año y propósitos).
        viewModelScope.launch {
            val h = hoy()
            val m = c.planAnual.metasMes(h.year, h.monthValue).first().filter { it.avance() < 1f }.map { it.texto }
            val a = c.planificador.metas(h.year).first().map { it.titulo }
            val p = c.planificador.propositos.first().map { it.titulo }
            metas = (m + a + p).distinct().take(12)
        }
    }

    fun publicar(alTerminar: () -> Unit) {
        if (texto.isBlank() && foto == null && medios.isEmpty()) return
        publicando = true
        viewModelScope.launch {
            val h = hoy()
            val extras = com.rutaalacima.app.data.social.ExtrasPost(
                camino = camino.takeUnless { it.vacio },
                compromiso = compromisoDias.takeIf { it > 0 }?.let { com.rutaalacima.app.data.social.Compromiso(h.plusDays(it.toLong()).toString()) },
                sello = selloMeses.takeIf { it > 0 }?.let { h.plusMonths(it.toLong()).toString() },
            )
            runCatching { c.social.publicar(tipo, texto, foto, eje?.codigo, visibilidad, meta, anio ?: h.year, mes, medios = medios, extras = extras) }
            publicando = false
            alTerminar()
        }
    }
}

/** Publicar: foto + texto + tipo (logro, evidencia, vision board, meta, reflexión) + eje + meta vinculada. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicarScreen(tipoInicial: String, onBack: () -> Unit, metaInicial: String? = null, textoInicial: String? = null, anioInicial: Int? = null, mesInicial: Int? = null) {
    val vm = rutaViewModel(key = "publicar-$tipoInicial-${metaInicial.orEmpty()}-${textoInicial.orEmpty().hashCode()}-${anioInicial}-${mesInicial}") { PublicarViewModel(it, tipoInicial) }
    androidx.compose.runtime.LaunchedEffect(metaInicial, textoInicial) {
        if (metaInicial != null && vm.meta.isBlank()) vm.meta = metaInicial
        if (textoInicial != null && vm.texto.isBlank()) vm.texto = textoInicial
        if (anioInicial != null) { vm.anio = anioInicial; vm.mes = mesInicial }
    }
    // Elegir → editar (formato cuadrado, vertical 4:5 u horizontal; recorte, giro y filtro) → usar
    val ctxPub = androidx.compose.ui.platform.LocalContext.current
    var editando by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null) }
    var proporcion by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableFloatStateOf(1f) }
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) editando = uri }
    editando?.let { uri ->
        com.rutaalacima.app.ui.components.EditorFoto(uri, com.rutaalacima.app.ui.components.FormatosFoto.PUBLICACION, ladoMax = 1440,
            onListo = { bytes ->
                val f = java.io.File(ctxPub.cacheDir, "editada-${System.currentTimeMillis()}.jpg").apply { writeBytes(bytes) }
                android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { o ->
                    android.graphics.BitmapFactory.decodeFile(f.absolutePath, o); if (o.outHeight > 0) proporcion = o.outWidth.toFloat() / o.outHeight
                }
                vm.foto = android.net.Uri.fromFile(f); editando = null
            },
            onCancelar = { editando = null })
    }

    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                title = {
                    Column {
                        Text(stringResource(R.string.nueva_publicacion))
                        // Recuerdo de otro año: se guarda en ese año del diario de vida
                        vm.anio?.takeIf { it != hoy().year }?.let { Text(stringResource(R.string.album_agregar, it), style = MaterialTheme.typography.labelMedium) }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = {
                    if (vm.publicando) CircularProgressIndicator(Modifier.size(22.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                    else TextButton(onClick = { vm.publicar(onBack) }, enabled = vm.texto.isNotBlank() || vm.foto != null || vm.medios.isNotEmpty()) {
                        Text(stringResource(R.string.publicar))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ChipSelector(null, TipoPost.entries, vm.tipo, { it.texto() }, { vm.tipo = it })
                Text(
                    stringResource(when (vm.tipo) {
                        TipoPost.LOGRO -> R.string.tipo_logro_ayuda
                        TipoPost.EVIDENCIA -> R.string.tipo_evidencia_ayuda
                        TipoPost.VISION -> R.string.tipo_vision_ayuda
                        TipoPost.META -> R.string.tipo_meta_ayuda
                        TipoPost.REFLEXION -> R.string.tipo_reflexion_ayuda
                    }),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                val forma = RoundedCornerShape(20.dp)
                Box(
                    Modifier.fillMaxWidth().aspectRatio(if (vm.foto != null) proporcion else 1f).clip(forma)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma)
                        .clickable { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center,
                ) {
                    val f = vm.foto
                    if (f != null) AsyncImage(f, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else MontanaArte("nueva-${vm.tipo}", Modifier.fillMaxSize()) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddAPhoto, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.agregar_foto), color = androidx.compose.ui.graphics.Color.White,
                                style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                if (vm.foto != null) Row {
                    TextButton(onClick = { vm.foto?.let { editando = it } }) { Text(stringResource(R.string.ef_editar)) }
                    TextButton(onClick = { vm.foto = null }) { Text(stringResource(R.string.quitar_foto)) }
                }
                // Varias fotos o un video corto: carrusel en Cimas
                val ctxMedios = androidx.compose.ui.platform.LocalContext.current
                val videoLargo = stringResource(R.string.pub_video_largo)
                val elegirMedios = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
                    if (uris.isEmpty()) return@rememberLauncherForActivityResult
                    val videos = uris.filter { ctxMedios.contentResolver.getType(it).orEmpty().startsWith("video/") }
                    val largo = videos.any { u -> runCatching {
                        android.media.MediaMetadataRetriever().run { setDataSource(ctxMedios, u); val d = extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0; release(); d > 60_500 }
                    }.getOrDefault(false) }
                    if (largo) com.rutaalacima.app.ui.theme.AvisoGlobal.mostrar(videoLargo)
                    else { vm.medios = if (videos.isNotEmpty()) listOf(videos.first()) else uris; vm.foto = null }
                }
                OutlinedButton(onClick = { elegirMedios.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AddAPhoto, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.pub_medios))
                }
                Text(stringResource(R.string.pub_medios_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (vm.medios.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                        items(vm.medios.size) { k ->
                            val u = vm.medios[k]
                            Box(Modifier.size(72.dp, 90.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                AsyncImage(u, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                IconButton(onClick = { vm.medios = vm.medios - u }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) {
                                    Icon(Icons.Filled.Close, stringResource(R.string.quitar_foto), tint = androidx.compose.ui.graphics.Color.White)
                                }
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(vm.texto, { vm.texto = it }, label = { Text(stringResource(R.string.que_quieres_compartir)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
            // La meta recién cumplida ya no está entre las activas: se agrega para que se vea elegida
            val opciones = (listOfNotNull(vm.meta.ifBlank { null }) + vm.metas).distinct()
            if (opciones.isNotEmpty()) {
                item {
                    ChipSelector(stringResource(R.string.vincular_meta), listOf("") + opciones, vm.meta,
                        { it.ifBlank { stringResource(R.string.ninguna) }.take(28) }, { vm.meta = it })
                }
            }
            item {
                ChipSelector(stringResource(R.string.eje), listOf<Eje?>(null) + Eje.entries, vm.eje, { it?.texto() ?: "—" },
                    { vm.eje = it }, color = { it?.color?.asColor() ?: androidx.compose.ui.graphics.Color.Gray })
            }
            item {
                ChipSelector(stringResource(R.string.quien_lo_ve), Visibilidad.entries, vm.visibilidad, { it.texto() }, { vm.visibilidad = it })
                Text(stringResource(R.string.siempre_en_tu_diario), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            // Comunidad viva: el camino detrás, un compromiso con fecha o una carta al yo futuro
            item {
                val cam = vm.camino
                androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)) {
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.Top) {
                        Icon(Icons.Filled.Terrain, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                        Text(if (cam.vacio) stringResource(R.string.cv_camino_vacio) else stringResource(R.string.cv_camino_nota, cam.racha, cam.ganados),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                ChipSelector(stringResource(R.string.cv_compromiso), listOf(0, 7, 30, 90), vm.compromisoDias,
                    { if (it == 0) stringResource(R.string.ninguna) else stringResource(R.string.cv_dias, it) }, { vm.compromisoDias = it })
                Text(stringResource(R.string.cv_compromiso_ayuda), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            item {
                ChipSelector(stringResource(R.string.cv_sellar), listOf(0, 1, 6, 12), vm.selloMeses,
                    { if (it == 0) stringResource(R.string.cv_no_sellar) else stringResource(R.string.cv_meses, it) }, { vm.selloMeses = it })
                Text(stringResource(R.string.cv_sellar_ayuda), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            item {
                Button(onClick = { vm.publicar(onBack) }, modifier = Modifier.fillMaxWidth(), enabled = !vm.publicando && (vm.texto.isNotBlank() || vm.foto != null || vm.medios.isNotEmpty())) {
                    Text(stringResource(R.string.publicar))
                }
            }
        }
    }
}
