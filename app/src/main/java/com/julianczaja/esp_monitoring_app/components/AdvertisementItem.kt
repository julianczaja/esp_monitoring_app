package com.julianczaja.esp_monitoring_app.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.domain.model.BleAdvertisement
import com.julianczaja.esp_monitoring_app.presentation.theme.color_green_connectable
import com.julianczaja.esp_monitoring_app.presentation.theme.color_red_not_connectable
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


private const val ADVERTISEMENT_ICON_SIZE_DP = 50

@Composable
fun AdvertisementItem(
    bleAdvertisement: BleAdvertisement,
    onDeviceClicked: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = bleAdvertisement.isEspMonitoringDevice,
                onClick = { onDeviceClicked(bleAdvertisement.address) }
            ),
        border = if (bleAdvertisement.isEspMonitoringDevice) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = getDeviceDrawableId(bleAdvertisement)),
                modifier = Modifier.size(ADVERTISEMENT_ICON_SIZE_DP.dp),
                contentDescription = null
            )
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = bleAdvertisement.name
                        ?: stringResource(R.string.advertisement_name_unknown, bleAdvertisement.address),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (bleAdvertisement.isEspMonitoringDevice) FontWeight.Bold else FontWeight.Normal,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.small))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = getReceptionIcon(bleAdvertisement.rssi)),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.rssi_dbm, bleAdvertisement.rssi),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = getConnectableDrawableId(bleAdvertisement.isConnectable)),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(id = getConnectableStringId(bleAdvertisement.isConnectable)),
                        color = getConnectableTextColor(bleAdvertisement.isConnectable),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun getDeviceDrawableId(advertisement: BleAdvertisement) = when {
    advertisement.isEspMonitoringDevice -> R.drawable.ic_devices
    advertisement.isConnectable -> R.drawable.ic_devices_question
    else -> R.drawable.ic_devices_x
}

private fun getConnectableDrawableId(isConnectable: Boolean) = when (isConnectable) {
    true -> R.drawable.ic_plug
    false -> R.drawable.ic_plug_x
}

private fun getConnectableTextColor(isConnectable: Boolean) = when (isConnectable) {
    true -> color_green_connectable
    false -> color_red_not_connectable
}

private fun getConnectableStringId(isConnectable: Boolean) = when (isConnectable) {
    true -> R.string.connectable_label
    false -> R.string.not_connectable_label
}

private fun getReceptionIcon(rssi: Int) = when {
    rssi > -60 -> R.drawable.ic_antenna_bars_5
    rssi in -60 downTo -75 -> R.drawable.ic_antenna_bars_4
    rssi in -75 downTo -85 -> R.drawable.ic_antenna_bars_3
    rssi in -85 downTo -95 -> R.drawable.ic_antenna_bars_2
    else -> R.drawable.ic_antenna_bars_1
}

//region Preview
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun AdvertisementItemGoodRSSIPreview() {
    AppBackground {
        AdvertisementItem(
            bleAdvertisement = BleAdvertisement(
                name = "Name",
                address = "11:22:33:AA:BB:CC",
                isEspMonitoringDevice = true,
                isConnectable = true,
                rssi = -50
            ),
            onDeviceClicked = {}
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun AdvertisementItemNotConnectablePreview() {
    AppBackground {
        AdvertisementItem(
            bleAdvertisement = BleAdvertisement(
                name = "Name",
                address = "11:22:33:AA:BB:CC",
                isEspMonitoringDevice = false,
                isConnectable = false,
                rssi = -75
            ),
            onDeviceClicked = {}
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun AdvertisementItemLowRSSIPreview() {
    AppBackground {
        AdvertisementItem(
            bleAdvertisement = BleAdvertisement(
                name = "Name",
                address = "11:22:33:AA:BB:CC",
                isEspMonitoringDevice = false,
                isConnectable = true,
                rssi = -105
            ),
            onDeviceClicked = {}
        )
    }
}

@Preview
@Composable
private fun AdvertisementItemUnknownNamePreview() {
    AppBackground {
        AdvertisementItem(
            bleAdvertisement = BleAdvertisement(
                name = null,
                address = "11:22:33:AA:BB:CC",
                isEspMonitoringDevice = true,
                isConnectable = true,
                rssi = -95
            ),
            onDeviceClicked = {}
        )
    }
}

@Preview
@Composable
private fun AdvertisementItemLongNamePreview() {
    AppBackground {
        AdvertisementItem(
            bleAdvertisement = BleAdvertisement(
                name = "Long nameeeeeee eeeeee eeeeeeeeeeeeeeeee",
                address = "11:22:33:AA:BB:CC",
                isEspMonitoringDevice = true,
                isConnectable = true,
                rssi = -95
            ),
            onDeviceClicked = {}
        )
    }
}
//endregion