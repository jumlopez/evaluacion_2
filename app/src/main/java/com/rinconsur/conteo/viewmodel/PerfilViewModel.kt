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

    // Temporales de cámara: el pendiente y el que se muestra; se borran cuando dejan de usarse.
    private var archivoPendiente: File? = null
    private var archivoMostrado: File? = null

    /** Crea el archivo temporal (FileProvider) donde la cámara dejará la foto. */
    fun crearUriParaCamara(): Uri {
        val contexto = getApplication<Application>()
        val carpeta = File(contexto.cacheDir, "images").apply { mkdirs() }
        val archivo = File.createTempFile("perfil_", ".jpg", carpeta).also { archivoPendiente = it }
        return FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
            .also { uriPendienteCamara = it }
    }

    fun onImagenDeGaleria(uri: Uri?) {
        if (uri == null) return // si el usuario cancela, no pasa nada
        _imagenUri.value = uri
        archivoMostrado?.delete()
        archivoMostrado = null
    }

    fun onFotoCapturada(exito: Boolean) {
        if (exito) {
            _imagenUri.value = uriPendienteCamara
            archivoMostrado?.delete()
            archivoMostrado = archivoPendiente
        } else {
            archivoPendiente?.delete() // si se cancela, se conserva la imagen anterior
        }
        uriPendienteCamara = null
        archivoPendiente = null
    }

    override fun onCleared() {
        archivoPendiente?.delete()
        archivoMostrado?.delete()
    }
}
