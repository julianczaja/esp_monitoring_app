package com.julianczaja.esp_monitoring_app.domain.repository

import com.julianczaja.esp_monitoring_app.domain.model.PhotoWidgetInfo
import kotlinx.coroutines.flow.Flow

interface WidgetsRepository {
    fun getPhotoWidgetsInfo(): Flow<List<PhotoWidgetInfo>>
    suspend fun addOrUpdatePhotoWidget(photoWidgetInfo: PhotoWidgetInfo)
    suspend fun removePhotoWidget(widgetId: Int)
}
