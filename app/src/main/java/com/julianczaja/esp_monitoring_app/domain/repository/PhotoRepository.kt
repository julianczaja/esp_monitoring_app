package com.julianczaja.esp_monitoring_app.domain.repository

import com.julianczaja.esp_monitoring_app.domain.model.Photo
import kotlinx.coroutines.flow.Flow


interface PhotoRepository {

    fun getAllPhotosLocal(deviceId: Long): Flow<List<Photo>>

    fun getPhotoByFileNameLocal(fileName: String): Photo?

    suspend fun updateAllPhotosRemote(
        deviceId: Long,
        from: Long? = null,
        to: Long? = null,
    ): Result<List<Photo>>

    suspend fun removePhotoByFileNameLocal(fileName: String): Result<Unit>

    suspend fun removePhotoByFileNameRemote(fileName: String): Result<Unit>
}
