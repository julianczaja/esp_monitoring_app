package com.julianczaja.esp_monitoring_app.domain.model

enum class EspCameraFrameSize(val description: String) {
    FrameSize96X96("96×96"),
    FrameSizeQQVGA("160×120"),
    FrameSizeQCIF("176×144"),
    FrameSizeHQVGA("240×176"),
    FrameSize240X240("240×240"),
    FrameSizeQVGA("320×240"),
    FrameSizeCIF("400×296"),
    FrameSizeHVGA("480×320"),
    FrameSizeVGA("640×480"),
    FrameSizeSVGA("800×600"),
    FrameSizeXGA("1024×768"),
    FrameSizeHD("1280×720"),
    FrameSizeSXGA("1280×1024"),
    FrameSizeUXGA("1600×1200")
}
