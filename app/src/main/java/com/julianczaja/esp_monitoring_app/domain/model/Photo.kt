package com.julianczaja.esp_monitoring_app.domain.model

import android.os.Parcelable
import androidx.compose.runtime.Stable
import com.julianczaja.esp_monitoring_app.data.local.database.entity.PhotoEntity
import com.julianczaja.esp_monitoring_app.data.utils.LocalDateTimeAsStringSerializer
import com.julianczaja.esp_monitoring_app.data.utils.toDefaultFormatString
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Stable
@Serializable
@Parcelize
data class Photo(
    val deviceId: Long,
    @Serializable(with = LocalDateTimeAsStringSerializer::class)
    val dateTime: LocalDateTime,
    val fileName: String,
    val size: String,
    val url: String,
    val thumbnailUrl: String,
    val isSaved: Boolean = false,
) : Parcelable {

    companion object {
        fun mock(
            deviceId: Long = 1L,
            dateTime: LocalDateTime = LocalDateTime.of(2024, 6, 15, 10, 25, 1),
            fileName: String = "fileName.jpeg",
            size: String = "1600x1200",
            url: String = "url",
            thumbnailUrl: String = "thumbnailUrl",
            isSaved: Boolean = false
        ) = Photo(
            deviceId = deviceId,
            dateTime = dateTime,
            fileName = fileName,
            size = size,
            url = url,
            thumbnailUrl = thumbnailUrl,
            isSaved = isSaved,
        )
    }
}

fun Photo.toPhotoEntity() = PhotoEntity(
    deviceId = deviceId,
    dateTime = dateTime.toDefaultFormatString(),
    fileName = fileName,
    size = size,
    url = url,
    thumbnailUrl = thumbnailUrl,
)
