package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraFrameSize
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraSpecialEffect
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraWhiteBalanceMode
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import com.juul.kable.AndroidAdvertisement
import timber.log.Timber
import kotlin.math.absoluteValue

@OptIn(ExperimentalPagerApi::class, ExperimentalPermissionsApi::class)
@Composable
fun PagerScope.DeviceSettingsScreen(viewModel: DeviceSettingsScreenViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val permissionsState = rememberMultiplePermissionsState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }
    ) {
        Timber.e("rememberMultiplePermissionsState RET: $it")
        if (!it.containsValue(false)) {
            viewModel.onPermissionsGranted()
        }
    }
    if (permissionsState.allPermissionsGranted) {
        viewModel.onPermissionsGranted() // FIXME
    }

    val uiState by viewModel.deviceSettingsUiState.collectAsStateWithLifecycle()

    DeviceSettingsScreenContent(
        uiState = uiState,
        permissionsState = permissionsState,
        onStartScanClicked = {
            when {
                isBluetoothEnabled(context) -> viewModel.startScanning()
                else -> promptEnableBluetooth(context)
            }
        },
        getTextToShowGivenPermissions = viewModel::getTextToShowGivenPermissions,
        onDeviceClicked = viewModel::connectDevice,
        onBrightnessSet = viewModel::onBrightnessSet
    )
}

@OptIn(ExperimentalPagerApi::class, ExperimentalPermissionsApi::class)
@Composable
private fun PagerScope.DeviceSettingsScreenContent(
    uiState: DeviceSettingsScreenUiState,
    permissionsState: MultiplePermissionsState,
    onStartScanClicked: () -> Unit,
    getTextToShowGivenPermissions: (List<PermissionState>, Boolean) -> String,
    onDeviceClicked: (String) -> Unit,
    onBrightnessSet: (Int) -> Unit,
) {
    when (uiState.deviceSettingsState) {
        is DeviceSettingsState.GrantPermissions -> DeviceSettingsGrantPermissionsScreen(
            permissionsState = permissionsState,
            getTextToShowGivenPermissions = getTextToShowGivenPermissions
        )

        is DeviceSettingsState.Scan -> DeviceSettingsScanScreen(
            uiState = uiState.deviceSettingsState,
            onStartScanClicked = onStartScanClicked,
            onDeviceClicked = onDeviceClicked
        )

        is DeviceSettingsState.Connect -> DeviceSettingsConnectScreen(
            uiState = uiState.deviceSettingsState,
            onBrightnessSet = onBrightnessSet
        )

        is DeviceSettingsState.Error -> DeviceSettingsErrorScreen(
            uiState.deviceSettingsState
        )
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DeviceSettingsGrantPermissionsScreen(
    permissionsState: MultiplePermissionsState,
    getTextToShowGivenPermissions: (List<PermissionState>, Boolean) -> String,
) {
    when {
        permissionsState.allPermissionsGranted -> {
            Text("PERMISSIONS GRANTED")
        }

        else -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(getTextToShowGivenPermissions(permissionsState.permissions, permissionsState.shouldShowRationale))
                Button(
                    onClick = { permissionsState.launchMultiplePermissionRequest() }
                ) {
                    Text("REQUEST PERMISSIONS")
                }
            }
        }
    }
}

@Composable
fun DeviceSettingsScanScreen(
    uiState: DeviceSettingsState.Scan,
    onStartScanClicked: () -> Unit,
    onDeviceClicked: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f)
                .padding(8.dp)
        ) {
            items(uiState.advertisements) {
                AdvertisementItem(it, onDeviceClicked)
            }
        }
        Column(
            Modifier.weight(0.2f)
        ) {
            Button(
                onClick = onStartScanClicked
            ) {
                Text(text = "Scan devices")
            }
            Text(text = "Scan Status: ${uiState.scanStatus}")
        }
    }
}

@Composable
fun AdvertisementItem(
    androidAdvertisement: AndroidAdvertisement,
    onDeviceClicked: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onDeviceClicked(androidAdvertisement.address) }
    ) {
        Text(
            text = androidAdvertisement.name ?: androidAdvertisement.identifier,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
        )
        Text(
            text = "RSSI: ${androidAdvertisement.rssi}",
            modifier = Modifier.padding(start = 8.dp, end = 8.dp)
        )
        Text(
            text = "Connectable: ${androidAdvertisement.isConnectable}",
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun DeviceSettingsConnectScreen(
    uiState: DeviceSettingsState.Connect,
    onBrightnessSet: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {

        // -   val deviceId: Long,
        // -   val name: String = "Default",
        // -   val frameSize: EspCameraFrameSize = EspCameraFrameSize.FrameSizeSVGA,
        // -   val jpegQuality: Int = 10,
        // -   val brightness: Int = 0, // -2 to 2
        // -   val contrast: Int = 0, // -2 to 2
        // -   val saturation: Int = 0, // -2 to 2
        // -   val flashOn: Boolean = false,
        // -   val specialEffect: EspCameraSpecialEffect = EspCameraSpecialEffect.NoEffect,
        // -   val whiteBalanceMode: EspCameraWhiteBalanceMode = EspCameraWhiteBalanceMode.Auto,
        // -   val verticalFlip: Boolean = false,
        // -   val horizontalMirror: Boolean = false,

        DefaultDropdownMenuBox(
            title = "Frame size",
            items = EspCameraFrameSize.values().map { it.description },
            selectedIndex = uiState.deviceSettings.frameSize.ordinal,
            onItemClicked = { EspCameraFrameSize.values()[it] }
        )
        DefaultDropdownMenuBox(
            title = "Special effect",
            items = EspCameraSpecialEffect.values().map { it.description },
            selectedIndex = uiState.deviceSettings.specialEffect.ordinal,
            onItemClicked = { EspCameraSpecialEffect.values()[it] }
        )
        DefaultDropdownMenuBox(
            title = "White balance mode",
            items = EspCameraWhiteBalanceMode.values().map { it.description },
            selectedIndex = uiState.deviceSettings.whiteBalanceMode.ordinal,
            onItemClicked = { EspCameraWhiteBalanceMode.values()[it] }
        )
        DefaultDropdownMenuBox(
            title = "White balance mode",
            items = EspCameraWhiteBalanceMode.values().map { it.description },
            selectedIndex = uiState.deviceSettings.whiteBalanceMode.ordinal,
            onItemClicked = { EspCameraWhiteBalanceMode.values()[it] }
        )
        DefaultDropdownMenuBox(
            title = "White balance mode",
            items = EspCameraWhiteBalanceMode.values().map { it.description },
            selectedIndex = uiState.deviceSettings.whiteBalanceMode.ordinal,
            onItemClicked = { EspCameraWhiteBalanceMode.values()[it] }
        )
        DefaultIntSliderRow(
            label = "Brightness",
            value = uiState.deviceSettings.brightness,
            steps = 3,
            valueRange = -2..2,
            onValueChange = { newValue ->
                onBrightnessSet(newValue)
            }
        )
        DefaultIntSliderRow(
            label = "Contrast",
            value = uiState.deviceSettings.contrast,
            steps = 3,
            valueRange = -2..2,
            onValueChange = { newValue -> }
        )
        DefaultIntSliderRow(
            label = "Saturation",
            value = uiState.deviceSettings.saturation,
            steps = 3,
            valueRange = -2..2,
            onValueChange = { newValue -> }
        )
        DefaultIntSliderRow(
            label = "Quality",
            value = uiState.deviceSettings.jpegQuality,
            steps = 10,
            valueRange = 10..63,
            onValueChange = { newValue -> }
        )
        SwitchWithLabel(
            label = "Flash LED",
            isChecked = uiState.deviceSettings.flashOn,
            onCheckedChange = { newValue -> },
            modifier = Modifier.fillMaxWidth(.7f)
        )
        SwitchWithLabel(
            label = "Vertical flip",
            isChecked = uiState.deviceSettings.flashOn,
            onCheckedChange = { newValue -> },
            modifier = Modifier.fillMaxWidth(.7f)
        )
        SwitchWithLabel(
            label = "Horizontal mirror",
            isChecked = uiState.deviceSettings.horizontalMirror,
            onCheckedChange = { newValue -> },
            modifier = Modifier.fillMaxWidth(.7f)
        )
    }
}


@Composable
private fun DeviceSettingsErrorScreen(
    uiState: DeviceSettingsState.Error,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(uiState.messageId),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DeviceSettingsLoadingScreen() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        CircularProgressIndicator()
    }
}

// --------------------------------------

@Composable
private fun SwitchWithLabel(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onClick = {
                    onCheckedChange(!isChecked)
                }
            )

    ) {
        Text(text = label)
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = {
                onCheckedChange(it)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultIntSliderRow(
    label: String,
    value: Int,
    steps: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
) {
    val interactionSource by remember { mutableStateOf(MutableInteractionSource()) }
    var tempValue by remember { mutableIntStateOf(value) }


    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(.7f)
    ) {
        Text(text = label)
        Slider(
            value = value.toFloat(),
            onValueChange = { tempValue = it.toInt() },
            onValueChangeFinished = { onValueChange(tempValue) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = steps,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary
                    ),
                    interactionSource = interactionSource,
                    enabled = true
                )
            },
            // thumb = { DefaultSliderThumbWithLabel(sliderPosition.toString(), it, interactionSource) },
            track = { sliderPositions ->
                SliderDefaults.Track(
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    sliderPositions = sliderPositions
                )
            },
            modifier = Modifier.fillMaxWidth(.7f)
        )
        Text(text = "$value")
    }
}

@Composable
fun DefaultSliderThumbWithLabel(
    labelText: String,
    sliderPositions: SliderPositions,
    interactionSource: MutableInteractionSource,
) {
    val isDragged by interactionSource.collectIsDraggedAsState()
    Timber.e("isDragged = $isDragged")

    SliderDefaults.Thumb(
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary
        ),
        interactionSource = interactionSource,
        enabled = true
    )
    if (isDragged) {
        Box(
            modifier = Modifier.offset(y = (-40).dp)
        ) {
            Text(
                text = labelText,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .offset(y = (-40).dp)
//                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultDropdownMenuBox(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onItemClicked: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = items[selectedIndex],
            onValueChange = { },
            label = { Text(title) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .heightIn(max = 250.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            items.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(text = label) },
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onSurface,
                        trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    onClick = {
                        expanded = false
                        onItemClicked(index)
                    },
                    trailingIcon = {
                        if (index == selectedIndex) {
                            Text("✕", textAlign = TextAlign.End)
                        }
                    }
                )
                Divider()
            }
        }
    }
}

private fun isBluetoothEnabled(context: Context): Boolean {
    val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter ?: throw (error("Device doesn't support Bluetooth"))

    return bluetoothAdapter.isEnabled
}

@SuppressLint("MissingPermission")
private fun promptEnableBluetooth(context: Context) {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    val activity = (context as ComponentActivity)

    activity.startActivity(enableBtIntent)
}
