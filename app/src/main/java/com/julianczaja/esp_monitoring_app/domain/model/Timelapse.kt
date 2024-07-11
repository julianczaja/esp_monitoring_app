package com.julianczaja.esp_monitoring_app.domain.model

import androidx.compose.runtime.Stable
import java.time.LocalDateTime

@Stable
data class Timelapse(
    val addedDateTime: LocalDateTime,
    val data: TimelapseData
)
