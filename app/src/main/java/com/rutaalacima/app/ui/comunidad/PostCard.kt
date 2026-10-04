package com.rutaalacima.app.ui.comunidad

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.data.social.TipoPost
import com.rutaalacima.app.data.social.Visibilidad
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.ui.components.EjeChip
import com.rutaalacima.app.ui.components.MontanaArte
import com.rutaalacima.app.ui.i18n.texto
import com.rutaalacima.app.util.haceCuanto
import java.io.File

/** Avatar circular con la inicial (o la foto si existe). */
@Composable
fun Avatar(nombre: String, url: String = "", tamano: Int = 40) {
    Box(
        Modifier.size(tamano.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNotBlank()) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else {
            // Primer carácter (respeta emojis de dos unidades UTF-16).
            val ini = if (nombre.isNotEmpty() && Character.isHighSurrogate(nombre[0])) nombre.take(2) else nombre.take(1).uppercase()
            Text(ini, color = Color.White, fontWeight = FontWeight.Bold, style = if (tamano > 48) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium)
        }
    }
}

/** Foto de la publicación o, si no tiene, un paisaje vectorial con el texto (estilo historia). */
@Composable
fun PostImagen(post: Post, modifier: Modifier = Modifier, conTexto: Boolean = true) {
    if (post.foto.isNotBlank()) {
        val modelo: Any = if (post.foto.startsWith("http")) post.foto else File(post.foto)
        AsyncImage(modelo, null, modifier.aspectRatio(1f), contentScale = ContentScale.Crop)
    } else {
        MontanaArte(post.id, modifier.aspectRatio(1f)) {
            if (conTexto) {
                Box(Modifier.fillMaxSize().background(Color(0x66000000)).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        post.metaTitulo.ifBlank { post.texto }, color = Color.White, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 6, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Tarjeta de publicación estilo Instagram: autor, foto, impulsos, comentarios y compartir. */
@Composable
fun PostCard(
    post: Post,
    onImpulsar: () -> Unit,
    onComentar: () -> Unit,
    onAbrir: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        onClick = onAbrir,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(post.autorNombre, post.avatarUrl)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(post.autorNombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOfNotNull(post.autorUsuario.takeIf { it.isNotBlank() }?.let { "@$it" }, haceCuanto(post.creadoEn)).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (post.visibilidad == Visibilidad.PRIVADA) Icon(Icons.Filled.Lock, stringResource(R.string.visibilidad_privada), Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            TipoBadge(post.tipo)
        }
        PostImagen(post, Modifier.fillMaxWidth())
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onImpulsar) {
                Icon(
                    if (post.yoImpulse) Icons.Filled.Bolt else Icons.Outlined.Bolt, stringResource(R.string.impulsar),
                    tint = if (post.yoImpulse) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text("${post.impulsos}", style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = onComentar) { Icon(Icons.AutoMirrored.Outlined.Comment, stringResource(R.string.comentar)) }
            Text("${post.comentarios}", style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = {
                val texto = listOf(post.metaTitulo, post.texto, "— RutaCima").filter { it.isNotBlank() }.joinToString("\n\n")
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), null))
            }) { Icon(Icons.Outlined.Share, stringResource(R.string.compartir)) }
            Spacer(Modifier.weight(1f))
            Eje.fromCodigo(post.eje)?.let { EjeChip(it, Modifier.padding(end = 8.dp)) }
        }
        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (post.metaTitulo.isNotBlank() && post.foto.isNotBlank()) {
                Text("🎯 ${post.metaTitulo}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            if (post.texto.isNotBlank()) Text(post.texto, style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TipoBadge(tipo: TipoPost) {
    val emoji = when (tipo) {
        TipoPost.LOGRO -> "🏆"; TipoPost.EVIDENCIA -> "📸"; TipoPost.VISION -> "✨"; TipoPost.META -> "🎯"; TipoPost.REFLEXION -> "💭"
    }
    Text(
        "$emoji ${tipo.texto()}",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
