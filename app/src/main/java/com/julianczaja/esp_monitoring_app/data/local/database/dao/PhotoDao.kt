package com.julianczaja.esp_monitoring_app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.julianczaja.esp_monitoring_app.data.local.database.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PhotoDao : EntityDao<PhotoEntity>() {

    @Query("SELECT * FROM photo where deviceId = :deviceId")
    abstract fun getAll(deviceId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photo where deviceId = :deviceId and substr(dateTime, 1, 8) = :date")
    abstract fun getAllByDate(deviceId: Long, date: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photo where deviceId = :deviceId ORDER BY dateTime DESC LIMIT 1")
    abstract fun getLast(deviceId: Long): Flow<PhotoEntity?>

    @Query("SELECT * FROM photo where fileName = :fileName")
    abstract fun getByFileName(fileName: String): Flow<PhotoEntity?>

    @Query("DELETE FROM photo where deviceId = :deviceId")
    abstract fun deleteAll(deviceId: Long)

    @Query("DELETE FROM photo where deviceId = :deviceId and substr(dateTime, 1, 8) = :date")
    abstract fun deleteAllByDate(deviceId: Long, date: String)

    @Query("DELETE FROM photo where fileName = :fileName")
    abstract fun deleteByFileName(fileName: String)

    @Query("SELECT COUNT(*) FROM photo WHERE filename = :filename")
    abstract fun countByFilename(filename: String): Int
}
