package com.julianczaja.esp_monitoring_app.domain.repository

import com.julianczaja.esp_monitoring_app.domain.model.Day
import kotlinx.coroutines.flow.Flow


interface DayRepository {

    fun getDeviceDaysLocal(deviceId: Long): Flow<List<Day>>

    suspend fun updateDeviceDaysRemote(deviceId: Long): Result<Unit>
}
