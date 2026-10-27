package com.rutaalacima.app.ui.vision

import com.rutaalacima.app.data.Cascada
import com.rutaalacima.app.domain.model.Mandala
import com.rutaalacima.app.domain.model.MetodoCima
import com.rutaalacima.app.ui.Rutas
import java.time.LocalDate
import kotlin.math.roundToInt

internal data class MetasVision(
    val avancePorCodigo: Map<String, Float>,
    val sostenPorId: Map<Long, List<MetaSosten>>,
)

/** Recorre la cascada una vez por cambio de datos, fuera de la composición de cada tarjeta. */
internal fun calcularMetasVision(cumbre: CasillaVista?, campamentos: List<CasillaVista?>, cascada: Cascada?, hoy: LocalDate): MetasVision {
    val datos = cascada ?: return MetasVision(emptyMap(), emptyMap())
    val anuales = datos.propositos.flatMap { p -> p.anios.map { it to (it.meta.eje ?: p.proposito.eje) } } +
        datos.aniosSueltos.map { it to it.meta.eje }
    val meses = anuales.flatMap { (a, eje) -> a.meses.map { it to (it.meta.eje ?: eje) } } +
        datos.mesesSueltos.map { it to it.meta.eje }
    val avances = anuales.filter { it.first.meta.anio == hoy.year }.map { it.second to it.first.avance } +
        datos.mesesSueltos.filter { it.meta.metaAnualId == null && it.meta.anio == hoy.year && it.meta.mes == hoy.monthValue }.map { it.meta.eje to it.avance }
    val avancePorCodigo = avances.groupBy({ it.first?.takeIf(String::isNotBlank) ?: "CON" }, { it.second })
        .mapValues { (_, valores) -> valores.average().toFloat() * 100f }
    val propositos = datos.propositos.map {
        it.proposito.eje to MetaSosten("p", it.proposito.id, it.proposito.titulo, (it.avance * 100).toInt(), Rutas.proposito(it.proposito.id))
    }
    val metasAnio = anuales.filter { it.first.meta.anio >= hoy.year }.map { (a, eje) ->
        eje to MetaSosten("a", a.meta.id, a.meta.titulo, (a.avance * 100).toInt(), Rutas.planificador(3, LocalDate.of(a.meta.anio, 1, 1)))
    }
    val metasMes = meses.filter { it.first.meta.anio == hoy.year && it.first.meta.mes == hoy.monthValue }.map { (m, eje) ->
        eje to MetaSosten("m", m.meta.id, m.meta.texto, (m.avance * 100).toInt(), Rutas.planificador(2, LocalDate.of(m.meta.anio, m.meta.mes, 1)))
    }
    val todas = propositos + metasAnio + metasMes
    val porCodigo = todas.groupBy({ it.first?.takeIf(String::isNotBlank) ?: "CON" }, { it.second })
    val porId = buildMap {
        cumbre?.let { put(it.casilla.id, todas.map { meta -> meta.second }) }
        campamentos.forEachIndexed { i, cv -> cv?.let {
            val codigo = Mandala.CAMPAMENTOS_FIJOS[i]
            put(it.casilla.id, porCodigo[codigo].orEmpty())
        } }
    }
    return MetasVision(avancePorCodigo, porId)
}

/** Los 64 pasos y el ritmo ya están calculados en DatosMandala; se reutilizan en toda la pantalla. */
internal fun porcentajesVision(datos: DatosMandala, estados: Map<String, String>, metas: Map<String, Float>): Map<Long, Int> {
    fun manual(id: Long, base: Float): Float = if (estados["vision:estado:$id"] == "LOGRADA") 100f else {
        maxOf(base, estados["vision:pct:$id"]?.toFloatOrNull()?.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 100f)
    }
    val avances = datos.campamentos.mapIndexed { i, cv ->
        if (cv == null) 0f else {
            val pasos = (0 until Mandala.PASOS).sumOf { MetodoCima.avance(datos.pasoCima(i, it), datos.ritmo.dificultad).toDouble() }
                .toFloat() * 100f / Mandala.PASOS
            val meta = metas[datos.codigo(i)] ?: 0f
            manual(cv.casilla.id, maxOf(pasos, meta))
        }
    }
    return buildMap {
        datos.campamentos.forEachIndexed { i, cv -> cv?.let { put(it.casilla.id, avances[i].roundToInt()) } }
        datos.cumbre?.let { put(it.casilla.id, manual(it.casilla.id, avances.average().toFloat()).roundToInt()) }
    }
}
