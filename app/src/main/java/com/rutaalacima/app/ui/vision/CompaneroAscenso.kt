package com.rutaalacima.app.ui.vision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Compañero de Ascenso: vive en la Brújula, en el Campamento Base (No subo solo). Herramientas vivas del
 * workbook 11 con las mismas claves que la web (comp#…): ¿estás listo/a?, las 3 compatibilidades,
 * red y green flags y la revisión mensual de pareja.
 */
class CompaneroViewModel(private val c: AppContainer) : ViewModel() {
    val r: StateFlow<Map<String, String>> = c.respuestas.observar(WB).stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    fun guardar(k: String, v: String) = viewModelScope.launch { c.respuestas.guardar(WB, "comp#$k", v) }
    companion object { const val WB = "companero" }
}

private val LISTO = listOf(
    R.string.cp_l1 to listOf(R.string.cp_si, R.string.cp_no, R.string.cp_aveces),
    R.string.cp_l2 to listOf(R.string.cp_si, R.string.cp_no, R.string.cp_aveces),
    R.string.cp_l3 to listOf(R.string.cp_si, R.string.cp_no, R.string.cp_mas_menos),
    R.string.cp_l4 to listOf(R.string.cp_si, R.string.cp_no, R.string.cp_depende),
    R.string.cp_l5 to listOf(R.string.cp_si, R.string.cp_no, R.string.cp_no_seguro),
)
private val CUMBRES = listOf(Triple("paralela", R.string.cp_paralela, R.string.cp_paralela_d),
    Triple("convergente", R.string.cp_convergente, R.string.cp_convergente_d), Triple("conflicto", R.string.cp_conflicto, R.string.cp_conflicto_d))
private val FASES = listOf(
    Triple(R.string.cp_f1, R.string.cp_f1_m, R.string.cp_f1_r), Triple(R.string.cp_f2, R.string.cp_f2_m, R.string.cp_f2_r),
    Triple(R.string.cp_f3, R.string.cp_f3_m, R.string.cp_f3_r), Triple(R.string.cp_f4, R.string.cp_f4_m, R.string.cp_f4_r),
    Triple(R.string.cp_f5, R.string.cp_f5_m, R.string.cp_f5_r), Triple(R.string.cp_f6, R.string.cp_f6_m, R.string.cp_f6_r),
    Triple(R.string.cp_f7, R.string.cp_f7_m, R.string.cp_f7_r))
private val RITMOS = listOf(R.string.cp_rapido, R.string.cp_moderado, R.string.cp_lento)
private val RED = listOf(R.string.cp_r1, R.string.cp_r2, R.string.cp_r3, R.string.cp_r4, R.string.cp_r5, R.string.cp_r6)
private val GREEN = listOf(R.string.cp_g1, R.string.cp_g2, R.string.cp_g3, R.string.cp_g4, R.string.cp_g5, R.string.cp_g6)
private val MES = listOf("fase" to R.string.cp_m1, "necesito" to R.string.cp_m2, "ofrezco" to R.string.cp_m3)

/** Botón de la barra de la Brújula que abre el espacio. */
@Composable
internal fun CompaneroAccion(onAbrir: () -> Unit) {
    val etiqueta = stringResource(R.string.cp_titulo)
    IconButton(onClick = onAbrir, modifier = Modifier.semantics { contentDescription = etiqueta }) { Icon(Icons.Outlined.FavoriteBorder, null) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CompaneroHoja(cumbre: String, onCerrar: () -> Unit, onGuia: () -> Unit) {
    val vm = rutaViewModel { CompaneroViewModel(it) }
    val r by vm.r.collectAsStateWithLifecycle()
    fun v(k: String) = r["comp#$k"].orEmpty()
    // Los textos se editan en local y se guardan al escribir (sin perder el cursor)
    val textos = remember { mutableStateMapOf<String, String>() }
    fun t(k: String) = textos[k] ?: v(k)
    fun escribir(k: String, s: String) { textos[k] = s; vm.guardar(k, s) }
    fun alternar(k: String, valor: String) = vm.guardar(k, if (v(k) == valor) "" else valor)
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cs = MaterialTheme.colorScheme; val ty = MaterialTheme.typography
    var abierta by rememberSaveable { mutableStateOf(0) }

    @Composable fun Opcion(k: String, valor: String, texto: String) {
        val sel = v(k) == valor
        FilterChip(selected = sel, onClick = { alternar(k, valor) }, label = { Text(texto) },
            leadingIcon = if (sel) ({ Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }) else null,
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = cs.primary.copy(alpha = .14f), selectedLabelColor = cs.primary))
    }
    @Composable fun Nota(texto: String, fuerte: Boolean = false, alerta: Boolean = false) {
        Text(texto, style = ty.bodyMedium, color = if (fuerte) cs.onSurface else cs.onSurfaceVariant,
            modifier = if (fuerte) Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(if (alerta) cs.primary.copy(alpha = .09f) else Color(0xFFC9973B).copy(alpha = .16f)).padding(12.dp) else Modifier)
    }
    @Composable fun Campo(k: String, etiqueta: String, ph: String = "") {
        OutlinedTextField(t(k), { escribir(k, it) }, Modifier.fillMaxWidth(), label = { Text(etiqueta) },
            placeholder = if (ph.isNotEmpty()) ({ Text(ph) }) else null, minLines = 2)
    }
    @Composable fun Seccion(n: Int, titulo: String, contenido: @Composable () -> Unit) {
        HorizontalDivider(color = cs.outlineVariant)
        Row(Modifier.fillMaxWidth().clickable { abierta = if (abierta == n) -1 else n }.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(cs.primary.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                Text("${n + 1}", style = ty.labelMedium, fontWeight = FontWeight.Bold, color = cs.primary)
            }
            Text(titulo, style = ty.titleSmall, modifier = Modifier.weight(1f))
            Icon(if (abierta == n) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = cs.onSurfaceVariant)
        }
        AnimatedVisibility(abierta == n) { Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { contenido() } }
    }
    @Composable fun Bloque(titulo: String, contenido: @Composable () -> Unit) {
        Column(Modifier.fillMaxWidth().border(1.dp, cs.outlineVariant, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = ty.titleSmall); contenido()
        }
    }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = estado, containerColor = cs.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(cs.primary.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.FavoriteBorder, null, tint = cs.primary)
                }
                Column { Text(stringResource(R.string.cp_titulo), style = ty.titleLarge); Text(stringResource(R.string.cp_sub), style = ty.bodySmall, color = cs.onSurfaceVariant) }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.cp_intro), style = ty.bodyMedium, fontStyle = FontStyle.Italic, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            // 1 · ¿Estás listo/a?
            Seccion(0, stringResource(R.string.cp_s1)) {
                LISTO.forEachIndexed { i, (p, ops) ->
                    Text(stringResource(p), style = ty.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { ops.forEachIndexed { j, o -> Opcion("listo#$i", "$j", stringResource(o)) } }
                }
                val resp = LISTO.indices.map { v("listo#$it") }
                if (resp.all { it.isNotEmpty() }) {
                    val noListo = resp[0] == "0" || resp[1] == "0" || resp[2] == "1" || resp[3] == "0" || resp[4] == "1"
                    Nota(stringResource(if (noListo) R.string.cp_listo_aun else R.string.cp_listo_si), fuerte = true, alerta = noListo)
                } else Nota(stringResource(R.string.cp_listo_falta))
            }
            // 2 · Las 3 compatibilidades
            Seccion(1, stringResource(R.string.cp_s2)) {
                Bloque(stringResource(R.string.cp_c_cumbres)) {
                    Text(stringResource(R.string.cp_mi_cumbre), style = ty.labelMedium, color = cs.onSurfaceVariant)
                    Text(cumbre.ifBlank { "—" }, style = ty.titleSmall)
                    Campo("su_cumbre", stringResource(R.string.cp_su_cumbre), stringResource(R.string.cp_su_cumbre_ph))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { CUMBRES.forEach { (k, n, _) -> Opcion("compat_cumbre", k, stringResource(n)) } }
                    CUMBRES.firstOrNull { it.first == v("compat_cumbre") }?.let { Nota(stringResource(it.third)) }
                }
                Bloque(stringResource(R.string.cp_c_fase)) {
                    listOf("fase_yo" to R.string.cp_mi_fase, "fase_pareja" to R.string.cp_su_fase).forEach { (k, etq) ->
                        Text(stringResource(etq), style = ty.labelMedium, color = cs.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { FASES.forEachIndexed { i, f -> Opcion(k, "$i", stringResource(f.first)) } }
                        v(k).toIntOrNull()?.let { FASES.getOrNull(it) }?.let { f ->
                            Nota(stringResource(R.string.cp_mejor, stringResource(f.second)) + "\n" + stringResource(R.string.cp_riesgo, stringResource(f.third)))
                        }
                    }
                }
                Bloque(stringResource(R.string.cp_c_ritmo)) {
                    listOf("ritmo_yo" to R.string.cp_mi_ritmo, "ritmo_pareja" to R.string.cp_su_ritmo).forEach { (k, etq) ->
                        Text(stringResource(etq), style = ty.labelMedium, color = cs.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { RITMOS.forEachIndexed { i, n -> Opcion(k, "$i", stringResource(n)) } }
                    }
                    val a = v("ritmo_yo").toIntOrNull(); val b = v("ritmo_pareja").toIntOrNull()
                    if (a != null && b != null) Nota(stringResource(when { kotlin.math.abs(a - b) == 2 -> R.string.cp_ritmo_opuesto; a == b -> R.string.cp_ritmo_igual; else -> R.string.cp_ritmo_cerca }))
                }
            }
            // 3 · Red y green flags
            Seccion(2, stringResource(R.string.cp_s3)) {
                val rojas = RED.indices.count { v("red#$it").isNotEmpty() }; val verdes = GREEN.indices.count { v("green#$it").isNotEmpty() }
                Text(stringResource(R.string.cp_red, rojas), style = ty.labelLarge, color = cs.primary)
                RED.forEachIndexed { i, s -> FilaMarca(v("red#$i").isNotEmpty(), stringResource(s)) { vm.guardar("red#$i", if (it) "1" else "") } }
                Text(stringResource(R.string.cp_green, verdes), style = ty.labelLarge, color = Color(0xFF2E7D4F))
                GREEN.forEachIndexed { i, s -> FilaMarca(v("green#$i").isNotEmpty(), stringResource(s)) { vm.guardar("green#$i", if (it) "1" else "") } }
                Campo("crucial", stringResource(R.string.cp_crucial))
                if (rojas >= 3) Nota(stringResource(R.string.cp_rojas_aviso, rojas), fuerte = true, alerta = true)
            }
            // 4 · Revisión mensual de pareja
            Seccion(3, stringResource(R.string.cp_s4)) {
                val hoy = java.time.YearMonth.now()
                val nombreMes = hoy.atDay(1).format(java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy"))
                Nota(stringResource(R.string.cp_mes_nota, nombreMes))
                MES.forEach { (k, p) -> Campo("mes#$hoy#$k", stringResource(p)) }
            }
            HorizontalDivider(color = cs.outlineVariant)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onGuia, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.MenuBook, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.cp_guia))
            }
        }
    }
}

@Composable
private fun FilaMarca(marcada: Boolean, texto: String, onCambio: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onCambio(!marcada) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(marcada, onCambio); Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}
