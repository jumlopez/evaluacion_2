package com.rinconsur.conteo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinconsur.conteo.R
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.GrupoConteo
import com.rinconsur.conteo.model.Movimiento
import com.rinconsur.conteo.model.generosCapturables
import com.rinconsur.conteo.ui.components.DatoNumerico
import com.rinconsur.conteo.viewmodel.RegistroViewModel

private val ALTO_BOTON_GENERO = 132.dp

@Composable
private fun ErrorTexto(mensaje: String?) {
    if (mensaje != null) {
        Text(mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Captura rápida para el operador de cámaras: toca el género, elige joven/adulto/
 * adulto mayor en el primer popup y, cuando es una salida, responde si compró en un
 * segundo popup. La entrada se guarda directo en SQLite, sin pasos intermedios.
 */
@Composable
fun RegistroScreen(
    registroViewModel: RegistroViewModel,
    modifier: Modifier = Modifier
) {
    val estado by registroViewModel.estado.collectAsStateWithLifecycle()
    val mensaje by registroViewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Paso 1: género tocado. Paso 2 (solo salida): si compró.
    var generoPendiente by remember { mutableStateOf<Genero?>(null) }
    var rangoElegido by remember { mutableStateOf<String?>(null) }
    var compraPendiente by remember { mutableStateOf<GrupoConteo?>(null) }

    val esSalida = estado.movimiento == Movimiento.SALIDA
    val compradores = estado.gruposRegistrados
        .filter { (grupo, _) -> grupo.compro == true }.sumOf { it.second }
    val noCompradores = estado.gruposRegistrados
        .filter { (grupo, _) -> grupo.compro == false }.sumOf { it.second }
    val sinDatoCompra = estado.totalPersonas - compradores - noCompradores

    LaunchedEffect(mensaje) {
        val texto = mensaje ?: return@LaunchedEffect
        snackbar.showSnackbar(texto)
        registroViewModel.consumirMensaje()
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.pantalla_registro),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(R.string.registro_descripcion),
                style = MaterialTheme.typography.bodySmall
            )

            SelectorAcceso(
                accesoId = estado.accesoId,
                onAcceso = registroViewModel::onAccesoChange,
                error = estado.errores.acceso
            )

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

            // Personas por género: el popup pide la característica etaria
            Text(
                stringResource(R.string.registro_personas_observadas),
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                generosCapturables.forEach { genero ->
                    BotonGenero(
                        genero = genero,
                        cantidad = estado.cantidad(genero),
                        onClick = {
                            rangoElegido = null
                            generoPendiente = genero
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            ErrorTexto(estado.errores.conteo)

            if (estado.gruposRegistrados.isNotEmpty()) {
                DetalleConteo(
                    grupos = estado.gruposRegistrados,
                    onAgregar = registroViewModel::agregarPersona,
                    onQuitar = registroViewModel::quitarPersona
                )
            }

            TarjetaNinos(
                ninos = estado.ninosAcompanantes,
                onAgregar = registroViewModel::agregarNino,
                onQuitar = registroViewModel::quitarNino
            )
            ErrorTexto(estado.errores.ninos)

            TarjetaTotales(
                totalPersonas = estado.totalPersonas,
                ninos = estado.ninosAcompanantes,
                totalConNinos = estado.totalConNinos,
                resumenCompra = if (esSalida && estado.totalPersonas > 0) {
                    stringResource(
                        R.string.registro_resumen_compra,
                        compradores,
                        noCompradores,
                        sinDatoCompra
                    )
                } else {
                    null
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = registroViewModel::reiniciarConteo,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.registro_limpiar))
                }
                Button(
                    onClick = registroViewModel::guardar,
                    enabled = estado.totalPersonas > 0,
                    modifier = Modifier.weight(2f)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.registro_guardar, estado.totalConNinos))
                }
            }
            Text(
                stringResource(R.string.registro_pie),
                style = MaterialTheme.typography.bodySmall
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }

    generoPendiente?.let { genero ->
        PopupCaracteristica(
            genero = genero,
            onSeleccion = { rango -> rangoElegido = rango },
            onCerrar = {
                val rango = rangoElegido
                generoPendiente = null
                rangoElegido = null
                if (rango != null) {
                    if (esSalida) {
                        compraPendiente = GrupoConteo(genero, rango)
                    } else {
                        registroViewModel.agregarPersona(genero, rango)
                    }
                }
            }
        )
    }

    compraPendiente?.let { grupo ->
        PopupCompra(
            grupo = grupo,
            onSeleccion = { compro ->
                registroViewModel.agregarPersona(grupo.genero, grupo.rango, compro)
            },
            onCerrar = { compraPendiente = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorAcceso(accesoId: Int, onAcceso: (Int) -> Unit, error: String?) {
    Text(stringResource(R.string.registro_acceso), style = MaterialTheme.typography.titleSmall)
    Text(
        stringResource(R.string.registro_acceso_ayuda),
        style = MaterialTheme.typography.bodySmall
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Accesos.ids.forEach { id ->
            FilterChip(
                selected = accesoId == id,
                onClick = { onAcceso(id) },
                label = { Text(Accesos.etiquetaCorta(id)) }
            )
        }
    }
    ErrorTexto(error)
}

@Composable
private fun DetalleConteo(
    grupos: List<Pair<GrupoConteo, Int>>,
    onAgregar: (Genero, String, Boolean?) -> Unit,
    onQuitar: (Genero, String, Boolean?) -> Unit
) {
    Text(stringResource(R.string.registro_detalle_conteo), style = MaterialTheme.typography.titleSmall)
    grupos.forEach { (grupo, cantidad) ->
        FilaConteo(
            grupo = grupo,
            cantidad = cantidad,
            onAgregar = { onAgregar(grupo.genero, grupo.rango, grupo.compro) },
            onQuitar = { onQuitar(grupo.genero, grupo.rango, grupo.compro) }
        )
    }
}

@Composable
private fun TarjetaTotales(
    totalPersonas: Int,
    ninos: Int,
    totalConNinos: Int,
    resumenCompra: String?
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DatoNumerico(stringResource(R.string.etiqueta_personas), totalPersonas)
                DatoNumerico(stringResource(R.string.etiqueta_ninos), ninos)
                DatoNumerico(stringResource(R.string.etiqueta_total), totalConNinos)
            }
            if (resumenCompra != null) {
                Text(resumenCompra, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BotonGenero(
    genero: Genero,
    cantidad: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(ALTO_BOTON_GENERO).clickable(onClick = onClick),
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
            Text("$cantidad", style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.registro_toca_para_sumar),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** Fila del detalle: permite corregir el conteo de una combinación ya registrada. */
@Composable
private fun FilaConteo(
    grupo: GrupoConteo,
    cantidad: Int,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconoGenero(grupo.genero),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                grupo.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onQuitar, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Remove,
                    stringResource(R.string.registro_quitar_una),
                    Modifier.size(18.dp)
                )
            }
            Text("$cantidad", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onAgregar, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Add,
                    stringResource(R.string.registro_sumar_una),
                    Modifier.size(18.dp)
                )
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
                Text(
                    stringResource(R.string.etiqueta_ninos_acompanantes),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    stringResource(R.string.registro_ninos_ayuda),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onQuitar, enabled = ninos > 0) {
                Icon(
                    Icons.Filled.Remove,
                    stringResource(R.string.registro_quitar_una),
                    Modifier.size(20.dp)
                )
            }
            Text("$ninos", style = MaterialTheme.typography.headlineSmall)
            FilledTonalButton(
                onClick = onAgregar,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.registro_ninos_sumar))
            }
        }
    }
}
