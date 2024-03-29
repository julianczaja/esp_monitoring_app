package com.julianczaja.esp_monitoring_app.data.repository

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

    override suspend fun getDeviceById(id: Long): Device? = deviceDao.getById(id)?.toDevice()

    override fun getDeviceByIdFlow(id: Long): Flow<Device?> = deviceDao.getByIdFlow(id).mapNotNull { it?.toDevice() }

    override suspend fun doesDeviceWithGivenIdAlreadyExist(deviceId: Long): Boolean {
        return deviceDao.hasDeviceWithId(deviceId)
    }

    override suspend fun doesDeviceWithGivenNameAlreadyExist(name: String): Boolean {
        return deviceDao.hasDeviceWithName(name)
    }

    override suspend fun addNew(device: Device) {
        deviceDao.insert(device.toDeviceEntity())
    }

    override suspend fun remove(device: Device) {
        if (deviceDao.deleteEntity(device.toDeviceEntity()) == 0) {
            throw InternalAppException()
        }
    }
}
