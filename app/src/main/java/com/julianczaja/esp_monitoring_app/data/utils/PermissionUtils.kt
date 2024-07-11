package com.julianczaja.esp_monitoring_app.data.utils

import android.Manifest
import android.os.Build
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf


fun getReadExternalStorageImagesPermissionName() = when {
    Build.VERSION.SDK_INT < 33 -> Manifest.permission.READ_EXTERNAL_STORAGE
    else -> Manifest.permission.READ_MEDIA_IMAGES
}

fun getReadExternalStorageVideosPermissionName() = when {
    Build.VERSION.SDK_INT < 33 -> Manifest.permission.READ_EXTERNAL_STORAGE
    else -> Manifest.permission.READ_MEDIA_VIDEO
}

fun getLocationPermissionNameOrEmpty() = when {
    Build.VERSION.SDK_INT >= 31 -> ""
    else -> Manifest.permission.ACCESS_FINE_LOCATION
}

fun getBluetoothPermissionsNamesOrEmpty(): ImmutableSet<String> = when {
    Build.VERSION.SDK_INT >= 31 -> persistentSetOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    else -> persistentSetOf()
}
