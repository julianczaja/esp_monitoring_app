package com.julianczaja.esp_monitoring_app.domain.repository

import com.julianczaja.esp_monitoring_app.domain.model.DeviceServerSettings
import kotlinx.coroutines.flow.Flow

interface DeviceServerSettingsRepository {

    fun getDeviceServerSettings(deviceId: Long): Flow<DeviceServerSettings>

    suspend fun refreshDeviceServerSettingsRemote(deviceId: Long): Result<Unit>

    suspend fun updateDeviceServerSettingsRemote(
        deviceId: Long,
        deviceServerSettings: DeviceServerSettings
    ): Result<Unit>
}
