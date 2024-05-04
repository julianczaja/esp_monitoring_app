package com.julianczaja.esp_monitoring_app.presentation.devices

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.components.ErrorText
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.presentation.devices.DevicesScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.AppTheme
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


const val DEFAULT_DEVICE_ITEM_MIN_HEIGHT = 150

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
    ) {
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
        devicesScreenContent(uiState, onDeviceClicked, onRemoveDeviceClicked, onEditDeviceClicked)
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
        columns = GridCells.Adaptive(DEFAULT_DEVICE_ITEM_MIN_HEIGHT.dp),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
    ) {
        devicesScreenContent(uiState, onDeviceClicked, onRemoveDeviceClicked, onEditDeviceClicked)
    }
}

private fun LazyListScope.devicesScreenContent(
    uiState: UiState,
    onDeviceClicked: (Long) -> Unit,
    onRemoveDeviceClicked: (Long) -> Unit,
    onEditDeviceClicked: (Device) -> Unit,
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

private fun LazyGridScope.devicesScreenContent(
    uiState: UiState,
    onDeviceClicked: (Long) -> Unit,
    onRemoveDeviceClicked: (Long) -> Unit,
    onEditDeviceClicked: (Device) -> Unit,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceItem(
    modifier: Modifier = Modifier,
    device: Device,
    minHeight: Dp = DEFAULT_DEVICE_ITEM_MIN_HEIGHT.dp,
    onClicked: (Long) -> Unit,
    onRemoveClicked: (Long) -> Unit,
    onEditClicked: (Device) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val menuItems = arrayOf(
        stringResource(R.string.remove_device_menu_item_remove) to { onRemoveClicked(device.id) },
        stringResource(R.string.edit) to { onEditClicked(device) }
    )
    Card(
        modifier = modifier
            .clickable(onClick = { onClicked(device.id) })
            .defaultMinSize(minHeight = minHeight)
            .testTag("DeviceItemCard"),
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.large)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = stringResource(id = R.string.device_id_label_with_format, device.id),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            ExposedDropdownMenuBox(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = !menuExpanded },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.menuAnchor()
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    menuItems.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(text = item.first) },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            onClick = {
                                menuExpanded = false
                                item.second.invoke()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(heightDp = 200)
@Composable
private fun DeviceItemPreview() {
    AppTheme {
        DeviceItem(
            device = Device(123L, "Device name"),
            onClicked = {},
            onRemoveClicked = {},
            onEditClicked = {}
        )
    }
}