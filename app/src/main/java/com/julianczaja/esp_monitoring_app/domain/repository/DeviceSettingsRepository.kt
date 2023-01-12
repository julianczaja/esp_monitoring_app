package com.julianczaja.esp_monitoring_app.domain.repository

import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import kotlinx.coroutines.flow.Flow


interface DeviceSettingsRepository {

    fun getDeviceSettingsLocal(deviceId: Long): Flow<DeviceSettings>

    suspend fun saveDeviceSettingsLocal(deviceSettings: DeviceSettings)

    suspend fun getCurrentDeviceSettingsRemote(deviceId: Long): Result<DeviceSettings>

    suspend fun setCurrentDeviceSettingsRemote(deviceId: Long, deviceSettings: DeviceSettings): Result<Unit>
}
