package com.julianczaja.esp_monitoring_app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceServerSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DeviceServerSettingsDao : EntityDao<DeviceServerSettingsEntity>() {

    @Query("SELECT * FROM device_server_settings where deviceId = :deviceId")
    abstract fun getById(deviceId: Long): Flow<DeviceServerSettingsEntity?>

    @Query("DELETE FROM device_server_settings where deviceId = :deviceId")
    abstract fun deleteAllWithDeviceId(deviceId: Long)
}
