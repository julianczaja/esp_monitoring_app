package com.julianczaja.esp_monitoring_app.presentation.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.julianczaja.esp_monitoring_app.common.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WidgetUpdateBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.ACTION_UPDATE_PHOTO_WIDGETS) {
            workManager.enqueueUniqueWork(
                Constants.UPDATE_PHOTO_WIDGETS_SINGLE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PhotoWidgetUpdateWorker>().build()
            )
        }
    }
}
