package com.rutaalacima.app.ui.comunidad

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
    var eje by mutableStateOf<Eje?>(null)
    var visibilidad by mutableStateOf(Visibilidad.PUBLICA)
    var meta by mutableStateOf("")
    var metas by mutableStateOf<List<String>>(emptyList())
        private set
    var publicando by mutableStateOf(false)
        private set

    init {
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
        if (texto.isBlank() && foto == null) return
        publicando = true
        viewModelScope.launch {
            runCatching { c.social.publicar(tipo, texto, foto, eje?.codigo, visibilidad, meta) }
            publicando = false
            alTerminar()
        }
    }
}

/** Publicar: foto + texto + tipo (logro, evidencia, vision board, meta, reflexión) + eje + meta vinculada. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicarScreen(tipoInicial: String, onBack: () -> Unit) {
    val vm = rutaViewModel(key = "publicar-$tipoInicial") { PublicarViewModel(it, tipoInicial) }
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.foto = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nueva_publicacion)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = {
                    if (vm.publicando) CircularProgressIndicator(Modifier.size(22.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                    else TextButton(onClick = { vm.publicar(onBack) }, enabled = vm.texto.isNotBlank() || vm.foto != null) {
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
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(forma)
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
                if (vm.foto != null) TextButton(onClick = { vm.foto = null }) { Text(stringResource(R.string.quitar_foto)) }
            }
            item {
                OutlinedTextField(vm.texto, { vm.texto = it }, label = { Text(stringResource(R.string.que_quieres_compartir)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
            if (vm.metas.isNotEmpty()) {
                item {
                    ChipSelector(stringResource(R.string.vincular_meta), listOf("") + vm.metas, vm.meta,
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
            item {
                Button(onClick = { vm.publicar(onBack) }, modifier = Modifier.fillMaxWidth(), enabled = !vm.publicando && (vm.texto.isNotBlank() || vm.foto != null)) {
                    Text(stringResource(R.string.publicar))
                }
            }
        }
    }
}
