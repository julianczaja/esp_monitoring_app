package com.julianczaja.esp_monitoring_app.di

import com.julianczaja.esp_monitoring_app.data.repository.DeviceRepositoryImpl
import com.julianczaja.esp_monitoring_app.data.repository.PhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun provideDeviceRepository(deviceRepositoryImpl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    abstract fun providePhotoRepository(photoRepositoryImpl: PhotoRepositoryImpl): PhotoRepository
}
