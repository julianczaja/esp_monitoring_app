package com.julianczaja.esp_monitoring_app.domain.model


data class DeviceSettings(
    val deviceId: Long = -1L,
    val wifiSsid: String = "",
    val frameSize: EspCameraFrameSize = EspCameraFrameSize.FrameSizeSVGA,
    val photoInterval: EspCameraPhotoInterval = EspCameraPhotoInterval.FIVE_MINUTES,
    val specialEffect: EspCameraSpecialEffect = EspCameraSpecialEffect.NoEffect,
    val whiteBalanceMode: EspCameraWhiteBalanceMode = EspCameraWhiteBalanceMode.Auto,
    val jpegQuality: Int = 10, // 10-63
    val brightness: Int = 0, // -2 to 2
    val contrast: Int = 0, // -2 to 2
    val saturation: Int = 0, // -2 to 2
    val flashOn: Boolean = false, // 0-1
    val verticalFlip: Boolean = false, // 0-1
    val horizontalMirror: Boolean = false // 0-1
)
