package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DeviceSettingsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceIdArgs: DeviceIdArgs = DeviceIdArgs(savedStateHandle)

    private val apiError = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<UiState> = deviceSettingsUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading
    )

    init {
        updateDeviceSettings()
    }

    private fun deviceSettingsUiState(): Flow<UiState> = combine(deviceSettingsStream(), apiError) { settings, apiErr ->
        return@combine if (apiErr != null) {
            UiState.Error(apiErr)
        } else {
            UiState.Success(settings)
        }
    }

    fun updateDeviceSettings() {}

    private fun deviceSettingsStream() = flowOf(DeviceSettings(deviceIdArgs.deviceId)) // it will be changed in future

    @Immutable
    sealed interface UiState {
        data class Success(val deviceSettings: DeviceSettings) : UiState
        data object Loading : UiState
        data class Error(@StringRes val messageId: Int) : UiState
    }
}
