package com.julianczaja.esp_monitoring_app.presentation.removephoto

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.PhotoFileNameArgs
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemovePhotoDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    enum class Event {
        PHOTO_REMOVED,
    }

    private val photoFileNameArgs = PhotoFileNameArgs(savedStateHandle)

    val eventFlow = MutableSharedFlow<Event>()

    private val _uiState = MutableStateFlow<RemovePhotoScreenUiState>(RemovePhotoScreenUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            val photo = photoRepository.getPhotoByFileNameLocal(photoFileNameArgs.fileName)
            if (photo != null) {
                _uiState.update { RemovePhotoScreenUiState.Success(photo) }
            } else {
                _uiState.update { RemovePhotoScreenUiState.Error(R.string.internal_app_error_message) }
            }
        }
    }

    fun removePhoto(photo: Photo) = viewModelScope.launch(ioDispatcher) {
        _uiState.update { RemovePhotoScreenUiState.Loading }
        try {
            photoRepository.removePhotoByFileNameRemote(photo.fileName)
            photoRepository.removePhotoByFileNameLocal(photo.fileName)
            eventFlow.emit(Event.PHOTO_REMOVED)
        } catch (e: Exception) {
            _uiState.update { RemovePhotoScreenUiState.Error(e.getErrorMessageId()) }
        }
    }

    fun convertExplanationStringToStyledText(explanationText: String, spanStyle: SpanStyle): AnnotatedString {
        return buildAnnotatedString {
            append(explanationText)
            val from = explanationText.indexOf("\"", startIndex = 0) + 1
            val to = explanationText.indexOf("\"", startIndex = from + 1)
            addStyle(spanStyle, start = from, end = to)
        }
    }
}

@Immutable
sealed interface RemovePhotoScreenUiState {
    data class Success(val photo: Photo) : RemovePhotoScreenUiState
    data object Loading : RemovePhotoScreenUiState
    data class Error(val messageId: Int) : RemovePhotoScreenUiState
}
