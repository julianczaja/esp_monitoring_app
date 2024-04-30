package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DevicePhotosScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceIdArgs: DeviceIdArgs = DeviceIdArgs(savedStateHandle)

    private val apiError = MutableStateFlow<Int?>(null)

    private val isRefreshing = MutableStateFlow(false)

    val devicePhotosUiState: StateFlow<DevicePhotosScreenUiState> = devicePhotosUiState()
        .onStart { updatePhotos() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DevicePhotosScreenUiState(DevicePhotosState.Loading, true)
        )

    private fun devicePhotosUiState() = combine(photosStream(), apiError, isRefreshing) { photos, apiErr, refreshing ->
        return@combine if (apiErr != null) {
            DevicePhotosScreenUiState(DevicePhotosState.Error(apiErr), refreshing)
        } else {
            DevicePhotosScreenUiState(photos, refreshing)
        }
    }

    private fun photosStream() = photoRepository.getAllPhotosLocal(deviceIdArgs.deviceId)
        .map<List<Photo>, DevicePhotosState> { photos -> DevicePhotosState.Success(groupPhotosByDayDesc(photos)) }
        .flowOn(ioDispatcher)
        .catch { emit(DevicePhotosState.Error(it.getErrorMessageId())) }
        .onStart { emit(DevicePhotosState.Loading) }

    fun updatePhotos() {
        isRefreshing.update { true }

        viewModelScope.launch(ioDispatcher) {
            photoRepository.updateAllPhotosRemote(deviceIdArgs.deviceId)
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

    private fun groupPhotosByDayDesc(photos: List<Photo>) = photos.groupBy { it.dateTime.toLocalDate() }

    @Immutable
    data class DevicePhotosScreenUiState(
        val devicePhotosUiState: DevicePhotosState,
        val isRefreshing: Boolean,
    )

    @Immutable
    sealed interface DevicePhotosState {
        data class Success(val dateGroupedPhotos: Map<LocalDate, List<Photo>>) : DevicePhotosState
        data object Loading : DevicePhotosState
        data class Error(@StringRes val messageId: Int) : DevicePhotosState
    }
}
