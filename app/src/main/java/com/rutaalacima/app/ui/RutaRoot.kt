package com.rutaalacima.app.ui

import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.indication
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.isTraversalGroup
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
    const val PUBLICAR = "publicar/{tipo}?meta={meta}&texto={texto}&anio={anio}&mes={mes}"
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
    const val RESUMEN = "resumen/{anio}"
    /** Diario de vida: mis álbumes por año, el álbum de un año y el diario de otra persona. */
    const val DIARIO = "diario"
    const val ALBUM = "album/{anio}"
    const val DIARIO_DE = "diario_de/{id}/{nombre}"
    /** Abrir la frase del día sellada desde dentro de la app. */
    const val SELLO = "sello"
    const val CARTA = "carta"
    /** Revisar reportes y sancionar (solo moderadores). */
    const val MODERACION = "moderacion"
    /** Plan Cumbre: qué incluye, precio y "Avísame". */
    const val PLAN = "plan"

    fun planificador(tab: Int = 0, fecha: java.time.LocalDate? = null) = "metas/$tab/${fecha ?: "-"}"
    fun workbook(id: String) = "workbook/$id"
    fun seccion(id: String, index: Int) = "seccion/$id/$index"
    fun proposito(id: Long) = "proposito/$id"
    fun nuevaMeta(nivel: NivelMeta) = "nueva_meta/${nivel.name}"
    fun publicar(tipo: String = "LOGRO", meta: String? = null, texto: String? = null, anio: Int? = null, mes: Int? = null): String {
        val q = listOfNotNull(meta?.let { "meta=" + android.net.Uri.encode(it) }, texto?.let { "texto=" + android.net.Uri.encode(it) },
            anio?.let { "anio=$it" }, mes?.let { "mes=$it" })
        return "publicar/$tipo" + if (q.isEmpty()) "" else q.joinToString("&", "?")
    }
    fun resumen(anio: Int) = "resumen/$anio"
    fun album(anio: Int) = "album/$anio"
    fun diarioDe(id: String, nombre: String) = "diario_de/$id/${android.net.Uri.encode(nombre.ifBlank { "-" })}"
    fun post(id: String) = "post/$id"
    fun chat(id: String) = "chat/$id"
    fun cordada(id: String) = "cordada/$id"
    fun revision(tipo: com.rutaalacima.app.domain.model.Revision.Tipo, dia: java.time.LocalDate) = "revision/${tipo.name}/$dia"
    fun acompanado(id: String, nombre: String) = "acompanado/$id/${android.net.Uri.encode(nombre.ifBlank { "-" })}"
}

/** Pestaña de la barra: ícono relleno cuando está activa y de contorno cuando no (Material 3). */
private data class Pestana(val ruta: String, val titulo: Int, val icono: ImageVector, val iconoInactivo: ImageVector = icono)

private val PESTANAS = listOf(
    Pestana(Rutas.RUTA, R.string.tab_ruta, Icons.Filled.Terrain, Icons.Outlined.Terrain),
    Pestana(Rutas.HOY, R.string.tab_hoy, Icons.Filled.Today, Icons.Outlined.Today),
    Pestana(Rutas.COMUNIDAD, R.string.tab_comunidad, Icons.Filled.Groups, Icons.Outlined.Groups),
    Pestana(Rutas.APRENDE, R.string.tab_aprende, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    Pestana(Rutas.PERFIL, R.string.tab_perfil, Icons.Filled.Person, Icons.Outlined.Person),
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
    // La primera vez: la carta de bienvenida sellada (después se relee desde el perfil)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var cartaLeida by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(com.rutaalacima.app.ui.inicio.CartaBienvenida.leida(ctx)) }
    if (!cartaLeida) {
        com.rutaalacima.app.ui.inicio.CartaBienvenidaScreen(onTerminar = {
            com.rutaalacima.app.ui.inicio.CartaBienvenida.marcar(ctx); cartaLeida = true; inicioVisto = true
        })
        return
    }
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
    // Cuentas que aún no aceptan la autorización de tratamiento de datos vigente (Ley 1581)
    com.rutaalacima.app.ui.legal.AutorizacionPendiente()

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
                    if (!ancho) BarraNavegacion(actual = rutaActual, onIr = { nav.irAPestana(it) })
                }
            }
        },
        // Publicar ("plantar tu bandera"): la acción principal de Comunidad, como botón flotante de Material 3.
        // En Mi ruta y Perfil se publica desde la propia pantalla; en tableta, desde el riel lateral.
        floatingActionButton = {
            if (!ancho && rutaActual == Rutas.COMUNIDAD) {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { nav.navigate(Rutas.publicar()) },
                    icon = { Icon(painterResource(R.drawable.ic_plantar_bandera), null, Modifier.size(22.dp)) },
                    text = { Text(stringResource(R.string.publicar)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                        defaultElevation = 2.dp, pressedElevation = 4.dp, focusedElevation = 2.dp, hoveredElevation = 3.dp,
                    ),
                )
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
                    onAlbum = { nav.navigate(Rutas.album(it)) },
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
                    onAutor = { id, nombre -> nav.navigate(Rutas.diarioDe(id, nombre)) },
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
            composable(Rutas.COACH) { CoachScreen(onBack = { nav.popBackStack() }, onCoachVida = { nav.navigate(Rutas.COACH_VIDA) }, onPlan = { nav.navigate(Rutas.PLAN) }) }
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
                navArgument("texto") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("anio") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("mes") { type = NavType.StringType; nullable = true; defaultValue = null },
            )) { e ->
                val tipo = e.arguments?.getString("tipo").orEmpty()
                val meta = e.arguments?.getString("meta")
                // Texto que llega escrito (frase del día, resumen del año) o el de un logro recién cumplido
                val texto = e.arguments?.getString("texto") ?: if (tipo == "LOGRO") meta?.let { stringResource(R.string.logro_texto_inicial, it) } else null
                PublicarScreen(tipoInicial = tipo, onBack = { nav.popBackStack() }, metaInicial = meta, textoInicial = texto,
                    anioInicial = e.arguments?.getString("anio")?.toIntOrNull(), mesInicial = e.arguments?.getString("mes")?.toIntOrNull())
            }
            composable(Rutas.RESUMEN, arguments = listOf(navArgument("anio") { type = NavType.IntType })) { e ->
                com.rutaalacima.app.ui.hoy.ResumenAnioScreen(
                    anio = e.arguments?.getInt("anio") ?: java.time.LocalDate.now().year,
                    onBack = { nav.popBackStack() },
                    onPublicar = { titulo, texto -> nav.navigate(Rutas.publicar("REFLEXION", titulo, texto)) },
                )
            }
            composable(Rutas.PLAN) { com.rutaalacima.app.ui.plan.PlanScreen(onBack = { nav.popBackStack() }, onCuenta = { nav.navigate(Rutas.AJUSTES) }) }
            composable(Rutas.MODERACION) { com.rutaalacima.app.ui.moderacion.ModeracionScreen(onBack = { nav.popBackStack() }) }
            composable(Rutas.CARTA) { com.rutaalacima.app.ui.inicio.CartaBienvenidaScreen(onTerminar = { nav.popBackStack() }) }
            composable(Rutas.SELLO) { com.rutaalacima.app.ui.inicio.InicioScreen(onTerminar = { nav.popBackStack() }) }
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
            composable(Rutas.VISION) { com.rutaalacima.app.ui.vision.VisionScreen(onBack = { nav.popBackStack() }, onGuia = { nav.navigate(Rutas.workbook(it)) }) }
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
            composable(Rutas.DIARIO) {
                com.rutaalacima.app.ui.perfil.DiarioScreen(onBack = { nav.popBackStack() }, onAnio = { nav.navigate(Rutas.album(it)) },
                    onRecuerdo = { nav.navigate(Rutas.publicar("LOGRO", anio = it)) })
            }
            composable(Rutas.ALBUM, arguments = listOf(navArgument("anio") { type = NavType.IntType })) { e ->
                com.rutaalacima.app.ui.perfil.AlbumAnioScreen(
                    anio = e.arguments?.getInt("anio") ?: java.time.LocalDate.now().year, onBack = { nav.popBackStack() },
                    onAbrirPost = { nav.navigate(Rutas.post(it)) }, onRecuerdo = { nav.navigate(Rutas.publicar("LOGRO", anio = it)) },
                )
            }
            composable(Rutas.DIARIO_DE, arguments = listOf(navArgument("id") { type = NavType.StringType }, navArgument("nombre") { type = NavType.StringType })) { e ->
                com.rutaalacima.app.ui.perfil.DiarioAjenoScreen(
                    autorId = e.arguments?.getString("id").orEmpty(), nombre = e.arguments?.getString("nombre").orEmpty().let { if (it == "-") "" else it },
                    onBack = { nav.popBackStack() }, onAbrirPost = { nav.navigate(Rutas.post(it)) },
                )
            }
            composable(Rutas.CONSTANCIA) { com.rutaalacima.app.ui.hoy.ConstanciaScreen(onBack = { nav.popBackStack() }, onResumen = { nav.navigate(Rutas.resumen(it)) }) }
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
 * Barra de navegación inferior (Material 3), sobria y limpia: cinco destinos con su nombre siempre
 * visible. El destino activo lleva el ícono relleno dentro de una píldora dorada suave; los demás, el
 * ícono de contorno en tinta tenue. Sin sombra: una línea de papel la separa del contenido.
 * Altura compacta de 64 dp (la "barra corta" de Material 3) más el espacio de los gestos del sistema.
 */
@Composable
private fun BarraNavegacion(actual: String?, onIr: (String) -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(colores.surface)) {
        androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = colores.outlineVariant.copy(alpha = 0.7f))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp).semantics { isTraversalGroup = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PESTANAS.forEach { p -> DestinoBarra(p, elegida = actual == p.ruta, onClick = { onIr(p.ruta) }) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DestinoBarra(p: Pestana, elegida: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val nombre = stringResource(p.titulo)
    val toque = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    // La píldora crece desde el centro al elegir (salida suave, sin rebote)
    val ancho by androidx.compose.animation.core.animateDpAsState(
        if (elegida) 56.dp else 32.dp,
        androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "pildora",
    )
    val fondo by androidx.compose.animation.animateColorAsState(
        if (elegida) colores.secondaryContainer else Color.Transparent, androidx.compose.animation.core.tween(220), label = "fondo",
    )
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(
                selected = elegida, onClick = onClick, role = Role.Tab,
                interactionSource = toque, indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(CircleShape)
                .indication(toque, androidx.compose.material3.ripple(bounded = true, color = colores.primary)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(width = ancho, height = 32.dp).background(fondo, CircleShape))
            Icon(
                if (elegida) p.icono else p.iconoInactivo, contentDescription = null,
                tint = if (elegida) colores.onSecondaryContainer else colores.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            nombre, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, // mismo peso: el activo se distingue por color y píldora, y nada se corta
            color = if (elegida) colores.onSurface else colores.onSurfaceVariant,
        )
    }
}
