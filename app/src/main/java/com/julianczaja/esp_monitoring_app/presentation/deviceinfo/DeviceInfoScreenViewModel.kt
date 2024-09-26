package com.julianczaja.esp_monitoring_app.presentation.deviceinfo

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.DeviceInfo
import com.julianczaja.esp_monitoring_app.domain.model.DeviceServerSettings
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceInfoRepository
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceServerSettingsRepository
import com.julianczaja.esp_monitoring_app.navigation.DeviceScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeviceInfoScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    networkManager: NetworkManager,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val deviceServerSettingsRepository: DeviceServerSettingsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _deviceId = savedStateHandle.toRoute<DeviceScreen>().deviceId
    private val _isRefreshing = MutableStateFlow(false)
    private val _isOnline = networkManager.isOnline

    val eventFlow = MutableSharedFlow<Event>()

    val uiState: StateFlow<UiState> = deviceDataStream()
        .flowOn(ioDispatcher)
        .catch { onError(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(
                deviceInfo = null,
                deviceServerSettings = null,
                isLoading = false,
                isOnline = true
            )
        )

    fun refreshData() {
        _isRefreshing.update { true }

        viewModelScope.launch(ioDispatcher) {
            val infoDef = async { deviceInfoRepository.refreshDeviceInfoRemote(_deviceId) }
            val settingsDef = async { deviceServerSettingsRepository.refreshDeviceServerSettingsRemote(_deviceId) }

            infoDef.await().onFailure { onError(it) }
            settingsDef.await().onFailure { onError(it) }

            _isRefreshing.update { false }
        }
    }

    fun onDetectMostlyBlackPhotosChange(value: Boolean) = viewModelScope.launch(ioDispatcher) {
        _isRefreshing.update { true }

        uiState.value.deviceServerSettings?.let { settings ->
            deviceServerSettingsRepository.updateDeviceServerSettingsRemote(
                deviceId = _deviceId,
                deviceServerSettings = settings.copy(detectMostlyBlackPhotos = value)
            ).onFailure { onError(it) }
        }
        _isRefreshing.update { false }
    }

    private fun deviceDataStream() = combine(
        deviceInfoRepository.getDeviceInfo(_deviceId),
        deviceServerSettingsRepository.getDeviceServerSettings(_deviceId),
        _isRefreshing,
        _isOnline
    ) { deviceInfo, deviceServerSettings, isRefreshing, isOnline ->
        UiState(
            deviceInfo = deviceInfo,
            deviceServerSettings = deviceServerSettings,
            isLoading = isRefreshing,
            isOnline = isOnline
        )
    }

    private suspend fun onError(e: Throwable) {
        Timber.e(e)
        eventFlow.emit(Event.ShowError(e.getErrorMessageId()))
    }

    sealed class Event {
        data class ShowError(@StringRes val messageId: Int) : Event()
    }

    data class UiState(
        val deviceInfo: DeviceInfo?,
        val deviceServerSettings: DeviceServerSettings?,
        val isLoading: Boolean,
        val isOnline: Boolean,
    )
}
