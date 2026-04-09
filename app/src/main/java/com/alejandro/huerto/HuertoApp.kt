package com.alejandro.huerto

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alejandro.huerto.ui.screens.ConfigScreen
import com.alejandro.huerto.ui.screens.ControlScreen
import com.alejandro.huerto.ui.screens.DashboardScreen
import com.alejandro.huerto.ui.screens.LogsScreen
import com.alejandro.huerto.ui.screens.TemperatureHistoryScreen
import com.alejandro.huerto.ui.theme.HuertoTheme

private const val DASHBOARD_ROUTE = "dashboard"
private const val CONTROL_ROUTE = "control"
private const val CONFIG_ROUTE = "config"
private const val LOGS_ROUTE = "logs"
private const val TEMPERATURE_HISTORY_ROUTE = "temperatureHistory"

data class HuertoDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomDestinations = listOf(
    HuertoDestination(DASHBOARD_ROUTE, "Dashboard", Icons.Default.Dashboard),
    HuertoDestination(CONTROL_ROUTE, "Control", Icons.Default.AutoAwesome),
    HuertoDestination(CONFIG_ROUTE, "Config", Icons.Default.Settings),
    HuertoDestination(LOGS_ROUTE, "Historial", Icons.Default.History)
)

@Composable
fun HuertoApp() {
    HuertoTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val bottomRoutes = bottomDestinations.map { it.route }
        val showBottomBar = currentDestination?.route in bottomRoutes

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentDestination.isRoute(destination.route),
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = DASHBOARD_ROUTE,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(DASHBOARD_ROUTE) {
                    DashboardScreen(onViewTemperatureHistory = {
                        navController.navigate(TEMPERATURE_HISTORY_ROUTE)
                    })
                }
                composable(CONTROL_ROUTE) { ControlScreen() }
                composable(CONFIG_ROUTE) { ConfigScreen() }
                composable(LOGS_ROUTE) { LogsScreen() }
                composable(TEMPERATURE_HISTORY_ROUTE) {
                    TemperatureHistoryScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}

private fun NavDestination?.isRoute(route: String): Boolean = this?.route == route
