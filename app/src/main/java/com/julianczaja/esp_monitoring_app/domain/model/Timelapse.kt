package com.julianczaja.esp_monitoring_app.domain.model

import java.time.LocalDateTime

data class Timelapse(
    val addedDateTime: LocalDateTime,
    val data: TimelapseData
)
