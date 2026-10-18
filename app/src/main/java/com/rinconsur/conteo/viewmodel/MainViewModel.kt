package com.rinconsur.conteo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.conteo.navigation.NavigationEvent
import com.rinconsur.conteo.navigation.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 8)
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    private fun emitir(evento: NavigationEvent) {
        viewModelScope.launch { _navigationEvents.emit(evento) }
    }

    fun navigateTo(screen: Screen, popUpTo: Screen? = null, inclusive: Boolean = false) {
        emitir(NavigationEvent.NavigateTo(route = screen, popUpToRoute = popUpTo, inclusive = inclusive))
    }

    /** Navegación entre destinos de primer nivel (menú lateral / barra inferior). */
    fun navigateTopLevel(screen: Screen) {
        emitir(
            NavigationEvent.NavigateTo(
                route = screen,
                popUpToRoute = Screen.Dashboard,
                singleTop = true,
                restoreState = true
            )
        )
    }

    fun navigateBack() = emitir(NavigationEvent.PopBackStack)

    fun navigateUp() = emitir(NavigationEvent.NavigateUp)
}
