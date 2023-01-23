package com.julianczaja.esp_monitoring_app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.toDefaultFormatLocalDateTime

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
    val url: String,
) : BaseEntity

fun PhotoEntity.toPhoto() = Photo(deviceId, dateTime.toDefaultFormatLocalDateTime(), fileName, url)
