package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.julianczaja.esp_monitoring_app.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.data.utils.combine
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.julianczaja.esp_monitoring_app.domain.model.DeviceStatus
import com.julianczaja.esp_monitoring_app.domain.model.ScanStatus
import com.juul.kable.AndroidAdvertisement
import com.juul.kable.ConnectionLostException
import com.juul.kable.ConnectionRejectedException
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.characteristicOf
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

        const val INFO_SERVICE_UUID = "691ff8e2-b4d3-4c2e-b72f-95b6e5acd64b"
        const val SETTINGS_SERVICE_UUID = "ecb44d46-93cf-45cc-bd34-b82205e80d7b"
        const val DEVICE_ID_CHARACTERISTIC_UUID = "17a4e7f7-f645-4f67-a618-98037cb4372a"
        const val FRAME_SIZE_CHARACTERISTIC_UUID = "2c0980bd-efc9-49e2-8043-ad94bf4bf81e"
        const val QUALITY_CHARACTERISTIC_UUID = "06135106-f60d-4d46-858d-b8988f33aafa"
        const val BRIGHTNESS_CHARACTERISTIC_UUID = "1d7e8059-f231-44f2-be7c-9cb51855c30b"
    }

    private val deviceIdArgs: DeviceIdArgs = DeviceIdArgs(savedStateHandle)

    private val _apiError = MutableStateFlow<Int?>(null)

    private val _isRefreshing = MutableStateFlow(false)

    private val _arePermissionsGranted = MutableStateFlow(false)

    // ---
    private val _scanStatus = MutableStateFlow<ScanStatus>(ScanStatus.Stopped)

    private val _deviceStatus = MutableStateFlow<DeviceStatus>(DeviceStatus.Disconnected)

    private val _advertisements = hashMapOf<String, AndroidAdvertisement>()

    private val _advertisementsSorted = MutableStateFlow<List<AndroidAdvertisement>>(emptyList())

    private var peripheral: Peripheral? = null

    private var isPeripheralConnected: Boolean = false

    private val characteristic = characteristicOf(
        service = SETTINGS_SERVICE_UUID,
        characteristic = BRIGHTNESS_CHARACTERISTIC_UUID,
    )

    private var scanJob: Job? = null


    private val scanner by lazy {
        Scanner {
            logging { level = Logging.Level.Events }
        }
    }
    // ---

    val deviceSettingsUiState: StateFlow<DeviceSettingsScreenUiState> = deviceSettingsUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeviceSettingsScreenUiState(DeviceSettingsState.Init, true)
        )

    private fun deviceSettingsUiState(): Flow<DeviceSettingsScreenUiState> =
        combine(
            _apiError,
            _isRefreshing,
            _arePermissionsGranted,
            _scanStatus,
            _deviceStatus,
            _advertisementsSorted
        ) { apiError, isRefreshing, arePermissionsGranted, scanStatus, deviceStatus, advertisementsSorted ->
            val lastState = deviceSettingsUiState.value.deviceSettingsState

            val deviceSettingsState = when {
                apiError != null -> DeviceSettingsState.Error(apiError)
                !arePermissionsGranted -> DeviceSettingsState.Init
                deviceStatus == DeviceStatus.Disconnected -> DeviceSettingsState.Init
                scanStatus == ScanStatus.else -> {
                    Timber.e("Unknown state apiError=$apiError, scanStatus=$scanStatus, deviceStatus=$deviceStatus, advertisements=$advertisements")
                    DeviceSettingsState.Loading
                }
            }

            DeviceSettingsScreenUiState(deviceSettingsState, isRefreshing)
        }

    fun setArePermissionsGranted(allPermissionsGranted: Boolean) {
        _arePermissionsGranted.value = allPermissionsGranted
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
        peripheral = viewModelScope.peripheral(address) {
            logging {
                engine = SystemLogEngine
                level = Logging.Level.Warnings
                format = Logging.Format.Multiline
                data = Hex
            }
        }
        Timber.e("connectDevice peripheral=$peripheral")
        Timber.e("connectDevice peripheral.services=${peripheral?.services}")

        viewModelScope.launch {
            viewModelScope.launch {
                peripheral?.state?.collect {
                    Timber.d("NEW PERIPHERIAL STATUS: $it")
                    isPeripheralConnected = it is State.Connected
                }
            }

            try {
                Timber.d("connect")
                peripheral!!.connect()
            } catch (e: ConnectionLostException) {
                Timber.w("Connection attempt failed (ConnectionLostException)")
            } catch (e: ConnectionRejectedException) {
                Timber.w("Connection attempt failed (ConnectionRejectedException)")
            } catch (e: Exception) {
                Timber.w("Connection attempt failed ($e)")
            }
        }
    }

    fun updateDeviceSettings() {
        Timber.e("updateDeviceSettings")
    }
}

// 1. permissions checking
// 2. enable bluetooth
// 3. scan for devices
// 4. connect to device
// 5. connected (data transfer) (on disconnect back to "scan for devices")

data class DeviceSettingsScreenUiState(
    val deviceSettingsState: DeviceSettingsState,
    val isRefreshing: Boolean,
)

sealed interface DeviceSettingsState {
    data object Init : DeviceSettingsState
    data class Disconnected(val scanStatus: ScanStatus, val advertisements: List<AndroidAdvertisement>) : DeviceSettingsState
    data class Connected(val deviceStatus: DeviceStatus, val deviceSettings: DeviceSettings) : DeviceSettingsState
    data class Error(@StringRes val messageId: Int) : DeviceSettingsState
    data object Loading : DeviceSettingsState
}
