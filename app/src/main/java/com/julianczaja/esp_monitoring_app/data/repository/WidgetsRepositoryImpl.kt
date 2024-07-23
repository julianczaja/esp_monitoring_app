package com.julianczaja.esp_monitoring_app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.julianczaja.esp_monitoring_app.data.local.datastore.WidgetsDataStoreKeys.WIDGETS_INFO_KEY
import com.julianczaja.esp_monitoring_app.domain.model.PhotoWidgetInfo
import com.julianczaja.esp_monitoring_app.domain.repository.WidgetsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class WidgetsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : WidgetsRepository {

    override fun getPhotoWidgetsInfo(): Flow<List<PhotoWidgetInfo>> = dataStore.data
        .map { preferences ->
            preferences[WIDGETS_INFO_KEY]?.getWidgetInfoList() ?: emptyList()
        }

    override suspend fun addOrUpdatePhotoWidget(photoWidgetInfo: PhotoWidgetInfo) {
        dataStore.edit { preferences ->
            val photoWidgetsInfo = getPhotoWidgetsInfo().first().toMutableList()
            photoWidgetsInfo.removeIf { it.widgetId == photoWidgetInfo.widgetId }
            photoWidgetsInfo.add(photoWidgetInfo)
            preferences[WIDGETS_INFO_KEY] = photoWidgetsInfo.toJsonString()
        }
    }

    override suspend fun removePhotoWidget(widgetId: Int) {
        dataStore.edit { preferences ->
            val photoWidgetsInfo = getPhotoWidgetsInfo().first().toMutableList()
            photoWidgetsInfo.removeIf { it.widgetId == widgetId }
            preferences[WIDGETS_INFO_KEY] = photoWidgetsInfo.toJsonString()
        }
    }

    private fun String.getWidgetInfoList() = try {
        json.decodeFromString<List<PhotoWidgetInfo>>(this)
    } catch (e: Exception) {
        Timber.e("getWidgetInfoList error: $e")
        emptyList()
    }

    private fun List<PhotoWidgetInfo>.toJsonString() = json.encodeToString(this)
}
