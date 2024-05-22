package com.julianczaja.esp_monitoring_app.presentation.devices

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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



@Composable
fun DevicesScreen(
    navigateToDevice: (Long) -> Unit,
    navigateToRemoveDevice: (Long) -> Unit,
    navigateToAddDevice: () -> Unit,
    navigateToEditDevice: (Device) -> Unit,
    viewModel: DevicesScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DevicesScreenContent(
        uiState = uiState,
        onDeviceClicked = navigateToDevice,
        onRemoveDeviceClicked = navigateToRemoveDevice,
        onAddDeviceClicked = navigateToAddDevice,
        onEditDeviceClicked = navigateToEditDevice
    )
}

@Composable
fun DevicesScreenContent(
    uiState: UiState,
    onDeviceClicked: (Long) -> Unit,
    onRemoveDeviceClicked: (Long) -> Unit,
    onAddDeviceClicked: () -> Unit,
    onEditDeviceClicked: (Device) -> Unit,
) {
    val configuration = LocalConfiguration.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDeviceClicked,
                modifier = Modifier.safeDrawingPadding()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_add_24),
                    contentDescription = null
                )
            }
        }
    ) { _ ->
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> DevicesScreenLandscape(
                uiState = uiState,
                onDeviceClicked = onDeviceClicked,
                onRemoveDeviceClicked = onRemoveDeviceClicked,
                onEditDeviceClicked = onEditDeviceClicked
            )

            else -> DevicesScreenPortrait(
                uiState = uiState,
                onDeviceClicked = onDeviceClicked,
                onRemoveDeviceClicked = onRemoveDeviceClicked,
                onEditDeviceClicked = onEditDeviceClicked
            )
        }
    }
}

@Composable
private fun DevicesScreenPortrait(
    uiState: UiState,
    onDeviceClicked: (Long) -> Unit,
    onRemoveDeviceClicked: (Long) -> Unit,
    onEditDeviceClicked: (Device) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
    ) {
        when (uiState) {
            UiState.Loading -> {
                item {
                    DefaultProgressIndicator()
                }
            }

            is UiState.Success -> {
                items(uiState.devices, key = { it.id }) {
                    DeviceItem(
                        device = it,
                        onClicked = onDeviceClicked,
                        onRemoveClicked = onRemoveDeviceClicked,
                        onEditClicked = onEditDeviceClicked
                    )
                }
            }

            is UiState.Error -> {
                item {
                    ErrorText(text = stringResource(uiState.messageId))
                }
            }
        }
    }
}

@Composable
private fun DevicesScreenLandscape(
    uiState: UiState,
    onDeviceClicked: (Long) -> Unit,
    onRemoveDeviceClicked: (Long) -> Unit,
    onEditDeviceClicked: (Device) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(DEVICE_ITEM_MIN_WIDTH_DP.dp),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
    ) {
        when (uiState) {
            UiState.Loading -> {
                item {
                    DefaultProgressIndicator()
                }
            }

            is UiState.Success -> {
                items(uiState.devices, key = { it.id }) {
                    DeviceItem(
                        device = it,
                        onClicked = onDeviceClicked,
                        onRemoveClicked = onRemoveDeviceClicked,
                        onEditClicked = onEditDeviceClicked
                    )
                }
            }

            is UiState.Error -> {
                item {
                    Text(text = stringResource(uiState.messageId))
                }
            }
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
            onAddDeviceClicked = { }
        ) {

        }
    }
}
//endregion
