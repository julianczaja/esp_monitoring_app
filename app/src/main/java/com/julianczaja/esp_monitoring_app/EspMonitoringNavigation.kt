package com.julianczaja.esp_monitoring_app

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.julianczaja.esp_monitoring_app.presentation.adddevice.AddNewDeviceDialog
import com.julianczaja.esp_monitoring_app.presentation.device.DeviceScreen
import com.julianczaja.esp_monitoring_app.presentation.devices.DevicesScreen


// Devices
const val devicesNavigationRoute = "devices_route"

fun NavController.navigateToDevices(navOptions: NavOptions? = null) {
    this.navigate(devicesNavigationRoute, navOptions)
}

fun NavGraphBuilder.devicesScreen(
    navigateToDevice: (Long) -> Unit,
) {
    composable(route = devicesNavigationRoute) {
        DevicesScreen(navigateToDevice)
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

// Device
const val deviceNavigationRoute = "device_route"
internal const val deviceIdArg = "deviceId"

data class DeviceArgs(val deviceId: Long) {
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle.get<Long>(deviceIdArg) ?: -1L)
}

fun NavController.navigateToDevice(deviceId: Long, navOptions: NavOptions? = null) {
    val encoded = Uri.encode(deviceId.toString())
    this.navigate("$deviceNavigationRoute/$encoded", navOptions)
}

fun NavGraphBuilder.deviceScreen() {
    composable(
        route = "$deviceNavigationRoute/{$deviceIdArg}",
        arguments = listOf(navArgument(deviceIdArg) { type = NavType.LongType })
    ) {
        DeviceScreen()
    }
}
