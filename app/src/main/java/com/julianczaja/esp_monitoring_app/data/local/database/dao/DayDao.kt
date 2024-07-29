package com.julianczaja.esp_monitoring_app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DayEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DayDao : EntityDao<DayEntity>() {

    @Query("SELECT * FROM day where deviceId = :deviceId")
    abstract fun getAll(deviceId: Long): Flow<List<DayEntity>>

    @Query("DELETE FROM day where deviceId = :deviceId")
    abstract fun deleteAll(deviceId: Long)
}
