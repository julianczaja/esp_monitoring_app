package com.julianczaja.esp_monitoring_app.data.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build


fun checkPermissionAndDoAction(
    context: Context,
    permission: String,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
        onGranted()
    } else {
        onDenied()
    }
}

fun getReadExternalStoragePermissionName() = if (Build.VERSION.SDK_INT < 33) {
    Manifest.permission.READ_EXTERNAL_STORAGE
} else {
    Manifest.permission.READ_MEDIA_IMAGES
}
