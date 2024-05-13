package com.julianczaja.esp_monitoring_app.presentation.devicesavedphotos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.SelectablePhoto
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.navigation.DeviceScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DeviceSavedPhotosScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceId = savedStateHandle.toRoute<DeviceScreen>().deviceId

    private val _savedPhotos = MutableStateFlow<Map<LocalDate, List<SelectablePhoto>>>(emptyMap())
    val savedPhotos = _savedPhotos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val eventFlow = MutableSharedFlow<Event>()

    fun updateSavedPhotos() {
        readSavedPhotos()
    }

    private fun readSavedPhotos() = viewModelScope.launch(ioDispatcher) {
        _isLoading.emit(true)
        photoRepository.readAllSavedPhotosFromExternalStorage(deviceId)
            .onSuccess {
                Timber.e("readAllSavedPhotosInInternalStorage success: $it")
                _savedPhotos.emit(groupPhotosByDayDesc(it))
            }
            .onFailure {
                Timber.e("readAllSavedPhotosInInternalStorage failure: $it")
                eventFlow.emit(Event.ShowError(R.string.internal_app_error_message))
            }
        _isLoading.emit(false)
    }

    private fun groupPhotosByDayDesc(photos: List<Photo>) = photos
        .map { SelectablePhoto(photo = it, isSelected = false) } // TODO: Implement selection mode
        .groupBy { it.photo.dateTime.toLocalDate() }

    sealed class Event {
        data class NavigateToPhotoPreview(val photo: Photo) : Event()
        data class ShowError(val messageId: Int) : Event()
    }
}
