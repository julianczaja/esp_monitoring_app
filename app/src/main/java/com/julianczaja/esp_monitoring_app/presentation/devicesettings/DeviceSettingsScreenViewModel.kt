package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DeviceSettingsScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val deviceIdArgs: DeviceIdArgs = DeviceIdArgs(savedStateHandle)

    private val apiError = MutableStateFlow<Int?>(null)

    private val isRefreshing = MutableStateFlow(false)

    val deviceSettingsUiState: StateFlow<DeviceSettingsScreenUiState> = deviceSettingsUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeviceSettingsScreenUiState(DeviceSettingsState.Loading, true)
        )

    private fun deviceSettingsUiState(): Flow<DeviceSettingsScreenUiState> =
//        combine(deviceSettingsStream(), apiError, isRefreshing) { settings, apiErr, refreshing ->
        combine(apiError, isRefreshing) { apiErr, refreshing ->
            return@combine if (apiErr != null) {
                DeviceSettingsScreenUiState(DeviceSettingsState.Error(apiErr), refreshing)
            } else {
                val tempState = DeviceSettingsState.Success(DeviceSettings(deviceIdArgs.deviceId))
                DeviceSettingsScreenUiState(tempState, refreshing)
            }
        }

    fun updateDeviceSettings() {

    }
}

data class DeviceSettingsScreenUiState(
    val deviceSettingsState: DeviceSettingsState,
    val isRefreshing: Boolean,
)

sealed interface DeviceSettingsState {
    data class Success(val deviceSettings: DeviceSettings) : DeviceSettingsState
    object Loading : DeviceSettingsState
    data class Error(@StringRes val messageId: Int) : DeviceSettingsState
}
