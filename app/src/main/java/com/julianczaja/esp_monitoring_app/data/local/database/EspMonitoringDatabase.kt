package com.julianczaja.esp_monitoring_app.data.local.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DayDao
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceDao
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceInfoDao
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceServerSettingsDao
import com.julianczaja.esp_monitoring_app.data.local.database.dao.PhotoDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DayEntity
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceEntity
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceInfoEntity
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceServerSettingsEntity
import com.julianczaja.esp_monitoring_app.data.local.database.entity.PhotoEntity

@Database(
    entities = [
        DeviceEntity::class,
        PhotoEntity::class,
        DeviceInfoEntity::class,
        DeviceServerSettingsEntity::class,
        DayEntity::class
    ],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, MigrationFrom3To4::class),
        AutoMigration(from = 4, to = 5, MigrationFrom4To5::class),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
    ]
)
abstract class EspMonitoringDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun photoDao(): PhotoDao
    abstract fun deviceInfoDao(): DeviceInfoDao
    abstract fun deviceServerSettingsDao(): DeviceServerSettingsDao
    abstract fun dayDao(): DayDao
}

@DeleteTable(tableName = "device_settings")
class MigrationFrom3To4 : AutoMigrationSpec

class MigrationFrom4To5 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE photo SET thumbnailUrl = url")
    }
}
