package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
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

    private val _savedPhotos = MutableStateFlow<List<Photo>>(emptyList())

    private val _selectedFilterDates = MutableStateFlow<List<Selectable<LocalDate>>>(emptyList())

    private val _filterSavedOnly = MutableStateFlow(false)

    private val _isSaving = MutableStateFlow(false)

    private val _isRefreshing = MutableStateFlow(false)

    private val _isRefreshed = MutableStateFlow(false)

    private val _isOnline = networkManager.isOnline

    val eventFlow = MutableSharedFlow<Event>()

    val devicePhotosUiState: StateFlow<UiState> = combine(
        filteredGroupedSelectablePhotosFlow(),
        isLoadingFlow(),
        _isOnline,
        _isRefreshed
    ) { (selectableFilterDates, dateGroupedSelectablePhotos), isLoading, isOnline, isRefreshed ->
        UiState(
            dateGroupedSelectablePhotos = dateGroupedSelectablePhotos,
            selectableFilterDates = selectableFilterDates,
            filterSavedOnly = _filterSavedOnly.value,
            isSavedPhotosListEmpty = _savedPhotos.value.isEmpty(),
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
                filterSavedOnly = false,
                isSavedPhotosListEmpty = true,
                isLoading = false,
                isOnline = true,
                isSelectionMode = false,
                isRefreshed = false,
                selectedCount = 0
            )
        )

    private fun filteredGroupedSelectablePhotosFlow(): Flow<Pair<List<Selectable<LocalDate>>, Map<LocalDate, List<Selectable<Photo>>>>> =
        combine(
            photoRepository.getAllPhotosLocal(deviceId).distinctUntilChanged(),
            _savedPhotos,
            _selectedPhotos,
            _selectedFilterDates,
            _filterSavedOnly,
        ) { localPhotos, savedPhotos, selectedPhotos, selectedFilterDates, filterSavedOnly ->

            val allPhotos = when (filterSavedOnly) {
                true -> savedPhotos
                false -> savedPhotos + localPhotos.filterNot { localPhoto ->
                    savedPhotos.fastAny { it.fileName == localPhoto.fileName }
                }
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

    private fun isLoadingFlow() = combine(_isSaving, _isRefreshing) { isSaving, isRefreshing ->
        return@combine isRefreshing || isSaving
    }

    fun updatePhotos() = viewModelScope.launch(ioDispatcher) {
        _isRefreshing.update { true }
        photoRepository.updateAllPhotosRemote(deviceId)
            .onFailure {
                Timber.e(it)
                eventFlow.emit(Event.ShowError(it.getErrorMessageId()))
            }
        updateSavedPhotos()
        _isRefreshing.update { false }
        _isRefreshed.update { true }
    }

    fun onPhotoClick(selectablePhoto: Selectable<Photo>) {
        val isSelectionMode = _selectedPhotos.value.isNotEmpty()

        when (isSelectionMode) {
            true -> onPhotoLongClick(selectablePhoto)
            false -> viewModelScope.launch { eventFlow.emit(Event.NavigateToPhotoPreview(selectablePhoto.item)) }
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

    fun onFilterSavedOnlyClicked(filterSavedOnly: Boolean) {
        _filterSavedOnly.update { filterSavedOnly }
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

    fun saveSelectedPhotos() {
        _isSaving.update { true }

        val selectedPhotos = _selectedPhotos.value
        val totalCount = selectedPhotos.size
        var savedCount = 0

        viewModelScope.launch(ioDispatcher) {
            selectedPhotos.forEach { photo ->
                photoRepository.downloadPhotoAndSaveToExternalStorage(photo)
                    .onSuccess {
                        Timber.i("Saved ${photo.fileName}")
                        savedCount += 1
                    }
                    .onFailure {
                        Timber.e("Error while saving ${photo.fileName}: $it")
                    }
            }
            resetSelectedPhotos()
            updateSavedPhotos()
            eventFlow.emit(Event.ShowSavedInfo(totalCount, savedCount))
            _isSaving.update { false }
        }
    }

    fun removeSelectedPhotos() = viewModelScope.launch(ioDispatcher) {
        val selectedPhotosFileNames = _selectedPhotos.value.map { it.fileName }
        val photosToRemove = (photoRepository.getAllPhotosLocal(deviceId).first() + _savedPhotos.value)
            .filter { selectedPhotosFileNames.contains(it.fileName) }

        eventFlow.emit(Event.NavigateToRemovePhotosDialog(photosToRemove))
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

    private suspend fun updateSavedPhotos() {
        photoRepository.readAllSavedPhotosFromExternalStorage(deviceId)
            .onSuccess { _savedPhotos.emit(it) }
            .onFailure {
                Timber.e(it)
                eventFlow.emit(Event.ShowError(it.getErrorMessageId()))
            }
    }

    sealed class Event {
        data class NavigateToPhotoPreview(val photo: Photo) : Event()
        data class NavigateToRemovePhotosDialog(val photos: List<Photo>) : Event()
        data class NavigateToTimelapseCreatorScreen(val photos: List<Photo>) : Event()
        data class ShowSavedInfo(val totalCount: Int, val savedCount: Int) : Event()
        data class ShowError(val messageId: Int) : Event()
        data object ShowNotEnoughSelectedInfo : Event()
    }

    @Immutable
    data class UiState(
        val dateGroupedSelectablePhotos: Map<LocalDate, List<Selectable<Photo>>>,
        val selectableFilterDates: List<Selectable<LocalDate>>,
        val filterSavedOnly: Boolean,
        val isSavedPhotosListEmpty: Boolean,
        val isLoading: Boolean,
        val isOnline: Boolean,
        val isSelectionMode: Boolean,
        val isRefreshed: Boolean,
        val selectedCount: Int
    )
}
