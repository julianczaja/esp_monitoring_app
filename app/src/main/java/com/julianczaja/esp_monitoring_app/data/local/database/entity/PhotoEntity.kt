package com.julianczaja.esp_monitoring_app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.julianczaja.esp_monitoring_app.data.utils.toDefaultFormatLocalDateTime
import com.julianczaja.esp_monitoring_app.domain.model.Photo

@Entity(
    tableName = "photo",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("deviceId"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    @ColumnInfo(index = true) val deviceId: Long,
    val dateTime: String,
    @ColumnInfo(defaultValue = "unknownFileName")
    val fileName: String,
    @ColumnInfo(defaultValue = "unknownSize")
    val size: String,
    val url: String,
    @ColumnInfo(defaultValue = "")
    val thumbnailUrl: String,
) : BaseEntity

fun PhotoEntity.toPhoto() = Photo(
    deviceId = deviceId,
    dateTime = dateTime.toDefaultFormatLocalDateTime(),
    fileName = fileName,
    size = size,
    url = url,
    thumbnailUrl = thumbnailUrl,
)
