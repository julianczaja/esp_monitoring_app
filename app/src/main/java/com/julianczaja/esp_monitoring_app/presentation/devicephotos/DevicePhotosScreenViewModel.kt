package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.TimelapseCreator
import com.julianczaja.esp_monitoring_app.domain.model.Day
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.PhotosFilterMode
import com.julianczaja.esp_monitoring_app.domain.model.Selectable
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.DayRepository
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.domain.usecase.SelectOrDeselectAllPhotosByDateUseCase
import com.julianczaja.esp_monitoring_app.navigation.DeviceScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DevicePhotosScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    networkManager: NetworkManager,
    private val photoRepository: PhotoRepository,
    private val dayRepository: DayRepository,
    private val timelapseCreator: TimelapseCreator,
    private val selectOrDeselectAllPhotosByDateUseCase: SelectOrDeselectAllPhotosByDateUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceId = savedStateHandle.toRoute<DeviceScreen>().deviceId

    private val _selectedPhotos = MutableStateFlow<List<Photo>>(emptyList())

    private val _selectedDay = MutableStateFlow<Day?>(null)

    private val _filterMode = MutableStateFlow(PhotosFilterMode.ALL)

    private val _isRefreshing = MutableStateFlow(false)

    private val _isInitiated = MutableStateFlow(false)

    private val _isOnline = networkManager.isOnline

    private val _daysFetched = mutableSetOf<Day>()

    private val _daysFetchedMutex = Mutex()

    private val _daysFlow = dayRepository.getDeviceDaysLocal(deviceId).distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _serverPhotosFlow = _daysFlow.flatMapLatest { days ->
        combine(
            days.map { day ->
                photoRepository.getAllPhotosByDayLocal(day).map { photos -> day to photos }
            }
        ) { dayToPhotos ->
            dayToPhotos.toMap()
        }
    }.distinctUntilChanged()

    private val _savedPhotosFlow = photoRepository.getAllSavedPhotosFromExternalStorageFlow(deviceId)
        .map { result ->
            result
                .onFailure { e ->
                    Timber.e("Error while reading saved photos: $e")
                    eventFlow.emit(Event.ShowError(R.string.saved_photos_read_error_message))
                }
                .onSuccess { photos ->
                    return@map photos
                        .sortedByDescending { it.dateTime }
                        .groupBy { Day(deviceId, it.dateTime.toLocalDate()) }
                }
            return@map emptyMap<Day, List<Photo>>()
        }.distinctUntilChanged()

    val eventFlow = MutableSharedFlow<Event>()

    val devicePhotosUiState: StateFlow<UiState> = combine(
        dayGroupedSelectablePhotosFlow(),
        _isRefreshing,
        _isOnline,
        _isInitiated
    ) { dayGroupedSelectablePhotos, isLoading, isOnline, isRefreshed ->
        UiState(
            dayGroupedSelectablePhotos = dayGroupedSelectablePhotos.toImmutableMap(),
            filterMode = _filterMode.value,
            isLoading = isLoading,
            isOnline = isOnline,
            isSelectionMode = _selectedPhotos.value.isNotEmpty(),
            isInitiated = isRefreshed,
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
                dayGroupedSelectablePhotos = persistentMapOf(),
                filterMode = PhotosFilterMode.ALL,
                isLoading = false,
                isOnline = true,
                isSelectionMode = false,
                isInitiated = false,
                selectedCount = 0
            )
        )

    private fun dayGroupedSelectablePhotosFlow(): Flow<Map<Day, List<Selectable<Photo>>>> =
        combine(
            _serverPhotosFlow,
            _savedPhotosFlow,
            _selectedPhotos,
            _filterMode,
        ) { serverPhotos, savedPhotos, selectedPhotos, filterMode ->
            when (filterMode) {
                PhotosFilterMode.SAVED_ONLY -> savedPhotos
                PhotosFilterMode.SERVER_ONLY -> serverPhotos
                PhotosFilterMode.ALL -> combineDaysToPhotosMaps(savedPhotos, serverPhotos)
            }.mapValues { entry ->
                entry.value.map { photo ->
                    Selectable(item = photo, isSelected = selectedPhotos.contains(photo))
                }
            }
        }

    fun init() = viewModelScope.launch(ioDispatcher) {
        _isRefreshing.update { true }
        updateDays(
            onSuccess = { days ->
                days.firstOrNull()?.let { day ->
                    updatePhotosForDay(day)
                    _selectedDay.update { day }
                }
            }
        )
        _isInitiated.update { true }
        _isRefreshing.update { false }
    }

    fun refreshData() = viewModelScope.launch(ioDispatcher) {
        _isRefreshing.update { true }
        updateDays(
            onSuccess = { days ->
                days.firstOrNull()?.let { day ->
                    _daysFetchedMutex.withLock { _daysFetched.remove(day) }
                    updatePhotosForDay(day)
                }
            }
        )
        _isRefreshing.update { false }
    }

    fun onPermissionsGranted() {
        refreshData()
        updateSavedPhotos()
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

    fun onDayChanged(day: Day) = viewModelScope.launch(ioDispatcher) {
        updatePhotosForDay(day)
        _selectedDay.update { day }
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

    fun selectDeselectAllPhotos() {
        val day = _selectedDay.value ?: return
        val updatedSelectedPhotos = selectOrDeselectAllPhotosByDateUseCase(
            allSelectablePhotosWithDate = devicePhotosUiState.value.dayGroupedSelectablePhotos[day].orEmpty(),
            allSelectedPhotos = _selectedPhotos.value,
            date = day.date
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
        eventFlow.emit(Event.NavigateToRemovePhotosDialog)
    }

    fun createTimelapseFromSelectedPhotos() = viewModelScope.launch {
        val timelapsePhotos = _selectedPhotos.value.sortedBy { it.dateTime }

        if (timelapsePhotos.size >= 2) {
            timelapseCreator.prepare(timelapsePhotos)
            eventFlow.emit(Event.NavigateToTimelapseCreatorScreen)
            resetSelectedPhotos()
        } else {
            eventFlow.emit(Event.ShowNotEnoughSelectedInfo)
        }
    }

    private suspend fun updateDays(onSuccess: suspend (days: List<Day>) -> Unit) {
        dayRepository.updateDeviceDaysRemote(deviceId)
            .onSuccess {
                val days = _daysFlow.firstOrNull() ?: emptyList()
                onSuccess(days)
            }
            .onFailure { e ->
                Timber.e("updateDays error: $e")
                eventFlow.emit(Event.ShowError(e.getErrorMessageId()))
            }
    }

    private fun combineDaysToPhotosMaps(
        map1: Map<Day, List<Photo>>,
        map2: Map<Day, List<Photo>>
    ): Map<Day, List<Photo>> {
        val combinedMap = mutableMapOf<Day, MutableList<Photo>>()

        for ((day, photos) in map1) {
            combinedMap.getOrPut(day) { mutableListOf() }.addAll(photos)
        }

        for ((day, photos) in map2) {
            combinedMap.getOrPut(day) { mutableListOf() }.addAll(photos)
        }

        return combinedMap
            .toSortedMap(compareByDescending { it.date })
            .mapValues { entry ->
                entry.value.sortedByDescending { it.dateTime }
            }
    }

    private suspend fun updatePhotosForDay(day: Day) {
        val shouldUpdate = _daysFetchedMutex.withLock { !_daysFetched.contains(day) }

        if (shouldUpdate) {
            _isRefreshing.update { true }
            photoRepository.updateAllPhotosByDayRemote(day)
                .onSuccess { _daysFetchedMutex.withLock { _daysFetched.add(day) } }
                .onFailure { e ->
                    Timber.e("updatePhotosForDay error: $e")
                    eventFlow.emit(Event.ShowError(e.getErrorMessageId()))
                }
            _isRefreshing.update { false }
        }
    }

    private fun updateSavedPhotos() {
        photoRepository.forceRefreshSavedPhotosContent()
    }

    private fun openPhotoPreview(selectedPhoto: Photo) = viewModelScope.launch(ioDispatcher) {
        // FIXME: pass selected day and selected photo as nav args (for now not possible because
        //        of this: https://issuetracker.google.com/issues/341319151)
        devicePhotosUiState.value.dayGroupedSelectablePhotos.values
            .flatten()
            .map { it.item }
            .sortedByDescending { it.dateTime }
            .let { photos ->
                val index = photos.indexOf(selectedPhoto)
                eventFlow.emit(Event.NavigateToPhotoPreview(index))
            }
    }

    sealed class Event {
        data class NavigateToPhotoPreview(val initialIndex: Int) : Event()
        data object NavigateToRemovePhotosDialog : Event()
        data class NavigateToSavePhotosDialog(val photos: List<Photo>) : Event()
        data object NavigateToTimelapseCreatorScreen : Event()
        data class ShowSavedInfo(val totalCount: Int, val savedCount: Int) : Event()
        data class ShowError(val messageId: Int) : Event()
        data object ShowNotEnoughSelectedInfo : Event()
    }

    @Immutable
    data class UiState(
        val dayGroupedSelectablePhotos: ImmutableMap<Day, List<Selectable<Photo>>>,
        val filterMode: PhotosFilterMode,
        val isLoading: Boolean,
        val isOnline: Boolean,
        val isSelectionMode: Boolean,
        val isInitiated: Boolean,
        val selectedCount: Int
    )
}
