package com.rinconsur.conteo.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** Guía 13: URI de la imagen elegida (galería) o capturada (cámara). */
class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val _imagenUri = MutableStateFlow<Uri?>(null)
    val imagenUri: StateFlow<Uri?> = _imagenUri.asStateFlow()

    private var uriPendienteCamara: Uri? = null

    /** Crea el archivo temporal (FileProvider) donde la cámara dejará la foto. */
    fun crearUriParaCamara(): Uri {
        val contexto = getApplication<Application>()
        val carpeta = File(contexto.cacheDir, "images").apply { mkdirs() }
        val archivo = File.createTempFile("perfil_", ".jpg", carpeta)
        return FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
            .also { uriPendienteCamara = it }
    }

    fun onImagenDeGaleria(uri: Uri?) {
        if (uri != null) _imagenUri.value = uri // si el usuario cancela, uri == null y no pasa nada
    }

    fun onFotoCapturada(exito: Boolean) {
        if (exito) _imagenUri.value = uriPendienteCamara
        uriPendienteCamara = null // si se cancela, se conserva la imagen anterior
    }
}
