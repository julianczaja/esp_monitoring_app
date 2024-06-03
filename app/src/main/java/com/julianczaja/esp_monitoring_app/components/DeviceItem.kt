package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing

const val DEVICE_ITEM_MIN_HEIGHT_DP = 100
const val DEVICE_ITEM_MIN_WIDTH_DP = 300

@Composable
fun DeviceItem(
    modifier: Modifier = Modifier,
    device: Device,
    minHeight: Dp = DEVICE_ITEM_MIN_HEIGHT_DP.dp,
    minWidth: Dp = DEVICE_ITEM_MIN_WIDTH_DP.dp,
    onClicked: (Long) -> Unit,
    onRemoveClicked: (Long) -> Unit,
    onEditClicked: (Device) -> Unit,
) {
    Card(
        modifier = modifier
            .clickable(onClick = { onClicked(device.id) })
            .defaultMinSize(minWidth, minHeight)
            .testTag("DeviceItemCard"),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier
                        .size(75.dp)
                        .padding(start = MaterialTheme.spacing.medium),
                    painter = painterResource(id = R.drawable.ic_devices),
                    contentDescription = null
                )
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(.8f)
                        .padding(MaterialTheme.spacing.medium)
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.headlineSmall,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(id = R.string.device_id_label_with_format, device.id),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "ORDER = ${device.order}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            MoreMenuButton(device, onRemoveClicked, onEditClicked)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.MoreMenuButton(
    device: Device,
    onRemoveClicked: (Long) -> Unit,
    onEditClicked: (Device) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val items = listOf(
        stringResource(R.string.remove_device_menu_item_remove) to { onRemoveClicked(device.id) },
        stringResource(R.string.edit_label) to { onEditClicked(device) }
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.align(Alignment.TopEnd)
    ) {
        IconButton(
            onClick = {},
            modifier = Modifier.menuAnchor()
        ) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
        }
        DefaultDropdownMenu(
            isExpanded = expanded,
            items = items.map { it.first },
            onItemClicked = {
                expanded = false
                items[it].second.invoke()
            },
            onDismiss = { expanded = false }
        )
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun DeviceItemPreview() {
    AppBackground(Modifier.height((DEVICE_ITEM_MIN_HEIGHT_DP).dp)) {
        DeviceItem(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            device = Device(123L, "Device name", 1),
            onClicked = {},
            onRemoveClicked = {},
            onEditClicked = {}
        )
    }
}

@Preview
@Composable
private fun DeviceItemLongNamePreview() {
    AppBackground(Modifier.height((DEVICE_ITEM_MIN_HEIGHT_DP).dp)) {
        DeviceItem(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            device = Device(123L, "Device very long name", 1),
            onClicked = {},
            onRemoveClicked = {},
            onEditClicked = {}
        )
    }
}
//endregion
