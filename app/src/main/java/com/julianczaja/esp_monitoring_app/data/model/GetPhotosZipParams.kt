package com.julianczaja.esp_monitoring_app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GetPhotosZipParams(
    val fileNames: List<String>,
    var isHighQuality: Boolean
)
