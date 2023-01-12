package com.julianczaja.esp_monitoring_app.data.utils


fun Boolean.toInt() = if (this) 1 else 0

fun Int.toBoolean() = this > 0
