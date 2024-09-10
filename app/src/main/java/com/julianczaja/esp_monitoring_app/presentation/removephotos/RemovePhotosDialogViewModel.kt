package com.julianczaja.esp_monitoring_app.presentation.removephotos

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RemovePhotosDialogViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    val eventFlow = MutableSharedFlow<Event>()

    private val _uiState = MutableStateFlow<UiState>(UiState.Init)
    val uiState = _uiState.asStateFlow()

    private var removePhotosJob: Job? = null

    fun init(parentViewModelState: DevicePhotosScreenViewModel.UiState) {
        val photos = parentViewModelState
            .dayGroupedSelectablePhotos.values
            .flatten()
            .filter { it.isSelected }
            .map { it.item }

        _uiState.update {
            UiState.Confirm(
                photos = photos,
                shouldShowRemoveSaved = photos.fastAny { it.isSaved } && photos.fastAny { !it.isSaved }, // TODO: Tests
                removeSaved = photos.all { it.isSaved } // TODO: Tests
            )
        }
    }

    fun onRemoveSavedChanged(removeSaved: Boolean) {
        (_uiState.value as? UiState.Confirm)?.let { uiState ->
            _uiState.update { uiState.copy(removeSaved = removeSaved) }
        }
    }

    fun removePhotos() = viewModelScope.launch(ioDispatcher) {
        val removeSaved = (_uiState.value as? UiState.Confirm)?.removeSaved ?: false
        val allPhotos = (_uiState.value as? UiState.Confirm)?.photos.orEmpty()
        val (savedPhotos, remotePhotos) = allPhotos.partition { it.isSaved }
        val results = mutableMapOf<Photo, Int?>()
        var index = 0

        _uiState.update { UiState.Removing(0f) }

        removePhotosJob = viewModelScope.launch(ioDispatcher) {
            // saved
            if (removeSaved) {
                savedPhotos.forEach { photo ->
                    try {
                        removeSavedPhoto(photo)
                        results[photo] = null
                    } catch (e: CancellationException) {
                        results[photo] = R.string.remove_photo_cancelled
                    } catch (e: SecurityException) {
                        results[photo] = R.string.remove_photo_security_error_message
                    } catch (e: Exception) {
                        Timber.e(e)
                        results[photo] = e.getErrorMessageId()
                    }
                    _uiState.update { UiState.Removing(index++ / allPhotos.size.toFloat()) }
                }
            }

            // remote
            try {
                photoRepository.removePhotosByFileNamesRemote(remotePhotos.map { it.fileName }).getOrThrow()
                remotePhotos.forEach { photo ->
                    try {
                        photoRepository.removePhotoByFileNameLocal(photo.fileName).getOrThrow()
                        results[photo] = null
                    } catch (e: CancellationException) {
                        results[photo] = R.string.remove_photo_cancelled
                    } catch (e: Exception) {
                        Timber.e(e)
                        results[photo] = e.getErrorMessageId()
                    }
                    _uiState.update { UiState.Removing(index++ / allPhotos.size.toFloat()) }
                }

            } catch (e: CancellationException) {
                remotePhotos.forEach { photo ->
                    results[photo] = R.string.remove_photo_cancelled
                }
            } catch (e: Exception) {
                Timber.e(e)
                remotePhotos.forEach { photo ->
                    results[photo] = e.getErrorMessageId()
                }
            }
        }

        removePhotosJob?.join()

        when (results.values.all { it == null }) {
            true -> eventFlow.emit(Event.PHOTOS_REMOVED)
            false -> _uiState.update { UiState.Error(results) }
        }
    }

    private suspend fun removeSavedPhoto(photo: Photo) {
        photoRepository.removeSavedPhotoFromExternalStorage(photo).getOrThrow()
    }

    fun cancelRemoval() {
        removePhotosJob?.cancel()
    }

    enum class Event {
        PHOTOS_REMOVED,
    }

    @Immutable
    sealed interface UiState {
        data object Init : UiState
        data class Confirm(
            val photos: List<Photo>,
            val shouldShowRemoveSaved: Boolean,
            val removeSaved: Boolean = false
        ) : UiState

        data class Removing(val progress: Float) : UiState
        data class Error(val results: Map<Photo, Int?>) : UiState
    }
}
