package com.julianczaja.esp_monitoring_app.data.local.database.dao;

import androidx.room.Dao
import androidx.room.Query
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DeviceDao : EntityDao<DeviceEntity>() {

    @Query("SELECT * FROM device")
    abstract fun getAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM device where id = :deviceId")
    abstract fun getById(deviceId: Long): DeviceEntity?

    @Query("SELECT EXISTS(SELECT * FROM device where id = :deviceId)")
    abstract suspend fun hasDeviceWithId(deviceId: Long): Boolean

    @Query("SELECT EXISTS(SELECT * FROM device where name = :name)")
    abstract suspend fun hasDeviceWithName(name: String): Boolean
}
