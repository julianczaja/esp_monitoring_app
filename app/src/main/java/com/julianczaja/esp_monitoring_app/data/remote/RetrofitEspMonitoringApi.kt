package com.julianczaja.esp_monitoring_app.data.remote

import com.julianczaja.esp_monitoring_app.data.model.GetPhotosZipParams
import com.julianczaja.esp_monitoring_app.domain.model.DeviceInfo
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import okhttp3.ResponseBody
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

    @GET(value = "/device/{deviceId}")
    suspend fun updateDeviceInfo(
        @Path("deviceId") deviceId: Long
    ): Result<DeviceInfo>

    @POST(value = "/photos")
    suspend fun getPhotosZip(
        @Body params: GetPhotosZipParams
    ): Result<ResponseBody>
}
