package com.rinconsur.conteo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.conteo.RinconApp
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.ConteoPortico
import com.rinconsur.conteo.model.inicioDelDia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilaPorticoUi(
    val accesoId: Int,
    val entradas: String = "",
    val salidas: String = ""
)

data class PorticosUiState(
    val filas: List<FilaPorticoUi> = Accesos.ids.map { FilaPorticoUi(it) },
    val cargando: Boolean = true,
    val error: String? = null,
    val guardadoOk: Boolean = false
) {
    fun fila(accesoId: Int): FilaPorticoUi =
        filas.firstOrNull { it.accesoId == accesoId } ?: FilaPorticoUi(accesoId)
}

/**
 * El operador transcribe las lecturas diarias de los pórticos de cada acceso para
 * poder compararlas con lo registrado desde las cámaras.
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
                        entradas = conteo?.entradas?.toString() ?: "",
                        salidas = conteo?.salidas?.toString() ?: ""
                    )
                },
                cargando = false
            )
        }
    }

    fun onEntradasChange(accesoId: Int, valor: String) =
        actualizarFila(accesoId) { it.copy(entradas = soloDigitos(valor)) }

    fun onSalidasChange(accesoId: Int, valor: String) =
        actualizarFila(accesoId) { it.copy(salidas = soloDigitos(valor)) }

    private fun actualizarFila(accesoId: Int, transform: (FilaPorticoUi) -> FilaPorticoUi) {
        _estado.update { actual ->
            actual.copy(
                filas = actual.filas.map { if (it.accesoId == accesoId) transform(it) else it },
                error = null,
                guardadoOk = false
            )
        }
    }

    fun guardar() {
        val actual = _estado.value
        if (actual.filas.all { it.entradas.isBlank() && it.salidas.isBlank() }) {
            _estado.update { it.copy(error = "Ingresa al menos un conteo de pórtico") }
            return
        }

        viewModelScope.launch {
            actual.filas.forEach { fila ->
                val existente = idsExistentes[fila.accesoId]
                if (fila.entradas.isBlank() && fila.salidas.isBlank()) {
                    if (existente != null) repositorio.eliminarPortico(fila.accesoId, dia)
                } else {
                    repositorio.guardarPortico(
                        ConteoPortico(
                            id = existente ?: 0,
                            accesoId = fila.accesoId,
                            fechaMillis = dia,
                            entradas = fila.entradas.toIntOrNull() ?: 0,
                            salidas = fila.salidas.toIntOrNull() ?: 0,
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

    private fun soloDigitos(valor: String): String = valor.filter(Char::isDigit).take(6)
}
