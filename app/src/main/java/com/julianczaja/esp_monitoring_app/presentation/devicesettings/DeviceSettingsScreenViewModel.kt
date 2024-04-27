package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceSettingsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceSettingsRepository: DeviceSettingsRepository,
) : ViewModel() {

    private val deviceIdArgs: DeviceIdArgs = DeviceIdArgs(savedStateHandle)

    private val apiError = MutableStateFlow<Int?>(null)

    private val isRefreshing = MutableStateFlow(false)

    val deviceSettingsUiState: StateFlow<DeviceSettingsScreenUiState> = deviceSettingsUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeviceSettingsScreenUiState(DeviceSettingsState.Loading, true)
        )

    init {
        updateDeviceSettings()
    }

    private fun deviceSettingsUiState(): Flow<DeviceSettingsScreenUiState> =
        combine(deviceSettingsStream(), apiError, isRefreshing) { settings, apiErr, refreshing ->
            return@combine if (apiErr != null) {
                DeviceSettingsScreenUiState(DeviceSettingsState.Error(apiErr), refreshing)
            } else {
                DeviceSettingsScreenUiState(settings, refreshing)
            }
        }

    fun updateDeviceSettings() = viewModelScope.launch(Dispatchers.IO) {
        isRefreshing.update { true }
        deviceSettingsRepository.getCurrentDeviceSettingsRemote(deviceIdArgs.deviceId) // TODO: Check if it's not in progress already
            .onFailure {
                apiError.emit(it.getErrorMessageId())
                isRefreshing.update { false }
            }
            .onSuccess {
                apiError.emit(null)
                isRefreshing.update { false }
            }
    }

    private fun deviceSettingsStream() = deviceSettingsRepository.getDeviceSettingsLocal(deviceIdArgs.deviceId)
        .onStart { DeviceSettingsState.Loading }
        .catch { DeviceSettingsState.Error(it.getErrorMessageId()) }
        .map { settings -> DeviceSettingsState.Success(deviceSettings = settings) }
}

data class DeviceSettingsScreenUiState(
    val deviceSettingsState: DeviceSettingsState,
    val isRefreshing: Boolean,
)

sealed interface DeviceSettingsState {
    data class Success(val deviceSettings: DeviceSettings) : DeviceSettingsState
    data object Loading : DeviceSettingsState
    data class Error(@StringRes val messageId: Int) : DeviceSettingsState
}
