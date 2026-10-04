package com.rutaalacima.app.ui.perfil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.Cascada
import com.rutaalacima.app.data.local.EvaluacionEjesEntity
import com.rutaalacima.app.data.local.PerfilEntity
import com.rutaalacima.app.data.local.puntajes
import com.rutaalacima.app.data.local.total
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.data.social.TipoPost
import com.rutaalacima.app.ui.comunidad.Avatar
import com.rutaalacima.app.ui.i18n.Textos
import com.rutaalacima.app.ui.comunidad.PostImagen
import com.rutaalacima.app.ui.comunidad.TipoBadge
import com.rutaalacima.app.ui.components.MontanaArte
import com.rutaalacima.app.ui.components.RadarEjes
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PerfilViewModel(c: AppContainer) : ViewModel() {
    private fun <T> estado(f: kotlinx.coroutines.flow.Flow<T>, i: T) = f.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), i)
    val perfil: StateFlow<PerfilEntity> = estado(c.perfil.perfil, PerfilEntity())
    val posts: StateFlow<List<Post>> = estado(c.social.misPublicaciones, emptyList())
    val evaluacion: StateFlow<EvaluacionEjesEntity?> = estado(c.ejes.ultima, null)
    val cascada: StateFlow<Cascada?> = estado(c.cascada.cascada, null)
    val usuario: String = c.social.usuarioPropio()
}

/** Perfil: quién soy, mi cumbre, mis publicaciones, mi vida por años, mi vision board y mis ejes. */
@Composable
fun PerfilScreen(
    contentPadding: PaddingValues,
    onAbrirPost: (String) -> Unit,
    onPublicar: (String) -> Unit,
    onEvaluarEjes: () -> Unit,
) {
    val vm = rutaViewModel { PerfilViewModel(it) }
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val posts by vm.posts.collectAsStateWithLifecycle()
    val eval by vm.evaluacion.collectAsStateWithLifecycle()
    val cascada by vm.cascada.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val logros = posts.count { it.tipo == TipoPost.LOGRO }
    val metasCumplidas = cascada?.let { c ->
        (c.propositos.flatMap { it.anios } + c.aniosSueltos).count { it.avance >= 1f } +
            (c.propositos.flatMap { p -> p.anios.flatMap { it.meses } } + c.aniosSueltos.flatMap { it.meses } + c.mesesSueltos).count { it.avance >= 1f }
    } ?: 0

    LazyColumn(
        contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Portada + avatar
        item {
            MontanaArte("perfil-${perfil.nombre}", Modifier.fillMaxWidth().height(110.dp), paleta = 2)
            Row(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(perfil.nombre.ifBlank { "R" }, tamano = 76)
                Spacer(Modifier.width(16.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Dato(posts.size, stringResource(R.string.publicaciones))
                    Dato(logros, stringResource(R.string.logros))
                    Dato(metasCumplidas, stringResource(R.string.metas_cumplidas))
                }
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(perfil.nombre.ifBlank { stringResource(R.string.senderista) }, style = MaterialTheme.typography.titleLarge)
                if (vm.usuario.isNotBlank()) Text("@${vm.usuario}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (perfil.cumbreFrase.isNotBlank()) Text("⛰ ${perfil.cumbreFrase}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
        item {
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                listOf(R.string.perfil_tab_publicaciones, R.string.perfil_tab_vida, R.string.perfil_tab_vision, R.string.perfil_tab_ejes)
                    .forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(t), maxLines = 1) }) }
            }
        }
        when (tab) {
            0 -> cuadricula(posts, onAbrirPost, vacio = R.string.sin_publicaciones) { onPublicar("LOGRO") }
            1 -> miVida(posts, onAbrirPost, onPublicar)
            2 -> {
                item {
                    Text(stringResource(R.string.vision_board_texto), style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp))
                }
                cuadricula(posts.filter { it.tipo == TipoPost.VISION }, onAbrirPost, columnas = 2, vacio = R.string.vision_vacia) { onPublicar("VISION") }
                item {
                    OutlinedButton(onClick = { onPublicar("VISION") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.agregar_vision))
                    }
                }
            }
            else -> item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    val e = eval
                    if (e == null) {
                        Text(stringResource(R.string.ejes_sin_evaluar), style = MaterialTheme.typography.bodyLarge)
                    } else {
                        RadarEjes(e.puntajes())
                        Text("${e.total()}/60", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(Textos.interpretacionTotal(e.total())), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onEvaluarEjes, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.evaluar_mis_ejes)) }
                }
            }
        }
    }
}

@Composable
private fun Dato(n: Int, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

/** Cuadrícula estilo Instagram (3 columnas) dentro de la lista. */
private fun androidx.compose.foundation.lazy.LazyListScope.cuadricula(
    posts: List<Post>,
    onAbrir: (String) -> Unit,
    columnas: Int = 3,
    vacio: Int,
    onCrear: () -> Unit,
) {
    if (posts.isEmpty()) {
        item {
            RutaCard(Modifier.padding(horizontal = 16.dp), onClick = onCrear) {
                Text(stringResource(vacio), style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }
    items(posts.chunked(columnas)) { fila ->
        Row(Modifier.padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            fila.forEach { p ->
                PostImagen(p, Modifier.weight(1f).clickable { onAbrir(p.id) }, conTexto = columnas < 3)
            }
            repeat(columnas - fila.size) { Spacer(Modifier.weight(1f).aspectRatio(1f)) }
        }
    }
}

/** "Mi vida por años": el registro de lo que hice en cada año de mi vida. */
private fun androidx.compose.foundation.lazy.LazyListScope.miVida(
    posts: List<Post>,
    onAbrir: (String) -> Unit,
    onPublicar: (String) -> Unit,
) {
    if (posts.isEmpty()) {
        item {
            RutaCard(Modifier.padding(horizontal = 16.dp), onClick = { onPublicar("LOGRO") }) {
                Text(stringResource(R.string.mi_vida_vacia), style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }
    posts.groupBy { it.anio }.toSortedMap(compareByDescending { it }).forEach { (anio, delAnio) ->
        item(key = "anio-$anio") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$anio", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.anio_resumen, delAnio.size, delAnio.count { it.tipo == TipoPost.LOGRO }),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(delAnio, key = { it.id }) { p ->
                        Column(Modifier.width(140.dp).clickable { onAbrir(p.id) }) {
                            PostImagen(p, Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)), conTexto = false)
                            Spacer(Modifier.height(4.dp))
                            TipoBadge(p.tipo)
                            Text(p.metaTitulo.ifBlank { p.texto }, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
