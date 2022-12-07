package com.julianczaja.esp_monitoring_app.presentation.device

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.DeviceArgs
import com.julianczaja.esp_monitoring_app.domain.model.Photo
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

    val deviceUiState: StateFlow<DeviceScreenUiState> = deviceUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeviceScreenUiState.Loading
        )

    private fun deviceUiState(): Flow<DeviceScreenUiState> = photoRepository.getAllPhotosLocal(deviceArgs.deviceId)
        .onStart { DeviceScreenUiState.Loading }
        .catch { DeviceScreenUiState.Error(it.message) }
        .map { DeviceScreenUiState.Success(photos = it) }

    fun updatePhotos() = viewModelScope.launch(Dispatchers.IO) {
        // TODO: Check if it's not in progress already
        photoRepository.getAllPhotosRemote(deviceArgs.deviceId)
    }
}

sealed interface DeviceScreenUiState {
    data class Success(val photos: List<Photo>) : DeviceScreenUiState
    object Loading : DeviceScreenUiState
    data class Error(val message: String?) : DeviceScreenUiState
}
