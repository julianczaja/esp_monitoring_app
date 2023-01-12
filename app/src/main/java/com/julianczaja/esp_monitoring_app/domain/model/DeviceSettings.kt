package com.julianczaja.esp_monitoring_app.domain.model

import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceSettingsEntity
import com.julianczaja.esp_monitoring_app.data.utils.toBoolean
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceSettingsDto(
    val deviceId: Long,
    val name: String,
    val frameSize: Int,
    val jpegQuality: Int,
    val brightness: Int,
    val contrast: Int,
    val saturation: Int,
    val flashOn: Int,
    val specialEffect: Int,
    val whiteBalanceMode: Int,
    val verticalFlip: Int,
    val horizontalMirror: Int,
)


fun DeviceSettingsDto.toDeviceSettings() = DeviceSettings(
    deviceId = deviceId,
    name = name,
    frameSize = EspCameraFrameSize.values()[frameSize],
    jpegQuality = jpegQuality,
    brightness = brightness,
    contrast = contrast,
    saturation = saturation,
    flashOn = flashOn.toBoolean(),
    specialEffect = EspCameraSpecialEffect.values()[specialEffect],
    whiteBalanceMode = EspCameraWhiteBalanceMode.values()[whiteBalanceMode],
    verticalFlip = verticalFlip.toBoolean(),
    horizontalMirror = horizontalMirror.toBoolean()
)

@Serializable
data class DeviceSettings(
    val deviceId: Long,
    val name: String = "Default",
//    @Serializable(with=LongAsStringSerializer::class)
    @SerialName("frameSize")
    val frameSize: EspCameraFrameSize = EspCameraFrameSize.FrameSizeSVGA,
    val jpegQuality: Int = 10, // 10-63
    val brightness: Int = 0, // -2 to 2
    val contrast: Int = 0, // -2 to 2
    val saturation: Int = 0, // -2 to 2
    val flashOn: Boolean = false,
    val specialEffect: EspCameraSpecialEffect = EspCameraSpecialEffect.NoEffect,
    val whiteBalanceMode: EspCameraWhiteBalanceMode = EspCameraWhiteBalanceMode.Auto,
    val verticalFlip: Boolean = false,
    val horizontalMirror: Boolean = false,
)

fun DeviceSettings.toDeviceSettingsEntity() = DeviceSettingsEntity(
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
