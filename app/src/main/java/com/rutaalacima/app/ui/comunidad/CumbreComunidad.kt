package com.rutaalacima.app.ui.comunidad

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rutaalacima.app.R
import com.rutaalacima.app.data.social.Post
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

/** Metros que suma cada publicación y cada impulso a la cumbre de la semana. */
object CumbreSemanal {
    const val META = 8848
    const val POR_PUBLICACION = 120
    const val POR_IMPULSO = 10

    data class Estado(val metros: Int, val publicaciones: Int, val impulsos: Int, val mios: Int)

    fun calcular(posts: List<Post>, ahora: Long = System.currentTimeMillis()): Estado {
        val semana = posts.filter { ahora - it.creadoEn <= 7L * 24 * 3600 * 1000 }
        val impulsos = semana.sumOf { it.impulsos }
        val mios = semana.count { it.propio } * POR_PUBLICACION + semana.count { it.yoImpulse } * POR_IMPULSO
        return Estado(semana.size * POR_PUBLICACION + impulsos * POR_IMPULSO, semana.size, impulsos, mios)
    }
}

// Cresta de la montaña (0..1): base a la izquierda, cumbre arriba a la derecha
private val CRESTA = listOf(
    0f to 0.92f, 0.12f to 0.78f, 0.22f to 0.82f, 0.34f to 0.58f, 0.44f to 0.64f,
    0.56f to 0.40f, 0.64f to 0.46f, 0.76f to 0.18f, 0.82f to 0.08f,
)

/**
 * "La cumbre de la comunidad": todos suben la misma montaña cada semana. Las publicaciones y los
 * impulsos son metros; la bandera avanza con un resorte y el sendero recorrido ondea como los
 * indicadores de progreso ondulados de Material 3 Expressive.
 */
@Composable
fun CumbreComunidadCard(posts: List<Post>, modifier: Modifier = Modifier) {
    val e = CumbreSemanal.calcular(posts)
    val objetivo = (e.metros.toFloat() / CumbreSemanal.META).coerceIn(0f, 1f)
    val avance by animateFloatAsState(objetivo, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessVeryLow), label = "avance")
    val fase by rememberInfiniteTransition(label = "ola").animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "fase")
    val nf = NumberFormat.getIntegerInstance()
    val esquema = MaterialTheme.colorScheme
    val descripcion = stringResource(R.string.cumbre_comunidad_metros, nf.format(e.metros), nf.format(CumbreSemanal.META))
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = esquema.primaryContainer, contentColor = esquema.onPrimaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.cumbre_comunidad_titulo), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                if (e.metros >= CumbreSemanal.META) stringResource(R.string.cumbre_comunidad_logro) else stringResource(R.string.cumbre_comunidad_texto),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(10.dp))
            val cielo = esquema.surface.copy(alpha = 0.55f)
            val roca = esquema.primary
            val nieve = Color.White.copy(alpha = 0.9f)
            val oro = esquema.secondary
            val sendero = esquema.onPrimaryContainer.copy(alpha = 0.35f)
            Canvas(Modifier.fillMaxWidth().aspectRatio(2.4f).semantics { contentDescription = descripcion }) {
                val w = size.width; val h = size.height
                val pts = CRESTA.map { (x, y) -> Offset(x * w, y * h) }
                // Cielo y montaña
                drawRoundRect(cielo, cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()))
                val montana = Path().apply {
                    moveTo(0f, h); pts.forEach { lineTo(it.x, it.y) }; lineTo(w, h * 0.5f); lineTo(w, h); close()
                }
                drawPath(montana, Brush.verticalGradient(listOf(roca, roca.copy(alpha = 0.75f))))
                val cima = pts.last()
                drawPath(Path().apply {
                    moveTo(cima.x, cima.y); lineTo(cima.x - w * 0.05f, cima.y + h * 0.13f); lineTo(cima.x + w * 0.04f, cima.y + h * 0.1f); lineTo(cima.x + w * 0.08f, cima.y + h * 0.16f); close()
                }, nieve)
                // Sendero completo (punteado) y lo recorrido (onda dorada)
                val largos = pts.zipWithNext { a, b -> hypot(b.x - a.x, b.y - a.y) }
                val total = largos.sum()
                fun punto(d: Float): Offset {
                    var r = d
                    for (i in largos.indices) {
                        if (r <= largos[i]) { val t = r / largos[i]; return Offset(pts[i].x + (pts[i + 1].x - pts[i].x) * t, pts[i].y + (pts[i + 1].y - pts[i].y) * t) }
                        r -= largos[i]
                    }
                    return pts.last()
                }
                val paso = 3.dp.toPx()
                var d = 0f
                while (d < total) { drawCircle(sendero, 1.4.dp.toPx(), punto(d)); d += paso * 3 }
                val hasta = total * avance
                if (hasta > 1f) {
                    val onda = Path()
                    val amp = 2.5.dp.toPx()
                    d = 0f
                    while (d <= hasta) {
                        val p = punto(d); val q = punto((d + 1f).coerceAtMost(total))
                        val nx = -(q.y - p.y); val ny = q.x - p.x; val n = hypot(nx, ny).coerceAtLeast(0.001f)
                        val o = sin((d / (14.dp.toPx()) - fase) * 2f * PI.toFloat()) * amp
                        val x = p.x + nx / n * o; val y = p.y + ny / n * o
                        if (d == 0f) onda.moveTo(x, y) else onda.lineTo(x, y)
                        d += 2f
                    }
                    drawPath(onda, oro, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                }
                // Bandera en el punto alcanzado
                val b = punto(hasta)
                drawLine(nieve, b, Offset(b.x, b.y - h * 0.22f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                val ondea = sin(fase * 2f * PI.toFloat()) * h * 0.012f
                drawPath(Path().apply {
                    moveTo(b.x, b.y - h * 0.22f); lineTo(b.x + w * 0.07f, b.y - h * 0.18f + ondea); lineTo(b.x, b.y - h * 0.13f); close()
                }, oro)
                drawCircle(oro, 5.dp.toPx(), b)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(nf.format(e.metros), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text(" / " + nf.format(CumbreSemanal.META) + " m", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(stringResource(R.string.cumbre_comunidad_datos, e.publicaciones, e.impulsos), style = MaterialTheme.typography.labelLarge)
            if (e.mios > 0) Text(stringResource(R.string.cumbre_comunidad_aporte, nf.format(e.mios)), style = MaterialTheme.typography.labelLarge,
                color = esquema.secondary, fontWeight = FontWeight.Bold)
        }
    }
}
