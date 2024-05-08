package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.data.local.database.dao.PhotoDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.toPhoto
import com.julianczaja.esp_monitoring_app.data.remote.RetrofitEspMonitoringApi
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.toPhotoEntity
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao,
    private val api: RetrofitEspMonitoringApi,
) : PhotoRepository {

    override fun getAllPhotosLocal(deviceId: Long): Flow<List<Photo>> =
        photoDao.getAll(deviceId).map { photos -> photos.map { it.toPhoto() } }

    override fun getPhotoByFileNameLocal(fileName: String): Flow<Photo?> =
        photoDao.getByFileName(fileName).map { photo -> photo?.toPhoto() }

    override suspend fun removePhotoByFileNameLocal(fileName: String): Result<Unit> = try {
        photoDao.deleteByFileName(fileName)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removePhotoByFileNameRemote(fileName: String) = api.removePhoto(fileName)

    override suspend fun updateAllPhotosRemote(deviceId: Long, from: Long?, to: Long?): Result<Unit> {
        var result = Result.success(Unit)
        api.getDevicePhotos(deviceId, from, to)
            .onFailure { result = Result.failure(it) }
            .onSuccess { refreshPhotosCache(deviceId, it) }
        return result
    }

    private suspend fun refreshPhotosCache(deviceId: Long, photos: List<Photo>) = photoDao.withTransaction {
        photoDao.deleteAll(deviceId)
        photoDao.insertAll(photos.map(Photo::toPhotoEntity))
    }
}
