package com.rinconsur.conteo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.conteo.ui.components.ImagenInteligente
import com.rinconsur.conteo.viewmodel.PerfilViewModel

/** Guía 13: foto de perfil del operador desde galería (GetContent) o cámara (TakePicture). */
@Composable
fun PerfilScreen(
    modifier: Modifier = Modifier,
    viewModel: PerfilViewModel = viewModel()
) {
    val contexto = LocalContext.current
    val imagenUri by viewModel.imagenUri.collectAsStateWithLifecycle()
    var mensaje by remember { mutableStateOf<String?>(null) }

    val lanzadorGaleria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.onImagenDeGaleria(uri)
    }
    val lanzadorCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        viewModel.onFotoCapturada(exito)
    }
    val lanzadorPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            mensaje = null
            lanzadorCamara.launch(viewModel.crearUriParaCamara())
        } else {
            mensaje = "Sin permiso de cámara no se puede tomar la foto."
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
        Text("Perfil del operador", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        ImagenInteligente(uri = imagenUri)

        Spacer(Modifier.height(24.dp))
        Button(onClick = { lanzadorGaleria.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Text("Elegir desde galería")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { abrirCamara() }, modifier = Modifier.fillMaxWidth()) {
            Text("Tomar foto con la cámara")
        }

        mensaje?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
