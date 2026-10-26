package com.rutaalacima.app.ui.mensajes

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.EnlacesRed
import com.rutaalacima.app.data.social.Huella
import com.rutaalacima.app.data.social.Persona
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.comunidad.Avatar
import com.rutaalacima.app.ui.theme.fondoPapel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Un contacto de la agenda (nunca sale del teléfono; solo sus huellas). */
data class ContactoAgenda(val nombre: String, val telefono: String, val correo: String, val huellas: List<String>)

class ContactosViewModel(private val c: AppContainer) : ViewModel() {
    val conCuenta get() = c.mensajes.disponible
    var resultados by mutableStateOf<List<Persona>?>(null); private set
    var enSuRoute by mutableStateOf<List<Pair<Persona, String>>?>(null); private set
    var invitar by mutableStateOf<List<ContactoAgenda>>(emptyList()); private set
    var revisando by mutableStateOf(false); private set
    var encontrable by mutableStateOf(false)
    var usuario by mutableStateOf(""); private set
    private var busca: Job? = null

    init {
        viewModelScope.launch {
            if (conCuenta) encontrable = c.mensajes.soyEncontrable()
            usuario = runCatching { c.social.usuarioPropio() }.getOrDefault("")
        }
    }

    fun buscar(t: String) {
        busca?.cancel()
        if (t.trim().length < 2) { resultados = null; return }
        busca = viewModelScope.launch { delay(300); resultados = c.red.buscar(t) }
    }

    /** Lee la agenda, calcula huellas aquí y pregunta al servidor quiénes ya están. */
    fun revisar(ctx: Context, onError: () -> Unit) = viewModelScope.launch {
        revisando = true
        val agenda = withContext(Dispatchers.IO) { leerAgenda(ctx) }
        val porHuella = agenda.flatMap { a -> a.huellas.map { it to a } }.toMap()
        val filas = if (conCuenta) runCatching { c.mensajes.encontrarContactos(porHuella.keys.toList()) }.getOrElse { onError(); revisando = false; return@launch }
        else c.red.buscar("a").take(2).mapIndexed { i, p -> (porHuella.keys.elementAtOrNull(i) ?: "") to p }
        val vistos = mutableSetOf<ContactoAgenda>()
        enSuRoute = filas.distinctBy { it.second.id }.map { (h, p) -> porHuella[h]?.let(vistos::add); p to (porHuella[h]?.nombre ?: "") }
        invitar = agenda.filter { it !in vistos }.sortedBy { it.nombre.lowercase() }
        revisando = false
    }

    fun guardarEncontrable(activo: Boolean, tel: String, ok: () -> Unit, error: () -> Unit) = viewModelScope.launch {
        runCatching { c.mensajes.hacermeEncontrable(activo, tel) }.onSuccess { encontrable = activo; ok() }.onFailure { error() }
    }

    fun escribir(id: String, onChat: (String) -> Unit) = viewModelScope.launch { runCatching { c.mensajes.abrirCon(id) }.onSuccess(onChat) }

    private fun leerAgenda(ctx: Context): List<ContactoAgenda> {
        val datos = linkedMapOf<Long, Triple<String, MutableList<String>, MutableList<String>>>()
        val cr = ctx.contentResolver
        cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null)?.use { q ->
            while (q.moveToNext()) {
                val d = datos.getOrPut(q.getLong(0)) { Triple(q.getString(1).orEmpty(), mutableListOf(), mutableListOf()) }
                q.getString(2)?.let(d.second::add)
            }
        }
        cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.CONTACT_ID, ContactsContract.CommonDataKinds.Email.DISPLAY_NAME, ContactsContract.CommonDataKinds.Email.ADDRESS),
            null, null, null)?.use { q ->
            while (q.moveToNext()) {
                val d = datos.getOrPut(q.getLong(0)) { Triple(q.getString(1).orEmpty(), mutableListOf(), mutableListOf()) }
                q.getString(2)?.let(d.third::add)
            }
        }
        return datos.values.mapNotNull { (nombre, tels, correos) ->
            val h = tels.mapNotNull(Huella::tel).distinct().map { Huella.de("tel", it) } + correos.mapNotNull(Huella::correo).distinct().map { Huella.de("correo", it) }
            if (h.isEmpty()) null else ContactoAgenda(nombre.ifBlank { tels.firstOrNull() ?: correos.first() }, tels.firstOrNull().orEmpty(), correos.firstOrNull().orEmpty(), h)
        }
    }
}

/** Agregar contactos: buscar en SuRoute, revisar la agenda, invitar desde otras redes y dejarse encontrar. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactosScreen(onBack: () -> Unit, onChat: (String) -> Unit, onPersona: (String) -> Unit) {
    val vm = rutaViewModel { ContactosViewModel(it) }
    val ctx = LocalContext.current
    val avisos = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val aviso: (String) -> Unit = { t -> scope.launch { avisos.showSnackbar(t) } }
    var texto by rememberSaveable { mutableStateOf("") }
    var tel by rememberSaveable { mutableStateOf("") }
    val txtPermiso = stringResource(R.string.contactos_permiso)
    val txtError = stringResource(R.string.contactos_error)
    val txtCopiado = stringResource(R.string.contactos_copiado)
    val txtListo = stringResource(R.string.contactos_guardado)
    val invitacion = stringResource(R.string.contactos_invitacion, EnlacesRed.invitacion(vm.usuario))
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { si ->
        if (si) vm.revisar(ctx) { aviso(txtError) } else aviso(txtPermiso)
    }
    fun revisar() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) vm.revisar(ctx) { aviso(txtError) }
        else pedir.launch(Manifest.permission.READ_CONTACTS)
    }

    Scaffold(
        modifier = Modifier.fondoPapel(), containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.contactos_titulo)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("sub") { Text(stringResource(R.string.contactos_sub), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
            item("usuario") { com.rutaalacima.app.ui.perfil.AvisoUsuario() }
            item("buscar") {
                OutlinedTextField(
                    texto, { texto = it; vm.buscar(it) }, Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.contactos_buscar)) }, singleLine = true, shape = RoundedCornerShape(28.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = if (texto.isNotEmpty()) ({ IconButton(onClick = { texto = ""; vm.buscar("") }) { Icon(Icons.Filled.Close, null) } }) else null,
                )
            }
            vm.resultados?.let { lista ->
                if (lista.isEmpty()) item("nada") { Text(stringResource(R.string.red_sin_resultados), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(lista, key = { "r" + it.id }) { p -> FilaContacto(p, if (p.cumbre.isNotBlank()) "@${p.usuario} · ${p.cumbre}" else "@${p.usuario}", { onPersona(p.id) }) { vm.escribir(p.id, onChat) } }
            }

            // Contactos del teléfono
            item("agenda") {
                RutaCard {
                    Encabezado(Icons.Filled.Contacts, stringResource(R.string.contactos_telefono))
                    Text(stringResource(R.string.contactos_telefono_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    if (vm.enSuRoute == null) Button(onClick = ::revisar, enabled = !vm.revisando) { Text(stringResource(R.string.contactos_revisar)) }
                    else {
                        val n = vm.enSuRoute.orEmpty().size
                        Text(if (n > 0) stringResource(R.string.contactos_encontrados, n) else stringResource(R.string.contactos_ninguno),
                            style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            items(vm.enSuRoute.orEmpty(), key = { "e" + it.first.id }) { (p, agenda) ->
                FilaContacto(p, agenda.ifBlank { "@${p.usuario}" }, { onPersona(p.id) }) { vm.escribir(p.id, onChat) }
            }
            if (vm.invitar.isNotEmpty()) {
                item("invitalos") { Text(stringResource(R.string.contactos_invitalos), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
                items(vm.invitar.take(200), key = { "i" + it.huellas.first() }) { a ->
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Avatar(a.nombre, "", 40) },
                        headlineContent = { Text(a.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(a.telefono.ifBlank { a.correo }, maxLines = 1) },
                        trailingContent = {
                            OutlinedButton(onClick = {
                                val i = if (a.telefono.isNotBlank()) Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(a.telefono))).putExtra("sms_body", invitacion)
                                else Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + a.correo)).putExtra(Intent.EXTRA_TEXT, invitacion)
                                runCatching { ctx.startActivity(i) }
                            }) { Text(stringResource(R.string.contactos_invitar)) }
                        },
                    )
                }
            }

            // Otras redes
            item("redes") {
                RutaCard {
                    Encabezado(Icons.Filled.Share, stringResource(R.string.contactos_redes))
                    Text(stringResource(R.string.contactos_redes_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WhatsApp" to "com.whatsapp", "Instagram" to "com.instagram.android", "Telegram" to "org.telegram.messenger",
                            "Messenger" to "com.facebook.orca", "TikTok" to "com.zhiliaoapp.musically", "X" to "com.twitter.android").forEach { (nombre, paquete) ->
                            AssistChip(onClick = { compartir(ctx, invitacion, paquete) }, label = { Text(nombre) })
                        }
                        AssistChip(onClick = { compartir(ctx, invitacion, null) }, label = { Text(stringResource(R.string.contactos_mas)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp)) })
                        AssistChip(onClick = {
                            (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("SuRoute", invitacion)); aviso(txtCopiado)
                        }, label = { Text(stringResource(R.string.contactos_copiar)) }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp)) })
                    }
                }
            }

            // Que me encuentren
            item("encontrable") {
                RutaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Encabezado(Icons.Filled.Visibility, stringResource(R.string.contactos_encontrable), Modifier.weight(1f))
                        if (vm.conCuenta) Switch(vm.encontrable, { si -> vm.guardarEncontrable(si, tel, { aviso(txtListo) }) { aviso(txtError) } })
                    }
                    Text(stringResource(if (vm.conCuenta) R.string.contactos_encontrable_texto else R.string.contactos_sin_cuenta),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (vm.conCuenta && vm.encontrable) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(tel, { tel = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.contactos_tu_numero)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(onClick = { vm.guardarEncontrable(true, tel, { aviso(txtListo) }) { aviso(txtError) } }) { Text(stringResource(R.string.contactos_guardar)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Encabezado(icono: ImageVector, texto: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(texto, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun FilaContacto(p: Persona, sub: String, onAbrir: () -> Unit, onMensaje: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onAbrir),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Avatar(p.nombre, p.avatar, 44) },
        headlineContent = { Text(p.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = { Button(onClick = onMensaje) { Text(stringResource(R.string.contactos_mensaje)) } },
    )
}

/** Comparte la invitación en una app concreta (si está instalada) o con el menú del sistema. */
private fun compartir(ctx: Context, texto: String, paquete: String?) {
    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, texto) }
    if (paquete != null) {
        val directo = Intent(i).setPackage(paquete)
        if (runCatching { ctx.startActivity(directo) }.isSuccess) return
    }
    ctx.startActivity(Intent.createChooser(i, null))
}
