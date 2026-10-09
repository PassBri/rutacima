package com.rutaalacima.app.ui.vision

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.VisionCasillaEntity
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.domain.model.MetodoCima
import com.rutaalacima.app.domain.model.VisionBoard
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.FormaHoja
import com.rutaalacima.app.ui.theme.fondoPapel
import com.rutaalacima.app.ui.theme.hojaPapel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.rutaalacima.app.data.social.Visibilidad
import com.rutaalacima.app.data.social.TipoPost
import androidx.compose.runtime.LaunchedEffect
import java.io.File

/** Una casilla con la foto que la llena (ruta local o URL), si ya la tiene. */
data class CasillaVista(val casilla: VisionCasillaEntity, val foto: String?)

class VisionViewModel(private val c: AppContainer) : ViewModel() {
    val casillas = combine(c.vision.casillas, c.social.misPublicaciones) { cs, posts ->
        val fotos = posts.associate { it.id to it.foto }
        cs.map { CasillaVista(it, it.publicacionId?.let { id -> fotos[id] }?.takeIf { f -> f.isNotBlank() }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var armando by mutableStateOf(false)
    /** null = sin mensaje; true = lo armó la IA; false = se armó sin IA. */
    var conIa by mutableStateOf<Boolean?>(null)

    fun armar() = viewModelScope.launch {
        armando = true
        conIa = runCatching { c.vision.armar() }.getOrDefault(false)
        llenarCampamentosVacios()
        armando = false
    }

    /**
     * Al llenar el vision board se llena el 9×9: cada campamento que todavía no tiene ningún paso
     * recibe 8 pasos sugeridos (las acciones de su propósito y el banco de acciones de su eje).
     */
    private suspend fun llenarCampamentosVacios() {
        val todas = c.vision.casillas.first().sortedWith(compareBy({ it.orden }, { it.id }))
        val (_, campamentos) = Mandala.repartir(todas) { it.origen }
        val r = c.respuestas.cargar(Mandala.WORKBOOK)
        campamentos.filter { cs -> (0 until Mandala.PASOS).all { r[Mandala.clave(cs.id, it)].isNullOrBlank() } }
            .forEach { llenarPasos(it, r) }
    }

    var compartiendo by mutableStateOf(false)
        private set
    /** null = nada; true = publicado; false = no se pudo. */
    var compartido by mutableStateOf<Boolean?>(null)

    /** Publica la imagen del 9×9 en la comunidad como publicación de visión. */
    fun compartirMandala(ctx: android.content.Context, datos: DatosMandala, textos: MandalaImagen.Textos, vis: Visibilidad) = viewModelScope.launch {
        compartiendo = true
        compartido = runCatching {
            val f = withContext(Dispatchers.Default) { MandalaImagen.generar(ctx.applicationContext, datos, textos) }
            c.social.publicar(TipoPost.VISION, datos.tituloCumbre + "\n#MetodoCima9x52", Uri.fromFile(f), null, vis, metaTitulo = textos.etiqueta)
            f.delete()
        }.isSuccess
        compartiendo = false
    }

    /** Pasos de la mandala 9×9 (respuestas del workbook "mandala"). */
    val mandala = c.respuestas.observar(Mandala.WORKBOOK).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val cumbreFrase = c.perfil.perfil.map { it.cumbreFrase }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun guardarPaso(casilla: VisionCasillaEntity, paso: Int, texto: String) = viewModelScope.launch {
        c.respuestas.guardar(Mandala.WORKBOOK, Mandala.clave(casilla.id, paso), texto.trim())
    }

    /** Registra la jornada de hoy en un paso (Método Cima 9×52): el paso se gana al juntar las jornadas que pide. */
    fun avanzarPaso(casilla: VisionCasillaEntity, paso: Int, campamentos: List<Long>) = viewModelScope.launch {
        val r = c.respuestas.cargar(Mandala.WORKBOOK)
        val hoy = java.time.LocalDate.now()
        val ritmo = MetodoCima.calcular(MetodoCima.pasosDe(campamentos, r), hoy)
        val a = MetodoCima.avanzar(MetodoCima.leer(casilla.id, paso, r), hoy, ritmo.dificultad)
        c.respuestas.guardar(Mandala.WORKBOOK, MetodoCima.claveJornadas(casilla.id, paso), a.jornadas.sorted().joinToString(","))
        c.respuestas.guardar(Mandala.WORKBOOK, MetodoCima.claveRequeridas(casilla.id, paso), a.requeridas.toString())
        a.cumplidoEn?.let { c.respuestas.guardar(Mandala.WORKBOOK, Mandala.claveHecho(casilla.id, paso), it.toString()) }
    }

    /** Llena los pasos vacíos de un campamento: primero las acciones de su propósito, luego el banco de acciones de su eje. */
    fun sugerirPasos(casilla: VisionCasillaEntity) = viewModelScope.launch { llenarPasos(casilla, c.respuestas.cargar(Mandala.WORKBOOK)) }

    private suspend fun llenarPasos(casilla: VisionCasillaEntity, r: Map<String, String>) {
        val actuales = List(Mandala.PASOS) { r[Mandala.clave(casilla.id, it)].orEmpty() }
        val sugerencias = buildList {
            if (casilla.origen.startsWith("proposito:")) casilla.origen.removePrefix("proposito:").toLongOrNull()?.let { id ->
                addAll(c.planificador.acciones(id).first().map { it.texto })
            }
            addAll(runCatching { c.bancos.bancos().accionesDe(casilla.eje).map { it.texto }.shuffled() }.getOrDefault(emptyList()))
        }
        Mandala.completar(actuales, sugerencias).forEachIndexed { i, t ->
            if (t != actuales[i]) c.respuestas.guardar(Mandala.WORKBOOK, Mandala.clave(casilla.id, i), t)
        }
    }

    fun ponerFoto(cv: VisionCasillaEntity, uri: Uri) = c.appScope.launch { c.vision.ponerFoto(cv, uri) }
    fun guardar(cv: VisionCasillaEntity) = viewModelScope.launch { c.vision.guardar(cv) }
    fun nueva(t: String, a: String, s: String) = viewModelScope.launch { c.vision.nueva(t, a, s); llenarCampamentosVacios() }
    fun borrar(cv: VisionCasillaEntity) = viewModelScope.launch { c.vision.borrar(cv) }
}

/**
 * Mi vision board: la IA propone las casillas con la cumbre, los propósitos, las metas y los
 * ejes de la persona, y ella las llena con sus propias fotos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionScreen(onBack: () -> Unit) {
    val vm = rutaViewModel { VisionViewModel(it) }
    val ctx = LocalContext.current
    val casillas by vm.casillas.collectAsStateWithLifecycle()
    var paraFoto by remember { mutableStateOf<VisionCasillaEntity?>(null) }
    var editando by remember { mutableStateOf<VisionCasillaEntity?>(null) }
    var creando by remember { mutableStateOf(false) }
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val c = paraFoto
        if (uri != null && c != null) vm.ponerFoto(c, uri)
        paraFoto = null
    }
    fun pedirFoto(c: VisionCasillaEntity) {
        paraFoto = c
        elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    fun ideas(c: VisionCasillaEntity) {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(VisionBoard.urlIdeas(c.busqueda)))) }
    }
    val conFoto = casillas.count { it.foto != null }
    val pasos by vm.mandala.collectAsStateWithLifecycle()
    val cumbreFrase by vm.cumbreFrase.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.vision_titulo)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = {
                    if (casillas.isNotEmpty()) IconButton(onClick = { creando = true }) { Icon(Icons.Filled.Add, stringResource(R.string.vision_casilla_nueva)) }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                RutaCard {
                    Text(stringResource(R.string.vision_intro), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    if (casillas.isNotEmpty()) {
                        Text(stringResource(R.string.vision_progreso, conFoto, casillas.size), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { conFoto.toFloat() / casillas.size }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                    }
                    if (vm.armando) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.vision_armando))
                        }
                    } else if (casillas.isEmpty()) {
                        Button(onClick = { vm.armar() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.vision_armar))
                        }
                    } else {
                        OutlinedButton(onClick = { vm.armar() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.vision_rearmar))
                        }
                        Text(stringResource(R.string.vision_rearmar_ayuda), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (vm.conIa == false) {
                        Text(stringResource(R.string.vision_sin_ia), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (casillas.isNotEmpty()) item(span = { GridItemSpan(2) }) {
                val (cumbre, campamentos) = Mandala.repartir(casillas) { it.casilla.origen }
                val datos = DatosMandala(cumbre, cumbreFrase, campamentos, pasos)
                val nf = java.text.NumberFormat.getIntegerInstance()
                val textos = MandalaImagen.Textos(
                    etiqueta = stringResource(R.string.mandala_imagen_etiqueta),
                    avance = stringResource(R.string.mandala_progreso, datos.progreso.escritos, datos.progreso.hechos),
                    montana = stringResource(R.string.metodo_altura, nf.format(datos.ritmo.mm)),
                    marca = stringResource(R.string.mandala_imagen_marca),
                )
                RutaCard {
                    MandalaSeccion(
                        datos,
                        onPaso = { cs, p, t -> vm.guardarPaso(cs, p, t) },
                        onAvanzar = { cs, p -> vm.avanzarPaso(cs, p, campamentos.map { it.casilla.id }) },
                        onSugerir = { vm.sugerirPasos(it) },
                        onAgregarCampamento = { creando = true },
                        onCompartir = { vis -> vm.compartirMandala(ctx, datos, textos, vis) },
                        compartiendo = vm.compartiendo,
                    )
                }
            }
            if (casillas.isNotEmpty()) item(span = { GridItemSpan(2) }) {
                Text(stringResource(R.string.mandala_casillas_titulo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(casillas, key = { it.casilla.id }) { cv ->
                Casilla(cv, onFoto = { pedirFoto(cv.casilla) }, onIdeas = { ideas(cv.casilla) }, onEditar = { editando = cv.casilla })
            }
            if (casillas.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(stringResource(R.string.vision_foto_privada), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    vm.compartido?.let { ok ->
        LaunchedEffect(ok) {
            android.widget.Toast.makeText(ctx, ctx.getString(if (ok) R.string.mandala_compartido else R.string.mandala_no_compartido), android.widget.Toast.LENGTH_LONG).show()
            vm.compartido = null
        }
    }

    editando?.let { c ->
        EditarCasilla(
            titulo = c.titulo, afirmacion = c.afirmacion, sugerencia = c.sugerencia,
            onGuardar = { t, a, s -> vm.guardar(c.copy(titulo = t, afirmacion = a, sugerencia = s, busqueda = VisionBoard.palabras(a))); editando = null },
            onBorrar = { vm.borrar(c); editando = null },
            onCancelar = { editando = null },
        )
    }
    if (creando) {
        EditarCasilla(
            titulo = "", afirmacion = "", sugerencia = "",
            onGuardar = { t, a, s -> vm.nueva(t, a, s); creando = false },
            onBorrar = null,
            onCancelar = { creando = false },
        )
    }
}

@Composable
private fun Casilla(cv: CasillaVista, onFoto: () -> Unit, onIdeas: () -> Unit, onEditar: () -> Unit) {
    val c = cv.casilla
    val color = c.eje?.let(::colorEje) ?: MaterialTheme.colorScheme.primary
    val forma = remember { FormaHoja(doblez = 14.dp, radio = 6.dp) }
    Column(Modifier.fillMaxWidth().hojaPapel(doblez = 14.dp).clip(forma)) {
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 5f).clickable(onClick = onFoto)) {
            if (cv.foto != null) {
                val modelo: Any = if (cv.foto.startsWith("http")) cv.foto else File(cv.foto)
                AsyncImage(modelo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC1D120D)))))
                Text(
                    c.afirmacion, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp), maxLines = 4, overflow = TextOverflow.Ellipsis,
                )
            } else {
                Column(
                    Modifier.fillMaxSize().background(color.copy(alpha = 0.10f)).padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(c.afirmacion, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color,
                        maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Text(c.sugerencia, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 5, overflow = TextOverflow.Ellipsis)
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.AddAPhoto, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.vision_agregar_foto), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(c.titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (cv.foto == null) IconButton(onClick = onIdeas) { Icon(Icons.Filled.Search, stringResource(R.string.vision_ideas)) }
            IconButton(onClick = onEditar) { Icon(Icons.Filled.Edit, stringResource(R.string.vision_editar)) }
        }
    }
}

internal fun colorEje(codigo: String): Color = when (codigo) {
    "VOL" -> Color(0xFF8E3B26); "MAE" -> Color(0xFF5C4A8A); "VOZ" -> Color(0xFF2F6F7A)
    "VAL" -> Color(0xFF8A6A1F); "EVO" -> Color(0xFF3F7A4A); else -> Color(0xFF6B2A1A)
}

@Composable
private fun EditarCasilla(
    titulo: String, afirmacion: String, sugerencia: String,
    onGuardar: (String, String, String) -> Unit, onBorrar: (() -> Unit)?, onCancelar: () -> Unit,
) {
    var t by remember { mutableStateOf(titulo) }
    var a by remember { mutableStateOf(afirmacion) }
    var s by remember { mutableStateOf(sugerencia) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (onBorrar == null) R.string.vision_casilla_nueva else R.string.vision_editar)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(a, { a = it }, label = { Text(stringResource(R.string.vision_frase)) }, minLines = 2)
                OutlinedTextField(t, { t = it }, label = { Text(stringResource(R.string.vision_rotulo)) }, singleLine = true)
                OutlinedTextField(s, { s = it }, label = { Text(stringResource(R.string.vision_que_foto)) }, minLines = 2)
                if (onBorrar != null) TextButton(onClick = onBorrar) {
                    Text(stringResource(R.string.vision_borrar), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = a.isNotBlank(), onClick = { onGuardar(t.ifBlank { a.take(30) }, a.trim(), s.trim()) }) {
                Text(stringResource(R.string.guardar))
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
    )
}
