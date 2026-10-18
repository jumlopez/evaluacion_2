package com.rinconsur.conteo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Transgender
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.GrupoConteo
import com.rinconsur.conteo.model.Movimiento
import com.rinconsur.conteo.model.RangosEtarios
import com.rinconsur.conteo.model.generosCapturables
import com.rinconsur.conteo.ui.components.DatoNumerico
import com.rinconsur.conteo.viewmodel.RegistroViewModel
import kotlinx.coroutines.delay

private const val DURACION_CIERRE_MS = 140L

private fun iconoGenero(genero: Genero): ImageVector = when (genero) {
    Genero.FEMENINO -> Icons.Filled.Female
    Genero.MASCULINO -> Icons.Filled.Male
    Genero.OTRO -> Icons.Filled.Transgender
    Genero.NO_DECLARA -> Icons.AutoMirrored.Filled.HelpOutline
}

private fun iconoRango(rango: String): ImageVector = when (rango) {
    RangosEtarios.JOVEN -> Icons.Filled.School
    RangosEtarios.ADULTO -> Icons.Filled.Work
    else -> Icons.Filled.Elderly
}

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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistroScreen(
    registroViewModel: RegistroViewModel,
    modifier: Modifier = Modifier
) {
    val estado by registroViewModel.estado.collectAsState()
    val mensaje by registroViewModel.mensaje.collectAsState()
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
            Text("Registro rápido", style = MaterialTheme.typography.titleLarge)
            Text(
                "Mira las cámaras: toca el género y elige la característica en el popup. " +
                    "En las salidas la app pregunta además si compró.",
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

            // Personas por género: el popup pide la característica etaria
            Text("Personas observadas", style = MaterialTheme.typography.titleSmall)
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

            // Detalle por género + característica (+ compra), con corrección manual
            if (estado.gruposRegistrados.isNotEmpty()) {
                Text("Detalle del conteo", style = MaterialTheme.typography.titleSmall)
                estado.gruposRegistrados.forEach { (grupo, cantidad) ->
                    FilaConteo(
                        grupo = grupo,
                        cantidad = cantidad,
                        onAgregar = {
                            registroViewModel.agregarPersona(grupo.genero, grupo.rango, grupo.compro)
                        },
                        onQuitar = {
                            registroViewModel.quitarPersona(grupo.genero, grupo.rango, grupo.compro)
                        }
                    )
                }
            }

            // Niños acompañantes
            TarjetaNinos(
                ninos = estado.ninosAcompanantes,
                onAgregar = registroViewModel::agregarNino,
                onQuitar = registroViewModel::quitarNino
            )
            ErrorTexto(estado.errores.ninos)

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        DatoNumerico("Personas", estado.totalPersonas)
                        DatoNumerico("Niños", estado.ninosAcompanantes)
                        DatoNumerico("Total", estado.totalConNinos)
                    }
                    if (esSalida && estado.totalPersonas > 0) {
                        Text(
                            "Compraron $compradores · No compraron $noCompradores · " +
                                "Sin dato $sinDatoCompra",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = registroViewModel::reiniciarConteo,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Limpiar")
                }
                Button(
                    onClick = registroViewModel::guardar,
                    enabled = estado.totalPersonas > 0,
                    modifier = Modifier.weight(2f)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar ${estado.totalConNinos}")
                }
            }
            Text(
                "Se guarda un registro por género, característica y compra en SQLite, " +
                    "sin salir de la pantalla.",
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

@Composable
private fun BotonGenero(
    genero: Genero,
    cantidad: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(132.dp).clickable(onClick = onClick),
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
            Text("Toca para sumar", style = MaterialTheme.typography.labelSmall)
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
                Icon(Icons.Filled.Remove, "Quitar una", Modifier.size(18.dp))
            }
            Text("$cantidad", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onAgregar, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Add, "Sumar una", Modifier.size(18.dp))
            }
        }
    }
}

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
            enter = fadeIn(tween(140)) + scaleIn(tween(180), initialScale = 0.85f),
            exit = fadeOut(tween(110)) + scaleOut(tween(120), targetScale = 0.9f)
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

/** Primer paso del popup: característica etaria de la persona observada. */
@Composable
private fun PopupCaracteristica(
    genero: Genero,
    onSeleccion: (String) -> Unit,
    onCerrar: () -> Unit
) {
    PopupAnimado(onDescartar = onCerrar) { cerrar ->
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconoGenero(genero),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Persona observada", style = MaterialTheme.typography.titleMedium)
                    Text(genero.etiqueta, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = cerrar) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                }
            }
            Text("¿Qué característica tiene?", style = MaterialTheme.typography.bodyMedium)
            RangosEtarios.opciones.forEach { rango ->
                FilledTonalButton(
                    onClick = {
                        onSeleccion(rango)
                        cerrar()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
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
private fun PopupCompra(
    grupo: GrupoConteo,
    onSeleccion: (Boolean?) -> Unit,
    onCerrar: () -> Unit
) {
    PopupAnimado(onDescartar = onCerrar) { cerrar ->
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconoGenero(grupo.genero),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("¿Compró?", style = MaterialTheme.typography.titleMedium)
                    Text(grupo.descripcion, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = cerrar) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                }
            }
            Button(
                onClick = {
                    onSeleccion(true)
                    cerrar()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sí, compró", style = MaterialTheme.typography.titleMedium)
            }
            FilledTonalButton(
                onClick = {
                    onSeleccion(false)
                    cerrar()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("No compró", style = MaterialTheme.typography.titleMedium)
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
                Text("Sin dato")
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
