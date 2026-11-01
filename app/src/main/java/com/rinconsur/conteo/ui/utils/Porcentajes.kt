package com.rinconsur.conteo.ui.utils

import kotlin.math.roundToInt

/**
 * Convierte una proporción (0..1) en el porcentaje entero que se muestra en la UI.
 * Centraliza el redondeo que antes se repetía en cada pantalla y gráfico.
 */
fun Float.comoPorcentaje(): Int = (this * 100).roundToInt()
