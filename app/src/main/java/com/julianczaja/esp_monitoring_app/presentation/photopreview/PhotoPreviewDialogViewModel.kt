package com.julianczaja.esp_monitoring_app.presentation.photopreview

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PhotoPreviewDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // FIXME: It seems to not work in beta04
    // private val photos = savedStateHandle.toRoute<PhotoPreviewDialog>().photos
    // private val initialIndex = savedStateHandle.toRoute<PhotoPreviewDialog>().initialIndex
    private val photos: List<Photo> = savedStateHandle.get<Array<Photo>>("photos")?.toList() ?: emptyList()
    private val initialIndex: Int = savedStateHandle.get<Int>("initialIndex") ?: -1


    val uiState: UiState = when (initialIndex) {
        -1 -> {
            Timber.e("Something went wrong: initialIndex is -1")
            UiState.Error(R.string.unknown_error_message)
        }
        else -> {
            UiState.Success(photos, initialIndex)
        }
    }

    @Immutable
    sealed interface UiState {
        data class Success(
            val photos: List<Photo>,
            val initialPhotoIndex: Int,
        ) : UiState

        data class Error(@StringRes val messageId: Int) : UiState
    }
}
