package com.julianczaja.esp_monitoring_app.presentation.adddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class AddDeviceDialogViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val deviceSettingsRepository: DeviceSettingsRepository,
) : ViewModel() {

    enum class Event {
        DEVICE_ADDED,
    }

    val eventFlow = MutableSharedFlow<Event>()

    private val _name = MutableStateFlow("")
    private val _nameError = MutableStateFlow<Int?>(null)
    val name = _name.asStateFlow()
    val nameError = _nameError.asStateFlow()

    private val _id = MutableStateFlow("")
    private val _idError = MutableStateFlow<Int?>(null)
    val id = _id.asStateFlow()
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

    fun addDevice() = viewModelScope.launch(Dispatchers.IO) {
        val device = createDevice()

        if (device != null) {
            deviceRepository.addNew(device)
            deviceSettingsRepository.saveDeviceSettingsLocal(DeviceSettings(deviceId = device.id))
            eventFlow.emit(Event.DEVICE_ADDED)
        } else {
            // TODO
        }
    }

    private fun createDevice(): Device? {
        if (nameError.value == null && idError.value == null) {
            try {
                val deviceId = id.value.toLong()
                val deviceWithGivenIdAlreadyExists = runBlocking { deviceRepository.doesDeviceWithGivenIdAlreadyExist(deviceId) }
                if (deviceWithGivenIdAlreadyExists) {
                    _idError.update { R.string.add_device_error_id_exists }
                    return null
                }

                val deviceWithGivenNameAlreadyExists = runBlocking { deviceRepository.doesDeviceWithGivenNameAlreadyExist(name.value) }
                if (deviceWithGivenNameAlreadyExists) {
                    _nameError.update { R.string.add_device_error_name_exists }
                    return null
                }
                return Device(deviceId, name.value)
            } catch (e: NumberFormatException) {
                return null
            }
        } else {
            return null
        }
    }
}
