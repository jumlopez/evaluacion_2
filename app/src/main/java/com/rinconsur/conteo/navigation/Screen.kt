package com.rinconsur.conteo.navigation

import androidx.annotation.StringRes
import com.rinconsur.conteo.R

/**
 * Rutas tipo-safe de la app (Guía 10). Los títulos se guardan como recursos de
 * texto para que la UI los resuelva con `stringResource`; [tituloCortoRes] es la
 * etiqueta que se muestra en la barra inferior.
 */
sealed class Screen(
    val route: String,
    @StringRes val tituloRes: Int,
    @StringRes val tituloCortoRes: Int = tituloRes
) {
    data object Dashboard : Screen(
        "dashboard_page",
        R.string.pantalla_dashboard,
        R.string.pantalla_corta_dashboard
    )

    data object Registro : Screen(
        "registro_page",
        R.string.pantalla_registro,
        R.string.pantalla_corta_registro
    )

    data object Porticos : Screen(
        "porticos_page",
        R.string.pantalla_porticos,
        R.string.pantalla_corta_porticos
    )

    data object Modo : Screen("modo_page", R.string.pantalla_modo)

    data object Analisis : Screen("analisis_page", R.string.pantalla_analisis)

    data object Perfil : Screen(
        "perfil_page",
        R.string.pantalla_perfil,
        R.string.pantalla_corta_perfil
    )

    companion object {
        /** Destinos de primer nivel que aparecen en el menú lateral. */
        val menuLateral = listOf(Dashboard, Registro, Porticos, Modo, Perfil)

        /** Destinos de primer nivel que aparecen en la barra inferior. */
        val barraInferior = listOf(Dashboard, Registro, Porticos, Perfil)

        /** Todos los destinos registrados en el NavHost. */
        private val todos = listOf(Dashboard, Registro, Porticos, Modo, Analisis, Perfil)

        fun desdeRuta(ruta: String?): Screen? = todos.firstOrNull { it.route == ruta }
    }
}
