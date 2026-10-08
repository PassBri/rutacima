package com.rutaalacima.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.rutaalacima.app.MainActivity
import com.rutaalacima.app.R
import com.rutaalacima.app.RutaApp
import com.rutaalacima.app.domain.model.ChecklistDiario
import com.rutaalacima.app.domain.model.Constancia
import com.rutaalacima.app.domain.model.FrasesDelDia
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Widget de la pantalla de inicio: la frase del día (sellada hasta que la abras en la app),
 * los hábitos marcados hoy y la racha. Tocarlo abre la app.
 */
class WidgetRuta : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = (context.applicationContext as RutaApp).container
        val hoy = LocalDate.now()
        val indice = FrasesDelDia.indice(hoy)
        val abierta = indice in c.frases.desbloqueadas.value
        val frase = if (abierta) runCatching { c.frases.delDia(hoy) }.getOrNull() else null
        val habitos = c.checklist.dia(hoy).first().size
        val dias = c.checklist.desde(hoy.minusDays(400)).first().filterValues { it.isNotEmpty() }.keys
        val racha = com.rutaalacima.app.domain.model.Comodines.calcular(dias, hoy).racha

        val titulo = context.getString(R.string.frase_dia_n, indice + 1)
        val cuerpo = frase?.let { "“${it.t}”" } ?: context.getString(R.string.widget_sellada)
        val autor = frase?.libro.orEmpty()
        val pie = context.getString(R.string.widget_habitos, habitos, ChecklistDiario.TOTAL) +
            if (racha > 0) " · " + context.getString(R.string.racha_dias, racha) else ""

        provideContent { Contenido(titulo, cuerpo, autor, pie, sellada = frase == null) }
    }

    companion object {
        /** Se llama cuando cambian los hábitos o se abre la frase. */
        suspend fun actualizar(context: Context) = runCatching { WidgetRuta().updateAll(context) }
    }
}

private val Papel = Color(0xFFFBF6EE)
private val Tinta = Color(0xFF2B1D16)
private val Suave = Color(0xFF6B5B52)
private val Oro = Color(0xFFB8862F)
private val Burdeos = Color(0xFF6B2A1A)

@Composable
private fun Contenido(titulo: String, cuerpo: String, autor: String, pie: String, sellada: Boolean) {
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(22.dp).background(ColorProvider(Papel)).padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.logo_sello), contentDescription = null, modifier = GlanceModifier.size(26.dp))
            Spacer(GlanceModifier.width(8.dp))
            Text(titulo.uppercase(), style = TextStyle(color = ColorProvider(Oro), fontWeight = FontWeight.Bold, fontSize = 11.sp))
        }
        Spacer(GlanceModifier.height(8.dp))
        Text(
            cuerpo, maxLines = 4,
            style = TextStyle(
                color = ColorProvider(if (sellada) Burdeos else Tinta), fontSize = if (sellada) 14.sp else 15.sp,
                fontStyle = if (sellada) FontStyle.Normal else FontStyle.Italic,
            ),
        )
        if (autor.isNotBlank()) Text("— $autor", maxLines = 1, style = TextStyle(color = ColorProvider(Suave), fontSize = 11.sp))
        Spacer(GlanceModifier.defaultWeight())
        Text(pie, maxLines = 1, style = TextStyle(color = ColorProvider(Suave), fontSize = 12.sp, fontWeight = FontWeight.Medium))
    }
}

class WidgetRutaReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = WidgetRuta()
}
