package com.rinconsur.conteo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.ui.components.Cargando
import com.rinconsur.conteo.ui.components.ErrorTexto
import com.rinconsur.conteo.viewmodel.ConteoPorticosViewModel

/**
 * El operador transcribe las lecturas diarias de los pórticos de cada acceso para
 * cruzarlas con las personas registradas desde las cámaras.
 */
@Composable
fun PorticosScreen(
    modifier: Modifier = Modifier,
    viewModel: ConteoPorticosViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.guardadoOk) {
        if (estado.guardadoOk) {
            snackbar.showSnackbar("Conteos de pórticos guardados")
            viewModel.consumirGuardado()
        }
    }

    if (estado.cargando) return Cargando(modifier)

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Conteo de pórticos de hoy", style = MaterialTheme.typography.titleLarge)
            Text(
                "Transcribe las entradas y salidas que marca cada pórtico. " +
                    "El dashboard las compara con lo registrado desde las cámaras.",
                style = MaterialTheme.typography.bodySmall
            )

            estado.filas.forEach { fila ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Accesos.etiqueta(fila.accesoId), style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = fila.entradas,
                                onValueChange = { viewModel.onEntradasChange(fila.accesoId, it) },
                                label = { Text("Entradas") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = fila.salidas,
                                onValueChange = { viewModel.onSalidasChange(fila.accesoId, it) },
                                label = { Text("Salidas") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            ErrorTexto(estado.error)

            Button(onClick = viewModel::guardar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Guardar conteos de hoy")
            }
            Text(
                "Deja un acceso en blanco si su pórtico no tiene lectura.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}
