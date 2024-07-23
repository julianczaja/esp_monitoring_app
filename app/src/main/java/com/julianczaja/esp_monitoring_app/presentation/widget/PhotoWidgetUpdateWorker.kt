package com.julianczaja.esp_monitoring_app.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.BitmapDownloader
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.PhotoWidgetInfo
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.domain.repository.WidgetsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
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

        val photos = mutableMapOf<Long, Photo>()

        widgetsInfo
            .map { it.deviceId }
            .distinct()
            .forEach { deviceId ->
                try {
                    photos[deviceId] = downloadLastPhoto(deviceId)
                } catch (e: Exception) {
                    Timber.e("PhotoWidgetUpdateWorker error: $e")
                    return Result.failure()
                }
            }

        widgetsInfo.forEach { widgetInfo ->
            val deviceName = deviceRepository.getDeviceById(widgetInfo.deviceId).first()?.name
                ?: appContext.getString(
                    R.string.device_id_label_with_format,
                    widgetInfo.deviceId
                )

            widgetsRepository.addOrUpdatePhotoWidget(
                PhotoWidgetInfo(
                    widgetId = widgetInfo.widgetId,
                    deviceId = widgetInfo.deviceId,
                    deviceName = deviceName,
                    lastUpdate = LocalTime.now().toPrettyString(),
                    photoDate = photos[widgetInfo.deviceId]?.dateTime?.toPrettyString()
                )
            )
        }

        val intent = Intent(
            /* action = */ AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            /* uri = */ null,
            /* packageContext = */ appContext,
            /* cls = */ PhotoWidgetProvider::class.java
        ).apply {
            val widgetIds = widgetsInfo.map { it.widgetId }
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds.toIntArray())
        }

        appContext.sendBroadcast(intent)
        return Result.success()
    }

    private suspend fun downloadLastPhoto(deviceId: Long): Photo = withContext(ioDispatcher) {
        photoRepository.updateAllPhotosRemote(deviceId, limit = 1).getOrThrow()

        val lastPhoto = photoRepository.getLastPhotoLocal(deviceId).first() ?: throw Exception("Last photo is null")
        val bitmap = bitmapDownloader.downloadBitmap(lastPhoto.url).getOrThrow()

        val directory = File(appContext.filesDir, deviceId.toString())
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, "last.jpeg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }

        return@withContext lastPhoto
    }
}
