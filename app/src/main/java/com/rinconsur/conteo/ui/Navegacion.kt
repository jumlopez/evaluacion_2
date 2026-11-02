package com.rinconsur.conteo.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rinconsur.conteo.R
import com.rinconsur.conteo.viewmodel.RegistroViewModel
import kotlinx.coroutines.launch

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

        private val todos = listOf(Dashboard, Registro, Porticos, Modo, Analisis, Perfil)

        fun desdeRuta(ruta: String?): Screen? = todos.firstOrNull { it.route == ruta }
    }
}

private fun icono(screen: Screen): ImageVector = when (screen) {
    Screen.Dashboard -> Icons.Default.Home
    Screen.Registro -> Icons.Default.Add
    Screen.Porticos -> Icons.Default.MeetingRoom
    Screen.Modo -> Icons.Default.Settings
    Screen.Analisis -> Icons.Default.BarChart
    Screen.Perfil -> Icons.Default.Person
}

/**
 * Guías 10 y 11: NavHost + TopAppBar + NavigationDrawer + NavigationBar. La
 * navegación se hace directamente sobre el `NavController`; el ViewModel de
 * registro se comparte para no perder el conteo al cambiar de pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(registroViewModel: RegistroViewModel = viewModel()) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun irA(screen: Screen) {
        navController.navigate(screen.route) {
            popUpTo(Screen.Dashboard.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val entradaActual by navController.currentBackStackEntryAsState()
    val pantallaActual = Screen.desdeRuta(entradaActual?.destination?.route)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    stringResource(R.string.nav_encabezado_menu),
                    modifier = Modifier.padding(16.dp)
                )
                Screen.menuLateral.forEach { screen ->
                    val titulo = stringResource(screen.tituloRes)
                    NavigationDrawerItem(
                        label = { Text(titulo) },
                        icon = { Icon(icono(screen), contentDescription = titulo) },
                        selected = pantallaActual == screen,
                        onClick = {
                            scope.launch { drawerState.close() }
                            irA(screen)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            pantallaActual?.let { stringResource(it.tituloRes) }
                                ?: stringResource(R.string.nav_app)
                        )
                    },
                    navigationIcon = {
                        if (pantallaActual == Screen.Analisis) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.nav_volver)
                                )
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.nav_menu)
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (pantallaActual in Screen.barraInferior) {
                    NavigationBar {
                        Screen.barraInferior.forEach { screen ->
                            val titulo = stringResource(screen.tituloRes)
                            NavigationBarItem(
                                selected = pantallaActual == screen,
                                onClick = { irA(screen) },
                                label = { Text(stringResource(screen.tituloCortoRes)) },
                                icon = { Icon(icono(screen), contentDescription = titulo) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) { DashboardScreen() }
                composable(Screen.Registro.route) { RegistroScreen(registroViewModel) }
                composable(Screen.Porticos.route) { PorticosScreen() }
                composable(Screen.Modo.route) {
                    ModoScreen(onVerGraficos = { navController.navigate(Screen.Analisis.route) })
                }
                composable(
                    route = Screen.Analisis.route,
                    enterTransition = {
                        slideInHorizontally(tween(320)) { ancho -> ancho } +
                            fadeIn(tween(220))
                    },
                    exitTransition = { fadeOut(tween(160)) },
                    popEnterTransition = { fadeIn(tween(200)) },
                    popExitTransition = {
                        slideOutHorizontally(tween(260)) { ancho -> ancho } + fadeOut(tween(200))
                    }
                ) { AnalisisScreen() }
                composable(Screen.Perfil.route) { PerfilScreen() }
            }
        }
    }
}
