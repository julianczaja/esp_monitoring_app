package com.julianczaja.esp_monitoring_app.data.local.database.dao;

import androidx.room.Dao
import androidx.room.Query
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DeviceSettingsDao : EntityDao<DeviceSettingsEntity>() {

    @Query("SELECT * FROM device_settings where deviceId = :deviceId")
    abstract fun getDeviceSettings(deviceId: Long): Flow<DeviceSettingsEntity>
//    @Query("SELECT * FROM device_settings where deviceId = :deviceId")
//    abstract fun getAll(deviceId: Long): Flow<List<DeviceSettingsEntity>>
}
