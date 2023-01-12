package com.julianczaja.esp_monitoring_app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraFrameSize
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraSpecialEffect
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraWhiteBalanceMode

@Entity(
    tableName = "device_settings",
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
data class DeviceSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    @ColumnInfo(index = true) val deviceId: Long,
    val name: String,
    val frameSize: EspCameraFrameSize,
    val jpegQuality: Int,
    val brightness: Int,
    val contrast: Int,
    val saturation: Int,
    val flashOn: Boolean,
    val specialEffect: EspCameraSpecialEffect,
    val whiteBalanceMode: EspCameraWhiteBalanceMode,
    val verticalFlip: Boolean,
    val horizontalMirror: Boolean,
) : BaseEntity

fun DeviceSettingsEntity.toDeviceSettings() = DeviceSettings(
    deviceId = deviceId,
    name = name,
    frameSize = frameSize,
    jpegQuality = jpegQuality,
    brightness = brightness,
    contrast = contrast,
    saturation = saturation,
    flashOn = flashOn,
    specialEffect = specialEffect,
    whiteBalanceMode = whiteBalanceMode,
    verticalFlip = verticalFlip,
    horizontalMirror = horizontalMirror
)
