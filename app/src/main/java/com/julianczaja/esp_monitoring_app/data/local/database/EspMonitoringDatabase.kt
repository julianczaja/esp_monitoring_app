package com.julianczaja.esp_monitoring_app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceDao
import com.julianczaja.esp_monitoring_app.data.local.database.dao.PhotoDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceEntity
import com.julianczaja.esp_monitoring_app.data.local.database.entity.PhotoEntity

@Database(
    entities = [
        DeviceEntity::class,
        PhotoEntity::class
    ],
    version = 1,
    exportSchema = true,
)
abstract class EspMonitoringDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun photoDao(): PhotoDao
}