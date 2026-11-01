package com.rinconsur.conteo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Transgender
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.Movimiento
import com.rinconsur.conteo.model.RangosEtarios
import com.rinconsur.conteo.ui.components.ContenidoBoton
import com.rinconsur.conteo.ui.components.DatoNumerico
import com.rinconsur.conteo.ui.components.ErrorTexto
import com.rinconsur.conteo.ui.components.PantallaConSnackbar
import com.rinconsur.conteo.viewmodel.RegistroViewModel

private fun iconoGenero(genero: Genero): ImageVector = when (genero) {
    Genero.FEMENINO -> Icons.Filled.Female
    Genero.MASCULINO -> Icons.Filled.Male
    Genero.OTRO -> Icons.Filled.Transgender
    Genero.NO_DECLARA -> Icons.AutoMirrored.Filled.HelpOutline
}

/**
 * Captura rápida para el operador de cámaras: botones grandes con iconos, un toque
 * por persona y un botón "+1" para niños acompañantes. Guarda directo en SQLite.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistroScreen(
    registroViewModel: RegistroViewModel,
    modifier: Modifier = Modifier
) {
    val estado by registroViewModel.estado.collectAsStateWithLifecycle()
    val mensaje by registroViewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        val texto = mensaje ?: return@LaunchedEffect
        snackbar.showSnackbar(texto)
        registroViewModel.consumirMensaje()
    }

    PantallaConSnackbar(snackbar, modifier) {
        Text("Registro rápido", style = MaterialTheme.typography.titleLarge)
        Text(
            "Mira las cámaras y toca un botón por cada persona. Cada toque suma 1.",
            style = MaterialTheme.typography.bodySmall
        )

        // Acceso
        Text("Acceso", style = MaterialTheme.typography.titleSmall)
        Text(
            "Piso 1: accesos 1, 2 y 3 · Piso 2: acceso 4",
            style = MaterialTheme.typography.bodySmall
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Accesos.ids.forEach { id ->
                FilterChip(
                    selected = estado.accesoId == id,
                    onClick = { registroViewModel.onAccesoChange(id) },
                    label = { Text(Accesos.etiquetaCorta(id)) }
                )
            }
        }
        ErrorTexto(estado.errores.acceso)

        // Entrada o salida
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Movimiento.entries.forEach { movimiento ->
                FilterChip(
                    selected = estado.movimiento == movimiento,
                    onClick = { registroViewModel.onMovimientoChange(movimiento) },
                    label = { Text(movimiento.etiqueta) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (movimiento == Movimiento.ENTRADA) {
                                Icons.AutoMirrored.Filled.Login
                            } else {
                                Icons.AutoMirrored.Filled.Logout
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        // Personas por género
        Text("Personas observadas", style = MaterialTheme.typography.titleSmall)
        // Dos botones por fila, en el orden de Genero.entries
        Genero.entries.chunked(2).forEach { par ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                par.forEach { genero ->
                    BotonGenero(
                        genero = genero,
                        cantidad = estado.cantidad(genero),
                        onAgregar = { registroViewModel.agregarPersona(genero) },
                        onQuitar = { registroViewModel.quitarPersona(genero) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        ErrorTexto(estado.errores.conteo)

        // Niños acompañantes
        TarjetaNinos(
            ninos = estado.ninosAcompanantes,
            onAgregar = registroViewModel::agregarNino,
            onQuitar = registroViewModel::quitarNino
        )
        ErrorTexto(estado.errores.ninos)

        // Rango etario
        Text("Rango etario (opcional)", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RangosEtarios.opciones.forEach { rango ->
                FilterChip(
                    selected = estado.rangoEtario == rango,
                    onClick = { registroViewModel.onRangoChange(rango) },
                    label = { Text(rango) }
                )
            }
        }
        ErrorTexto(estado.errores.rango)

        // Compra: solo aplica a las entradas
        if (estado.movimiento == Movimiento.ENTRADA) {
            Text("¿Compró? (opcional)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(true, "Sí", Icons.Filled.Check),
                    Triple(false, "No", Icons.Filled.Close),
                    Triple(null, "Sin dato", null)
                ).forEach { (valor, texto, icono) ->
                    FilterChip(
                        selected = estado.compro == valor,
                        onClick = { registroViewModel.onComproChange(valor) },
                        label = { Text(texto) },
                        leadingIcon = icono?.let { { Icon(it, null, Modifier.size(18.dp)) } }
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DatoNumerico("Personas", estado.totalPersonas)
                DatoNumerico("Niños", estado.ninosAcompanantes)
                DatoNumerico("Total", estado.totalConNinos)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = registroViewModel::reiniciarConteo,
                modifier = Modifier.weight(1f)
            ) {
                ContenidoBoton(Icons.Filled.Clear, "Limpiar")
            }
            Button(
                onClick = registroViewModel::guardar,
                enabled = estado.totalPersonas > 0,
                modifier = Modifier.weight(2f)
            ) {
                ContenidoBoton(Icons.Filled.Save, "Guardar ${estado.totalConNinos}")
            }
        }
        Text(
            "Se guarda un registro por género en SQLite, sin salir de la pantalla.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun BotonGenero(
    genero: Genero,
    cantidad: Int,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(148.dp).clickable(onClick = onAgregar),
        colors = CardDefaults.cardColors(
            containerColor = if (cantidad > 0) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = iconoGenero(genero),
                contentDescription = genero.etiqueta,
                modifier = Modifier.size(32.dp)
            )
            Text(
                genero.etiqueta,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onQuitar,
                    enabled = cantidad > 0,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Remove, "Quitar una", Modifier.size(18.dp))
                }
                Text("$cantidad", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onAgregar, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Add, "Sumar una", Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun TarjetaNinos(ninos: Int, onAgregar: () -> Unit, onQuitar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ChildCare, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Niños acompañantes", style = MaterialTheme.typography.titleSmall)
                Text("Usa +1 por cada niño", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onQuitar, enabled = ninos > 0) {
                Icon(Icons.Filled.Remove, "Quitar niño", Modifier.size(20.dp))
            }
            Text("$ninos", style = MaterialTheme.typography.headlineSmall)
            FilledTonalButton(
                onClick = onAgregar,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("+1")
            }
        }
    }
}
