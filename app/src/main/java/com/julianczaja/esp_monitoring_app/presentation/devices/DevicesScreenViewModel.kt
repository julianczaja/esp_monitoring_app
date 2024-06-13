package com.julianczaja.esp_monitoring_app.presentation.devices

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class DevicesScreenViewModel @Inject constructor(
    deviceRepository: DeviceRepository,
    photoRepository: PhotoRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> = deviceRepository.getAllDevices()
        .flatMapLatest { devices ->
            if (devices.isEmpty()) return@flatMapLatest flowOf(emptyMap<Device, Photo?>())

            val flows = devices.map { device ->
                photoRepository.getLastPhotoLocal(device.id).map { photo ->
                    device to photo
                }
            }
            return@flatMapLatest combine(flows) { it.toMap() }
        }
        .map<Map<Device, Photo?>, UiState>(UiState::Success)
        .flowOn(ioDispatcher)
        .catch {
            Timber.e(it)
            emit(UiState.Error(it.getErrorMessageId()))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    @Immutable
    sealed interface UiState {
        data class Success(val devicesWithLastPhoto: Map<Device, Photo?>) : UiState
        data object Loading : UiState
        data class Error(@StringRes val messageId: Int) : UiState
    }
}
