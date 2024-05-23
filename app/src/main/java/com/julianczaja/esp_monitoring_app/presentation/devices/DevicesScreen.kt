package com.julianczaja.esp_monitoring_app.presentation.devices

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.DEVICE_ITEM_MIN_WIDTH_DP
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.components.DeviceItem
import com.julianczaja.esp_monitoring_app.components.ErrorText
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.presentation.devices.DevicesScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing

private const val HEADER_HEIGHT_DP = 70

@Composable
fun DevicesScreen(
    navigateToAppSettings: () -> Unit,
    navigateToDevice: (Long) -> Unit,
    navigateToRemoveDevice: (Long) -> Unit,
    navigateToAddDevice: () -> Unit,
    navigateToEditDevice: (Device) -> Unit,
    viewModel: DevicesScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DevicesScreenContent(
        modifier = Modifier.fillMaxSize(),
        uiState = uiState,
        onDeviceClicked = navigateToDevice,
        onRemoveDeviceClicked = navigateToRemoveDevice,
        onAddDeviceClicked = navigateToAddDevice,
        onEditDeviceClicked = navigateToEditDevice,
        onAppSettingsClicked = navigateToAppSettings
    )
}

@Composable
fun DevicesScreenContent(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onDeviceClicked: (Long) -> Unit,
    onRemoveDeviceClicked: (Long) -> Unit,
    onAddDeviceClicked: () -> Unit,
    onEditDeviceClicked: (Device) -> Unit,
    onAppSettingsClicked: () -> Unit,
) {
    when (uiState) {
        is UiState.Success -> DevicesScreenSuccessContent(
            modifier = modifier,
            uiState = uiState,
            onDeviceClicked = onDeviceClicked,
            onRemoveDeviceClicked = onRemoveDeviceClicked,
            onAddDeviceClicked = onAddDeviceClicked,
            onEditDeviceClicked = onEditDeviceClicked,
            onAppSettingsClicked = onAppSettingsClicked
        )

        is UiState.Loading -> Box(modifier = modifier) {
            DefaultProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        is UiState.Error -> Box(modifier = modifier) {
            ErrorText(text = stringResource(uiState.messageId), modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun DevicesScreenSuccessContent(
    modifier: Modifier = Modifier,
    uiState: UiState.Success,
    onDeviceClicked: (Long) -> Unit,
    onRemoveDeviceClicked: (Long) -> Unit,
    onAddDeviceClicked: () -> Unit,
    onEditDeviceClicked: (Device) -> Unit,
    onAppSettingsClicked: () -> Unit,
) {
    val configuration = LocalConfiguration.current

    Column(modifier) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(HEADER_HEIGHT_DP.dp),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(MaterialTheme.spacing.medium)
        ) {
            item {
                CardButton(
                    labelId = R.string.add_new_device_label,
                    iconId = R.drawable.ic_baseline_add_24,
                    onClicked = onAddDeviceClicked
                )
            }
            item {
                CardButton(
                    labelId = R.string.open_settings_label,
                    iconId = R.drawable.ic_baseline_settings_24,
                    onClicked = onAppSettingsClicked
                )
            }
            item {
                CardButton(
                    labelId = R.string.app_name,
                    iconId = R.drawable.ic_devices,
                    onClicked = { }
                )
            }
        }

        HorizontalDivider()

        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> DevicesListLandscape(
                uiState = uiState,
                onDeviceClicked = onDeviceClicked,
                onRemoveDeviceClicked = onRemoveDeviceClicked,
                onEditDeviceClicked = onEditDeviceClicked
            )

            else -> DevicesListPortrait(
                uiState = uiState,
                onDeviceClicked = onDeviceClicked,
                onRemoveDeviceClicked = onRemoveDeviceClicked,
                onEditDeviceClicked = onEditDeviceClicked
            )
        }
    }
}

@Composable
fun CardButton(
    modifier: Modifier = Modifier,
    @StringRes labelId: Int,
    @DrawableRes iconId: Int,
    onClicked: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClicked)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(id = labelId), style = MaterialTheme.typography.labelLarge)
                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun DevicesListPortrait(
    uiState: UiState.Success,
    onDeviceClicked: (Long) -> Unit,
    onRemoveDeviceClicked: (Long) -> Unit,
    onEditDeviceClicked: (Device) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium)
    ) {
        items(uiState.devices, key = { it.id }) {
            DeviceItem(
                device = it,
                onClicked = onDeviceClicked,
                onRemoveClicked = onRemoveDeviceClicked,
                onEditClicked = onEditDeviceClicked
            )
        }
    }
}

@Composable
private fun DevicesListLandscape(
    uiState: UiState.Success,
    onDeviceClicked: (Long) -> Unit,
    onRemoveDeviceClicked: (Long) -> Unit,
    onEditDeviceClicked: (Device) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(DEVICE_ITEM_MIN_WIDTH_DP.dp),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium)
    ) {
        items(uiState.devices, key = { it.id }) {
            DeviceItem(
                device = it,
                onClicked = onDeviceClicked,
                onRemoveClicked = onRemoveDeviceClicked,
                onEditClicked = onEditDeviceClicked
            )
        }
    }
}


//region Preview
@PreviewLightDark
@Preview(device = "spec: width = 411dp, height = 891dp, orientation = landscape, dpi = 420", showSystemUi = true)
@Composable
private fun DevicesScreenSuccessPreview() {
    AppBackground {
        DevicesScreenContent(
            uiState = UiState.Success(
                listOf(
                    Device(1, "Device 1"),
                    Device(12, "Device 2"),
                    Device(123, "Device 3"),
                )
            ),
            onDeviceClicked = {},
            onRemoveDeviceClicked = {},
            onAddDeviceClicked = { },
            onEditDeviceClicked = {},
            onAppSettingsClicked = {}
        )
    }
}
//endregion
