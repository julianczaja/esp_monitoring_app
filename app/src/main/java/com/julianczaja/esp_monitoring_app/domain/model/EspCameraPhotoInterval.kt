package com.julianczaja.esp_monitoring_app.domain.model

enum class EspCameraPhotoInterval(val description: String) {
    FIVE_SECONDS("5 s"),
    HALF_MINUTE("30 s"),
    ONE_MINUTE("1 min"),
    TWO_MINUTES("2 min"),
    FIVE_MINUTES("5 min"),
    TEN_MINUTES("10 min"),
    HALF_HOUR("30 min"),
    ONE_HOUR("1 h"),
}
