package com.julianczaja.esp_monitoring_app.data.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.julianczaja.esp_monitoring_app.domain.model.PermissionState
import timber.log.Timber


fun Boolean.toInt() = if (this) 1 else 0

fun Int.toBoolean() = this > 0

fun Context.getActivity(): ComponentActivity = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> throw IllegalStateException("Permissions should be called in the context of an Activity")
}

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}

fun Activity.getPermissionsState(permissions: Array<String>): PermissionState {
    val permissionsStates = permissions.map { getPermissionState(it) }
    return when {
        permissionsStates.all { it == PermissionState.GRANTED } -> PermissionState.GRANTED
        permissionsStates.any { it == PermissionState.RATIONALE_NEEDED } -> PermissionState.RATIONALE_NEEDED
        else -> PermissionState.DENIED
    }
}

fun Activity.getPermissionState(permission: String): PermissionState =
    when (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
        true -> PermissionState.GRANTED
        false -> if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            PermissionState.RATIONALE_NEEDED
        } else {
            PermissionState.DENIED
        }
    }.also { Timber.e("getPermissionState($permission) returning $it") }

fun Context.isBluetoothEnabled(): Boolean {
    val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
    return bluetoothManager.adapter.isEnabled
}

@SuppressLint("MissingPermission")
fun Activity.promptEnableBluetooth() = startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
