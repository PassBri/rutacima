package com.rutaalacima.app.ui.mochila

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rutaalacima.app.R
import com.rutaalacima.app.ui.aprende.AprendeViewModel
import com.rutaalacima.app.ui.aprende.GuiaItem
import com.rutaalacima.app.ui.components.IconosRuta
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.fondoPapel

/**
 * Tu mochila: los bonos del método como herramientas del senderista (igual que en la web, web/js/24-mochila.js).
 * Se ordenan por el momento en que sirven, y aparecen también donde se usan (al trazar una meta).
 */
data class GrupoMochila(@StringRes val titulo: Int, @StringRes val descripcion: Int, val herramientas: List<Pair<String, Int>>)

val MOCHILA = listOf(
    GrupoMochila(R.string.mochila_g_metas, R.string.mochila_g_metas_d, listOf(
        "bono_metas" to R.string.mochila_u_metas, "bono_indicadores" to R.string.mochila_u_indicadores, "bono_confluencia" to R.string.mochila_u_confluencia)),
    GrupoMochila(R.string.mochila_g_dias, R.string.mochila_g_dias_d, listOf("bono_acciones" to R.string.mochila_u_acciones)),
    GrupoMochila(R.string.mochila_g_mes, R.string.mochila_g_mes_d, listOf(
        "bono_cierre" to R.string.mochila_u_cierre, "bono_evidencias" to R.string.mochila_u_evidencias)),
    GrupoMochila(R.string.mochila_g_seguir, R.string.mochila_g_seguir_d, listOf("bono_anti_abandono" to R.string.mochila_u_anti)),
)

/** Las herramientas que sirven al trazar metas (para la hoja «De tu mochila»). */
val MOCHILA_METAS = MOCHILA.first().herramientas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MochilaScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm = rutaViewModel { AprendeViewModel(it) }
    val guias by vm.guias.collectAsStateWithLifecycle()
    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Column { Text(stringResource(R.string.mochila_titulo)); Text(stringResource(R.string.mochila_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(stringResource(R.string.mochila_intro), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
            }
            MOCHILA.forEach { g ->
                item(key = g.titulo) {
                    RutaCard(Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle(stringResource(g.titulo))
                        Text(stringResource(g.descripcion), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.size(4.dp))
                        g.herramientas.forEach { (id, uso) -> guias[id]?.let { Herramienta(it, stringResource(uso)) { onOpen(id) } } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Herramienta(g: GuiaItem, uso: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(g.resumen.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(uso, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (g.progreso > 0f) {
                    Spacer(Modifier.size(6.dp))
                    LinearProgressIndicator(progress = { g.progreso }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        leadingContent = {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(IconosRuta.Mosqueton, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
            }
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
    )
}

/** «De tu mochila»: hoja con las herramientas que sirven en este momento (por ejemplo, al trazar una meta). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaMochila(herramientas: List<Pair<String, Int>>, onOpen: (String) -> Unit, onCerrar: () -> Unit) {
    val vm = rutaViewModel { AprendeViewModel(it) }
    val guias by vm.guias.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 24.dp)) {
            Row(Modifier.padding(start = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(IconosRuta.Mosqueton, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.mochila_de_tu), style = MaterialTheme.typography.titleMedium)
            }
            herramientas.forEach { (id, uso) -> guias[id]?.let { Herramienta(it, stringResource(uso)) { onCerrar(); onOpen(id) } } }
        }
    }
}
