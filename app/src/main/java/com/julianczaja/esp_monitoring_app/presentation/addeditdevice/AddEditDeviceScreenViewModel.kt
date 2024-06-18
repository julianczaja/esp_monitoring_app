package com.julianczaja.esp_monitoring_app.presentation.addeditdevice

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.navigation.AddEditDeviceScreen
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


private const val DEVICE_NAME_MAX_LENGTH = 20

@HiltViewModel
class AddEditDeviceScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _deviceId = savedStateHandle.toRoute<AddEditDeviceScreen>().deviceId

    private var localDevice: Device? = null

    val mode = when (_deviceId) {
        DeviceIdArgs.NO_VALUE -> Mode.Add
        else -> Mode.Edit
    }

    val eventFlow = MutableSharedFlow<Event>()

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _id = MutableStateFlow("")
    val id = _id.asStateFlow()

    val nameError: StateFlow<Int?> = _name
        .drop(1)
        .map(::validateName)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val idError: StateFlow<Int?> = _id
        .drop(1)
        .map(::validateId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
    }

    fun updateId(newId: String) {
        _id.update { newId }
    }

    fun apply() = viewModelScope.launch(ioDispatcher) {
        when (mode) {
            Mode.Add -> addDevice()
            Mode.Edit -> updateDevice()
        }
    }

    private suspend fun validateId(id: String): Int? {
        val newId = id.toLongOrNull() ?: return R.string.add_device_error_wrong_id

        return when {
            newId < 0 -> R.string.add_device_error_below_zero
            deviceRepository.doesDeviceWithGivenIdAlreadyExist(newId) -> R.string.add_device_error_id_exists
            else -> null
        }
    }

    private suspend fun validateName(name: String) = when {
        name.isEmpty() -> R.string.add_device_error_wrong_name
        name.length > DEVICE_NAME_MAX_LENGTH -> R.string.add_device_error_name_too_long
        name == localDevice?.name -> null
        deviceRepository.doesDeviceWithGivenNameAlreadyExist(name) -> R.string.add_device_error_name_exists
        else -> null
    }

    private suspend fun addDevice() {
        if (nameError.value != null || idError.value != null) return

        deviceRepository.addNew(Device(id.value.toLong(), name.value))
            .onFailure {
                Timber.e(it)
                eventFlow.emit(Event.ShowError(R.string.cant_add_device))
            }
            .onSuccess { eventFlow.emit(Event.DeviceAdded) }
    }

    private suspend fun updateDevice() {
        if (nameError.value != null || idError.value != null) return

        val updatedDevice = localDevice?.copy(name = _name.value)

        if (updatedDevice == null) {
            eventFlow.emit(Event.ShowError(R.string.cant_update_device))
            return
        }

        if (localDevice == updatedDevice) {
            eventFlow.emit(Event.DeviceUpdated)
            return
        }

        deviceRepository.update(updatedDevice)
            .onFailure {
                Timber.e(it)
                eventFlow.emit(Event.ShowError(R.string.cant_update_device))
            }
            .onSuccess { eventFlow.emit(Event.DeviceUpdated) }
    }

    enum class Mode { Edit, Add }

    sealed class Event {
        data object DeviceAdded : Event()
        data object DeviceUpdated : Event()
        data class ShowError(@StringRes val messageId: Int) : Event()
    }
}
