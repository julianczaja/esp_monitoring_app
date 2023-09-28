package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.julianczaja.esp_monitoring_app.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.data.utils.combine
import com.julianczaja.esp_monitoring_app.domain.MonitoringDevice
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.julianczaja.esp_monitoring_app.domain.model.DeviceStatus
import com.julianczaja.esp_monitoring_app.domain.model.ScanStatus
import com.juul.kable.AndroidAdvertisement
import com.juul.kable.ConnectionLostException
import com.juul.kable.ConnectionRejectedException
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.logs.Hex
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import com.juul.kable.peripheral
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeviceSettingsScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        const val SCAN_DURATION_MILLIS = 5000L
    }

    private val deviceIdArgs: DeviceIdArgs = DeviceIdArgs(savedStateHandle)

    private val _apiError = MutableStateFlow<Int?>(null)

    private val _arePermissionsGranted = MutableStateFlow(false)

    // ---
    private val _scanStatus = MutableStateFlow<ScanStatus>(ScanStatus.Stopped)

    private val _deviceStatus = MutableStateFlow<DeviceStatus>(DeviceStatus.Disconnected)

    private val _advertisements = hashMapOf<String, AndroidAdvertisement>()

    private val _advertisementsSorted = MutableStateFlow<List<AndroidAdvertisement>>(emptyList())

    private var scanJob: Job? = null

    private val scanner by lazy {
        Scanner {
            logging { level = Logging.Level.Events }
        }
    }

    private var monitoringDevice: MonitoringDevice? = null
    // ---

    val deviceSettingsUiState: StateFlow<DeviceSettingsScreenUiState> = deviceSettingsUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeviceSettingsScreenUiState(DeviceSettingsState.GrantPermissions)
        )

    private fun deviceSettingsUiState(): Flow<DeviceSettingsScreenUiState> =
        combine(
            _apiError,
            _arePermissionsGranted,
            _scanStatus,
            _deviceStatus,
            _advertisementsSorted
        ) { apiError, arePermissionsGranted, scanStatus, deviceStatus, advertisementsSorted, ->
            val lastState = deviceSettingsUiState.value.deviceSettingsState

            Timber.i("NEW state lastState=${lastState.javaClass.simpleName}, monitoringDevice=$monitoringDevice, arePermissionsGranted=$arePermissionsGranted apiError=$apiError, scanStatus=$scanStatus, deviceStatus=$deviceStatus, advertisementsSorted=$advertisementsSorted")

            val deviceSettingsState = when {
                apiError != null -> {
                    DeviceSettingsState.Error(apiError)
                }

                lastState is DeviceSettingsState.GrantPermissions && arePermissionsGranted -> {
                    DeviceSettingsState.Scan(scanStatus, advertisementsSorted)
                }

                lastState is DeviceSettingsState.Scan && monitoringDevice != null -> {
                    DeviceSettingsState.Connect(deviceStatus, monitoringDevice!!.deviceSettings.value)
                }

                lastState is DeviceSettingsState.Connect && deviceStatus == DeviceStatus.Disconnected -> {
                    DeviceSettingsState.Scan(scanStatus, advertisementsSorted)
                }

                else -> {
                    Timber.e("Unknown state lastState=$lastState, arePermissionsGranted=$arePermissionsGranted apiError=$apiError, scanStatus=$scanStatus, deviceStatus=$deviceStatus, advertisementsSorted=$advertisementsSorted")
                    when (lastState) {
                        is DeviceSettingsState.GrantPermissions, is DeviceSettingsState.Error -> lastState
                        is DeviceSettingsState.Connect -> {
                            Timber.e("NEW SETTINGS: ${monitoringDevice!!.deviceSettings.value}")
                            DeviceSettingsState.Connect(deviceStatus, monitoringDevice!!.deviceSettings.value)
                        }

                        is DeviceSettingsState.Scan -> DeviceSettingsState.Scan(scanStatus, advertisementsSorted)
                    }
                }
            }

            DeviceSettingsScreenUiState(deviceSettingsState)
        }

    fun onPermissionsGranted() {
        Timber.e("onPermissionsGranted")
        _arePermissionsGranted.value = true
    }

    @OptIn(ExperimentalPermissionsApi::class)
    fun getTextToShowGivenPermissions(
        permissions: List<PermissionState>,
        shouldShowRationale: Boolean,
    ): String {
        val revokedPermissionsSize = permissions.size
        if (revokedPermissionsSize == 0) return ""

        val textToShow = StringBuilder().apply {
            append("The ")
        }

        for (i in permissions.indices) {
            textToShow.append(permissions[i].permission)
            when {
                revokedPermissionsSize > 1 && i == revokedPermissionsSize - 2 -> textToShow.append(", and ")
                i == revokedPermissionsSize - 1 -> textToShow.append(" ")
                else -> textToShow.append(", ")
            }
        }
        textToShow.append(if (revokedPermissionsSize == 1) "permission is" else "permissions are")
        textToShow.append(
            if (shouldShowRationale) {
                " important. Please grant all of them for the app to function properly."
            } else {
                " denied. The app cannot function without them."
            }
        )
        return textToShow.toString()
    }

    fun startScanning() {
        if (_scanStatus.value == ScanStatus.Scanning) return
        _scanStatus.value = ScanStatus.Scanning

        scanJob = viewModelScope.launch {
            withTimeoutOrNull(SCAN_DURATION_MILLIS) {
                scanner
                    .advertisements
                    .catch { cause -> _scanStatus.value = ScanStatus.Failed(cause.message ?: "Unknown error") }
                    .onCompletion { cause -> if (cause == null || cause is CancellationException) _scanStatus.value = ScanStatus.Stopped }
                    .collect { advertisement ->
                        _advertisements[advertisement.address] = advertisement
                        _advertisementsSorted.value = _advertisements.values.toList().sortedByDescending { it.isConnectable }
                    }
            }
        }
    }

    fun stopScanning() = viewModelScope.launch {
        scanJob?.cancelAndJoin()
    }

    fun connectDevice(address: String) {
        Timber.e("connectDevice address=$address")
        stopScanning()
        monitoringDevice = MonitoringDevice(
            deviceId = deviceIdArgs.deviceId,
            peripheral = viewModelScope.peripheral(address) {
                logging {
                    engine = SystemLogEngine
                    level = Logging.Level.Warnings
                    format = Logging.Format.Multiline
                    data = Hex
                }
            }
        )
        Timber.e("connectDevice monitoringDevice=$monitoringDevice")
        Timber.e("connectDevice monitoringDevice.services=${monitoringDevice?.services}")

        var lastStatus = State.Disconnected()

        viewModelScope.launch {
            viewModelScope.launch {
                monitoringDevice?.state?.collect {
                    Timber.d("NEW monitoringDevice STATUS: $it")
                    _deviceStatus.value = when (it) {
                        is State.Disconnected -> {
                            if (lastStatus !is State.Disconnected) {
                                Timber.e("SETTING monitoringDevice TO NULL !!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                                monitoringDevice = null
                            }
                            DeviceStatus.Disconnected
                        }

                        is State.Connecting -> DeviceStatus.Connecting
                        State.Connected -> {
                            monitoringDevice?.updateBrightness()
                            DeviceStatus.Connected
                        }

                        State.Disconnecting -> DeviceStatus.Disconnecting
                    }
                }
            }

            try {
                Timber.d("connect")
                monitoringDevice?.connect()
            } catch (e: ConnectionLostException) {
                Timber.w("Connection attempt failed (ConnectionLostException)")
            } catch (e: ConnectionRejectedException) {
                Timber.w("Connection attempt failed (ConnectionRejectedException)")
            } catch (e: Exception) {
                Timber.w("Connection attempt failed ($e)")
            }
        }
    }

    fun onBrightnessSet(newBrightness: Int) = viewModelScope.launch {
        monitoringDevice?.setBrightness(newBrightness)
    }
}

data class DeviceSettingsScreenUiState(
    val deviceSettingsState: DeviceSettingsState,
)

sealed interface DeviceSettingsState {
    data object GrantPermissions : DeviceSettingsState
    data class Scan(val scanStatus: ScanStatus, val advertisements: List<AndroidAdvertisement>) : DeviceSettingsState
    data class Connect(val deviceStatus: DeviceStatus, val deviceSettings: DeviceSettings) : DeviceSettingsState
    data class Error(@StringRes val messageId: Int) : DeviceSettingsState
}
