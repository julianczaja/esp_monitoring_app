package com.julianczaja.esp_monitoring_app.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.julianczaja.esp_monitoring_app.presentation.adddevice.AddNewDeviceDialog
import com.julianczaja.esp_monitoring_app.presentation.device.DeviceScreen
import com.julianczaja.esp_monitoring_app.presentation.devices.DevicesScreen
import com.julianczaja.esp_monitoring_app.presentation.photopreview.PhotoPreviewDialog
import com.julianczaja.esp_monitoring_app.presentation.removedevice.RemoveDeviceDialog
import com.julianczaja.esp_monitoring_app.presentation.removephoto.RemovePhotoDialog


// Devices
const val devicesNavigationRoute = "devices_route"

fun NavController.navigateToDevices(navOptions: NavOptions? = null) {
    this.navigate(devicesNavigationRoute, navOptions)
}

fun NavGraphBuilder.devicesScreen(
    navigateToDevice: (Long) -> Unit,
    navigateToRemoveDevice: (Long) -> Unit,
) {
    composable(route = devicesNavigationRoute) {
        DevicesScreen(navigateToDevice, navigateToRemoveDevice)
    }
}

// Add new device
const val addNewDeviceDialogNavigationRoute = "add_new_device_route"

fun NavController.navigateToAddNewDeviceDialog(navOptions: NavOptions? = null) {
    this.navigate(addNewDeviceDialogNavigationRoute, navOptions)
}

fun NavGraphBuilder.addNewDeviceDialog(
    onDismiss: () -> Unit,
) {
    dialog(route = addNewDeviceDialogNavigationRoute) {
        AddNewDeviceDialog(onDismiss)
    }
}

// Remove device
const val removeDeviceDialogNavigationRoute = "remove_device_route"

fun NavController.navigateToRemoveDeviceDialog(deviceId: Long, navOptions: NavOptions? = null) {
    val encoded = Uri.encode(deviceId.toString())
    this.navigate(
        "$removeDeviceDialogNavigationRoute/$encoded", navOptions
    )
}

fun NavGraphBuilder.removeDeviceDialog(
    onDismiss: () -> Unit,
) {
    dialog(
        route = "$removeDeviceDialogNavigationRoute/{${DeviceIdArgs.KEY}}",
        arguments = listOf(navArgument(DeviceIdArgs.KEY) { type = DeviceIdArgs.NAV_TYPE })
    ) {
        RemoveDeviceDialog(onDismiss)
    }
}

// Remove photo
const val removePhotoDialogNavigationRoute = "remove_photo_route"

fun NavController.navigateToRemovePhotoDialog(photoFileName: String, navOptions: NavOptions? = null) {
    val encoded = Uri.encode(photoFileName)
    this.navigate("$removePhotoDialogNavigationRoute/$encoded", navOptions)
}

fun NavGraphBuilder.removePhotoDialog(
    onDismiss: () -> Unit,
) {
    dialog(
        route = "$removePhotoDialogNavigationRoute/{${PhotoFileNameArgs.KEY}}",
        arguments = listOf(navArgument(PhotoFileNameArgs.KEY) { type = PhotoFileNameArgs.NAV_TYPE })
    ) {
        RemovePhotoDialog(onDismiss)
    }
}

// Device
const val deviceNavigationRoute = "device_route"

fun NavController.navigateToDevice(deviceId: Long, navOptions: NavOptions? = null) {
    val encoded = Uri.encode(deviceId.toString())
    this.navigate("$deviceNavigationRoute/$encoded", navOptions)
}

fun NavGraphBuilder.deviceScreen(
    snackbarHostState: SnackbarHostState,
    navigateToPhotoPreview: (Long, String) -> Unit,
    navigateToRemovePhotoDialog: (String) -> Unit,
) {
    composable(
        route = "$deviceNavigationRoute/{${DeviceIdArgs.KEY}}",
        arguments = listOf(navArgument(DeviceIdArgs.KEY) { type = DeviceIdArgs.NAV_TYPE }),
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
        DeviceScreen(snackbarHostState, navigateToPhotoPreview, navigateToRemovePhotoDialog)
    }
}

// Photo preview
const val photoPreviewNavigationRoute = "photo_preview_route"

fun NavController.navigateToPhotoPreview(deviceId: Long, photoFileName: String, navOptions: NavOptions? = null) {
    val encodedDeviceId = Uri.encode(deviceId.toString())
    val encodedFileName = Uri.encode(photoFileName)
    this.navigate("$photoPreviewNavigationRoute/$encodedDeviceId/$encodedFileName", navOptions)
}

fun NavGraphBuilder.photoPreviewDialog(
    onDismiss: () -> Unit,
) {
    dialog(
        route = "$photoPreviewNavigationRoute/{${DeviceIdArgs.KEY}}/{${PhotoFileNameArgs.KEY}}",
        arguments = listOf(
            navArgument(DeviceIdArgs.KEY) { type = DeviceIdArgs.NAV_TYPE },
            navArgument(PhotoFileNameArgs.KEY) { type = PhotoFileNameArgs.NAV_TYPE },
        )
    ) {
        PhotoPreviewDialog(onDismiss)
    }
}

// Args
data class DeviceIdArgs(val deviceId: Long) {
    companion object {
        const val KEY = "deviceId"
        val NAV_TYPE = NavType.LongType
    }

    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle.get<Long>(KEY) ?: -1L)
}


data class PhotoFileNameArgs(val fileName: String) {
    companion object {
        const val KEY = "fileName"
        val NAV_TYPE = NavType.StringType
    }

    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle.get<String>(KEY) ?: "")
}
