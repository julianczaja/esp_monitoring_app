package com.julianczaja.esp_monitoring_app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


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
