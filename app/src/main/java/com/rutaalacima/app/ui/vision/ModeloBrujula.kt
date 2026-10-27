package com.rutaalacima.app.ui.vision

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R

private val OroModelo = Color(0xFFC9973B)

/** Ícono del Modelo 1·9·81: esquinas suaves, los 4 rumbos en tono más fuerte y la cumbre en oro. */
@Composable
internal fun IconoModelo(color: Color, modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier) {
        val u = size.minDimension / 24f; val lado = 5f * u; val r = CornerRadius(1.6f * u)
        val pos = floatArrayOf(3f, 9.5f, 16f)
        for (f in 0 until 3) for (c in 0 until 3) {
            val o = Offset(pos[c] * u, pos[f] * u)
            val tinte = when {
                f == 1 && c == 1 -> OroModelo
                f == 1 || c == 1 -> color.copy(alpha = color.alpha * .65f)
                else -> color.copy(alpha = color.alpha * .3f)
            }
            drawRoundRect(tinte, o, Size(lado, lado), r)
        }
    }
}

/** Botón «Modelo» de la barra: abre la explicación de la Brújula 1·9·81. */
@Composable
internal fun ModeloBrujulaAccion() {
    var abierto by rememberSaveable { mutableStateOf(false) }
    val etiqueta = stringResource(R.string.modelo_boton)
    IconButton(onClick = { abierto = true }, modifier = Modifier.semantics { contentDescription = etiqueta }) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { IconoModelo(MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) }
    }
    if (abierto) ModeloBrujulaHoja(onCerrar = { abierto = false })
}

/** Diagrama 9×9: las 81 casillas, el 3×3 central (9) y la cumbre (1) en oro. */
@Composable
private fun DiagramaModelo(modifier: Modifier) {
    val tinta = MaterialTheme.colorScheme.onSurface; val burdeos = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val paso = size.minDimension / 9f; val lado = paso * .85f
        for (f in 0 until 9) for (c in 0 until 9) {
            val nueve = f in 3..5 && c in 3..5; val centro = f == 4 && c == 4; val cab = f % 3 == 1 && c % 3 == 1
            val color = when { centro -> OroModelo; nueve -> burdeos.copy(alpha = .7f); cab -> burdeos.copy(alpha = .3f); else -> tinta.copy(alpha = .09f) }
            drawRoundRect(color, Offset(c * paso, f * paso), Size(lado, lado), CornerRadius(lado * .2f))
        }
    }
}

private fun referenciaApa(texto: String): AnnotatedString = buildAnnotatedString {
    var resto = texto
    while (true) {
        val i = resto.indexOf("<i>"); if (i < 0) { append(resto); break }
        val j = resto.indexOf("</i>", i); if (j < 0) { append(resto); break }
        append(resto.substring(0, i)); withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(resto.substring(i + 3, j)) }
        resto = resto.substring(j + 4)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModeloBrujulaHoja(onCerrar: () -> Unit) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cs = MaterialTheme.colorScheme; val ty = MaterialTheme.typography
    val campamentos = listOf(R.string.brujula_con, R.string.eje_tra, R.string.eje_val, R.string.eje_evo, R.string.eje_voz,
        R.string.brujula_cam, R.string.eje_vol, R.string.eje_mae).map { stringResource(it) }.joinToString(", ")
    @Composable fun Titulo(t: String) = Text(t, style = ty.labelLarge, fontWeight = FontWeight.Bold, color = cs.primary, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
    @Composable fun Parrafo(t: String) = Text(t, style = ty.bodyMedium, color = cs.onSurfaceVariant)
    @Composable fun Fila(marca: @Composable () -> Unit, t: String, d: String, linea: Boolean) {
        if (linea) HorizontalDivider(color = cs.outlineVariant)
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            marca()
            Column(Modifier.weight(1f)) { Text(t, style = ty.titleSmall); Text(d, style = ty.bodyMedium, color = cs.onSurfaceVariant) }
        }
    }
    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = estado, containerColor = cs.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DiagramaModelo(Modifier.size(96.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.modelo_nombre), style = ty.headlineSmall)
                    Spacer(Modifier.height(4.dp)); Parrafo(stringResource(R.string.modelo_intro))
                }
            }
            Titulo(stringResource(R.string.modelo_escalas))
            listOf(Triple("1", R.string.modelo_e1_t, stringResource(R.string.modelo_e1_d)),
                Triple("9", R.string.modelo_e9_t, stringResource(R.string.modelo_e9_d, campamentos)),
                Triple("81", R.string.modelo_e81_t, stringResource(R.string.modelo_e81_d))).forEachIndexed { i, (n, t, d) ->
                Fila({
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (i == 0) OroModelo.copy(alpha = .22f) else cs.primary.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                        Text(n, style = ty.titleSmall, fontWeight = FontWeight.Bold, color = if (i == 0) Color(0xFF8A6418) else cs.primary)
                    }
                }, stringResource(t), d, i > 0)
            }
            Titulo(stringResource(R.string.modelo_camino))
            listOf(R.string.modelo_c1_t to R.string.modelo_c1_d, R.string.modelo_c2_t to R.string.modelo_c2_d, R.string.modelo_c3_t to R.string.modelo_c3_d,
                R.string.modelo_c4_t to R.string.modelo_c4_d, R.string.modelo_c5_t to R.string.modelo_c5_d, R.string.modelo_c6_t to R.string.modelo_c6_d,
                R.string.modelo_c7_t to R.string.modelo_c7_d, R.string.modelo_c8_t to R.string.modelo_c8_d).forEachIndexed { i, (t, d) ->
                Fila({
                    Box(Modifier.padding(horizontal = 4.dp).size(28.dp).border(1.dp, cs.outlineVariant, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = ty.labelMedium, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant)
                    }
                }, stringResource(t), stringResource(d), i > 0)
            }
            Titulo(stringResource(R.string.modelo_tiempo_t)); Parrafo(stringResource(R.string.modelo_tiempo_d))
            Titulo(stringResource(R.string.modelo_solo_t)); Parrafo(stringResource(R.string.modelo_solo_d))
            Titulo(stringResource(R.string.modelo_origen_t)); Parrafo(stringResource(R.string.modelo_origen_d))
            Spacer(Modifier.height(8.dp)); Parrafo(stringResource(R.string.modelo_origen_suroute))
            Titulo(stringResource(R.string.modelo_refs_t))
            stringArrayResource(R.array.modelo_refs).forEach { r ->
                Text(referenciaApa(r), style = ty.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}
