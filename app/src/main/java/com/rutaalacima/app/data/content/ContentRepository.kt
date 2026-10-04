package com.rutaalacima.app.data.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Lee los workbooks desde assets/content (generados con tools/convert.py) y los mantiene en caché.
 */
class ContentRepository(private val context: Context) {

    private val cache = mutableMapOf<String, Workbook>()
    private var index: List<WorkbookSummary>? = null
    private val mutex = Mutex()

    suspend fun index(): List<WorkbookSummary> = mutex.withLock {
        index ?: withContext(Dispatchers.IO) {
            val text = context.assets.open("content/index.json").bufferedReader().use { it.readText() }
            ContentJson.decodeFromString<List<WorkbookSummary>>(text).sortedBy { it.order }
        }.also { index = it }
    }

    suspend fun workbook(id: String): Workbook = mutex.withLock {
        cache[id] ?: withContext(Dispatchers.IO) {
            val text = context.assets.open("content/$id.json").bufferedReader().use { it.readText() }
            ContentJson.decodeFromString<Workbook>(text)
        }.also { cache[id] = it }
    }

    companion object {
        // Identificadores de workbooks usados por pantallas nativas.
        const val KIT = "kit"
        const val NIEBLA = "niebla"
        const val CIERRE_MENSUAL = "bono_cierre"
        const val DESCUBRE = "descubre"
        const val SEIS_EJES = "seis_ejes"
        const val DIAGNOSTICO = "diagnostico"
        const val PORTALES = "portales"
    }
}
