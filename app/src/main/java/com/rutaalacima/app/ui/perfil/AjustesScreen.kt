package com.rutaalacima.app.ui.perfil

import com.rutaalacima.app.domain.model.Vida
import com.rutaalacima.app.ui.components.FechaField
import androidx.compose.material3.Slider
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.BuildConfig
import com.rutaalacima.app.R
import com.rutaalacima.app.data.remote.Sesion
import com.rutaalacima.app.ui.components.ChipSelector
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Los 12 idiomas de la interfaz (código → nombre en su propio idioma). */
val IDIOMAS = listOf(
    "es" to "Español", "en" to "English", "pt" to "Português", "fr" to "Français",
    "de" to "Deutsch", "it" to "Italiano", "zh" to "中文", "ja" to "日本語",
    "ko" to "한국어", "ar" to "العربية", "hi" to "हिन्दी", "ru" to "Русский",
)

class AjustesViewModel(private val c: AppContainer) : ViewModel() {
    val sesion: StateFlow<Sesion?> = c.supabase.sesion
    val servidor: Boolean get() = c.supabase.configurado
    var nombre by mutableStateOf("")
    var cumbre by mutableStateOf("")
    /** Nacimiento como fecha UTC del día 1 del mes (para el selector de fecha). */
    var nacimiento by mutableStateOf<Long?>(null)
    var esperanza by mutableStateOf<Int?>(null)
    var email by mutableStateOf("")
    var clave by mutableStateOf("")
    var usuario by mutableStateOf(c.social.usuarioPropio())
    var registrando by mutableStateOf(false)
    var cargando by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            val p = c.perfil.perfil.first()
            nombre = p.nombre
            cumbre = p.cumbreFrase
            nacimiento = p.anioNacimiento?.let { a ->
                java.time.LocalDate.of(a, p.mesNacimiento ?: 1, 1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            }
            esperanza = p.esperanzaVida
        }
    }

    fun guardarPerfil() = viewModelScope.launch {
        val fecha = nacimiento?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate() }
        c.perfil.actualizar {
            it.copy(
                nombre = nombre.trim(), cumbreFrase = cumbre.trim(),
                anioNacimiento = fecha?.year, mesNacimiento = fecha?.monthValue, esperanzaVida = esperanza,
            )
        }
        c.social.guardarIdentidad(nombre.trim(), usuario.trim())
        mensaje = null
    }

    fun entrar(textoOk: String, textoConfirmar: String) {
        cargando = true
        viewModelScope.launch {
            runCatching {
                if (registrando) {
                    val s = c.supabase.registrarse(email.trim(), clave, usuario.trim().lowercase(), nombre.trim())
                    c.social.guardarIdentidad(nombre.trim(), usuario.trim().lowercase())
                    if (s == null) textoConfirmar else textoOk
                } else {
                    c.supabase.iniciarSesion(email.trim(), clave)
                    textoOk
                }
            }.onSuccess { mensaje = it; clave = "" }.onFailure { mensaje = it.message }
            cargando = false
        }
    }

    fun salir() = c.supabase.cerrarSesion()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(onBack: () -> Unit) {
    val vm = rutaViewModel { AjustesViewModel(it) }
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val idiomaActual = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').ifBlank {
        java.util.Locale.getDefault().language
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ajustes)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle(stringResource(R.string.idioma)) }
            item {
                ChipSelector(null, IDIOMAS, IDIOMAS.firstOrNull { it.first == idiomaActual }, { it.second }, { (codigo, _) ->
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codigo))
                })
                Text(stringResource(R.string.idioma_contenido), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item { SectionTitle(stringResource(R.string.mi_perfil)) }
            item {
                OutlinedTextField(vm.nombre, { vm.nombre = it }, label = { Text(stringResource(R.string.tu_nombre)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(vm.cumbre, { vm.cumbre = it }, label = { Text(stringResource(R.string.mi_cumbre_es)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(Modifier.height(8.dp))
                FechaField(stringResource(R.string.fecha_nacimiento), vm.nacimiento, { vm.nacimiento = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                val estimada = Vida.esperanzaPais(java.util.Locale.getDefault().country)
                val valor = vm.esperanza ?: estimada
                Text(stringResource(R.string.esperanza_vida, valor), style = MaterialTheme.typography.labelLarge)
                Slider(value = valor.toFloat(), onValueChange = { vm.esperanza = kotlin.math.round(it).toInt() }, valueRange = 50f..110f, steps = 59)
                Text(stringResource(R.string.esperanza_ayuda, estimada), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.guardarPerfil() }) { Text(stringResource(R.string.guardar)) }
            }

            item { SectionTitle(stringResource(R.string.cuenta_comunidad)) }
            item {
                RutaCard {
                    val s = sesion
                    when {
                        !vm.servidor -> Text(stringResource(R.string.servidor_no_configurado), style = MaterialTheme.typography.bodyMedium)
                        s != null -> {
                            Text(stringResource(R.string.sesion_iniciada, s.email), style = MaterialTheme.typography.bodyLarge)
                            TextButton(onClick = vm::salir) { Text(stringResource(R.string.cerrar_sesion)) }
                        }
                        else -> {
                            Text(stringResource(if (vm.registrando) R.string.crear_cuenta else R.string.iniciar_sesion), style = MaterialTheme.typography.titleMedium)
                            if (vm.registrando) {
                                OutlinedTextField(vm.usuario, { vm.usuario = it.filter { ch -> ch.isLetterOrDigit() || ch == '.' || ch == '_' } },
                                    label = { Text(stringResource(R.string.usuario)) }, prefix = { Text("@") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            }
                            OutlinedTextField(vm.email, { vm.email = it }, label = { Text(stringResource(R.string.correo)) }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                            OutlinedTextField(vm.clave, { vm.clave = it }, label = { Text(stringResource(R.string.contrasena)) }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                            Spacer(Modifier.height(8.dp))
                            val ok = stringResource(R.string.bienvenido_comunidad)
                            val confirmar = stringResource(R.string.confirma_correo)
                            if (vm.cargando) CircularProgressIndicator()
                            else Button(
                                onClick = { vm.entrar(ok, confirmar) }, modifier = Modifier.fillMaxWidth(),
                                enabled = vm.email.contains('@') && vm.clave.length >= 6 && (!vm.registrando || vm.usuario.length >= 3),
                            ) { Text(stringResource(if (vm.registrando) R.string.crear_cuenta else R.string.iniciar_sesion)) }
                            OutlinedButton(onClick = { vm.registrando = !vm.registrando }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(if (vm.registrando) R.string.ya_tengo_cuenta else R.string.no_tengo_cuenta))
                            }
                        }
                    }
                    vm.mensaje?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
                }
            }

            item { SectionTitle(stringResource(R.string.acerca_de)) }
            item {
                Text(stringResource(R.string.acerca_texto, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
