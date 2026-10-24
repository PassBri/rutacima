package com.rutaalacima.app.ui.finanzas

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rutaalacima.app.AppContainer
import com.rutaalacima.app.R
import com.rutaalacima.app.domain.model.Cuentas
import com.rutaalacima.app.domain.model.Cuentas.Mochila
import com.rutaalacima.app.domain.model.Cuentas.Mov
import com.rutaalacima.app.domain.model.Cuentas.Tipo
import com.rutaalacima.app.domain.model.Finanzas
import com.rutaalacima.app.ui.components.RutaCard
import com.rutaalacima.app.ui.components.SectionTitle
import com.rutaalacima.app.ui.components.rutaViewModel
import com.rutaalacima.app.ui.planner.nombreMes
import com.rutaalacima.app.ui.theme.fondoPapel
import com.rutaalacima.app.util.hoy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ====================================================================== Datos

/** Todo el libro de cuentas: los movimientos de cada día (con los del control de antes) y la moneda. */
data class Libro(val dias: Map<String, List<Mov>> = emptyMap(), val moneda: String = Cuentas.moneda(null), val respuestas: Map<String, String> = emptyMap()) {
    val propias: Map<String, List<Cuentas.Propia>> by lazy { Cuentas.leerPropias(respuestas["fin#cats"]) }
    val deudasDatos: com.rutaalacima.app.domain.model.Deudas.Datos by lazy { com.rutaalacima.app.domain.model.Deudas.leer(respuestas[com.rutaalacima.app.domain.model.Deudas.CLAVE]) }
    val deudas: List<com.rutaalacima.app.domain.model.Deudas.Hoy> by lazy { com.rutaalacima.app.domain.model.Deudas.hoy(deudasDatos, dias.values.flatten()) }
    val activos: List<com.rutaalacima.app.domain.model.Patrimonio.Activo> by lazy { com.rutaalacima.app.domain.model.Patrimonio.leer(respuestas[com.rutaalacima.app.domain.model.Patrimonio.CLAVE]) }
    val fijos: List<com.rutaalacima.app.domain.model.Fijos.Fijo> by lazy { com.rutaalacima.app.domain.model.Fijos.leer(respuestas[com.rutaalacima.app.domain.model.Fijos.CLAVE]) }
    val publica get() = respuestas["fin#visibilidad"] == "publica"
    val bloqueo get() = respuestas["fin#bloqueo"] == "1"
    val ocultarFuera get() = !publica && respuestas["fin#ocultar"] == "1"
    fun salud(anio: Int?) = com.rutaalacima.app.domain.model.Salud.de(dias, anio, deudas, activos, propias)
    fun movs(fecha: String): List<Mov> = dias[fecha].orEmpty()
    fun suma(prefijo: String): Cuentas.Suma = Cuentas.sumar(dias.filterKeys { it.startsWith(prefijo) }.values.flatten(), propias)
    val total: Cuentas.Suma by lazy { Cuentas.sumar(dias.values.flatten(), propias) }
    val anios: List<Int> get() = (dias.keys.mapNotNull { it.take(4).toIntOrNull() } + hoy().year).distinct().sorted()
    fun presupuesto(ym: YearMonth): String = respuestas[Finanzas.clavePresupuesto(ym.year, ym.monthValue)].orEmpty()
}

class CuentasViewModel(private val c: AppContainer) : ViewModel() {
    val libro: StateFlow<Libro> = combine(c.respuestas.observar(Cuentas.WB), c.agenda.conFinanzas) { r, agendas ->
        val dias = mutableMapOf<String, MutableList<Mov>>()
        agendas.forEach { a -> Cuentas.deAgenda(a.ingresos, a.gastos, a.ahorro, a.inversion).takeIf { it.isNotEmpty() }?.let { dias.getOrPut(a.fecha) { mutableListOf() } += it } }
        r.forEach { (k, v) -> if (k.startsWith(Cuentas.PREFIJO)) Cuentas.leer(v).takeIf { it.isNotEmpty() }?.let { dias.getOrPut(k.removePrefix(Cuentas.PREFIJO)) { mutableListOf() } += it } }
        val validos = dias.filterKeys { runCatching { LocalDate.parse(it) }.isSuccess }
        Libro(com.rutaalacima.app.domain.model.Fijos.unir(validos, com.rutaalacima.app.domain.model.Fijos.leer(r[com.rutaalacima.app.domain.model.Fijos.CLAVE]), hoy()), Cuentas.moneda(r[Cuentas.CLAVE_MONEDA]), r)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Libro())

    private suspend fun guardados(f: String) = Cuentas.leer(c.respuestas.cargar(Cuentas.WB)[Cuentas.claveDia(f)])
    private suspend fun guardar(f: String, xs: List<Mov>) = c.respuestas.guardar(Cuentas.WB, Cuentas.claveDia(f), Cuentas.escribir(xs))

    fun agregar(f: String, t: Tipo, monto: Double, cat: String, nota: String, repite: Boolean = false) = viewModelScope.launch {
        if (repite) fijos { it + com.rutaalacima.app.domain.model.Fijos.Fijo("f" + System.currentTimeMillis(), t.codigo, monto, cat, nota.trim(), f.takeLast(2).toInt(), f.take(7)) }
        else guardar(f, guardados(f) + Mov("m" + System.currentTimeMillis(), t.codigo, monto, cat, nota.trim()))
    }

    // ---------------------------------------------------------------- Fijos de cada mes
    private fun fijos(cambio: (List<com.rutaalacima.app.domain.model.Fijos.Fijo>) -> List<com.rutaalacima.app.domain.model.Fijos.Fijo>) =
        poner(com.rutaalacima.app.domain.model.Fijos.CLAVE, com.rutaalacima.app.domain.model.Fijos.escribir(cambio(libro.value.fijos)))
    fun fijoCambiar(id: String, cambio: (com.rutaalacima.app.domain.model.Fijos.Fijo) -> com.rutaalacima.app.domain.model.Fijos.Fijo) = fijos { l -> l.map { if (it.id == id) cambio(it) else it } }
    /** El monto nuevo rige desde este mes; si este mes ya se cambió a mano, sigue enlazado al fijo nuevo. */
    fun fijoEditar(id: String, n: String, dia: Int, v: Double) = viewModelScope.launch {
        val nuevoId = "f" + System.currentTimeMillis()
        val antes = libro.value.fijos.map { if (it.id == id) it.copy(n = n, dia = dia) else it }
        val cambia = antes.firstOrNull { it.id == id }?.m != v
        val despues = if (cambia) com.rutaalacima.app.domain.model.Fijos.cambiarMonto(antes, id, v, hoy(), nuevoId) else antes
        c.respuestas.guardar(Cuentas.WB, com.rutaalacima.app.domain.model.Fijos.CLAVE, com.rutaalacima.app.domain.model.Fijos.escribir(despues))
        if (despues.any { it.id == nuevoId }) antes.firstOrNull { it.id == id }?.let { x ->
            val f = com.rutaalacima.app.domain.model.Fijos.fecha(x, YearMonth.from(hoy())).toString()
            val g = guardados(f); if (g.any { it.fx == id }) guardar(f, g.map { if (it.fx == id) it.copy(fx = nuevoId) else it })
        }
    }
    fun fijoParar(id: String) = fijos { com.rutaalacima.app.domain.model.Fijos.parar(it, id, hoy()) }
    fun fijoRestaurar(x: com.rutaalacima.app.domain.model.Fijos.Fijo) = fijos { l -> l.filter { it.id != x.id } + x }

    /** Cambia un movimiento; si venía del control de antes, pasa al libro de cuentas y se borra de la agenda. */
    fun editar(f: String, x: Mov, nuevo: Mov) = viewModelScope.launch {
        val campo = x.agenda
        if (x.virtual) guardar(f, guardados(f) + nuevo.copy(id = "m" + System.currentTimeMillis(), fx = x.fx))   // un fijo cambiado solo este mes
        else if (campo != null) {
            limpiarAgenda(f, campo)
            guardar(f, guardados(f) + nuevo.copy(id = "m" + System.currentTimeMillis()))
        } else guardar(f, guardados(f).map { if (it.id == x.id) nuevo.copy(id = x.id) else it })
    }

    fun borrar(f: String, x: Mov) = viewModelScope.launch {
        val campo = x.agenda
        x.fx?.let { fx -> fijos { com.rutaalacima.app.domain.model.Fijos.saltar(it, fx, f.take(7), true) } }   // un fijo quitado: solo este mes
        if (campo != null) limpiarAgenda(f, campo) else if (!x.virtual) guardar(f, guardados(f).filter { it.id != x.id })
    }

    fun deshacerBorrado(f: String, x: Mov) = viewModelScope.launch {
        x.fx?.let { fx -> fijos { com.rutaalacima.app.domain.model.Fijos.saltar(it, fx, f.take(7), false) } }
        if (x.virtual) return@launch
        val campo = x.agenda
        if (campo != null) c.agenda.dia(LocalDate.parse(f)).let { d ->
            val v = Cuentas.numero(x.m)
            c.agenda.guardar(when (campo) { "ingresos" -> d.copy(ingresos = v); "gastos" -> d.copy(gastos = v); "ahorro" -> d.copy(ahorro = v); else -> d.copy(inversion = v) })
        } else guardar(f, guardados(f) + x)
    }

    private suspend fun limpiarAgenda(f: String, campo: String) {
        val d = c.agenda.dia(LocalDate.parse(f))
        c.agenda.guardar(when (campo) { "ingresos" -> d.copy(ingresos = ""); "gastos" -> d.copy(gastos = ""); "ahorro" -> d.copy(ahorro = ""); else -> d.copy(inversion = "") })
    }

    fun elegirMoneda(m: String) = viewModelScope.launch { c.respuestas.guardar(Cuentas.WB, Cuentas.CLAVE_MONEDA, m) }
    private fun poner(clave: String, v: String) = viewModelScope.launch { c.respuestas.guardar(Cuentas.WB, clave, v) }

    // ---------------------------------------------------------------- Categorías propias
    fun crearCategoria(t: Tipo, nombre: String, verde: Boolean): String? {
        val n = nombre.trim().take(40); if (n.isEmpty()) return null
        val cod = Cuentas.codigoPropia(n)
        val todas = libro.value.propias.toMutableMap()
        if (Cuentas.categorias(t, todas).none { it.first == cod })
            todas[t.codigo] = todas[t.codigo].orEmpty() + Cuentas.Propia(cod, n, if (t == Tipo.GAS) (if (verde) "verde" else "amarilla") else null)
        poner("fin#cats", Cuentas.escribirPropias(todas))
        return cod
    }

    // ---------------------------------------------------------------- Deudas
    private fun deudas(cambio: (com.rutaalacima.app.domain.model.Deudas.Datos) -> com.rutaalacima.app.domain.model.Deudas.Datos) =
        poner(com.rutaalacima.app.domain.model.Deudas.CLAVE, com.rutaalacima.app.domain.model.Deudas.escribir(cambio(libro.value.deudasDatos)))
    fun deudaAgregar(nombre: String, saldo: Double, tasa: Double, minimo: Double, buena: Boolean) =
        deudas { it.copy(lista = it.lista + com.rutaalacima.app.domain.model.Deudas.Deuda("d" + System.currentTimeMillis(), nombre.trim().take(60), saldo, tasa, minimo, if (buena) "buena" else "mala")) }
    fun deudaCambiar(id: String, cambio: (com.rutaalacima.app.domain.model.Deudas.Deuda) -> com.rutaalacima.app.domain.model.Deudas.Deuda) =
        deudas { d -> d.copy(lista = d.lista.map { if (it.id == id) cambio(it) else it }) }
    fun deudaQuitar(id: String) = deudas { d -> d.copy(lista = d.lista.filter { it.id != id }) }
    fun deudaRestaurar(x: com.rutaalacima.app.domain.model.Deudas.Deuda) = deudas { it.copy(lista = it.lista + x) }
    fun deudaMetodo(m: String) = deudas { it.copy(metodo = m) }
    fun deudaExtra(v: Double) = deudas { it.copy(extra = v) }
    /** Un pago: gasto de hoy en "Pago de deudas" que nombra la deuda (así baja su saldo). */
    fun deudaPagar(x: com.rutaalacima.app.domain.model.Deudas.Deuda, monto: Double, nota: String) = viewModelScope.launch {
        val f = hoy().toString()
        guardar(f, guardados(f) + Mov("m" + System.currentTimeMillis(), "gas", monto, "deudas", nota, d = x.id))
    }

    // ---------------------------------------------------------------- Activos
    private fun activos(cambio: (List<com.rutaalacima.app.domain.model.Patrimonio.Activo>) -> List<com.rutaalacima.app.domain.model.Patrimonio.Activo>) =
        poner(com.rutaalacima.app.domain.model.Patrimonio.CLAVE, com.rutaalacima.app.domain.model.Patrimonio.escribir(cambio(libro.value.activos)))
    fun activoAgregar(nombre: String, tipo: String, valor: Double) = activos { it + com.rutaalacima.app.domain.model.Patrimonio.Activo("a" + System.currentTimeMillis(), nombre.trim().take(60), tipo, valor) }
    fun activoCambiar(x: com.rutaalacima.app.domain.model.Patrimonio.Activo) = activos { l -> l.map { if (it.id == x.id) x else it } }
    fun activoQuitar(id: String) = activos { l -> l.filter { it.id != id } }

    // ---------------------------------------------------------------- Privacidad
    fun visibilidad(publica: Boolean) = poner("fin#visibilidad", if (publica) "publica" else "")
    fun bloqueo(si: Boolean) = poner("fin#bloqueo", if (si) "1" else "")
    fun ocultar(si: Boolean) = poner("fin#ocultar", if (si) "1" else "")
    /** Hasta cuándo queda abierto el módulo tras la huella (5 minutos o hasta salir de la app). */
    var abiertoHasta by mutableStateOf(0L)
    val abierto get() = System.currentTimeMillis() < abiertoHasta
    var oculto by mutableStateOf(false)

    // ---------------------------------------------------------------- Recibos
    private val dirRecibos get() = java.io.File(c.contexto.filesDir, "recibos").apply { mkdirs() }
    /** Guarda el recibo: en el depósito privado de la cuenta si hay sesión; si no, en el teléfono. */
    fun agregarRecibo(f: String, x: Mov, uri: android.net.Uri, alTerminar: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = runCatching {
            val cr = c.contexto.contentResolver
            val tipo = cr.getType(uri) ?: "image/jpeg"
            require(tipo.startsWith("image/") || tipo == "application/pdf")
            val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { cr.openInputStream(uri)!!.use { it.readBytes() } }
            require(bytes.size <= 15_000_000)
            val nom = cr.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "recibo"
            val id = "r" + System.currentTimeMillis()
            val ext = if (tipo == "application/pdf") "pdf" else tipo.substringAfter("/").replace("jpeg", "jpg")
            val sesion = c.supabase.sesion.value
            val ruta = if (sesion != null && c.supabase.configurado) "${sesion.userId}/$id.$ext".also { c.supabase.subirPrivado("recibos", it, bytes, tipo) }
                else "local:$id.$ext".also { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { java.io.File(dirRecibos, it.removePrefix("local:")).writeBytes(bytes) } }
            guardar(f, guardados(f).map { if (it.id == x.id) it.copy(r = it.r + Cuentas.Recibo(id, nom.take(80), tipo, ruta)) else it })
        }.isSuccess
        alTerminar(ok)
    }
    fun quitarRecibo(f: String, x: Mov, r: Cuentas.Recibo) = viewModelScope.launch {
        guardar(f, guardados(f).map { if (it.id == x.id) it.copy(r = it.r.filter { y -> y.id != r.id }) else it })
        if (r.ruta.startsWith("local:")) java.io.File(dirRecibos, r.ruta.removePrefix("local:")).delete() else c.supabase.borrarArchivo("recibos", r.ruta)
    }
    /** El archivo del recibo en el teléfono (lo descarga si está en la nube). */
    suspend fun archivoRecibo(r: Cuentas.Recibo): java.io.File? = runCatching {
        if (r.ruta.startsWith("local:")) java.io.File(dirRecibos, r.ruta.removePrefix("local:")).takeIf { it.exists() }
        else java.io.File(c.contexto.cacheDir, "recibo-" + r.ruta.substringAfterLast('/')).also { f ->
            if (!f.exists()) { val b = c.supabase.descargarPrivado("recibos", r.ruta); kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { f.writeBytes(b) } }
        }
    }.getOrNull()

    private var guardadoPres: kotlinx.coroutines.Job? = null
    fun presupuesto(ym: YearMonth, v: String) {
        guardadoPres?.cancel()
        guardadoPres = c.appScope.launch { kotlinx.coroutines.delay(500); c.respuestas.guardar(Finanzas.WB, Finanzas.clavePresupuesto(ym.year, ym.monthValue), v.trim()) }
    }
}

// ====================================================================== Niveles

/** Dónde estás en el libro: el balance de todos los años, un año, un mes o un día. */
sealed interface NivelCuentas {
    data object Balance : NivelCuentas
    data class Anio(val a: Int) : NivelCuentas
    data class Mes(val ym: YearMonth) : NivelCuentas
    data class Dia(val f: LocalDate) : NivelCuentas
    data object DeudasN : NivelCuentas
    data object PatrimonioN : NivelCuentas
    data class SaludN(val a: Int?) : NivelCuentas

    fun codigo(): String = when (this) { Balance -> "b"; is Anio -> "a:$a"; is Mes -> "m:$ym"; is Dia -> "d:$f"; DeudasN -> "deudas"; PatrimonioN -> "patrimonio"; is SaludN -> if (a == null) "salud" else "salud:$a" }
    fun arriba(): NivelCuentas? = when (this) { Balance -> null; is Anio -> Balance; is Mes -> Anio(ym.year); is Dia -> Mes(YearMonth.from(f)); DeudasN, PatrimonioN, is SaludN -> Balance }

    companion object {
        fun de(c: String?): NivelCuentas = runCatching {
            val (t, v) = (c ?: "b").split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            when (t) { "a" -> Anio(v.toInt()); "m" -> Mes(YearMonth.parse(v)); "d" -> Dia(LocalDate.parse(v)); "deudas" -> DeudasN; "patrimonio" -> PatrimonioN; "salud" -> SaludN(v.toIntOrNull()); else -> Balance }
        }.getOrDefault(Balance)
    }
}

// ====================================================================== Colores y textos

/** Un color por tipo de movimiento y por mochila (iguales a los de la web). */
@Composable
fun colorTipo(t: Tipo): Color {
    val oscuro = MaterialTheme.colorScheme.background.luminanciaBaja()
    return when (t) {
        Tipo.ING -> if (oscuro) Color(0xFF8CC796) else Color(0xFF2F6B4A)
        Tipo.GAS -> MaterialTheme.colorScheme.primary
        Tipo.AHO -> if (oscuro) Color(0xFF9CC3E0) else Color(0xFF2F5D7C)
        Tipo.INV -> if (oscuro) Color(0xFFC9AEDB) else Color(0xFF6A4A7E)
    }
}

@Composable
fun colorMochila(m: Mochila): Color {
    val oscuro = MaterialTheme.colorScheme.background.luminanciaBaja()
    return when (m) {
        Mochila.VERDE -> if (oscuro) Color(0xFF86BC93) else Color(0xFF4C7A5A)
        Mochila.AMARILLA -> if (oscuro) Color(0xFFD9B871) else Color(0xFFB08A3E)
        Mochila.ROJA -> MaterialTheme.colorScheme.primary
    }
}

private fun Color.luminanciaBaja() = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

@Composable fun nombreTipo(t: Tipo, plural: Boolean = true): String = stringResource(
    when (t) { Tipo.ING -> if (plural) R.string.cu_ing else R.string.cu_ing1; Tipo.GAS -> if (plural) R.string.cu_gas else R.string.cu_gas1
        Tipo.AHO -> if (plural) R.string.cu_aho else R.string.cu_aho1; Tipo.INV -> if (plural) R.string.cu_inv else R.string.cu_inv1 })

/** Nombre de una categoría en el idioma de la app. */
/** Las categorías creadas por la persona (para sus nombres) y el ViewModel del libro (para crear más). */
val LocalPropias = androidx.compose.runtime.staticCompositionLocalOf<Map<String, List<Cuentas.Propia>>> { emptyMap() }
val LocalCuentas = androidx.compose.runtime.staticCompositionLocalOf<CuentasViewModel?> { null }

@Composable
fun nombreCategoria(t: Tipo, c: String): String {
    if (c.startsWith("u_")) LocalPropias.current[t.codigo]?.firstOrNull { it.c == c }?.let { return it.n }
    val ctx = LocalContext.current
    val id = ctx.resources.getIdentifier("cu_cat_${t.codigo}_$c", "string", ctx.packageName)
    return if (id != 0) stringResource(id) else c
}

@Composable private fun nombreMochila(m: Mochila) = stringResource(when (m) { Mochila.VERDE -> R.string.cu_m_verde; Mochila.AMARILLA -> R.string.cu_m_amarilla; Mochila.ROJA -> R.string.cu_m_roja })
@Composable private fun descMochila(m: Mochila) = stringResource(when (m) { Mochila.VERDE -> R.string.cu_m_verde_d; Mochila.AMARILLA -> R.string.cu_m_amarilla_d; Mochila.ROJA -> R.string.cu_m_roja_d })

/** Mes en una frase: en minúscula en los idiomas que así lo escriben. */
private fun mesFrase(m: Int) = com.rutaalacima.app.ui.ruta.mesEnFrase(m)

// ====================================================================== Pantalla

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AscensoFinancieroScreen(inicial: String?, onBack: () -> Unit, onAprende: () -> Unit) {
    val vm = rutaViewModel { CuentasViewModel(it) }
    val libro by vm.libro.collectAsStateWithLifecycle()
    var codigo by rememberSaveable { mutableStateOf(inicial ?: "b") }
    val nivel = NivelCuentas.de(codigo)
    val ir: (NivelCuentas) -> Unit = { codigo = it.codigo() }
    val avisos = remember { SnackbarHostState() }
    BackHandler { nivel.arriba()?.let(ir) ?: onBack() }
    val plata: (Double) -> String = { if (vm.oculto) "•••••" else Cuentas.formato(it, libro.moneda) }
    // Se vuelve a cerrar al salir de la app
    val ciclo = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(ciclo) {
        val o = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) vm.abiertoHasta = 0L }
        ciclo.lifecycle.addObserver(o); onDispose { ciclo.lifecycle.removeObserver(o) }
    }
    val bloqueado = libro.bloqueo && !vm.abierto

    androidx.compose.runtime.CompositionLocalProvider(LocalPropias provides libro.propias, LocalCuentas provides vm) {
    Scaffold(
        modifier = Modifier.fondoPapel(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.cu_titulo)) },
                navigationIcon = { IconButton(onClick = { nivel.arriba()?.let(ir) ?: onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.volver)) } },
                actions = {
                    if (!bloqueado) IconButton(onClick = { vm.oculto = !vm.oculto }) {
                        Icon(if (vm.oculto) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, stringResource(if (vm.oculto) R.string.cu_mostrar_montos else R.string.cu_ocultar_montos))
                    }
                },
            )
        },
    ) { padding ->
        if (bloqueado) { PantallaBloqueo(Modifier.padding(padding)) { vm.abiertoHasta = System.currentTimeMillis() + 5 * 60_000 }; return@Scaffold }
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            Migas(nivel, ir)
            AnimatedContent(nivel, label = "nivel", transitionSpec = { fadeIn() togetherWith fadeOut() }) { n ->
                LazyColumn(
                    Modifier.fillMaxSize().imePadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = padding.calculateBottomPadding() + 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (n) {
                        NivelCuentas.Balance -> balance(libro, plata, ir, vm, onAprende)
                        is NivelCuentas.Anio -> anio(n.a, libro, plata, ir)
                        is NivelCuentas.Mes -> mes(n.ym, libro, plata, ir, vm)
                        is NivelCuentas.Dia -> dia(n.f, libro, plata, ir, vm, avisos)
                        NivelCuentas.DeudasN -> deudasNivel(libro, plata, vm, avisos)
                        NivelCuentas.PatrimonioN -> patrimonioNivel(libro, plata, ir, vm)
                        is NivelCuentas.SaludN -> saludNivel(n.a, libro, plata, ir)
                    }
                }
            }
        }
    }
    }
}

/** Migas de pan: Balance general › 2026 › Octubre › 24. Cada una sube a su nivel. */
@Composable
private fun Migas(n: NivelCuentas, ir: (NivelCuentas) -> Unit) {
    val partes = buildList {
        add(stringResource(R.string.cu_balance) to NivelCuentas.Balance)
        when (n) {
            is NivelCuentas.Anio -> add("${n.a}" to n)
            is NivelCuentas.Mes -> { add("${n.ym.year}" to NivelCuentas.Anio(n.ym.year)); add(nombreMes(n.ym.monthValue) to n) }
            is NivelCuentas.Dia -> { add("${n.f.year}" to NivelCuentas.Anio(n.f.year)); add(nombreMes(n.f.monthValue) to NivelCuentas.Mes(YearMonth.from(n.f))); add("${n.f.dayOfMonth}" to n) }
            NivelCuentas.DeudasN -> add(stringResource(R.string.cu_deudas) to n)
            NivelCuentas.PatrimonioN -> add(stringResource(R.string.cu_patrimonio_titulo) to n)
            is NivelCuentas.SaludN -> add(stringResource(R.string.cu_salud) to n)
            else -> {}
        }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        partes.forEachIndexed { i, (t, destino) ->
            if (i < partes.lastIndex) {
                TextButton(onClick = { ir(destino) }) { Text(t) }
                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else Text(t, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp))
        }
    }
}

/** ‹ 2026 › para moverse entre periodos del mismo nivel. */
@Composable
private fun NavPeriodo(titulo: String, onAntes: () -> Unit, onDespues: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onAntes) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.cu_anterior)) }
        Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).semantics { heading() })
        IconButton(onClick = onDespues) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.siguiente)) }
    }
}

// ---------------------------------------------------------------- Piezas

/** Las cuatro cifras y lo disponible. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CifrasCuentas(s: Cuentas.Suma, plata: (Double) -> String) {
    val items = Tipo.entries.map { Triple(nombreTipo(it), s.de(it), colorTipo(it)) } + Triple(stringResource(R.string.cu_disp), s.disponible, null)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
        items.forEachIndexed { i, (t, v, color) ->
            val disp = color == null
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                .background(if (disp) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color ?: MaterialTheme.colorScheme.onSurfaceVariant))
                    Spacer(Modifier.width(6.dp))
                    Text(t, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(plata(v), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (v < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
            if (i == items.lastIndex && items.size % 2 == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** Un periodo de la tabla: su etiqueta, su suma y a dónde lleva al tocarlo. */
data class FilaPeriodo(val etiqueta: String, val corta: String, val suma: Cuentas.Suma, val destino: NivelCuentas, val actual: Boolean = false)

private val ANCHO_ETQ = 88.dp
private val ANCHO_NUM = 116.dp

/**
 * Tabla tipo hoja de cálculo: la primera columna queda fija y las cifras se deslizan de lado juntas;
 * cada fila baja a su periodo y la columna Acumulado suma de arriba hacia abajo.
 */
@Composable
fun TablaPeriodos(filas: List<FilaPeriodo>, columna: String, total: Cuentas.Suma, plata: (Double) -> String, onFila: (NivelCuentas) -> Unit) {
    val desliza = rememberScrollState()
    val cols = Tipo.entries.map { nombreTipo(it) } + stringResource(R.string.cu_disp) + stringResource(R.string.cu_acum)
    val linea = MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        FilaTabla(columna, cols, desliza, cabecera = true)
        HorizontalDivider(color = linea)
        var acum = 0.0
        filas.forEach { f ->
            acum += f.suma.disponible
            val valores = Tipo.entries.map { f.suma.de(it) } + f.suma.disponible + acum
            FilaTabla(f.etiqueta, valores.mapIndexed { i, v -> if (v == 0.0 && (i < 5 || f.suma.n == 0)) "—" else plata(v) }, desliza,
                negativos = valores.map { it < 0 }, actual = f.actual, vacia = f.suma.n == 0, onClick = { onFila(f.destino) })
            HorizontalDivider(color = linea.copy(alpha = 0.6f))
        }
        val tv = Tipo.entries.map { total.de(it) } + total.disponible
        FilaTabla(stringResource(R.string.cu_total), tv.map { if (it == 0.0) "—" else plata(it) } + "", desliza, negativos = tv.map { it < 0 } + false, total = true)
    }
}

@Composable
private fun FilaTabla(etq: String, valores: List<String>, desliza: ScrollState, cabecera: Boolean = false, total: Boolean = false, actual: Boolean = false,
                      vacia: Boolean = false, negativos: List<Boolean> = emptyList(), onClick: (() -> Unit)? = null) {
    val estilo = if (cabecera) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val peso = if (total) FontWeight.SemiBold else FontWeight.Normal
    val fondo = if (cabecera || total) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent
    Row(
        Modifier.fillMaxWidth().background(fondo).heightIn(min = 44.dp)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = etq, onClick = onClick).semantics { role = Role.Button } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etq, Modifier.width(ANCHO_ETQ).padding(horizontal = 12.dp), style = estilo, fontWeight = if (actual) FontWeight.Bold else peso, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = when { cabecera -> MaterialTheme.colorScheme.onSurfaceVariant; actual -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurface })
        Row(Modifier.weight(1f).horizontalScroll(desliza)) {
            valores.forEachIndexed { i, v ->
                Text(v, Modifier.width(ANCHO_NUM).padding(horizontal = 10.dp), style = estilo, fontWeight = peso, textAlign = TextAlign.End, maxLines = 1,
                    color = when {
                        cabecera -> MaterialTheme.colorScheme.onSurfaceVariant
                        negativos.getOrElse(i) { false } -> MaterialTheme.colorScheme.error
                        vacia || v == "—" || i == 5 -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    })
            }
        }
    }
}

/** Barras de lo que entró y lo que salió por periodo, a escala; toca una para bajar. */
@Composable
fun GraficaPeriodos(filas: List<FilaPeriodo>, plata: (Double) -> String, onFila: (NivelCuentas) -> Unit) {
    if (filas.none { it.suma.n > 0 }) return
    val ing = colorTipo(Tipo.ING); val sal = colorTipo(Tipo.GAS); val guia = MaterialTheme.colorScheme.outlineVariant
    val max = filas.maxOf { maxOf(it.suma.ing, it.suma.gas + it.suma.aho + it.suma.inv) }.coerceAtLeast(1.0)
    val desc = filas.filter { it.suma.n > 0 }.joinToString { "${it.etiqueta}: ${plata(it.suma.ing)} / ${plata(it.suma.gas + it.suma.aho + it.suma.inv)}" }
    Column {
        Canvas(Modifier.fillMaxWidth().height(130.dp).semantics { contentDescription = desc }.pointerInput(filas) {
            detectTapGestures { p -> val i = (p.x / size.width * filas.size).toInt().coerceIn(0, filas.lastIndex); onFila(filas[i].destino) }
        }) {
            val w = size.width / filas.size
            val bw = (w * 0.32f).coerceAtMost(18.dp.toPx())
            filas.forEachIndexed { i, f ->
                val cx = w * i + w / 2
                val hi = (f.suma.ing / max * size.height).toFloat()
                val hs = ((f.suma.gas + f.suma.aho + f.suma.inv) / max * size.height).toFloat()
                if (hi > 0) drawRoundRect(ing, Offset(cx - bw - 1, size.height - hi), Size(bw, hi), CornerRadius(3f, 3f))
                if (hs > 0) drawRoundRect(sal, Offset(cx + 1, size.height - hs), Size(bw, hs), CornerRadius(3f, 3f))
            }
            drawLine(guia, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        }
        Row(Modifier.fillMaxWidth()) {
            filas.forEach { Text(it.corta, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Leyenda(ing, stringResource(R.string.cu_entro)); Leyenda(sal, stringResource(R.string.cu_salio))
        }
    }
}

@Composable
private fun Leyenda(c: Color, t: String) = Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(c)); Spacer(Modifier.width(6.dp))
    Text(t, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Las tres mochilas del libro Cima financiera frente a lo que entró. */
@Composable
fun MochilasCard(s: Cuentas.Suma, plata: (Double) -> String) {
    val base = (if (s.ing > 0) s.ing else s.mochilas.values.sum()).coerceAtLeast(1.0)
    RutaCard {
        Text(stringResource(R.string.cu_mochilas), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.cu_mochilas_ayuda, 50, 30, 20), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        val libre = (s.ing - s.gas - s.construido).coerceAtLeast(0.0)
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Mochila.entries.forEach { m -> val v = s.mochilas[m] ?: 0.0; if (v > 0) Box(Modifier.weight(v.toFloat()).fillMaxHeight().background(colorMochila(m))) }
            if (libre > 0) Spacer(Modifier.weight(libre.toFloat()))
            if (s.mochilas.values.sum() == 0.0 && libre == 0.0) Spacer(Modifier.weight(1f))
        }
        Mochila.entries.forEach { m ->
            val v = s.mochilas[m] ?: 0.0; val p = v / base
            val bien = if (m == Mochila.ROJA) p >= m.ideal else p <= m.ideal + 0.05
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(colorMochila(m)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(nombreMochila(m), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(descMochila(m) + " · " + stringResource(if (m == Mochila.ROJA) R.string.cu_ideal_min else R.string.cu_ideal, (m.ideal * 100).toInt()),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(plata(v), style = MaterialTheme.typography.bodyMedium)
                Text(if (s.ing > 0) "${Math.round(p * 100)} %" else "—", Modifier.width(56.dp), textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold,
                    color = when { s.ing <= 0 -> MaterialTheme.colorScheme.onSurfaceVariant; bien -> colorTipo(Tipo.ING); else -> MaterialTheme.colorScheme.error })
            }
        }
    }
}

/** ¿A dónde fue tu dinero? Las categorías de mayor a menor. */
@Composable
fun CategoriasCard(s: Cuentas.Suma, plata: (Double) -> String) {
    val l = s.categorias.filterKeys { it.first != Tipo.ING }.entries.sortedByDescending { it.value }.take(8)
    if (l.isEmpty()) {
        RutaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cu_a_donde), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                BotonCrearCategoria()
            }
            Text(stringResource(R.string.cu_cats_vacio), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val max = l.first().value
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cu_a_donde), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            BotonCrearCategoria()
        }
        l.forEach { (k, v) ->
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(nombreCategoria(k.first, k.second), Modifier.width(130.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(Modifier.fillMaxWidth((v / max).toFloat()).fillMaxHeight().background(colorTipo(k.first)))
                }
                Text(plata(v), Modifier.width(104.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonedaCard(actual: String, onElegir: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val loc = Locale.getDefault()
    fun nombre(c: String) = runCatching { java.util.Currency.getInstance(c).getDisplayName(loc) }.getOrDefault(c)
    RutaCard {
        Text(stringResource(R.string.cu_moneda), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.cu_moneda_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
            OutlinedTextField("$actual · ${nombre(actual)}", {}, readOnly = true, singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(abierto) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
            ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                (listOf(actual) + Cuentas.MONEDAS).distinct().forEach { c ->
                    DropdownMenuItem(text = { Text("$c · ${nombre(c)}") }, onClick = { onElegir(c); abierto = false })
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Niveles

private fun LazyListScope.balance(libro: Libro, plata: (Double) -> String, ir: (NivelCuentas) -> Unit, vm: CuentasViewModel, onAprende: () -> Unit) {
    val t = libro.total
    val filas = libro.anios.map { a -> FilaPeriodo("$a", "$a", libro.suma("$a-"), NivelCuentas.Anio(a), a == hoy().year) }
    if (t.n == 0) item(key = "vacio") {
        RutaCard {
            Text(stringResource(R.string.cu_vacio), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { ir(NivelCuentas.Dia(hoy())) }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.cu_registrar_hoy)) }
        }
    }
    item(key = "atajos") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { ir(NivelCuentas.Dia(hoy())) }, Modifier.weight(1f)) { Text(stringResource(R.string.cu_hoy), maxLines = 1) }
            FilledTonalButton(onClick = { ir(NivelCuentas.Mes(YearMonth.from(hoy()))) }, Modifier.weight(1f)) { Text(nombreMes(hoy().monthValue), maxLines = 1) }
        }
    }
    item(key = "salud") { SaludAcceso(libro.salud(null), null) { ir(NivelCuentas.SaludN(null)) } }
    item(key = "cifras") { CifrasCuentas(t, plata) }
    item(key = "grafica") { GraficaAscensoCard(libro, plata) }
    item(key = "tabla") {
        RutaCard {
            Text(stringResource(R.string.cu_anio_por_anio), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.cu_toca_anio), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            GraficaPeriodos(filas, plata, ir)
            Spacer(Modifier.height(10.dp))
            TablaPeriodos(filas, stringResource(R.string.cu_col_anio), t, plata, ir)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.cu_patrimonio, plata(t.construido)) + (t.tasa?.let { stringResource(R.string.cu_patrimonio_tasa, Math.round(it * 100).toInt()) } ?: ""),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    item(key = "deudas") { DeudasResumen(libro, plata) { ir(NivelCuentas.DeudasN) } }
    item(key = "fijos") { FijosCard(libro, null, plata, vm) }
    item(key = "mochilas") { MochilasCard(t, plata) }
    item(key = "cats") { CategoriasCard(t, plata) }
    item(key = "privacidad") { PrivacidadCard(libro, vm) }
    item(key = "moneda") { MonedaCard(libro.moneda) { vm.elegirMoneda(it) } }
    item(key = "aprende") {
        RutaCard(onClick = onAprende) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.cu_aprende), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.cu_aprende_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
        }
    }
}

private fun LazyListScope.anio(a: Int, libro: Libro, plata: (Double) -> String, ir: (NivelCuentas) -> Unit) {
    val actual = YearMonth.from(hoy())
    val filas = (1..12).map { m -> val ym = YearMonth.of(a, m)
        FilaPeriodo(nombreMes(m), nombreMes(m).take(3), libro.suma("$ym-"), NivelCuentas.Mes(ym), ym == actual) }
    val t = libro.suma("$a-")
    item(key = "nav") { NavPeriodo("$a", { ir(NivelCuentas.Anio(a - 1)) }, { ir(NivelCuentas.Anio(a + 1)) }) }
    item(key = "cifras") { CifrasCuentas(t, plata) }
    item(key = "tabla") {
        RutaCard {
            Text(stringResource(R.string.cu_mes_a_mes), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.cu_toca_mes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            GraficaPeriodos(filas, plata, ir)
            Spacer(Modifier.height(10.dp))
            TablaPeriodos(filas, stringResource(R.string.cu_col_mes), t, plata, ir)
        }
    }
    item(key = "salud-$a") { SaludAcceso(libro.salud(a), a) { ir(NivelCuentas.SaludN(a)) } }
    item(key = "mochilas") { MochilasCard(t, plata) }
    item(key = "cats") { CategoriasCard(t, plata) }
}

private fun LazyListScope.mes(ym: YearMonth, libro: Libro, plata: (Double) -> String, ir: (NivelCuentas) -> Unit, vm: CuentasViewModel) {
    val hoyF = hoy()
    val fechas = (libro.dias.keys.filter { it.startsWith("$ym-") } + if (YearMonth.from(hoyF) == ym) listOf(hoyF.toString()) else emptyList()).distinct().sorted()
    val filas = fechas.map { f -> val d = LocalDate.parse(f)
        FilaPeriodo("${d.dayOfMonth} · ${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}", "${d.dayOfMonth}", Cuentas.sumar(libro.movs(f)), NivelCuentas.Dia(d), d == hoyF) }
    val t = libro.suma("$ym-")
    item(key = "nav") { NavPeriodo("${nombreMes(ym.monthValue)} ${ym.year}", { ir(NivelCuentas.Mes(ym.minusMonths(1))) }, { ir(NivelCuentas.Mes(ym.plusMonths(1))) }) }
    item(key = "cifras") { CifrasCuentas(t, plata) }
    item(key = "pres") {
        val guardado = libro.presupuesto(ym)
        var texto by remember(ym) { mutableStateOf(guardado) }
        val pres = kotlin.math.abs(Finanzas.monto(texto))
        RutaCard {
            Text(stringResource(R.string.cu_presupuesto), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(texto, { texto = it; vm.presupuesto(ym, it) }, label = { Text(stringResource(R.string.cu_presupuesto_campo)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                placeholder = { Text(plata(1_500_000.0)) })
            if (pres > 0) {
                val uso = t.gas / pres
                com.rutaalacima.app.ui.components.ProgressLine(uso.toFloat().coerceAtMost(1f), Modifier.padding(top = 10.dp),
                    color = if (uso > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.fin_gastaste, plata(t.gas), plata(pres)) + " · " +
                    if (uso > 1) stringResource(R.string.fin_te_pasaste, plata(t.gas - pres)) else stringResource(R.string.cu_te_quedan, plata(pres - t.gas)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    item(key = "tabla") {
        RutaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.cu_dia_a_dia), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.cu_toca_dia), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = { ir(NivelCuentas.Dia(if (YearMonth.from(hoyF) == ym) hoyF else ym.atDay(1))) }) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.cu_registrar))
                }
            }
            Spacer(Modifier.height(10.dp))
            if (filas.isEmpty()) Text(stringResource(R.string.cu_sin_movs_mes, mesFrase(ym.monthValue)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else {
                GraficaPeriodos(filas, plata, ir)
                Spacer(Modifier.height(10.dp))
                TablaPeriodos(filas, stringResource(R.string.cu_col_dia), t, plata, ir)
            }
        }
    }
    item(key = "fijos") { FijosCard(libro, ym, plata, vm) }
    item(key = "mochilas") { MochilasCard(t, plata) }
    item(key = "cats") { CategoriasCard(t, plata) }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.dia(f: LocalDate, libro: Libro, plata: (Double) -> String, ir: (NivelCuentas) -> Unit, vm: CuentasViewModel, avisos: SnackbarHostState) {
    val fs = f.toString(); val movs = libro.movs(fs); val s = Cuentas.sumar(movs); val ym = YearMonth.from(f); val mesS = libro.suma("$ym-")
    item(key = "nav") { NavPeriodo("${f.dayOfMonth} ${nombreMes(f.monthValue)} ${f.year}", { ir(NivelCuentas.Dia(f.minusDays(1))) }, { ir(NivelCuentas.Dia(f.plusDays(1))) }) }
    item(key = "nuevo") { NuevoMovimiento(plata, f.dayOfMonth) { t, m, c, n, rep -> vm.agregar(fs, t, m, c, n, rep) } }
    item(key = "movs-tit") { SectionTitle(stringResource(R.string.cu_movimientos, movs.size)) }
    if (movs.isEmpty()) item(key = "movs-vacio") {
        Text(stringResource(R.string.cu_sin_movs_dia), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(movs, key = { "mov-${it.id}" }) { x -> FilaMovimiento(x, fs, plata, vm, avisos) }
    if (movs.isNotEmpty()) item(key = "disp") {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp)) {
            Text(stringResource(R.string.cu_disp_dia), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(plata(s.disponible), fontWeight = FontWeight.SemiBold, color = if (s.disponible < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
    }
    item(key = "cifras") { CifrasCuentas(s, plata) }
    item(key = "sube") {
        RutaCard(onClick = { ir(NivelCuentas.Mes(ym)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.cu_asi_va, mesFrase(f.monthValue)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.cu_disponible_x, plata(mesS.disponible)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        color = if (mesS.disponible < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.cu_mes_resumen, plata(mesS.ing), plata(mesS.gas + mesS.aho + mesS.inv)), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
        }
    }
}

/** Anotar un movimiento: tipo, monto, categoría y concepto. El monto y la categoría bastan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoMovimiento(plata: (Double) -> String, dia: Int, onAgregar: (Tipo, Double, String, String, Boolean) -> Unit) {
    var repite by rememberSaveable { mutableStateOf(false) }
    var tipo by rememberSaveable { mutableStateOf(Tipo.GAS) }
    var monto by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable(tipo) { mutableStateOf(Cuentas.CATEGORIAS.getValue(tipo).first().first) }
    var nota by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val agregar = {
        val v = kotlin.math.abs(Finanzas.monto(monto))
        if (v == 0.0) error = true
        else {
            onAgregar(tipo, v, cat, nota, repite)
            android.widget.Toast.makeText(ctx, if (repite) ctx.getString(R.string.cu_fijo_quedo, plata(v), dia) else ctx.getString(R.string.cu_anotado, ctx.getString(when (tipo) {
                Tipo.ING -> R.string.cu_ing1; Tipo.GAS -> R.string.cu_gas1; Tipo.AHO -> R.string.cu_aho1; Tipo.INV -> R.string.cu_inv1 }), plata(v)), android.widget.Toast.LENGTH_SHORT).show()
            monto = ""; nota = ""; error = false; repite = false
        }
    }
    RutaCard {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Tipo.entries.forEachIndexed { i, t ->
                SegmentedButton(selected = tipo == t, onClick = { tipo = t }, shape = SegmentedButtonDefaults.itemShape(i, Tipo.entries.size),
                    colors = SegmentedButtonDefaults.colors(activeContainerColor = colorTipo(t).copy(alpha = 0.16f), activeContentColor = colorTipo(t)),
                    icon = {}) { Text(nombreTipo(t, plural = false), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(monto, { monto = it; error = false }, label = { Text(stringResource(R.string.cu_monto)) }, singleLine = true, isError = error,
                supportingText = if (error) { { Text(stringResource(R.string.cu_escribe_monto)) } } else null,
                placeholder = { Text(plata(0.0)) }, textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { agregar() }),
                modifier = Modifier.weight(1f))
            SelectorCategoria(tipo, cat, Modifier.weight(1.2f)) { cat = it }
        }
        OutlinedTextField(nota, { nota = it.take(120) }, label = { Text(stringResource(R.string.cu_concepto)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { agregar() }),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Row(Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(8.dp))
            .toggleable(repite, role = androidx.compose.ui.semantics.Role.Checkbox) { repite = it }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Checkbox(repite, null)
            Icon(Icons.Filled.Repeat, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Column {
                Text(stringResource(R.string.cu_repite_dia, dia), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.cu_repite_ej), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = agregar, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text(stringResource(R.string.cu_agregar)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorCategoria(tipo: Tipo, actual: String, modifier: Modifier = Modifier, onElegir: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    var creando by remember { mutableStateOf(false) }
    if (creando) NuevaCategoriaDialogo(tipo, onCerrar = { creando = false }) { onElegir(it); creando = false }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }, modifier = modifier) {
        OutlinedTextField(nombreCategoria(tipo, actual), {}, readOnly = true, singleLine = true, label = { Text(stringResource(R.string.cu_categoria)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(abierto) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Cuentas.categorias(tipo, LocalPropias.current).forEach { (c, m) ->
                val mo = m?.takeIf { tipo == Tipo.GAS }
                val punto = if (mo != null) colorMochila(mo) else null
                DropdownMenuItem(text = { Text(nombreCategoria(tipo, c)) }, onClick = { onElegir(c); abierto = false },
                    leadingIcon = if (punto != null) { { Box(Modifier.size(10.dp).clip(CircleShape).background(punto)) } } else null)
            }
            val vmC = LocalCuentas.current
            if (vmC != null) DropdownMenuItem(text = { Text(stringResource(R.string.cu_nueva_categoria)) }, onClick = { abierto = false; creando = true },
                leadingIcon = { Icon(Icons.Filled.Add, null) })
        }
    }
}

/** Un movimiento: tipo, categoría, concepto y monto; tocarlo lo edita, el bote lo quita (con deshacer). */
@Composable
private fun FilaMovimiento(x: Mov, fecha: String, plata: (Double) -> String, vm: CuentasViewModel, avisos: SnackbarHostState) {
    val t = Tipo.de(x.t)
    var editar by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()
    val quitado = stringResource(if (x.fx != null) R.string.cu_fijo_saltado else R.string.cu_quitado); val deshacer = stringResource(R.string.cu_deshacer)
    val nota = x.n.ifBlank { if (x.agenda != null) stringResource(R.string.cu_registro_dia) else "" }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClickLabel = stringResource(R.string.cu_editar_mov)) { editar = true }.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(nombreTipo(t, plural = false), Modifier.clip(RoundedCornerShape(50)).background(colorTipo(t).copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium, color = colorTipo(t))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(nombreCategoria(t, x.c), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (nota.isNotBlank()) Text(nota, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (x.fx != null) Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Repeat, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(4.dp))
                Text(stringResource(if (x.virtual) R.string.cu_fijo_marca else R.string.cu_fijo_cambiado), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (x.r.isNotEmpty()) Icon(Icons.Filled.AttachFile, stringResource(R.string.cu_recibos_n, x.r.size), Modifier.size(18.dp).padding(end = 2.dp), tint = MaterialTheme.colorScheme.primary)
        Text(plata(x.m), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = {
            vm.borrar(fecha, x)
            alcance.launch { if (avisos.showSnackbar(quitado, deshacer, withDismissAction = true) == SnackbarResult.ActionPerformed) vm.deshacerBorrado(fecha, x) }
        }) { Icon(Icons.Filled.Delete, stringResource(R.string.cu_quitar), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (editar) EditarMovimiento(x, fecha, plata, vm, onCerrar = { editar = false }) { nuevo -> vm.editar(fecha, x, nuevo); editar = false }
}

@Composable
private fun EditarMovimiento(x: Mov, fecha: String, plata: (Double) -> String, vm: CuentasViewModel, onCerrar: () -> Unit, onGuardar: (Mov) -> Unit) {
    val t = Tipo.de(x.t)
    var monto by remember { mutableStateOf(Cuentas.numero(x.m)) }
    var cat by remember { mutableStateOf(x.c) }
    var nota by remember { mutableStateOf(x.n) }
    val v = kotlin.math.abs(Finanzas.monto(monto))
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.cu_editar_mov)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(monto, { monto = it }, label = { Text(stringResource(R.string.cu_monto)) }, singleLine = true, isError = v == 0.0,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), supportingText = { Text(plata(v)) }, modifier = Modifier.fillMaxWidth())
                SelectorCategoria(t, cat, Modifier.fillMaxWidth()) { cat = it }
                OutlinedTextField(nota, { nota = it.take(120) }, label = { Text(stringResource(R.string.cu_concepto)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (x.agenda == null && !x.virtual) RecibosMovimiento(x, fecha, vm)
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(x.copy(m = v, c = cat, n = nota.trim())) }, enabled = v > 0) { Text(stringResource(R.string.guardar)) } },
        dismissButton = { TextButton(onClick = onCerrar) { Text(stringResource(R.string.cancelar)) } },
    )
}

// ====================================================================== Para otras pantallas

/** Abre Ascenso financiero en un nivel ("d:2026-10-24", "m:2026-10"…) desde cualquier pantalla. */
@Composable
fun irACuentas(): (String) -> Unit {
    val app = LocalContext.current.applicationContext as com.rutaalacima.app.RutaApp
    return { nivel -> app.container.navegacionPendiente.value = com.rutaalacima.app.ui.Rutas.finanzas(nivel) }
}

/** Resumen del día para el planificador: sus movimientos y lo disponible, y el botón para anotar. */
@Composable
fun ResumenCuentasDia(fecha: LocalDate) {
    val vm = rutaViewModel { CuentasViewModel(it) }
    val libro by vm.libro.collectAsStateWithLifecycle()
    val abrir = irACuentas()
    val movs = libro.movs(fecha.toString()); val s = Cuentas.sumar(movs, libro.propias)
    val plata: (Double) -> String = { if (libro.ocultarFuera) "•••••" else Cuentas.formato(it, libro.moneda) }
    RutaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.control_financiero), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = { abrir("d:$fecha") }) { Text(stringResource(if (movs.isEmpty()) R.string.cu_anotar else R.string.cu_ver_anotar)) }
        }
        if (movs.isEmpty()) Text(stringResource(R.string.cu_control_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        movs.take(5).forEach { x ->
            val t = Tipo.de(x.t)
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(colorTipo(t))); Spacer(Modifier.width(8.dp))
                Text(x.n.ifBlank { nombreCategoria(t, x.c) }, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(plata(x.m), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (movs.isNotEmpty()) Row(Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.fin_disponible_hoy) + " ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(plata(s.disponible), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = if (s.disponible < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Finanzas del mes (balance del mes y Mi ruta): las cifras del libro de cuentas y el botón para abrirlo. */
@Composable
fun ResumenCuentasMes(ym: YearMonth, modifier: Modifier = Modifier) {
    val vm = rutaViewModel { CuentasViewModel(it) }
    val libro by vm.libro.collectAsStateWithLifecycle()
    val abrir = irACuentas()
    val t = libro.suma("$ym-")
    val plata: (Double) -> String = { if (libro.ocultarFuera) "•••••" else Cuentas.formato(it, libro.moneda) }
    RutaCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.fin_mes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = { abrir("m:$ym") }) { Text(stringResource(R.string.cu_abrir)) }
        }
        Spacer(Modifier.height(8.dp))
        if (t.n == 0) Text(stringResource(R.string.cu_sin_movs_mes, mesFrase(ym.monthValue)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            CifrasCuentas(t, plata)
            t.tasa?.let { Text(stringResource(R.string.fin_tasa, Math.round(it * 100).toInt()), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}
