package com.rutaalacima.app.ui.vision

import android.animation.ValueAnimator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.VisionCasillaEntity
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.ui.theme.hojaPapel
import kotlinx.coroutines.delay

/** El modo persiste como texto, para poder ampliar sus estados sin depender de ordinales. */
enum class ModoTarjetas(val clave: String) {
    IMAGEN("imagen"), TEXTO("texto"), EDICION("edicion");
    val muestraDorso: Boolean get() = this != IMAGEN
}

internal enum class CampoTarjeta { FRASE, ROTULO, SUGERENCIA, QUITAR_FOTO }

internal data class AccionesTarjeta(
    val onFoto: (VisionCasillaEntity) -> Unit,
    val onCampo: (Long, CampoTarjeta, String) -> Unit,
    val onApodo: (Long, String) -> Unit,
    val onPorcentaje: (Long, Int) -> Unit,
    val onBorrar: (VisionCasillaEntity) -> Unit,
    val onIdeas: (VisionCasillaEntity) -> Unit,
    val onMeta: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectorModoTarjetas(modo: ModoTarjetas, onModo: (ModoTarjetas) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier) {
        val opciones = remember {
            listOf(R.string.vision_modo_imagen to Icons.Outlined.Image,
                R.string.vision_modo_texto to Icons.Filled.FormatQuote,
                R.string.vision_modo_edicion to Icons.Outlined.Edit)
        }
        opciones.forEachIndexed { i, (etiqueta, icono) ->
            SegmentedButton(selected = modo == ModoTarjetas.entries[i], onClick = { onModo(ModoTarjetas.entries[i]) },
                shape = SegmentedButtonDefaults.itemShape(i, 3),
                icon = { Icon(icono, null, Modifier.size(16.dp)) }) {
                Text(stringResource(etiqueta), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NavegacionVision(modo: ModoTarjetas, onModo: (ModoTarjetas) -> Unit, habilitada: Boolean, onSeccion: (Int) -> Unit, onGrande: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()) {
        listOf(1, 9, 81).forEachIndexed { i, numero ->
            val descripcion = stringResource(listOf(R.string.vision_seccion_cumbre, R.string.vision_seccion_rosa, R.string.brujula_titulo)[i])
            TextButton(onClick = { onSeccion(i) }, enabled = habilitada, modifier = Modifier.semantics { contentDescription = descripcion }) {
                Text(stringResource(R.string.vision_nivel_numero, numero))
            }
        }
        SelectorModoTarjetas(modo, onModo)
        androidx.compose.material3.IconButton(onClick = onGrande, enabled = habilitada) {
            Icon(Icons.Filled.Fullscreen, stringResource(R.string.vision_ver_grande))
        }
    }
}

@Composable
internal fun RosaVision(
    cumbre: CasillaVista?, campamentos: List<CasillaVista?>, onCrear: (String) -> Unit,
    tarjeta: @Composable (CasillaVista, Int, String?, Modifier) -> Unit,
) {
    Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { columna ->
                    key(fila, columna) {
                        val celda = Mandala.celda(3 + fila, 3 + columna)
                        val centro = celda.tipo == Mandala.Tipo.CUMBRE
                        val cv = if (centro) cumbre else campamentos[celda.campamento]
                        val forma = RoundedCornerShape(12.dp)
                        val modificador = Modifier.weight(1f).then(if (centro) Modifier.border(2.dp, Color(0xFFC9973B), forma) else Modifier)
                        if (cv != null) key(cv.casilla.id) { tarjeta(cv, fila * 3 + columna, if (centro) null else Mandala.RUMBOS[celda.campamento], modificador) }
                        else {
                            val codigo = if (centro) null else Mandala.CAMPAMENTOS_FIJOS[celda.campamento]
                            val color = codigo?.let(::colorCampamento) ?: MaterialTheme.colorScheme.primary
                            Box(modificador.aspectRatio(1f).clip(forma).background(color.copy(alpha = .06f))
                                .clickable(enabled = codigo != null) { codigo?.let(onCrear) }, contentAlignment = Alignment.Center) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawRoundRect(color.copy(alpha = .6f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                                        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
                                }
                                Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Add, null, tint = color)
                                    Text(if (codigo == null) stringResource(R.string.mandala_cumbre)
                                        else stringResource(R.string.vision_campamento_libre, nombreCodigo(codigo)),
                                        style = MaterialTheme.typography.labelSmall, color = color)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Un cambio global tiene cascada; un giro individual responde inmediatamente. */
@Composable
private fun anguloTarjeta(id: Long, girada: Boolean, generacion: Int, indice: Int): State<Float> {
    var objetivo by remember(id) { mutableStateOf(girada) }
    var anterior by remember(id) { mutableStateOf(generacion) }
    val animar = ValueAnimator.areAnimatorsEnabled()
    LaunchedEffect(girada, generacion) {
        if (generacion != anterior && animar) delay(indice.coerceAtLeast(0) * 20L)
        anterior = generacion
        objetivo = girada
    }
    return animateFloatAsState(if (objetivo) 180f else 0f,
        animationSpec = if (animar) tween(340, easing = FastOutSlowInEasing) else snap(), label = "visionFlip")
}

@Composable
internal fun TarjetaVision(
    cv: CasillaVista, apodo: String, girada: Boolean, generacion: Int, indice: Int, porcentaje: Int, modo: ModoTarjetas,
    acciones: AccionesTarjeta, onEditar: () -> Unit, onGirar: () -> Unit, onVer: () -> Unit, onAmpliar: () -> Unit,
    compacta: Boolean, cumbre: Boolean, rumbo: String?, onPasos: (() -> Unit)?,
    metas: List<MetaSosten>, apodosMetas: Map<String, String>, modifier: Modifier = Modifier,
) {
    val angulo = anguloTarjeta(cv.casilla.id, girada, generacion, indice)
    val dorso by remember(angulo) { derivedStateOf { angulo.value > 90f } }
    val forma = RoundedCornerShape(if (compacta) 12.dp else 16.dp)
    Box(modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer {
        rotationY = angulo.value; cameraDistance = 12f * density
    }.clip(forma).background(MaterialTheme.colorScheme.surface)) {
        if (dorso) {
            if (modo != ModoTarjetas.EDICION) DorsoLectura(cv, apodo, porcentaje, onGirar, onEditar,
                Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }, compacta = compacta)
            else if (compacta) DorsoCompacto(cv, apodo, porcentaje, acciones, onGirar, onAmpliar,
                Modifier.fillMaxSize().graphicsLayer { rotationY = 180f })
            else DorsoCompleto(cv, apodo, porcentaje, acciones, onGirar, metas, apodosMetas,
                Modifier.fillMaxSize().graphicsLayer { rotationY = 180f })
        } else {
            FrenteTarjeta(cv, apodo, porcentaje, acciones, onVer, onGirar, compacta, rumbo, onPasos)
        }
    }
}

/** El dorso de lectura no contiene campos ni acciones que cambien el avance. */
@Composable
private fun DorsoLectura(
    cv: CasillaVista, apodo: String, porcentaje: Int, onFoto: () -> Unit, onEditar: () -> Unit,
    modifier: Modifier = Modifier, compacta: Boolean = false, pantallaCompleta: Boolean = false,
) {
    val codigo = cv.casilla.eje ?: cv.casilla.origen.removePrefix("campamento:")
    val eje = if (codigo in Mandala.CAMPAMENTOS_FIJOS) colorCampamento(codigo) else MaterialTheme.colorScheme.primary
    val fondo = if (pantallaCompleta) androidx.compose.ui.graphics.lerp(Color(0xFF0B0D10), eje, .45f) else MaterialTheme.colorScheme.surface
    val tinta = if (pantallaCompleta) Color.White else MaterialTheme.colorScheme.onSurface
    val estilo = when { pantallaCompleta -> MaterialTheme.typography.headlineLarge; compacta -> MaterialTheme.typography.titleSmall; else -> MaterialTheme.typography.headlineSmall }
    val estado = stringResource(if (porcentaje >= 100) R.string.tarjeta_lograda else if (porcentaje > 0) R.string.tarjeta_camino else R.string.tarjeta_abierta)
    val margen = if (compacta) 7.dp else 20.dp
    val contenido = if (pantallaCompleta) modifier.background(fondo).safeDrawingPadding().padding(top = 116.dp, bottom = 24.dp, start = margen, end = margen)
        else modifier.background(fondo).padding(margen)
    Column(contenido, verticalArrangement = Arrangement.spacedBy(if (compacta) 4.dp else 12.dp)) {
        if (apodo.isNotBlank()) Text(stringResource(R.string.apodo_entre_comillas, apodo), color = tinta,
            style = if (compacta) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall)
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.CenterStart) {
            Text(cv.casilla.afirmacion.ifBlank { cv.casilla.titulo }, style = estilo, fontWeight = FontWeight.Bold, color = tinta)
        }
        Text(stringResource(R.string.vision_estado_porcentaje, estado, porcentaje), color = tinta,
            style = MaterialTheme.typography.labelSmall)
        if (compacta) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onFoto, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Autorenew, stringResource(R.string.vision_ver_foto), Modifier.size(16.dp), tint = tinta)
            }
            IconButton(onClick = onEditar, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Edit, stringResource(R.string.editar), Modifier.size(16.dp), tint = tinta)
            }
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onFoto) {
                Icon(Icons.Filled.Autorenew, null, Modifier.size(18.dp), tint = tinta)
                Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.vision_ver_foto), color = tinta)
            }
            TextButton(onClick = onEditar) {
                Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp), tint = tinta)
                Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.editar), color = tinta)
            }
        }
    }
}

/** Mantiene el borrador local durante la escritura: una emisión anterior de Room no lo pisa. */
@Composable
internal fun CampoTarjetaTexto(
    valor: String, etiqueta: String, onCambio: (String) -> Unit, modifier: Modifier = Modifier,
    compacto: Boolean = false, unaLinea: Boolean = false, maximo: Int = Int.MAX_VALUE,
    centrado: Boolean = false, tamano: androidx.compose.ui.unit.TextUnit = 11.sp, placeholder: String = etiqueta,
) {
    var texto by remember { mutableStateOf(valor) }
    var enfocado by remember { mutableStateOf(false) }
    LaunchedEffect(valor, enfocado) { if (!enfocado) texto = valor }
    val campo = modifier.onFocusChanged { enfocado = it.isFocused }.semantics { contentDescription = etiqueta }
    if (compacto) BasicTextField(texto, { texto = it.take(maximo); onCambio(texto) },
        modifier = campo.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(if (centrado) 1.dp else 4.dp),
        singleLine = unaLinea, textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = tamano, lineHeight = tamano * 1.25f, textAlign = if (centrado) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { entrada -> Box(contentAlignment = if (centrado) Alignment.Center else Alignment.TopStart) { if (texto.isEmpty()) Text(placeholder, style = MaterialTheme.typography.labelSmall.copy(fontSize = tamano), color = MaterialTheme.colorScheme.onSurfaceVariant); entrada() } })
    else OutlinedTextField(texto, { texto = it.take(maximo); onCambio(texto) }, modifier = campo,
        label = { Text(etiqueta) }, singleLine = unaLinea, minLines = if (unaLinea) 1 else 2)
}

@Composable
private fun DorsoCompacto(cv: CasillaVista, apodo: String, porcentaje: Int, acciones: AccionesTarjeta,
    onGirar: () -> Unit, onAmpliar: () -> Unit, modifier: Modifier) {
    val id = cv.casilla.id
    Column(modifier.padding(5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        CampoTarjetaTexto(cv.casilla.afirmacion, stringResource(R.string.vision_frase), { acciones.onCampo(id, CampoTarjeta.FRASE, it) },
            Modifier.fillMaxWidth().weight(1f), compacto = true)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EstadoTarjeta(porcentaje, { acciones.onPorcentaje(id, siguienteEstado(porcentaje)) }, Modifier.weight(1f), compacto = true)
            IconButton(onClick = onAmpliar, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Filled.OpenInFull, stringResource(R.string.vision_ver_grande), Modifier.size(15.dp))
            }

        }
    }
}

private fun siguienteEstado(porcentaje: Int) = if (porcentaje == 0) 25 else if (porcentaje < 100) 100 else 0

@Composable
private fun EstadoTarjeta(porcentaje: Int, onClick: () -> Unit, modifier: Modifier = Modifier, compacto: Boolean = false) {
    Text(stringResource(if (porcentaje >= 100) R.string.tarjeta_lograda else if (porcentaje > 0) R.string.tarjeta_camino else R.string.tarjeta_abierta),
        modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer).clickable(onClick = onClick).padding(horizontal = 5.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compacto) 9.sp else 12.sp),
        color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun DorsoCompleto(cv: CasillaVista, apodo: String, porcentaje: Int, acciones: AccionesTarjeta,
    onGirar: () -> Unit, metas: List<MetaSosten>, apodosMetas: Map<String, String>, modifier: Modifier) {
    val c = cv.casilla
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { acciones.onFoto(c) }) { Icon(Icons.Filled.AddAPhoto, stringResource(R.string.tarjeta_cambiar_foto)) }
            IconButton(onClick = onGirar) { Icon(Icons.Filled.Autorenew, stringResource(R.string.tarjeta_volver_foto)) }
        }
        CampoTarjetaTexto(apodo, stringResource(R.string.apodo_opcional), { acciones.onApodo(c.id, it.trim()) }, Modifier.fillMaxWidth(), unaLinea = true, maximo = 32)
        CampoTarjetaTexto(c.afirmacion, stringResource(R.string.vision_frase), { acciones.onCampo(c.id, CampoTarjeta.FRASE, it) }, Modifier.fillMaxWidth())
        CampoTarjetaTexto(c.titulo, stringResource(R.string.vision_rotulo), { acciones.onCampo(c.id, CampoTarjeta.ROTULO, it) }, Modifier.fillMaxWidth(), unaLinea = true)
        EstadoTarjeta(porcentaje, { acciones.onPorcentaje(c.id, siguienteEstado(porcentaje)) })
        Text(stringResource(R.string.progreso_pct, porcentaje), style = MaterialTheme.typography.labelLarge)
        Slider(porcentaje.toFloat(), { acciones.onPorcentaje(c.id, (it / 5).toInt() * 5) }, valueRange = 0f..100f)
        if (metas.isNotEmpty()) {
            Text(stringResource(R.string.vision_metas_sostienen), style = MaterialTheme.typography.titleSmall)
            metas.forEach { meta ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { acciones.onMeta(meta.ruta) }.padding(8.dp)) {
                    Text(meta.titulo, style = MaterialTheme.typography.bodyMedium)
                    apodosMetas["apodo:${meta.tipo}:${meta.id}"]?.takeIf { it.isNotBlank() }?.let {
                        Text(stringResource(R.string.apodo_entre_comillas, it), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(stringResource(R.string.tarjeta_pct, meta.porcentaje), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        CampoTarjetaTexto(c.sugerencia, stringResource(R.string.vision_que_foto), { acciones.onCampo(c.id, CampoTarjeta.SUGERENCIA, it) }, Modifier.fillMaxWidth())
        TextButton(onClick = { acciones.onIdeas(c) }) { Text(stringResource(R.string.vision_ideas)) }
        if (cv.foto != null) TextButton(onClick = { acciones.onCampo(c.id, CampoTarjeta.QUITAR_FOTO, "") }) { Text(stringResource(R.string.vision_quitar_foto)) }
        TextButton(onClick = { acciones.onBorrar(c) }) { Text(stringResource(R.string.vision_borrar), color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun FrenteTarjeta(cv: CasillaVista, apodo: String, porcentaje: Int, acciones: AccionesTarjeta,
    onVer: () -> Unit, onGirar: () -> Unit, compacta: Boolean, rumbo: String?, onPasos: (() -> Unit)?) {
    val foto = cv.foto
    Box(Modifier.fillMaxSize()) {
        if (foto != null) {
            AsyncImage(solicitudFotoVision(foto, if (compacta) 200.dp else 400.dp), cv.casilla.titulo,
                Modifier.fillMaxSize().clickable(onClick = onVer), contentScale = ContentScale.Crop)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (compacta) 70.dp else 110.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .65f)))))
        } else Column(Modifier.fillMaxSize().clickable { acciones.onFoto(cv.casilla) }.padding(8.dp).padding(top = 34.dp, bottom = 40.dp), verticalArrangement = Arrangement.Center) {
            if (!compacta) Text(cv.casilla.sugerencia, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Filled.AddAPhoto, stringResource(R.string.vision_agregar_foto), Modifier.align(Alignment.CenterHorizontally))
            if (!compacta) Text(stringResource(R.string.vision_agregar_foto), style = MaterialTheme.typography.labelSmall)
        }
        if (rumbo != null && onPasos != null) Text(rumbo,
            Modifier.align(Alignment.TopStart).padding(4.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary).clickable(onClick = onPasos).padding(horizontal = 6.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
        Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(if (compacta) 32.dp else 44.dp).clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .9f)).clickable(onClick = onGirar), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { porcentaje / 100f }, modifier = Modifier.fillMaxSize(), strokeWidth = 2.dp)
            Text(stringResource(R.string.tarjeta_pct, porcentaje), style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compacta) 9.sp else 11.sp))
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(if (compacta) 4.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (apodo.isNotBlank()) stringResource(R.string.apodo_entre_comillas, apodo) else cv.casilla.titulo,
                Modifier.weight(1f).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface.copy(alpha = .9f)).padding(4.dp),
                style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onGirar, modifier = Modifier.size(if (compacta) 28.dp else 40.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface)) {
                Icon(Icons.Filled.Autorenew, stringResource(R.string.tarjeta_girar), Modifier.size(if (compacta) 16.dp else 20.dp))
            }
        }
    }
}

internal data class MetaSosten(val tipo: String, val id: Long, val titulo: String, val porcentaje: Int, val ruta: String)


@Composable
internal fun VisorTarjetas(
    tarjetas: List<CasillaVista>, inicial: Long, apodos: Map<String, String>, modo: ModoTarjetas, onModo: (ModoTarjetas) -> Unit,
    giradas: List<Long>, generacion: Int, onGirar: (Long) -> Unit, onEditar: (Long) -> Unit, acciones: AccionesTarjeta,
    porcentajes: Map<Long, Int>, metas: Map<Long, List<MetaSosten>>, onCerrar: () -> Unit,
) {
    val pager = rememberPagerState(initialPage = tarjetas.indexOfFirst { it.casilla.id == inicial }.coerceAtLeast(0), pageCount = { tarjetas.size })
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VerticalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { tarjetas[it].casilla.id }) { i ->
                val cv = tarjetas[i]; val c = cv.casilla
                val angulo = anguloTarjeta(c.id, modo.muestraDorso xor (c.id in giradas), generacion, i)
                val dorso by remember(angulo) { derivedStateOf { angulo.value > 90f } }
                val porcentaje = porcentajes[c.id] ?: 0
                Box(Modifier.fillMaxSize().graphicsLayer { rotationY = angulo.value; cameraDistance = 12f * density }) {
                    if (dorso) {
                        if (modo == ModoTarjetas.EDICION) {
                            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.safeDrawingPadding().imePadding().padding(top = 116.dp, bottom = 12.dp, start = 12.dp, end = 12.dp), contentAlignment = Alignment.Center) {
                                DorsoCompleto(cv, apodos["apodo:vision:${c.id}"].orEmpty(), porcentaje, acciones, { onGirar(c.id) }, metas[c.id].orEmpty(), apodos,
                                    Modifier.widthIn(max = 460.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).hojaPapel())
                            }
                        } else DorsoLectura(cv, apodos["apodo:vision:${c.id}"].orEmpty(), porcentaje,
                            { onGirar(c.id) }, { onEditar(c.id) },
                            Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }, pantallaCompleta = true)
                    } else {
                        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(top = 116.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)) {
                            val ladoFoto = minOf(maxWidth, maxHeight * .6f, 560.dp)
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                                val foto = cv.foto
                                Box(Modifier.size(ladoFoto).aspectRatio(1f).clip(RoundedCornerShape(16.dp))) {
                                    if (foto != null) AsyncImage(solicitudFotoVision(foto, ladoFoto), c.titulo,
                                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else Column(Modifier.fillMaxSize().background(c.eje?.let(::colorCampamento) ?: MaterialTheme.colorScheme.primary)
                                        .clickable { acciones.onFoto(c) }.padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.AddAPhoto, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                        Text(stringResource(R.string.vision_agregar_foto), color = Color.White)
                                        Text(c.sugerencia, color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    apodos["apodo:vision:${c.id}"]?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.apodo_entre_comillas, it), color = Color.White, style = MaterialTheme.typography.titleSmall) }
                                    Text(c.afirmacion, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                                    Text(stringResource(R.string.tarjeta_rotulo_pct, c.titulo, porcentaje), color = Color.White)
                                    TextButton(onClick = { onEditar(c.id) }, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.editar), color = Color.White) }
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .65f)).safeDrawingPadding().padding(horizontal = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, stringResource(R.string.audio_cerrar_corto), tint = Color.White) }
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.tarjeta_contador, pager.currentPage + 1, tarjetas.size), color = Color.White,
                        modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
                }
                SelectorModoTarjetas(modo, onModo, Modifier.fillMaxWidth())
            }
        }
    }
}
