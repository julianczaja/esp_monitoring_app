package com.julianczaja.esp_monitoring_app.domain.model

import androidx.annotation.StringRes

data class FieldState<T>(
    val data: T,
    @StringRes val error: Int? = null
)
