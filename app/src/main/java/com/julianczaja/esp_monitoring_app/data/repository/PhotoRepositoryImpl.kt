package com.julianczaja.esp_monitoring_app.data.repository

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.julianczaja.esp_monitoring_app.data.local.database.dao.PhotoDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.toPhoto
import com.julianczaja.esp_monitoring_app.data.remote.RetrofitEspMonitoringApi
import com.julianczaja.esp_monitoring_app.data.utils.PHOTOS_DIR_PATH_FORMAT
import com.julianczaja.esp_monitoring_app.data.utils.canReadInMediaStore
import com.julianczaja.esp_monitoring_app.data.utils.canWriteInMediaStore
import com.julianczaja.esp_monitoring_app.data.utils.checkIfPhotoExists
import com.julianczaja.esp_monitoring_app.data.utils.createPhotoUri
import com.julianczaja.esp_monitoring_app.data.utils.scanPhotoUri
import com.julianczaja.esp_monitoring_app.domain.BitmapDownloader
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.toPhotoEntity
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class PhotoRepositoryImpl @Inject constructor(
    private val context: Context,
    private val photoDao: PhotoDao,
    private val api: RetrofitEspMonitoringApi,
    private val bitmapDownloader: BitmapDownloader
) : PhotoRepository {

    override fun getAllPhotosLocal(deviceId: Long): Flow<List<Photo>> =
        photoDao.getAll(deviceId).map { photos -> photos.map { it.toPhoto() } }

    override fun getPhotoByFileNameLocal(fileName: String): Flow<Photo?> =
        photoDao.getByFileName(fileName).map { photo -> photo?.toPhoto() }

    override suspend fun removePhotoByFileNameLocal(fileName: String): Result<Unit> = try {
        photoDao.deleteByFileName(fileName)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removePhotoByFileNameRemote(fileName: String) = api.removePhoto(fileName)

    override suspend fun updateAllPhotosRemote(deviceId: Long, from: Long?, to: Long?): Result<Unit> {
        var result = Result.success(Unit)
        api.getDevicePhotos(deviceId, from, to)
            .onFailure { result = Result.failure(it) }
            .onSuccess { refreshPhotosCache(deviceId, it) }
        return result
    }


    override suspend fun downloadPhotoAndSaveToExternalStorage(photo: Photo): Result<Unit> {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return Result.failure(Exception("External storage not mounted"))
        }

        val canWriteInMediaStore = canWriteInMediaStore(context)
        val canReadInMediaStore = canReadInMediaStore(context)

        if (!canReadInMediaStore || !canWriteInMediaStore) {
            Timber.e("Permissions needed (canWriteInMediaStore=$canWriteInMediaStore, canReadInMediaStore=$canReadInMediaStore)")
        }

        val contentResolver = context.contentResolver

        if (checkIfPhotoExists(context, photo)) {
            return Result.failure(Exception("Photo already exists"))
        }

        val photoUri = createPhotoUri(context, photo) ?: return Result.failure(Exception("Image URI is null"))

        val bitmap: Bitmap = bitmapDownloader.downloadBitmap(photo.url).getOrElse {
            return Result.failure(Exception("Error while downloading image from server"))
        }

        try {
            contentResolver.openOutputStream(photoUri)?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            scanPhotoUri(context, photoUri)
        } catch (e: IOException) {
            e.printStackTrace()
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    override suspend fun readAllSavedPhotosFromExternalStorage(deviceId: Long): Result<List<Uri>> {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return Result.failure(Exception("External storage not mounted"))
        }

        val canReadInMediaStore = canReadInMediaStore(context)
        if (!canReadInMediaStore) {
            Timber.e("Permissions needed (canReadInMediaStore=false)")
        }

        val photos = mutableListOf<Uri>()
        val contentResolver = context.contentResolver

        val directory = PHOTOS_DIR_PATH_FORMAT.format(deviceId)
        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media._ID,
        )
        val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%${directory}%")

        return try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                // val nameColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val idColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val photoPath = cursor.getString(dataColumnIndex)
                    // val fileName = cursor.getString(nameColumnIndex)
                    val id = cursor.getLong(idColumnIndex)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    Timber.e("Found id: $id, uri=$contentUri, photoPath: $photoPath")
                    photos.add(contentUri)
                }
            }
            Result.success(photos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun refreshPhotosCache(deviceId: Long, photos: List<Photo>) = photoDao.withTransaction {
        photoDao.deleteAll(deviceId)
        photoDao.insertAll(photos.map(Photo::toPhotoEntity))
    }
}
