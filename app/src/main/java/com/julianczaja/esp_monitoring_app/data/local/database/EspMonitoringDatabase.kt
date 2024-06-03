package com.julianczaja.esp_monitoring_app.data.local.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceDao
import com.julianczaja.esp_monitoring_app.data.local.database.dao.PhotoDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceEntity
import com.julianczaja.esp_monitoring_app.data.local.database.entity.PhotoEntity

@Database(
    entities = [
        DeviceEntity::class,
        PhotoEntity::class
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, MigrationFrom3To4::class),
        AutoMigration(from = 4, to = 5, MigrationFrom4To5::class),
        AutoMigration(from = 5, to = 6, MigrationFrom5To6::class),
    ]
)
abstract class EspMonitoringDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun photoDao(): PhotoDao
}

@DeleteTable(tableName = "device_settings")
class MigrationFrom3To4 : AutoMigrationSpec

class MigrationFrom4To5 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE photo SET thumbnailUrl = url")
    }
}

class MigrationFrom5To6 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE device SET `order` = id")
    }
}
