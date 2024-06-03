package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AdvertisementItem
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.DropdownMenuBox
import com.julianczaja.esp_monitoring_app.components.GrantPermissionButton
import com.julianczaja.esp_monitoring_app.components.IntSliderRow
import com.julianczaja.esp_monitoring_app.components.PermissionRationaleDialog
import com.julianczaja.esp_monitoring_app.components.StateBar
import com.julianczaja.esp_monitoring_app.components.SwitchWithLabel
import com.julianczaja.esp_monitoring_app.data.utils.getActivity
import com.julianczaja.esp_monitoring_app.data.utils.getBluetoothPermissionsNamesOrEmpty
import com.julianczaja.esp_monitoring_app.data.utils.getLocationPermissionNameOrEmpty
import com.julianczaja.esp_monitoring_app.data.utils.getPermissionState
import com.julianczaja.esp_monitoring_app.data.utils.getPermissionsState
import com.julianczaja.esp_monitoring_app.data.utils.isBluetoothEnabled
import com.julianczaja.esp_monitoring_app.data.utils.openAppSettings
import com.julianczaja.esp_monitoring_app.data.utils.promptEnableBluetooth
import com.julianczaja.esp_monitoring_app.data.utils.promptEnableLocation
import com.julianczaja.esp_monitoring_app.domain.model.BleAdvertisement
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.julianczaja.esp_monitoring_app.domain.model.DeviceStatus
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraFrameSize
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraPhotoInterval
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraSpecialEffect
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraWhiteBalanceMode
import com.julianczaja.esp_monitoring_app.domain.model.PermissionState
import com.julianczaja.esp_monitoring_app.presentation.devicesettings.DeviceSettingsScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.devicesettings.DeviceSettingsScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing

@Composable
fun DeviceSettingsScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: DeviceSettingsScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
    val isBleLocationEnabled by viewModel.isBleLocationEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is Event.ShowError -> snackbarHostState.showSnackbar(
                    message = context.getString(event.messageId),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val locationPermissionName = getLocationPermissionNameOrEmpty()
    var locationPermissionState by rememberSaveable {
        mutableStateOf(
            when (locationPermissionName.isEmpty()) {
                true -> PermissionState.GRANTED
                false -> context.getActivity().getPermissionState(locationPermissionName)
            }
        )
    }

    val bluetoothPermissionsNames = getBluetoothPermissionsNamesOrEmpty()
    var bluetoothPermissionState by rememberSaveable {
        mutableStateOf(
            when (bluetoothPermissionsNames.isEmpty()) {
                true -> PermissionState.GRANTED
                false -> context.getActivity().getPermissionsState(bluetoothPermissionsNames)
            }
        )
    }

    LaunchedEffect(locationPermissionState, bluetoothPermissionState) {
        viewModel.updatePermissionsStatus(listOf(locationPermissionState, bluetoothPermissionState))
    }

    BackHandler(enabled = uiState is UiState.Connect) {
        viewModel.disconnectDevice()
    }

    DeviceSettingsScreenContent(
        modifier = Modifier.fillMaxSize(),
        uiState = uiState,
        isBluetoothEnabled = isBluetoothEnabled,
        isBleLocationEnabled = isBleLocationEnabled,
        locationPermissionState = locationPermissionState,
        locationPermissionName = locationPermissionName,
        onLocationPermissionChanged = { locationPermissionState = it },
        bluetoothPermissionState = bluetoothPermissionState,
        bluetoothPermissionsNames = bluetoothPermissionsNames,
        onBluetoothPermissionChanged = { bluetoothPermissionState = it },
        onStartScanClicked = {
            when {
                context.isBluetoothEnabled() -> viewModel.startScanning()
                else -> context.getActivity().promptEnableBluetooth()
            }
        },
        onStopScanClicked = viewModel::stopScan,
        onDeviceClicked = viewModel::connectDevice,
        onDeviceSettingsChanged = viewModel::updateDeviceSettings,
        onDisconnectClicked = viewModel::disconnectDevice
    )
}

@Composable
private fun DeviceSettingsScreenContent(
    modifier: Modifier = Modifier,
    uiState: UiState,
    isBluetoothEnabled: Boolean,
    isBleLocationEnabled: Boolean,
    locationPermissionState: PermissionState,
    locationPermissionName: String,
    onLocationPermissionChanged: (PermissionState) -> Unit,
    bluetoothPermissionState: PermissionState,
    bluetoothPermissionsNames: Array<String>,
    onBluetoothPermissionChanged: (PermissionState) -> Unit,
    onStartScanClicked: () -> Unit,
    onStopScanClicked: () -> Unit,
    onDeviceClicked: (String) -> Unit,
    onDeviceSettingsChanged: (DeviceSettings) -> Unit,
    onDisconnectClicked: () -> Unit,
) {
    when (uiState) {
        is UiState.CheckPermission -> PermissionsRequiredScreen(
            modifier = modifier,
            locationPermissionState = locationPermissionState,
            locationPermissionName = locationPermissionName,
            onLocationPermissionChanged = onLocationPermissionChanged,
            bluetoothPermissionState = bluetoothPermissionState,
            bluetoothPermissionsNames = bluetoothPermissionsNames,
            onBluetoothPermissionChanged = onBluetoothPermissionChanged
        )

        is UiState.Scan -> DeviceSettingsScanScreen(
            modifier = modifier,
            uiState = uiState,
            isBluetoothEnabled = isBluetoothEnabled,
            isBleLocationEnabled = isBleLocationEnabled,
            onStartScanClicked = onStartScanClicked,
            onStopScanClicked = onStopScanClicked,
            onDeviceClicked = onDeviceClicked
        )

        is UiState.Connect -> DeviceSettingsConnectScreen(
            modifier = modifier,
            uiState = uiState,
            onDeviceSettingsChanged = onDeviceSettingsChanged,
            onDisconnectClicked = onDisconnectClicked
        )
    }
}

// region Permissions
@Composable
private fun PermissionsRequiredScreen(
    modifier: Modifier = Modifier,
    locationPermissionState: PermissionState,
    locationPermissionName: String,
    onLocationPermissionChanged: (PermissionState) -> Unit,
    bluetoothPermissionState: PermissionState,
    bluetoothPermissionsNames: Array<String>,
    onBluetoothPermissionChanged: (PermissionState) -> Unit,
) {
    val context = LocalContext.current
    var shouldShowLocationPermissionRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var shouldShowBluetoothPermissionRationaleDialog by rememberSaveable { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            when (isGranted) {
                true -> {
                    onLocationPermissionChanged(PermissionState.GRANTED)
                    shouldShowLocationPermissionRationaleDialog = false
                }

                false -> {
                    onLocationPermissionChanged(context.getActivity().getPermissionState(locationPermissionName))
                    shouldShowLocationPermissionRationaleDialog = true
                }
            }
        }
    )

    val bluetoothPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { map ->
            val allGranted = map.values.all { it }
            when (allGranted) {
                true -> {
                    onBluetoothPermissionChanged(PermissionState.GRANTED)
                    shouldShowBluetoothPermissionRationaleDialog = false
                }

                false -> {
                    val notGrantedName = map.entries.first { !it.value }.key
                    onBluetoothPermissionChanged(context.getActivity().getPermissionState(notGrantedName))
                    shouldShowBluetoothPermissionRationaleDialog = true
                }
            }
        }
    )

    if (shouldShowLocationPermissionRationaleDialog) {
        LocationPermissionRationaleDialog(
            permissionState = locationPermissionState,
            onRequestPermission = {
                if (locationPermissionState == PermissionState.RATIONALE_NEEDED) {
                    locationPermissionLauncher.launch(locationPermissionName)
                } else {
                    context.getActivity().openAppSettings()
                }
            },
            onDismiss = { shouldShowLocationPermissionRationaleDialog = false }
        )
    }

    if (shouldShowBluetoothPermissionRationaleDialog) {
        BluetoothPermissionRationaleDialog(
            permissionState = bluetoothPermissionState,
            onRequestPermission = {
                if (bluetoothPermissionState == PermissionState.RATIONALE_NEEDED) {
                    bluetoothPermissionsLauncher.launch(bluetoothPermissionsNames)
                } else {
                    context.getActivity().openAppSettings()
                }
            },
            onDismiss = { shouldShowBluetoothPermissionRationaleDialog = false }
        )
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.large)
    ) {
        if (locationPermissionState != PermissionState.GRANTED) {
            GrantPermissionButton(
                titleId = R.string.location_permission_needed_title,
                onButtonClicked = { locationPermissionLauncher.launch(locationPermissionName) }
            )
        }
        if (locationPermissionState != PermissionState.GRANTED && bluetoothPermissionState != PermissionState.GRANTED) {
            HorizontalDivider()
        }
        if (bluetoothPermissionState != PermissionState.GRANTED) {
            GrantPermissionButton(
                titleId = R.string.bluetooth_permission_needed_title,
                onButtonClicked = { bluetoothPermissionsLauncher.launch(bluetoothPermissionsNames) }
            )
        }
    }
}

@Composable
private fun LocationPermissionRationaleDialog(
    permissionState: PermissionState,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionRationaleDialog(
        title = R.string.location_permission_needed_title,
        bodyRationale = R.string.location_permission_rationale_body,
        bodyDenied = R.string.location_permission_denied_body,
        permissionState = permissionState,
        onRequestPermission = onRequestPermission,
        onDismiss = onDismiss
    )
}

@Composable
private fun BluetoothPermissionRationaleDialog(
    permissionState: PermissionState,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionRationaleDialog(
        title = R.string.bluetooth_permission_needed_title,
        bodyRationale = R.string.bluetooth_permission_rationale_body,
        bodyDenied = R.string.bluetooth_permission_denied_body,
        permissionState = permissionState,
        onRequestPermission = onRequestPermission,
        onDismiss = onDismiss
    )
}
//endregion

//region Scan
@Composable
fun DeviceSettingsScanScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Scan,
    isBluetoothEnabled: Boolean,
    isBleLocationEnabled: Boolean,
    onStartScanClicked: () -> Unit,
    onStopScanClicked: () -> Unit,
    onDeviceClicked: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StateBar(
            isVisible = !isBluetoothEnabled,
            title = R.string.bluetooth_disabled_label,
            onButtonClicked = { context.getActivity().promptEnableBluetooth() }
        )
        StateBar(
            isVisible = !isBleLocationEnabled,
            title = R.string.location_disabled_label,
            onButtonClicked = { context.getActivity().promptEnableLocation() }
        )

        AnimatedVisibility(visible = uiState.isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(.5f),
            enabled = isBluetoothEnabled && isBleLocationEnabled,
            onClick = if (uiState.isScanning) onStopScanClicked else onStartScanClicked
        ) {
            Text(
                text = if (uiState.isScanning) stringResource(R.string.bluetooth_stop_scan_label)
                else stringResource(R.string.bluetooth_scan_label)
            )
        }
        HorizontalDivider()
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.Top),
            contentPadding = PaddingValues(MaterialTheme.spacing.medium)
        ) {
            items(uiState.bleAdvertisements) {
                AdvertisementItem(it, onDeviceClicked)
            }
        }
    }
}
//endregion

//region Connect
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSettingsConnectScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Connect,
    onDeviceSettingsChanged: (DeviceSettings) -> Unit,
    onDisconnectClicked: () -> Unit
) {
    val pullRefreshState = rememberPullToRefreshState(enabled = { false })

    LaunchedEffect(uiState.isBusy) {
        if (uiState.isBusy) {
            pullRefreshState.startRefresh()
        } else {
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = uiState.deviceStatus.stringId),
                modifier = Modifier.padding(top = MaterialTheme.spacing.small)
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = MaterialTheme.spacing.small)
            )
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.deviceStatus == DeviceStatus.Connected) {
                    DeviceSettingsContent(
                        deviceSettings = uiState.deviceSettings,
                        enabled = !uiState.isBusy,
                        onChanged = onDeviceSettingsChanged
                    )
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(.5f)
                            .padding(top = MaterialTheme.spacing.small),
                        onClick = onDisconnectClicked
                    ) {
                        Text(text = stringResource(id = R.string.disconnect_label))
                    }
                }
            }
        }
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullRefreshState,
        )
    }
}

@Composable
fun DeviceSettingsContent(
    deviceSettings: DeviceSettings,
    enabled: Boolean,
    onChanged: (DeviceSettings) -> Unit
) {
    DropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.device_settings_frame_size_label),
        items = EspCameraFrameSize.entries.map { it.description },
        selectedIndex = deviceSettings.frameSize.ordinal,
        enabled = enabled,
        onItemClicked = { onChanged(deviceSettings.copy(frameSize = EspCameraFrameSize.entries[it])) }
    )
    DropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.device_settings_photo_interval_label),
        items = EspCameraPhotoInterval.entries.map { it.description },
        selectedIndex = deviceSettings.photoInterval.ordinal,
        enabled = enabled,
        onItemClicked = { onChanged(deviceSettings.copy(photoInterval = EspCameraPhotoInterval.entries[it])) }
    )
    DropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.device_settings_special_effect_label),
        items = EspCameraSpecialEffect.entries.map { stringResource(id = it.descriptionResId) },
        selectedIndex = deviceSettings.specialEffect.ordinal,
        enabled = enabled,
        onItemClicked = { onChanged(deviceSettings.copy(specialEffect = EspCameraSpecialEffect.entries[it])) }
    )
    DropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.device_settings_white_balance_mode_label),
        items = EspCameraWhiteBalanceMode.entries.map { stringResource(id = it.descriptionResId) },
        selectedIndex = deviceSettings.whiteBalanceMode.ordinal,
        enabled = enabled,
        onItemClicked = { onChanged(deviceSettings.copy(whiteBalanceMode = EspCameraWhiteBalanceMode.entries[it])) }
    )
    IntSliderRow(
        label = stringResource(R.string.device_settings_quality_label),
        value = deviceSettings.jpegQuality,
        steps = 54,
        enabled = enabled,
        valueRange = 10f..63f,
        onValueChange = { newValue -> onChanged(deviceSettings.copy(jpegQuality = newValue)) }
    )
    IntSliderRow(
        label = stringResource(R.string.device_settings_brightness_label),
        value = deviceSettings.brightness,
        steps = 3,
        enabled = enabled,
        valueRange = -2f..2f,
        onValueChange = { newValue -> onChanged(deviceSettings.copy(brightness = newValue)) }
    )
    IntSliderRow(
        label = stringResource(R.string.device_settings_contrast_label),
        value = deviceSettings.contrast,
        steps = 3,
        enabled = enabled,
        valueRange = -2f..2f,
        onValueChange = { newValue -> onChanged(deviceSettings.copy(contrast = newValue)) }
    )
    IntSliderRow(
        label = stringResource(R.string.device_settings_saturation_label),
        value = deviceSettings.saturation,
        steps = 3,
        enabled = enabled,
        valueRange = -2f..2f,
        onValueChange = { newValue -> onChanged(deviceSettings.copy(saturation = newValue)) }
    )
    SwitchWithLabel(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.device_settings_flash_led_label),
        isChecked = deviceSettings.flashOn,
        enabled = enabled,
        onCheckedChange = { newValue -> onChanged(deviceSettings.copy(flashOn = newValue)) }
    )
    SwitchWithLabel(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.device_settings_vertical_flip_label),
        isChecked = deviceSettings.verticalFlip,
        enabled = enabled,
        onCheckedChange = { newValue -> onChanged(deviceSettings.copy(verticalFlip = newValue)) }
    )
    SwitchWithLabel(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.device_settings_horizontal_mirror_label),
        isChecked = deviceSettings.horizontalMirror,
        enabled = enabled,
        onCheckedChange = { newValue -> onChanged(deviceSettings.copy(horizontalMirror = newValue)) }
    )
}
//endregion

//region Preview
@PreviewLightDark
@Composable
private fun DeviceSettingsConnectScreenPreview() {
    AppBackground {
        DeviceSettingsConnectScreen(
            modifier = Modifier,
            uiState = UiState.Connect(
                deviceStatus = DeviceStatus.Connected,
                deviceSettings = DeviceSettings(),
                isBusy = false
            ),
            onDeviceSettingsChanged = {},
            onDisconnectClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceSettingsScanScreenPreview() {
    AppBackground {
        DeviceSettingsScanScreen(
            uiState = UiState.Scan(
                isScanning = true,
                bleAdvertisements = listOf(
                    BleAdvertisement("ESP Monitoring device", "address", true, true, -50),
                    BleAdvertisement("Name123", "address", false, true, -70),
                    BleAdvertisement("Name321", "address", false, false, -100),
                )
            ),
            isBluetoothEnabled = true,
            isBleLocationEnabled = true,
            onStopScanClicked = {},
            onStartScanClicked = {},
            onDeviceClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceSettingsScanScreenStatusBarsPreview() {
    AppBackground {
        DeviceSettingsScanScreen(
            uiState = UiState.Scan(
                isScanning = true,
                bleAdvertisements = emptyList()
            ),
            isBluetoothEnabled = false,
            isBleLocationEnabled = false,
            onStopScanClicked = {},
            onStartScanClicked = {},
            onDeviceClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun PermissionsRequiredScreenTwoPermissionsPreview() {
    AppBackground {
        PermissionsRequiredScreen(
            locationPermissionName = "",
            locationPermissionState = PermissionState.DENIED,
            onLocationPermissionChanged = {},
            bluetoothPermissionsNames = emptyArray(),
            bluetoothPermissionState = PermissionState.DENIED,
            onBluetoothPermissionChanged = {}
        )
    }
}

@Preview
@Composable
private fun PermissionsRequiredScreenOnePermissionPreview() {
    AppBackground {
        PermissionsRequiredScreen(
            locationPermissionName = "",
            locationPermissionState = PermissionState.GRANTED,
            onLocationPermissionChanged = {},
            bluetoothPermissionsNames = emptyArray(),
            bluetoothPermissionState = PermissionState.DENIED,
            onBluetoothPermissionChanged = {}
        )
    }
}
//endregion
