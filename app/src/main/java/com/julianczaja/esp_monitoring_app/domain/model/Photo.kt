package com.julianczaja.esp_monitoring_app.domain.model

import com.julianczaja.esp_monitoring_app.LocalDateTimeAsStringSerializer
import com.julianczaja.esp_monitoring_app.data.local.database.entity.PhotoEntity
import com.julianczaja.esp_monitoring_app.toDefaultFormatString
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Photo(
    val deviceId: Long,
    @Serializable(with = LocalDateTimeAsStringSerializer::class)
    val dateTime: LocalDateTime,
    val fileName: String,
    val size: String,
    val url: String,
)

fun Photo.toPhotoEntity() = PhotoEntity(
    deviceId = deviceId,
    dateTime = dateTime.toDefaultFormatString(),
    fileName = fileName,
    size = size,
    url = url
)
