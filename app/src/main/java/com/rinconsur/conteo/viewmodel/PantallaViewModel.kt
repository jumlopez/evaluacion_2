package com.rinconsur.conteo.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.conteo.RinconApp
import com.rinconsur.conteo.data.ModoDataStore
import com.rinconsur.conteo.model.CrucePorticos
import com.rinconsur.conteo.model.Periodo
import com.rinconsur.conteo.model.ResumenGeneral
import com.rinconsur.conteo.model.calcularCruce
import com.rinconsur.conteo.model.calcularResumen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class DashboardUiState(
    val cargando: Boolean = true,
    val resumen: ResumenGeneral = ResumenGeneral(emptyList(), emptyMap(), emptyMap(), 0),
    val cruce: CrucePorticos = CrucePorticos.vacio()
)

/** Observa el periodo elegido y recalcula resumen y cruce con pórticos. */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = (application as RinconApp).repositorio

    private val _periodo = MutableStateFlow(Periodo.SIETE_DIAS)
    val periodo: StateFlow<Periodo> = _periodo.asStateFlow()

    val estado: StateFlow<DashboardUiState> =
        combine(
            _periodo.flatMapLatest { repositorio.observar(it) },
            _periodo.flatMapLatest { repositorio.observarPorticos(it) }
        ) { registros, porticos ->
            DashboardUiState(
                cargando = false,
                resumen = calcularResumen(registros),
                cruce = calcularCruce(registros, porticos)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun cambiarPeriodo(nuevo: Periodo) {
        _periodo.value = nuevo
    }
}

/**
 * Guía 12: estado persistente con DataStore + feedback animado. Cuando el modo
 * análisis está activo, el dashboard destaca el perfil (género, característica
 * etaria, niños acompañantes y compra en las salidas) además del cruce con los
 * pórticos, y desde esta pantalla se abre la vista de gráficos.
 *
 * El estado se lee desde DataStore, así Dashboard y ModoScreen (cada una con su
 * propia instancia del ViewModel) siempre ven el mismo valor.
 */
class ModoViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = ModoDataStore(application)

    /** null = cargando (muestra CircularProgressIndicator). */
    val activo: StateFlow<Boolean?> = dataStore.obtenerEstado()
        .map { it ?: false }
        .onStart { delay(1_000) } // simula demora para mostrar el loader
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _mostrarMensaje = MutableStateFlow(false)
    val mostrarMensaje: StateFlow<Boolean> = _mostrarMensaje.asStateFlow()

    fun activar() {
        if (activo.value == true) return
        viewModelScope.launch {
            dataStore.guardarEstado(true)
            mostrarFeedback()
        }
    }

    fun desactivar() {
        if (activo.value == false) return
        viewModelScope.launch {
            dataStore.guardarEstado(false)
            mostrarFeedback()
        }
    }

    private suspend fun mostrarFeedback() {
        _mostrarMensaje.value = true
        delay(2_000)
        _mostrarMensaje.value = false
    }
}

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
