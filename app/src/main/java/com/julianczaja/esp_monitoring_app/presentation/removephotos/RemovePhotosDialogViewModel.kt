package com.julianczaja.esp_monitoring_app.presentation.removephotos

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
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
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // FIXME: It seems to not work in beta04
    // private val photos = savedStateHandle.toRoute<RemovePhotosDialog>().photos
    private val photos: List<Photo> = savedStateHandle.get<Array<Photo>>("photos")?.toList() ?: emptyList()

    val eventFlow = MutableSharedFlow<Event>()

    private val _uiState = MutableStateFlow<UiState>(
        UiState.Confirm(
            photos = photos,
            shouldShowRemoveSaved = photos.fastAny { it.isSaved } && photos.fastAny { !it.isSaved }, // TODO: Tests
            removeSaved = photos.all { it.isSaved } // TODO: Tests
        )
    )
    val uiState = _uiState.asStateFlow()

    private var removePhotosJob: Job? = null

    fun onRemoveSavedChanged(removeSaved: Boolean) {
        (uiState.value as? UiState.Confirm)?.let { uiState ->
            _uiState.update { uiState.copy(removeSaved = removeSaved) }
        }
    }

    fun removePhotos() = viewModelScope.launch(ioDispatcher) {
        val removeSaved = (uiState.value as? UiState.Confirm)?.removeSaved ?: false

        _uiState.update { UiState.Removing(0f) }

        val results = mutableMapOf<Photo, Int?>()

        removePhotosJob = viewModelScope.launch(ioDispatcher) {
            photos.forEachIndexed { index, photo ->
                _uiState.update { UiState.Removing(index / photos.size.toFloat()) }
                try {
                    when (photo.isSaved) { // TODO: Tests
                        true -> if (removeSaved) {
                            removeSavedPhoto(photo)
                            results[photo] = null
                        }

                        false -> {
                            removeBackendPhoto(photo)
                            results[photo] = null
                        }
                    }
                } catch (e: CancellationException) {
                    results[photo] = R.string.remove_photo_cancelled
                } catch (e: SecurityException) {
                    results[photo] = R.string.remove_photo_security_error_message
                } catch (e: Exception) {
                    Timber.e(e)
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

    private suspend fun removeBackendPhoto(photo: Photo) {
        photoRepository.removePhotoByFileNameRemote(photo.fileName).getOrThrow()
        photoRepository.removePhotoByFileNameLocal(photo.fileName).getOrThrow()
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
        data class Confirm(
            val photos: List<Photo>,
            val shouldShowRemoveSaved: Boolean,
            val removeSaved: Boolean = false
        ) : UiState

        data class Removing(val progress: Float) : UiState
        data class Error(val results: Map<Photo, Int?>) : UiState
    }
}
