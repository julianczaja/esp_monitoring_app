package com.julianczaja.esp_monitoring_app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.julianczaja.esp_monitoring_app.presentation.addeditdevice.AddEditDeviceScreen
import com.julianczaja.esp_monitoring_app.presentation.device.DeviceScreen
import com.julianczaja.esp_monitoring_app.presentation.devices.DevicesScreen
import com.julianczaja.esp_monitoring_app.presentation.photopreview.PhotoPreviewDialog
import com.julianczaja.esp_monitoring_app.presentation.removedevice.RemoveDeviceDialog
import com.julianczaja.esp_monitoring_app.presentation.removephoto.RemovePhotoDialog

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
                navigateToDevice = navController::navigateToDevice,
                navigateToRemoveDevice = navController::navigateToRemoveDeviceDialog,
                navigateToAddDevice = navController::navigateToAddEditDeviceScreen,
                navigateToEditDevice = navController::navigateToAddEditDeviceScreen
            )
        }
        composable<DeviceScreen>(
            enterTransition = {
                slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = AnimatedContentTransitionScope.SlideDirection.Start
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    animationSpec = tween(300, easing = EaseOut),
                    towards = AnimatedContentTransitionScope.SlideDirection.End
                )
            }
        ) {
            DeviceScreen(
                snackbarHostState = snackbarHostState,
                navigateToPhotoPreview = navController::navigateToPhotoPreview,
                navigateToRemovePhotoDialog = navController::navigateToRemovePhotoDialog
            )
        }
        composable<AddEditDeviceScreen> {
            AddEditDeviceScreen(
                snackbarHostState = snackbarHostState,
                onDismiss = onBackClick
            )
        }
        dialog<RemoveDeviceDialog> {
            RemoveDeviceDialog(
                onDismiss = onBackClick
            )
        }
        dialog<PhotoPreviewDialog> {
            PhotoPreviewDialog(
                onDismiss = onBackClick
            )
        }
        dialog<RemovePhotoDialog> {
            RemovePhotoDialog(
                onDismiss = onBackClick
            )
        }
    }
}
