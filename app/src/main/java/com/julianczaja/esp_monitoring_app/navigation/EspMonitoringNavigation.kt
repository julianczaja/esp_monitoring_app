package com.julianczaja.esp_monitoring_app.navigation

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavController
import androidx.navigation.NavType
import com.julianczaja.esp_monitoring_app.domain.model.Device
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


// Devices
@Serializable
object DevicesScreen

// Device
@Serializable
data class DeviceScreen(val deviceId: Long)

fun NavController.navigateToDevice(deviceId: Long) {
    navigate(DeviceScreen(deviceId)) {
        launchSingleTop = true
    }
}


// Device settings
@Serializable
data object DeviceSettingsScreen

fun NavController.navigateToDeviceSettings() {
    navigate(DeviceSettingsScreen) {
        launchSingleTop = true
    }
}

// Add or edit device
@Serializable
data class AddEditDeviceScreen(val deviceId: Long)

fun NavController.navigateToAddEditDeviceScreen(device: Device? = null) {
    navigate(AddEditDeviceScreen(device?.id ?: DeviceIdArgs.NO_VALUE)) {
        launchSingleTop = true
    }
}

// Remove device
@Serializable
data class RemoveDeviceDialog(val deviceId: Long)

fun NavController.navigateToRemoveDeviceDialog(deviceId: Long) {
    navigate(RemoveDeviceDialog(deviceId)) {
        launchSingleTop = true
    }
}

// Remove photo
@Serializable
data class RemovePhotosDialog(val params: RemovePhotosDialogParameters)

@Serializable
@Parcelize
data class RemovePhotosDialogParameters(
    val photosFileNames: List<String>
) : Parcelable

fun NavController.navigateToRemovePhotosDialog(photoFileNames: List<String>) {
    navigate(RemovePhotosDialog(RemovePhotosDialogParameters(photoFileNames))) {
        launchSingleTop = true
    }
}

// Photo preview
@Serializable
data class PhotoPreviewDialog(val deviceId: Long, val photoFileName: String)

fun NavController.navigateToPhotoPreview(deviceId: Long?, photoFileName: String) {
    navigate(PhotoPreviewDialog(deviceId ?: DeviceIdArgs.NO_VALUE, photoFileName)) {
        launchSingleTop = true
    }
}

// App settings
@Serializable
object AppSettingsScreen

fun NavController.navigateToAppSettings() {
    navigate(AppSettingsScreen) {
        launchSingleTop = true
    }
}

// Args
object DeviceIdArgs {
    const val KEY = "deviceId"
    const val NO_VALUE = -1L
}

// https://github.com/edgarmiro/compose-navigation-type-safety/blob/main/app/src/main/java/com/example/composenavigation/typesafety/ParcelableType.kt
inline fun <reified T : Parcelable> parcelableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = object : NavType<T>(isNullableAllowed) {
    override fun get(bundle: Bundle, key: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key)
        }

    override fun parseValue(value: String): T = json.decodeFromString(value)

    override fun serializeAsValue(value: T): String = json.encodeToString(value)

    override fun put(bundle: Bundle, key: String, value: T) = bundle.putParcelable(key, value)
}
