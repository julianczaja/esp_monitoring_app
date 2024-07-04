package com.julianczaja.esp_monitoring_app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.julianczaja.esp_monitoring_app.BuildConfig
import com.julianczaja.esp_monitoring_app.common.Constants
import com.julianczaja.esp_monitoring_app.data.BleLocationManager
import com.julianczaja.esp_monitoring_app.data.BluetoothManager
import com.julianczaja.esp_monitoring_app.data.CoilBitmapDownloader
import com.julianczaja.esp_monitoring_app.data.LocalTimelapseCreator
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.data.local.database.EspMonitoringDatabase
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceDao
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceInfoDao
import com.julianczaja.esp_monitoring_app.data.local.database.dao.PhotoDao
import com.julianczaja.esp_monitoring_app.data.remote.HostSelectionInterceptor
import com.julianczaja.esp_monitoring_app.data.remote.RetrofitEspMonitoringApi
import com.julianczaja.esp_monitoring_app.data.repository.DeviceInfoRepositoryImpl
import com.julianczaja.esp_monitoring_app.data.repository.DeviceRepositoryImpl
import com.julianczaja.esp_monitoring_app.data.repository.PhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.data.repository.TimelapseRepositoryImpl
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.BitmapDownloader
import com.julianczaja.esp_monitoring_app.domain.TimelapseCreator
import com.julianczaja.esp_monitoring_app.domain.model.ResultCallAdapterFactory
import com.julianczaja.esp_monitoring_app.domain.repository.AppSettingsRepository
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceInfoRepository
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.domain.repository.TimelapseRepository
import com.julianczaja.esp_monitoring_app.domain.usecase.SelectOrDeselectAllPhotosByDateUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.time.Duration
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestAppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    @Provides
    @Singleton
    fun provideRetrofitEspMonitoringApi(
        appSettingsRepository: AppSettingsRepository,
        networkJson: Json
    ): RetrofitEspMonitoringApi = Retrofit.Builder()
        .baseUrl(Constants.defaultBaseUrl)
        .client(
            OkHttpClient().newBuilder()
                .addInterceptor(HostSelectionInterceptor(appSettingsRepository))
                .apply {
                    if (BuildConfig.DEBUG)
                        addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                }
                .connectTimeout(Duration.ofSeconds(Constants.CONNECT_TIMEOUT_SECONDS))
                .readTimeout(Duration.ofSeconds(Constants.READ_TIMEOUT_SECONDS))
                .build()
        )
        .addCallAdapterFactory(ResultCallAdapterFactory())
        .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RetrofitEspMonitoringApi::class.java)

    @Provides
    @Singleton
    fun provideNetworkManager(@ApplicationContext context: Context) = NetworkManager(context)

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context) = BluetoothManager(context)

    @Provides
    @Singleton
    fun provideBleLocationManager(@ApplicationContext context: Context) = BleLocationManager(context)

    @Provides
    @Singleton
    fun provideEspMonitoringDatabase(@ApplicationContext context: Context) = Room.inMemoryDatabaseBuilder(
        context,
        EspMonitoringDatabase::class.java
    ).build()

    @Provides
    @Singleton
    fun provideDeviceDao(espMonitoringDatabase: EspMonitoringDatabase) = espMonitoringDatabase.deviceDao()

    @Provides
    @Singleton
    fun providePhotoDao(espMonitoringDatabase: EspMonitoringDatabase) = espMonitoringDatabase.photoDao()

    @Provides
    @Singleton
    fun provideDeviceInfoDao(espMonitoringDatabase: EspMonitoringDatabase) = espMonitoringDatabase.deviceInfoDao()

    @Provides
    @Singleton
    fun provideBitmapDownloader(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): BitmapDownloader = CoilBitmapDownloader(context, ioDispatcher)

    @Provides
    @Singleton
    fun provideTimelapseCreator(
        @ApplicationContext context: Context,
        bitmapDownloader: BitmapDownloader,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): TimelapseCreator = LocalTimelapseCreator(context, bitmapDownloader, ioDispatcher)

    @Provides
    @Singleton
    fun provideDeviceRepository(deviceDao: DeviceDao): DeviceRepository = DeviceRepositoryImpl(deviceDao)

    @Provides
    @Singleton
    fun providePhotoRepository(
        @ApplicationContext context: Context,
        photoDao: PhotoDao,
        api: RetrofitEspMonitoringApi,
        bitmapDownloader: BitmapDownloader
    ): PhotoRepository = PhotoRepositoryImpl(context, photoDao, api, bitmapDownloader)

    @Provides
    @Singleton
    fun provideDeviceInfoRepository(
        deviceInfoDao: DeviceInfoDao,
        api: RetrofitEspMonitoringApi
    ): DeviceInfoRepository = DeviceInfoRepositoryImpl(api, deviceInfoDao)

    @Provides
    @Singleton
    fun provideTimelapseRepository(
        @ApplicationContext context: Context
    ): TimelapseRepository = TimelapseRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext applicationContext: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = {
                applicationContext.preferencesDataStoreFile(Constants.SETTINGS_DATA_STORE_NAME)
            }
        )

    @Provides
    @Singleton
    fun provideSelectOrDeselectAllPhotosByDateUseCase() = SelectOrDeselectAllPhotosByDateUseCase()
}
