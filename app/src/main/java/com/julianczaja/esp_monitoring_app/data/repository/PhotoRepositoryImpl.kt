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

    override suspend fun getAllPhotosRemote(
        deviceId: Long,
        from: Long?,
        to: Long?,
    ): List<Photo> {
        val newPhotos = api.getDevicePhotos(deviceId, from, to)

        photoDao.deleteAll(deviceId) // TODO: from/to
        photoDao.insertAll(newPhotos.map { it.toPhotoEntity() })

        return newPhotos
    }
}
