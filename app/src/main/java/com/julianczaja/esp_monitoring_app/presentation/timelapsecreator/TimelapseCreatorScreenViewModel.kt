package com.julianczaja.esp_monitoring_app.presentation.timelapsecreator

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.TimelapseCreator
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.TimelapseData
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TimelapseCreatorScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val timelapseCreator: TimelapseCreator,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val photos: List<Photo> = savedStateHandle.get<Array<Photo>>("photos")?.toList() ?: emptyList()
    // FIXME: It seems to not work in beta03
    // private val photos: List<Photo> = (savedStateHandle.toRoute<TimelapseCreatorScreen>().photos as ArrayList).toList()

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(
        UiState.Configure(
            photosCount = photos.size,
            estimatedTime = photos.size / 30f
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateFrameRate(newFrameRate: Int) {
        (_uiState.value as? UiState.Configure)?.let { uiState ->
            _uiState.update {
                uiState.copy(
                    estimatedTime = photos.size / newFrameRate.toFloat(),
                    frameRate = newFrameRate
                )
            }
        } ?: run {
            Timber.e("Error: start() - UI state is not UiState.Configure (${_uiState.value})")
        }
    }

    fun updateIsHighQuality(newIsHighQuality: Boolean) {
        (_uiState.value as? UiState.Configure)?.let { uiState ->
            _uiState.update { uiState.copy(isHighQuality = newIsHighQuality) }
        } ?: run {
            Timber.e("Error: start() - UI state is not UiState.Configure (${_uiState.value})")
        }
    }

    fun start() {
        (_uiState.value as? UiState.Configure)?.let { uiState ->

            _uiState.update { UiState.Process(false, 0f, 0f) }

            viewModelScope.launch(ioDispatcher) {
                combine(
                    timelapseCreator.isBusy,
                    timelapseCreator.downloadProgress,
                    timelapseCreator.processProgress,
                ) { isBusy, downloadProgress, processProgress ->
                    _uiState.update { UiState.Process(isBusy, downloadProgress, processProgress) }
                }.collect()
            }

            viewModelScope.launch(ioDispatcher) {
                try {
                    timelapseCreator.createTimelapse(
                        photos = photos,
                        isHighQuality = uiState.isHighQuality,
                        frameRate = uiState.frameRate
                    ).onSuccess { timelapseData ->
                        _uiState.update { UiState.Done(timelapseData) }
                    }.onFailure { e ->
                        Timber.e("Error: start() onFailure - $e")
                        _uiState.update { UiState.Error(e.getErrorMessageId()) }
                    }
                } catch (e: Exception) {
                    Timber.e("Error: start() - $e")
                    _uiState.update { UiState.Error(e.getErrorMessageId()) }
                }
            }
        } ?: run {
            Timber.e("Error: start() - UI state is not UiState.Configure (${_uiState.value})")
        }
    }

    override fun onCleared() {
        timelapseCreator.clear()
        super.onCleared()
    }

    sealed class UiState {
        data class Configure(
            val photosCount: Int,
            val estimatedTime: Float,
            val frameRate: Int = 30,
            val isHighQuality: Boolean = false
        ) : UiState()

        data class Process(val isBusy: Boolean, val downloadProgress: Float, val processProgress: Float) : UiState()
        data class Done(val timelapseData: TimelapseData) : UiState()
        data class Error(@StringRes val messageId: Int) : UiState()
    }
}
