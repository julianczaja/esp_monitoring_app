package com.julianczaja.esp_monitoring_app.domain.model

import androidx.annotation.StringRes
import com.julianczaja.esp_monitoring_app.R
import com.juul.kable.State

sealed class DeviceStatus(@StringRes val stringId: Int) {
    data object Connecting : DeviceStatus(R.string.status_connecting_label)
    data object Connected : DeviceStatus(R.string.status_connected_label)
    data object Disconnecting : DeviceStatus(R.string.status_disconnecting_label)
    data object Disconnected : DeviceStatus(R.string.status_disconnected_label)
}

fun State.toDeviceStatus() = when (this) {
    State.Connected -> DeviceStatus.Connected
    is State.Connecting -> DeviceStatus.Connecting
    is State.Disconnected -> DeviceStatus.Disconnected
    State.Disconnecting -> DeviceStatus.Disconnecting
}
