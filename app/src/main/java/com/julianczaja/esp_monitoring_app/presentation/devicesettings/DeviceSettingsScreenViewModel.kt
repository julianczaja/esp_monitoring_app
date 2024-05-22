package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.common.Constants.SCAN_DURATION_MILLIS
import com.julianczaja.esp_monitoring_app.data.BleLocationManager
import com.julianczaja.esp_monitoring_app.data.BluetoothManager
import com.julianczaja.esp_monitoring_app.di.IoDispatcher
import com.julianczaja.esp_monitoring_app.domain.MonitoringDevice
import com.julianczaja.esp_monitoring_app.domain.model.BleAdvertisement
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.julianczaja.esp_monitoring_app.domain.model.DeviceStatus
import com.julianczaja.esp_monitoring_app.domain.model.PermissionState
import com.julianczaja.esp_monitoring_app.domain.model.getErrorMessageId
import com.julianczaja.esp_monitoring_app.domain.model.toBleAdvertisement
import com.julianczaja.esp_monitoring_app.domain.model.toDeviceStatus
import com.julianczaja.esp_monitoring_app.navigation.DeviceScreen
import com.juul.kable.GattStatusException
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import com.juul.kable.peripheral
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class DeviceSettingsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    bluetoothManager: BluetoothManager,
    bleLocationManager: BleLocationManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val deviceId = savedStateHandle.toRoute<DeviceScreen>().deviceId // TODO: Use it to filter out proper device

    private val _uiState = MutableStateFlow<UiState>(UiState.CheckPermission)
    val uiState: StateFlow<UiState> = _uiState

    val isBluetoothEnabled = bluetoothManager.isBluetoothEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false
        )

    val isBleLocationEnabled = bleLocationManager.isLocationForBleEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false
        )

    private var scanJob: Job? = null
    private var _monitoringDevice: MonitoringDevice? = null

    private val _scanner by lazy {
        Scanner {
            logging { level = Logging.Level.Data }
        }
    }

    val eventFlow = MutableSharedFlow<Event>()

    fun updatePermissionsStatus(permissionsStates: List<PermissionState>) {
        Timber.d("updatePermissionsStatus: $permissionsStates")
        val allPermissionsGranted = permissionsStates.all { it == PermissionState.GRANTED }
        if (allPermissionsGranted) {
            if (_uiState.value is UiState.CheckPermission) {
                _uiState.update { UiState.Scan(false, emptyList()) }
            }
        } else {
            _uiState.update { UiState.CheckPermission }
        }
    }

    fun startScanning() {
        val state = _uiState.value as? UiState.Scan
        if (state == null || state.isScanning) {
            return
        }

        val advertisements = hashMapOf<String, BleAdvertisement>()

        fun getSortedAdvertisements() = advertisements.values
            .sortedWith(compareByDescending<BleAdvertisement> { it.isEspMonitoringDevice }.thenByDescending { it.rssi })
            .toList()

        scanJob = viewModelScope.launch(ioDispatcher) {
            withTimeoutOrNull(SCAN_DURATION_MILLIS) {
                _scanner
                    .advertisements
                    .onStart {
                        advertisements.clear()
                        _uiState.update {
                            UiState.Scan(isScanning = true, bleAdvertisements = getSortedAdvertisements())
                        }
                    }
                    .catch { throwable ->
                        Timber.e("BLE scan error: $throwable")
                        _uiState.update {
                            UiState.Scan(isScanning = false, bleAdvertisements = getSortedAdvertisements())
                        }
                        eventFlow.emit(Event.ShowError(R.string.unknown_error_message))
                    }
                    .onCompletion {
                        _uiState.update {
                            UiState.Scan(isScanning = false, bleAdvertisements = getSortedAdvertisements())
                        }
                    }
                    .collect { androidAdvertisement ->
                        advertisements[androidAdvertisement.address] = androidAdvertisement.toBleAdvertisement()
                        _uiState.update {
                            UiState.Scan(isScanning = true, bleAdvertisements = getSortedAdvertisements())
                        }
                    }
            }
        }
    }

    fun stopScan() = viewModelScope.launch(ioDispatcher) {
        scanJob?.cancelAndJoin()
    }

    fun connectDevice(address: String) {
        if (_uiState.value !is UiState.Scan) return
        stopScan()

        val peripheral = viewModelScope.peripheral(address) {
            logging {
                engine = SystemLogEngine
                level = Logging.Level.Warnings
                format = Logging.Format.Multiline
            }
        }

        val monitoringDevice = MonitoringDevice(peripheral)
        _monitoringDevice = monitoringDevice

        _uiState.update {
            UiState.Connect(
                deviceStatus = DeviceStatus.Disconnected,
                deviceSettings = monitoringDevice.deviceSettings.value,
                isBusy = false
            )
        }

        viewModelScope.launch(ioDispatcher) {
            combine(
                monitoringDevice.state,
                monitoringDevice.deviceSettings,
                monitoringDevice.isBusy
            ) { state, settings, isBusy ->
                _uiState.update {
                    UiState.Connect(
                        deviceStatus = state.toDeviceStatus(),
                        deviceSettings = settings,
                        isBusy = state == State.Connected && isBusy
                    )
                }
                if (state is State.Disconnected) {
                    delay(500)
                    _uiState.update { UiState.Scan(isScanning = false, bleAdvertisements = emptyList()) }
                }
            }.catch { throwable ->
                Timber.e("MonitoringDevice error: $throwable")
                eventFlow.emit(Event.ShowError(throwable.getErrorMessageId()))
                when (throwable) {
                    is NoSuchElementException -> Unit
                    else -> _uiState.update { UiState.Scan(isScanning = false, bleAdvertisements = emptyList()) }
                }
            }.collect()
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                with(monitoringDevice) {
                    connect()
                    init()
                }
            } catch (e: Exception) {
                Timber.e("Connection attempt failed: $e")
                eventFlow.emit(Event.ShowError(e.getErrorMessageId()))
            }
        }
    }

    fun disconnectDevice() = viewModelScope.launch(ioDispatcher) {
        _monitoringDevice?.apply { disconnect() } ?: run {
            Timber.e("disconnectDevice error: `_monitoringDevice` is null")
            _uiState.update { UiState.Scan(isScanning = false, bleAdvertisements = emptyList()) }
        }
    }

    fun updateDeviceSettings(deviceSettings: DeviceSettings) = viewModelScope.launch(ioDispatcher) {
        Timber.d("viewModel.updateDeviceSettings")
        _monitoringDevice?.let { device ->
            try {
                device.updateSettings(deviceSettings)
            } catch (e: GattStatusException) {
                Timber.e("updateDeviceSettings error: $e")
                device.disconnect()
                eventFlow.emit(Event.ShowError(e.getErrorMessageId()))
            } catch (e: Exception) {
                Timber.e("updateDeviceSettings error: $e")
                eventFlow.emit(Event.ShowError(e.getErrorMessageId()))
            }
        }
    }

    @Immutable
    sealed interface UiState {
        data object CheckPermission : UiState

        @Immutable
        data class Scan(val isScanning: Boolean, val bleAdvertisements: List<BleAdvertisement>) : UiState

        @Immutable
        data class Connect(
            val deviceStatus: DeviceStatus,
            val deviceSettings: DeviceSettings,
            val isBusy: Boolean
        ) : UiState
    }

    sealed class Event {
        data class ShowError(@StringRes val messageId: Int) : Event()
    }
}
