package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.toDevice
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.toDeviceEntity
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao,
) : DeviceRepository {

    override fun getAllDevices(): Flow<List<Device>> = deviceDao.getAll().map { devices -> devices.map { it.toDevice() } }

    override suspend fun doesDeviceWithGivenIdAlreadyExist(deviceId: Long): Boolean {
        return deviceDao.hasDeviceWithId(deviceId)
    }

    override suspend fun doesDeviceWithGivenNameAlreadyExist(name: String): Boolean {
        return deviceDao.hasDeviceWithName(name)
    }

    override suspend fun addNew(device: Device) {
        deviceDao.insert(device.toDeviceEntity())
    }
}