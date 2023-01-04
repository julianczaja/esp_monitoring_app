package com.julianczaja.esp_monitoring_app.presentation.devicephotos

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
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DevicePhotosScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    private val deviceArgs: DeviceArgs = DeviceArgs(savedStateHandle)

    private val apiError = MutableStateFlow<Int?>(null)

    private val isRefreshing = MutableStateFlow(false)

    val devicePhotosUiState: StateFlow<DevicePhotosScreenUiState> = devicePhotosUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DevicePhotosScreenUiState(DevicePhotosState.Loading, true)
        )

    init {
        updatePhotos()
    }

    private fun devicePhotosUiState(): Flow<DevicePhotosScreenUiState> =
        combine(photosStream(), apiError, isRefreshing) { photos, apiErr, refreshing ->
            return@combine if (apiErr != null) {
                DevicePhotosScreenUiState(DevicePhotosState.Error(apiErr), refreshing)
            } else {
                DevicePhotosScreenUiState(photos, refreshing)
            }
        }

    private fun photosStream() = photoRepository.getAllPhotosLocal(deviceArgs.deviceId)
        .onStart { DevicePhotosState.Loading }
        .catch { DevicePhotosState.Error(it.getErrorMessageId()) }
        .map { photos -> DevicePhotosState.Success(dateGroupedPhotos = photos.groupBy { it.dateTime.toLocalDate() }) }

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

data class DevicePhotosScreenUiState(
    val devicePhotosUiState: DevicePhotosState,
    val isRefreshing: Boolean,
)

sealed interface DevicePhotosState {
    data class Success(val dateGroupedPhotos: Map<LocalDate, List<Photo>>) : DevicePhotosState
    object Loading : DevicePhotosState
    data class Error(@StringRes val messageId: Int) : DevicePhotosState
}
