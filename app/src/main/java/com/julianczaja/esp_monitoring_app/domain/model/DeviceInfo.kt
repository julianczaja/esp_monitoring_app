package com.julianczaja.esp_monitoring_app.domain.model

import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceInfoEntity
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val deviceId: Long,
    val freeSpaceMb: Float,
    val usedSpaceMb: Float,
    val spaceLimitMb: Float,
    val lastPhotoSizeMb: Float,
    val averagePhotoSizeMb: Float,
    val photosCount: Int,
    val newestPhotoTimestamp: Long?,
    val oldestPhotoTimestamp: Long?,
)

fun DeviceInfo.toDeviceInfoEntity() = DeviceInfoEntity(
    deviceId = deviceId,
    freeSpaceMb = freeSpaceMb,
    usedSpaceMb = usedSpaceMb,
    spaceLimitMb = spaceLimitMb,
    lastPhotoSizeMb = lastPhotoSizeMb,
    averagePhotoSizeMb = averagePhotoSizeMb,
    photosCount = photosCount,
    newestPhotoTimestamp = newestPhotoTimestamp,
    oldestPhotoTimestamp = oldestPhotoTimestamp,
)
