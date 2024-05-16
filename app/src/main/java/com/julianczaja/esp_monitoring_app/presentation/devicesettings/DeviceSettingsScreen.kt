package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderPositions
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.GrantPermissionButton
import com.julianczaja.esp_monitoring_app.components.PermissionRationaleDialog
import com.julianczaja.esp_monitoring_app.data.utils.getActivity
import com.julianczaja.esp_monitoring_app.data.utils.getBluetoothPermissionNameOrEmpty
import com.julianczaja.esp_monitoring_app.data.utils.getLocationPermissionName
import com.julianczaja.esp_monitoring_app.data.utils.getPermissionState
import com.julianczaja.esp_monitoring_app.data.utils.openAppSettings
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraFrameSize
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraSpecialEffect
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraWhiteBalanceMode
import com.julianczaja.esp_monitoring_app.domain.model.PermissionState
import com.julianczaja.esp_monitoring_app.presentation.devicesettings.DeviceSettingsScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import timber.log.Timber

@Composable
fun DeviceSettingsScreen(
    viewModel: DeviceSettingsScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val locationPermissionName = getLocationPermissionName()
    var locationPermissionState by rememberSaveable {
        mutableStateOf(
            context.getActivity().getPermissionState(locationPermissionName)
        )
    }

    val bluetoothPermissionName = getBluetoothPermissionNameOrEmpty()
    var bluetoothPermissionState by rememberSaveable {
        mutableStateOf(
            when (bluetoothPermissionName.isEmpty()) {
                true -> PermissionState.GRANTED
                false -> context.getActivity().getPermissionState(bluetoothPermissionName)
            }
        )
    }

    if (bluetoothPermissionState == PermissionState.GRANTED && locationPermissionState == PermissionState.GRANTED) {
        DeviceSettingsScreenContent(
            uiState = uiState
        )
    } else {
        PermissionsRequiredScreen(
            locationPermissionState = locationPermissionState,
            locationPermissionName = locationPermissionName,
            onLocationPermissionChanged = { locationPermissionState = it },
            bluetoothPermissionState = bluetoothPermissionState,
            bluetoothPermissionName = bluetoothPermissionName,
            onBluetoothPermissionChanged = { bluetoothPermissionState = it }
        )
    }
}

@Composable
private fun PermissionsRequiredScreen(
    modifier: Modifier = Modifier,
    locationPermissionState: PermissionState,
    locationPermissionName: String,
    onLocationPermissionChanged: (PermissionState) -> Unit,
    bluetoothPermissionState: PermissionState,
    bluetoothPermissionName: String,
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

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            when (isGranted) {
                true -> {
                    onBluetoothPermissionChanged(PermissionState.GRANTED)
                    shouldShowBluetoothPermissionRationaleDialog = false
                }

                false -> {
                    onBluetoothPermissionChanged(context.getActivity().getPermissionState(bluetoothPermissionName))
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
                    bluetoothPermissionLauncher.launch(bluetoothPermissionName)
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
            .padding(16.dp)
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
                onButtonClicked = { bluetoothPermissionLauncher.launch(bluetoothPermissionName) }
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

@Composable
private fun DeviceSettingsScreenContent(
    uiState: UiState,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState) {
            is UiState.Error -> DeviceSettingsErrorScreen(uiState.messageId)
            UiState.Loading -> DeviceSettingsLoadingScreen()
            is UiState.Success -> {

                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        MaterialTheme.spacing.medium,
                        Alignment.CenterVertically
                    ),
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
                        items = EspCameraFrameSize.entries.map { it.description },
                        selectedIndex = uiState.deviceSettings.frameSize.ordinal,
                        onItemClicked = { EspCameraFrameSize.entries[it] }
                    )
                    DefaultDropdownMenuBox(
                        title = "Special effect",
                        items = EspCameraSpecialEffect.entries.map { it.description },
                        selectedIndex = uiState.deviceSettings.specialEffect.ordinal,
                        onItemClicked = { EspCameraSpecialEffect.entries[it] }
                    )
                    DefaultDropdownMenuBox(
                        title = "White balance mode",
                        items = EspCameraWhiteBalanceMode.entries.map { it.description },
                        selectedIndex = uiState.deviceSettings.whiteBalanceMode.ordinal,
                        onItemClicked = { EspCameraWhiteBalanceMode.entries[it] }
                    )
                    DefaultDropdownMenuBox(
                        title = "White balance mode",
                        items = EspCameraWhiteBalanceMode.entries.map { it.description },
                        selectedIndex = uiState.deviceSettings.whiteBalanceMode.ordinal,
                        onItemClicked = { EspCameraWhiteBalanceMode.entries[it] }
                    )
                    DefaultDropdownMenuBox(
                        title = "White balance mode",
                        items = EspCameraWhiteBalanceMode.entries.map { it.description },
                        selectedIndex = uiState.deviceSettings.whiteBalanceMode.ordinal,
                        onItemClicked = { EspCameraWhiteBalanceMode.entries[it] }
                    )
                    DefaultIntSliderRow(
                        label = "Brightness",
                        value = uiState.deviceSettings.brightness,
                        steps = 3,
                        valueRange = -2..2,
                        onValueChange = { newValue -> }
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
        }
    }
}

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
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(.7f)
    ) {
        Text(text = label)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
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
//                SliderDefaults.Track( // FIXME
//                    colors = SliderDefaults.colors(
//                        activeTrackColor = MaterialTheme.colorScheme.primary
//                    ),
//                    sliderPositions = sliderPositions
//                )
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
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DeviceSettingsErrorScreen(@StringRes errorMessageId: Int) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(errorMessageId),
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

//region Preview
@Preview
@Composable
private fun PermissionsRequiredScreenTwoPermissionsPreview() {
    AppBackground {
        PermissionsRequiredScreen(
            locationPermissionName = "",
            locationPermissionState = PermissionState.DENIED,
            onLocationPermissionChanged = {},
            bluetoothPermissionName = "",
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
            bluetoothPermissionName = "",
            bluetoothPermissionState = PermissionState.DENIED,
            onBluetoothPermissionChanged = {}
        )
    }
}


@Preview
@Composable
private fun LocationPermissionRationaleDialogPreview() {
    LocationPermissionRationaleDialog(
        permissionState = PermissionState.RATIONALE_NEEDED,
        onRequestPermission = {},
        onDismiss = {}
    )
}

@Preview
@Composable
private fun LocationPermissionDeniedDialogPreview() {
    LocationPermissionRationaleDialog(
        permissionState = PermissionState.DENIED,
        onRequestPermission = {},
        onDismiss = {}
    )
}

@Preview
@Composable
private fun BluetoothPermissionRationaleDialogPreview() {
    BluetoothPermissionRationaleDialog(
        permissionState = PermissionState.RATIONALE_NEEDED,
        onRequestPermission = {},
        onDismiss = {}
    )
}

@Preview
@Composable
private fun BluetoothPermissionDeniedDialogPreview() {
    BluetoothPermissionRationaleDialog(
        permissionState = PermissionState.DENIED,
        onRequestPermission = {},
        onDismiss = {}
    )
}
//endregion
