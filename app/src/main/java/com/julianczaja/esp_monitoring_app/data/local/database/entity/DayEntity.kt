package com.julianczaja.esp_monitoring_app.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.julianczaja.esp_monitoring_app.data.utils.toLocalDate
import com.julianczaja.esp_monitoring_app.domain.model.Day

@Entity(
    tableName = "day",
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
data class DayEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val date: String,
) : BaseEntity

fun DayEntity.toDay() = Day(deviceId, date.toLocalDate())
