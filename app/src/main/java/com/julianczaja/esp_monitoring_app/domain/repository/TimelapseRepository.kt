package com.julianczaja.esp_monitoring_app.domain.repository

import com.julianczaja.esp_monitoring_app.domain.model.Timelapse
import kotlinx.coroutines.flow.Flow


interface TimelapseRepository {

    fun getAllTimelapsesFromExternalStorageFlow(deviceId: Long): Flow<Result<List<Timelapse>>>

    fun forceRefreshContent()
}
