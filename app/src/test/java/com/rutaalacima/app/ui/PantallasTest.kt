package com.rutaalacima.app.ui

import android.app.Application
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rutaalacima.app.ui.components.Celebracion
import com.rutaalacima.app.ui.components.CelebracionCumbre
import com.rutaalacima.app.ui.components.EstadoVacio
import com.rutaalacima.app.ui.components.RadarEjes
import com.rutaalacima.app.ui.components.SelloDeCera
import com.rutaalacima.app.ui.hoy.MapaConstancia
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Pantallas y componentes dibujados a mano: se muestran, responden y se leen con TalkBack. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "es")
class PantallasTest {
    @get:Rule val compose = createComposeRule()

    @Test fun estadoVacioMuestraTextoYSuBoton() {
        var tocado = false
        compose.setContent { MaterialTheme { EstadoVacio("prueba", "Planta la primera bandera", "Aún no hay nada", "Publicar", { tocado = true }) } }
        compose.onNodeWithText("Planta la primera bandera").assertIsDisplayed()
        compose.onNodeWithText("Publicar").performClick()
        assertTrue(tocado)
    }

    @Test fun elSelloSeDibujaEnTodaLaRotura() {
        var progreso by mutableFloatStateOf(0f)
        compose.setContent { SelloDeCera(progreso, semilla = 2026_277L, modifier = Modifier.size(120.dp)) }
        listOf(0.05f, 0.15f, 0.3f, 0.6f, 0.95f, 1f).forEach { progreso = it; compose.waitForIdle() }
    }

    @Test fun laCelebracionMuestraLaMetaYSeCierraAlTocar() {
        var cerrada = false
        compose.mainClock.autoAdvance = false
        compose.setContent { MaterialTheme { CelebracionCumbre(Celebracion("Correr 3 veces por semana", anual = false)) { cerrada = true } } }
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("Correr 3 veces por semana").assertExists()
        compose.onRoot().performClick()
        assertTrue(cerrada)
    }

    @Test fun elMapaYElRadarSeLeenConTalkBack() {
        compose.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    MapaConstancia(List(10) { List(7) { 3 } }, Modifier.size(200.dp, 140.dp))
                    RadarEjes(listOf(8, 6, 5, 7, 9, 4), Modifier.size(200.dp))
                }
            }
        }
        compose.onNodeWithContentDescription("Mapa de los días con hábitos").assertExists()
        compose.onNodeWithContentDescription("Voluntad 8/10", substring = true).assertExists()
    }
}
