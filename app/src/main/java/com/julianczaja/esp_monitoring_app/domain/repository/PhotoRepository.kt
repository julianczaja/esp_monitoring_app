package com.julianczaja.esp_monitoring_app.domain.repository

import com.julianczaja.esp_monitoring_app.domain.model.Day
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.ZipDownloadStatus
import kotlinx.coroutines.flow.Flow
import java.io.File


interface PhotoRepository {

    fun getAllPhotosByDayLocal(day: Day): Flow<List<Photo>>

    fun getLastPhotoLocal(deviceId: Long): Flow<Photo?>

    fun getPhotoByFileNameLocal(fileName: String): Flow<Photo?>

    suspend fun removePhotoByFileNameLocal(fileName: String): Result<Unit>

    suspend fun removePhotoByFileNameRemote(fileName: String): Result<Unit>

    suspend fun removePhotosByFileNamesRemote(fileNames: List<String>): Result<Unit>

    suspend fun updateAllPhotosByDayRemote(day: Day): Result<Unit>

    suspend fun updateLastPhotoRemote(deviceId: Long): Result<Unit>

    fun getPhotosZipRemoteAndSaveToFile(
        fileNames: List<String>,
        isHighQuality: Boolean,
                                        file: File
    ): Flow<ZipDownloadStatus>

    suspend fun downloadPhotoAndSaveToExternalStorage(photo: Photo): Result<Unit>

    fun getAllSavedPhotosFromExternalStorageFlow(deviceId: Long): Flow<Result<List<Photo>>>

    suspend fun removeSavedPhotoFromExternalStorage(photo: Photo): Result<Unit>

    fun forceRefreshSavedPhotosContent()
}
