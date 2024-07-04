package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.PhotosFilterMode
import com.julianczaja.esp_monitoring_app.domain.model.Selectable
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DevicePhotosScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    networkManager: NetworkManager,
    private val photoRepository: PhotoRepository,
    private val selectOrDeselectAllPhotosByDateUseCase: SelectOrDeselectAllPhotosByDateUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceId = savedStateHandle.toRoute<DeviceScreen>().deviceId

    private val _selectedPhotos = MutableStateFlow<List<Photo>>(emptyList())

    private val _selectedFilterDates = MutableStateFlow<List<Selectable<LocalDate>>>(emptyList())

    private val _filterMode = MutableStateFlow(PhotosFilterMode.ALL)

    private val _isRefreshing = MutableStateFlow(false)

    private val _isRefreshed = MutableStateFlow(false)

    private val _isOnline = networkManager.isOnline

    private val serverPhotosFlow = photoRepository.getAllPhotosLocal(deviceId).distinctUntilChanged()

    private val savedPhotosFlow = photoRepository.getAllSavedPhotosFromExternalStorageFlow(deviceId)
        .map {
            it.exceptionOrNull()?.let { e ->
                Timber.e("Error while reading saved photos: $e")
                eventFlow.emit(Event.ShowError(R.string.saved_photos_read_error_message))
            }
            return@map it.getOrNull() ?: emptyList()
        }
        .distinctUntilChanged()

    val eventFlow = MutableSharedFlow<Event>()

    val devicePhotosUiState: StateFlow<UiState> = combine(
        filteredGroupedSelectablePhotosFlow(),
        _isRefreshing,
        _isOnline,
        _isRefreshed
    ) { (selectableFilterDates, dateGroupedSelectablePhotos), isLoading, isOnline, isRefreshed ->
        UiState(
            dateGroupedSelectablePhotos = dateGroupedSelectablePhotos,
            selectableFilterDates = selectableFilterDates,
            filterMode = _filterMode.value,
            isLoading = isLoading,
            isOnline = isOnline,
            isSelectionMode = _selectedPhotos.value.isNotEmpty(),
            isRefreshed = isRefreshed,
            selectedCount = _selectedPhotos.value.size
        )
    }
        .flowOn(ioDispatcher)
        .catch {
            Timber.e(it)
            eventFlow.emit(Event.ShowError(it.getErrorMessageId()))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(
                dateGroupedSelectablePhotos = emptyMap(),
                selectableFilterDates = emptyList(),
                filterMode = PhotosFilterMode.ALL,
                isLoading = false,
                isOnline = true,
                isSelectionMode = false,
                isRefreshed = false,
                selectedCount = 0
            )
        )

    private fun filteredGroupedSelectablePhotosFlow(): Flow<Pair<List<Selectable<LocalDate>>, Map<LocalDate, List<Selectable<Photo>>>>> =
        combine(
            serverPhotosFlow,
            savedPhotosFlow,
            _selectedPhotos,
            _selectedFilterDates,
            _filterMode,
        ) { serverPhotos, savedPhotos, selectedPhotos, selectedFilterDates, filterMode ->

            val allPhotos = when (filterMode) {
                PhotosFilterMode.SAVED_ONLY -> savedPhotos
                PhotosFilterMode.SERVER_ONLY -> serverPhotos
                PhotosFilterMode.ALL -> savedPhotos + serverPhotos
            }

            // Map photos to SelectablePhoto
            val groupedSelectablePhotos = allPhotos
                .sortedByDescending { it.dateTime }
                .map { photo -> Selectable(item = photo, isSelected = selectedPhotos.contains(photo)) }
                .groupBy { it.item.dateTime.toLocalDate() }

            // Calculate new filter dates
            val oldDates = selectedFilterDates
            val newDates = groupedSelectablePhotos.keys
            val newSelectedFilterDates = newDates.map { newDate ->
                oldDates.find { it.item == newDate } ?: Selectable<LocalDate>(newDate, false)
            }
            _selectedFilterDates.update { newSelectedFilterDates }

            // Filter selectable photos
            val selectedDates = selectedFilterDates.filter { it.isSelected }
            val filteredPhotos = groupedSelectablePhotos.filter { photo ->
                when (selectedDates.isEmpty()) {
                    true -> true
                    false -> selectedDates.any { it.item == photo.key }
                }
            }
            return@combine (newSelectedFilterDates to filteredPhotos)
        }

    fun updatePhotos() = viewModelScope.launch(ioDispatcher) {
        _isRefreshing.update { true }
        photoRepository.updateAllPhotosRemote(deviceId)
            .onFailure {
                Timber.e(it)
                eventFlow.emit(Event.ShowError(it.getErrorMessageId()))
            }
        _isRefreshing.update { false }
        _isRefreshed.update { true }
    }

    fun updateSavedPhotos() {
        photoRepository.forceRefreshSavedPhotosContent()
    }

    fun onPhotoClick(selectablePhoto: Selectable<Photo>) {
        val isSelectionMode = _selectedPhotos.value.isNotEmpty()

        when (isSelectionMode) {
            true -> onPhotoLongClick(selectablePhoto)
            false -> openPhotoPreview(selectablePhoto.item)
        }
    }

    fun onPhotoLongClick(selectablePhoto: Selectable<Photo>) {
        _selectedPhotos.update { photos ->
            if (!photos.contains(selectablePhoto.item)) {
                photos + selectablePhoto.item
            } else {
                photos - selectablePhoto.item
            }
        }
    }

    fun onFilterDateClicked(selectableLocalDate: Selectable<LocalDate>) {
        _selectedFilterDates.update { dates ->
            dates.map {
                if (it.item == selectableLocalDate.item) it.copy(isSelected = !it.isSelected) else it
            }
        }
    }

    fun onFilterModeClicked() {
        _filterMode.update {
            val currentFilterIndex = it.ordinal
            val newFilterIndex = when {
                currentFilterIndex + 1 < PhotosFilterMode.entries.size -> currentFilterIndex + 1
                else -> 0
            }
            PhotosFilterMode.entries[newFilterIndex]
        }
    }

    fun onSelectDeselectAllClicked(localDate: LocalDate) {
        val updatedSelectedPhotos = selectOrDeselectAllPhotosByDateUseCase.invoke(
            allSelectablePhotosWithDate = devicePhotosUiState.value.dateGroupedSelectablePhotos[localDate].orEmpty(),
            allSelectedPhotos = _selectedPhotos.value,
            date = localDate
        )
        _selectedPhotos.update { updatedSelectedPhotos }
    }

    fun resetSelectedPhotos() {
        _selectedPhotos.update { emptyList() }
    }

    fun saveSelectedPhotos() = viewModelScope.launch {
        eventFlow.emit(Event.NavigateToSavePhotosDialog(_selectedPhotos.value))
        resetSelectedPhotos()
    }

    fun removeSelectedPhotos() = viewModelScope.launch {
        eventFlow.emit(Event.NavigateToRemovePhotosDialog(_selectedPhotos.value))
        resetSelectedPhotos()
    }

    fun createTimelapseFromSelectedPhotos() {
        viewModelScope.launch {
            if (_selectedPhotos.value.size >= 2) {
                eventFlow.emit(Event.NavigateToTimelapseCreatorScreen(_selectedPhotos.value))
                resetSelectedPhotos()
            } else {
                eventFlow.emit(Event.ShowNotEnoughSelectedInfo)
            }
        }
    }

    private fun openPhotoPreview(selectedPhoto: Photo) = viewModelScope.launch(ioDispatcher) {
        val photos = devicePhotosUiState.value.dateGroupedSelectablePhotos.values
            .flatten()
            .map { it.item }

        eventFlow.emit(Event.NavigateToPhotoPreview(photos, photos.indexOf(selectedPhoto)))
    }

    sealed class Event {
        data class NavigateToPhotoPreview(val photos: List<Photo>, val initialIndex: Int) : Event()
        data class NavigateToRemovePhotosDialog(val photos: List<Photo>) : Event()
        data class NavigateToSavePhotosDialog(val photos: List<Photo>) : Event()
        data class NavigateToTimelapseCreatorScreen(val photos: List<Photo>) : Event()
        data class ShowSavedInfo(val totalCount: Int, val savedCount: Int) : Event()
        data class ShowError(val messageId: Int) : Event()
        data object ShowNotEnoughSelectedInfo : Event()
    }

    @Immutable
    data class UiState(
        val dateGroupedSelectablePhotos: Map<LocalDate, List<Selectable<Photo>>>,
        val selectableFilterDates: List<Selectable<LocalDate>>,
        val filterMode: PhotosFilterMode,
        val isLoading: Boolean,
        val isOnline: Boolean,
        val isSelectionMode: Boolean,
        val isRefreshed: Boolean,
        val selectedCount: Int
    )
}
