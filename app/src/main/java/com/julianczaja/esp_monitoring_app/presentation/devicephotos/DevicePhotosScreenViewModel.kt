package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.SelectablePhoto
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
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

    private val _selectedPhotos = MutableStateFlow<List<Photo>>(emptyList())

    private var _isSelectionMode = false

    private val _isRefreshing = MutableStateFlow(false)

    private val _isOnline = networkManager.isOnline

    val eventFlow = MutableSharedFlow<Event>()

    val devicePhotosUiState: StateFlow<UiState> = devicePhotosUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(
                dateGroupedSelectablePhotos = emptyMap(),
                isRefreshing = false,
                isOnline = true,
                isSelectionMode = false
            )
        )

    private fun devicePhotosUiState(): Flow<UiState> =
        combine(
            photoRepository.getAllPhotosLocal(deviceIdArgs.deviceId),
            _isRefreshing,
            _isOnline,
            _selectedPhotos
        ) { photos, isRefreshing, isOnline, selectedPhotos ->
            _isSelectionMode = selectedPhotos.isNotEmpty()
            UiState(
                dateGroupedSelectablePhotos = groupPhotosByDayDesc(photos),
                isRefreshing = isRefreshing,
                isOnline = isOnline,
                isSelectionMode = _isSelectionMode,
            )
        }
            .flowOn(ioDispatcher)
            .catch { eventFlow.emit(Event.ShowError(it.getErrorMessageId())) }

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

    fun onPhotoClick(selectablePhoto: SelectablePhoto) {
        when (_isSelectionMode) {
            true -> onPhotoLongClick(selectablePhoto)
            false -> viewModelScope.launch { eventFlow.emit(Event.NavigateToPhotoPreview(selectablePhoto.photo)) }
        }
    }

    fun onPhotoLongClick(selectablePhoto: SelectablePhoto) {
        _selectedPhotos.update { photos ->
            if (!photos.contains(selectablePhoto.photo)) {
                photos + selectablePhoto.photo
            } else {
                photos - selectablePhoto.photo
            }
        }
    }

    fun resetSelectedPhotos() {
        _selectedPhotos.update { emptyList() }
    }

    fun saveSelectedPhotos() {
        TODO("Not yet implemented")
    }

    fun removeSelectedPhotos() {
        TODO("Not yet implemented")
    }

    private fun groupPhotosByDayDesc(photos: List<Photo>) = photos
        .map { SelectablePhoto(photo = it, isSelected = _selectedPhotos.value.contains(it)) }
        .groupBy { it.photo.dateTime.toLocalDate() }

    sealed class Event {
        data class NavigateToPhotoPreview(val photo: Photo) : Event()
        data class ShowError(val messageId: Int) : Event()
    }

    @Immutable
    data class UiState(
        val dateGroupedSelectablePhotos: Map<LocalDate, List<SelectablePhoto>>,
        val isRefreshing: Boolean,
        val isOnline: Boolean,
        val isSelectionMode: Boolean
    )
}
