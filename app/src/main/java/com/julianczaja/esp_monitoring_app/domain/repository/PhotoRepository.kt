package com.julianczaja.esp_monitoring_app.domain.repository

import android.net.Uri
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import kotlinx.coroutines.flow.Flow


interface PhotoRepository {

    fun getAllPhotosLocal(deviceId: Long): Flow<List<Photo>>

    fun getPhotoByFileNameLocal(fileName: String): Flow<Photo?>

    suspend fun removePhotoByFileNameLocal(fileName: String): Result<Unit>

    suspend fun removePhotoByFileNameRemote(fileName: String): Result<Unit>

    suspend fun updateAllPhotosRemote(deviceId: Long, from: Long? = null, to: Long? = null): Result<Unit>

    suspend fun downloadPhotoAndSaveToExternalStorage(photo: Photo): Result<Unit>

    suspend fun readAllSavedPhotosFromExternalStorage(deviceId: Long): Result<List<Photo>>

    suspend fun removeSavedPhotoFromExternalStorage(photo: Photo): Result<Unit>
}
