package com.rinconsur.conteo.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Transgender
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.RangosEtarios

/** Icono asociado a cada género del catálogo (incluye los históricos). */
internal fun iconoGenero(genero: Genero): ImageVector = when (genero) {
    Genero.FEMENINO -> Icons.Filled.Female
    Genero.MASCULINO -> Icons.Filled.Male
    Genero.OTRO -> Icons.Filled.Transgender
    Genero.NO_DECLARA -> Icons.AutoMirrored.Filled.HelpOutline
}

/** Icono asociado a cada característica etaria. */
internal fun iconoRango(rango: String): ImageVector = when (rango) {
    RangosEtarios.JOVEN -> Icons.Filled.School
    RangosEtarios.ADULTO -> Icons.Filled.Work
    else -> Icons.Filled.Elderly
}
