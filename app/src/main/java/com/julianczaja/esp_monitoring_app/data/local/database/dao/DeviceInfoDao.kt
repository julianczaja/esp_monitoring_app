package com.julianczaja.esp_monitoring_app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DeviceInfoDao : EntityDao<DeviceInfoEntity>() {

    @Query("SELECT * FROM device_info where deviceId = :deviceId")
    abstract fun getById(deviceId: Long): Flow<DeviceInfoEntity?>

    @Query("DELETE FROM device_info where deviceId = :deviceId")
    abstract fun deleteAllWithDeviceId(deviceId: Long)
}
