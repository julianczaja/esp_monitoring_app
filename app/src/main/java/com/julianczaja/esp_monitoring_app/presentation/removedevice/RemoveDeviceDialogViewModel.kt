package com.julianczaja.esp_monitoring_app.presentation.removedevice

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoveDeviceDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceIdArgs: DeviceIdArgs = DeviceIdArgs(savedStateHandle)

    val eventFlow = MutableSharedFlow<Event>()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            val device = deviceRepository.getDeviceById(deviceIdArgs.deviceId).first()
            if (device != null) {
                _uiState.update { UiState.Success(device) }
            } else {
                _uiState.update { UiState.Error(R.string.internal_app_error_message) }
            }
        }
    }

    fun removeDevice(device: Device) = viewModelScope.launch {
        _uiState.update { UiState.Loading }
        deviceRepository.remove(device)
            .onFailure { e -> _uiState.update { UiState.Error(e.getErrorMessageId()) } }
            .onSuccess { eventFlow.emit(Event.DEVICE_REMOVED) }
    }

    fun convertExplanationStringToStyledText(explanationText: String, spanStyle: SpanStyle): AnnotatedString {
        return buildAnnotatedString {
            append(explanationText)
            val from = explanationText.indexOf("\"", startIndex = 0) + 1
            val to = explanationText.indexOf("\"", startIndex = from + 1)
            addStyle(spanStyle, start = from, end = to)
        }
    }

    enum class Event {
        DEVICE_REMOVED,
    }

    @Immutable
    sealed interface UiState {
        data class Success(val device: Device) : UiState
        data object Loading : UiState
        data class Error(@StringRes val messageId: Int) : UiState
    }
}
