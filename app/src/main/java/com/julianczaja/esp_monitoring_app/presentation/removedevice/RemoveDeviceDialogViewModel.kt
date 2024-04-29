package com.julianczaja.esp_monitoring_app.presentation.removedevice

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoveDeviceDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    enum class Event {
        DEVICE_REMOVED,
    }

    private val deviceIdArgs: DeviceIdArgs = DeviceIdArgs(savedStateHandle)

    val eventFlow = MutableSharedFlow<Event>()

    private val _uiState = MutableStateFlow<RemoveDeviceScreenUiState>(RemoveDeviceScreenUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            val device = deviceRepository.getDeviceById(deviceIdArgs.deviceId)
            if (device != null) {
                _uiState.update { RemoveDeviceScreenUiState.Success(device) }
            } else {
                _uiState.update { RemoveDeviceScreenUiState.Error(R.string.internal_app_error_message) }
            }
        }
    }

    fun removeDevice(device: Device) = viewModelScope.launch {
        _uiState.update { RemoveDeviceScreenUiState.Loading }
        try {
            deviceRepository.remove(device)
            eventFlow.emit(Event.DEVICE_REMOVED)
        } catch (e: Exception) {
            _uiState.update { RemoveDeviceScreenUiState.Error(e.getErrorMessageId()) }
        }
    }

    fun convertExplanationStringToStyledText(explanationText: String, spanStyle: SpanStyle): AnnotatedString {
        return buildAnnotatedString {
            append(explanationText)
            val from = explanationText.indexOf("\"", startIndex = 0) + 1
            val to = explanationText.indexOf("\"", startIndex = from + 1)
            addStyle(spanStyle, start = from, end = to)
        }
    }
}

@Immutable
sealed interface RemoveDeviceScreenUiState {
    data class Success(val device: Device) : RemoveDeviceScreenUiState
    data object Loading : RemoveDeviceScreenUiState
    data class Error(val messageId: Int) : RemoveDeviceScreenUiState
}
