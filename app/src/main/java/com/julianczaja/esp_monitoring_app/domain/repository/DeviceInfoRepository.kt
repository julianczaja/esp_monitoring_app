package com.julianczaja.esp_monitoring_app.domain.repository

import com.julianczaja.esp_monitoring_app.domain.model.DeviceInfo
import kotlinx.coroutines.flow.Flow

interface DeviceInfoRepository {

    fun getDeviceInfo(id: Long): Flow<DeviceInfo>

    suspend fun updateDeviceInfoRemote(id: Long): Result<Unit>
}
