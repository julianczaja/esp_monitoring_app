package com.julianczaja.esp_monitoring_app.domain.model


sealed class ZipDownloadStatus {
    data class Progress(val progress: Float) : ZipDownloadStatus()
    data class Complete(val data: ByteArray) : ZipDownloadStatus()
    data class Error(val exception: Exception) : ZipDownloadStatus()
}
