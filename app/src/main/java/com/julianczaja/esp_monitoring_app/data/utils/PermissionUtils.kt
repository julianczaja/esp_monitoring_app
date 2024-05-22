package com.julianczaja.esp_monitoring_app.data.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build


fun checkPermissionAndDoAction(
    context: Context,
    permission: String,
    onGranted: () -> Unit,
    onDenied: (() -> Unit)? = null,
) {
    when (PackageManager.PERMISSION_GRANTED) {
        context.checkSelfPermission(permission) -> onGranted()
        else -> onDenied?.invoke()
    }
}

fun getReadExternalStoragePermissionName() = when {
    Build.VERSION.SDK_INT < 33 -> Manifest.permission.READ_EXTERNAL_STORAGE
    else -> Manifest.permission.READ_MEDIA_IMAGES
}

fun getLocationPermissionNameOrEmpty() = when {
    Build.VERSION.SDK_INT >= 31 -> ""
    else -> Manifest.permission.ACCESS_FINE_LOCATION
}

fun getBluetoothPermissionsNamesOrEmpty(): Array<String> = when {
    Build.VERSION.SDK_INT >= 31 -> arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else -> emptyArray()
}
