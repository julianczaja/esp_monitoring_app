package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceSettingsDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.toDeviceSettings
import com.julianczaja.esp_monitoring_app.data.remote.RetrofitEspMonitoringApi
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.julianczaja.esp_monitoring_app.domain.model.toDeviceSettings
import com.julianczaja.esp_monitoring_app.domain.model.toDeviceSettingsEntity
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceSettingsRepository
import kotlinx.coroutines.flow.map

class DeviceSettingsRepositoryImpl(
    private val deviceSettingsDao: DeviceSettingsDao,
    private val api: RetrofitEspMonitoringApi,
) : DeviceSettingsRepository {

    override fun getDeviceSettingsLocal(deviceId: Long) = deviceSettingsDao.getDeviceSettings(deviceId).map { it.toDeviceSettings() }

    override suspend fun saveDeviceSettingsLocal(deviceSettings: DeviceSettings) {
        deviceSettingsDao.insert(deviceSettings.toDeviceSettingsEntity())
    }

    override suspend fun getCurrentDeviceSettingsRemote(
        deviceId: Long,
    ): Result<DeviceSettings> = api.getCurrentDeviceSettings(deviceId).map { it.toDeviceSettings() }

    override suspend fun setCurrentDeviceSettingsRemote(
        deviceId: Long,
        deviceSettings: DeviceSettings,
    ): Result<Unit> = api.setCurrentDeviceSettings(deviceId, deviceSettings)
}
