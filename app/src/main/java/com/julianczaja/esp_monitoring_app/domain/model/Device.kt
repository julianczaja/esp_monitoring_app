package com.julianczaja.esp_monitoring_app.domain.model

import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceEntity

data class Device(
    val id: Long,
    val name: String,
)

fun Device.toDeviceEntity() = DeviceEntity(id, name)
