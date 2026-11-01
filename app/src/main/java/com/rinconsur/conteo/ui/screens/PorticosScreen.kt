package com.rinconsur.conteo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.ui.components.Cargando
import com.rinconsur.conteo.ui.components.ContenidoBoton
import com.rinconsur.conteo.ui.components.ErrorTexto
import com.rinconsur.conteo.ui.components.PantallaConSnackbar
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

    PantallaConSnackbar(snackbar, modifier) {
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
                        listOf(
                            Triple("Entradas", fila.entradas, viewModel::onEntradasChange),
                            Triple("Salidas", fila.salidas, viewModel::onSalidasChange)
                        ).forEach { (etiqueta, valor, onCambio) ->
                            OutlinedTextField(
                                value = valor,
                                onValueChange = { onCambio(fila.accesoId, it) },
                                label = { Text(etiqueta) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        ErrorTexto(estado.error)

        Button(onClick = viewModel::guardar, modifier = Modifier.fillMaxWidth()) {
            ContenidoBoton(Icons.Filled.Save, "Guardar conteos de hoy")
        }
        Text(
            "Deja un acceso en blanco si su pórtico no tiene lectura.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
