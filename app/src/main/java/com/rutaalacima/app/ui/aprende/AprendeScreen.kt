package com.rutaalacima.app.ui.aprende

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.content.WorkbookSummary
import com.rutaalacima.app.ui.components.MontanaArte
import com.rutaalacima.app.ui.components.ProgressLine
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.iconoGuia
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Organización de la biblioteca: las guías siguen las etapas del ascenso del método,
 * luego los bancos (que también se usan dentro del asistente de metas) y las lecturas.
 */
enum class Etapa(@StringRes val titulo: Int, @StringRes val descripcion: Int, val paleta: Int, val guias: List<String>) {
    CONOCETE(R.string.etapa_1, R.string.etapa_1_desc, 0, listOf("descubre", "diagnostico", "seis_ejes")),
    TRAZA(R.string.etapa_2, R.string.etapa_2_desc, 3, listOf("viaje", "confluencia", "proposito_valor")),
    CAMINA(R.string.etapa_3, R.string.etapa_3_desc, 7, listOf("portales", "campamento", "companero")),
    SUPERA(R.string.etapa_4, R.string.etapa_4_desc, 2, listOf("caidas", "niebla", "bono_anti_abandono", "kit")),
    CIMA(R.string.etapa_5, R.string.etapa_5_desc, 1, listOf("desde_cima", "bono_evidencias", "planificador_cierre")),
    BANCOS(R.string.etapa_bancos, R.string.etapa_bancos_desc, 4, listOf("bono_metas", "bono_indicadores", "bono_acciones", "bono_confluencia", "bono_cierre")),
    LECTURAS(R.string.etapa_lecturas, R.string.etapa_lecturas_desc, 5, listOf("ebook", "teoria")),
    FACILITADORES(R.string.etapa_facilitadores, R.string.etapa_facilitadores_desc, 6, listOf("facilitadores")),
}

data class GuiaItem(val resumen: WorkbookSummary, val progreso: Float)

class AprendeViewModel(c: AppContainer) : ViewModel() {
    val guias: StateFlow<Map<String, GuiaItem>> =
        combine(flow { emit(c.contenido.index()) }, c.respuestas.conteos, c.respuestas.ultimasSecciones) { index, conteos, ultimas ->
            index.associate { w ->
                val p = when {
                    w.inputs > 0 -> (conteos[w.id] ?: 0).toFloat() / w.inputs
                    w.sections > 0 -> ultimas[w.id]?.let { (it + 1).toFloat() / w.sections } ?: 0f
                    else -> 0f
                }
                w.id to GuiaItem(w, p.coerceAtMost(1f))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

@Composable
fun AprendeScreen(contentPadding: PaddingValues, onOpen: (String) -> Unit) {
    val vm = rutaViewModel { AprendeViewModel(it) }
    val guias by vm.guias.collectAsStateWithLifecycle()
    val enOtroIdioma = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language != "es"

    LazyColumn(
        contentPadding = PaddingValues(top = contentPadding.calculateTopPadding() + 4.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Las guías están escritas en español: si la app está en otro idioma, se dice claramente
        if (enOtroIdioma) item {
            com.rutaalacima.app.ui.components.RutaCard(Modifier.padding(horizontal = 16.dp)) {
                androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.rutaalacima.app.R.string.guias_en_espanol),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Text(stringResource(R.string.aprende_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Etapa.entries.forEachIndexed { i, etapa ->
            val lista = etapa.guias.mapNotNull { guias[it] }
            if (lista.isNotEmpty()) item(key = etapa.name) {
                Column {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (i < 5) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                        }
                        Column {
                            SectionTitle(stringResource(etapa.titulo))
                            Text(stringResource(etapa.descripcion), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(lista, key = { it.resumen.id }) { g -> GuiaCard(g, etapa.paleta) { onOpen(g.resumen.id) } }
                    }
                }
            }
        }
    }
}

/** Tarjeta de guía: paisaje vectorial + ícono, título y avance. */
@Composable
private fun GuiaCard(g: GuiaItem, paleta: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.width(200.dp), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        MontanaArte(g.resumen.id, Modifier.fillMaxWidth().height(110.dp), paleta = paleta, bandera = false) {
            Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopStart) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xE6FFFFFF)), contentAlignment = Alignment.Center) {
                    Icon(iconoGuia(g.resumen.id), null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(g.resumen.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(g.resumen.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            ProgressLine(g.progreso)
        }
    }
}
