package com.julianczaja.esp_monitoring_app.domain.model

enum class EspCameraFrameSize(val description: String) {
    FrameSize96X96("96x96"),
    FrameSizeQQVGA("160x120"),
    FrameSizeQCIF("176x144"),
    FrameSizeHQVGA("240x176"),
    FrameSize240X240("240x240"),
    FrameSizeQVGA("320x240"),
    FrameSizeCIF("400x296"),
    FrameSizeHVGA("480x320"),
    FrameSizeVGA("640x480"),
    FrameSizeSVGA("800x600"),
    FrameSizeXGA("1024x768"),
    FrameSizeHD("1280x720"),
    FrameSizeSXGA("1280x1024"),
    FrameSizeUXGA("1600x1200")
}
