package com.julianczaja.esp_monitoring_app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.julianczaja.esp_monitoring_app.common.Constants
import com.julianczaja.esp_monitoring_app.data.local.datastore.AppSettingsDataStoreKeys.BASE_URL_HISTORY_KEY
import com.julianczaja.esp_monitoring_app.data.local.datastore.AppSettingsDataStoreKeys.BASE_URL_KEY
import com.julianczaja.esp_monitoring_app.data.local.datastore.AppSettingsDataStoreKeys.DYNAMIC_COLOR_KEY
import com.julianczaja.esp_monitoring_app.data.local.datastore.AppSettingsDataStoreKeys.FIRST_TIME_USER_KEY
import com.julianczaja.esp_monitoring_app.domain.model.AppSettings
import com.julianczaja.esp_monitoring_app.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppSettingsRepository {

    override fun getAppSettings(): Flow<AppSettings> = dataStore.data
        .map { preferences ->
            AppSettings(
                baseUrl = preferences[BASE_URL_KEY] ?: Constants.defaultBaseUrl,
                isFirstTimeUser = preferences[FIRST_TIME_USER_KEY] ?: true,
                isDynamicColor = preferences[DYNAMIC_COLOR_KEY] ?: Constants.DEFAULT_IS_DYNAMIC_COLOR,
            )
        }

    override fun getIsFirstTimeUser(): Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[FIRST_TIME_USER_KEY] ?: true
        }

    override suspend fun setIsFirstTimeUser(isFirstTimeUser: Boolean) {
        dataStore.edit { preferences ->
            preferences[FIRST_TIME_USER_KEY] = isFirstTimeUser
        }
    }

    override fun getBaseUrl(): Flow<String> = dataStore.data
        .map { preferences ->
            preferences[BASE_URL_KEY] ?: Constants.defaultBaseUrl
        }

    override suspend fun setBaseUrl(baseUrl: String, addToHistory: Boolean) {
        dataStore.edit { preferences ->
            preferences[BASE_URL_KEY] = baseUrl

            if (addToHistory) {
                val history = getBaseUrlHistory().first().toMutableSet()
                history.add(baseUrl)
                if (history.size > Constants.BASE_URL_HISTORY_LIMIT) {
                    history.remove(history.first())
                }
                preferences[BASE_URL_HISTORY_KEY] = history
            }
        }
    }

    override fun getBaseUrlHistory(): Flow<Set<String>> = dataStore.data
        .map { preferences ->
            preferences[BASE_URL_HISTORY_KEY] ?: emptySet()
        }

    override fun getDynamicColor(): Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DYNAMIC_COLOR_KEY] ?: Constants.DEFAULT_IS_DYNAMIC_COLOR
        }

    override suspend fun setDynamicColor(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = isEnabled
        }
    }
}
