package com.julianczaja.esp_monitoring_app.presentation.photopreview

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.navigation.PhotoPreviewDialog
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PhotoPreviewDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val initialIndex = savedStateHandle.toRoute<PhotoPreviewDialog>().initialIndex

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun init(parentViewModelState: DevicePhotosScreenViewModel.UiState) {
        viewModelScope.launch(ioDispatcher) {
            when (initialIndex) {
                -1 -> {
                    Timber.e("Something went wrong: initialIndex is -1")
                    _uiState.update { UiState.Error(R.string.unknown_error_message) }
                }

                else -> {
                    val photos = parentViewModelState
                        .dayGroupedSelectablePhotos.values
                        .flatten()
                        .map { it.item }
                        .sortedByDescending { it.dateTime }
                        .toImmutableList()

                    _uiState.update { UiState.Success(photos, initialIndex) }
                }
            }
        }
    }

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val photos: ImmutableList<Photo>,
            val initialPhotoIndex: Int,
        ) : UiState

        data class Error(@StringRes val messageId: Int) : UiState
    }
}
