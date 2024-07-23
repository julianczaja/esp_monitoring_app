package com.julianczaja.esp_monitoring_app.data.repository

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.julianczaja.esp_monitoring_app.data.local.database.dao.PhotoDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.toPhoto
import com.julianczaja.esp_monitoring_app.data.remote.RetrofitEspMonitoringApi
import com.julianczaja.esp_monitoring_app.data.utils.EXIF_UTC_OFFSET
import com.julianczaja.esp_monitoring_app.data.utils.PHOTOS_DIR_PATH_FORMAT
import com.julianczaja.esp_monitoring_app.data.utils.checkIfPhotoExists
import com.julianczaja.esp_monitoring_app.data.utils.createPhotoUri
import com.julianczaja.esp_monitoring_app.data.utils.millisToDefaultFormatLocalDateTime
import com.julianczaja.esp_monitoring_app.data.utils.observe
import com.julianczaja.esp_monitoring_app.data.utils.scanPhotoUri
import com.julianczaja.esp_monitoring_app.data.utils.toExifString
import com.julianczaja.esp_monitoring_app.domain.BitmapDownloader
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.PhotoAlreadyExistsException
import com.julianczaja.esp_monitoring_app.domain.model.toPhotoEntity
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
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

    private val projection = arrayOf(
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media._ID,
    )

    override fun getAllPhotosLocal(deviceId: Long): Flow<List<Photo>> =
        photoDao.getAll(deviceId).map { photos -> photos.map { it.toPhoto() } }

    override fun getLastPhotoLocal(deviceId: Long): Flow<Photo?> =
        photoDao.getLast(deviceId).map { it?.toPhoto() }

    override fun getPhotoByFileNameLocal(fileName: String): Flow<Photo?> =
        photoDao.getByFileName(fileName).map { photo -> photo?.toPhoto() }

    override suspend fun removePhotoByFileNameLocal(fileName: String): Result<Unit> = try {
        photoDao.deleteByFileName(fileName)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removePhotoByFileNameRemote(fileName: String) = api.removePhoto(fileName)

    override suspend fun updateAllPhotosRemote(deviceId: Long, limit: Int?): Result<Unit> {
        api.getDevicePhotos(deviceId, limit)
            .onFailure { return Result.failure(it) }
            .onSuccess { photos ->
                when {
                    limit == null -> refreshPhotosCache(deviceId, photos)
                    else -> insertIfNotExist(photos)
                }
            }
        return Result.success(Unit)
    }

    override suspend fun downloadPhotoAndSaveToExternalStorage(photo: Photo): Result<Unit> {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return Result.failure(Exception("External storage not mounted"))
        }

        val contentResolver = context.contentResolver

        if (checkIfPhotoExists(context, photo)) {
            return Result.failure(PhotoAlreadyExistsException())
        }

        val bitmap: Bitmap = bitmapDownloader.downloadBitmap(photo.url).getOrElse {
            return Result.failure(Exception("Error while downloading image from server"))
        }

        val photoUri = createPhotoUri(context, photo) ?: return Result.failure(Exception("Image URI is null"))

        try {
            contentResolver.openOutputStream(photoUri)?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            contentResolver.openFileDescriptor(photoUri, "rw")?.use {
                ExifInterface(it.fileDescriptor)
                    .apply {
                        setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, photo.dateTime.toExifString())
                        setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, EXIF_UTC_OFFSET)
                        saveAttributes()
                    }
            }
            scanPhotoUri(context, photoUri)
        } catch (e: IOException) {
            Timber.e(e)
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    @OptIn(FlowPreview::class)
    override fun getAllSavedPhotosFromExternalStorageFlow(deviceId: Long): Flow<Result<List<Photo>>> = flow {
        context.contentResolver.observe(uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            .debounce(300)
            .collect {
                val result = readAllSavedPhotosFromExternalStorage(deviceId)
                emit(result)
            }
    }

    private fun readAllSavedPhotosFromExternalStorage(deviceId: Long): Result<List<Photo>> {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return Result.failure(Exception("External storage not mounted"))
        }

        val directory = PHOTOS_DIR_PATH_FORMAT.format(deviceId)
        val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%${directory}%")

        val photos = mutableListOf<Photo>()

        return try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val nameColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val dateColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                while (cursor.moveToNext()) {
                    val fileName = cursor.getString(nameColumnIndex)
                    val width = cursor.getString(widthColumnIndex)
                    val height = cursor.getString(heightColumnIndex)
                    val date = cursor.getLong(dateColumnIndex)
                    val id = cursor.getLong(idColumnIndex)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    try {
                        val photo = Photo(
                            deviceId = deviceId,
                            dateTime = date.millisToDefaultFormatLocalDateTime(),
                            fileName = fileName,
                            size = "${width}x${height}",
                            url = contentUri.toString(),
                            thumbnailUrl = contentUri.toString(),
                            isSaved = true
                        )
                        photos.add(photo)
                    } catch (e: Exception) {
                        Timber.e(
                            "Can't parse photo from (fileName=$fileName, width=$width, height=$height, date=$date, id=$id)"
                        )
                    }
                }
            }
            Result.success(photos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeSavedPhotoFromExternalStorage(photo: Photo): Result<Unit> {
        try {
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(photo.fileName)

            context.contentResolver.delete(
                Uri.parse(photo.url),
                selection,
                selectionArgs
            )
            return Result.success(Unit)
        } catch (e: Exception) {
            // TODO: Handle RecoverableSecurityException
            return Result.failure(e)
        }
    }

    override fun forceRefreshSavedPhotosContent() {
        context.contentResolver.notifyChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
    }

    private suspend fun refreshPhotosCache(deviceId: Long, photos: List<Photo>) = photoDao.withTransaction {
        photoDao.deleteAll(deviceId)
        photoDao.insertAll(photos.map(Photo::toPhotoEntity))
    }


    private suspend fun insertIfNotExist(photos: List<Photo>) = photoDao.withTransaction {
        photoDao.withTransaction {
            photos.forEach { photo ->
                if (photoDao.countByFilename(photo.fileName) == 0) {
                    photoDao.insert(photo.toPhotoEntity())
                }
            }
        }
    }
}
