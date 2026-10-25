package com.rutaalacima.app.ui.comunidad

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.rutaalacima.app.data.social.MediaPost
import java.io.File

/** ¿La publicación se muestra como carrusel o video (y no como una sola foto)? */
fun List<MediaPost>.esCarrusel() = size > 1 || any { it.esVideo }

/**
 * Carrusel de Cimas: fotos que se deslizan de lado con sus puntos, o un video que se reproduce
 * sin sonido mientras se ve (el botón activa el sonido).
 */
@Composable
fun MediaCarrusel(media: List<MediaPost>, activo: Boolean, modifier: Modifier = Modifier) {
    val pager = rememberPagerState { media.size }
    Box(modifier) {
        HorizontalPager(pager, Modifier.fillMaxSize(), key = { media[it].url }) { i ->
            val m = media[i]
            if (m.esVideo) VideoCima(m.url, activo && pager.currentPage == i, Modifier.fillMaxSize())
            else AsyncImage(if (m.url.startsWith("http")) m.url else File(m.url), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        if (media.size > 1) Row(Modifier.align(Alignment.TopCenter).padding(top = 104.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            media.indices.forEach { k ->
                Box(Modifier.size(if (k == pager.currentPage) 7.dp else 6.dp).clip(CircleShape)
                    .background(if (k == pager.currentPage) Color.White else Color.White.copy(alpha = 0.45f)))
            }
        }
    }
}

@Composable
private fun VideoCima(url: String, reproducir: Boolean, modifier: Modifier) {
    var sonido by remember { mutableStateOf(false) }
    var jugador by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(if (url.startsWith("http")) Uri.parse(url) else Uri.fromFile(File(url)))
                    setOnPreparedListener { mp -> mp.isLooping = true; mp.setVolume(0f, 0f); jugador = mp; if (reproducir) start() }
                }
            },
            update = { v -> if (reproducir && !v.isPlaying) v.start() else if (!reproducir && v.isPlaying) v.pause() },
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
        )
        IconButton(
            onClick = { sonido = !sonido; val vol = if (sonido) 1f else 0f; jugador?.setVolume(vol, vol) },
            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 100.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
        ) { Icon(if (sonido) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, null, tint = Color.White) }
    }
    DisposableEffect(url) { onDispose { jugador = null } }
}
