package com.julianczaja.esp_monitoring_app.domain.model

import androidx.compose.runtime.Stable
import java.time.LocalDate

@Stable
data class Day(
    val deviceId: Long,
    val date: LocalDate,
)
