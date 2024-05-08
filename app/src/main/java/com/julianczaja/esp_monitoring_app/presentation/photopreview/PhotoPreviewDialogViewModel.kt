package com.julianczaja.esp_monitoring_app.presentation.photopreview

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.InternalAppException
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.navigation.PhotoPreviewDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PhotoPreviewDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceId = savedStateHandle.toRoute<PhotoPreviewDialog>().deviceId
    private val photoFileName = savedStateHandle.toRoute<PhotoPreviewDialog>().photoFileName

    val uiState: StateFlow<UiState> = photoPreviewUiState()
        .catch { UiState.Error(it.getErrorMessageId()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    private fun photoPreviewUiState() = photosStream()
        .map { photos ->
            if (photos.isNotEmpty()) {
               UiState.Success(
                    photos = photos,
                    initialPhotoIndex = photos.indexOfFirst { it.fileName == photoFileName }
                )
            } else {
                UiState.Error(InternalAppException().getErrorMessageId())
            }
        }
        .flowOn(ioDispatcher)

    private fun photosStream(): Flow<List<Photo>> = if (deviceId != DeviceIdArgs.NO_VALUE) {
        photoRepository.getAllPhotosLocal(deviceId)
    } else {
        photoRepository.getPhotoByFileNameLocal(photoFileName)
            .mapNotNull { photo -> photo?.let { listOf(it) } } // FIXME
    }

    @Immutable
    sealed interface UiState {
        data class Success(
            val photos: List<Photo>,
            val initialPhotoIndex: Int,
        ) : UiState

        data object Loading : UiState
        data class Error(@StringRes val messageId: Int) : UiState
    }
}
