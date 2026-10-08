package com.rutaalacima.app.domain.model

import java.time.LocalDate

/**
 * Comodines de racha (como el "protector de racha" de Duolingo): si un día no marcas ningún
 * hábito, un comodín lo cubre y la racha sigue. Se gana uno por cada [CADA] días seguidos con
 * hábitos y se guardan hasta [MAX]. Todo se calcula con los días marcados, así que el teléfono
 * y la web llegan siempre al mismo resultado sin guardar nada aparte.
 */
object Comodines {
    const val CADA = 7
    const val MAX = 2

    data class Estado(
        /** Días seguidos con hábitos (los días cubiertos por un comodín no suman, pero no la cortan). */
        val racha: Int,
        val mejorRacha: Int,
        val disponibles: Int,
        /** Días que protegió un comodín. */
        val usados: Set<LocalDate>,
        /** Días con hábitos que faltan para ganar el siguiente comodín (0 si ya tienes el máximo). */
        val proximoEn: Int,
    )

    fun calcular(dias: Set<LocalDate>, hoy: LocalDate, desde: LocalDate? = null): Estado {
        val inicio = desde ?: dias.filter { !it.isAfter(hoy) }.minOrNull() ?: return Estado(0, 0, 0, emptySet(), CADA)
        var racha = 0; var mejor = 0; var disponibles = 0; var seguidos = 0
        val usados = mutableSetOf<LocalDate>()
        var d = inicio
        while (!d.isAfter(hoy)) {
            when {
                d in dias -> {
                    racha++; seguidos++
                    if (seguidos == CADA) { seguidos = 0; if (disponibles < MAX) disponibles++ }
                    mejor = maxOf(mejor, racha)
                }
                d == hoy -> Unit                  // hoy todavía puedes marcar: no se gasta nada
                racha > 0 && disponibles > 0 -> { disponibles--; usados += d }
                else -> { racha = 0; seguidos = 0 }
            }
            d = d.plusDays(1)
        }
        return Estado(racha, mejor, disponibles, usados, if (disponibles >= MAX) 0 else CADA - seguidos)
    }
}

/**
 * Resumen del año (como el "Year in Sport" de Strava): las cifras de tu ascenso en un año, para
 * verlas como historias y compartirlas.
 */
object ResumenAnio {
    data class Datos(
        val anio: Int,
        val diasConHabitos: Int,
        val habitos: Int,
        val mejorRacha: Int,
        val comodinesUsados: Int,
        /** Código del eje con más hábitos (VOL, MAE…) o null si no hay hábitos. */
        val ejeFuerte: String?,
        /** Mes (1-12) con más días con hábitos, o null. */
        val mejorMes: Int?,
        val metasMesCumplidas: Int,
        val metasAnioCumplidas: Int,
        val recuerdos: Int,
        val frasesAbiertas: Int,
    ) {
        /** Con menos de una semana de hábitos y nada más, todavía no hay un año que contar. */
        val suficiente: Boolean get() = diasConHabitos >= 7 || metasMesCumplidas + metasAnioCumplidas + recuerdos > 0
    }

    /** [checks]: hábitos marcados por día (ids como "VOL1"); el eje son sus tres primeras letras. */
    fun calcular(
        anio: Int, checks: Map<LocalDate, Set<String>>, hoy: LocalDate,
        metasMesCumplidas: Int, metasAnioCumplidas: Int, recuerdos: Int, frasesAbiertas: Int,
    ): Datos {
        val delAnio = checks.filterKeys { it.year == anio && !it.isAfter(hoy) }.filterValues { it.isNotEmpty() }
        val fin = minOf(hoy, LocalDate.of(anio, 12, 31))
        val comodines = Comodines.calcular(delAnio.keys, fin, LocalDate.of(anio, 1, 1).takeIf { delAnio.isNotEmpty() })
        val ejes = delAnio.values.flatten().groupingBy { it.take(3) }.eachCount()
        val meses = delAnio.keys.groupingBy { it.monthValue }.eachCount()
        return Datos(
            anio = anio, diasConHabitos = delAnio.size, habitos = delAnio.values.sumOf { it.size },
            mejorRacha = comodines.mejorRacha, comodinesUsados = comodines.usados.size,
            ejeFuerte = ejes.maxByOrNull { it.value }?.key, mejorMes = meses.maxByOrNull { it.value }?.key,
            metasMesCumplidas = metasMesCumplidas, metasAnioCumplidas = metasAnioCumplidas,
            recuerdos = recuerdos, frasesAbiertas = frasesAbiertas,
        )
    }

    /** El resumen se ofrece en Hoy desde el 1 de diciembre hasta el 15 de enero (del año que termina). */
    fun anioParaOfrecer(hoy: LocalDate): Int? = when {
        hoy.monthValue == 12 -> hoy.year
        hoy.monthValue == 1 && hoy.dayOfMonth <= 15 -> hoy.year - 1
        else -> null
    }
}

/**
 * La frase del día como invitación a publicar (como BeReal): cada día una pregunta distinta sobre
 * la frase, para responder con una reflexión o una foto del día.
 */
object InvitacionFrase {
    const val PREGUNTAS = 7

    /** Pregunta del día (0 a [PREGUNTAS]-1): cambia cada día y se repite cada semana. */
    fun pregunta(fecha: LocalDate): Int = (fecha.toEpochDay() % PREGUNTAS).toInt().let { if (it < 0) it + PREGUNTAS else it }

    /** Etiqueta con la que se vincula la publicación a la frase de ese día ("Frase del día 281"). */
    fun etiqueta(indiceFrase: Int, plantilla: String): String = plantilla.format(indiceFrase + 1)
}
