package com.julianczaja.esp_monitoring_app.presentation.widget

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.julianczaja.esp_monitoring_app.common.Constants
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.PhotoWidgetInfo
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.WidgetsRepository
import com.julianczaja.esp_monitoring_app.domain.usecase.GetDevicesWithLastPhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class PhotoWidgetConfigurationViewModel @Inject constructor(
    getDevicesWithLastPhotoUseCase: GetDevicesWithLastPhotoUseCase,
    private val widgetsRepository: WidgetsRepository,
    private val workManager: WorkManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)

    val eventFlow = MutableSharedFlow<Event>()

    val uiState = combine(
        getDevicesWithLastPhotoUseCase(),
        _isLoading
    ) { devicesWithPhotos, isLoading ->
        UiState(devicesWithPhotos.toImmutableMap(), isLoading)
    }
        .flowOn(ioDispatcher)
        .catch { e ->
            Timber.e("PhotoWidgetConfigurationViewModel error: $e")
            eventFlow.emit(Event.Error(e.getErrorMessageId()))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(persistentMapOf(), false)
        )

    fun onDeviceClicked(device: Device, appWidgetId: Int) = viewModelScope.launch(ioDispatcher) {
        _isLoading.update { true }

        widgetsRepository.addOrUpdatePhotoWidget(
            PhotoWidgetInfo(
                widgetId = appWidgetId,
                deviceId = device.id,
                deviceName = device.name,
                lastUpdate = LocalTime.now().toPrettyString()
            )
        )

        workManager.enqueueUniqueWork(
            Constants.UPDATE_PHOTO_WIDGETS_SINGLE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<PhotoWidgetUpdateWorker>().build()
        ).await()

        eventFlow.emit(Event.FinishWithOkResult)
        _isLoading.update { false }
    }

    sealed class Event {
        data class Error(@StringRes val messageId: Int) : Event()
        data object FinishWithOkResult : Event()
    }

    data class UiState(
        val devicesWithLastPhoto: ImmutableMap<Device, Photo?>,
        val isLoading: Boolean
    )
}
