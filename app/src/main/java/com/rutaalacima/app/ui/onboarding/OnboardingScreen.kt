package com.rutaalacima.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Fase
import com.rutaalacima.app.ui.components.ChipSelector
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.workbook.QuoteView
import kotlinx.coroutines.launch

class OnboardingViewModel(private val c: AppContainer) : ViewModel() {
    fun completar(nombre: String, cumbre: String, fase: Fase?) = viewModelScope.launch {
        c.perfil.actualizar {
            it.copy(
                nombre = nombre.trim(),
                cumbreFrase = cumbre.trim(),
                faseActual = fase?.name,
                compromisoFirmadoEn = System.currentTimeMillis(),
                onboardingCompleto = true,
            )
        }
    }
}

private val COMPROMISOS = listOf(
    "Ser brutalmente honesto conmigo mismo",
    "Tomar acción consistente, no solo soñar",
    "Levantarme cada vez que caiga",
    "Celebrar cada pequeño avance",
    "Revisar mi ruta cada 30 días",
    "No rendirme cuando el camino se ponga difícil",
)

/** Bienvenida → nombre y cumbre → fase actual → compromiso. */
@Composable
fun OnboardingScreen() {
    val vm = rutaViewModel { OnboardingViewModel(it) }
    var paso by rememberSaveable { mutableIntStateOf(0) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var cumbre by rememberSaveable { mutableStateOf("") }
    var faseNombre by rememberSaveable { mutableStateOf<String?>(null) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (paso) {
                0 -> {
                    Spacer(Modifier.height(24.dp))
                    Image(painterResource(R.drawable.sello_ruta), "Sello Ruta a la Cima",
                        modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally))
                    Text("Bienvenido, senderista.", style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    QuoteView("Hay un momento en la vida en que sientes un llamado silencioso")
                    Text(
                        "Tu Cumbre Personal no es un punto geográfico ni una simple meta de vida. Es un estado de conciencia " +
                            "donde todas tus dimensiones interiores convergen: tu voluntad, tu maestría, tu voz, tu capacidad " +
                            "de crear valor, tu evolución y tu conexión con lo trascendente.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = { paso = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Comenzar el ascenso") }
                }
                1 -> {
                    Text("¿Quién emprende este viaje?", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(nombre, { nombre = it }, label = { Text("Tu nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Tu Cumbre Personal en una frase (puedes escribirla después):", style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(cumbre, { cumbre = it }, label = { Text("Mi Cumbre Personal es…") }, minLines = 3,
                        modifier = Modifier.fillMaxWidth())
                    Row {
                        TextButton(onClick = { paso = 0 }) { Text("Atrás") }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { paso = 2 }, enabled = nombre.isNotBlank()) { Text("Continuar") }
                    }
                }
                2 -> {
                    Text("¿En qué fase estás HOY?", style = MaterialTheme.typography.headlineSmall)
                    Text("Lee cada fase y marca la que mejor describe tu situación actual.", style = MaterialTheme.typography.bodyLarge)
                    val fase = Fase.fromName(faseNombre)
                    ChipSelector(null, Fase.entries, fase, { "${it.numero}. ${it.nombre}" }, { faseNombre = it.name })
                    fase?.let { Text(it.autodiagnostico, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary) }
                    Row {
                        TextButton(onClick = { paso = 1 }) { Text("Atrás") }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { paso = 3 }) { Text(if (fase == null) "Omitir" else "Continuar") }
                    }
                }
                else -> {
                    Text("Tu compromiso con la cumbre", style = MaterialTheme.typography.headlineSmall)
                    QuoteView("El ascenso comienza hoy")
                    Text("Yo, ${nombre.trim()}, me comprometo hoy a emprender el ascenso hacia mi Cumbre Personal. Me comprometo a:",
                        style = MaterialTheme.typography.bodyLarge)
                    COMPROMISOS.forEach { Text("✓  $it", style = MaterialTheme.typography.bodyLarge) }
                    Button(
                        onClick = { vm.completar(nombre, cumbre, Fase.fromName(faseNombre)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Me comprometo") }
                    TextButton(onClick = { paso = 2 }) { Text("Atrás") }
                }
            }
        }
    }
}
