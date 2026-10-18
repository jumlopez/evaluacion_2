package com.rinconsur.conteo.ui.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private tailrec fun Context.buscarActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActivity()
    else -> error("No hay una Activity en el contexto")
}

/** Guía 9: función reutilizable para calcular el tamaño de pantalla en toda la app. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun obtenerWindowSizeClass(): WindowSizeClass =
    calculateWindowSizeClass(LocalContext.current.buscarActivity())
