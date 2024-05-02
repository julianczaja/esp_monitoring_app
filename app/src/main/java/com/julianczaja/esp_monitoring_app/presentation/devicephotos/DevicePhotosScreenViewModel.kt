package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DevicePhotosScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    networkManager: NetworkManager,
    private val photoRepository: PhotoRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceIdArgs: DeviceIdArgs = DeviceIdArgs(savedStateHandle)

    private val _isRefreshing = MutableStateFlow(false)

    private val _isOnline = networkManager.isOnline

    val eventFlow = MutableSharedFlow<Event>()

    val devicePhotosUiState: StateFlow<UiState> = devicePhotosUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(
                dateGroupedPhotos = emptyMap(),
                isRefreshing = false,
                isOnline = true
            )
        )

    private fun devicePhotosUiState(): Flow<UiState> =
        combine(photosStream(), _isRefreshing, _isOnline) { photos, isRefreshing, isOnline ->
            UiState(photos, isRefreshing, isOnline)
        }

    private fun photosStream() = photoRepository.getAllPhotosLocal(deviceIdArgs.deviceId)
        .map { groupPhotosByDayDesc(it) }
        .catch { eventFlow.emit(Event.ShowError(it.getErrorMessageId())) }
        .flowOn(ioDispatcher)

    fun updatePhotos() {
        _isRefreshing.update { true }

        viewModelScope.launch(ioDispatcher) {
            photoRepository.updateAllPhotosRemote(deviceIdArgs.deviceId)
                .onFailure {
                    eventFlow.emit(Event.ShowError(it.getErrorMessageId()))
                    _isRefreshing.update { false }
                }
                .onSuccess {
                    _isRefreshing.update { false }
                }
        }
    }

    private fun groupPhotosByDayDesc(photos: List<Photo>) = photos.groupBy { it.dateTime.toLocalDate() }

    sealed class Event {
        data class ShowError(val messageId: Int) : Event()
    }

    @Immutable
    data class UiState(
        val dateGroupedPhotos: Map<LocalDate, List<Photo>>,
        val isRefreshing: Boolean,
        val isOnline: Boolean
    )
}
