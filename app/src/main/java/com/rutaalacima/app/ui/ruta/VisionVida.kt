package com.rutaalacima.app.ui.ruta

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rutaalacima.app.R
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.vision.CasillaVista
import com.rutaalacima.app.ui.vision.colorCampamento
import com.rutaalacima.app.ui.vision.nombreCampamento
import com.rutaalacima.app.ui.vision.nombreCodigo
import java.io.File
import java.time.LocalDate

/** Las metas de un campamento: las del mismo eje; Confluencia toma las metas sin eje; Campamento Base no tiene metas. */
internal fun metasDeCampamento(codigo: String, metas: List<MetaAnualEntity>): List<MetaAnualEntity> = when (codigo) {
    "CAM" -> emptyList()
    "CON" -> metas.filter { it.eje.isNullOrBlank() }
    else -> metas.filter { it.eje == codigo }
}.sortedBy { it.anio }

/**
 * Mi vida empieza aquí: la cumbre al centro y los 8 campamentos del vision board alrededor. Cada uno
 * dice cuántas metas lo sostienen y en qué años; al tocarlo se abre su planeación por año con
 * "Meta para…" (el asistente llega con el eje y el año elegidos).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisionVida(
    casillas: List<CasillaVista>,
    metas: List<MetaAnualEntity>,
    armando: Boolean,
    onArmar: () -> Unit,
    onVision: () -> Unit,
    onCompartir: () -> Unit,
    onAnio: (Int) -> Unit,
    onNuevaMeta: (eje: String?, anio: Int) -> Unit,
) {
    val hoy = LocalDate.now().year
    if (casillas.isEmpty()) {
        RutaCard {
            Text(stringResource(R.string.vv_titulo), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.vv_vacio), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            Button(onClick = onArmar, enabled = !armando) {
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(stringResource(if (armando) R.string.vv_armando else R.string.vv_armar))
            }
        }
        return
    }
    val (cumbre, camps) = Mandala.repartir(casillas, { it.casilla.origen }, { it.casilla.eje })
    var elegido by rememberSaveable { mutableStateOf<Int?>(null) }
    RutaCard {
        Text(stringResource(R.string.vv_titulo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.vv_ayuda), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (f in 0 until 3) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0 until 3) {
                    val cel = Mandala.celda(3 + f, 3 + col)
                    Box(Modifier.weight(1f).aspectRatio(0.86f)) {
                        if (cel.tipo == Mandala.Tipo.CUMBRE) {
                            Tile(color = MaterialTheme.colorScheme.primary, foto = cumbre?.foto, etiqueta = stringResource(R.string.mi_cumbre).uppercase(),
                                titulo = cumbre?.let { nombreCampamento(it) } ?: stringResource(R.string.mi_cumbre), pie = null, serif = true, elegido = false, onClick = onVision)
                        } else {
                            val i = cel.campamento; val cod = Mandala.CAMPAMENTOS_FIJOS[i]; val cv = camps.getOrNull(i)
                            val ms = metasDeCampamento(cod, metas); val anios = ms.map { it.anio }.distinct()
                            val pie = when {
                                cod == "CAM" -> stringResource(R.string.vv_cordadas)
                                ms.isEmpty() -> stringResource(R.string.vv_sin_metas)
                                else -> stringResource(R.string.vv_metas_n, ms.size) + " · " + anios.take(2).joinToString(", ") + if (anios.size > 2) "…" else ""
                            }
                            Tile(color = colorCampamento(cod), foto = cv?.foto, etiqueta = nombreCodigo(cod).uppercase(),
                                titulo = cv?.let { nombreCampamento(it) } ?: "+", pie = pie, serif = false, elegido = elegido == i,
                                onClick = { elegido = if (elegido == i) null else i })
                        }
                    }
                }
            }
        }
        val sel = elegido
        AnimatedVisibility(sel != null) {
            if (sel != null) {
                val cod = Mandala.CAMPAMENTOS_FIJOS[sel]; val color = colorCampamento(cod)
                val ms = metasDeCampamento(cod, metas)
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, color.copy(alpha = 0.35f)), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text("${nombreCodigo(cod)} · ${Mandala.RUMBOS[sel]}".uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
                                Text(camps.getOrNull(sel)?.let { nombreCampamento(it) } ?: "—", style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(onClick = { elegido = null }) { Icon(Icons.Filled.Close, stringResource(R.string.cerrar_brujula)) }
                        }
                        if (cod == "CAM") {
                            Text(stringResource(R.string.vv_cam_texto), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            if (ms.isEmpty()) Text(stringResource(R.string.vv_sin_metas_texto), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                            ms.groupBy { it.anio }.forEach { (anio, delAnio) ->
                                Text(
                                    "$anio" + when { anio == hoy -> " · " + stringResource(R.string.vv_este_anio); anio > hoy -> " · " + stringResource(R.string.vv_por_vivir); else -> "" },
                                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 10.dp).heightIn(min = 32.dp).clickable { onAnio(anio) }.padding(vertical = 4.dp),
                                )
                                delAnio.forEach { m ->
                                    Row(Modifier.fillMaxWidth().clickable { onAnio(anio) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(m.titulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Text("${m.avance}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    }
                                    LinearProgressIndicator(progress = { m.avance / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp))
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                                listOf(hoy, hoy + 1, hoy + 2).forEach { a ->
                                    FilledTonalButton(onClick = { onNuevaMeta(if (cod == "CON") null else cod, a) }) {
                                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                                        Text(stringResource(R.string.vv_meta_para, a))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            OutlinedButton(onClick = onVision) { Icon(Icons.Filled.GridView, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.vv_brujula)) }
            Button(onClick = onCompartir) { Icon(Icons.Filled.IosShare, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.compartir)) }
        }
    }
}

@Composable
private fun Tile(color: Color, foto: String?, etiqueta: String, titulo: String, pie: String?, serif: Boolean, elegido: Boolean, onClick: () -> Unit) {
    val fondo = color.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surface)
    Surface(
        onClick = onClick, shape = RoundedCornerShape(14.dp), color = fondo,
        border = BorderStroke(if (elegido) 2.dp else 1.dp, if (elegido) color else color.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxSize().semantics { contentDescription = listOfNotNull(etiqueta, titulo, pie).joinToString(". ") },
    ) {
        Box {
            if (foto != null) {
                AsyncImage(if (foto.startsWith("http")) foto else File(foto), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x10000000), Color(0xC8060A12)))))
            }
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Bottom) {
                Text(etiqueta, fontSize = 9.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (foto != null) Color.White.copy(alpha = 0.85f) else color)
                Text(titulo, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    fontFamily = if (serif) FontFamily.Serif else FontFamily.Default,
                    color = if (foto != null) Color.White else MaterialTheme.colorScheme.onSurface)
                if (pie != null) Text(pie, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (foto != null) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
