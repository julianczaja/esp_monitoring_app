package com.julianczaja.esp_monitoring_app.domain.model

import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceServerSettingsEntity
import kotlinx.serialization.Serializable

@Serializable
data class DeviceServerSettings(
    val detectMostlyBlackPhotos: Boolean = false,
)

fun DeviceServerSettings.toDeviceServerSettingsEntity(deviceId: Long) = DeviceServerSettingsEntity(
    deviceId = deviceId,
    detectMostlyBlackPhotos = detectMostlyBlackPhotos
)
