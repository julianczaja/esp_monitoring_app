package com.julianczaja.esp_monitoring_app.presentation.appsettings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.common.Constants
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.AppSettings
import com.julianczaja.esp_monitoring_app.domain.model.FieldState
import com.julianczaja.esp_monitoring_app.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        private val BASE_URL_REGEX = Regex("""^\s*(https?://[a-z0-9_.]+:\d{4}/?)""")
    }

    private val _baseUrlFieldState = MutableStateFlow(FieldState(""))
    private var _isBaseUrlFieldInitiated = false
    private var _lastSavedBaseUrl = ""

    val eventFlow = MutableSharedFlow<Event>()

    val uiState = combine(
        appSettingsRepository.getAppSettings(),
        _baseUrlFieldState
    ) { appSettings, baseUrlFieldState ->
        _lastSavedBaseUrl = appSettings.baseUrl
        return@combine if (!_isBaseUrlFieldInitiated) {
            UiState.Success(
                appSettings = appSettings,
                baseUrlFieldValue = appSettings.baseUrl, // on start set data from datastore
                baseUrlFieldError = null
            ).also { _isBaseUrlFieldInitiated = true }
        } else {
            UiState.Success(
                appSettings = appSettings,
                baseUrlFieldValue = baseUrlFieldState.data,
                baseUrlFieldError = baseUrlFieldState.error
            )
        }
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
        if (baseUrlFieldState.data != _lastSavedBaseUrl && baseUrlFieldState.error == null) {
            appSettingsRepository.setBaseUrl(baseUrlFieldState.data)
            eventFlow.emit(Event.BaseUrlSaved)
        }
    }

    fun onBaseUrlRestoreDefault() {
        setBaseUrl(Constants.defaultBaseUrl)
    }

    sealed class Event {
        data object BaseUrlSaved : Event()
    }

    sealed class UiState {
        data object Loading : UiState()
        data class Success(
            val appSettings: AppSettings,
            val baseUrlFieldValue: String,
            @StringRes val baseUrlFieldError: Int?,
        ) : UiState()
    }
}
