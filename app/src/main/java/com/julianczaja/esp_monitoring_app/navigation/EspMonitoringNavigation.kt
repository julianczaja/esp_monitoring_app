package com.julianczaja.esp_monitoring_app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.julianczaja.esp_monitoring_app.domain.model.Device
import kotlinx.serialization.Serializable


// Devices
@Serializable
object DevicesScreen

fun NavController.navigateToDevices(navOptions: NavOptions? = null) {
    navigate(DevicesScreen, navOptions)
}

// Add or edit device
@Serializable
data class AddEditDeviceScreen(val deviceId: Long)

fun NavController.navigateToAddEditDeviceScreen(
    device: Device? = null,
    navOptions: NavOptions? = null
) {
    navigate(AddEditDeviceScreen(device?.id ?: DeviceIdArgs.NO_VALUE), navOptions)
}

// Remove device
@Serializable
data class RemoveDeviceDialog(val deviceId: Long)

fun NavController.navigateToRemoveDeviceDialog(deviceId: Long, navOptions: NavOptions? = null) {
    navigate(RemoveDeviceDialog(deviceId), navOptions)
}

// Remove photo
@Serializable
data class RemovePhotoDialog(val photoFileName: String)

fun NavController.navigateToRemovePhotoDialog(photoFileName: String, navOptions: NavOptions? = null) {
    navigate(RemovePhotoDialog(photoFileName), navOptions)
}

// Device
@Serializable
data class DeviceScreen(val deviceId: Long)

fun NavController.navigateToDevice(deviceId: Long, navOptions: NavOptions? = null) {
    navigate(DeviceScreen(deviceId), navOptions)
}

// Photo preview
@Serializable
data class PhotoPreviewDialog(val deviceId: Long, val photoFileName: String)

fun NavController.navigateToPhotoPreview(deviceId: Long?, photoFileName: String, navOptions: NavOptions? = null) {
    navigate(PhotoPreviewDialog(deviceId ?: DeviceIdArgs.NO_VALUE, photoFileName), navOptions)
}

// Args
object DeviceIdArgs {
    const val KEY = "deviceId"
    const val NO_VALUE = -1L
}
