package com.julianczaja.esp_monitoring_app.domain

import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.TimelapseData
import kotlinx.coroutines.flow.MutableStateFlow

interface TimelapseCreator {

    val isBusy: MutableStateFlow<Boolean>
    val downloadProgress: MutableStateFlow<Float>
    val unZipProgress: MutableStateFlow<Float>
    val processProgress: MutableStateFlow<Float>

    var photos: List<Photo>

    fun prepare(photos: List<Photo>)

    suspend fun createTimelapse(
        photos: List<Photo>,
        isHighQuality: Boolean = false,
        isReversed: Boolean = false,
        frameRate: Int = 60,
        compressionRate: Int,
    ): Result<TimelapseData>

    suspend fun saveTimelapse(deviceId: Long): Result<Unit>

    fun cancel()

    fun clear()
}
