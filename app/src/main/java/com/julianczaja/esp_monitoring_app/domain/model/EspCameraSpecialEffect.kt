package com.julianczaja.esp_monitoring_app.domain.model

import com.julianczaja.esp_monitoring_app.R

enum class EspCameraSpecialEffect(val descriptionResId: Int) {
    NoEffect(R.string.special_effect_no_effect),
    Negative(R.string.special_effect_negative),
    Grayscale(R.string.special_effect_grayscale),
    RedTint(R.string.special_effect_red_tint),
    GreenTint(R.string.special_effect_green_tint),
    BlueTint(R.string.special_effect_blue_tint),
    Sepia(R.string.special_effect_sepia)
}
