package com.julianczaja.esp_monitoring_app.presentation.appsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.common.Constants
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.FieldState
import com.julianczaja.esp_monitoring_app.domain.repository.AppSettingsRepository
import com.julianczaja.esp_monitoring_app.presentation.widget.PhotoWidgetUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AppSettingsScreenViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val workManager: WorkManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        private val BASE_URL_REGEX = Regex("""^\s*(https?://[a-zA-Z0-9_.-]+:\d{4,5}/?)""")
    }

    private val _baseUrlFieldState = MutableStateFlow(FieldState(""))
    private var _isBaseUrlFieldInitiated = false

    val eventFlow = MutableSharedFlow<Event>()

    val uiState = combine(
        appSettingsRepository.getBaseUrl(),
        appSettingsRepository.getBaseUrlHistory(),
        appSettingsRepository.getDynamicColor(),
        _baseUrlFieldState
    ) { baseUrl, baseUrlHistory, dynamicColor, baseUrlFieldState ->
        if (!_isBaseUrlFieldInitiated) {
            _baseUrlFieldState.update { FieldState(baseUrl) }
            _isBaseUrlFieldInitiated = true
        }
        return@combine UiState.Success(
            baseUrlFieldState = baseUrlFieldState,
            baseUrlHistory = baseUrlHistory.toImmutableSet(),
            isDynamicColor = dynamicColor
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading
    )

    fun setBaseUrl(newBaseUrl: String) {
        if (!newBaseUrl.matches(BASE_URL_REGEX)) {
            _baseUrlFieldState.update { FieldState(newBaseUrl, R.string.base_url_invalid) }
        } else {
            _baseUrlFieldState.update { FieldState(newBaseUrl, null) }
        }
    }

    fun applyBaseUrl() = viewModelScope.launch(ioDispatcher) {
        val baseUrlFieldState = _baseUrlFieldState.value
        if (baseUrlFieldState.error == null) {
            appSettingsRepository.setBaseUrl(baseUrlFieldState.data)
            eventFlow.emit(Event.BaseUrlSaved)
        }
    }

    fun onBaseUrlRestoreDefault() {
        setBaseUrl(Constants.defaultBaseUrl)
    }

    fun setDynamicColor(dynamicColor: Boolean) = viewModelScope.launch(ioDispatcher) {
        appSettingsRepository.setDynamicColor(dynamicColor)
    }

    fun onRefreshWidgetsClicked() = workManager.enqueueUniqueWork(
        Constants.UPDATE_PHOTO_WIDGETS_SINGLE_WORK_NAME,
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<PhotoWidgetUpdateWorker>().build()
    )

    sealed class Event {
        data object BaseUrlSaved : Event()
    }

    sealed class UiState {
        data object Loading : UiState()
        data class Success(
            val baseUrlFieldState: FieldState<String>,
            val baseUrlHistory: ImmutableSet<String>,
            val isDynamicColor: Boolean,
        ) : UiState()
    }
}
