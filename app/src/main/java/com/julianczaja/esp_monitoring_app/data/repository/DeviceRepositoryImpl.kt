package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.data.local.database.RoomDeleteResult
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.toDevice
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.InternalAppException
import com.julianczaja.esp_monitoring_app.domain.model.toDeviceEntity
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao,
) : DeviceRepository {

    override fun getAllDevices(): Flow<List<Device>> = deviceDao.getAll().map { devices -> devices.map { it.toDevice() } }

    override fun getDeviceById(id: Long): Flow<Device?> = deviceDao.getById(id).mapNotNull { it?.toDevice() }

    override suspend fun doesDeviceWithGivenIdAlreadyExist(deviceId: Long) = deviceDao.hasDeviceWithId(deviceId)

    override suspend fun doesDeviceWithGivenNameAlreadyExist(name: String) = deviceDao.hasDeviceWithName(name)

    override suspend fun addNew(device: Device): Result<Unit> = try {
        deviceDao.insert(device.toDeviceEntity())
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun remove(device: Device): Result<Unit> =
        if (deviceDao.deleteEntity(device.toDeviceEntity()) == RoomDeleteResult.SUCCESS) {
            Result.success(Unit)
        } else {
            Result.failure(InternalAppException())
        }
}
