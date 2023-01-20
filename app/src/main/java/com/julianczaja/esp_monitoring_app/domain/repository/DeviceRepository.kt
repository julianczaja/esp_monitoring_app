package com.julianczaja.esp_monitoring_app.domain.repository

import com.julianczaja.esp_monitoring_app.domain.model.Device
import kotlinx.coroutines.flow.Flow


interface DeviceRepository {

    fun getAllDevices(): Flow<List<Device>>

    suspend fun getDeviceById(id: Long): Device?

    suspend fun doesDeviceWithGivenIdAlreadyExist(deviceId: Long): Boolean

    suspend fun doesDeviceWithGivenNameAlreadyExist(name: String): Boolean

    suspend fun addNew(device: Device)

    suspend fun remove(device: Device)

}
