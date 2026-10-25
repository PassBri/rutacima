package com.rutaalacima.app.ui.comunidad

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.unit.sp
import com.rutaalacima.app.ui.i18n.texto
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
 * Cimas: la comunidad a pantalla completa, como los videos cortos, pero en el mundo de Ruta a la Cima.
 * Una publicación por pantalla, de borde a borde; se desliza hacia arriba para la siguiente. Doble toque
 * para impulsar (cae un sello de lacre). Acciones al costado derecho y el pie con autor, texto y meta.
 * Las publicaciones sin foto se ven sobre el cielo real del momento en que se publicaron.
 */
@Composable
fun CimasFeed(
    posts: List<Post>,
    estado: PagerState,
    onImpulsar: (Post) -> Unit,
    onAbrir: (Post) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(0.dp),
    guardados: Set<String> = emptySet(),
    onGuardar: (Post) -> Unit = {},
    onAutor: (Post) -> Unit = {},
    onMas: (Post) -> Unit = {},
    /** Al final del feed: una página que cierra ("Ya viste lo nuevo") y lleva a dar el paso de hoy. */
    onFin: (() -> Unit)? = null,
) {
    VerticalPager(
        state = estado,
        modifier = modifier,
        contentPadding = padding,
        beyondViewportPageCount = 1,
        key = { if (it < posts.size) posts[it].id else "fin" },
    ) { i ->
        if (i >= posts.size) { FinDelFeed(onFin ?: {}); return@VerticalPager }
        CimaPagina(
            post = posts[i],
            primera = i == 0,
            activo = estado.currentPage == i,
            guardado = posts[i].id in guardados,
            onImpulsar = { onImpulsar(posts[i]) },
            onAbrir = { onAbrir(posts[i]) },
            onGuardar = { onGuardar(posts[i]) },
            onAutor = { onAutor(posts[i]) },
            onMas = { onMas(posts[i]) },
        )
    }
}

/** El final del feed: la comunidad tiene fin; la invitación es volver a la propia ruta. */
@Composable
private fun FinDelFeed(onHoy: () -> Unit) {
    com.rutaalacima.app.ui.components.MontanaArte("fin-del-feed", Modifier.fillMaxSize(), paleta = 3) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x66060A12), 1f to Color(0xD9060A12))))
        Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.logo_suroute), null, Modifier.size(84.dp), tint = Color.White)
            Text(stringResource(R.string.fin_feed_titulo), color = Color.White, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(stringResource(R.string.fin_feed_texto), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            androidx.compose.material3.Button(onClick = onHoy, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.inicio_accion)) }
        }
    }
}

@Composable
private fun CimaPagina(
    post: Post, primera: Boolean, guardado: Boolean, activo: Boolean = true,
    onImpulsar: () -> Unit, onAbrir: () -> Unit, onGuardar: () -> Unit, onAutor: () -> Unit, onMas: () -> Unit,
) {
    val context = LocalContext.current
    val eje = Eje.fromCodigo(post.eje)
    var sello by remember { mutableStateOf(false) }
    var abierto by remember { mutableStateOf(false) }
    LaunchedEffect(sello) { if (sello) { delay(900); sello = false } }
    val haptico = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF060A12))
            .pointerInput(post.id) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!post.yoImpulse) onImpulsar()
                        sello = true
                        haptico.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    },
                    onTap = { abierto = false },
                )
            },
    ) {
        // Fondo: la foto de borde a borde, o el cielo real del momento en que se publicó con la frase en letra de carta
        if (post.media.esCarrusel()) {
            MediaCarrusel(post.media, activo, Modifier.fillMaxSize())
        } else if (post.foto.isNotBlank()) {
            val modelo: Any = if (post.foto.startsWith("http")) post.foto else File(post.foto)
            AsyncImage(modelo, androidx.compose.ui.res.stringResource(R.string.foto_de, post.autorNombre) +
                post.metaTitulo.ifBlank { post.texto }.take(120).let { if (it.isBlank()) "" else ": $it" },
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            com.rutaalacima.app.ui.components.CieloVivo(Modifier.fillMaxSize(), horizonte = 0.72f, lectura = false, instante = post.creadoEn) {
                Box(Modifier.fillMaxSize().padding(start = 32.dp, end = 76.dp, top = 120.dp, bottom = 200.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "“${post.texto}”", color = Color.White, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Normal, lineHeight = 34.sp),
                        maxLines = 10, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // Velos para leer: arriba (pestañas) y abajo (pie)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x8C060A12), 0.16f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color(0xE0060A12))))

        // Doble toque: cae un sello de lacre (el impulso de Ruta a la Cima)
        AnimatedVisibility(
            visible = sello, modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(tween(220), initialScale = 1.6f) + fadeIn(tween(160)), exit = scaleOut(tween(420), targetScale = 0.9f) + fadeOut(tween(420)),
        ) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.logo_sello), null, Modifier.size(132.dp))
        }

        // Derecha: autor y acciones
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.clip(CircleShape).clickable(onClick = onAutor).padding(2.dp).semantics { contentDescription = post.autorNombre }) {
                Box(Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)).padding(2.dp)) { Avatar(post.autorNombre, post.avatarUrl, tamano = 46) }
            }
            Accion(if (post.yoImpulse) com.rutaalacima.app.ui.components.IconosRuta.Banderin else com.rutaalacima.app.ui.components.IconosRuta.BanderinContorno, contar(post.impulsos),
                stringResource(R.string.impulsar), if (post.yoImpulse) MaterialTheme.colorScheme.secondary else Color.White, onImpulsar)
            Accion(Icons.AutoMirrored.Outlined.Comment, contar(post.comentarios), stringResource(R.string.comentar), Color.White, onAbrir)
            Accion(if (guardado) com.rutaalacima.app.ui.components.IconosRuta.Morral else com.rutaalacima.app.ui.components.IconosRuta.MorralContorno, "", stringResource(R.string.guardar),
                if (guardado) MaterialTheme.colorScheme.secondary else Color.White, onGuardar)
            Accion(Icons.Outlined.Share, "", stringResource(R.string.compartir), Color.White) {
                val enlace = com.rutaalacima.app.data.social.EnlacesRed.publicacion(post)
                val texto = listOf(post.metaTitulo, post.texto, enlace.ifBlank { "— SuRoute" }).filter { it.isNotBlank() }.joinToString("\n\n")
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), null))
            }
            // Más: reportar, bloquear y copiar, siempre a un toque
            Accion(Icons.Filled.MoreHoriz, "", stringResource(R.string.mas_opciones), Color.White, onMas)
        }

        // Abajo: autor, texto (se abre con "más"), meta y eje
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 16.dp, end = 84.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    post.autorUsuario.takeIf { it.isNotBlank() }?.let { "@$it" } ?: post.autorNombre,
                    color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).clickable(onClick = onAutor),
                )
                InsigniaCoach(post.autorId)
            }
            Text(
                listOfNotNull(post.tipo.texto(), eje?.texto(), haceCuanto(post.creadoEn)).joinToString(" · "),
                color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium, maxLines = 1,
            )
            if (post.foto.isNotBlank() && post.texto.isNotBlank()) {
                TextoEnlazado(
                    post.texto, color = Color.White, style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (abierto) 12 else 2, enlace = Color.White,
                )
                if (post.texto.length > 90) Text(
                    stringResource(if (abierto) R.string.cimas_menos else R.string.cimas_mas), color = Color.White, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { abierto = !abierto }.padding(vertical = 4.dp),
                )
            } else if (post.foto.isBlank()) {
                Text(stringResource(R.string.cimas_publicado_bajo), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
            }
            if (post.metaTitulo.isNotBlank()) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Terrain, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(post.metaTitulo, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (primera) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.cimas_ayuda), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** 1.234 → "1,2 mil" como en las redes; los números pequeños se muestran tal cual. */
private fun contar(n: Int): String = when {
    n >= 1_000_000 -> String.format(java.util.Locale.getDefault(), "%.1fM", n / 1_000_000f)
    n >= 10_000 -> "${n / 1000}k"
    n >= 1_000 -> String.format(java.util.Locale.getDefault(), "%.1fk", n / 1000f)
    else -> "$n"
}

@Composable
private fun Accion(icono: ImageVector, texto: String, descripcion: String, tinte: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(2.dp)
            .semantics { contentDescription = if (texto.isEmpty()) descripcion else "$descripcion, $texto" },
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Color(0x40060A12)).border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icono, null, tint = tinte, modifier = Modifier.size(26.dp)) }
        if (texto.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(texto, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}
