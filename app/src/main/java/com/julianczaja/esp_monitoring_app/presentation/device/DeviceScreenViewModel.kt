package com.julianczaja.esp_monitoring_app.presentation.device

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.DeviceArgs
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    private val deviceArgs: DeviceArgs = DeviceArgs(savedStateHandle)

    private val apiError = MutableStateFlow<Int?>(null)

    val deviceUiState: StateFlow<DeviceScreenUiState> = deviceUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeviceScreenUiState.Loading
        )

    private fun deviceUiState(): Flow<DeviceScreenUiState> = combine(photosStream(), apiError) { photos, apiErr ->
        return@combine if (apiErr != null) {
            DeviceScreenUiState.Error(apiErr)
        } else {
            photos
        }
    }

    private fun photosStream() = photoRepository.getAllPhotosLocal(deviceArgs.deviceId)
        .onStart { DeviceScreenUiState.Loading }
        .catch { DeviceScreenUiState.Error(it.getErrorMessageId()) }
        .map { DeviceScreenUiState.Success(photos = it) }

    fun updatePhotos() = viewModelScope.launch(Dispatchers.IO) {
        photoRepository.updateAllPhotosRemote(deviceArgs.deviceId) // TODO: Check if it's not in progress already
            .onFailure { apiError.emit(it.getErrorMessageId()) }
            .onSuccess { apiError.emit(null) }
    }
}

sealed interface DeviceScreenUiState {
    data class Success(val photos: List<Photo>) : DeviceScreenUiState
    object Loading : DeviceScreenUiState
    data class Error(@StringRes val messageId: Int) : DeviceScreenUiState
}
