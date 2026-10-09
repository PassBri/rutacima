package com.rutaalacima.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.BuildConfig
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Planes
import com.rutaalacima.app.domain.model.Planes.Beneficio
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.fondoPapel
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class PlanViewModel(private val c: AppContainer) : ViewModel() {
    var estado by mutableStateOf(Planes.GRATIS); private set
    var cargando by mutableStateOf(true); private set
    var error by mutableStateOf(false); private set
    val hayCuenta get() = c.plan.hayCuenta

    init { cargar() }

    fun cargar() = viewModelScope.launch { cargando = true; estado = c.plan.miPlan(); cargando = false }

    fun avisame() = viewModelScope.launch {
        cargando = true
        runCatching { c.plan.avisame() }.onSuccess { estado = estado.copy(avisame = true); error = false }.onFailure { error = true }
        cargando = false
    }
}

@Composable
private fun nombre(b: Beneficio) = stringResource(
    when (b) {
        Beneficio.METODO -> R.string.plan_b_metodo
        Beneficio.BRUJULA -> R.string.plan_b_brujula
        Beneficio.COMUNIDAD -> R.string.plan_b_comunidad
        Beneficio.WEB -> R.string.plan_b_web
        Beneficio.DATOS -> R.string.plan_b_datos
        Beneficio.COACH -> R.string.plan_b_coach
        Beneficio.AUDIOLIBROS -> R.string.plan_b_audio
        Beneficio.CIERRE_GUIADO -> R.string.plan_b_cierre
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(onBack: () -> Unit, onCuenta: () -> Unit) {
    val vm = rutaViewModel { PlanViewModel(it) }
    val e = vm.estado
    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.plan_titulo)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Encabezado
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Terrain, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.plan_titulo), fontFamily = FontFamily.Serif, style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.plan_sub), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
            // Tu plan
            item {
                RutaCard {
                    if (e.esCumbre) {
                        val hasta = e.hasta?.atZone(ZoneId.systemDefault())?.toLocalDate()?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)).orEmpty()
                        Text(stringResource(R.string.plan_tu_cumbre, hasta), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        val origen = when (e.origen) {
                            Planes.Origen.INSTITUCION -> R.string.plan_origen_institucion
                            Planes.Origen.PILOTO -> R.string.plan_origen_piloto
                            Planes.Origen.REGALO -> R.string.plan_origen_regalo
                            else -> null
                        }
                        origen?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        Text(stringResource(R.string.plan_tu_gratis), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.plan_hoy_abierto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Qué incluye cada plan
            item {
                RutaCard {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        Spacer(Modifier.weight(1f))
                        Text(stringResource(R.string.plan_gratis), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
                        Text(stringResource(R.string.plan_cumbre), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
                    }
                    Beneficio.entries.forEachIndexed { i, b ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        FilaBeneficio(b)
                    }
                }
            }
            // Precio y "Avísame"
            item {
                RutaCard {
                    Text(stringResource(R.string.plan_precio, BuildConfig.PLAN_PRECIO_MES, BuildConfig.PLAN_PRECIO_ANIO),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text(stringResource(R.string.plan_precio_nota), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 10.dp), textAlign = TextAlign.Center)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        when {
                            vm.cargando -> CircularProgressIndicator(Modifier.size(28.dp))
                            e.esCumbre -> {}
                            e.avisame -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.plan_avisado), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            !vm.hayCuenta -> OutlinedButton(onClick = onCuenta) { Text(stringResource(R.string.plan_sin_cuenta)) }
                            else -> Button(onClick = { vm.avisame() }) {
                                Icon(Icons.Filled.NotificationsActive, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.plan_avisame))
                            }
                        }
                    }
                    if (vm.error) Text(stringResource(R.string.legal_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun FilaBeneficio(b: Beneficio) {
    val texto = nombre(b)
    val gratis = when (b) { Beneficio.COACH -> stringResource(R.string.plan_b_coach_gratis); else -> null }
    val cumbre = when (b) { Beneficio.COACH -> stringResource(R.string.plan_b_coach_cumbre); else -> null }
    val incluidoGratis = Planes.incluido(b, Planes.Plan.GRATIS)
    val descGratis = stringResource(R.string.plan_gratis) + ": " + (gratis ?: if (incluidoGratis) "✓" else "—")
    val descCumbre = stringResource(R.string.plan_cumbre) + ": " + (cumbre ?: "✓")
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(texto, style = MaterialTheme.typography.bodyMedium)
            if (b.esencial) Text(stringResource(R.string.plan_siempre_gratis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (gratis != null) Text("${stringResource(R.string.plan_gratis)}: $gratis · ${stringResource(R.string.plan_cumbre)}: $cumbre",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Celda(incluidoGratis, descGratis)
        Celda(true, descCumbre, destacada = true)
    }
}

@Composable
private fun Celda(incluido: Boolean, descripcion: String, destacada: Boolean = false) {
    Box(Modifier.width(64.dp).semantics { contentDescription = descripcion }, contentAlignment = Alignment.Center) {
        Icon(
            if (incluido) Icons.Filled.Check else Icons.Filled.Remove, null,
            tint = when {
                !incluido -> MaterialTheme.colorScheme.outline
                destacada -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            },
        )
    }
}
