package com.rutaalacima.app.ui.web

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.web.Dispositivo
import com.rutaalacima.app.data.web.RutaWebRepository
import com.rutaalacima.app.domain.model.Sincronia
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.fondoPapel
import kotlinx.coroutines.launch

class WebViewModel(c: AppContainer) : ViewModel() {
    private val web = c.web
    val estado = web.estado
    val pendiente = web.codigoPendiente
    val sesion = c.supabase.sesion
    val url = web.urlWeb
    val dispositivos = mutableStateListOf<Dispositivo>()
    var trabajando by mutableStateOf(false)
    /** Mensaje para mostrar: (texto, es error). */
    var aviso by mutableStateOf<Pair<String, Boolean>?>(null)

    fun falta() = web.falta()

    fun cargar() = viewModelScope.launch {
        if (web.falta() != null) return@launch
        runCatching { web.dispositivos() }.onSuccess { dispositivos.clear(); dispositivos.addAll(it) }
    }

    fun vincular(codigo: String, listo: (String) -> String, error: String) = viewModelScope.launch {
        trabajando = true
        aviso = runCatching { web.vincular(codigo) }.fold({ listo(it) to false }, { error to true })
        trabajando = false
        cargar()
    }

    fun desvincular(d: Dispositivo) = viewModelScope.launch {
        runCatching { web.desvincular(d.webUid) }
        cargar()
    }

    fun sincronizar(error: String) = viewModelScope.launch {
        runCatching { web.sincronizar() }.onFailure { aviso = error to true }
    }

    fun dejarDeCompartir(listo: String) = viewModelScope.launch {
        trabajando = true
        runCatching { web.dejarDeCompartir() }.onSuccess { aviso = listo to false }
        trabajando = false
        cargar()
    }
}

/**
 * Rutaalacima Web: la misma app en el computador. Desde aquí se abre la web, se vincula un
 * computador escaneando su código QR y se ven los computadores vinculados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(onBack: () -> Unit, onCuenta: () -> Unit) {
    val vm = rutaViewModel { WebViewModel(it) }
    val ctx = LocalContext.current
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val pendiente by vm.pendiente.collectAsStateWithLifecycle()
    var escribiendo by remember { mutableStateOf(false) }
    var porConfirmar by remember { mutableStateOf<String?>(null) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    val txtListo = stringResource(R.string.web_vinculado, "%s")
    val txtError = stringResource(R.string.web_error_vincular)
    val txtErrorSync = stringResource(R.string.web_error_sincronizar)
    val txtBorrado = stringResource(R.string.web_borrado)
    val txtInvalido = stringResource(R.string.web_codigo_invalido)
    val txtSinEscaner = stringResource(R.string.web_escaner_no)

    LaunchedEffect(sesion) { vm.cargar() }
    // Código que llegó por el QR escaneado con la cámara del teléfono
    LaunchedEffect(pendiente) {
        pendiente?.let { porConfirmar = it; vm.pendiente.value = null }
    }

    fun escanear() {
        val opciones = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(ctx, opciones).startScan()
            .addOnSuccessListener { b ->
                val c = Sincronia.codigoDeVinculo(b.rawValue.orEmpty())
                if (c != null) porConfirmar = c else vm.aviso = txtInvalido to true
            }
            .addOnFailureListener { vm.aviso = txtSinEscaner to true; escribiendo = true }
    }

    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.web_titulo)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RutaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.logo_sello), null, Modifier.size(44.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.web_intro), style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(vm.url))) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.web_abrir))
                    }
                    Text(vm.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item { SectionTitle(stringResource(R.string.web_vincular)) }
            item {
                RutaCard {
                    when (vm.falta()) {
                        RutaWebRepository.Falta.SERVIDOR ->
                            Text(stringResource(R.string.web_sin_servidor), style = MaterialTheme.typography.bodyMedium)
                        RutaWebRepository.Falta.CUENTA -> {
                            Text(stringResource(R.string.web_sin_cuenta), style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = onCuenta) { Text(stringResource(R.string.web_ir_cuenta)) }
                        }
                        null -> {
                            Text(stringResource(R.string.web_vincular_pasos), style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { escanear() }, modifier = Modifier.fillMaxWidth(), enabled = !vm.trabajando) {
                                Icon(Icons.Filled.QrCodeScanner, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.web_escanear))
                            }
                            OutlinedButton(onClick = { escribiendo = true }, modifier = Modifier.fillMaxWidth(), enabled = !vm.trabajando) {
                                Text(stringResource(R.string.web_escribir_codigo))
                            }
                        }
                    }
                    vm.aviso?.let { (t, error) ->
                        Text(t, style = MaterialTheme.typography.bodyMedium,
                            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (vm.falta() == null) {
                item { SectionTitle(stringResource(R.string.web_dispositivos)) }
                if (vm.dispositivos.isEmpty()) {
                    item { Text(stringResource(R.string.web_ninguno), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(vm.dispositivos, key = { it.webUid }) { d ->
                    RutaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Computer, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                                Text(d.nombre, style = MaterialTheme.typography.titleMedium)
                                d.ultimoUso?.let {
                                    Text(stringResource(R.string.web_ultimo_uso, DateUtils.getRelativeTimeSpanString(it.toEpochMilli()).toString()),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            TextButton(onClick = { vm.desvincular(d) }) { Text(stringResource(R.string.web_desvincular)) }
                        }
                    }
                }
                if (estado.activo) {
                    item {
                        RutaCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Sync, null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    when {
                                        estado.sincronizando -> stringResource(R.string.web_sincronizando)
                                        estado.ultima != null -> stringResource(R.string.web_sincronizado,
                                            DateUtils.getRelativeTimeSpanString(estado.ultima!!).toString())
                                        else -> stringResource(R.string.web_sin_sincronizar)
                                    },
                                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { vm.sincronizar(txtErrorSync) }, enabled = !estado.sincronizando) {
                                    Text(stringResource(R.string.web_sincronizar))
                                }
                            }
                            Text(stringResource(R.string.web_privacidad), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { confirmarBorrado = true }) {
                                Text(stringResource(R.string.web_borrar), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (escribiendo) {
        var texto by remember { mutableStateOf("") }
        val codigo = Sincronia.codigoDeVinculo(texto)
        AlertDialog(
            onDismissRequest = { escribiendo = false },
            title = { Text(stringResource(R.string.web_escribir_codigo)) },
            text = {
                OutlinedTextField(
                    texto, { texto = it.uppercase().take(9) }, singleLine = true,
                    label = { Text(stringResource(R.string.web_codigo)) },
                    placeholder = { Text("K7P4-QX9M") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
            },
            confirmButton = {
                TextButton(enabled = codigo != null, onClick = { escribiendo = false; porConfirmar = codigo }) {
                    Text(stringResource(R.string.continuar))
                }
            },
            dismissButton = { TextButton(onClick = { escribiendo = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }

    porConfirmar?.let { c ->
        AlertDialog(
            onDismissRequest = { porConfirmar = null },
            icon = { Icon(Icons.Filled.Computer, null) },
            title = { Text(stringResource(R.string.web_confirmar_titulo)) },
            text = { Text(stringResource(R.string.web_confirmar_texto, Sincronia.codigoLegible(c))) },
            confirmButton = {
                Button(onClick = {
                    porConfirmar = null
                    if (vm.falta() == null) vm.vincular(c, { nombre -> txtListo.replace("%s", nombre) }, txtError)
                }) { Text(stringResource(R.string.web_vincular_si)) }
            },
            dismissButton = { TextButton(onClick = { porConfirmar = null }) { Text(stringResource(R.string.cancelar)) } },
        )
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text(stringResource(R.string.web_borrar)) },
            text = { Text(stringResource(R.string.web_borrar_texto)) },
            confirmButton = {
                TextButton(onClick = { confirmarBorrado = false; vm.dejarDeCompartir(txtBorrado) }) {
                    Text(stringResource(R.string.web_borrar_si), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}
