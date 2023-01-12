package com.julianczaja.esp_monitoring_app.domain.model

// TODO: Extract strings
enum class EspCameraSpecialEffect(val description: String) {
    NoEffect("No effect"),
    Negative("Negative"),
    Grayscale("Grayscale"),
    RedTint("Red Tint"),
    BlueTint("Blue Tint"),
    Sepia("Sepia")
}
