package com.rinconsur.conteo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.conteo.R
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.viewmodel.ConteoPorticosViewModel

/**
 * El operador transcribe las personas que marcó el pórtico de cada acceso para
 * cruzarlas con las personas registradas desde las cámaras. El pórtico no distingue
 * entrada de salida, así que solo se anota un total por acceso.
 */
@Composable
fun PorticosScreen(modifier: Modifier = Modifier, viewModel: ConteoPorticosViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val mensajeGuardado = stringResource(R.string.porticos_guardado_ok)

    LaunchedEffect(estado.guardadoOk) {
        if (estado.guardadoOk) {
            snackbar.showSnackbar(mensajeGuardado)
            viewModel.consumirGuardado()
        }
    }

    if (estado.cargando) {
        Cargando(modifier)
        return
    }

    PantallaConSnackbar(snackbar, modifier) {
        EncabezadoPantalla(R.string.porticos_titulo, R.string.porticos_descripcion)

        Accesos.ids.forEach { id ->
            val fila = estado.fila(id)
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(Accesos.etiqueta(id), style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = fila.personas,
                        onValueChange = { viewModel.onPersonasChange(id, it) },
                        label = { Text(stringResource(R.string.porticos_campo)) },
                        supportingText = {
                            Text(stringResource(R.string.porticos_campo_ayuda))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        estado.error?.let { mensaje ->
            Text(
                mensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(onClick = viewModel::guardar, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Save, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.porticos_guardar))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.porticos_blanco),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.porticos_total, estado.totalPersonas),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
