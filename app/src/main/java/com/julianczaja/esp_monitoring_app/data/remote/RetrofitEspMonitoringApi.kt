package com.julianczaja.esp_monitoring_app.data.remote

import com.julianczaja.esp_monitoring_app.domain.model.DeviceInfo
import com.julianczaja.esp_monitoring_app.domain.model.DeviceServerSettings
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RetrofitEspMonitoringApi {

    @GET(value = "/dates/{deviceId}")
    suspend fun getDeviceDates(
        @Path("deviceId") deviceId: Long
    ): Result<List<String>>

    @GET(value = "/photos/{deviceId}/{date}")
    suspend fun getDevicePhotosByDate(
        @Path("deviceId") deviceId: Long,
        @Path("date") date: String
    ): Result<List<Photo>>

    @GET(value = "/last_photo/{deviceId}")
    suspend fun getDeviceLastPhoto(
        @Path("deviceId") deviceId: Long
    ): Result<Photo>

    @DELETE(value = "/photos/{fileName}")
    suspend fun removePhoto(
        @Path("fileName") fileName: String,
    ): Result<Unit>

    @POST(value = "/photos/remove")
    suspend fun removePhotos(
        @Body params: List<String>
    ): Result<Unit>

    @GET(value = "/device/{deviceId}")
    suspend fun getDeviceInfo(
        @Path("deviceId") deviceId: Long
    ): Result<DeviceInfo>

    @GET(value = "/device/{deviceId}/settings")
    suspend fun getDeviceServerSettings(
        @Path("deviceId") deviceId: Long
    ): Result<DeviceServerSettings>

    @POST(value = "/device/{deviceId}/settings")
    suspend fun updateDeviceServerSettings(
        @Path("deviceId") deviceId: Long,
        @Body settings: DeviceServerSettings
    ): Result<DeviceServerSettings>
}
