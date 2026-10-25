package com.rutaalacima.app.ui.ruta

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rutaalacima.app.R
import com.rutaalacima.app.data.FotosPerfil
import com.rutaalacima.app.data.fotoVisible
import com.rutaalacima.app.data.local.PerfilEntity
import com.rutaalacima.app.ui.components.MontanaArte
import com.rutaalacima.app.ui.components.RutaCard
import java.io.File

/** Coil: una ruta local se carga como archivo; una URL, como URL. */
private fun modeloFoto(s: String): Any = if (s.startsWith("/")) File(s) else s

/**
 * Cabecera de Mi vida, como un perfil: foto de portada, foto de perfil, nombre, edad y la cumbre en una frase.
 * Sin portada propia se ve la cordillera de la persona (la misma de la web).
 */
@Composable
fun MiVidaCabecera(
    perfil: PerfilEntity,
    edad: Int?,
    diaDeVida: Int?,
    onFoto: (FotosPerfil.Tipo) -> Unit,
    onQuitar: (FotosPerfil.Tipo) -> Unit,
) {
    val portada = fotoVisible(perfil.fotoPortada, perfil.fotoPortadaUrl)
    val foto = fotoVisible(perfil.fotoPerfil, perfil.fotoPerfilUrl)
    var menu by remember { mutableStateOf<FotosPerfil.Tipo?>(null) }
    val tocar: (FotosPerfil.Tipo, Boolean) -> Unit = { t, hay -> if (hay) menu = t else onFoto(t) }

    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            // Portada
            Box(Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(24.dp))) {
                if (portada.isNotBlank()) {
                    AsyncImage(modeloFoto(portada), stringResource(R.string.mv_portada), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    MontanaArte("portada-${perfil.nombre}", Modifier.fillMaxSize(), bandera = false)
                }
                Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                    FilledTonalIconButton(onClick = { tocar(FotosPerfil.Tipo.PORTADA, portada.isNotBlank()) }) {
                        Icon(Icons.Filled.PhotoCamera, stringResource(R.string.mv_cambiar_portada))
                    }
                    MenuFoto(menu == FotosPerfil.Tipo.PORTADA, { menu = null }, R.string.mv_cambiar_portada,
                        { onFoto(FotosPerfil.Tipo.PORTADA) }, { onQuitar(FotosPerfil.Tipo.PORTADA) })
                }
            }
            // Foto de perfil, montada sobre el borde de la portada
            Box(Modifier.padding(start = 20.dp).offset(y = 124.dp)) {
                Box(
                    Modifier.size(104.dp).clip(CircleShape).border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClickLabel = stringResource(R.string.mv_cambiar_foto)) { tocar(FotosPerfil.Tipo.PERFIL, foto.isNotBlank()) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (foto.isNotBlank()) {
                        AsyncImage(modeloFoto(foto), stringResource(R.string.mv_foto_perfil), Modifier.fillMaxSize().padding(4.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop)
                    } else {
                        Text(perfil.nombre.trim().take(1).uppercase().ifBlank { "·" }, style = MaterialTheme.typography.displaySmall,
                            fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                // Distintivo de cámara
                Box(
                    Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.PhotoCamera, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                MenuFoto(menu == FotosPerfil.Tipo.PERFIL, { menu = null }, R.string.mv_cambiar_foto,
                    { onFoto(FotosPerfil.Tipo.PERFIL) }, { onQuitar(FotosPerfil.Tipo.PERFIL) })
            }
        }
        Spacer(Modifier.height(60.dp))
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(perfil.nombre.ifBlank { stringResource(R.string.mv_titulo) }, style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
            if (edad != null && diaDeVida != null) {
                Text(stringResource(R.string.mv_edad_dia, edad, java.text.NumberFormat.getIntegerInstance().format(diaDeVida)),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (perfil.cumbreFrase.isNotBlank()) {
                Text("«${perfil.cumbreFrase}»", style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun MenuFoto(abierto: Boolean, cerrar: () -> Unit, cambiar: Int, onCambiar: () -> Unit, onQuitar: () -> Unit) {
    DropdownMenu(expanded = abierto, onDismissRequest = cerrar) {
        DropdownMenuItem(text = { Text(stringResource(cambiar)) }, onClick = { cerrar(); onCambiar() })
        DropdownMenuItem(text = { Text(stringResource(R.string.mv_quitar_foto)) }, onClick = { cerrar(); onQuitar() })
    }
}

/** «Quién soy»: la presentación en primera persona. Se edita aquí mismo. */
@Composable
fun QuienSoyCard(texto: String, onGuardar: (String) -> Unit) {
    var editando by rememberSaveable { mutableStateOf(false) }
    var borrador by rememberSaveable(texto) { mutableStateOf(texto) }
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.mv_quien_soy), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif,
                modifier = Modifier.weight(1f))
            if (!editando) TextButton(onClick = { borrador = texto; editando = true }) {
                Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text(stringResource(R.string.editar))
            }
        }
        if (editando) {
            OutlinedTextField(borrador, { borrador = it.take(1200) }, Modifier.fillMaxWidth().padding(top = 6.dp), minLines = 4,
                placeholder = { Text(stringResource(R.string.mv_quien_soy_pista)) })
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { editando = false }) { Text(stringResource(R.string.cancelar)) }
                TextButton(onClick = { onGuardar(borrador.trim()); editando = false }) { Text(stringResource(R.string.guardar)) }
            }
        } else if (texto.isNotBlank()) {
            Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
        } else {
            Text(stringResource(R.string.mv_quien_soy_vacio), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)
                    .clickable { borrador = ""; editando = true })
        }
    }
}
