package com.rutaalacima.app.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.data.local.AgendaDiaEntity
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.PropositoEntity
import com.rutaalacima.app.domain.model.Finanzas
import com.rutaalacima.app.ui.cordadas.SelectorMetaCordada
import com.rutaalacima.app.ui.planner.AgendaViewModel
import com.rutaalacima.app.ui.planner.DayPlannerTab
import com.rutaalacima.app.ui.planner.DisponibleHoy
import com.rutaalacima.app.ui.planner.FinanzasMesContenido
import com.rutaalacima.app.ui.planner.WB_DIA
import com.rutaalacima.app.ui.ruta.CintaCumbre
import com.rutaalacima.app.ui.ruta.CumbresAnio
import com.rutaalacima.app.ui.ruta.SinPasoMes
import com.rutaalacima.app.ui.theme.RutaTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/**
 * Capturas de las pantallas nuevas para revisarlas sin teléfono. Solo corren con CAPTURAS=1
 * (paso aparte en GitHub, que sube las imágenes de app/build/capturas como artefacto).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "es-w400dp-h1500dp-xhdpi")
class CapturasTest {
    @get:Rule val compose = createComposeRule()

    @Before fun soloSiSePiden() = assumeTrue(System.getenv("CAPTURAS") == "1")

    private val hoy = LocalDate.now()
    private val ym = YearMonth.from(hoy)

    private fun guardar(nombre: String) {
        compose.waitForIdle()
        val dir = File("build/capturas").apply { mkdirs() }
        File(dir, "$nombre.png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun pantalla(oscuro: Boolean, contenido: @Composable () -> Unit) = compose.setContent {
        RutaTheme(darkTheme = oscuro) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                contenido()
            }
        }
    }

    private val propositos = listOf(
        PropositoEntity(id = 1, titulo = "Escuela de montaña propia", eje = "MAE", horizonte = 10,
            creadoEn = LocalDate.of(2024, 3, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()),
    )
    private val anuales = listOf(
        MetaAnualEntity(id = 10, anio = hoy.year, propositoId = 1, titulo = "Certificarme como guía de montaña", avance = 45),
        MetaAnualEntity(id = 11, anio = hoy.year, propositoId = 1, titulo = "Ahorrar el capital semilla", avance = 70),
        MetaAnualEntity(id = 12, anio = hoy.year + 2, propositoId = 1, titulo = "Primer grupo de 12 estudiantes"),
    )
    private val mensuales = listOf(
        MetaMensualEntity(id = 100, anio = hoy.year, mes = hoy.monthValue, texto = "Estudiar 1 módulo del curso", metaAnualId = 10, dias = "1,2,3,5"),
    )

    @Test fun cascadaDelAnioYDelMes() {
        for (oscuro in listOf(false, true)) {
            pantalla(oscuro) {
                CumbresAnio(hoy.year, propositos, anuales, {}, {})
                CintaCumbre(propositos[0], anuales) {}
                SinPasoMes(ym, anuales, mensuales) {}
                SelectorMetaCordada("100", mensuales) {}
            }
            guardar(if (oscuro) "cascada-oscuro" else "cascada-claro")
        }
    }

    @Test fun finanzasDelMes() {
        val dias = mapOf(
            1 to Finanzas.dia("2.400.000", "85.000", "300.000", ""),
            3 to Finanzas.dia("", "120.000", "", "100.000"),
            8 to Finanzas.dia("", "64.500", "", ""),
            12 to Finanzas.dia("150.000", "210.000", "", ""),
            15 to Finanzas.dia("", "45.000", "50.000", ""),
        )
        for (oscuro in listOf(false, true)) {
            pantalla(oscuro) {
                FinanzasMesContenido(ym, Finanzas.mes(dias, "1.500.000"), "1.500.000", {})
                DisponibleHoy("150.000", "64.500", "20.000", "")
            }
            guardar(if (oscuro) "finanzas-oscuro" else "finanzas-claro")
        }
    }

    @Test fun planDelDia() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val c = AppContainer(app)
        runBlocking {
            c.planAnual.guardarMetaMes(mensuales[0])
            c.agenda.guardar(AgendaDiaEntity(fecha = hoy.toString(), intencion = "Subir con calma y constancia",
                horario = """{"6":"Correr 5 km","8":"Módulo 4 del curso","14":"Llamar a la escuela"}"""))
            c.respuestas.guardar(WB_DIA, "dia#$hoy#top",
                """[{"t":"Terminar el módulo 4","meta":"100","hecho":true},{"t":"Llamar a mamá","meta":"","hecho":false}]""")
            c.respuestas.guardar(WB_DIA, "dia#$hoy#8#meta", "100")
            c.respuestas.guardar(WB_DIA, "dia#$hoy#8#hecho", "1")
        }
        val vm = AgendaViewModel(c)
        compose.setContent { RutaTheme(darkTheme = false) { Column(Modifier.background(MaterialTheme.colorScheme.background)) { DayPlannerTab(vm, 0.dp) } } }
        compose.waitUntil(10_000) { vm.dia != null && vm.resp.value.isNotEmpty() && vm.metasMes.value.isNotEmpty() }
        guardar("plan-dia")
    }
}
