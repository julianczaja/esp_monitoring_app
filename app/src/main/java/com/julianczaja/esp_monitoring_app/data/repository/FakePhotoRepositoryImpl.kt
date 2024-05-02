package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow


class FakePhotoRepositoryImpl : PhotoRepository {

    private val _allPhotosLocalFlow = MutableSharedFlow<List<Photo>>(replay = 1, extraBufferCapacity = 1)

    var updateAllPhotosReturnsException = false
    var remotePhotos = emptyList<Photo>()

    suspend fun emitAllPhotosLocalData(data: List<Photo>) = _allPhotosLocalFlow.emit(data)

    override fun getAllPhotosLocal(deviceId: Long) = _allPhotosLocalFlow

    override fun getPhotoByFileNameLocal(fileName: String): Photo? {
        TODO("Not yet implemented")
    }

    override suspend fun updateAllPhotosRemote(deviceId: Long, from: Long?, to: Long?): Result<Unit> {
        delay(1000)
        return if (updateAllPhotosReturnsException) {
            Result.failure(Exception("error"))
        } else {
            emitAllPhotosLocalData(remotePhotos)
            Result.success(Unit)
        }
    }

    override suspend fun removePhotoByFileNameLocal(fileName: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun removePhotoByFileNameRemote(fileName: String): Result<Unit> {
        return Result.success(Unit)
    }
}
