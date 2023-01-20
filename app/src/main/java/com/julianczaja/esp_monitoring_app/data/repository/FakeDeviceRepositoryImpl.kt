package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.InternalAppException
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow


class FakeDeviceRepositoryImpl : DeviceRepository {

    private val _allDevicesDataFlow = MutableSharedFlow<List<Device>>(replay = 1)

    override fun getAllDevices(): Flow<List<Device>> = _allDevicesDataFlow

    override suspend fun getDeviceById(id: Long): Device? =
        _allDevicesDataFlow.replayCache.firstOrNull()?.find { it.id == id }

    override suspend fun doesDeviceWithGivenIdAlreadyExist(deviceId: Long) =
        _allDevicesDataFlow.replayCache.firstOrNull()?.any { it.id == deviceId } ?: false

    override suspend fun doesDeviceWithGivenNameAlreadyExist(name: String) =
        _allDevicesDataFlow.replayCache.firstOrNull()?.any { it.name == name } ?: false

    override suspend fun addNew(device: Device) {
        _allDevicesDataFlow.replayCache.firstOrNull()?.let {
            _allDevicesDataFlow.emit(it + device)
        } ?: run {
            _allDevicesDataFlow.emit(listOf(device))
        }
    }

    override suspend fun remove(device: Device) {
        _allDevicesDataFlow.replayCache.firstOrNull()?.let {
            if (it.contains(device)) {
                _allDevicesDataFlow.emit(it.minus(device))
            } else {
                throw InternalAppException()
            }
        } ?: run {
            throw InternalAppException()
        }
    }
}
