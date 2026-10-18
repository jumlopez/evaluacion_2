package com.rinconsur.conteo.model

import java.util.Calendar

const val DIA_MS = 24L * 60 * 60 * 1000

/** Periodos de consulta histórica del dashboard. */
enum class Periodo(val etiqueta: String) {
    HOY("Hoy"),
    SIETE_DIAS("7 días"),
    TODO("Todo");

    fun desdeMillis(ahora: Long = System.currentTimeMillis()): Long = when (this) {
        HOY -> inicioDelDia(ahora)
        SIETE_DIAS -> inicioDelDia(ahora) - 6 * DIA_MS
        TODO -> 0L
    }
}

fun inicioDelDia(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
