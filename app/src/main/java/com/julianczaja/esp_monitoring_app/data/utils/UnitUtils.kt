package com.julianczaja.esp_monitoring_app.data.utils

import android.annotation.SuppressLint
import kotlin.math.log10
import kotlin.math.pow

@SuppressLint("DefaultLocale")
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

