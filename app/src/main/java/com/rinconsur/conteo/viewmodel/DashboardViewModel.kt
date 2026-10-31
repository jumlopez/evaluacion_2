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
    val resumen: ResumenGeneral = calcularResumen(emptyList()),
    val cruce: CrucePorticos = calcularCruce(resumen, emptyList())
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = (application as RinconApp).repositorio

    private val _periodo = MutableStateFlow(Periodo.SIETE_DIAS)
    val periodo: StateFlow<Periodo> = _periodo.asStateFlow()

    val estado: StateFlow<DashboardUiState> = _periodo
        .flatMapLatest { p ->
            combine(repositorio.observar(p), repositorio.observarPorticos(p)) { registros, porticos ->
                val resumen = calcularResumen(registros)
                DashboardUiState(cargando = false, resumen = resumen, cruce = calcularCruce(resumen, porticos))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun cambiarPeriodo(nuevo: Periodo) { _periodo.value = nuevo }
}
