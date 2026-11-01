package com.rinconsur.conteo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.conteo.RinconApp
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.Movimiento
import com.rinconsur.conteo.model.RegistroUiState
import com.rinconsur.conteo.model.construirRegistros
import com.rinconsur.conteo.model.validarRegistro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Captura rápida: cada toque suma una persona por género; "Guardar" lo persiste en Room/SQLite. */
class RegistroViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = (application as RinconApp).repositorio

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado: StateFlow<RegistroUiState> = _estado.asStateFlow()

    /** Mensaje de confirmación del último guardado (se consume desde la UI). */
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    /** Evita filas duplicadas por doble toque mientras se inserta. */
    private var guardando = false

    fun onAccesoChange(id: Int) = _estado.update {
        it.copy(accesoId = id, errores = it.errores.copy(acceso = null))
    }

    fun onMovimientoChange(movimiento: Movimiento) = _estado.update {
        it.copy(
            movimiento = movimiento,
            compro = if (movimiento == Movimiento.SALIDA) null else it.compro
        )
    }

    fun onRangoChange(rango: String) = _estado.update {
        it.copy(rangoEtario = rango, errores = it.errores.copy(rango = null))
    }

    fun onComproChange(compro: Boolean?) = _estado.update { it.copy(compro = compro) }

    /** Un toque = una persona observada en las cámaras. */
    fun agregarPersona(genero: Genero) = _estado.update {
        it.conCantidad(genero, it.cantidad(genero) + 1)
    }

    fun quitarPersona(genero: Genero) = _estado.update {
        it.conCantidad(genero, it.cantidad(genero) - 1)
    }

    fun agregarNino() = _estado.update {
        it.copy(
            ninosAcompanantes = (it.ninosAcompanantes + 1).coerceAtMost(20),
            errores = it.errores.copy(ninos = null)
        )
    }

    fun quitarNino() = _estado.update {
        it.copy(ninosAcompanantes = (it.ninosAcompanantes - 1).coerceAtLeast(0))
    }

    /** Limpia el grupo en captura sin cambiar el acceso ni el movimiento. */
    fun reiniciarConteo() = _estado.update {
        RegistroUiState(accesoId = it.accesoId, movimiento = it.movimiento)
    }

    fun guardar() {
        if (guardando) return
        val actual = _estado.value
        val errores = validarRegistro(actual)
        if (errores.hayErrores()) {
            _estado.update { it.copy(errores = errores) }
            _mensaje.value = null
            return
        }

        val registros = construirRegistros(actual, System.currentTimeMillis())
        guardando = true
        viewModelScope.launch {
            try {
                repositorio.guardarTodos(registros)
                _mensaje.value = "Guardado: ${actual.totalConNinos} personas · " +
                    Accesos.etiqueta(actual.accesoId)
                reiniciarConteo()
            } finally {
                guardando = false
            }
        }
    }

    fun consumirMensaje() { _mensaje.value = null }
}
