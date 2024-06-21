package com.julianczaja.esp_monitoring_app.domain.repository

import com.julianczaja.esp_monitoring_app.domain.model.Timelapse


interface TimelapseRepository {

    fun readAllTimelapsesFromExternalStorage(deviceId: Long): Result<List<Timelapse>>
}
