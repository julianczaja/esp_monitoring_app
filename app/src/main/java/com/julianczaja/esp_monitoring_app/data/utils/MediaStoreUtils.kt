package com.julianczaja.esp_monitoring_app.data.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.ZoneOffset
import kotlin.coroutines.resume

// https://github.com/android/storage-samples/blob/main/ScopedStorage/app/src/main/java/com/samples/storage/scopedstorage/common/MediaStoreUtils.kt

const val PHOTOS_DIR_PATH_FORMAT = "Pictures/ESP_Monitoring/%d"
const val PHOTO_MIME_TYPE = "image/jpg"


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
    val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    return withContext(Dispatchers.IO) {
        val content = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, photo.fileName)
            put(MediaStore.Images.Media.DATE_ADDED, photo.dateTime.toInstant(ZoneOffset.UTC).toEpochMilli())
            put(MediaStore.Images.Media.DATE_TAKEN, photo.dateTime.toInstant(ZoneOffset.UTC).toEpochMilli())
            put(MediaStore.Images.Media.DATE_MODIFIED, photo.dateTime.toInstant(ZoneOffset.UTC).toEpochMilli())
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, PHOTOS_DIR_PATH_FORMAT.format(photo.deviceId))
        }
        return@withContext context.contentResolver.insert(imageCollection, content)
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
