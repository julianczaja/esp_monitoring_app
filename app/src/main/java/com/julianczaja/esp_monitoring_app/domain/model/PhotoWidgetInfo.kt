package com.julianczaja.esp_monitoring_app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoWidgetInfo(
    val widgetId: Int,
    val deviceId: Long,
    val deviceName: String,
    val lastUpdate: String,
    val photoDate: String? = null
)
