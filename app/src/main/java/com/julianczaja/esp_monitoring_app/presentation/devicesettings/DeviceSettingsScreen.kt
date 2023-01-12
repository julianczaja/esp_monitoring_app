package com.julianczaja.esp_monitoring_app.presentation.devicesettings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerScope
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraFrameSize
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraSpecialEffect
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraWhiteBalanceMode
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import timber.log.Timber

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PagerScope.DeviceSettingsScreen(
    viewModel: DeviceSettingsScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.deviceSettingsUiState.collectAsStateWithLifecycle()
    DeviceSettingsScreenContent(uiState, viewModel::updateDeviceSettings)
}

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PagerScope.DeviceSettingsScreenContent(
    uiState: DeviceSettingsScreenUiState,
    updateSettings: () -> Unit,
) {
    val pullRefreshState = rememberPullRefreshState(uiState.isRefreshing, updateSettings)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        when (uiState.deviceSettingsState) {
            is DeviceSettingsState.Error -> DeviceSettingsErrorScreen(uiState.deviceSettingsState.messageId)
            DeviceSettingsState.Loading -> DeviceSettingsLoadingScreen()
            is DeviceSettingsState.Success -> {

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
                        selectedIndex = uiState.deviceSettingsState.deviceSettings.frameSize.ordinal,
                        onItemClicked = { EspCameraFrameSize.values()[it] }
                    )
                    DefaultDropdownMenuBox(
                        title = "Special effect",
                        items = EspCameraSpecialEffect.values().map { it.description },
                        selectedIndex = uiState.deviceSettingsState.deviceSettings.specialEffect.ordinal,
                        onItemClicked = { EspCameraSpecialEffect.values()[it] }
                    )
                    DefaultDropdownMenuBox(
                        title = "White balance mode",
                        items = EspCameraWhiteBalanceMode.values().map { it.description },
                        selectedIndex = uiState.deviceSettingsState.deviceSettings.whiteBalanceMode.ordinal,
                        onItemClicked = { EspCameraWhiteBalanceMode.values()[it] }
                    )
                    DefaultDropdownMenuBox(
                        title = "White balance mode",
                        items = EspCameraWhiteBalanceMode.values().map { it.description },
                        selectedIndex = uiState.deviceSettingsState.deviceSettings.whiteBalanceMode.ordinal,
                        onItemClicked = { EspCameraWhiteBalanceMode.values()[it] }
                    )
                    DefaultDropdownMenuBox(
                        title = "White balance mode",
                        items = EspCameraWhiteBalanceMode.values().map { it.description },
                        selectedIndex = uiState.deviceSettingsState.deviceSettings.whiteBalanceMode.ordinal,
                        onItemClicked = { EspCameraWhiteBalanceMode.values()[it] }
                    )
                    DefaultIntSliderRow(
                        label = "Brightness",
                        value = uiState.deviceSettingsState.deviceSettings.brightness,
                        steps = 3,
                        valueRange = -2..2,
                        onValueChange = { newValue -> }
                    )
                    DefaultIntSliderRow(
                        label = "Contrast",
                        value = uiState.deviceSettingsState.deviceSettings.contrast,
                        steps = 3,
                        valueRange = -2..2,
                        onValueChange = { newValue -> }
                    )
                    DefaultIntSliderRow(
                        label = "Saturation",
                        value = uiState.deviceSettingsState.deviceSettings.saturation,
                        steps = 3,
                        valueRange = -2..2,
                        onValueChange = { newValue -> }
                    )
                    DefaultIntSliderRow(
                        label = "Quality",
                        value = uiState.deviceSettingsState.deviceSettings.jpegQuality,
                        steps = 10,
                        valueRange = 10..63,
                        onValueChange = { newValue -> }
                    )
                    SwitchWithLabel(
                        label = "Flash LED",
                        isChecked = uiState.deviceSettingsState.deviceSettings.flashOn,
                        onCheckedChange = { newValue -> },
                        modifier = Modifier.fillMaxWidth(.7f)
                    )
                    SwitchWithLabel(
                        label = "Vertical flip",
                        isChecked = uiState.deviceSettingsState.deviceSettings.flashOn,
                        onCheckedChange = { newValue -> },
                        modifier = Modifier.fillMaxWidth(.7f)
                    )
                    SwitchWithLabel(
                        label = "Horizontal mirror",
                        isChecked = uiState.deviceSettingsState.deviceSettings.horizontalMirror,
                        onCheckedChange = { newValue -> },
                        modifier = Modifier.fillMaxWidth(.7f)
                    )

                }

            }
        }
        PullRefreshIndicator(uiState.isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
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

@OptIn(ExperimentalMaterial3Api::class)
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