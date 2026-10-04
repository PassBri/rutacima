package com.rutaalacima.app.data

import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.MetaMensualEntity
import com.rutaalacima.app.data.local.PropositoEntity
import com.rutaalacima.app.data.local.avance

/**
 * La cascada de metas de Ruta a la Cima:
 *   Propósito (5 años) → Meta anual (campamento base) → Meta mensual → check diario.
 * El avance sube solo: los días cumplidos alimentan la meta del mes, las metas del mes
 * alimentan la del año y las del año alimentan el propósito. Si un nivel no tiene hijos,
 * se usa el avance que el usuario marcó a mano.
 */
data class NodoMes(val meta: MetaMensualEntity, val avance: Float)

data class NodoAnio(
    val meta: MetaAnualEntity,
    val meses: List<NodoMes>,
    val avance: Float,
    val automatico: Boolean,
)

data class NodoProposito(
    val proposito: PropositoEntity,
    val anios: List<NodoAnio>,
    val avance: Float,
    val automatico: Boolean,
)

data class Cascada(
    val propositos: List<NodoProposito>,
    /** Metas anuales que todavía no están conectadas a un propósito. */
    val aniosSueltos: List<NodoAnio>,
    /** Metas mensuales que todavía no están conectadas a una meta anual. */
    val mesesSueltos: List<NodoMes>,
) {
    val avanceGlobal: Float
        get() = propositos.map { it.avance }.ifEmpty { aniosSueltos.map { it.avance } }.ifEmpty { listOf(0f) }.average().toFloat()

    companion object {
        fun construir(
            propositos: List<PropositoEntity>,
            anuales: List<MetaAnualEntity>,
            mensuales: List<MetaMensualEntity>,
        ): Cascada {
            val mesesPorAnual = mensuales.filter { it.metaAnualId != null }.groupBy { it.metaAnualId }
            fun nodoAnio(m: MetaAnualEntity): NodoAnio {
                val hijos = mesesPorAnual[m.id].orEmpty().map { NodoMes(it, it.avance()) }
                return if (hijos.isNotEmpty()) NodoAnio(m, hijos, hijos.map { it.avance }.average().toFloat(), true)
                else NodoAnio(m, emptyList(), m.avance / 100f, false)
            }
            val anioPorProp = anuales.filter { it.propositoId != null }.groupBy { it.propositoId }
            val nodos = propositos.map { p ->
                val hijos = anioPorProp[p.id].orEmpty().map(::nodoAnio)
                if (hijos.isNotEmpty()) NodoProposito(p, hijos, hijos.map { it.avance }.average().toFloat(), true)
                else NodoProposito(p, emptyList(), p.progreso / 100f, false)
            }
            val idsProp = propositos.map { it.id }.toSet()
            val idsAnual = anuales.map { it.id }.toSet()
            return Cascada(
                propositos = nodos,
                aniosSueltos = anuales.filter { it.propositoId == null || it.propositoId !in idsProp }.map(::nodoAnio),
                mesesSueltos = mensuales.filter { it.metaAnualId == null || it.metaAnualId !in idsAnual }.map { NodoMes(it, it.avance()) },
            )
        }
    }
}
