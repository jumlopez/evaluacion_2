package com.rinconsur.conteo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.conteo.RinconApp
import com.rinconsur.conteo.R
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.MAX_NINOS_ACOMPANANTES
import com.rinconsur.conteo.model.Movimiento
import com.rinconsur.conteo.model.RegistroUiState
import com.rinconsur.conteo.model.construirRegistros
import com.rinconsur.conteo.model.validarRegistro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Captura rápida: el operador toca el género, elige la característica etaria en el
 * primer popup y, si es una salida, responde en el segundo si compró. La app suma
 * una persona a esa combinación. El guardado va directo a Room/SQLite, sin paso de
 * confirmación para no frenar al operador.
 */
class RegistroViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = (application as RinconApp).repositorio

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado: StateFlow<RegistroUiState> = _estado.asStateFlow()

    /** Mensaje de confirmación del último guardado (se consume desde la UI). */
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun onAccesoChange(id: Int) = _estado.update {
        it.copy(accesoId = id, errores = it.errores.copy(acceso = null))
    }

    fun onMovimientoChange(movimiento: Movimiento) = _estado.update {
        it.conMovimiento(movimiento)
    }

    /** Un recorrido por los popups (característica y compra) = una persona observada. */
    fun agregarPersona(genero: Genero, rango: String, compro: Boolean? = null) =
        _estado.update {
            it.conCantidad(genero, rango, it.cantidad(genero, rango, compro) + 1, compro)
        }

    fun quitarPersona(genero: Genero, rango: String, compro: Boolean? = null) =
        _estado.update {
            it.conCantidad(genero, rango, it.cantidad(genero, rango, compro) - 1, compro)
        }

    fun agregarNino() = _estado.update {
        it.copy(
            ninosAcompanantes = (it.ninosAcompanantes + 1).coerceAtMost(MAX_NINOS_ACOMPANANTES),
            errores = it.errores.copy(ninos = null)
        )
    }

    fun quitarNino() = _estado.update {
        it.copy(ninosAcompanantes = (it.ninosAcompanantes - 1).coerceAtLeast(0))
    }

    /** Limpia el grupo en captura sin cambiar el acceso ni el movimiento. */
    fun reiniciarConteo() = _estado.update { it.limpiarConteo() }

    fun guardar() {
        val actual = _estado.value
        val errores = validarRegistro(actual)
        if (errores.hayErrores()) {
            _estado.update { it.copy(errores = errores) }
            _mensaje.value = null
            return
        }

        val registros = construirRegistros(actual, System.currentTimeMillis())
        viewModelScope.launch {
            repositorio.guardarTodos(registros)
            _mensaje.value = getApplication<Application>().getString(
                R.string.registro_guardado,
                actual.totalConNinos,
                Accesos.etiqueta(actual.accesoId)
            )
            _estado.update { it.limpiarConteo() }
        }
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }
}
