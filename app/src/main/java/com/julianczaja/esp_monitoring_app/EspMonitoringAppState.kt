package com.julianczaja.esp_monitoring_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.julianczaja.esp_monitoring_app.navigation.DevicesScreen


@Composable
fun rememberEspMonitoringAppState(
    navController: NavHostController = rememberNavController(),
) = remember {
    EspMonitoringAppState(
        navController = navController
    )
}

@Stable
class EspMonitoringAppState(
    val navController: NavHostController,
) {
    private val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val shouldShowNavigationIcon: Boolean
        @Composable get() = !currentDestination.isDevicesScreen()

    fun onBackClick() {
        navController.popBackStack()
    }

    @Composable
    private fun NavDestination?.isDevicesScreen() = this?.hasRoute<DevicesScreen>() ?: false
}
