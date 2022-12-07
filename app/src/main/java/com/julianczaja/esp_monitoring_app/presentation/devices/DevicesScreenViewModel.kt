package com.julianczaja.esp_monitoring_app.presentation.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.domain.model.Device
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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

    private fun devicesUiState(deviceRepository: DeviceRepository): Flow<DevicesScreenUiState> {
        return deviceRepository.getAllDevices()
            .onStart { DevicesScreenUiState.Loading }
            .catch { DevicesScreenUiState.Error(it.message) }
            .map { DevicesScreenUiState.Success(devices = it) }
    }
}

sealed interface DevicesScreenUiState {
    data class Success(val devices: List<Device>) : DevicesScreenUiState
    object Loading : DevicesScreenUiState
    data class Error(val message: String?) : DevicesScreenUiState
}
