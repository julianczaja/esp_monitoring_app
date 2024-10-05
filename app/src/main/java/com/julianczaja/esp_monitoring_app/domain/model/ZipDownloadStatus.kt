package com.julianczaja.esp_monitoring_app.domain.model


sealed class ZipDownloadStatus {
    data class Progress(val bytesDownloaded: Long) : ZipDownloadStatus()
    data object Complete : ZipDownloadStatus()
    data class Error(val exception: Exception) : ZipDownloadStatus()
}
