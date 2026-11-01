package com.rinconsur.conteo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.conteo.RinconApp
import com.rinconsur.conteo.R
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.ConteoPortico
import com.rinconsur.conteo.model.inicioDelDia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_DIGITOS_PORTICO = 6

/** Personas que el pórtico marcó en un acceso. Un solo total, sin entrada/salida. */
data class FilaPorticoUi(
    val accesoId: Int,
    val personas: String = ""
)

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
 * el pórtico no distingue entrada de salida, se guarda solo el total de personas
 * que transitaron por cada acceso.
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
                    FilaPorticoUi(
                        accesoId = id,
                        personas = conteo?.personas?.toString() ?: ""
                    )
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
