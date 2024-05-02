package com.julianczaja.esp_monitoring_app

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun EspMonitoringNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    startDestination: String = devicesNavigationRoute,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        devicesScreen(
            navigateToDevice = navController::navigateToDevice,
            navigateToRemoveDevice = navController::navigateToRemoveDeviceDialog
        )
        deviceScreen(
            snackbarHostState = snackbarHostState,
            navigateToPhotoPreview = navController::navigateToPhotoPreview,
            navigateToRemovePhotoDialog = navController::navigateToRemovePhotoDialog
        )
        addNewDeviceDialog(
            onDismiss = onBackClick
        )
        removeDeviceDialog(
            onDismiss = onBackClick
        )
        photoPreviewDialog(
            onDismiss = onBackClick
        )
        removePhotoDialog(
            onDismiss = onBackClick
        )
    }
}
