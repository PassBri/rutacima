package com.rutaalacima.app.data.vision

import com.rutaalacima.app.data.RespuestasRepository
import com.rutaalacima.app.data.local.MetaAnualEntity
import com.rutaalacima.app.data.local.PropositoEntity
import com.rutaalacima.app.data.local.RutaDatabase
import com.rutaalacima.app.data.local.VisionCasillaEntity
import com.rutaalacima.app.domain.model.Mandala
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Brújula ↔ Metas ↔ Mi ruta, sincronizadas (la misma regla que web/js/37-sincro-metas.js).
 * - Brújula general: cada campamento ↔ un propósito de largo plazo de ese eje; sus 8 pasos ↔ sus metas específicas.
 * - Brújula de un año: cada campamento ↔ la meta de ese año con ese eje; sus 8 pasos ↔ sus sub-metas.
 * - Confluencia va con lo que no tiene eje; Campamento Base lleva el código CAM.
 * - La Brújula manda: lo escrito en un paso pisa la sub-meta; una sub-meta en un paso vacío lo llena.
 * - Las sub-metas 1 a 8 son los pasos 1 a 8; desde la 9 se quedan solo en Metas.
 * - Si un campamento no tiene su meta (o propósito), se crea con el nombre del campamento.
 */
class SincroBrujula(private val db: RutaDatabase, private val respuestas: RespuestasRepository) {
    companion object {
        val CODS = listOf("CON", "TRA", "VAL", "EVO", "VOZ", "CAM", "VOL", "MAE")
        const val PASOS = 8
        fun lineas(s: String): MutableList<String> = s.split("\n").map { it.trim() }.toMutableList()
        fun unir(ls: List<String>): String = ls.dropLastWhile { it.isEmpty() }.joinToString("\n")
        private fun ejeDe(cod: String): String? = if (cod == "CON") null else cod
        private fun nombre(c: VisionCasillaEntity) = c.afirmacion.ifBlank { c.titulo }.trim().take(120)
    }

    private val candado = Mutex()
    private val dao get() = db.planificadorDao()

    private suspend fun campamentos(anio: Int?): List<VisionCasillaEntity?> =
        Mandala.repartir(db.visionDao().todas().filter { Mandala.anioDeOrigen(it.origen) == anio }, { it.origen }, { it.eje }).second

    private suspend fun proposito(cod: String): PropositoEntity? =
        dao.observePropositos().first().filter { it.eje == ejeDe(cod) }.minByOrNull { it.id }

    private suspend fun meta(anio: Int, cod: String): MetaAnualEntity? =
        dao.observeTodasLasMetas().first().filter { it.anio == anio && it.eje == ejeDe(cod) }.minByOrNull { it.id }

    /** La casilla (campamento) de una meta del año, si su Brújula existe y es la meta que la acompaña. */
    suspend fun casillaDeMeta(m: MetaAnualEntity): VisionCasillaEntity? {
        val cod = m.eje ?: "CON"
        if (cod !in CODS || meta(m.anio, cod)?.id != m.id) return null
        return campamentos(m.anio)[Mandala.CAMPAMENTOS_FIJOS.indexOf(cod)]
    }

    /** La casilla (campamento) de un propósito en la Brújula general. */
    suspend fun casillaDeProposito(p: PropositoEntity): VisionCasillaEntity? {
        val cod = p.eje ?: "CON"
        if (cod !in CODS || proposito(cod)?.id != p.id) return null
        return campamentos(null)[Mandala.CAMPAMENTOS_FIJOS.indexOf(cod)]
    }

    private suspend fun lineasCon(casilla: VisionCasillaEntity, actuales: String, r: Map<String, String>): String {
        val ls = lineas(actuales); while (ls.size < PASOS) ls.add("")
        for (p in 0 until PASOS) {
            val t = r[Mandala.clave(casilla.id, p)].orEmpty().trim()
            if (t.isNotEmpty()) ls[p] = t
            else if (ls[p].isNotEmpty()) respuestas.guardar(Mandala.WORKBOOK, Mandala.clave(casilla.id, p), ls[p])
        }
        return unir(ls)
    }

    /** Deja la Brújula general, las de cada año y sus metas iguales (sin escrituras si ya lo están). */
    suspend fun reconciliar() = candado.withLock {
        val r = respuestas.cargar(Mandala.WORKBOOK)
        val anios = listOf<Int?>(null) + db.visionDao().todas().mapNotNull { Mandala.anioDeOrigen(it.origen) }.distinct()
        for (anio in anios) {
            val camps = campamentos(anio)
            for (cod in CODS) {
                val c = camps[Mandala.CAMPAMENTOS_FIJOS.indexOf(cod)] ?: continue
                if (anio == null) {
                    val p = proposito(cod)
                    val base = p ?: PropositoEntity(orden = dao.contarPropositos(), titulo = nombre(c), eje = ejeDe(cod))
                    val txt = lineasCon(c, base.metasEspecificas, r)
                    if (p == null) dao.insertProposito(base.copy(metasEspecificas = txt))
                    else if (txt != unir(lineas(p.metasEspecificas))) dao.updateProposito(p.copy(metasEspecificas = txt))
                } else {
                    val m = meta(anio, cod)
                    val base = m ?: MetaAnualEntity(anio = anio, titulo = nombre(c), eje = ejeDe(cod), propositoId = proposito(cod)?.id)
                    val txt = lineasCon(c, base.subMetas, r)
                    if (m == null || txt != unir(lineas(m.subMetas))) dao.upsertMeta(base.copy(subMetas = txt))
                }
            }
        }
    }

    /** Un paso escrito (o borrado) en la Brújula: su sub-meta cambia al instante. */
    suspend fun pasoEscrito(casilla: VisionCasillaEntity, paso: Int, texto: String) = candado.withLock {
        val anio = Mandala.anioDeOrigen(casilla.origen)
        val i = campamentos(anio).indexOfFirst { it?.id == casilla.id }
        val cod = Mandala.CAMPAMENTOS_FIJOS.getOrNull(i) ?: return@withLock
        if (cod !in CODS) return@withLock
        val t = texto.trim()
        if (anio == null) {
            val p = proposito(cod) ?: if (t.isEmpty()) return@withLock
                else PropositoEntity(orden = dao.contarPropositos(), titulo = nombre(casilla), eje = ejeDe(cod))
            val ls = lineas(p.metasEspecificas); while (ls.size < PASOS) ls.add(""); ls[paso] = t
            if (p.id == 0L) dao.insertProposito(p.copy(metasEspecificas = unir(ls))) else dao.updateProposito(p.copy(metasEspecificas = unir(ls)))
        } else {
            val m = meta(anio, cod) ?: if (t.isEmpty()) return@withLock
                else MetaAnualEntity(anio = anio, titulo = nombre(casilla), eje = ejeDe(cod), propositoId = proposito(cod)?.id)
            val ls = lineas(m.subMetas); while (ls.size < PASOS) ls.add(""); ls[paso] = t
            dao.upsertMeta(m.copy(subMetas = unir(ls)))
        }
    }

    /**
     * Las sub-metas editadas en Metas (lista completa, por posición): las 8 primeras escriben su paso.
     * Devuelve el texto a guardar (una por línea).
     */
    suspend fun lineasEditadas(casilla: VisionCasillaEntity?, ls: List<String>): String {
        if (casilla != null) for (p in 0 until PASOS)
            respuestas.guardar(Mandala.WORKBOOK, Mandala.clave(casilla.id, p), ls.getOrNull(p).orEmpty().trim())
        return unir(ls.map { it.trim() })
    }
}
