package com.julianczaja.esp_monitoring_app.presentation.devices

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@HiltViewModel
class DevicesScreenViewModel @Inject constructor(
    deviceRepository: DeviceRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    val devicesUiState: StateFlow<DevicesScreenUiState> = deviceRepository.getAllDevices()
        .map<List<Device>, DevicesScreenUiState>(DevicesScreenUiState::Success)
        .flowOn(ioDispatcher)
        .catch { emit(DevicesScreenUiState.Error(it.getErrorMessageId())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DevicesScreenUiState.Loading
        )

    @Immutable
    sealed interface DevicesScreenUiState {
        data class Success(val devices: List<Device>) : DevicesScreenUiState
        data object Loading : DevicesScreenUiState
        data class Error(@StringRes val messageId: Int) : DevicesScreenUiState
    }
}
