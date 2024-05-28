package com.julianczaja.esp_monitoring_app.domain.model

import java.time.LocalDate


data class SelectableLocalDate(
    val date: LocalDate,
    val isSelected: Boolean
)
