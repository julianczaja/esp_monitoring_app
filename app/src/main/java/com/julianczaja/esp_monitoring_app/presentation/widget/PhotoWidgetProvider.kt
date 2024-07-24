package com.julianczaja.esp_monitoring_app.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.common.Constants
import com.julianczaja.esp_monitoring_app.common.Constants.UPDATE_PHOTO_WIDGETS_PERIODIC_WORK_NAME
import com.julianczaja.esp_monitoring_app.common.Constants.WIDGET_LAST_PHOTO_FILENAME
import com.julianczaja.esp_monitoring_app.domain.model.PhotoWidgetInfo
import com.julianczaja.esp_monitoring_app.domain.repository.WidgetsRepository
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import java.time.Duration
import javax.inject.Inject

@AndroidEntryPoint
class PhotoWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var widgetsRepository: WidgetsRepository

    @Inject
    lateinit var workManager: WorkManager

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        runBlocking {
            appWidgetIds.forEach { widgetId ->
                widgetsRepository.removePhotoWidget(widgetId)
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        workManager.cancelUniqueWork(UPDATE_PHOTO_WIDGETS_PERIODIC_WORK_NAME)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = PeriodicWorkRequestBuilder<PhotoWidgetUpdateWorker>(Duration.ofMinutes(15))
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UPDATE_PHOTO_WIDGETS_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) = runBlocking {
        val widgetsInfo = widgetsRepository.getPhotoWidgetsInfo().first()

        appWidgetIds.forEach { appWidgetId ->
            widgetsInfo.firstOrNull { it.widgetId == appWidgetId }?.let { widgetInfo ->
                updateWidget(context, appWidgetId, appWidgetManager, widgetInfo)
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager,
        widgetInfo: PhotoWidgetInfo
    ) {
        Timber.d("PhotoWidgetProvider.updateWidget (appWidgetId=$appWidgetId, widgetInfo=$widgetInfo)")
        val views = RemoteViews(context.packageName, R.layout.photo_widget)

        when (widgetInfo.deviceId) {
            DeviceIdArgs.NO_VALUE -> setUpError(context, appWidgetId, views, widgetInfo)
            else -> setUpNotEmpty(context, appWidgetId, views, widgetInfo)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setUpError(
        context: Context,
        appWidgetId: Int,
        views: RemoteViews,
        widgetInfo: PhotoWidgetInfo
    ) {
        // Intent to open configuration
        val configIntent = Intent(context, PhotoWidgetConfigurationActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

        val configPendingIntent = PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ appWidgetId,
            /* intent = */ configIntent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.photo_iv, configPendingIntent)

        views.setViewVisibility(R.id.error_tv, View.VISIBLE)
        views.setViewVisibility(R.id.photo_time_tv, View.GONE)
        views.setViewVisibility(R.id.last_update_tv, View.GONE)
        views.setTextViewText(R.id.device_tv, widgetInfo.deviceName)
    }

    private fun setUpNotEmpty(
        context: Context,
        appWidgetId: Int,
        views: RemoteViews,
        widgetInfo: PhotoWidgetInfo
    ) {
        // Intent to open app
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */  appWidgetId,
            /* intent = */ Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.photo_iv, pendingIntent)

        // TODO: Intent to refresh widget
        // val intentSync = Intent(context, PhotoWidgetProvider::class.java)
        // intentSync.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        // intentSync.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        // val pendingSync = PendingIntent.getBroadcast(
        //     /* context = */ context,
        //     /* requestCode = */ appWidgetId,
        //     /* intent = */ intentSync,
        //     /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        // )
        // views.setOnClickPendingIntent(R.id.refresh_btn, pendingSync)

        val directory = File(context.filesDir, widgetInfo.deviceId.toString())
        val file = File(directory, WIDGET_LAST_PHOTO_FILENAME)
        if (file.exists()) {
            BitmapFactory.decodeFile(file.path).let {
                views.setImageViewBitmap(R.id.photo_iv, it)
            }
        } else {
            Timber.e("Last photo doesn't exist. Starting work...")
            workManager.enqueueUniqueWork(
                Constants.UPDATE_PHOTO_WIDGETS_SINGLE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PhotoWidgetUpdateWorker>().build()
            )
        }

        views.setViewVisibility(R.id.error_tv, View.GONE)

        views.setTextViewText(
            R.id.last_update_tv,
            context.getString(R.string.last_widget_update, widgetInfo.lastUpdate)
        )

        views.setTextViewText(R.id.device_tv, widgetInfo.deviceName)

        if (widgetInfo.photoDate != null) {
            views.setTextViewText(R.id.photo_time_tv, widgetInfo.photoDate)
            views.setViewVisibility(R.id.photo_time_tv, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.photo_time_tv, View.GONE)
        }
    }
}
