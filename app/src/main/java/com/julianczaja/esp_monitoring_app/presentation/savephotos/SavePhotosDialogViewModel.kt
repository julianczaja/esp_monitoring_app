package com.julianczaja.esp_monitoring_app.presentation.savephotos

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.PhotoAlreadyExistsException
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class SavePhotosDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // FIXME: It seems to not work in beta04
    // private val photos = savedStateHandle.toRoute<SavePhotosDialog>().photos
    private val photos: List<Photo> = savedStateHandle.get<Array<Photo>>("photos")?.toList() ?: emptyList()

    val eventFlow = MutableSharedFlow<Event>()

    private val _uiState = MutableStateFlow<UiState>(UiState.Saving(progress = 0f))
    val uiState = _uiState.asStateFlow()

    private var savePhotosJob: Job? = null

    fun save() = viewModelScope.launch(ioDispatcher) {

        val results = mutableMapOf<Photo, Int?>()

        savePhotosJob = viewModelScope.launch(ioDispatcher) {
            photos.forEachIndexed { index, photo ->
                _uiState.update { UiState.Saving(index / photos.size.toFloat()) }
                try {
                    when (photo.isSaved) {
                        true -> {
                            results[photo] = R.string.already_saved_label
                        }

                        false -> {
                            savePhoto(photo)
                            results[photo] = null
                        }
                    }
                } catch (e: CancellationException) {
                    results[photo] = R.string.save_photo_cancelled
                } catch (e: PhotoAlreadyExistsException) {
                    results[photo] = R.string.already_saved_label
                } catch (e: Exception) {
                    Timber.e("Error while saving ${photo.fileName}: $e")
                    results[photo] = e.getErrorMessageId()
                }
            }
        }

        savePhotosJob?.join()

        when (results.values.all { it == null }) {
            true -> eventFlow.emit(Event.PHOTOS_SAVED)
            false -> _uiState.update { UiState.Error(results) }
        }
    }

    private suspend fun savePhoto(photo: Photo) {
        photoRepository.downloadPhotoAndSaveToExternalStorage(photo).getOrThrow()
    }

    fun cancelSaving() {
        savePhotosJob?.cancel()
    }

    @Immutable
    sealed interface UiState {
        data class Saving(val progress: Float) : UiState
        data class Error(val results: Map<Photo, Int?>) : UiState
    }

    enum class Event {
        PHOTOS_SAVED,
    }
}
