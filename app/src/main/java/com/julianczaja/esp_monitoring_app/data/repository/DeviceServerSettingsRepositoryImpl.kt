package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceServerSettingsDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceServerSettingsEntity
import com.julianczaja.esp_monitoring_app.data.local.database.entity.toDeviceServerSettings
import com.julianczaja.esp_monitoring_app.data.remote.RetrofitEspMonitoringApi
import com.julianczaja.esp_monitoring_app.domain.model.DeviceServerSettings
import com.julianczaja.esp_monitoring_app.domain.model.toDeviceServerSettingsEntity
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceServerSettingsRepository
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class DeviceServerSettingsRepositoryImpl @Inject constructor(
    private val api: RetrofitEspMonitoringApi,
    private val deviceServerSettingsDao: DeviceServerSettingsDao
) : DeviceServerSettingsRepository {

    override fun getDeviceServerSettings(deviceId: Long) = deviceServerSettingsDao.getById(deviceId)
        .mapNotNull { it?.toDeviceServerSettings() }

    override suspend fun refreshDeviceServerSettingsRemote(deviceId: Long): Result<Unit> {
        var result = Result.success(Unit)
        api.getDeviceServerSettings(deviceId)
            .onFailure { result = Result.failure(it) }
            .onSuccess { refreshDeviceServerSettingsCache(it.toDeviceServerSettingsEntity(deviceId)) }
        return result
    }

    override suspend fun updateDeviceServerSettingsRemote(
        deviceId: Long,
        deviceServerSettings: DeviceServerSettings
    ): Result<Unit> {
        var result = Result.success(Unit)
        api.updateDeviceServerSettings(deviceId, deviceServerSettings)
            .onFailure { result = Result.failure(it) }
            .onSuccess { refreshDeviceServerSettingsCache(it.toDeviceServerSettingsEntity(deviceId)) }
        return result
    }

    private suspend fun refreshDeviceServerSettingsCache(
        deviceServerSettingsEntity: DeviceServerSettingsEntity
    ) = deviceServerSettingsDao.insertOrUpdate(deviceServerSettingsEntity)
}
