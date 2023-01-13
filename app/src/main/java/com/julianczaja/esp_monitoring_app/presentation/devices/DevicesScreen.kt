package com.julianczaja.esp_monitoring_app.presentation.devices

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.components.ErrorText
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


const val DEFAULT_DEVICE_ITEM_MIN_HEIGHT = 150

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun DevicesScreen(
    navigateToDevice: (Long) -> Unit,
    viewModel: DevicesScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.devicesUiState.collectAsStateWithLifecycle()
    DevicesScreenContent(uiState, navigateToDevice)
}

@Composable
fun DevicesScreenContent(
    uiState: DevicesScreenUiState,
    onDeviceClicked: (Long) -> Unit,
) {
    val configuration = LocalConfiguration.current

    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> DevicesScreenLandscape(uiState, onDeviceClicked)
        else -> DevicesScreenPortrait(uiState, onDeviceClicked)
    }
}

@Composable
private fun DevicesScreenPortrait(
    uiState: DevicesScreenUiState,
    onDeviceClicked: (Long) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
    ) {
        devicesScreenContent(uiState, onDeviceClicked)
    }
}

@Composable
private fun DevicesScreenLandscape(
    uiState: DevicesScreenUiState,
    onDeviceClicked: (Long) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(DEFAULT_DEVICE_ITEM_MIN_HEIGHT.dp),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
    ) {
        devicesScreenContent(uiState, onDeviceClicked)
    }
}

private fun LazyListScope.devicesScreenContent(
    uiState: DevicesScreenUiState,
    onDeviceClicked: (Long) -> Unit,
) {
    when (uiState) {
        DevicesScreenUiState.Loading -> {
            item {
                DefaultProgressIndicator()
            }
        }
        is DevicesScreenUiState.Success -> {
            items(uiState.devices, key = { it.id }) {
                DeviceItem(device = it, onClicked = onDeviceClicked)
            }
        }
        is DevicesScreenUiState.Error -> {
            item {
                ErrorText(text = stringResource(uiState.messageId))
            }
        }
    }
}

private fun LazyGridScope.devicesScreenContent(
    uiState: DevicesScreenUiState,
    onDeviceClicked: (Long) -> Unit,
) {
    when (uiState) {
        DevicesScreenUiState.Loading -> {
            item {
                DefaultProgressIndicator()
            }
        }
        is DevicesScreenUiState.Success -> {
            items(uiState.devices, key = { it.id }) {
                DeviceItem(device = it, onClicked = onDeviceClicked)
            }
        }
        is DevicesScreenUiState.Error -> {
            item {
                Text(text = stringResource(uiState.messageId))
            }
        }
    }
}

@Composable
private fun DeviceItem(
    modifier: Modifier = Modifier,
    device: Device,
    minHeight: Dp = DEFAULT_DEVICE_ITEM_MIN_HEIGHT.dp,
    onClicked: (Long) -> Unit,
) {
    Card(
        modifier = modifier
            .clickable(onClick = { onClicked(device.id) })
            .defaultMinSize(minHeight = minHeight)
            .testTag("DeviceItemCard"),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(id = R.string.device_id_label_with_format, device.id)
            )
        }
    }
}
