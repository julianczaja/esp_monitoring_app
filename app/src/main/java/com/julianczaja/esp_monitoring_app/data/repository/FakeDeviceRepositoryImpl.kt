package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.InternalAppException
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform


class FakeDeviceRepositoryImpl : DeviceRepository {

    private val _allDevicesDataFlow = MutableSharedFlow<List<Device>>(replay = 1)

    var getAllDevicesThrowError = false

    override fun getAllDevices(): Flow<List<Device>> = if (getAllDevicesThrowError) {
        flow { throw Exception("error") }
    } else {
        _allDevicesDataFlow
    }

    override fun getDeviceById(id: Long): Flow<Device?> =
        _allDevicesDataFlow.transform { it.find { device -> device.id == id } }

    override suspend fun doesDeviceWithGivenIdAlreadyExist(deviceId: Long) =
        _allDevicesDataFlow.replayCache.firstOrNull()?.any { it.id == deviceId } ?: false

    override suspend fun doesDeviceWithGivenNameAlreadyExist(name: String) =
        _allDevicesDataFlow.replayCache.firstOrNull()?.any { it.name == name } ?: false

    override suspend fun addNew(device: Device): Result<Unit> {
        _allDevicesDataFlow.replayCache.firstOrNull()?.let {
            _allDevicesDataFlow.emit(it + device)
            return Result.success(Unit)
        } ?: run {
            _allDevicesDataFlow.emit(listOf(device))
            return Result.success(Unit)
        }
    }

    override suspend fun remove(device: Device): Result<Unit> {
        _allDevicesDataFlow.replayCache.firstOrNull()?.let {
            if (it.contains(device)) {
                _allDevicesDataFlow.emit(it.minus(device))
                return Result.success(Unit)
            } else {
                return Result.failure(InternalAppException())
            }
        } ?: run {
            return Result.failure(InternalAppException())
        }
    }
}
