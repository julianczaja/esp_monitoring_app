package com.julianczaja.esp_monitoring_app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.julianczaja.esp_monitoring_app.common.Constants
import com.julianczaja.esp_monitoring_app.data.local.datastore.AppSettingsDataStoreKeys.BASE_URL_KEY
import com.julianczaja.esp_monitoring_app.data.local.datastore.AppSettingsDataStoreKeys.FIRST_TIME_USER_KEY
import com.julianczaja.esp_monitoring_app.domain.model.AppSettings
import com.julianczaja.esp_monitoring_app.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppSettingsRepository {

    override fun getAppSettings(): Flow<AppSettings> = dataStore.data
        .map { preferences ->
            AppSettings(
                baseUrl = preferences[BASE_URL_KEY] ?: Constants.defaultBaseUrl,
                isFirstTimeUser = preferences[FIRST_TIME_USER_KEY] ?: true
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

    override suspend fun setBaseUrl(baseUrl: String) {
        dataStore.edit { preferences ->
            preferences[BASE_URL_KEY] = baseUrl
        }
    }
}
