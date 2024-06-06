package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceInfoDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceInfoEntity
import com.julianczaja.esp_monitoring_app.data.local.database.entity.toDeviceInfo
import com.julianczaja.esp_monitoring_app.data.remote.RetrofitEspMonitoringApi
import com.julianczaja.esp_monitoring_app.domain.model.DeviceInfo
import com.julianczaja.esp_monitoring_app.domain.model.toDeviceInfoEntity
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class DeviceInfoRepositoryImpl @Inject constructor(
    private val api: RetrofitEspMonitoringApi,
    private val deviceInfoDao: DeviceInfoDao
) : DeviceInfoRepository {

    override fun getDeviceInfo(id: Long): Flow<DeviceInfo> = deviceInfoDao.getById(id).mapNotNull { it?.toDeviceInfo() }

    override suspend fun updateDeviceInfoRemote(id: Long): Result<Unit> {
        var result = Result.success(Unit)
        api.updateDeviceInfo(id)
            .onFailure { result = Result.failure(it) }
            .onSuccess { refreshDeviceInfoCache(id, it.toDeviceInfoEntity()) }
        return result
    }

    private suspend fun refreshDeviceInfoCache(id: Long, deviceInfoEntity: DeviceInfoEntity) =
        deviceInfoDao.withTransaction {
            deviceInfoDao.deleteAllWithDeviceId(id)
            deviceInfoDao.insert(deviceInfoEntity)
        }
}
