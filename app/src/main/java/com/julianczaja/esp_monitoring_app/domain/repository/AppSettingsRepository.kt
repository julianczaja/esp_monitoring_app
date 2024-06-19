package com.julianczaja.esp_monitoring_app.domain.repository

import com.julianczaja.esp_monitoring_app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {

    fun getAppSettings(): Flow<AppSettings>

    fun getIsFirstTimeUser(): Flow<Boolean>

    suspend fun setIsFirstTimeUser(isFirstTimeUser: Boolean)

    fun getBaseUrl(): Flow<String>

    suspend fun setBaseUrl(baseUrl: String)

    fun getDynamicColor(): Flow<Boolean>

    suspend fun setDynamicColor(isEnabled: Boolean)
}
