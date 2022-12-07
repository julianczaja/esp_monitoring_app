package com.julianczaja.esp_monitoring_app.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.julianczaja.esp_monitoring_app.*
import com.julianczaja.esp_monitoring_app.data.local.database.EspMonitoringDatabase
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceDao
import com.julianczaja.esp_monitoring_app.data.local.database.dao.PhotoDao
import com.julianczaja.esp_monitoring_app.data.remote.RetrofitEspMonitoringApi
import com.julianczaja.esp_monitoring_app.data.repository.DeviceRepositoryImpl
import com.julianczaja.esp_monitoring_app.data.repository.PhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideRetrofitEspMonitoringApi(networkJson: Json): RetrofitEspMonitoringApi = Retrofit.Builder()
        .baseUrl("http://192.168.1.11:8123/") // FIXME
//        .baseUrl("http://127.0.0.1:8123/") // FIXME
//        .baseUrl("http://10.0.2.2:8123/") // FIXME
        .client(OkHttpClient().newBuilder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
                    )
                }
            }
            .build())
        .addConverterFactory(
            @OptIn(ExperimentalSerializationApi::class)
            networkJson.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(RetrofitEspMonitoringApi::class.java)

    @Provides
    @Singleton
    fun provideEspMonitoringDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(
        context,
        EspMonitoringDatabase::class.java,
        "esp_monitoring_database"
    ).build()

    @Provides
    @Singleton
    fun provideDeviceDao(espMonitoringDatabase: EspMonitoringDatabase) = espMonitoringDatabase.deviceDao()

    @Provides
    @Singleton
    fun providePhotoDao(espMonitoringDatabase: EspMonitoringDatabase) = espMonitoringDatabase.photoDao()

    @Provides
    @Singleton
    fun provideDeviceRepository(deviceDao: DeviceDao): DeviceRepository = DeviceRepositoryImpl(deviceDao)

    @Provides
    @Singleton
    fun providePhotoRepository(photoDao: PhotoDao, api: RetrofitEspMonitoringApi): PhotoRepository = PhotoRepositoryImpl(photoDao, api)
}
