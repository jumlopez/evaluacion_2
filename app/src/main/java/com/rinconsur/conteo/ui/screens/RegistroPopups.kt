package com.rinconsur.conteo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rinconsur.conteo.R
import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.GrupoConteo
import com.rinconsur.conteo.model.RangosEtarios
import kotlinx.coroutines.delay

private const val DURACION_CIERRE_MS = 140L
private const val DURACION_ENTRADA_MS = 140
private const val DURACION_ESCALA_ENTRADA_MS = 180
private const val DURACION_SALIDA_MS = 110
private const val DURACION_ESCALA_SALIDA_MS = 120
private const val ALTO_BOTON_POPUP_DP = 56

/**
 * Contenedor animado común a los popups: escala + fade al entrar y al salir. El
 * cierre real (cambio de estado del padre) se avisa cuando termina la animación.
 */
@Composable
private fun PopupAnimado(
    onDescartar: () -> Unit,
    contenido: @Composable (cerrar: () -> Unit) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var cerrando by remember { mutableStateOf(false) }
    val cerrar: () -> Unit = {
        cerrando = true
        visible = false
    }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(cerrando) {
        if (cerrando) {
            delay(DURACION_CIERRE_MS)
            onDescartar()
        }
    }

    Dialog(
        onDismissRequest = cerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(DURACION_ENTRADA_MS)) +
                scaleIn(tween(DURACION_ESCALA_ENTRADA_MS), initialScale = 0.85f),
            exit = fadeOut(tween(DURACION_SALIDA_MS)) +
                scaleOut(tween(DURACION_ESCALA_SALIDA_MS), targetScale = 0.9f)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                contenido(cerrar)
            }
        }
    }
}

/** Encabezado compartido por los popups: icono, título, subtítulo y botón de cierre. */
@Composable
private fun EncabezadoPopup(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onCerrar: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onCerrar) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.nav_cerrar))
        }
    }
}

/** Primer paso del popup: característica etaria de la persona observada. */
@Composable
internal fun PopupCaracteristica(
    genero: Genero,
    onSeleccion: (String) -> Unit,
    onCerrar: () -> Unit
) {
    PopupAnimado(onDescartar = onCerrar) { cerrar ->
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EncabezadoPopup(
                icono = iconoGenero(genero),
                titulo = stringResource(R.string.popup_persona_observada),
                subtitulo = genero.etiqueta,
                onCerrar = cerrar
            )
            Text(
                stringResource(R.string.popup_caracteristica_pregunta),
                style = MaterialTheme.typography.bodyMedium
            )
            RangosEtarios.opciones.forEach { rango ->
                FilledTonalButton(
                    onClick = {
                        onSeleccion(rango)
                        cerrar()
                    },
                    modifier = Modifier.fillMaxWidth().height(ALTO_BOTON_POPUP_DP.dp)
                ) {
                    Icon(
                        imageVector = iconoRango(rango),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(rango, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/**
 * Segundo paso del popup, solo en salidas: después de la característica etaria se
 * pregunta si la persona compró. "Sin dato" cuenta a la persona sin dato de compra;
 * cerrar el popup descarta el registro.
 */
@Composable
internal fun PopupCompra(
    grupo: GrupoConteo,
    onSeleccion: (Boolean?) -> Unit,
    onCerrar: () -> Unit
) {
    PopupAnimado(onDescartar = onCerrar) { cerrar ->
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EncabezadoPopup(
                icono = iconoGenero(grupo.genero),
                titulo = stringResource(R.string.popup_compra_pregunta),
                subtitulo = grupo.descripcion,
                onCerrar = cerrar
            )
            Button(
                onClick = {
                    onSeleccion(true)
                    cerrar()
                },
                modifier = Modifier.fillMaxWidth().height(ALTO_BOTON_POPUP_DP.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.popup_si_compro),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            FilledTonalButton(
                onClick = {
                    onSeleccion(false)
                    cerrar()
                },
                modifier = Modifier.fillMaxWidth().height(ALTO_BOTON_POPUP_DP.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.etiqueta_no_compro),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            FilledTonalButton(
                onClick = {
                    onSeleccion(null)
                    cerrar()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.etiqueta_sin_dato))
            }
        }
    }
}
