package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.data.local.database.dao.DayDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DayEntity
import com.julianczaja.esp_monitoring_app.data.local.database.entity.toDay
import com.julianczaja.esp_monitoring_app.data.remote.RetrofitEspMonitoringApi
import com.julianczaja.esp_monitoring_app.domain.model.Day
import com.julianczaja.esp_monitoring_app.domain.repository.DayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DayRepositoryImpl @Inject constructor(
    private val api: RetrofitEspMonitoringApi,
    private val dayDao: DayDao
) : DayRepository {

    override fun getDeviceDaysLocal(deviceId: Long): Flow<List<Day>> = dayDao.getAll(deviceId).map { days -> days.map { it.toDay() } }

    override suspend fun updateDeviceDaysRemote(deviceId: Long): Result<Unit> {
        api.getDeviceDates(deviceId)
            .onFailure { return Result.failure(it) }
            .onSuccess { dates ->
                val days = dates.map { date -> DayEntity(deviceId = deviceId, date = date) }
                refreshDaysCache(deviceId, days)
            }
        return Result.success(Unit)
    }

    private suspend fun refreshDaysCache(deviceId: Long, days: List<DayEntity>) = dayDao.withTransaction {
        dayDao.deleteAll(deviceId)
        dayDao.insertAll(days)
    }
}
