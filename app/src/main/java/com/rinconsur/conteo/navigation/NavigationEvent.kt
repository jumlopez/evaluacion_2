package com.rinconsur.conteo.navigation

/** Eventos de navegación emitidos por el ViewModel y ejecutados por la UI (Guía 10). */
sealed class NavigationEvent {

    data class NavigateTo(
        val route: Screen,
        val popUpToRoute: Screen? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false,
        val restoreState: Boolean = false
    ) : NavigationEvent()

    data object PopBackStack : NavigationEvent()

    data object NavigateUp : NavigationEvent()
}
