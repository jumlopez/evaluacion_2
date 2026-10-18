package com.rinconsur.conteo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.conteo.RinconApp
import com.rinconsur.conteo.model.CrucePorticos
import com.rinconsur.conteo.model.Periodo
import com.rinconsur.conteo.model.ResumenGeneral
import com.rinconsur.conteo.model.calcularCruce
import com.rinconsur.conteo.model.calcularResumen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val cargando: Boolean = true,
    val resumen: ResumenGeneral = ResumenGeneral(emptyList(), emptyMap(), emptyMap(), 0),
    val cruce: CrucePorticos = CrucePorticos.vacio()
)

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
