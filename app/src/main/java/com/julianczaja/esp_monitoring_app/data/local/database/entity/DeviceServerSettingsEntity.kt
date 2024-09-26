package com.julianczaja.esp_monitoring_app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.julianczaja.esp_monitoring_app.domain.model.DeviceServerSettings

@Entity(
    tableName = "device_server_settings",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("deviceId"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class DeviceServerSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    @ColumnInfo(index = true) val deviceId: Long,
    val detectMostlyBlackPhotos: Boolean = false,
) : BaseEntity

fun DeviceServerSettingsEntity.toDeviceServerSettings() = DeviceServerSettings(
    detectMostlyBlackPhotos = detectMostlyBlackPhotos
)
