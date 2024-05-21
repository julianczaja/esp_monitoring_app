package com.julianczaja.esp_monitoring_app.domain.model

import com.juul.kable.State

sealed class DeviceStatus {
    data object Connecting : DeviceStatus()
    data object Connected : DeviceStatus()
    data object Disconnecting : DeviceStatus()
    data object Disconnected : DeviceStatus()
}

fun State.toDeviceStatus() = when (this) {
    State.Connected -> DeviceStatus.Connected
    is State.Connecting -> DeviceStatus.Connecting
    is State.Disconnected -> DeviceStatus.Disconnected
    State.Disconnecting -> DeviceStatus.Disconnecting
}
