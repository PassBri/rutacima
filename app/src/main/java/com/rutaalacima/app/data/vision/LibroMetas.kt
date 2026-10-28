package com.rutaalacima.app.data.vision

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Lo que viaja en el libro "metas" de las respuestas (igual que la web): el período de cada propósito
 * ("periodo:p:<id>" = año en que empieza) y las sub-metas escogidas para cada meta del mes ("mes:pasos:<id>" = "0,2,5").
 * Se mantiene al día desde AppContainer para que las pantallas lo lean sin consultas.
 */
object LibroMetas {
    var valores by mutableStateOf<Map<String, String>>(emptyMap())
    fun desde(propositoId: Long): Int? = valores["periodo:p:$propositoId"]?.toIntOrNull()
    fun pasosDelMes(metaMesId: Long): Set<Int> = valores["mes:pasos:$metaMesId"].orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
}
