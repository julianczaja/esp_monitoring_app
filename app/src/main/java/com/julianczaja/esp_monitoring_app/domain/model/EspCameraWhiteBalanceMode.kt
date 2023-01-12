package com.julianczaja.esp_monitoring_app.domain.model

// TODO: Extract strings
enum class EspCameraWhiteBalanceMode(val description: String) {
    Auto("Auto"),
    Sunny("Sunny"),
    Cloudy("Cloudy"),
    Office("Office"),
    Home("Home")
}
