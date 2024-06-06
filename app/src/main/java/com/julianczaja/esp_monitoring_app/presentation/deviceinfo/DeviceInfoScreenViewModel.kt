package com.julianczaja.esp_monitoring_app.presentation.deviceinfo

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.DeviceInfo
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceInfoRepository
import com.julianczaja.esp_monitoring_app.navigation.DeviceScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _deviceId = savedStateHandle.toRoute<DeviceScreen>().deviceId

    private val _isRefreshing = MutableStateFlow(false)

    private val _isOnline = networkManager.isOnline

    val eventFlow = MutableSharedFlow<Event>()

    val uiState: StateFlow<UiState> = deviceInfoStream()
        .flowOn(ioDispatcher)
        .catch {
            Timber.e(it)
            eventFlow.emit(Event.ShowError(it.getErrorMessageId()))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(
                deviceInfo = null,
                isLoading = false,
                isOnline = true
            )
        )

    fun updateDeviceInfo() {
        _isRefreshing.update { true }

        viewModelScope.launch(ioDispatcher) {
            deviceInfoRepository.updateDeviceInfoRemote(_deviceId)
                .onFailure {
                    Timber.e(it)
                    eventFlow.emit(Event.ShowError(it.getErrorMessageId()))
                    _isRefreshing.update { false }
                }
                .onSuccess {
                    _isRefreshing.update { false }
                }
        }
    }

    private fun deviceInfoStream() = combine(
        deviceInfoRepository.getDeviceInfo(_deviceId),
        _isRefreshing,
        _isOnline
    ) { deviceInfo, isRefreshing, isOnline ->
        UiState(
            deviceInfo = deviceInfo,
            isLoading = isRefreshing,
            isOnline = isOnline
        )
    }

    sealed class Event {
        data class ShowError(@StringRes val messageId: Int) : Event()
    }

    data class UiState(
        val deviceInfo: DeviceInfo?,
        val isLoading: Boolean,
        val isOnline: Boolean,
    )
}
