package com.rutaalacima.app.ui.finanzas

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.seguridad.Bloqueo
import com.rutaalacima.app.seguridad.FinAprobacion
import com.rutaalacima.app.seguridad.FinCifrado
import com.rutaalacima.app.ui.theme.AvisoGlobal
import kotlinx.coroutines.launch

/** El QR del Ascenso financiero de SuRoute Web, escaneado con la cámara: se aprueba con la huella. */
@Composable
fun AprobarFinanzasWeb(c: AppContainer) {
    val pedido by FinAprobacion.pendiente.collectAsStateWithLifecycle()
    val (id, llave) = pedido ?: return
    val ctx = LocalContext.current
    val alcance = rememberCoroutineScope()
    var trabajando by remember { mutableStateOf(false) }
    val titulo = stringResource(R.string.fa_titulo); val sub = stringResource(R.string.fa_desc)
    val listo = stringResource(R.string.fa_listo); val error = stringResource(R.string.fa_error); val sinCuenta = stringResource(R.string.fa_cuenta)
    val cerrar = { FinAprobacion.pendiente.value = null }
    val enviar = {
        trabajando = true
        alcance.launch {
            // Con cifrado, la misma huella abre la llave guardada en el teléfono para entregársela a la web
            if (!FinCifrado.abierta()) FinCifrado.abrirConTelefono(ctx)
            val ok = runCatching { FinAprobacion.aprobar(c.supabase, id, llave) }.isSuccess
            AvisoGlobal.mostrar(if (ok) listo else error); trabajando = false; cerrar()
        }
        Unit
    }
    AlertDialog(
        onDismissRequest = { if (!trabajando) cerrar() },
        icon = { Icon(Icons.Filled.Fingerprint, null) },
        title = { Text(titulo) }, text = { Text(sub) },
        confirmButton = {
            Button(enabled = !trabajando, onClick = {
                if (c.supabase.sesion.value == null) { AvisoGlobal.mostrar(sinCuenta); cerrar(); return@Button }
                val act = Bloqueo.actividad(ctx)
                if (act != null && Bloqueo.biometriaDisponible(ctx)) Bloqueo.pedirBiometria(act, titulo, sub, onExito = { enviar() }, onError = { if (it.isNotBlank()) AvisoGlobal.mostrar(it) })
                else enviar()
            }) { Text(stringResource(R.string.fa_aprobar)) }
        },
        dismissButton = { TextButton(enabled = !trabajando, onClick = cerrar) { Text(stringResource(R.string.cu_cif_cancelar)) } },
    )
}
