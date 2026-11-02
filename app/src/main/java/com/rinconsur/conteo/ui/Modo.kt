package com.rinconsur.conteo.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.conteo.R
import com.rinconsur.conteo.viewmodel.ModoViewModel
import com.rinconsur.conteo.viewmodel.PerfilViewModel

private val COLOR_ACTIVO = Color(0xFF2E7D32)
private val COLOR_INACTIVO = Color(0xFF1E88E5)
private const val ALTO_BOTON_PRINCIPAL_DP = 60
private const val ALTO_BOTON_SECUNDARIO_DP = 52
private const val TIPO_CONTENIDO_IMAGEN = "image/*"

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
        Cargando(modifier)
        return
    }

    // Estado local que sobrevive a rotaciones (rememberSaveable)
    var cambiosEnSesion by rememberSaveable { mutableIntStateOf(0) }

    val colorAnimado by animateColorAsState(
        targetValue = if (estaActivo) COLOR_ACTIVO else COLOR_INACTIVO,
        animationSpec = tween(durationMillis = 500),
        label = "colorModo"
    )

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.pantalla_modo), style = MaterialTheme.typography.headlineSmall)

        AnimatedContent(targetState = estaActivo, label = "descripcionModo") { activoAhora ->
            Text(
                stringResource(
                    if (activoAhora) R.string.modo_activo_descripcion
                    else R.string.modo_inactivo_descripcion
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
            Icon(Icons.Default.BarChart, contentDescription = null, Modifier.size(22.dp))
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
            enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { alto -> alto / 3 }
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

/** Guía 13: foto de perfil del operador desde galería (GetContent) o cámara (TakePicture). */
@Composable
fun PerfilScreen(modifier: Modifier = Modifier, viewModel: PerfilViewModel = viewModel()) {
    val contexto = LocalContext.current
    val imagenUri by viewModel.imagenUri.collectAsStateWithLifecycle()
    var mensaje by remember { mutableStateOf<String?>(null) }
    val mensajeSinPermiso = stringResource(R.string.perfil_sin_permiso)

    val lanzadorGaleria =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            viewModel.onImagenDeGaleria(uri)
        }
    val lanzadorCamara =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
            viewModel.onFotoCapturada(exito)
        }
    val lanzadorPermiso =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) {
                mensaje = null
                lanzadorCamara.launch(viewModel.crearUriParaCamara())
            } else {
                mensaje = mensajeSinPermiso
            }
        }

    fun abrirCamara() {
        val tienePermiso = ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (tienePermiso) lanzadorCamara.launch(viewModel.crearUriParaCamara())
        else lanzadorPermiso.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.pantalla_perfil), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        ImagenInteligente(uri = imagenUri)

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { lanzadorGaleria.launch(TIPO_CONTENIDO_IMAGEN) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.perfil_galeria))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { abrirCamara() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.perfil_camara))
        }

        mensaje?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
