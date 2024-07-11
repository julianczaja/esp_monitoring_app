package com.julianczaja.esp_monitoring_app.presentation.devicetimelapses

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Timelapse
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.TimelapseRepository
import com.julianczaja.esp_monitoring_app.navigation.DeviceScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeviceTimelapsesScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val timelapseRepository: TimelapseRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val deviceId = savedStateHandle.toRoute<DeviceScreen>().deviceId

    private val savedTimelapsesFlow = timelapseRepository.getAllTimelapsesFromExternalStorageFlow(deviceId)
        .map {
            it.exceptionOrNull()?.let { e ->
                Timber.e("Error while reading timelapses: $e")
                eventFlow.emit(Event.ShowError(R.string.timelapses_read_error_message))
            }
            return@map it.getOrNull() ?: emptyList()
        }
        .distinctUntilChanged()

    val eventFlow = MutableSharedFlow<Event>()

    val uiState: StateFlow<UiState> = savedTimelapsesFlow
        .map { UiState(it.toImmutableList()) }
        .flowOn(ioDispatcher)
        .catch {
            Timber.e(it)
            eventFlow.emit(Event.ShowError(it.getErrorMessageId()))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(persistentListOf())
        )

    fun updateTimelapses() {
        timelapseRepository.forceRefreshContent()
    }

    @Immutable
    data class UiState(
        val timelapses: ImmutableList<Timelapse>
    )

    sealed class Event {
        data class ShowError(val messageId: Int) : Event()
    }
}
