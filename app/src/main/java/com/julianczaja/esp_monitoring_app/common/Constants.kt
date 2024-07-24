package com.julianczaja.esp_monitoring_app.common

import com.julianczaja.esp_monitoring_app.BuildConfig

object Constants {

    private const val DEFAULT_BASE_URL = "http://maluch.mikr.us:30188/"
    private const val DEFAULT_BASE_URL_DEBUG = "http://192.168.1.57:8123/"

    val defaultBaseUrl = if (BuildConfig.DEBUG) DEFAULT_BASE_URL_DEBUG else DEFAULT_BASE_URL

    const val SETTINGS_DATA_STORE_NAME = "settings_data_store"
    const val DEFAULT_DEVICE_NAME = "ESP Monitoring device"

    const val UPDATE_PHOTO_WIDGETS_PERIODIC_WORK_NAME = "update_photo_widgets_periodic_work"
    const val UPDATE_PHOTO_WIDGETS_SINGLE_WORK_NAME = "update_photo_widgets_work"
    const val WIDGET_LAST_PHOTO_FILENAME = "last.jpeg"

    const val ACTION_UPDATE_PHOTO_WIDGETS = "com.julianczaja.esp_monitoring_app.ACTION_START_UPDATE_PHOTO_WIDGETS_WORK"

    const val BASE_URL_HISTORY_LIMIT = 10
    const val DEFAULT_IS_DYNAMIC_COLOR = false

    const val SCAN_DURATION_MILLIS = 5000L
    const val CONNECT_TIMEOUT_SECONDS = 5L
    const val READ_TIMEOUT_SECONDS = 10L
}
