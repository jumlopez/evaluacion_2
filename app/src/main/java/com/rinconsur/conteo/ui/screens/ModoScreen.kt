package com.rinconsur.conteo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.conteo.ui.components.Cargando
import com.rinconsur.conteo.viewmodel.ModoViewModel

/** Guía 12: estado persistente (DataStore), loader inicial y feedback animado. */
@Composable
fun ModoScreen(
    modifier: Modifier = Modifier,
    viewModel: ModoViewModel = viewModel()
) {
    val mostrarMensaje by viewModel.mostrarMensaje.collectAsStateWithLifecycle()
    // null = simulación de carga inicial
    val estaActivo = viewModel.activo.collectAsStateWithLifecycle().value ?: return Cargando(modifier)

    // Estado local que sobrevive a rotaciones (rememberSaveable)
    var cambiosEnSesion by rememberSaveable { mutableIntStateOf(0) }

    // Texto derivado del estado (derivedStateOf)
    val textoBoton by remember(estaActivo) {
        derivedStateOf { if (estaActivo) "Desactivar modo análisis" else "Activar modo análisis" }
    }

    // Color animado según el estado
    val colorAnimado by animateColorAsState(
        targetValue = if (estaActivo) Color(0xFF4CAF50) else Color(0xFFB0BEC5),
        animationSpec = tween(durationMillis = 500),
        label = "colorModo"
    )

    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Modo análisis", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        AnimatedContent(targetState = estaActivo, label = "descripcionModo") { activoAhora ->
            Text(
                if (activoAhora) "Activo: el dashboard muestra el perfil por género y edad de las entradas."
                else "Inactivo: el dashboard muestra solo los totales por acceso.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                cambiosEnSesion++
                viewModel.alternarEstado()
            },
            colors = ButtonDefaults.buttonColors(containerColor = colorAnimado),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Text(textoBoton, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))
        Text("Cambios en esta sesión: $cambiosEnSesion", style = MaterialTheme.typography.labelMedium)

        Spacer(Modifier.height(16.dp))
        AnimatedVisibility(visible = mostrarMensaje) {
            Text(
                "¡Estado guardado exitosamente!",
                color = Color(0xFF2E7D32),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
