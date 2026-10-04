package com.rutaalacima.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_CO = Locale("es", "CO")
private val FMT_FECHA = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE_CO)
private val FMT_DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE_CO)

fun formatoFecha(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(FMT_FECHA)

fun formatoFecha(fecha: LocalDate): String = fecha.format(FMT_FECHA)

fun formatoDia(fecha: LocalDate): String = fecha.format(FMT_DIA).replaceFirstChar { it.uppercase() }

fun LocalDate.aMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun hoy(): LocalDate = LocalDate.now()
