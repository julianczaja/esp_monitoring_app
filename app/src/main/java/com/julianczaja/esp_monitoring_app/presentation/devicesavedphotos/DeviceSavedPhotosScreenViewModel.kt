package com.julianczaja.esp_monitoring_app.presentation.devicesavedphotos

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.SelectablePhoto
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.domain.usecase.SelectOrDeselectAllPhotosByDateUseCase
import com.julianczaja.esp_monitoring_app.navigation.DeviceScreen
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
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DeviceSavedPhotosScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    private val selectOrDeselectAllPhotosByDateUseCase: SelectOrDeselectAllPhotosByDateUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceId = savedStateHandle.toRoute<DeviceScreen>().deviceId

    private var _isSelectionMode = false

    private val _selectedPhotos = MutableStateFlow<List<Photo>>(emptyList())

    private val _savedPhotos = MutableStateFlow<List<Photo>>(emptyList())

    private val _isLoading = MutableStateFlow(false)

    val eventFlow = MutableSharedFlow<Event>()

    val uiState: StateFlow<UiState> = devicePhotosUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(
                dateGroupedSelectablePhotos = emptyMap(),
                isLoading = false,
                isSelectionMode = false
            )
        )

    private fun devicePhotosUiState(): Flow<UiState> =
        combine(
            _savedPhotos,
            _selectedPhotos,
            _isLoading,
        ) { savedPhotos, selectedPhotos, isLoading ->
            _isSelectionMode = selectedPhotos.isNotEmpty()
            UiState(
                dateGroupedSelectablePhotos = groupPhotosByDayDesc(savedPhotos),
                isLoading = isLoading,
                isSelectionMode = _isSelectionMode,
            )
        }
            .flowOn(ioDispatcher)
            .catch {
                Timber.e(it)
                eventFlow.emit(Event.ShowError(it.getErrorMessageId()))
            }

    fun updateSavedPhotos() {
        readSavedPhotos()
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

    fun onSelectDeselectAllClicked(localDate: LocalDate) {
        val updatedSelectedPhotos = selectOrDeselectAllPhotosByDateUseCase.invoke(
            allSelectablePhotosWithDate = uiState.value.dateGroupedSelectablePhotos[localDate].orEmpty(),
            allSelectedPhotos = _selectedPhotos.value,
            date = localDate
        )
        _selectedPhotos.update { updatedSelectedPhotos }
    }

    fun resetSelectedPhotos() {
        _selectedPhotos.update { emptyList() }
    }

    fun removeSelectedPhotos() = viewModelScope.launch(ioDispatcher) {
        _isLoading.emit(true)

        val selectedPhotos = _selectedPhotos.value
        val removedPhotos = mutableSetOf<Photo>()
        val totalCount = selectedPhotos.size

        selectedPhotos.forEach { photo ->
            photoRepository.removeSavedPhotoFromExternalStorage(photo)
                .onSuccess {
                    Timber.i("Removed ${photo.fileName}")
                    removedPhotos.add(photo)
                }
                .onFailure {
                    Timber.e("Error while removing ${photo.fileName}: $it")
                }
        }
        _selectedPhotos.update { it - removedPhotos }
        updateSavedPhotos()
        eventFlow.emit(Event.ShowRemovedInfo(totalCount, removedPhotos.size))

        _isLoading.emit(false)
    }

    private fun readSavedPhotos() = viewModelScope.launch(ioDispatcher) {
        _isLoading.emit(true)
        photoRepository.readAllSavedPhotosFromExternalStorage(deviceId)
            .onSuccess { _savedPhotos.emit(it) }
            .onFailure {
                Timber.e(it)
                eventFlow.emit(Event.ShowError(R.string.internal_app_error_message))
            }
        _isLoading.emit(false)
    }

    private fun groupPhotosByDayDesc(photos: List<Photo>) = photos
        .map { SelectablePhoto(photo = it, isSelected = _selectedPhotos.value.contains(it)) }
        .groupBy { it.photo.dateTime.toLocalDate() }

    sealed class Event {
        data class NavigateToPhotoPreview(val photo: Photo) : Event()
        data class ShowError(val messageId: Int) : Event()
        data class ShowRemovedInfo(val totalCount: Int, val removedCount: Int) : Event()
    }

    @Immutable
    data class UiState(
        val dateGroupedSelectablePhotos: Map<LocalDate, List<SelectablePhoto>>,
        val isLoading: Boolean,
        val isSelectionMode: Boolean
    )
}
