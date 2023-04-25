package com.julianczaja.esp_monitoring_app.data.local.database.dao;

import androidx.room.Dao
import androidx.room.Query
import com.julianczaja.esp_monitoring_app.data.local.database.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PhotoDao : EntityDao<PhotoEntity>() {

    @Query("SELECT * FROM photo where deviceId = :deviceId")
    abstract fun getAll(deviceId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photo where fileName = :fileName")
    abstract fun getByFileName(fileName: String): PhotoEntity?

    @Query("DELETE FROM photo where deviceId = :deviceId")
    abstract fun deleteAll(deviceId: Long)

    @Query("DELETE FROM photo where fileName = :fileName")
    abstract fun deleteByFileName(fileName: String)
}
