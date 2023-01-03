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

    private val isRefreshing = MutableStateFlow(false)

    val deviceUiState: StateFlow<DeviceScreenUiState> = deviceUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeviceScreenUiState(DevicePhotosUiState.Loading, true)
        )

    init {
        updatePhotos()
    }

    private fun deviceUiState(): Flow<DeviceScreenUiState> = combine(photosStream(), apiError, isRefreshing) { photos, apiErr, refreshing ->
        return@combine if (apiErr != null) {
            DeviceScreenUiState(DevicePhotosUiState.Error(apiErr), refreshing)
        } else {
            DeviceScreenUiState(photos, refreshing)
        }
    }

    private fun photosStream() = photoRepository.getAllPhotosLocal(deviceArgs.deviceId)
        .onStart { DevicePhotosUiState.Loading }
        .catch { DevicePhotosUiState.Error(it.getErrorMessageId()) }
        .map { DevicePhotosUiState.Success(photos = it) }

    fun updatePhotos() = viewModelScope.launch(Dispatchers.IO) {
        isRefreshing.update { true }
        photoRepository.updateAllPhotosRemote(deviceArgs.deviceId) // TODO: Check if it's not in progress already
            .onFailure {
                apiError.emit(it.getErrorMessageId())
                isRefreshing.update { false }
            }
            .onSuccess {
                apiError.emit(null)
                isRefreshing.update { false }
            }
    }
}

data class DeviceScreenUiState(
    val devicePhotosUiState: DevicePhotosUiState,
    val isRefreshing: Boolean,
)

sealed interface DevicePhotosUiState {
    data class Success(val photos: List<Photo>) : DevicePhotosUiState
    object Loading : DevicePhotosUiState
    data class Error(@StringRes val messageId: Int) : DevicePhotosUiState
}
