package com.rutaalacima.app.ui

import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import com.rutaalacima.app.ui.theme.Papel
import com.rutaalacima.app.ui.theme.fondoPapel
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.rutaalacima.app.R
import com.rutaalacima.app.ui.aprende.AprendeScreen
import com.rutaalacima.app.ui.axes.AxesScreen
import com.rutaalacima.app.ui.coach.CoachScreen
import com.rutaalacima.app.ui.comunidad.ComunidadScreen
import com.rutaalacima.app.ui.comunidad.PostDetalleScreen
import com.rutaalacima.app.ui.comunidad.PublicarScreen
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.hoy.HoyScreen
import com.rutaalacima.app.ui.kit.KitScreen
import com.rutaalacima.app.ui.metas.PlanificadorScreen
import com.rutaalacima.app.ui.ruta.RutaScreen
import com.rutaalacima.app.ui.metas.NivelMeta
import com.rutaalacima.app.ui.metas.NuevaMetaScreen
import com.rutaalacima.app.ui.onboarding.OnboardingScreen
import com.rutaalacima.app.ui.perfil.AjustesScreen
import com.rutaalacima.app.ui.perfil.PerfilScreen
import com.rutaalacima.app.ui.planner.PropositoScreen
import com.rutaalacima.app.ui.workbook.SectionScreen
import com.rutaalacima.app.ui.workbook.WorkbookIndexScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Rutas de navegación. */
object Rutas {
    const val RUTA = "ruta"
    const val HOY = "hoy"
    /** Planificador completo: pestaña inicial y fecha (ISO o "-"). */
    const val METAS = "metas/{tab}/{fecha}"
    const val COMUNIDAD = "comunidad"
    const val APRENDE = "aprende"
    const val PERFIL = "perfil"
    const val WORKBOOK = "workbook/{id}"
    const val SECCION = "seccion/{id}/{index}"
    const val PROPOSITO = "proposito/{id}"
    const val KIT = "kit"
    const val EJES = "ejes"
    const val COACH = "coach"
    const val NUEVA_META = "nueva_meta/{nivel}"
    const val PUBLICAR = "publicar/{tipo}?meta={meta}"
    const val POST = "post/{id}"
    const val AJUSTES = "ajustes"
    const val FRASES = "frases"
    const val WEB = "web"
    const val VISION = "vision"
    const val MENSAJES = "mensajes"
    const val CHAT = "chat/{id}"
    const val COACH_VIDA = "coach_vida"
    const val ACOMPANADO = "acompanado/{id}/{nombre}"
    const val REVISION = "revision/{tipo}/{dia}"
    const val CONSTANCIA = "constancia"
    const val CORDADAS = "cordadas"
    const val CORDADA = "cordada/{id}"

    fun planificador(tab: Int = 0, fecha: java.time.LocalDate? = null) = "metas/$tab/${fecha ?: "-"}"
    fun workbook(id: String) = "workbook/$id"
    fun seccion(id: String, index: Int) = "seccion/$id/$index"
    fun proposito(id: Long) = "proposito/$id"
    fun nuevaMeta(nivel: NivelMeta) = "nueva_meta/${nivel.name}"
    fun publicar(tipo: String = "LOGRO", meta: String? = null) = "publicar/$tipo" + (meta?.let { "?meta=" + android.net.Uri.encode(it) } ?: "")
    fun post(id: String) = "post/$id"
    fun chat(id: String) = "chat/$id"
    fun cordada(id: String) = "cordada/$id"
    fun revision(tipo: com.rutaalacima.app.domain.model.Revision.Tipo, dia: java.time.LocalDate) = "revision/${tipo.name}/$dia"
    fun acompanado(id: String, nombre: String) = "acompanado/$id/${android.net.Uri.encode(nombre.ifBlank { "-" })}"
}

private data class Pestana(val ruta: String, val titulo: Int, val icono: ImageVector)

private val PESTANAS = listOf(
    Pestana(Rutas.RUTA, R.string.tab_ruta, Icons.Filled.Terrain),
    Pestana(Rutas.HOY, R.string.tab_hoy, Icons.Filled.Today),
    Pestana(Rutas.COMUNIDAD, R.string.tab_comunidad, Icons.Filled.Groups),
    Pestana(Rutas.APRENDE, R.string.tab_aprende, Icons.AutoMirrored.Filled.MenuBook),
    Pestana(Rutas.PERFIL, R.string.tab_perfil, Icons.Filled.Person),
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
    // Ventana de inicio animada una vez por arranque
    var inicioVisto by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    if (!inicioVisto) {
        com.rutaalacima.app.ui.inicio.InicioScreen(onTerminar = { inicioVisto = true })
        return
    }
    when (estado) {
        EstadoApp.CARGANDO -> Box(Modifier.fillMaxSize().fondoPapel())
        EstadoApp.ONBOARDING -> OnboardingScreen()
        EstadoApp.LISTO -> AppPrincipal()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPrincipal() {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val pestana = PESTANAS.firstOrNull { it.ruta == rutaActual }

    // Código QR de Rutaalacima Web escaneado con la cámara: abrir la pantalla para confirmarlo
    val contenedor = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.rutaalacima.app.RutaApp).container
    val codigoWeb by contenedor.web.codigoPendiente.collectAsStateWithLifecycle()
    // Al tocar una notificación de mensajes: ir directo a esa conversación
    val irA by contenedor.navegacionPendiente.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(irA) {
        irA?.let { destino ->
            contenedor.navegacionPendiente.value = null
            runCatching { nav.navigate(destino) { launchSingleTop = true } }
        }
    }
    androidx.compose.runtime.LaunchedEffect(codigoWeb) {
        if (codigoWeb != null && rutaActual != Rutas.WEB) nav.navigate(Rutas.WEB) { launchSingleTop = true }
    }

    // Tableta o teléfono en horizontal: las pestañas van en un riel a la izquierda
    val ancho = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val celebracion by contenedor.celebracion.collectAsStateWithLifecycle()
  Box(Modifier.fillMaxSize().fondoPapel()) {
   Row(Modifier.fillMaxSize()) {
    if (ancho && pestana != null) {
        RielLateral(actual = rutaActual, onIr = { nav.irAPestana(it) }, onPublicar = { nav.navigate(Rutas.publicar()) })
    }
    Scaffold(
        modifier = Modifier.weight(1f),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            if (pestana != null) {
                TopAppBar(
                    // El sello firma la app; el texto dice dónde estás (Mi ruta, Hoy, Comunidad…).
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.logo_sello), stringResource(R.string.app_name), Modifier.size(30.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(pestana.titulo), style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    actions = {
                        if (pestana.ruta == Rutas.COMUNIDAD) {
                            IconButton(onClick = { nav.navigate(Rutas.CORDADAS) }) {
                                Icon(Icons.Filled.Diversity3, stringResource(R.string.cordadas))
                            }
                            IconButton(onClick = { nav.navigate(Rutas.MENSAJES) }) {
                                Icon(Icons.AutoMirrored.Filled.Chat, stringResource(R.string.mensajes))
                            }
                        }
                        if (pestana.ruta == Rutas.PERFIL) {
                            IconButton(onClick = { nav.navigate(Rutas.AJUSTES) }) { Icon(Icons.Filled.Settings, stringResource(R.string.ajustes)) }
                        }
                        IconButton(onClick = { nav.navigate(Rutas.COACH) }) {
                            Icon(Icons.Filled.AutoAwesome, stringResource(R.string.coach), tint = MaterialTheme.colorScheme.secondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                )
            }
        },
        bottomBar = {
            if (pestana != null) {
                Column {
                    // Mientras suena un audiolibro: pausa o vuelve al capítulo desde cualquier pestaña
                    com.rutaalacima.app.ui.workbook.MiniReproductor(onAbrir = { wb, sec -> nav.navigate(Rutas.seccion(wb, sec)) })
                    if (!ancho) BarraIconos(actual = rutaActual, onIr = { nav.irAPestana(it) }, onPublicar = { nav.navigate(Rutas.publicar()) })
                }
            }
        },
    ) { padding ->
        val abrirWorkbook: (String) -> Unit = { nav.navigate(Rutas.workbook(it)) }
        val abrirSeccion: (String, Int) -> Unit = { id, i -> nav.navigate(Rutas.seccion(id, i)) }
        NavHost(nav, startDestination = Rutas.RUTA) {
            composable(Rutas.RUTA) {
                RutaScreen(
                    contentPadding = padding,
                    onPlanificador = { tab, fecha -> nav.navigate(Rutas.planificador(tab, fecha)) },
                    onNuevaMeta = { nav.navigate(Rutas.nuevaMeta(it)) },
                    onProposito = { nav.navigate(Rutas.proposito(it)) },
                    onPublicar = { nav.navigate(Rutas.publicar(it)) },
                    onAbrirPost = { nav.navigate(Rutas.post(it)) },
                    onKit = { nav.navigate(Rutas.KIT) },
                    onAjustes = { nav.navigate(Rutas.AJUSTES) },
                )
            }
            composable(Rutas.HOY) {
                HoyScreen(
                    contentPadding = padding,
                    onAbrirWorkbook = abrirWorkbook,
                    onAbrirSeccion = abrirSeccion,
                    onIrA = { ruta -> if (PESTANAS.any { it.ruta == ruta }) nav.irAPestana(ruta) else nav.navigate(ruta) },
                )
            }
            composable(
                Rutas.METAS,
                arguments = listOf(navArgument("tab") { type = NavType.IntType }, navArgument("fecha") { type = NavType.StringType }),
            ) { e ->
                PlanificadorScreen(
                    tabInicial = e.arguments?.getInt("tab") ?: 0,
                    fechaInicial = e.arguments?.getString("fecha")?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                    onBack = { nav.popBackStack() },
                    onNuevaMeta = { nav.navigate(Rutas.nuevaMeta(it)) },
                    onOpenProposito = { nav.navigate(Rutas.proposito(it)) },
                    onOpenWorkbook = abrirWorkbook,
                )
            }
            composable(Rutas.COMUNIDAD) {
                ComunidadScreen(
                    contentPadding = padding,
                    onAbrirPost = { nav.navigate(Rutas.post(it)) },
                    onPublicar = { nav.navigate(Rutas.publicar(it)) },
                    onCuenta = { nav.navigate(Rutas.AJUSTES) },
                )
            }
            composable(Rutas.APRENDE) { AprendeScreen(padding, onOpen = abrirWorkbook) }
            composable(Rutas.PERFIL) {
                PerfilScreen(
                    contentPadding = padding,
                    onAbrirPost = { nav.navigate(Rutas.post(it)) },
                    onPublicar = { nav.navigate(Rutas.publicar(it)) },
                    onEvaluarEjes = { nav.navigate(Rutas.EJES) },
                    onAjustes = { nav.navigate(Rutas.AJUSTES) },
                    onFrases = { nav.navigate(Rutas.FRASES) },
                    onVision = { nav.navigate(Rutas.VISION) },
                    onIrA = { ruta -> runCatching { nav.navigate(ruta) } },
                )
            }
            composable(Rutas.WORKBOOK, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                WorkbookIndexScreen(workbookId = id, onBack = { nav.popBackStack() }, onOpenSection = { abrirSeccion(id, it) })
            }
            composable(
                Rutas.SECCION,
                arguments = listOf(navArgument("id") { type = NavType.StringType }, navArgument("index") { type = NavType.IntType }),
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
            composable(Rutas.KIT) {
                KitScreen(onBack = { nav.popBackStack() }, onOpenSection = abrirSeccion, onOpenWorkbook = abrirWorkbook)
            }
            composable(Rutas.EJES) { AxesScreen(onBack = { nav.popBackStack() }) }
            composable(Rutas.COACH) { CoachScreen(onBack = { nav.popBackStack() }, onCoachVida = { nav.navigate(Rutas.COACH_VIDA) }) }
            composable(Rutas.NUEVA_META, arguments = listOf(navArgument("nivel") { type = NavType.StringType })) { e ->
                val nivel = runCatching { NivelMeta.valueOf(e.arguments?.getString("nivel").orEmpty()) }.getOrDefault(NivelMeta.MES)
                NuevaMetaScreen(
                    nivelInicial = nivel,
                    onBack = { nav.popBackStack() },
                    onCompartir = { nav.popBackStack(); nav.navigate(Rutas.publicar("META")) },
                )
            }
            composable(Rutas.PUBLICAR, arguments = listOf(
                navArgument("tipo") { type = NavType.StringType },
                navArgument("meta") { type = NavType.StringType; nullable = true; defaultValue = null },
            )) { e ->
                val meta = e.arguments?.getString("meta")
                PublicarScreen(
                    tipoInicial = e.arguments?.getString("tipo").orEmpty(), onBack = { nav.popBackStack() },
                    metaInicial = meta, textoInicial = meta?.let { stringResource(R.string.logro_texto_inicial, it) },
                )
            }
            composable(Rutas.POST, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                PostDetalleScreen(
                    postId = e.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onChat = { nav.navigate(Rutas.chat(it)) },
                )
            }
            composable(Rutas.AJUSTES) {
                AjustesScreen(onBack = { nav.popBackStack() }, onFrases = { nav.navigate(Rutas.FRASES) }, onWeb = { nav.navigate(Rutas.WEB) })
            }
            composable(Rutas.VISION) { com.rutaalacima.app.ui.vision.VisionScreen(onBack = { nav.popBackStack() }) }
            composable(Rutas.WEB) {
                com.rutaalacima.app.ui.web.WebScreen(onBack = { nav.popBackStack() }, onCuenta = { nav.navigate(Rutas.AJUSTES) })
            }
            composable(Rutas.MENSAJES) {
                com.rutaalacima.app.ui.mensajes.MensajesScreen(
                    onBack = { nav.popBackStack() },
                    onAbrir = { nav.navigate(Rutas.chat(it)) },
                    onCuenta = { nav.navigate(Rutas.AJUSTES) },
                    onCoachVida = { nav.navigate(Rutas.COACH_VIDA) },
                )
            }
            composable(Rutas.CHAT, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                com.rutaalacima.app.ui.mensajes.ChatScreen(id = e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable(Rutas.COACH_VIDA) {
                com.rutaalacima.app.ui.mensajes.CoachVidaScreen(
                    onBack = { nav.popBackStack() },
                    onChat = { nav.navigate(Rutas.chat(it)) },
                    onAcompanado = { id, nombre -> nav.navigate(Rutas.acompanado(id, nombre)) },
                    onCuenta = { nav.navigate(Rutas.AJUSTES) },
                )
            }
            composable(
                Rutas.ACOMPANADO,
                arguments = listOf(navArgument("id") { type = NavType.StringType }, navArgument("nombre") { type = NavType.StringType }),
            ) { e ->
                com.rutaalacima.app.ui.mensajes.AcompanadoScreen(
                    id = e.arguments?.getString("id").orEmpty(),
                    nombre = e.arguments?.getString("nombre").orEmpty().let { if (it == "-") "" else it },
                    onBack = { nav.popBackStack() },
                    onChat = { nav.navigate(Rutas.chat(it)) },
                )
            }
            composable(
                Rutas.REVISION,
                arguments = listOf(navArgument("tipo") { type = NavType.StringType }, navArgument("dia") { type = NavType.StringType }),
            ) { e ->
                val tipo = runCatching { com.rutaalacima.app.domain.model.Revision.Tipo.valueOf(e.arguments?.getString("tipo").orEmpty()) }
                    .getOrDefault(com.rutaalacima.app.domain.model.Revision.Tipo.SEMANA)
                val dia = runCatching { java.time.LocalDate.parse(e.arguments?.getString("dia")) }.getOrDefault(java.time.LocalDate.now())
                com.rutaalacima.app.ui.hoy.RevisionScreen(tipo, dia, onBack = { nav.popBackStack() })
            }
            composable(Rutas.CORDADAS) {
                com.rutaalacima.app.ui.cordadas.CordadasScreen(
                    onBack = { nav.popBackStack() }, onAbrir = { nav.navigate(Rutas.cordada(it)) }, onCuenta = { nav.navigate(Rutas.AJUSTES) },
                )
            }
            composable(Rutas.CORDADA, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                com.rutaalacima.app.ui.cordadas.CordadaScreen(id = e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable(Rutas.CONSTANCIA) { com.rutaalacima.app.ui.hoy.ConstanciaScreen(onBack = { nav.popBackStack() }) }
            composable(Rutas.FRASES) { com.rutaalacima.app.ui.frases.FrasesScreen(onBack = { nav.popBackStack() }) }
        }
    }
   }
    // Meta cumplida: la bandera se clava en la cumbre
    celebracion?.let { c ->
        com.rutaalacima.app.ui.components.CelebracionCumbre(c, onFin = { contenedor.celebracion.value = null }, onCompartir = {
            // Como en Strava: el logro se puede compartir al instante, ya con la meta vinculada
            contenedor.celebracion.value = null
            nav.navigate(Rutas.publicar("LOGRO", c.titulo))
        })
    }
  }
}

/** Riel de pestañas para pantallas anchas, con el botón de publicar arriba. */
@Composable
private fun RielLateral(actual: String?, onIr: (String) -> Unit, onPublicar: () -> Unit) {
    androidx.compose.material3.NavigationRail(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        header = {
            Image(painterResource(R.drawable.logo_sello), null, Modifier.size(40.dp).padding(top = 4.dp))
            androidx.compose.material3.FilledTonalIconButton(onClick = onPublicar, modifier = Modifier.padding(top = 12.dp)) {
                Icon(painterResource(R.drawable.ic_plantar_bandera), stringResource(R.string.publicar))
            }
        },
    ) {
        Spacer(Modifier.weight(1f))
        PESTANAS.forEach { p ->
            androidx.compose.material3.NavigationRailItem(
                selected = actual == p.ruta, onClick = { onIr(p.ruta) },
                icon = { Icon(p.icono, null) }, label = { Text(stringResource(p.titulo)) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

private fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Barra inferior compacta: solo íconos (56 dp en vez de 80). Al apoyar el dedo aparece el
 * nombre de la pestaña en una etiqueta de papel; si deslizas el dedo por la barra, la etiqueta
 * sigue al dedo (con una vibración suave en cada ícono) y al soltar abre esa pestaña.
 * Con TalkBack cada ícono se anuncia con su nombre y se activa con doble toque.
 */
@Composable
private fun BarraIconos(actual: String?, onIr: (String) -> Unit, onPublicar: () -> Unit) {
    var sobre by remember { mutableStateOf<Int?>(null) }
    val ir by androidx.compose.runtime.rememberUpdatedState(onIr)
    val publicar by androidx.compose.runtime.rememberUpdatedState(onPublicar)
    val haptic = LocalHapticFeedback.current
    // Las 5 pestañas y, en el centro, el botón para publicar ("plantar tu bandera")
    val n = PESTANAS.size + 1
    fun pestanaEn(k: Int): Pestana? = if (k == CENTRO) null else PESTANAS[if (k < CENTRO) k else k - 1]
    val colores = MaterialTheme.colorScheme
    val nombrePublicar = stringResource(R.string.publicar)
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RectangleShape, clip = false, ambientColor = Papel.Sombra, spotColor = Papel.Sombra)
            .background(colores.surfaceContainerLow)
            .navigationBarsPadding()
            .height(56.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val abajo = awaitFirstDown()
                    fun indice(x: Float) = (x / size.width * n).toInt().coerceIn(0, n - 1)
                    var i = indice(abajo.position.x)
                    sobre = i
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    var soltado = false
                    try {
                        while (true) {
                            val evento = awaitPointerEvent()
                            val c = evento.changes.firstOrNull { it.id == abajo.id } ?: break
                            if (!c.pressed) { soltado = true; break }
                            val j = indice(c.position.x)
                            if (j != i) {
                                i = j
                                sobre = j
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            c.consume()
                        }
                    } finally {
                        sobre = null
                    }
                    if (soltado) pestanaEn(i)?.let { ir(it.ruta) } ?: publicar()
                }
            },
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            for (k in 0 until n) {
                val p = pestanaEn(k)
                val elegida = p != null && actual == p.ruta
                val nombre = p?.let { stringResource(it.titulo) } ?: nombrePublicar
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            role = if (p == null) Role.Button else Role.Tab
                            if (p != null) selected = elegida
                            contentDescription = nombre
                            onClick { if (p != null) onIr(p.ruta) else onPublicar(); true }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (p == null) {
                        // Botón para publicar: igual a las pestañas, con un acento leve (borde dorado y fondo apenas teñido)
                        val presionado = sobre == k
                        Box(
                            Modifier
                                .size(width = 56.dp, height = 32.dp)
                                .background(
                                    if (presionado) colores.secondaryContainer else colores.primary.copy(alpha = 0.08f),
                                    CircleShape,
                                )
                                .border(1.dp, colores.secondary.copy(alpha = 0.55f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                androidx.compose.ui.res.painterResource(R.drawable.ic_plantar_bandera), contentDescription = null,
                                tint = colores.primary, modifier = Modifier.size(24.dp),
                            )
                        }
                    } else {
                        val resaltada = elegida || sobre == k
                        Box(
                            Modifier
                                .size(width = 56.dp, height = 32.dp)
                                .background(if (resaltada) colores.secondaryContainer else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                p.icono, contentDescription = null,
                                tint = if (elegida) colores.primary else colores.onSurfaceVariant,
                            )
                        }
                    }
                    // Etiqueta flotante con el nombre, encima del ícono que toca el dedo
                    if (sobre == k) {
                        Text(
                            nombre,
                            style = MaterialTheme.typography.labelLarge,
                            color = colores.onPrimary,
                            maxLines = 1,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .wrapContentSize(unbounded = true)
                                .offset(y = (-44).dp)
                                .zIndex(1f)
                                .shadow(6.dp, RoundedCornerShape(10.dp), ambientColor = Papel.Sombra, spotColor = Papel.Sombra)
                                .background(colores.primary, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Posición del botón para publicar en la barra inferior (en medio, como en otras redes). */
private const val CENTRO = 2
