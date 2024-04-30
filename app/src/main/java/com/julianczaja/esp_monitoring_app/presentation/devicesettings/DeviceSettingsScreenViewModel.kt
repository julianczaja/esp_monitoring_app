package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceSettingsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceSettingsRepository: DeviceSettingsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
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

    fun updateDeviceSettings() {
        isRefreshing.update { true }

        viewModelScope.launch(ioDispatcher) {
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
    }

    private fun deviceSettingsStream() = deviceSettingsRepository.getDeviceSettingsLocal(deviceIdArgs.deviceId)
        .onStart { DeviceSettingsState.Loading }
        .catch { DeviceSettingsState.Error(it.getErrorMessageId()) }
        .map { settings -> DeviceSettingsState.Success(deviceSettings = settings) }

    @Immutable
    data class DeviceSettingsScreenUiState(
        val deviceSettingsState: DeviceSettingsState,
        val isRefreshing: Boolean,
    )

    @Immutable
    sealed interface DeviceSettingsState {
        data class Success(val deviceSettings: DeviceSettings) : DeviceSettingsState
        data object Loading : DeviceSettingsState
        data class Error(@StringRes val messageId: Int) : DeviceSettingsState
    }
}
