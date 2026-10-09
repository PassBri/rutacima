package com.rutaalacima.app.ui.legal

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rutaalacima.app.BuildConfig
import com.rutaalacima.app.R
import com.rutaalacima.app.RutaApp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Documentos legales (web/legal.html y compañía) y prueba de la autorización de tratamiento de datos
 * (Ley 1581 de 2012). La versión aceptada viaja al registrarse ("legal_version" en los metadatos) o
 * con aceptar_legal() para las cuentas anteriores; aquí se recuerda por cuenta para no preguntar dos veces.
 */
object Legal {
    /** Sube este número cuando cambien los documentos de forma sustancial: se vuelve a pedir la autorización. */
    const val VERSION = "1.0"

    enum class Doc(val archivo: String, @StringRes val titulo: Int) {
        CENTRO("legal.html", R.string.legal_centro),
        PRIVACIDAD("privacidad.html", R.string.privacidad_politica),
        TERMINOS("terminos.html", R.string.legal_terminos),
        AVISO("aviso-privacidad.html", R.string.legal_aviso),
        AUTORIZACION("autorizacion.html", R.string.legal_autorizacion),
        NORMAS("normas.html", R.string.legal_normas),
    }

    fun url(d: Doc) = BuildConfig.WEB_URL.trimEnd('/') + "/" + d.archivo

    private const val PREFS = "rutacima_legal"
    private fun clave(usuario: String) = "aceptada_$usuario"
    fun aceptada(c: Context, usuario: String) =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(clave(usuario), null) == VERSION
    fun marcar(c: Context, usuario: String) =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(clave(usuario), VERSION).apply()
}

/** Enlaces a los documentos (Ajustes). */
@Composable
fun LegalEnlaces() {
    val abrir = LocalUriHandler.current
    Column {
        Text(stringResource(R.string.legal_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Legal.Doc.entries.forEach { d ->
            TextButton(onClick = { runCatching { abrir.openUri(Legal.url(d)) } }) { Text(stringResource(d.titulo)) }
        }
    }
}

/** Casilla con texto (toda la fila se puede tocar). */
@Composable
private fun Casilla(marcada: Boolean, onCambio: (Boolean) -> Unit, texto: String) {
    Row(
        Modifier.fillMaxWidth().toggleable(marcada, role = Role.Checkbox, onValueChange = onCambio).padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(checked = marcada, onCheckedChange = null, modifier = Modifier.padding(end = 8.dp, top = 2.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall)
    }
}

/** Autorización al crear la cuenta: sin las dos casillas no se puede registrar. */
@Composable
fun ConsentimientoRegistro(acepto: Boolean, onAcepto: (Boolean) -> Unit, edad: Boolean, onEdad: (Boolean) -> Unit) {
    val abrir = LocalUriHandler.current
    Column {
        Casilla(acepto, onAcepto, stringResource(R.string.registro_acepto))
        Casilla(edad, onEdad, stringResource(R.string.registro_edad))
        TextButton(onClick = { runCatching { abrir.openUri(Legal.url(Legal.Doc.CENTRO)) } }) { Text(stringResource(R.string.registro_leer)) }
    }
}

/**
 * Para cuentas que aún no han aceptado esta versión: pide la autorización una vez. Si el servidor ya
 * la tiene (por ejemplo, la dio al registrarse en otro teléfono), solo se recuerda aquí.
 */
@Composable
fun AutorizacionPendiente() {
    val ctx = LocalContext.current
    val c = remember { (ctx.applicationContext as RutaApp).container }
    val sesion by c.supabase.sesion.collectAsStateWithLifecycle()
    val usuario = sesion?.userId ?: return
    var pendiente by remember(usuario) { mutableStateOf(false) }
    var omitida by remember(usuario) { mutableStateOf(false) }
    LaunchedEffect(usuario) {
        if (Legal.aceptada(ctx, usuario)) return@LaunchedEffect
        val enServidor = runCatching {
            (c.supabase.select("consentimientos", "select=version&version=eq.${Legal.VERSION}") as? JsonArray)?.isNotEmpty() == true
        }.getOrNull() ?: return@LaunchedEffect // sin conexión: se pregunta en otro momento
        if (enServidor) Legal.marcar(ctx, usuario) else pendiente = true
    }
    if (!pendiente || omitida) return

    val alcance = rememberCoroutineScope()
    val abrir = LocalUriHandler.current
    var acepto by remember { mutableStateOf(false) }
    var edad by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.legal_pendiente_titulo)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.legal_pendiente_texto), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { runCatching { abrir.openUri(Legal.url(Legal.Doc.CENTRO)) } }) { Text(stringResource(R.string.registro_leer)) }
                Casilla(acepto, { acepto = it }, stringResource(R.string.registro_acepto))
                Casilla(edad, { edad = it }, stringResource(R.string.registro_edad))
                if (error) Text(stringResource(R.string.legal_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            if (guardando) CircularProgressIndicator(Modifier.padding(8.dp))
            else TextButton(enabled = acepto && edad, onClick = {
                guardando = true; error = false
                alcance.launch {
                    runCatching { c.supabase.rpc("aceptar_legal", buildJsonObject { put("p_version", JsonPrimitive(Legal.VERSION)); put("p_origen", JsonPrimitive("app")) }) }
                        .onSuccess { Legal.marcar(ctx, usuario); pendiente = false }
                        .onFailure { error = true }
                    guardando = false
                }
            }) { Text(stringResource(R.string.legal_aceptar)) }
        },
        dismissButton = { TextButton(onClick = { omitida = true }) { Text(stringResource(R.string.legal_ahora_no)) } },
    )
}
