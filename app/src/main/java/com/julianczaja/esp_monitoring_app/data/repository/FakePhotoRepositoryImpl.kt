package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map


class FakePhotoRepositoryImpl : PhotoRepository {

    private val _allPhotosLocalFlow = MutableSharedFlow<List<Photo>>(replay = 1, extraBufferCapacity = 1)

    var getLastPhotoLocalThrowsError = false
    var updateAllPhotosReturnsException = false
    var readAllSavedPhotosReturnsException = false
    var removePhotoByFileNameLocalReturnsException = false
    var removePhotoByFileNameRemoteReturnsException = false

    var remotePhotos = emptyList<Photo>()
    var savedPhotos = emptyList<Photo>()

    suspend fun emitAllPhotosLocalData(data: List<Photo>) = _allPhotosLocalFlow.emit(data)

    override fun getAllPhotosLocal(deviceId: Long) = _allPhotosLocalFlow

    override fun getLastPhotoLocal(deviceId: Long) = when (getLastPhotoLocalThrowsError) {
        true -> flow { throw Exception("error") }
        else -> _allPhotosLocalFlow.map { photos -> photos.firstOrNull { it.deviceId == deviceId } }
    }

    override fun getPhotoByFileNameLocal(fileName: String) =
        _allPhotosLocalFlow.map { photos -> photos.firstOrNull { it.fileName == fileName } }

    override suspend fun updateAllPhotosRemote(deviceId: Long): Result<Unit> {
        delay(1000)
        return if (updateAllPhotosReturnsException) {
            Result.failure(Exception("error"))
        } else {
            emitAllPhotosLocalData(remotePhotos)
            Result.success(Unit)
        }
    }

    override suspend fun readAllSavedPhotosFromExternalStorage(deviceId: Long): Result<List<Photo>> {
        delay(1000)
        return if (readAllSavedPhotosReturnsException) {
            Result.failure(Exception("error"))
        } else {
            Result.success(savedPhotos)
        }
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

    override suspend fun downloadPhotoAndSaveToExternalStorage(photo: Photo): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun removeSavedPhotoFromExternalStorage(photo: Photo): Result<Unit> {
        TODO("Not yet implemented")
    }
}
