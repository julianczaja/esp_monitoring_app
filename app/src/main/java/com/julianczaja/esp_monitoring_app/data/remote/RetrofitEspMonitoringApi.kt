package com.julianczaja.esp_monitoring_app.data.remote

import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettingsDto
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import retrofit2.http.*

interface RetrofitEspMonitoringApi {

    @GET(value = "/photos/{deviceId}")
    suspend fun getDevicePhotos(
        @Path("deviceId") deviceId: Long,
        @Query("from") from: Long? = null,
        @Query("to") to: Long? = null,
    ): Result<List<Photo>>

    @DELETE(value = "/photos/{fileName}")
    suspend fun removePhoto(
        @Path("fileName") fileName: String,
    ): Result<Unit>

    @GET(value = "/settings/{deviceId}")
    suspend fun getCurrentDeviceSettings(
        @Path("deviceId") deviceId: Long,
    ): Result<DeviceSettingsDto>

    @POST(value = "/settings/{deviceId}")
    suspend fun setCurrentDeviceSettings(
        @Path("deviceId") deviceId: Long,
        @Body deviceSettings: DeviceSettings,
    ): Result<Unit>
}
