package com.julianczaja.esp_monitoring_app.data.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import kotlin.coroutines.resume

// https://github.com/android/storage-samples/blob/main/ScopedStorage/app/src/main/java/com/samples/storage/scopedstorage/common/MediaStoreUtils.kt

const val PHOTOS_DIR_PATH_FORMAT = "Pictures/ESP_Monitoring/%d"
const val PHOTO_MIME_TYPE = "image/jpg"

const val TIMELAPSES_DIR_PATH_FORMAT = "Movies/ESP_Monitoring/%d"
const val TIMELAPSE_MIME_TYPE = "video/mp4"


fun checkIfPhotoExists(context: Context, photo: Photo): Boolean {
    val contentResolver = context.contentResolver

    val directory = PHOTOS_DIR_PATH_FORMAT.format(photo.deviceId)
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
    )
    val selection = "${MediaStore.Images.Media.DATA} LIKE ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("%$directory%", "%${photo.fileName}%")

    var result = false

    contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        try {
            val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(columnIndex)
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                Timber.e("${photo.fileName} already exists (id=$id, uri=$contentUri)")
                result = true
            }
        } catch (e: Exception) {
            result = false
        }
    }
    return result
}

suspend fun createPhotoUri(context: Context, photo: Photo): Uri? {
    val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    return withContext(Dispatchers.IO) {
        val content = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, photo.fileName)
            put(MediaStore.Images.Media.DATE_TAKEN, photo.dateTime.toEpochMillis())
            put(MediaStore.Images.Media.MIME_TYPE, PHOTO_MIME_TYPE)
            put(MediaStore.Images.Media.RELATIVE_PATH, PHOTOS_DIR_PATH_FORMAT.format(photo.deviceId))
        }
        return@withContext context.contentResolver.insert(imageCollection, content)
    }
}

suspend fun createTimelapseUri(context: Context, deviceId: Long): Uri? {
    val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    return withContext(Dispatchers.IO) {
        val content = ContentValues().apply {
            val now = LocalDateTime.now()
            now.toPrettyString()
            put(MediaStore.Video.Media.DISPLAY_NAME, "${deviceId}_${now.toDefaultFormatString()}")
            put(MediaStore.Video.Media.MIME_TYPE, TIMELAPSE_MIME_TYPE)
            put(MediaStore.Video.Media.RELATIVE_PATH, TIMELAPSES_DIR_PATH_FORMAT.format(deviceId))
        }
        return@withContext context.contentResolver.insert(videoCollection, content)
    }
}

suspend fun scanPhotoUri(context: Context, uri: Uri, mimeType: String = PHOTO_MIME_TYPE): Uri? {
    val cursor = context.contentResolver.query(
        uri,
        arrayOf(MediaStore.Files.FileColumns.DATA),
        null,
        null,
        null
    ) ?: throw Exception("Uri $uri could not be found")

    val path = cursor.use {
        if (!cursor.moveToFirst()) {
            throw Exception("Uri $uri could not be found")
        }

        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
    }

    return suspendCancellableCoroutine { continuation ->
        MediaScannerConnection.scanFile(
            context,
            arrayOf(path),
            arrayOf(mimeType)
        ) { _, scannedUri ->
            if (scannedUri == null) {
                continuation.cancel(Exception("File $path could not be scanned"))
            } else {
                continuation.resume(scannedUri)
            }
        }
    }
}

fun ContentResolver.observe(uri: Uri): Flow<Unit> = callbackFlow {
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            trySend(Unit)
        }
    }

    registerContentObserver(uri, true, observer)

    trySend(Unit)

    awaitClose {
        unregisterContentObserver(observer)
    }
}
