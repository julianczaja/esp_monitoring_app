package com.julianczaja.esp_monitoring_app.presentation.removephotos

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.navigation.RemovePhotosDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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

    private val params = savedStateHandle.toRoute<RemovePhotosDialog>().params

    val eventFlow = MutableSharedFlow<Event>()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        if (params.photosFileNames.isNotEmpty()) {
            _uiState.update { UiState.Success(params.photosFileNames) }
        } else {
            _uiState.update { UiState.Error(R.string.internal_app_error_message) }
        }
    }

    fun removePhotos() = viewModelScope.launch(ioDispatcher) {
        _uiState.update { UiState.Loading }

        var isError = false

        params.photosFileNames.forEach { photoFileName ->
            photoRepository.removePhotoByFileNameRemote(photoFileName)
                .onFailure { e ->
                    Timber.e(e)
                    _uiState.update { UiState.Error(e.getErrorMessageId()) }
                    isError = true
                    return@forEach
                }
                .onSuccess {
                    photoRepository.removePhotoByFileNameLocal(photoFileName)
                        .onFailure { e ->
                            Timber.e(e)
                            _uiState.update { UiState.Error(e.getErrorMessageId()) }
                            isError = true
                            return@forEach
                        }
                }
        }
        if (!isError) eventFlow.emit(Event.PHOTOS_REMOVED)
    }

    fun convertExplanationStringToStyledText(explanationText: String, spanStyle: SpanStyle): AnnotatedString {
        return buildAnnotatedString {
            val from = explanationText.indexOf("*")
            val to = explanationText.indexOf("*", startIndex = from + 1)
            append(explanationText.replace("*", ""))
            addStyle(spanStyle, start = from, end = to)
        }
    }

    enum class Event {
        PHOTOS_REMOVED,
    }

    @Immutable
    sealed interface UiState {
        data class Success(val photosFileNames: List<String>) : UiState
        data object Loading : UiState
        data class Error(@StringRes val messageId: Int) : UiState
    }
}
