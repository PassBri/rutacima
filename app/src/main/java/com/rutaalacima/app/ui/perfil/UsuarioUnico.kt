package com.rutaalacima.app.ui.perfil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.RedRepository
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** El @usuario: único, personal y nunca el correo. Es como te encuentran para escribirte. */
class UsuarioViewModel(private val c: AppContainer) : ViewModel() {
    private val correo get() = c.supabase.sesion.value?.email.orEmpty()
    val conCuenta get() = c.supabase.sesion.value != null
    var actual by mutableStateOf(c.social.usuarioPropio()); private set
    var texto by mutableStateOf(""); private set
    /** null = libre; "formato", "correo", "ocupado"; "" = aún sin revisar */
    var problema by mutableStateOf<String?>(""); private set
    var guardando by mutableStateOf(false); private set
    val pendiente get() = conCuenta && RedRepository.pendiente(actual, correo)
    val pareceCorreo get() = RedRepository.pareceCorreo(actual, correo)
    private var revisa: Job? = null

    init { viewModelScope.launch { if (conCuenta) actual = c.red.miUsuario() } }

    fun escribir(t: String) {
        texto = t.lowercase().filter { it.isLetterOrDigit() && it.code < 128 || it == '.' || it == '_' }.take(30)
        revisa?.cancel(); problema = ""
        val u = texto
        revisa = viewModelScope.launch { delay(350); problema = c.red.problemaUsuario(u, correo) }
    }

    fun guardar(ok: (String) -> Unit, error: () -> Unit) = viewModelScope.launch {
        val u = texto
        problema = c.red.problemaUsuario(u, correo)
        if (problema != null) return@launch
        guardando = true
        runCatching { c.red.cambiarUsuario(u) }
            .onSuccess { actual = u; texto = ""; problema = ""; ok(u) }
            .onFailure { e -> if ("duplicate" in e.message.orEmpty() || "unique" in e.message.orEmpty()) problema = "ocupado" else error() }
        guardando = false
    }
}

/** Campo para elegir el @usuario, con aviso en vivo de si está libre. */
@Composable
fun EditorUsuario(vm: UsuarioViewModel, onGuardado: (String) -> Unit = {}, onError: () -> Unit = {}) {
    val ayuda = when (vm.problema) {
        null -> stringResource(R.string.usuario_libre, vm.texto)
        "ocupado" -> stringResource(R.string.usuario_ocupado, vm.texto)
        "correo" -> stringResource(R.string.usuario_no_correo)
        else -> stringResource(R.string.usuario_formato)
    }
    Column {
        OutlinedTextField(
            vm.texto, vm::escribir, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(stringResource(R.string.usuario_titulo)) },
            placeholder = { Text(vm.actual.takeUnless { vm.pendiente }.orEmpty().ifBlank { "brian.suarez" }) },
            prefix = { Text("@") },
            isError = vm.problema == "ocupado" || vm.problema == "correo" || (vm.problema == "formato" && vm.texto.length >= 3),
            supportingText = { Text(ayuda, color = if (vm.problema == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = { vm.guardar(onGuardado, onError) }, enabled = vm.problema == null && !vm.guardando) { Text(stringResource(R.string.guardar)) }
    }
}

/**
 * Si el @usuario es el correo o el automático (senderista_…), una tarjeta invita a elegir uno propio.
 * Al tocarla se abre el editor en un diálogo.
 */
@Composable
fun AvisoUsuario(modifier: Modifier = Modifier, onCambio: (String) -> Unit = {}) {
    val vm = rutaViewModel { UsuarioViewModel(it) }
    var abierto by remember { mutableStateOf(false) }
    if (!vm.pendiente) return
    val txtError = stringResource(R.string.usuario_error)
    var error by remember { mutableStateOf<String?>(null) }
    RutaCard(modifier) {
        Text(stringResource(R.string.usuario_elegir), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.usuario_explica) + if (vm.pareceCorreo) " " + stringResource(R.string.usuario_correo_aviso) else "",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = { abierto = true }) { Text(stringResource(R.string.usuario_elegir)) }
    }
    if (abierto) AlertDialog(
        onDismissRequest = { abierto = false },
        title = { Text(stringResource(R.string.usuario_titulo)) },
        text = {
            Column {
                Text(stringResource(R.string.usuario_explica), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                EditorUsuario(vm, { abierto = false; onCambio(it) }, { error = txtError })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.cancelar)) } },
    )
    LaunchedEffect(vm.actual) { error = null }
}
