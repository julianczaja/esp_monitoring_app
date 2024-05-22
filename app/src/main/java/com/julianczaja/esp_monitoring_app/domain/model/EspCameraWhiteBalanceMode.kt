package com.julianczaja.esp_monitoring_app.domain.model

import com.julianczaja.esp_monitoring_app.R

enum class EspCameraWhiteBalanceMode(val descriptionResId: Int) {
    Auto(R.string.white_balance_auto),
    Sunny(R.string.white_balance_sunny),
    Cloudy(R.string.white_balance_cloudy),
    Office(R.string.white_balance_office),
    Home(R.string.white_balance_home)
}