package com.rutaalacima.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ApodosViewModel(private val c: AppContainer) : ViewModel() {
    val apodos = c.respuestas.observar("apodo").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    fun guardar(clave: String, texto: String) = viewModelScope.launch { c.respuestas.guardar("apodo", clave, texto.trim()) }
}

@Composable
fun CampoApodo(valor: String, onChange: (String) -> Unit) {
    // Las emisiones del guardado no deben reemplazar el borrador mientras se escribe.
    var texto by remember { mutableStateOf(valor) }
    var enfocado by remember { mutableStateOf(false) }
    LaunchedEffect(valor, enfocado) { if (!enfocado) texto = valor }
    OutlinedTextField(texto, { texto = it.take(32); onChange(texto) }, singleLine = true,
        label = { Text(stringResource(R.string.apodo_opcional)) },
        placeholder = { Text(stringResource(R.string.apodo_ejemplo)) },
        modifier = Modifier.fillMaxWidth().onFocusChanged { enfocado = it.isFocused })
}

@Composable
fun ApodoField(tipo: String, id: Long) {
    val vm = rutaViewModel { ApodosViewModel(it) }
    val apodos by vm.apodos.collectAsStateWithLifecycle()
    val clave = "apodo:$tipo:$id"
    var texto by remember(clave, apodos[clave]) { mutableStateOf(apodos[clave].orEmpty()) }
    CampoApodo(texto) { texto = it; vm.guardar(clave, it) }
}

@Composable
fun ApodoLista(tipo: String, id: Long) {
    val vm = rutaViewModel { ApodosViewModel(it) }
    val apodos by vm.apodos.collectAsStateWithLifecycle()
    apodos["apodo:$tipo:$id"]?.trim()?.takeIf { it.isNotEmpty() }?.let {
        Text(stringResource(R.string.apodo_entre_comillas, it), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
