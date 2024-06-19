package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing

const val DEVICE_ITEM_MIN_HEIGHT_DP = 100
const val DEVICE_ITEM_MIN_WIDTH_DP = 300

@Composable
fun DeviceItem(
    modifier: Modifier = Modifier,
    device: Device,
    lastPhotoUri: String? = null,
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
            Text(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .padding(start = MaterialTheme.spacing.large, end = 60.dp),
                text = device.name,
                style = MaterialTheme.typography.headlineSmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            if (lastPhotoUri != null) {
                LastPhoto(lastPhotoUri)
            }
            MoreMenuButton(
                device = device,
                addIconBackground = lastPhotoUri != null,
                onRemoveClicked = onRemoveClicked,
                onEditClicked = onEditClicked
            )
        }
    }
}

@Composable
private fun BoxScope.LastPhoto(lastPhotoUri: String) {
    val context = LocalContext.current

    SubcomposeAsyncImage(
        modifier = Modifier
            .fillMaxHeight()
            .align(Alignment.CenterEnd)
            .graphicsLayer { alpha = 0.99f }
            .drawWithContent {
                val colors = listOf(
                    Color.Black,
                    Color.Transparent
                )
                drawContent()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = colors,
                        startX = Float.POSITIVE_INFINITY,
                        endX = 0.0f
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        contentScale = ContentScale.FillHeight,
        contentDescription = null,
        model = ImageRequest.Builder(context)
            .data(lastPhotoUri)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.MoreMenuButton(
    device: Device,
    addIconBackground: Boolean = false,
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
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .run {
                    if (addIconBackground) {
                        val brush = Brush.radialGradient(0.3f to Color.Black, 0.7f to Color.Transparent)

                        this
                            .graphicsLayer { alpha = 0.99f }
                            .drawWithContent {
                                drawContent()
                                drawRect(brush = brush, blendMode = BlendMode.DstIn)
                            }
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = .3f))
                    } else {
                        this
                    }
                },
            onClick = {},
        ) {
            Icon(

                imageVector = Icons.Default.MoreVert,
                contentDescription = null
            )
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
            device = Device(123L, "Device name"),
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
            device = Device(123L, "Device very long name"),
            onClicked = {},
            onRemoveClicked = {},
            onEditClicked = {}
        )
    }
}
//endregion
