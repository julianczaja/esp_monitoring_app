package com.julianczaja.esp_monitoring_app.domain.model


data class Selectable<T>(
    val item: T,
    val isSelected: Boolean
)
