package com.rinconsur.conteo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF00695C),
    secondary = Color(0xFF4F6B66),
    tertiary = Color(0xFFB26A00)
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    secondary = Color(0xFFB0CCC6),
    tertiary = Color(0xFFFFB74D)
)

@Composable
fun AppRinconTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        content = content
    )
}
