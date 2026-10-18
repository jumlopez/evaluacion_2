package com.rinconsur.conteo.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rinconsur.conteo.ui.screens.DashboardScreen
import com.rinconsur.conteo.ui.screens.ModoScreen
import com.rinconsur.conteo.ui.screens.PerfilScreen
import com.rinconsur.conteo.ui.screens.PorticosScreen
import com.rinconsur.conteo.ui.screens.RegistroScreen
import com.rinconsur.conteo.viewmodel.MainViewModel
import com.rinconsur.conteo.viewmodel.RegistroViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private fun icono(screen: Screen): ImageVector = when (screen) {
    Screen.Dashboard -> Icons.Default.Home
    Screen.Registro -> Icons.Default.Add
    Screen.Porticos -> Icons.Default.MeetingRoom
    Screen.Modo -> Icons.Default.Settings
    Screen.Perfil -> Icons.Default.Person
}

/**
 * Guías 10 y 11: NavHost + TopAppBar + NavigationDrawer + NavigationBar.
 * Los ViewModels (navegación y registro) se crean una sola vez y se comparten entre pantallas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    mainViewModel: MainViewModel = viewModel(),
    registroViewModel: RegistroViewModel = viewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Ejecuta los eventos de navegación emitidos por el MainViewModel
    LaunchedEffect(Unit) {
        mainViewModel.navigationEvents.collectLatest { evento ->
            when (evento) {
                is NavigationEvent.NavigateTo -> navController.navigate(evento.route.route) {
                    evento.popUpToRoute?.let { destino ->
                        popUpTo(destino.route) {
                            inclusive = evento.inclusive
                            saveState = evento.restoreState
                        }
                    }
                    launchSingleTop = evento.singleTop
                    restoreState = evento.restoreState
                }

                NavigationEvent.PopBackStack -> navController.popBackStack()
                NavigationEvent.NavigateUp -> navController.navigateUp()
            }
        }
    }

    val entradaActual by navController.currentBackStackEntryAsState()
    val pantallaActual = Screen.desdeRuta(entradaActual?.destination?.route)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Rincón Sur · Flujo", modifier = Modifier.padding(16.dp))
                Screen.menuLateral.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.titulo) },
                        icon = { Icon(icono(screen), contentDescription = screen.titulo) },
                        selected = pantallaActual == screen,
                        onClick = {
                            scope.launch { drawerState.close() }
                            mainViewModel.navigateTopLevel(screen)
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
                    title = { Text(pantallaActual?.titulo ?: "Rincón Sur") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    }
                )
            },
            bottomBar = {
                if (pantallaActual in Screen.barraInferior) {
                    NavigationBar {
                        Screen.barraInferior.forEach { screen ->
                            NavigationBarItem(
                                selected = pantallaActual == screen,
                                onClick = { mainViewModel.navigateTopLevel(screen) },
                                label = { Text(screen.titulo.substringBefore(" ")) },
                                icon = { Icon(icono(screen), contentDescription = screen.titulo) }
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
                composable(Screen.Modo.route) { ModoScreen() }
                composable(Screen.Perfil.route) { PerfilScreen() }
            }
        }
    }
}
