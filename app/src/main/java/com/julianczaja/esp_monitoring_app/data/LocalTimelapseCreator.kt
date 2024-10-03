package com.julianczaja.esp_monitoring_app.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformation
import com.julianczaja.esp_monitoring_app.data.utils.createTimelapseUri
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.BitmapDownloader
import com.julianczaja.esp_monitoring_app.domain.TimelapseCreator
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.TimelapseCancelledException
import com.julianczaja.esp_monitoring_app.domain.model.TimelapseData
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject


class LocalTimelapseCreator @Inject constructor(
    private val context: Context,
    private val bitmapDownloader: BitmapDownloader,
    private val photoRepository: PhotoRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TimelapseCreator {

    private companion object {
        const val VIDEO_CACHE_FOLDER_NAME = "video_cache"
        const val HIGH_QUALITY_PHOTO_PREFIX = "H_"
        const val LOW_QUALITY_PHOTO_PREFIX = "L_"
    }

    override val isBusy = MutableStateFlow(false)
    override val downloadProgress = MutableStateFlow(0f)
    override val unZipProgress = MutableStateFlow(0f)
    override val processProgress = MutableStateFlow(0f)

    private val cacheDir = File(context.cacheDir, VIDEO_CACHE_FOLDER_NAME)

    override var photos: List<Photo> = emptyList()

    override fun prepare(photos: List<Photo>) {
        clear()
        this.photos = photos
    }

    override suspend fun createTimelapse(
        photos: List<Photo>,
        isHighQuality: Boolean,
        isReversed: Boolean,
        frameRate: Int,
        compressionRate: Int,
    ): Result<TimelapseData> = withContext(ioDispatcher) {
        downloadProgress.update { 0f }
        unZipProgress.update { 0f }
        processProgress.update { 0f }

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        downloadAndSavePhotos(photos, isHighQuality, isReversed)

        val photoPrefix = when (isHighQuality) {
            true -> HIGH_QUALITY_PHOTO_PREFIX
            false -> LOW_QUALITY_PHOTO_PREFIX
        }

        FFmpegKitConfig.enableStatisticsCallback { statistics ->
            processProgress.update { (statistics.videoFrameNumber / photos.size.toFloat()) }
        }

        FFmpegKitConfig.enableLogCallback { log ->
            Timber.d("Session log: ${log.message}")
        }

        val aspectRatio = findBestAspectRatio(photos)
        val aspectRatioString = "${aspectRatio.first}:${aspectRatio.second}"

        val outputPath = File(cacheDir, "timelapse.mp4").path
        val command = listOf(
            "-y",
            "-framerate", frameRate.toString(),
            "-i", "$cacheDir/$photoPrefix%4d.jpeg",
            "-c:v", "mpeg4",
            "-q:v", compressionRate.toString(),
            "-vf", "scale=$aspectRatioString:force_original_aspect_ratio=decrease,pad=$aspectRatioString:(ow-iw)/2:(oh-ih)/2",
            "-r", frameRate.toString(),
            outputPath
        )

        val session = FFmpegKit.execute(command.joinToString(" "))

        when {
            session.returnCode.isValueError -> {
                Timber.e("createTimelapse completed with error: ${session.failStackTrace}")
                return@withContext Result.failure(Exception(session.failStackTrace))
            }

            session.returnCode.isValueSuccess -> {
                Timber.e("createTimelapse completed - success")
                val mediaInformation = FFprobeKit.getMediaInformation(outputPath).mediaInformation
                val timelapseData = mediaInformation.toTimelapseData(outputPath)
                return@withContext Result.success(timelapseData)
            }

            session.returnCode.isValueCancel -> {
                Timber.e("createTimelapse completed - cancelled")
                return@withContext Result.failure(TimelapseCancelledException())
            }

            else -> {
                Timber.e("createTimelapse completed - unknown (${session.returnCode})")
                return@withContext Result.failure(Exception("Unknown timelapse return code (${session.returnCode})"))
            }
        }
    }

    override suspend fun saveTimelapse(deviceId: Long): Result<Unit> = withContext(ioDispatcher) {
        val timelapseCacheFile = File(cacheDir, "timelapse.mp4")
        if (!timelapseCacheFile.exists() || !timelapseCacheFile.isFile) {
            return@withContext Result.failure(Exception("Cannot find cached timelapse file"))
        }

        val externalTimelapseUri = createTimelapseUri(context, deviceId)
            ?: return@withContext Result.failure(Exception("externalTimelapseUri is null"))

        return@withContext try {
            isBusy.emit(true)
            FileInputStream(timelapseCacheFile).use { input ->
                context.contentResolver.openOutputStream(externalTimelapseUri)?.use { output ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    Timber.d("Timelapse file successfully copied to external storage")
                }
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e("Failed to copy file to external storage: ${e.message}")
            Result.failure(e)
        } finally {
            isBusy.emit(false)
        }
    }

    override fun cancel() {
        FFmpegKit.cancel()
    }

    override fun clear() {
        this.photos = emptyList()
        cacheDir.deleteRecursively()
    }

    private suspend fun downloadAndSavePhotos(
        photos: List<Photo>,
        isHighQuality: Boolean,
        isReversed: Boolean
    ) {
        val (savedPhotos, remotePhotos) = photos
            .distinctBy { it.fileName }
            .partition { it.isSaved }

        val photoPrefix = when (isHighQuality) {
            true -> HIGH_QUALITY_PHOTO_PREFIX
            false -> LOW_QUALITY_PHOTO_PREFIX
        }

        var progress = 1

        // saved photos
        savedPhotos.forEach { photo ->
            val photoFile = File(cacheDir, photo.fileName)

            if (photoFile.exists() && photoFile.length() == 0L) {
                photoFile.delete()
            }
            if (!photoFile.exists()) {
                photoFile.createNewFile()

                val url = when (isHighQuality) {
                    true -> photo.url
                    false -> photo.thumbnailUrl
                }
                val bitmap = bitmapDownloader.downloadBitmap(url).getOrElse { throw it }
                photoFile.outputStream().use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                }
            }
            downloadProgress.update { ((progress++) / photos.size.toFloat()) }
        }

        if (savedPhotos.isEmpty()) {
            downloadProgress.update { .3f }
        }

        // remote
        val remoteFileNames = remotePhotos.map { it.fileName }
        if (remoteFileNames.isNotEmpty()) {
            // TODO: Implement download progress
            val zipByteArray = photoRepository.getPhotosZipRemote(remoteFileNames, isHighQuality).getOrThrow()
            downloadProgress.update { 1f }

            ZipInputStream(zipByteArray.inputStream()).use { zipIn ->
                progress = 1
                var entry: ZipEntry? = zipIn.nextEntry

                while (entry != null) {
                    val photoFile = File(cacheDir, entry.name)
                    if (photoFile.exists() && photoFile.length() == 0L) {
                        photoFile.delete()
                    }
                    if (!photoFile.exists()) {
                        photoFile.createNewFile()
                        FileOutputStream(photoFile).use { outputStream ->
                            zipIn.copyTo(outputStream)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry

                    unZipProgress.update { ((progress++) / remoteFileNames.size.toFloat()) }
                }
            }
        } else {
            downloadProgress.update { 1f }
        }

        // rename
        var index = 0
        cacheDir.listFiles()
            ?.filter { it.isFile }
            ?.run { if (isReversed) sortedByDescending { it.name } else sortedBy { it.name } }
            ?.forEach {
                it.renameTo(File(cacheDir, "$photoPrefix%04d.jpeg".format(index++)))
            }
    }

    @SuppressLint("DefaultLocale")
    private fun MediaInformation.toTimelapseData(path: String): TimelapseData {
        val durationSeconds = duration
            .split(".")
            .let { it[0].toFloat() + (it[1].take(1).toFloat() / 10f) }

        return TimelapseData(
            path = path,
            sizeBytes = size.toLongOrNull() ?: 0L,
            durationSeconds = durationSeconds
        )
    }

    /** Returns the most common aspect ratio through all photos */
    private fun findBestAspectRatio(photos: List<Photo>): Pair<Int, Int> {
        val ratiosCount = mutableMapOf<Pair<Int, Int>, Int>()
        val ratios = photos.map { photo ->
            val split = photo.size
                .split(" ")
                .first()
                .split("x")
            val width = split[0].toInt()
            val height = split[1].toInt()

            return@map width to height
        }

        for (ratio in ratios) {
            ratiosCount[ratio] = ratiosCount.getOrDefault(ratio, 0) + 1
        }

        return ratiosCount.maxBy { it.value }.key
    }
}
