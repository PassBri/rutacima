package com.rutaalacima.app.ui.comunidad

import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.ui.graphics.vector.ImageVector
import com.rutaalacima.app.ui.theme.asColor
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.foundation.clickable
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
fun Avatar(nombre: String, url: String = "", tamano: Int = 40, forma: androidx.compose.ui.graphics.Shape = CircleShape) {
    Box(
        Modifier.size(tamano.dp).clip(forma)
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

/**
 * Imagen de la publicación. Formato propio de Rutaalacima: horizontal 4:3 (la "postal de cumbre"),
 * distinto del cuadrado/vertical de otras redes. Sin foto, se dibuja un paisaje vectorial.
 */
@Composable
fun PostImagen(post: Post, modifier: Modifier = Modifier, conTexto: Boolean = true, ratio: Float = 4f / 3f) {
    if (post.foto.isNotBlank()) {
        val modelo: Any = if (post.foto.startsWith("http")) post.foto else File(post.foto)
        AsyncImage(modelo, null, modifier.aspectRatio(ratio), contentScale = ContentScale.Crop)
    } else {
        MontanaArte(post.id, modifier.aspectRatio(ratio), paleta = Eje.fromCodigo(post.eje)?.ordinal ?: 0) {
            if (conTexto) {
                Box(Modifier.fillMaxSize().background(Color(0x55000000)).padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        post.metaTitulo.ifBlank { post.texto }, color = Color.White, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 5, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta "bitácora de ascenso": franja del color del eje, encabezado del autor, postal 4:3
 * con margen y esquinas redondeadas (o una cita destacada si no hay foto), la meta como etiqueta
 * y una barra de acciones en píldoras.
 */
@Composable
fun PostCard(
    post: Post,
    onImpulsar: () -> Unit,
    onComentar: () -> Unit,
    onAbrir: () -> Unit,
    guardado: Boolean = false,
    onGuardar: (() -> Unit)? = null,
    onAutor: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val eje = Eje.fromCodigo(post.eje)
    val acento = eje?.color?.asColor() ?: MaterialTheme.colorScheme.secondary
    Card(
        onClick = onAbrir,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Franja superior del color del eje
        Box(Modifier.fillMaxWidth().height(5.dp).background(Brush.horizontalGradient(listOf(acento, acento.copy(alpha = 0.25f)))))
        Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar en forma de "galleta" (Material 3 Expressive) con un aro del color del eje
            val galleta = androidx.compose.runtime.remember { com.rutaalacima.app.ui.components.FormaPoligono(com.rutaalacima.app.ui.components.FormasRuta.galleta) }
            Box(Modifier.size(42.dp).background(acento, galleta), contentAlignment = Alignment.Center) {
                Avatar(post.autorNombre, post.avatarUrl, tamano = 36, forma = galleta)
            }
            Spacer(Modifier.width(10.dp))
            val clicAutor = if (onAutor != null && !post.propio) Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = stringResource(R.string.diario_ver_autor), onClick = onAutor) else Modifier
            Column(Modifier.weight(1f).then(clicAutor)) {
                Text(post.autorNombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(post.autorUsuario.takeIf { it.isNotBlank() }?.let { "@$it" }, haceCuanto(post.creadoEn)).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                )
            }
            if (post.visibilidad == Visibilidad.PRIVADA) Icon(Icons.Filled.Lock, stringResource(R.string.visibilidad_privada), Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            TipoBadge(post.tipo)
        }

        if (post.brujula.isNotBlank() && com.rutaalacima.app.ui.vision.brujulaValida(post.brujula)) {
            // Brújula de la Cima: la cumbre en papel; al tocarla se explora con el zoom 1 · 9 · 81
            com.rutaalacima.app.ui.vision.BrujulaEnMuro(post.brujula, post.autorNombre, Modifier.padding(horizontal = 12.dp))
        } else if (post.foto.isNotBlank()) {
            // Postal 4:3 con margen, la meta y el eje sobre un degradado
            Box(Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(18.dp))) {
                PostImagen(post, Modifier.fillMaxWidth())
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000))))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (post.metaTitulo.isNotBlank()) {
                        MetaEtiqueta(post.metaTitulo, Color.White, Modifier.weight(1f))
                    } else Spacer(Modifier.weight(1f))
                    eje?.let { EjeChip(it) }
                }
            }
            if (post.texto.isNotBlank()) {
                Text(post.texto, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp))
            }
        } else {
            // Sin foto: cita destacada, compacta
            Column(
                Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(acento.copy(alpha = 0.10f)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("“", style = MaterialTheme.typography.displaySmall, color = acento, fontWeight = FontWeight.Black,
                    modifier = Modifier.height(28.dp))
                Text(post.texto, style = MaterialTheme.typography.titleMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (post.metaTitulo.isNotBlank()) {
                        MetaEtiqueta(post.metaTitulo, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    } else Spacer(Modifier.weight(1f))
                    eje?.let { EjeChip(it) }
                }
            }
        }

        // Acciones en píldoras
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            com.rutaalacima.app.ui.components.BotonImpulso(
                activo = post.yoImpulse, cantidad = post.impulsos,
                icono = if (post.yoImpulse) Icons.Filled.Bolt else Icons.Outlined.Bolt,
                descripcion = stringResource(R.string.impulsar), onClick = onImpulsar,
            )
            Pildora(Icons.AutoMirrored.Outlined.Comment, "${post.comentarios}", stringResource(R.string.comentar), false, onComentar)
            Spacer(Modifier.weight(1f))
            if (onGuardar != null) IconButton(onClick = onGuardar) {
                Icon(if (guardado) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    stringResource(if (guardado) R.string.quitar_guardado else R.string.guardar_publicacion),
                    tint = if (guardado) MaterialTheme.colorScheme.secondary else LocalContentColor.current)
            }
            IconButton(onClick = {
                val texto = listOf(post.metaTitulo, post.texto, "— Rutaalacima").filter { it.isNotBlank() }.joinToString("\n\n")
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), null))
            }) { Icon(Icons.Outlined.Share, stringResource(R.string.compartir)) }
        }
    }
}

@Composable
private fun Pildora(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, descripcion: String, activa: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(50),
        color = if (activa) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, descripcion, Modifier.size(18.dp),
                tint = if (activa) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Ícono de cada tipo de publicación (sistema de íconos Material, sin emojis). */
fun iconoTipo(tipo: TipoPost): ImageVector = when (tipo) {
    TipoPost.LOGRO -> Icons.Filled.EmojiEvents
    TipoPost.EVIDENCIA -> Icons.Filled.PhotoCamera
    TipoPost.VISION -> Icons.Filled.AutoAwesome
    TipoPost.META -> Icons.Filled.TrackChanges
    TipoPost.REFLEXION -> Icons.Filled.Lightbulb
}

@Composable
fun TipoBadge(tipo: TipoPost) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(iconoTipo(tipo), null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(Modifier.width(4.dp))
        Text(tipo.texto(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/** La meta vinculada, con su ícono. */
@Composable
private fun MetaEtiqueta(texto: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.TrackChanges, null, Modifier.size(16.dp), tint = color)
        Spacer(Modifier.width(6.dp))
        Text(texto, color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
