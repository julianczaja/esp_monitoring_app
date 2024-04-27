package com.julianczaja.esp_monitoring_app.presentation.devices

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject


@HiltViewModel
class DevicesScreenViewModel @Inject constructor(
    deviceRepository: DeviceRepository,
) : ViewModel() {

    val devicesUiState: StateFlow<DevicesScreenUiState> = deviceRepository.getAllDevices()
        .map<List<Device>, DevicesScreenUiState>(DevicesScreenUiState::Success)
        .catch { emit(DevicesScreenUiState.Error(it.getErrorMessageId())) }
        .onStart { emit(DevicesScreenUiState.Loading) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DevicesScreenUiState.Loading
        )
}

@Immutable
sealed interface DevicesScreenUiState {
    data class Success(val devices: List<Device>) : DevicesScreenUiState
    data object Loading : DevicesScreenUiState
    data class Error(val messageId: Int) : DevicesScreenUiState
}
