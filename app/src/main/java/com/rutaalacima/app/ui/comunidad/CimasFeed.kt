package com.rutaalacima.app.ui.comunidad

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.Post
import com.rutaalacima.app.domain.model.Eje
import com.rutaalacima.app.ui.components.EjeChip
import com.rutaalacima.app.ui.components.MontanaArte
import com.rutaalacima.app.ui.theme.Papel
import com.rutaalacima.app.ui.theme.asColor
import com.rutaalacima.app.util.haceCuanto
import kotlinx.coroutines.delay
import java.io.File

/**
 * Cimas: la comunidad a pantalla completa, una publicación por página. Se desliza hacia arriba
 * para ver la siguiente (como los videos cortos), doble toque para impulsar y los botones de
 * impulsar, comentar y compartir al costado derecho.
 */
@Composable
fun CimasFeed(
    posts: List<Post>,
    estado: PagerState,
    onImpulsar: (Post) -> Unit,
    onAbrir: (Post) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(0.dp),
) {
    VerticalPager(
        state = estado,
        modifier = modifier,
        contentPadding = padding,
        pageSpacing = 12.dp,
        key = { posts[it].id },
    ) { i ->
        CimaPagina(
            post = posts[i],
            primera = i == 0,
            onImpulsar = { onImpulsar(posts[i]) },
            onAbrir = { onAbrir(posts[i]) },
        )
    }
}

@Composable
private fun CimaPagina(post: Post, primera: Boolean, onImpulsar: () -> Unit, onAbrir: () -> Unit) {
    val context = LocalContext.current
    val eje = Eje.fromCodigo(post.eje)
    val acento = eje?.color?.asColor() ?: MaterialTheme.colorScheme.primary
    var destello by remember { mutableStateOf(false) }
    LaunchedEffect(destello) { if (destello) { delay(700); destello = false } }

    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Papel.Sombra, spotColor = Papel.Sombra)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1D120D))
            .pointerInput(post.id) {
                detectTapGestures(
                    onDoubleTap = { if (!post.yoImpulse) onImpulsar(); destello = true },
                    onTap = { onAbrir() },
                )
            },
    ) {
        // Fondo: la foto a pantalla completa, o el paisaje del eje con la frase grande
        if (post.foto.isNotBlank()) {
            val modelo: Any = if (post.foto.startsWith("http")) post.foto else File(post.foto)
            AsyncImage(modelo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            MontanaArte(post.id, Modifier.fillMaxSize(), paleta = eje?.ordinal ?: 0) {
                Box(Modifier.fillMaxSize().background(Color(0x66000000)).padding(horizontal = 36.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "“${post.texto}”", color = Color.White, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        maxLines = 9, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // Degradados para leer el texto
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x66000000), 0.18f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color(0xD9000000))))

        // Arriba: tipo de publicación
        Row(Modifier.align(Alignment.TopStart).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TipoBadge(post.tipo)
        }

        // Impulso grande al dar doble toque
        AnimatedVisibility(
            visible = destello, modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(tween(180)) + fadeIn(), exit = scaleOut(tween(300)) + fadeOut(),
        ) {
            Icon(Icons.Filled.Bolt, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(120.dp))
        }

        // Derecha: acciones
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Avatar(post.autorNombre, post.avatarUrl, tamano = 44)
            Accion(if (post.yoImpulse) Icons.Filled.Bolt else Icons.Outlined.Bolt, "${post.impulsos}",
                stringResource(R.string.impulsar), if (post.yoImpulse) MaterialTheme.colorScheme.secondary else Color.White, onImpulsar)
            Accion(Icons.AutoMirrored.Outlined.Comment, "${post.comentarios}", stringResource(R.string.comentar), Color.White, onAbrir)
            Accion(Icons.Outlined.Share, "", stringResource(R.string.compartir), Color.White) {
                val texto = listOf(post.metaTitulo, post.texto, "— Rutaalacima").filter { it.isNotBlank() }.joinToString("\n\n")
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), null))
            }
        }

        // Abajo: autor, texto, meta y eje
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 16.dp, end = 80.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                listOfNotNull(post.autorNombre, post.autorUsuario.takeIf { it.isNotBlank() }?.let { "@$it" }, haceCuanto(post.creadoEn)).joinToString(" · "),
                color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (post.foto.isNotBlank() && post.texto.isNotBlank()) {
                Text(post.texto, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (post.metaTitulo.isNotBlank()) {
                    Text(
                        "⛰ ${post.metaTitulo}", color = Color.White, style = MaterialTheme.typography.labelMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(50)).background(acento.copy(alpha = 0.75f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                eje?.let { EjeChip(it) }
            }
            if (primera) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.cimas_ayuda), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun Accion(icono: ImageVector, texto: String, descripcion: String, tinte: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(4.dp)
            .semantics { contentDescription = descripcion },
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
            Icon(icono, null, tint = tinte, modifier = Modifier.size(26.dp))
        }
        if (texto.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(texto, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}
