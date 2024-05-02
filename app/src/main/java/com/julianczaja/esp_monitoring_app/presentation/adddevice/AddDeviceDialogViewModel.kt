package com.julianczaja.esp_monitoring_app.presentation.adddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddDeviceDialogViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    val eventFlow = MutableSharedFlow<Event>()

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _nameError = MutableStateFlow<Int?>(null)
    val nameError = _nameError.asStateFlow()

    private val _id = MutableStateFlow("")
    val id = _id.asStateFlow()

    private val _idError = MutableStateFlow<Int?>(null)
    val idError = _idError.asStateFlow()

    fun updateName(newVal: String) {
        _name.update { newVal }

        if (newVal.isEmpty()) {
            _nameError.update { R.string.add_device_error_wrong_name }
        } else {
            _nameError.update { null }
        }
    }

    fun updateId(newId: String) {
        _id.update { newId }

        try {
            newId.toLong()
            _idError.update { null }
        } catch (e: NumberFormatException) {
            _idError.update { R.string.add_device_error_wrong_id }
        }
    }

    fun addDevice() = viewModelScope.launch(ioDispatcher) {
        val device = createDeviceOrNull()

        if (device != null) {
            deviceRepository.addNew(device)
                .onFailure { Timber.e(it) } // TODO: Add error state and retry button in view that reset inputs
                .onSuccess { eventFlow.emit(Event.DEVICE_ADDED) }

        } else {
            Timber.e("Can't create device")  // TODO: Add error state and retry button in view that reset inputs
        }
    }

    private suspend fun createDeviceOrNull(): Device? {
        if (nameError.value != null || idError.value != null) return null

        val deviceId = try {
            id.value.toLong()
        } catch (e: NumberFormatException) {
            return null
        }

        if (deviceRepository.doesDeviceWithGivenIdAlreadyExist(deviceId)) {
            _idError.update { R.string.add_device_error_id_exists }
            return null
        }

        if (deviceRepository.doesDeviceWithGivenNameAlreadyExist(name.value)) {
            _nameError.update { R.string.add_device_error_name_exists }
            return null
        }

        return Device(deviceId, name.value)
    }

    enum class Event {
        DEVICE_ADDED,
    }
}
