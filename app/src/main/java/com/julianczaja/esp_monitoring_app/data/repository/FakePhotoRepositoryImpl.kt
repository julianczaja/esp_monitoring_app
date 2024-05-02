package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableSharedFlow


class FakePhotoRepositoryImpl : PhotoRepository {

    private val _allPhotosLocalFlow = MutableSharedFlow<List<Photo>>(replay = 1, extraBufferCapacity = 1)

    var updateAllPhotosReturnsException = false

    suspend fun emitAllPhotosLocalData(data: List<Photo>) = _allPhotosLocalFlow.emit(data)

    override fun getAllPhotosLocal(deviceId: Long) = _allPhotosLocalFlow

    override fun getPhotoByFileNameLocal(fileName: String): Photo? {
        TODO("Not yet implemented")
    }

    override suspend fun updateAllPhotosRemote(deviceId: Long, from: Long?, to: Long?): Result<Unit> {
        return if (updateAllPhotosReturnsException) {
            Result.failure(Exception("error"))
        } else {
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
