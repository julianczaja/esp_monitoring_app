package com.julianczaja.esp_monitoring_app.domain.model

sealed class DeviceStatus {
    data object Connecting : DeviceStatus()
    data object Connected : DeviceStatus()
    data object Disconnecting : DeviceStatus()
    data object Disconnected : DeviceStatus()
}
