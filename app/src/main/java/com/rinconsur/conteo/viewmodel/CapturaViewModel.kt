package com.rinconsur.conteo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.conteo.R
import com.rinconsur.conteo.RinconApp
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.ConteoPortico
import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.MAX_NINOS_ACOMPANANTES
import com.rinconsur.conteo.model.Movimiento
import com.rinconsur.conteo.model.RegistroUiState
import com.rinconsur.conteo.model.construirRegistros
import com.rinconsur.conteo.model.inicioDelDia
import com.rinconsur.conteo.model.validarRegistro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Captura rápida: el operador toca el género, elige la característica etaria en el
 * primer popup y, si es una salida, responde en el segundo si compró. El guardado va
 * directo a Room/SQLite, sin paso de confirmación para no frenar al operador.
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

private const val MAX_DIGITOS_PORTICO = 6

/** Personas que el pórtico marcó en un acceso. Un solo total, sin entrada/salida. */
data class FilaPorticoUi(val accesoId: Int, val personas: String = "")

data class PorticosUiState(
    val filas: List<FilaPorticoUi> = Accesos.ids.map { FilaPorticoUi(it) },
    val cargando: Boolean = true,
    val error: String? = null,
    val guardadoOk: Boolean = false
) {
    fun fila(accesoId: Int): FilaPorticoUi =
        filas.firstOrNull { it.accesoId == accesoId } ?: FilaPorticoUi(accesoId)

    val totalPersonas: Int get() = filas.sumOf { it.personas.toIntOrNull() ?: 0 }
}

/**
 * El operador transcribe las lecturas diarias de los pórticos de cada acceso. Como
 * el pórtico no distingue entrada de salida, se guarda solo el total de personas que
 * transitaron por cada acceso.
 */
class ConteoPorticosViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = (application as RinconApp).repositorio
    private val dia: Long = inicioDelDia(System.currentTimeMillis())
    private var idsExistentes: Map<Int, Long> = emptyMap()

    private val _estado = MutableStateFlow(PorticosUiState())
    val estado: StateFlow<PorticosUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val guardados = repositorio.porticosDelDia(dia)
            idsExistentes = guardados.associate { it.accesoId to it.id }
            _estado.value = PorticosUiState(
                filas = Accesos.ids.map { id ->
                    val conteo = guardados.firstOrNull { it.accesoId == id }
                    FilaPorticoUi(accesoId = id, personas = conteo?.personas?.toString() ?: "")
                },
                cargando = false
            )
        }
    }

    fun onPersonasChange(accesoId: Int, valor: String) = _estado.update { actual ->
        actual.copy(
            filas = actual.filas.map {
                if (it.accesoId == accesoId) it.copy(personas = soloDigitos(valor)) else it
            },
            error = null,
            guardadoOk = false
        )
    }

    fun guardar() {
        val actual = _estado.value
        if (actual.filas.all { it.personas.isBlank() }) {
            _estado.update {
                it.copy(
                    error = getApplication<Application>()
                        .getString(R.string.porticos_error_sin_conteo)
                )
            }
            return
        }

        viewModelScope.launch {
            actual.filas.forEach { fila ->
                val existente = idsExistentes[fila.accesoId]
                if (fila.personas.isBlank()) {
                    if (existente != null) repositorio.eliminarPortico(fila.accesoId, dia)
                } else {
                    repositorio.guardarPortico(
                        ConteoPortico(
                            id = existente ?: 0,
                            accesoId = fila.accesoId,
                            fechaMillis = dia,
                            personas = fila.personas.toIntOrNull() ?: 0,
                            actualizadoEn = System.currentTimeMillis()
                        )
                    )
                }
            }
            idsExistentes = repositorio.porticosDelDia(dia).associate { it.accesoId to it.id }
            _estado.update { it.copy(guardadoOk = true) }
        }
    }

    fun consumirGuardado() {
        _estado.update { it.copy(guardadoOk = false) }
    }

    private fun soloDigitos(valor: String): String =
        valor.filter(Char::isDigit).take(MAX_DIGITOS_PORTICO)
}
