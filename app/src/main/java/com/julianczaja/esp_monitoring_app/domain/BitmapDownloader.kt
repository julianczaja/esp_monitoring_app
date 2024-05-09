package com.julianczaja.esp_monitoring_app.domain

import android.graphics.Bitmap

interface BitmapDownloader {

    suspend fun downloadBitmap(url: String): Result<Bitmap>
}