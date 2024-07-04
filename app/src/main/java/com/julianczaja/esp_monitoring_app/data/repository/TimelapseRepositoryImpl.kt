package com.julianczaja.esp_monitoring_app.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.julianczaja.esp_monitoring_app.data.utils.TIMELAPSES_DIR_PATH_FORMAT
import com.julianczaja.esp_monitoring_app.data.utils.millisToDefaultFormatLocalDateTime
import com.julianczaja.esp_monitoring_app.data.utils.observe
import com.julianczaja.esp_monitoring_app.domain.model.Timelapse
import com.julianczaja.esp_monitoring_app.domain.model.TimelapseData
import com.julianczaja.esp_monitoring_app.domain.repository.TimelapseRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class TimelapseRepositoryImpl @Inject constructor(
    private val context: Context
) : TimelapseRepository {

    @OptIn(FlowPreview::class)
    override fun getAllTimelapsesFromExternalStorageFlow(deviceId: Long): Flow<Result<List<Timelapse>>> = flow {
        context.contentResolver.observe(uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .debounce(300)
            .collect {
                val result = readAllTimelapsesFromExternalStorage(deviceId)
                emit(result)
            }
    }

    override fun forceRefreshContent() {
        context.contentResolver.notifyChange(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null)
    }

    private fun readAllTimelapsesFromExternalStorage(deviceId: Long): Result<List<Timelapse>> {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return Result.failure(Exception("External storage not mounted"))
        }

        val timelapses = mutableListOf<Timelapse>()
        val contentResolver = context.contentResolver

        val directory = TIMELAPSES_DIR_PATH_FORMAT.format(deviceId)
        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media._ID,
        )
        val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%${directory}%")
        val sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC"

        return try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val nameColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val sizeColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

                while (cursor.moveToNext()) {
                    val fileName = cursor.getString(nameColumnIndex)
                    val date = cursor.getString(dateColumnIndex).toLongOrNull() ?: 0L
                    val size = cursor.getString(sizeColumnIndex).toLongOrNull() ?: 0L
                    val duration = cursor.getString(durationColumnIndex).toLongOrNull() ?: 0L
                    val id = cursor.getLong(idColumnIndex)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    try {
                        val timelapse = Timelapse(
                            addedDateTime = (date * 1000L).millisToDefaultFormatLocalDateTime(),
                            data = TimelapseData(
                                path = contentUri.toString(),
                                sizeBytes = size,
                                durationSeconds = duration / 1000f
                            )
                        )
                        timelapses.add(timelapse)
                    } catch (e: Exception) {
                        Timber.e(
                            "Can't parse timelapse from: fileName=$fileName, date=$date, size=$size, " +
                                    "duration=$duration, id=$id, contentUri=$contentUri"
                        )
                    }
                }
            }
            Result.success(timelapses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
