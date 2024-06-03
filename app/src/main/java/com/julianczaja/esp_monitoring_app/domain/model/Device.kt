package com.julianczaja.esp_monitoring_app.domain.model

import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceEntity

data class Device(
    val id: Long,
    val name: String,
    val order: Long = -1L,
)

fun Device.toDeviceEntity() = DeviceEntity(id = id, name = name, order = order)

fun Device.toDeviceEntity(customOrder: Long) = DeviceEntity(id = id, name = name, order = customOrder)
