package com.julianczaja.esp_monitoring_app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.julianczaja.esp_monitoring_app.domain.model.DeviceInfo


@Entity(tableName = "device_info")
class DeviceInfoEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val freeSpaceMb: Float,
    val usedSpaceMb: Float,
    val spaceLimitMb: Float,
    val lastPhotoSizeMb: Float,
    val averagePhotoSizeMb: Float,
    val photosCount: Int,
    val newestPhotoTimestamp: Long?,
    val oldestPhotoTimestamp: Long?,
) : BaseEntity

fun DeviceInfoEntity.toDeviceInfo() = DeviceInfo(
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
