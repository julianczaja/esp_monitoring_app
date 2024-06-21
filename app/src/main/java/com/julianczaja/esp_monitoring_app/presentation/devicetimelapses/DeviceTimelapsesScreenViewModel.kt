package com.julianczaja.esp_monitoring_app.presentation.devicetimelapses

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Timelapse
import com.julianczaja.esp_monitoring_app.domain.repository.TimelapseRepository
import com.julianczaja.esp_monitoring_app.navigation.DeviceScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeviceTimelapsesScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val timelapseRepository: TimelapseRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceId = savedStateHandle.toRoute<DeviceScreen>().deviceId

    val eventFlow = MutableSharedFlow<Event>()

    private val _savedTimelapses = MutableStateFlow<List<Timelapse>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<UiState> = combine(_savedTimelapses, _isLoading) { savedTimelapses, isLoading ->
        UiState(savedTimelapses, isLoading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState(emptyList(), false)
    )

    fun updateTimelapses() {
        readSavedTimelapses()
    }

    private fun readSavedTimelapses() = viewModelScope.launch(ioDispatcher) {
        _isLoading.emit(true)
        timelapseRepository.readAllTimelapsesFromExternalStorage(deviceId)
            .onSuccess { _savedTimelapses.emit(it) }
            .onFailure {
                Timber.e(it)
                eventFlow.emit(Event.ShowError(R.string.internal_app_error_message))
            }
        _isLoading.emit(false)
    }

    @Immutable
    data class UiState(
        val timelapses: List<Timelapse>,
        val isLoading: Boolean
    )

    sealed class Event {
        data class ShowError(val messageId: Int) : Event()
    }
}
