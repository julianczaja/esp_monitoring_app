package com.julianczaja.esp_monitoring_app.presentation.photopreview

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.navigation.PhotoFileNameArgs
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.InternalAppException
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PhotoPreviewDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    private val deviceIdArgs: DeviceIdArgs = DeviceIdArgs(savedStateHandle)
    private val photoFileNameArgs: PhotoFileNameArgs = PhotoFileNameArgs(savedStateHandle)

    val uiState: StateFlow<UiState> = photoPreviewUiState()
        .catch { UiState.Error(it.getErrorMessageId()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    private fun photoPreviewUiState() = combine(deviceStream(), photosStream()) { device, photos ->
        if (device != null && photos.isNotEmpty()) {
            UiState.Success(
                device = device,
                photos = photos,
                initialPhotoIndex = photos.indexOfFirst { it.fileName == photoFileNameArgs.fileName }
            )
        } else {
            UiState.Error(InternalAppException().getErrorMessageId())
        }
    }

    private fun deviceStream() = deviceRepository.getDeviceById(deviceIdArgs.deviceId)

    private fun photosStream() = photoRepository.getAllPhotosLocal(deviceIdArgs.deviceId)

    @Immutable
    sealed interface UiState {
        data class Success(
            val device: Device,
            val photos: List<Photo>,
            val initialPhotoIndex: Int,
        ) : UiState

        data object Loading : UiState
        data class Error(@StringRes val messageId: Int) : UiState
    }
}
