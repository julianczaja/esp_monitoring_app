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
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class DevicesScreenViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    val uiState: StateFlow<UiState> = deviceRepository.getAllDevices()
        .map { devices -> devices.sortedBy { it.order } }
        .map<List<Device>, UiState>(UiState::Success)
        .flowOn(ioDispatcher)
        .catch { emit(UiState.Error(it.getErrorMessageId())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    fun onDevicesReordered(device1Id: Long, device2Id: Long) = viewModelScope.launch(ioDispatcher) {
        deviceRepository.reorderDevices(device1Id, device2Id)
    }

    @Immutable
    sealed interface UiState {
        data class Success(val devices: List<Device>) : UiState
        data object Loading : UiState
        data class Error(@StringRes val messageId: Int) : UiState
    }
}
