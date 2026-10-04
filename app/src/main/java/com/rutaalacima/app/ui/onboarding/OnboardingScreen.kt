package com.rutaalacima.app.ui.onboarding

import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Fase
import com.rutaalacima.app.ui.components.ChipSelector
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.i18n.Textos
import com.rutaalacima.app.ui.i18n.texto
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
    R.string.compromiso_1, R.string.compromiso_2, R.string.compromiso_3,
    R.string.compromiso_4, R.string.compromiso_5, R.string.compromiso_6,
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
                    Image(painterResource(R.drawable.logo_rutacima), stringResource(R.string.app_name),
                        modifier = Modifier.size(140.dp).align(Alignment.CenterHorizontally))
                    Text(stringResource(R.string.onb_bienvenida), style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    QuoteView(stringResource(R.string.onb_cita))
                    Text(
                        stringResource(R.string.onb_texto),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = { paso = 1 }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onb_comenzar)) }
                }
                1 -> {
                    Text(stringResource(R.string.onb_quien), style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(nombre, { nombre = it }, label = { Text(stringResource(R.string.tu_nombre)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.onb_cumbre_frase), style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(cumbre, { cumbre = it }, label = { Text(stringResource(R.string.mi_cumbre_es)) }, minLines = 3,
                        modifier = Modifier.fillMaxWidth())
                    Row {
                        TextButton(onClick = { paso = 0 }) { Text(stringResource(R.string.atras)) }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { paso = 2 }, enabled = nombre.isNotBlank()) { Text(stringResource(R.string.continuar)) }
                    }
                }
                2 -> {
                    Text(stringResource(R.string.onb_fase), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.onb_fase_texto), style = MaterialTheme.typography.bodyLarge)
                    val fase = Fase.fromName(faseNombre)
                    ChipSelector(null, Fase.entries, fase, { "${it.numero}. " + it.texto() }, { faseNombre = it.name })
                    fase?.let { Text(stringResource(Textos.diagnostico(it)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary) }
                    Row {
                        TextButton(onClick = { paso = 1 }) { Text(stringResource(R.string.atras)) }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { paso = 3 }) { Text(stringResource(if (fase == null) R.string.omitir else R.string.continuar)) }
                    }
                }
                else -> {
                    Text(stringResource(R.string.onb_compromiso), style = MaterialTheme.typography.headlineSmall)
                    QuoteView(stringResource(R.string.onb_compromiso_cita))
                    Text(stringResource(R.string.onb_compromiso_texto, nombre.trim()),
                        style = MaterialTheme.typography.bodyLarge)
                    COMPROMISOS.forEach {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.size(10.dp))
                            Text(stringResource(it), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Button(
                        onClick = { vm.completar(nombre, cumbre, Fase.fromName(faseNombre)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.me_comprometo)) }
                    TextButton(onClick = { paso = 2 }) { Text(stringResource(R.string.atras)) }
                }
            }
        }
    }
}
