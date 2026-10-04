package com.rutaalacima.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Los formatos siguen el idioma elegido en la app (Locale.getDefault() lo actualiza AppCompat). */
private fun fmtFecha() = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())

fun formatoFecha(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(fmtFecha())

fun formatoFecha(fecha: LocalDate): String = fecha.format(fmtFecha())

fun formatoDia(fecha: LocalDate): String {
    val l = Locale.getDefault()
    val dia = fecha.dayOfWeek.getDisplayName(TextStyle.FULL, l)
    val mes = fecha.month.getDisplayName(TextStyle.FULL, l)
    return "$dia ${fecha.dayOfMonth} $mes".replaceFirstChar { it.titlecase(l) }
}

fun LocalDate.aMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun hoy(): LocalDate = LocalDate.now()
