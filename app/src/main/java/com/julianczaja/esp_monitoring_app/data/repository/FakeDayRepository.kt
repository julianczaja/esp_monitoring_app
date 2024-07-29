package com.julianczaja.esp_monitoring_app.data.repository

import com.julianczaja.esp_monitoring_app.domain.model.Day
import com.julianczaja.esp_monitoring_app.domain.repository.DayRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeDayRepository : DayRepository {

    private val _allDaysLocalFlow = MutableSharedFlow<List<Day>>(replay = 1, extraBufferCapacity = 1)

    var updateAllDaysReturnsException = false
    var remoteDays = emptyList<Day>()

    suspend fun emitAllDaysLocalData(data: List<Day>) = _allDaysLocalFlow.emit(data)

    override fun getDeviceDaysLocal(deviceId: Long): Flow<List<Day>> = _allDaysLocalFlow

    override suspend fun updateDeviceDaysRemote(deviceId: Long): Result<Unit> {
        delay(1000)
        return if (updateAllDaysReturnsException) {
            Result.failure(Exception("error"))
        } else {
            emitAllDaysLocalData(remoteDays)
            Result.success(Unit)
        }
    }
}
