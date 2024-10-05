package com.julianczaja.esp_monitoring_app.data.remote

import com.julianczaja.esp_monitoring_app.data.model.GetPhotosZipParams
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface RetrofitEspMonitoringTimelapseApi {

    @Streaming
    @POST(value = "/photos")
    suspend fun getPhotosZip(
        @Body params: GetPhotosZipParams
    ): ResponseBody
}
