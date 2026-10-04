package com.rutaalacima.app.ui.frases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.FraseLibro
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.theme.EstiloCita
import com.rutaalacima.app.ui.theme.fondoPapel
import com.rutaalacima.app.util.formatoFecha
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class FrasesViewModel(c: AppContainer) : ViewModel() {
    var todas by mutableStateOf<List<FraseLibro>>(emptyList())
        private set
    val desbloqueadas: StateFlow<Set<Int>> = c.frases.desbloqueadas
    init { viewModelScope.launch { todas = c.frases.todas() } }
}

/** "Mis frases": las frases del año que la persona ya abrió, de la más reciente a la primera. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrasesScreen(onBack: () -> Unit) {
    val vm = rutaViewModel { FrasesViewModel(it) }
    val abiertas by vm.desbloqueadas.collectAsStateWithLifecycle()
    val anio = LocalDate.now().year
    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.mis_frases)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.mis_frases_n, abiertas.size), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            if (abiertas.isEmpty()) item {
                Text(stringResource(R.string.frases_vacio), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val lista = abiertas.sortedDescending().mapNotNull { i -> vm.todas.getOrNull(i)?.let { i to it } }
            items(lista, key = { it.first }) { (i, f) ->
                RutaCard {
                    Text(formatoFecha(LocalDate.ofYearDay(anio, i + 1)), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary)
                    Text("“${f.t}”", style = EstiloCita, color = MaterialTheme.colorScheme.onSurface)
                    Text("— ${f.libro}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
