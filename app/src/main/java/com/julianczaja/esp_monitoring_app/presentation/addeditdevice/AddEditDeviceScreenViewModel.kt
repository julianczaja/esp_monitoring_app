package com.julianczaja.esp_monitoring_app.presentation.addeditdevice

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


private const val DEVICE_NAME_MAX_LENGTH = 20

@HiltViewModel
class AddEditDeviceScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _deviceId = savedStateHandle.get<Long>(DeviceIdArgs.KEY) ?: DeviceIdArgs.NO_VALUE
    // private val _deviceId = savedStateHandle.toRoute<AddEditDeviceScreen>().deviceId // correct, but bugged in tests

    private var localDevice: Device? = null

    val mode = when (_deviceId) {
        DeviceIdArgs.NO_VALUE -> Mode.Add
        else -> Mode.Edit
    }

    val eventFlow = MutableSharedFlow<Event>()

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _nameError = MutableStateFlow<Int?>(null)
    val nameError = _nameError.asStateFlow()

    private val _id = MutableStateFlow("")
    val id = _id.asStateFlow()

    private val _idError = MutableStateFlow<Int?>(null)
    val idError = _idError.asStateFlow()

    val isIdEnabled = mode == Mode.Add

    fun init() = viewModelScope.launch(ioDispatcher) {
        localDevice = deviceRepository.getDeviceById(_deviceId).firstOrNull()
        localDevice?.let { localDevice ->
            _id.update { localDevice.id.toString() }
            _name.update { localDevice.name }
        }
    }

    fun updateName(newVal: String) {
        _name.update { newVal }

        if (newVal.isEmpty()) {
            _nameError.update { R.string.add_device_error_wrong_name }
        } else if (newVal.length > DEVICE_NAME_MAX_LENGTH) {
            _nameError.update { R.string.add_device_error_name_too_long }
        } else {
            _nameError.update { null }
        }
    }

    fun updateId(newId: String) {
        _id.update { newId }

        try {
            if (newId.toLong() < 0) {
                _idError.update { R.string.add_device_error_below_zero }
            } else {
                _idError.update { null }
            }
        } catch (e: NumberFormatException) {
            _idError.update { R.string.add_device_error_wrong_id }
        }
    }

    fun apply() = viewModelScope.launch(ioDispatcher) {
        when (mode) {
            Mode.Add -> addDevice()
            Mode.Edit -> updateDevice()
        }
    }

    private suspend fun addDevice() {
        if (nameError.value != null || idError.value != null) return // should never happen coz btn is locked when err

        val deviceId = id.value.toLong()
        var isError = false

        if (deviceRepository.doesDeviceWithGivenIdAlreadyExist(deviceId)) {
            _idError.update { R.string.add_device_error_id_exists }
            isError = true
        }

        if (deviceRepository.doesDeviceWithGivenNameAlreadyExist(name.value)) {
            _nameError.update { R.string.add_device_error_name_exists }
            isError = true
        }

        if (isError) return

        deviceRepository.addNew(Device(deviceId, name.value))
            .onFailure { eventFlow.emit(Event.ShowError(R.string.cant_add_device)) }
            .onSuccess { eventFlow.emit(Event.DeviceAdded) }
    }

    private suspend fun updateDevice() {
        if (nameError.value != null || idError.value != null) return // should never happen coz btn is locked when err

        if (name.value == localDevice?.name || deviceRepository.doesDeviceWithGivenNameAlreadyExist(name.value)) {
            _nameError.update { R.string.add_device_error_name_exists }
            return
        }

        localDevice?.copy(name = _name.value)?.let { device ->
            deviceRepository.update(device)
                .onFailure { eventFlow.emit(Event.ShowError(R.string.cant_update_device)) }
                .onSuccess { eventFlow.emit(Event.DeviceUpdated) }
        }
    }

    enum class Mode { Edit, Add }

    sealed class Event {
        data object DeviceAdded : Event()
        data object DeviceUpdated : Event()
        data class ShowError(@StringRes val messageId: Int) : Event()
    }
}
