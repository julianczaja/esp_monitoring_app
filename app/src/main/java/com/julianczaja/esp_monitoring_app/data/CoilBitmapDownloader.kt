package com.julianczaja.esp_monitoring_app.data

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.BitmapDownloader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CoilBitmapDownloader @Inject constructor(
    private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BitmapDownloader {

    override suspend fun downloadBitmap(url: String): Result<Bitmap> = withContext(ioDispatcher) {
        var bitmap: Bitmap? = null
        val imageRequest = ImageRequest.Builder(context)
            .data(url)
            .diskCachePolicy(CachePolicy.DISABLED)
            .target(
                onSuccess = { bitmap = it.toBitmap() }
            ).build()

        return@withContext try {
            context.imageLoader.execute(imageRequest)
            bitmap.let {
                if (it != null) {
                    Result.success(it)
                } else {
                    Result.failure(Exception("Error while downloading image from server"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
