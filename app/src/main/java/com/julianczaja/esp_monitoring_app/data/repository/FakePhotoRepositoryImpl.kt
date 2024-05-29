package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow


class FakePhotoRepositoryImpl : PhotoRepository {

    private val _allPhotosLocalFlow = MutableSharedFlow<List<Photo>>(replay = 1, extraBufferCapacity = 1)

    var updateAllPhotosReturnsException = false
    var removePhotoByFileNameLocalReturnsException = false
    var removePhotoByFileNameRemoteReturnsException = false
    var remotePhotos = emptyList<Photo>()

    suspend fun emitAllPhotosLocalData(data: List<Photo>) = _allPhotosLocalFlow.emit(data)

    override fun getAllPhotosLocal(deviceId: Long) = _allPhotosLocalFlow

    override fun getPhotoByFileNameLocal(fileName: String): Flow<Photo?> {
        TODO("Not yet implemented")
    }

    override suspend fun updateAllPhotosRemote(deviceId: Long): Result<Unit> {
        delay(1000)
        return if (updateAllPhotosReturnsException) {
            Result.failure(Exception("error"))
        } else {
            emitAllPhotosLocalData(remotePhotos)
            Result.success(Unit)
        }
    }

    override suspend fun downloadPhotoAndSaveToExternalStorage(photo: Photo): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun readAllSavedPhotosFromExternalStorage(deviceId: Long): Result<List<Photo>> {
        TODO("Not yet implemented")
    }

    override suspend fun removeSavedPhotoFromExternalStorage(photo: Photo): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun removePhotoByFileNameLocal(fileName: String): Result<Unit> =
        when (removePhotoByFileNameLocalReturnsException) {
            true -> Result.failure(Exception())
            false -> Result.success(Unit)
        }

    override suspend fun removePhotoByFileNameRemote(fileName: String): Result<Unit> =
        when (removePhotoByFileNameRemoteReturnsException) {
            true -> Result.failure(Exception())
            false -> Result.success(Unit)
        }
}
