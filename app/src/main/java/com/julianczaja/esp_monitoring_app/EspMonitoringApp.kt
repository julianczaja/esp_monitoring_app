package com.julianczaja.esp_monitoring_app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class EspMonitoringApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}