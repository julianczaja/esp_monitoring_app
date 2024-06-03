package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.data.local.database.RoomDeleteResult
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.toDevice
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.InternalAppException
import com.julianczaja.esp_monitoring_app.domain.model.toDeviceEntity
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao,
) : DeviceRepository {

    override fun getAllDevices(): Flow<List<Device>> =
        deviceDao.getAll().map { devices -> devices.map { it.toDevice() } }

    override fun getDeviceById(id: Long): Flow<Device?> = deviceDao.getById(id).mapNotNull { it?.toDevice() }

    override suspend fun doesDeviceWithGivenIdAlreadyExist(deviceId: Long) = deviceDao.hasDeviceWithId(deviceId)

    override suspend fun doesDeviceWithGivenNameAlreadyExist(name: String) = deviceDao.hasDeviceWithName(name)

    override suspend fun addNew(device: Device): Result<Unit> = try {
        if (device.order == -1L) {
            val maxOrder = deviceDao.getMaxOrder() ?: 0
            deviceDao.insert(device.toDeviceEntity(customOrder = maxOrder + 1L))
        } else {
            deviceDao.insert(device.toDeviceEntity())
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun update(device: Device): Result<Unit> = try {
        deviceDao.update(device.toDeviceEntity())
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

    override suspend fun reorderDevices(device1Id: Long, device2Id: Long): Result<Unit> {
        val device1 = deviceDao.getById(device1Id).first()
        val device2 = deviceDao.getById(device2Id).first()

        if (device1 != null && device2 != null) {
            val device1Order = device1.order
            val device2Order = device2.order

            deviceDao.update(device1.copy(order = device2Order))
            deviceDao.update(device2.copy(order = device1Order))

            return Result.success(Unit)
        } else {
            return Result.failure(IllegalArgumentException("One or both devices not found"))
        }
    }
}
