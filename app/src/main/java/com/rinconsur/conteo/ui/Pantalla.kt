package com.rinconsur.conteo.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rinconsur.conteo.R
import kotlin.math.roundToInt

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

private tailrec fun Context.buscarActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActivity()
    else -> error("No hay una Activity en el contexto")
}

/** Guía 9: tamaño de ventana reutilizable para la UI adaptativa. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun obtenerWindowSizeClass(): WindowSizeClass =
    calculateWindowSizeClass(LocalContext.current.buscarActivity())

/** Convierte una proporción (0..1) en el porcentaje entero que se muestra en la UI. */
fun Float.comoPorcentaje(): Int = (this * 100).roundToInt()

/** Guía 13: imagen circular reutilizable; muestra un ícono de perfil si no hay imagen. */
@Composable
internal fun ImagenInteligente(uri: Uri?, modifier: Modifier = Modifier, tamano: Dp = 160.dp) {
    if (uri == null) {
        Box(
            modifier = modifier
                .size(tamano)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = stringResource(R.string.perfil_sin_foto),
                modifier = Modifier.size(tamano / 2),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    } else {
        AsyncImage(
            model = uri,
            contentDescription = stringResource(R.string.perfil_foto),
            contentScale = ContentScale.Crop,
            modifier = modifier.size(tamano).clip(CircleShape)
        )
    }
}
