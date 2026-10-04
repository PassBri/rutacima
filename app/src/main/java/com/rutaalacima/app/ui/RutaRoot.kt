package com.rutaalacima.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.ui.axes.AxesScreen
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.home.HomeScreen
import com.rutaalacima.app.ui.kit.KitScreen
import com.rutaalacima.app.ui.library.LibraryScreen
import com.rutaalacima.app.ui.onboarding.OnboardingScreen
import com.rutaalacima.app.ui.planner.PlannerScreen
import com.rutaalacima.app.ui.planner.PropositoScreen
import com.rutaalacima.app.ui.workbook.SectionScreen
import com.rutaalacima.app.ui.workbook.WorkbookIndexScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Rutas de navegación. */
object Rutas {
    const val INICIO = "inicio"
    const val RUTA = "ruta"
    const val PLAN = "plan"
    const val EJES = "ejes"
    const val KIT = "kit"
    const val WORKBOOK = "workbook/{id}"
    const val SECCION = "seccion/{id}/{index}"
    const val PROPOSITO = "proposito/{id}"

    fun workbook(id: String) = "workbook/$id"
    fun seccion(id: String, index: Int) = "seccion/$id/$index"
    fun proposito(id: Long) = "proposito/$id"
}

private data class Pestana(val ruta: String, val titulo: String, val icono: ImageVector)

private val PESTANAS = listOf(
    Pestana(Rutas.INICIO, "Inicio", Icons.Filled.Home),
    Pestana(Rutas.RUTA, "Ruta", Icons.AutoMirrored.Filled.MenuBook),
    Pestana(Rutas.PLAN, "Plan 5 años", Icons.Filled.Terrain),
    Pestana(Rutas.EJES, "Ejes", Icons.Filled.Hub),
    Pestana(Rutas.KIT, "Kit", Icons.Filled.MedicalServices),
)

enum class EstadoApp { CARGANDO, ONBOARDING, LISTO }

class RootViewModel(c: AppContainer) : ViewModel() {
    val estado: StateFlow<EstadoApp> = c.perfil.perfil
        .map { if (it.onboardingCompleto) EstadoApp.LISTO else EstadoApp.ONBOARDING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoApp.CARGANDO)
}

@Composable
fun RutaRoot() {
    val vm = rutaViewModel { RootViewModel(it) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    when (estado) {
        EstadoApp.CARGANDO -> Box(Modifier.fillMaxSize())
        EstadoApp.ONBOARDING -> OnboardingScreen()
        EstadoApp.LISTO -> AppPrincipal()
    }
}

@Composable
private fun AppPrincipal() {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val mostrarBarra = PESTANAS.any { it.ruta == rutaActual }

    Scaffold(
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    PESTANAS.forEach { p ->
                        NavigationBarItem(
                            selected = rutaActual == p.ruta,
                            onClick = { nav.irAPestana(p.ruta) },
                            icon = { Icon(p.icono, contentDescription = p.titulo) },
                            label = { Text(p.titulo) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.secondaryContainer),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Rutas.INICIO) {
            composable(Rutas.INICIO) {
                HomeScreen(
                    contentPadding = padding,
                    onOpenWorkbook = { nav.navigate(Rutas.workbook(it)) },
                    onGoToAxes = { nav.irAPestana(Rutas.EJES) },
                    onGoToKit = { nav.irAPestana(Rutas.KIT) },
                )
            }
            composable(Rutas.RUTA) { LibraryScreen(padding) { nav.navigate(Rutas.workbook(it)) } }
            composable(Rutas.PLAN) {
                PlannerScreen(
                    contentPadding = padding,
                    onOpenProposito = { nav.navigate(Rutas.proposito(it)) },
                    onOpenWorkbook = { nav.navigate(Rutas.workbook(it)) },
                )
            }
            composable(Rutas.EJES) { AxesScreen(padding) }
            composable(Rutas.KIT) {
                KitScreen(
                    contentPadding = padding,
                    onOpenSection = { id, i -> nav.navigate(Rutas.seccion(id, i)) },
                    onOpenWorkbook = { nav.navigate(Rutas.workbook(it)) },
                )
            }
            composable(Rutas.WORKBOOK, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                WorkbookIndexScreen(
                    workbookId = id,
                    onBack = { nav.popBackStack() },
                    onOpenSection = { nav.navigate(Rutas.seccion(id, it)) },
                )
            }
            composable(
                Rutas.SECCION,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("index") { type = NavType.IntType },
                ),
            ) { e ->
                SectionScreen(
                    workbookId = e.arguments?.getString("id").orEmpty(),
                    startIndex = e.arguments?.getInt("index") ?: 0,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Rutas.PROPOSITO, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                PropositoScreen(propositoId = e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
