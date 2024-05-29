package com.julianczaja.esp_monitoring_app.data.remote

import com.julianczaja.esp_monitoring_app.domain.model.Photo
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface RetrofitEspMonitoringApi {

    @GET(value = "/photos/{deviceId}")
    suspend fun getDevicePhotos(
        @Path("deviceId") deviceId: Long
    ): Result<List<Photo>>

    @DELETE(value = "/photos/{fileName}")
    suspend fun removePhoto(
        @Path("fileName") fileName: String,
    ): Result<Unit>
}
