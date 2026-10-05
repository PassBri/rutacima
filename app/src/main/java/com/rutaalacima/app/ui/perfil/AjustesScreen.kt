package com.rutaalacima.app.ui.perfil

import com.rutaalacima.app.seguridad.ModoBloqueo
import com.rutaalacima.app.seguridad.Bloqueo
import com.rutaalacima.app.ui.theme.EstiloActual
import com.rutaalacima.app.ui.theme.EstiloPapel
import com.rutaalacima.app.ui.theme.fondoPapel
import com.rutaalacima.app.notificaciones.Recordatorios
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.rutaalacima.app.domain.model.Vida
import com.rutaalacima.app.ui.components.FechaField
import androidx.compose.material3.Slider
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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

    suspend fun respaldo(): String = c.web.respaldo()
    suspend fun restaurar(texto: String): Int = c.web.restaurar(texto)

    /** Elimina la cuenta y todo lo que está en el servidor; lo del teléfono se queda. */
    fun eliminarCuenta(textoOk: String, textoError: String) {
        cargando = true
        viewModelScope.launch {
            runCatching { c.supabase.rpc("eliminar_mi_cuenta") }
                .onSuccess { c.web.olvidarVinculo(); c.supabase.cerrarSesion(); mensaje = textoOk }
                .onFailure { mensaje = textoError }
            cargando = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(onBack: () -> Unit, onFrases: () -> Unit = {}, onWeb: () -> Unit = {}) {
    val vm = rutaViewModel { AjustesViewModel(it) }
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val idiomaActual = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').ifBlank {
        java.util.Locale.getDefault().language
    }

    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
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

            item { SectionTitle(stringResource(R.string.estilo)) }
            item {
                val ctx = LocalContext.current
                ChipSelector(
                    null, EstiloPapel.entries, EstiloActual.estilo,
                    {
                        stringResource(
                            when (it) {
                                EstiloPapel.BLANCO -> R.string.estilo_blanco
                                EstiloPapel.ANTIGUO -> R.string.estilo_antiguo
                                EstiloPapel.PASTEL -> R.string.estilo_pastel
                            },
                        )
                    },
                    { EstiloActual.cambiar(ctx, it) },
                )
            }

            item { SectionTitle(stringResource(R.string.bloqueo_titulo)) }
            item {
                val ctx = LocalContext.current
                val hayBiometria = remember { Bloqueo.biometriaDisponible(ctx) }
                var modo by remember { mutableStateOf(Bloqueo.modo(ctx)) }
                var creando by remember { mutableStateOf(false) }
                RutaCard {
                    Text(stringResource(R.string.bloqueo_ayuda), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChipSelector(
                        null, ModoBloqueo.entries.filter { it != ModoBloqueo.BIOMETRIA || hayBiometria }, modo,
                        {
                            stringResource(
                                when (it) {
                                    ModoBloqueo.BIOMETRIA -> R.string.bloqueo_rostro
                                    ModoBloqueo.CLAVE -> R.string.bloqueo_clave
                                    ModoBloqueo.TOQUE -> R.string.bloqueo_toque
                                },
                            )
                        },
                        { nuevo ->
                            if (nuevo == ModoBloqueo.CLAVE && !Bloqueo.tieneClave(ctx)) creando = true
                            else { modo = nuevo; Bloqueo.elegir(ctx, nuevo) }
                        },
                    )
                    if (!hayBiometria) Text(stringResource(R.string.bloqueo_sin_biometria), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {
                        if (Bloqueo.tieneClave(ctx)) TextButton(onClick = { creando = true }) { Text(stringResource(R.string.cambiar_clave)) }
                        TextButton(onClick = onFrases) { Text(stringResource(R.string.mis_frases)) }
                    }
                }
                if (creando) {
                    CrearClaveDialog(
                        onListo = { c -> Bloqueo.guardarClave(ctx, c); Bloqueo.elegir(ctx, ModoBloqueo.CLAVE); modo = ModoBloqueo.CLAVE; creando = false },
                        onCancelar = { creando = false },
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.recordatorio_titulo)) }
            item {
                val ctx = LocalContext.current
                val alcance = rememberCoroutineScope()
                var activo by remember { mutableStateOf(Recordatorios.activo(ctx)) }
                var hora by remember { mutableStateOf(Recordatorios.hora(ctx)) }
                val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                RutaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.recordatorio_ajuste), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(checked = activo, onCheckedChange = {
                            activo = it
                            Recordatorios.configurar(ctx, it, hora)
                            if (it && android.os.Build.VERSION.SDK_INT >= 33 && !Recordatorios.puedeNotificar(ctx)) {
                                permiso.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        })
                    }
                    if (activo) {
                        ChipSelector(stringResource(R.string.recordatorio_hora), Recordatorios.HORAS, hora,
                            { "%02d:00".format(it) }, { hora = it; Recordatorios.configurar(ctx, true, it) })
                        TextButton(onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= 33 && !Recordatorios.puedeNotificar(ctx)) {
                                permiso.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else alcance.launch { Recordatorios.mostrar(ctx) }
                        }) { Text(stringResource(R.string.recordatorio_probar)) }
                    }
                }
                // Avisos de mensajes, solicitudes y coach de vida
                var avisos by remember { mutableStateOf(com.rutaalacima.app.notificaciones.AvisosMensajes.activos(ctx)) }
                RutaCard(Modifier.padding(top = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.avisos_mensajes_ajuste), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.avisos_mensajes_ayuda), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = avisos, onCheckedChange = {
                            avisos = it
                            com.rutaalacima.app.notificaciones.AvisosMensajes.configurar(ctx, it)
                            if (it && android.os.Build.VERSION.SDK_INT >= 33 && !Recordatorios.puedeNotificar(ctx)) {
                                permiso.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        })
                    }
                }
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
                val valor = vm.esperanza ?: Vida.META_DEFECTO
                Text(stringResource(R.string.esperanza_vida, valor), style = MaterialTheme.typography.labelLarge)
                Slider(value = valor.toFloat(), onValueChange = { vm.esperanza = kotlin.math.round(it).toInt() }, valueRange = 60f..120f, steps = 59)
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
                            var confirmar by remember { mutableStateOf(false) }
                            TextButton(onClick = { confirmar = true }, enabled = !vm.cargando) {
                                Text(stringResource(R.string.cuenta_eliminar), color = MaterialTheme.colorScheme.error)
                            }
                            if (confirmar) {
                                var escrito by remember { mutableStateOf("") }
                                val palabra = stringResource(R.string.cuenta_eliminar_palabra)
                                val ok = stringResource(R.string.cuenta_eliminada)
                                val error = stringResource(R.string.cuenta_eliminar_error)
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { confirmar = false },
                                    title = { Text(stringResource(R.string.cuenta_eliminar)) },
                                    text = {
                                        Column {
                                            Text(stringResource(R.string.cuenta_eliminar_texto, palabra), style = MaterialTheme.typography.bodyMedium)
                                            OutlinedTextField(escrito, { escrito = it }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = { confirmar = false; vm.eliminarCuenta(ok, error) },
                                            enabled = escrito.trim().equals(palabra, ignoreCase = true),
                                        ) { Text(stringResource(R.string.cuenta_eliminar), color = MaterialTheme.colorScheme.error) }
                                    },
                                    dismissButton = { TextButton(onClick = { confirmar = false }) { Text(stringResource(R.string.cancelar)) } },
                                )
                            }
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

            item { SectionTitle(stringResource(R.string.web_titulo)) }
            item {
                RutaCard {
                    Text(stringResource(R.string.web_intro), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onWeb, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.web_abrir_o_vincular)) }
                }
            }

            item { SectionTitle(stringResource(R.string.respaldo_titulo)) }
            item {
                val ctx = LocalContext.current
                val alcance = rememberCoroutineScope()
                val txtGuardado = stringResource(R.string.respaldo_guardado)
                val txtError = stringResource(R.string.respaldo_error)
                var aviso by remember { mutableStateOf<String?>(null) }
                val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                    if (uri != null) alcance.launch {
                        aviso = runCatching {
                            val texto = vm.respaldo()
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                ctx.contentResolver.openOutputStream(uri)?.use { it.write(texto.toByteArray()) } ?: error("sin archivo")
                            }
                        }.fold({ txtGuardado }, { txtError })
                    }
                }
                val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) alcance.launch {
                        aviso = runCatching {
                            val texto = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("sin archivo")
                            }
                            vm.restaurar(texto)
                        }.fold({ n -> ctx.getString(R.string.respaldo_restaurado, n) }, { txtError })
                    }
                }
                RutaCard {
                    Text(stringResource(R.string.respaldo_texto), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { guardar.launch("rutaalacima-respaldo-${java.time.LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.respaldo_guardar))
                    }
                    OutlinedButton(onClick = { abrir.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.respaldo_restaurar))
                    }
                    aviso?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
                }
            }

            item { SectionTitle(stringResource(R.string.acerca_de)) }
            item {
                Text(stringResource(R.string.acerca_texto, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
                val abrirEnlace = androidx.compose.ui.platform.LocalUriHandler.current
                TextButton(onClick = { runCatching { abrirEnlace.openUri(BuildConfig.WEB_URL.trimEnd('/') + "/privacidad.html") } }) {
                    Text(stringResource(R.string.privacidad_politica))
                }
            }
        }
    }
}

/** Crear o cambiar la clave propia: se escribe dos veces y se guarda solo su hash. */
@Composable
private fun CrearClaveDialog(onListo: (String) -> Unit, onCancelar: () -> Unit) {
    var primera by remember { mutableStateOf<String?>(null) }
    var valor by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val noCoincide = stringResource(R.string.clave_no_coincide)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancelar,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
        text = {
            com.rutaalacima.app.ui.inicio.PinPad(
                titulo = stringResource(if (primera == null) R.string.clave_nueva else R.string.clave_repite),
                valor = valor, onCambio = { valor = it; error = null }, error = error,
                onConfirmar = {
                    val p = primera
                    when {
                        p == null -> { primera = valor; valor = "" }
                        p == valor -> onListo(valor)
                        else -> { primera = null; valor = ""; error = noCoincide }
                    }
                },
            )
        },
    )
}
