package com.julianczaja.esp_monitoring_app.data.local.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AppSettingsDataStoreKeys {
    val FIRST_TIME_USER_KEY = booleanPreferencesKey("first_time_user_key")
    val BASE_URL_KEY = stringPreferencesKey("base_url_key")
}
