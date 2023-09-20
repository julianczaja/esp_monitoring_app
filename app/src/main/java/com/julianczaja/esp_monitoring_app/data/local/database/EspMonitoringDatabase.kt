package com.julianczaja.esp_monitoring_app.data.local.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceDao
import com.julianczaja.esp_monitoring_app.data.local.database.dao.PhotoDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceEntity
import com.julianczaja.esp_monitoring_app.data.local.database.entity.PhotoEntity

@Database(
    entities = [
        DeviceEntity::class,
        PhotoEntity::class
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, MigrationFrom3To4::class),
    ]
)
abstract class EspMonitoringDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun photoDao(): PhotoDao
}

@DeleteTable(tableName = "device_settings")
class MigrationFrom3To4 : AutoMigrationSpec