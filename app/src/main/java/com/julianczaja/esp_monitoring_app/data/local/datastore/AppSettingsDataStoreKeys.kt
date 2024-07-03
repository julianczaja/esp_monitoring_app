package com.julianczaja.esp_monitoring_app.data.local.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object AppSettingsDataStoreKeys {
    val FIRST_TIME_USER_KEY = booleanPreferencesKey("first_time_user_key")
    val BASE_URL_KEY = stringPreferencesKey("base_url_key")
    val BASE_URL_HISTORY_KEY = stringSetPreferencesKey("base_url_history_key")
    val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_colors_key")
}
