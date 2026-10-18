package com.rinconsur.conteo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.conteo.data.preferences.ModoDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Guía 12: estado persistente con DataStore + feedback animado.
 * "Modo análisis": cuando está activo, el dashboard destaca el perfil por género y edad
 * de las entradas, además de los niños acompañantes.
 *
 * El estado se observa directamente desde DataStore, así Dashboard y ModoScreen
 * (cada una con su propia instancia del ViewModel) siempre ven el mismo valor.
 */
class ModoViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = ModoDataStore(application)

    // null = cargando (muestra CircularProgressIndicator)
    val activo: StateFlow<Boolean?> = dataStore.obtenerEstado()
        .map { it ?: false }
        .onStart { delay(1_000) } // simula demora para mostrar el loader
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _mostrarMensaje = MutableStateFlow(false)
    val mostrarMensaje: StateFlow<Boolean> = _mostrarMensaje.asStateFlow()

    fun alternarEstado() {
        viewModelScope.launch {
            val nuevo = !(activo.value ?: false)
            dataStore.guardarEstado(nuevo) // el flujo de DataStore actualiza 'activo'

            _mostrarMensaje.value = true
            delay(2_000)
            _mostrarMensaje.value = false
        }
    }
}
