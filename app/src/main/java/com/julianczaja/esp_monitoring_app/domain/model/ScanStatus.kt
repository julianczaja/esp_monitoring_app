package com.julianczaja.esp_monitoring_app.domain.model

sealed class ScanStatus {
    data object Stopped : ScanStatus()
    data object Scanning : ScanStatus()
    data class Failed(val message: CharSequence) : ScanStatus()
}
