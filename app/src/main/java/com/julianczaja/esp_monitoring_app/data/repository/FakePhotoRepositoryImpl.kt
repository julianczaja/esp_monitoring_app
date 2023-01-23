package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow


class FakePhotoRepositoryImpl : PhotoRepository {

    private val _allPhotosLocalFlow = MutableSharedFlow<List<Photo>>(replay = 1)

    private var _updateAllPhotosRemoteData = Result.success(listOf<Photo>())

    fun emitAllPhotosLocalData(data: List<Photo>) = _allPhotosLocalFlow.tryEmit(data)

    fun setUpdateAllPhotosRemoteReturnData(data: Result<List<Photo>>) {
        _updateAllPhotosRemoteData = data
    }

    override fun getAllPhotosLocal(deviceId: Long) = _allPhotosLocalFlow

    override suspend fun updateAllPhotosRemote(deviceId: Long, from: Long?, to: Long?): Result<List<Photo>> {
        delay(1000L)
        return _updateAllPhotosRemoteData
    }

    override suspend fun removePhotoRemote(fileName: String): Result<Unit> {
        return Result.success(Unit)
    }
}
