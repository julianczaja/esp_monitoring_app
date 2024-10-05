package com.julianczaja.esp_monitoring_app.presentation.timelapsecreator

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.TimelapseCreator
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.TimelapseCancelledException
import com.julianczaja.esp_monitoring_app.domain.model.TimelapseData
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class TimelapseCreatorScreenViewModel @Inject constructor(
    private val timelapseCreator: TimelapseCreator,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private companion object {
        const val DEFAULT_FRAME_RATE = 30
        const val DEFAULT_COMPRESSION_RATE = 4
    }

    private val photos: List<Photo> = timelapseCreator.photos

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.initial(photos))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val eventFlow = MutableSharedFlow<Event>()

    private var lastFrameRate = DEFAULT_FRAME_RATE
    private var lastCompressionRate = DEFAULT_COMPRESSION_RATE
    private var lastIsHighQuality = false
    private var lastIsReversed = false

    private var processTimelapseJob: Job? = null
    private var createTimelapseJob: Job? = null

    fun onBackPressed() {
        val uiState = uiState.value

        when (uiState) {
            is UiState.Configure -> Unit
            is UiState.Preview, is UiState.Process -> {
                timelapseCreator.cancel()
                processTimelapseJob?.cancel()
                createTimelapseJob?.cancel()
                _uiState.update {
                    UiState.initial(
                        photos = photos,
                        frameRate = lastFrameRate,
                        compressionRate = lastCompressionRate,
                        isHighQuality = lastIsHighQuality,
                        isReversed = lastIsReversed
                    )
                }
            }
        }
    }

    fun updateFrameRate(newFrameRate: Int) {
        (_uiState.value as? UiState.Configure)?.let { uiState ->
            lastFrameRate = newFrameRate
            _uiState.update {
                uiState.copy(
                    estimatedTime = photos.size / newFrameRate.toFloat(),
                    frameRate = newFrameRate
                )
            }
        }
    }

    fun updateCompressionRate(newCompressionRate: Int) {
        (_uiState.value as? UiState.Configure)?.let { uiState ->
            lastCompressionRate = newCompressionRate
            _uiState.update { uiState.copy(compressionRate = newCompressionRate) }
        }
    }

    fun updateIsHighQuality(newIsHighQuality: Boolean) {
        (_uiState.value as? UiState.Configure)?.let { uiState ->
            lastIsHighQuality = newIsHighQuality
            _uiState.update { uiState.copy(isHighQuality = newIsHighQuality) }
        }
    }

    fun updateIsReversed(newIsReversed: Boolean) {
        (_uiState.value as? UiState.Configure)?.let { uiState ->
            lastIsReversed = newIsReversed
            _uiState.update { uiState.copy(isReversed = newIsReversed) }
        }
    }

    fun start() {
        (_uiState.value as? UiState.Configure)?.let { uiState ->

            _uiState.update { UiState.Process(0L, 0f, 0f) }

            processTimelapseJob = viewModelScope.launch(ioDispatcher) {
                combine(
                    timelapseCreator.downloadedBytes,
                    timelapseCreator.unZipProgress,
                    timelapseCreator.processProgress,
                ) { downloadedBytes, unZipProgress, processProgress ->
                    _uiState.update { UiState.Process(downloadedBytes, unZipProgress, processProgress) }
                }.collect()
            }

            createTimelapseJob = viewModelScope.launch(ioDispatcher) {
                try {
                    timelapseCreator.createTimelapse(
                        photos = photos,
                        isHighQuality = uiState.isHighQuality,
                        isReversed = uiState.isReversed,
                        frameRate = uiState.frameRate,
                        compressionRate = uiState.compressionRate
                    ).onSuccess { timelapseData ->
                        _uiState.update { UiState.Preview(timelapseData) }
                    }.onFailure { e ->
                        onError(e)
                    }
                } catch (e: CancellationException) {
                    onError(TimelapseCancelledException())
                } catch (e: Exception) {
                    onError(e)
                }
            }
        } ?: run {
            Timber.e("Error: start() - UI state is not UiState.Configure (${_uiState.value})")
        }
    }

    fun saveTimelapse() = viewModelScope.launch(ioDispatcher) {
        (_uiState.value as? UiState.Preview)?.let { uiState ->
            _uiState.update { uiState.copy(isBusy = true) }
        }
        timelapseCreator.saveTimelapse(photos.last().deviceId)
            .onFailure { e -> onError(e) }
            .onSuccess {
                (_uiState.value as? UiState.Preview)?.let { uiState ->
                    _uiState.update { uiState.copy(isBusy = false, isSaved = true) }
                }
                eventFlow.emit(Event.ShowSaved)
            }
    }

    private fun onError(throwable: Throwable) = viewModelScope.launch {
        Timber.e(throwable)
        _uiState.update {
            UiState.initial(
                photos = photos,
                frameRate = lastFrameRate,
                compressionRate = lastCompressionRate,
                isHighQuality = lastIsHighQuality,
                isReversed = lastIsReversed
            )
        }
        eventFlow.emit(Event.ShowError(throwable.getErrorMessageId()))
    }

    override fun onCleared() {
        timelapseCreator.clear()
        super.onCleared()
    }

    sealed class Event {
        data class ShowError(val messageId: Int) : Event()
        data object ShowSaved : Event()
    }

    sealed class UiState {
        @Stable
        data class Configure(
            val photosCount: Int,
            val oldestPhotoDateTime: LocalDateTime,
            val newestPhotoDateTime: LocalDateTime,
            val estimatedTime: Float,
            val frameRate: Int,
            val compressionRate: Int,
            val isHighQuality: Boolean,
            val isReversed: Boolean
        ) : UiState()

        data class Process(
            val downloadedBytes: Long,
            val unZipProgress: Float,
            val processProgress: Float
        ) : UiState()

        data class Preview(
            val timelapseData: TimelapseData,
            val isBusy: Boolean = false,
            val isSaved: Boolean = false
        ) : UiState()

        companion object {
            fun initial(
                photos: List<Photo>,
                frameRate: Int = DEFAULT_FRAME_RATE,
                compressionRate: Int = DEFAULT_COMPRESSION_RATE,
                isHighQuality: Boolean = false,
                isReversed: Boolean = false
            ) = Configure(
                photosCount = photos.size,
                oldestPhotoDateTime = photos.first().dateTime,
                newestPhotoDateTime = photos.last().dateTime,
                estimatedTime = photos.size.toFloat() / DEFAULT_FRAME_RATE,
                frameRate = frameRate,
                compressionRate = compressionRate,
                isHighQuality = isHighQuality,
                isReversed = isReversed
            )
        }
    }
}
