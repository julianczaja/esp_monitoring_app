package com.julianczaja.esp_monitoring_app.data.utils

import java.lang.Float.max
import java.lang.Float.min


fun getClampedPercent(value: Float, maxValue: Float) = when {
    else -> max(min(100f, ((value / maxValue) * 100f)), 0f)
}
