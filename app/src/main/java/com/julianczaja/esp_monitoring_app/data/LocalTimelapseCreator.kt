package com.julianczaja.esp_monitoring_app.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformationSession
import com.julianczaja.esp_monitoring_app.data.utils.createTimelapseUri
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.BitmapDownloader
import com.julianczaja.esp_monitoring_app.domain.TimelapseCreator
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.TimelapseData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow


class LocalTimelapseCreator @Inject constructor(
    private val context: Context,
    private val bitmapDownloader: BitmapDownloader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TimelapseCreator {

    private companion object {
        const val VIDEO_CACHE_FOLDER_NAME = "video_cache"
        const val HIGH_QUALITY_PHOTO_PREFIX = "H_"
        const val LOW_QUALITY_PHOTO_PREFIX = "L_"
    }

    override val isBusy = MutableStateFlow(false)
    override val downloadProgress = MutableStateFlow(0f)
    override val processProgress = MutableStateFlow(0f)

    private val cacheDir = File(context.cacheDir, VIDEO_CACHE_FOLDER_NAME)

    override suspend fun createTimelapse(
        photos: List<Photo>,
        isHighQuality: Boolean,
        frameRate: Int
    ): Result<TimelapseData> = withContext(ioDispatcher) {
        downloadProgress.update { 0f }
        processProgress.update { 0f }

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        downloadAndSavePhotos(photos, isHighQuality)

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

        val outputPath = File(cacheDir, "timelapse.mp4").path
        val command = listOf(
            "-y",
            "-framerate", frameRate.toString(),
            "-i", "$cacheDir/$photoPrefix%4d.jpeg",
            "-c:v", "mpeg4",
            "-q:v", "3",
            "-r", frameRate.toString(),
            outputPath
        )

        val session = FFmpegKit.execute(command.joinToString(" "))

        when {
            session.returnCode.isValueError -> {
                Timber.e("createTimelapse completed with error")
                return@withContext Result.failure(Exception(session.failStackTrace))
            }

            session.returnCode.isValueSuccess -> {
                Timber.e("createTimelapse completed - success")
                val mediaInformation = FFprobeKit.getMediaInformation(outputPath)
                return@withContext Result.success(mediaInformation.toTimelapseData(path = outputPath))
            }

            session.returnCode.isValueCancel -> {
                Timber.e("createTimelapse completed - cancelled")
                return@withContext Result.failure(Exception("Cancelled"))
            }

            else -> {
                Timber.e("createTimelapse completed - unknown (${session.returnCode})")
                return@withContext Result.failure(Exception("Unknown"))
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
        cacheDir.deleteRecursively()
    }

    private suspend fun downloadAndSavePhotos(
        photos: List<Photo>,
        isHighQuality: Boolean,
    ) {
        photos.forEachIndexed { index, photo ->
            val photoPrefix = when (isHighQuality) {
                true -> HIGH_QUALITY_PHOTO_PREFIX
                false -> LOW_QUALITY_PHOTO_PREFIX
            }
            val photoFile = File(cacheDir, "$photoPrefix%04d.jpeg".format(index))

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
            downloadProgress.update { ((index + 1) / photos.size.toFloat()) }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun MediaInformationSession.toTimelapseData(path: String): TimelapseData {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB")
            val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
            return String.format("%.1f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
        }

        val size = mediaInformation.size.toLongOrNull() ?: 0L
        val sizeString = formatBytes(size)

        val duration = mediaInformation.duration
        val durationString = duration.split(".").let { "${it[0]}.${it[1].take(1)} s" }

        return TimelapseData(
            path = path,
            size = sizeString,
            duration = durationString
        )
    }
}
