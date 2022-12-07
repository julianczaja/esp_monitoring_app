package com.julianczaja.esp_monitoring_app.data.remote

import com.julianczaja.esp_monitoring_app.domain.model.Photo
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface RetrofitEspMonitoringApi {

    @GET(value = "/photos/{deviceId}")
    suspend fun getDevicePhotos(
        @Path("deviceId") deviceId: Long,
        @Query("from") from: Long? = null,
        @Query("to") to: Long? = null,
    ): List<Photo>
}
