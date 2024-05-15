package com.julianczaja.esp_monitoring_app.di

import com.julianczaja.esp_monitoring_app.data.repository.AppSettingsRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.repository.AppSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {

    @Singleton
    @Binds
    abstract fun bindAppSettingsRepository(dataStoreImpl: AppSettingsRepositoryImpl): AppSettingsRepository
}
