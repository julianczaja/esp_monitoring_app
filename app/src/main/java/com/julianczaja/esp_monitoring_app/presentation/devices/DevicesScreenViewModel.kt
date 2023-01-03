package com.julianczaja.esp_monitoring_app.presentation.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.annotation.concurrent.Immutable
import javax.inject.Inject


@HiltViewModel
class DevicesScreenViewModel @Inject constructor(
    deviceRepository: DeviceRepository,
) : ViewModel() {

    val devicesUiState: StateFlow<DevicesScreenUiState> = devicesUiState(deviceRepository)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DevicesScreenUiState.Loading
        )

    private fun devicesUiState(deviceRepository: DeviceRepository) = deviceRepository.getAllDevices()
        .onStart { DevicesScreenUiState.Loading }
        .catch { DevicesScreenUiState.Error(it.getErrorMessageId()) }
        .map { DevicesScreenUiState.Success(devices = it) }
}

@Immutable
sealed interface DevicesScreenUiState {
    data class Success(val devices: List<Device>) : DevicesScreenUiState
    object Loading : DevicesScreenUiState
    data class Error(val messageId: Int) : DevicesScreenUiState
}
