package com.rinconsur.conteo.navigation

/** Rutas tipo-safe de la app (Guía 10). */
sealed class Screen(val route: String, val titulo: String) {
    data object Dashboard : Screen("dashboard_page", "Flujo por acceso")
    data object Registro : Screen("registro_page", "Registro rápido")
    data object Porticos : Screen("porticos_page", "Conteos de pórticos")
    data object Modo : Screen("modo_page", "Modo análisis")
    data object Perfil : Screen("perfil_page", "Perfil del operador")

    companion object {
        /** Destinos de primer nivel que aparecen en el menú lateral. */
        val menuLateral = listOf(Dashboard, Registro, Porticos, Modo, Perfil)

        /** Destinos de primer nivel que aparecen en la barra inferior. */
        val barraInferior = listOf(Dashboard, Registro, Porticos, Perfil)

        fun desdeRuta(ruta: String?): Screen? = menuLateral.firstOrNull { it.route == ruta }
    }
}
