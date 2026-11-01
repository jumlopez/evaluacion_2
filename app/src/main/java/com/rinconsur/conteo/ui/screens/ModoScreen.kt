package com.rinconsur.conteo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.conteo.R
import com.rinconsur.conteo.viewmodel.ModoViewModel

private val COLOR_ACTIVO = Color(0xFF2E7D32)
private val COLOR_INACTIVO = Color(0xFF1E88E5)
private const val DURACION_COLOR_MS = 500
private const val DURACION_APARICION_MS = 220
private const val DURACION_DESPLAZAMIENTO_MS = 260
private const val ALTO_BOTON_PRINCIPAL_DP = 60
private const val ALTO_BOTON_SECUNDARIO_DP = 52

/**
 * Guía 12: estado persistente (DataStore), loader inicial y feedback animado.
 * Aquí vive el botón que abre la pantalla de gráficos con una transición.
 */
@Composable
fun ModoScreen(
    modifier: Modifier = Modifier,
    viewModel: ModoViewModel = viewModel(),
    onVerGraficos: () -> Unit = {}
) {
    val activo by viewModel.activo.collectAsStateWithLifecycle()
    val mostrarMensaje by viewModel.mostrarMensaje.collectAsStateWithLifecycle()

    val estaActivo = activo
    if (estaActivo == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // Estado local que sobrevive a rotaciones (rememberSaveable)
    var cambiosEnSesion by rememberSaveable { mutableIntStateOf(0) }

    val colorAnimado by animateColorAsState(
        targetValue = if (estaActivo) COLOR_ACTIVO else COLOR_INACTIVO,
        animationSpec = tween(durationMillis = DURACION_COLOR_MS),
        label = "colorModo"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.pantalla_modo),
            style = MaterialTheme.typography.headlineSmall
        )

        AnimatedContent(targetState = estaActivo, label = "descripcionModo") { activoAhora ->
            Text(
                stringResource(
                    if (activoAhora) {
                        R.string.modo_activo_descripcion
                    } else {
                        R.string.modo_inactivo_descripcion
                    }
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = {
                cambiosEnSesion++
                viewModel.activar()
                onVerGraficos()
            },
            colors = ButtonDefaults.buttonColors(containerColor = colorAnimado),
            modifier = Modifier.fillMaxWidth().height(ALTO_BOTON_PRINCIPAL_DP.dp)
        ) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.modo_ver_graficos),
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (estaActivo) {
            OutlinedButton(
                onClick = {
                    cambiosEnSesion++
                    viewModel.desactivar()
                },
                modifier = Modifier.fillMaxWidth().height(ALTO_BOTON_SECUNDARIO_DP.dp)
            ) {
                Text(stringResource(R.string.modo_desactivar))
            }
        }

        Text(
            stringResource(R.string.modo_cambios_sesion, cambiosEnSesion),
            style = MaterialTheme.typography.labelMedium
        )

        AnimatedVisibility(
            visible = mostrarMensaje,
            enter = fadeIn(tween(DURACION_APARICION_MS)) +
                slideInVertically(tween(DURACION_DESPLAZAMIENTO_MS)) { alto -> alto / 3 }
        ) {
            Text(
                stringResource(R.string.modo_guardado_ok),
                color = COLOR_ACTIVO,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.modo_incluye_titulo),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.modo_incluye_genero),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    stringResource(R.string.modo_incluye_compra),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    stringResource(R.string.modo_incluye_ninos),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    stringResource(R.string.modo_incluye_porticos),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
