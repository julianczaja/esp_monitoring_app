package com.julianczaja.esp_monitoring_app.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.julianczaja.esp_monitoring_app.common.Constants.WIDGET_LAST_PHOTO_FILENAME
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.BitmapDownloader
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.PhotoWidgetInfo
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.domain.repository.WidgetsRepository
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.time.LocalTime

@HiltWorker
class PhotoWidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val widgetsRepository: WidgetsRepository,
    private val deviceRepository: DeviceRepository,
    private val photoRepository: PhotoRepository,
    private val bitmapDownloader: BitmapDownloader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val widgetsInfo = widgetsRepository.getPhotoWidgetsInfo().first()
        Timber.d("PhotoWidgetUpdateWorker: widgetsInfo=$widgetsInfo")

        if (widgetsInfo.isEmpty()) {
            return Result.success()
        }

        widgetsInfo
            .groupBy { it.deviceId }
            .forEach { (deviceId, widgetsInfoList) ->
                try {
                    widgetsInfoList.forEach { widgetInfo ->
                        val device = deviceRepository.getDeviceById(widgetInfo.deviceId).firstOrNull()

                        when {
                            device == null -> { // may be deleted
                                widgetsRepository.addOrUpdatePhotoWidget(
                                    PhotoWidgetInfo(
                                        widgetId = widgetInfo.widgetId,
                                        deviceId = DeviceIdArgs.NO_VALUE,
                                        deviceName = "Unknown",
                                        lastUpdate = LocalTime.now().toPrettyString(),
                                        photoDate = null
                                    )
                                )
                            }

                            else -> {
                                val lastPhoto = downloadLastPhoto(deviceId)

                                widgetsRepository.addOrUpdatePhotoWidget(
                                    PhotoWidgetInfo(
                                        widgetId = widgetInfo.widgetId,
                                        deviceId = widgetInfo.deviceId,
                                        deviceName = device.name,
                                        lastUpdate = LocalTime.now().toPrettyString(),
                                        photoDate = lastPhoto.dateTime.toPrettyString()
                                    )
                                )
                            }
                        }
                    }
                    val widgetIds = widgetsInfoList.map { it.widgetId }.toIntArray()
                    updateWidgets(widgetIds)
                } catch (e: Exception) {
                    Timber.e("PhotoWidgetUpdateWorker error: $e")
                    return Result.failure()
                }
            }

        return Result.success()
    }

    private fun updateWidgets(widgetIds: IntArray) {
        val intent = Intent(
            /* action = */ AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            /* uri = */ null,
            /* packageContext = */ appContext,
            /* cls = */ PhotoWidgetProvider::class.java
        ).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        }

        appContext.sendBroadcast(intent)
    }

    private suspend fun downloadLastPhoto(deviceId: Long): Photo = withContext(ioDispatcher) {
        photoRepository.updateLastPhotoRemote(deviceId).getOrThrow()

        val lastPhoto = photoRepository.getLastPhotoLocal(deviceId).first() ?: throw Exception("Last photo is null")
        val bitmap = bitmapDownloader.downloadBitmap(lastPhoto.url).getOrThrow()

        val directory = File(appContext.filesDir, deviceId.toString())
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, WIDGET_LAST_PHOTO_FILENAME)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }

        return@withContext lastPhoto
    }
}
