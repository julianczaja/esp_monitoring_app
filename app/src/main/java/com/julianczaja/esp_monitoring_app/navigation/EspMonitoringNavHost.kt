package com.julianczaja.esp_monitoring_app.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.julianczaja.esp_monitoring_app.presentation.addeditdevice.AddEditDeviceScreen
import com.julianczaja.esp_monitoring_app.presentation.devices.DevicesScreen

@Composable
fun EspMonitoringNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = DevicesScreen
    ) {
        composable<DevicesScreen> {
            DevicesScreen(
                navController::navigateToDevice,
                navController::navigateToRemoveDeviceDialog,
                navController::navigateToAddEditDeviceScreen,
                navController::navigateToAddEditDeviceScreen
            )
        }
        deviceScreen(
            snackbarHostState = snackbarHostState,
            navigateToPhotoPreview = navController::navigateToPhotoPreview,
            navigateToRemovePhotoDialog = navController::navigateToRemovePhotoDialog
        )
        composable<AddEditDeviceScreen> {
            AddEditDeviceScreen(onDismiss = onBackClick)
        }
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
