package com.julianczaja.esp_monitoring_app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.julianczaja.esp_monitoring_app.domain.model.Device

@Entity(tableName = "device")
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long,
    val name: String,
    @ColumnInfo(defaultValue = "-1")
    val order: Long,
) : BaseEntity

fun DeviceEntity.toDevice() = Device(id = id, name = name, order = order)
