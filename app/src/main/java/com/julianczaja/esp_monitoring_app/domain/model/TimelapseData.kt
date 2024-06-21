package com.julianczaja.esp_monitoring_app.domain.model

data class TimelapseData(
    val path: String,
    val sizeBytes: Long,
    val durationSeconds: Float
)
