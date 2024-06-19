package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
import com.julianczaja.esp_monitoring_app.domain.model.WifiCredentials
import com.julianczaja.esp_monitoring_app.presentation.devicesettings.DeviceSettingsScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.devicesettings.DeviceSettingsScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.devicesettings.components.ServerInfoEditDialog
import com.julianczaja.esp_monitoring_app.presentation.devicesettings.components.WiFiCredentialsEditDialog
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing

@Composable
fun DeviceSettingsScreen(
    onSetAppBarTitle: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: DeviceSettingsScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
    val isBleLocationEnabled by viewModel.isBleLocationEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = uiState) {
        if (uiState is UiState.Connect) {
            onSetAppBarTitle(R.string.device_settings_connect_screen_title)
        } else {
            onSetAppBarTitle(R.string.device_settings_scan_screen_title)
        }
    }

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
        onWifiCredentialsChanged = viewModel::updateDeviceWifiCredentials,
        onDeviceServerUrlChanged = viewModel::updateDeviceServerUrl,
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
    onWifiCredentialsChanged: (WifiCredentials) -> Unit,
    onDeviceServerUrlChanged: (String) -> Unit,
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
            onDisconnectClicked = onDisconnectClicked,
            onWifiCredentialsChanged = onWifiCredentialsChanged,
            onDeviceServerUrlChanged = onDeviceServerUrlChanged
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
        modifier = modifier.padding(MaterialTheme.spacing.large)
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
private fun DeviceSettingsScanScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Scan,
    isBluetoothEnabled: Boolean,
    isBleLocationEnabled: Boolean,
    onStartScanClicked: () -> Unit,
    onStopScanClicked: () -> Unit,
    onDeviceClicked: (String) -> Unit
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val isFabExpanded by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ScanFloatingActionButton(
                isScanning = uiState.isScanning,
                isExpanded = isFabExpanded,
                onStartScanClicked = onStartScanClicked,
                onStopScanClicked = onStopScanClicked
            )

        }
    ) { padding ->
        Column(modifier.padding(paddingValues = padding)) {
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
            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.Top),
                contentPadding = PaddingValues(MaterialTheme.spacing.medium)
            ) {
                items(uiState.bleAdvertisements) {
                    AdvertisementItem(it, onDeviceClicked)
                }
            }
        }
    }
}

@Composable
private fun ScanFloatingActionButton(
    isScanning: Boolean,
    isExpanded: Boolean,
    onStartScanClicked: () -> Unit,
    onStopScanClicked: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = if (isScanning) onStopScanClicked else onStartScanClicked,
        icon = {
            Icon(
                painter = when (isScanning) {
                    true ->  painterResource(id = R.drawable.ic_x)
                    false -> painterResource(id = R.drawable.ic_search)
                },
                contentDescription = null,
            )
        },
        text = {
            Text(
                text = when (isScanning) {
                    true -> stringResource(R.string.bluetooth_stop_scan_label)
                    false -> stringResource(R.string.bluetooth_scan_label)
                }
            )
        },
        expanded = isExpanded
    )
}
//endregion

//region Connect
@Composable
private fun DeviceSettingsConnectScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Connect,
    onDeviceSettingsChanged: (DeviceSettings) -> Unit,
    onWifiCredentialsChanged: (WifiCredentials) -> Unit,
    onDeviceServerUrlChanged: (String) -> Unit,
    onDisconnectClicked: () -> Unit
) {
    when (uiState.deviceStatus) {
        DeviceStatus.Connected -> {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DeviceSettingsContent(
                    deviceSettings = uiState.deviceSettings,
                    enabled = !uiState.isBusy,
                    onSettingsChanged = onDeviceSettingsChanged,
                    onWifiCredentialsChanged = onWifiCredentialsChanged,
                    onDeviceServerUrlChanged = onDeviceServerUrlChanged,
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

        else -> {
            Box(modifier) {
                Text(
                    text = stringResource(id = uiState.deviceStatus.stringId),
                    modifier = Modifier
                        .padding(MaterialTheme.spacing.medium)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun DeviceSettingsContent(
    deviceSettings: DeviceSettings,
    enabled: Boolean,
    onSettingsChanged: (DeviceSettings) -> Unit,
    onWifiCredentialsChanged: (WifiCredentials) -> Unit,
    onDeviceServerUrlChanged: (String) -> Unit,
) {
    var isWifiCredentialsDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isServerInfoDialogVisible by rememberSaveable { mutableStateOf(false) }

    if (isWifiCredentialsDialogVisible) {
        WiFiCredentialsEditDialog(
            initialSsid = deviceSettings.wifiSsid,
            onDismiss = { isWifiCredentialsDialogVisible = false },
            onApply = { ssid, password ->
                onWifiCredentialsChanged(WifiCredentials(ssid, password))
                isWifiCredentialsDialogVisible = false
            }
        )
    }

    if (isServerInfoDialogVisible) {
        ServerInfoEditDialog(
            initialUrl = deviceSettings.serverUrl,
            onDismiss = { isServerInfoDialogVisible = false },
            onApply = { url ->
                onDeviceServerUrlChanged(url)
                isServerInfoDialogVisible = false
            }
        )
    }

    Text(text = stringResource(R.string.device_id_label_with_format, deviceSettings.deviceId))
    HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))

    OutlinedCardWithEditButton(
        title = stringResource(R.string.wifi_credentials_label),
        enabled = enabled,
        onEditClicked = { isWifiCredentialsDialogVisible = true }
    ) {
        Text(text = stringResource(R.string.ssid_label) + ": ${deviceSettings.wifiSsid}")
        Text(text = stringResource(R.string.password_label_with_placeholder))
    }
    OutlinedCardWithEditButton(
        title = stringResource(R.string.server_info_label),
        enabled = enabled,
        onEditClicked = { isServerInfoDialogVisible = true }
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(.7f),
            text = stringResource(R.string.url_label) + ": ${deviceSettings.serverUrl}",
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
    DropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.device_settings_frame_size_label),
        items = EspCameraFrameSize.entries.map { it.description },
        selectedIndex = deviceSettings.frameSize.ordinal,
        enabled = enabled,
        onItemClicked = { onSettingsChanged(deviceSettings.copy(frameSize = EspCameraFrameSize.entries[it])) }
    )
    DropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.device_settings_photo_interval_label),
        items = EspCameraPhotoInterval.entries.map { it.description },
        selectedIndex = deviceSettings.photoInterval.ordinal,
        enabled = enabled,
        onItemClicked = { onSettingsChanged(deviceSettings.copy(photoInterval = EspCameraPhotoInterval.entries[it])) }
    )
    DropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.device_settings_special_effect_label),
        items = EspCameraSpecialEffect.entries.map { stringResource(id = it.descriptionResId) },
        selectedIndex = deviceSettings.specialEffect.ordinal,
        enabled = enabled,
        onItemClicked = { onSettingsChanged(deviceSettings.copy(specialEffect = EspCameraSpecialEffect.entries[it])) }
    )
    DropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.device_settings_white_balance_mode_label),
        items = EspCameraWhiteBalanceMode.entries.map { stringResource(id = it.descriptionResId) },
        selectedIndex = deviceSettings.whiteBalanceMode.ordinal,
        enabled = enabled,
        onItemClicked = { onSettingsChanged(deviceSettings.copy(whiteBalanceMode = EspCameraWhiteBalanceMode.entries[it])) }
    )
    IntSliderRow(
        label = stringResource(R.string.device_settings_quality_label),
        value = deviceSettings.jpegQuality,
        steps = 54,
        enabled = enabled,
        valueRange = 10f..63f,
        onValueChange = { newValue -> onSettingsChanged(deviceSettings.copy(jpegQuality = newValue)) }
    )
    IntSliderRow(
        label = stringResource(R.string.device_settings_brightness_label),
        value = deviceSettings.brightness,
        steps = 3,
        enabled = enabled,
        valueRange = -2f..2f,
        onValueChange = { newValue -> onSettingsChanged(deviceSettings.copy(brightness = newValue)) }
    )
    IntSliderRow(
        label = stringResource(R.string.device_settings_contrast_label),
        value = deviceSettings.contrast,
        steps = 3,
        enabled = enabled,
        valueRange = -2f..2f,
        onValueChange = { newValue -> onSettingsChanged(deviceSettings.copy(contrast = newValue)) }
    )
    IntSliderRow(
        label = stringResource(R.string.device_settings_saturation_label),
        value = deviceSettings.saturation,
        steps = 3,
        enabled = enabled,
        valueRange = -2f..2f,
        onValueChange = { newValue -> onSettingsChanged(deviceSettings.copy(saturation = newValue)) }
    )
    SwitchWithLabel(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.device_settings_flash_led_label),
        isChecked = deviceSettings.flashOn,
        enabled = enabled,
        onCheckedChange = { newValue -> onSettingsChanged(deviceSettings.copy(flashOn = newValue)) }
    )
    SwitchWithLabel(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.device_settings_vertical_flip_label),
        isChecked = deviceSettings.verticalFlip,
        enabled = enabled,
        onCheckedChange = { newValue -> onSettingsChanged(deviceSettings.copy(verticalFlip = newValue)) }
    )
    SwitchWithLabel(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.device_settings_horizontal_mirror_label),
        isChecked = deviceSettings.horizontalMirror,
        enabled = enabled,
        onCheckedChange = { newValue -> onSettingsChanged(deviceSettings.copy(horizontalMirror = newValue)) }
    )
}

@Composable
private fun OutlinedCardWithEditButton(
    modifier: Modifier = Modifier,
    title: String,
    enabled: Boolean,
    onEditClicked: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit)
) {
    OutlinedCard {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline
                )
                Spacer(modifier = Modifier.padding(MaterialTheme.spacing.small))
                content()
            }
            TextButton(
                onClick = onEditClicked,
                enabled = enabled
            ) {
                Text(text = stringResource(id = R.string.edit_label))
            }
        }
    }
}

//endregion

//region Preview
@PreviewLightDark
@Composable
private fun DeviceSettingsConnectScreenConnectedPreview() {
    AppBackground {
        DeviceSettingsConnectScreen(
            uiState = UiState.Connect(
                deviceStatus = DeviceStatus.Connected,
                deviceSettings = DeviceSettings(serverUrl = "http://192.168.1.1:1234/"),
                isBusy = false
            ),
            onDeviceSettingsChanged = {},
            onDisconnectClicked = {},
            onWifiCredentialsChanged = {},
            onDeviceServerUrlChanged = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceSettingsConnectScreenConnectingPreview() {
    AppBackground(
        modifier = Modifier.size(300.dp)
    ) {
        DeviceSettingsConnectScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = UiState.Connect(
                deviceStatus = DeviceStatus.Connecting,
                deviceSettings = DeviceSettings(),
                isBusy = true
            ),
            onDeviceSettingsChanged = {},
            onDisconnectClicked = {},
            onWifiCredentialsChanged = {},
            onDeviceServerUrlChanged = {}
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
