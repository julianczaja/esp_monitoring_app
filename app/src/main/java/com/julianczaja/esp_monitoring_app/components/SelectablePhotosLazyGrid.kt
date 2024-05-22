package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.SelectablePhoto
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DEFAULT_PHOTO_HEIGHT
import com.julianczaja.esp_monitoring_app.presentation.theme.AppTheme
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import java.time.LocalDate
import java.time.LocalDateTime


@Composable
fun SelectablePhotosLazyGrid(
    dateGroupedSelectablePhotos: Map<LocalDate, List<SelectablePhoto>>,
    isSelectionMode: Boolean,
    minSize: Dp = DEFAULT_PHOTO_HEIGHT.dp,
    onPhotoClick: (SelectablePhoto) -> Unit,
    onPhotoLongClick: (SelectablePhoto) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.Top),
        modifier = Modifier.fillMaxSize()
    ) {
        dateGroupedSelectablePhotos.onEachIndexed { index, (localDate, photos) ->
            header {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = MaterialTheme.spacing.medium,
                        alignment = Alignment.CenterHorizontally
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null
                    )
                    Text(
                        text = localDate.toString(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            items(photos, key = { it.photo.fileName }) { selectablePhoto ->
                SelectableDevicePhoto(
                    selectablePhoto = selectablePhoto,
                    isSelectionMode = isSelectionMode,
                    minSize = minSize,
                    onClick = onPhotoClick,
                    onLongClick = onPhotoLongClick
                )
            }
            if (index < dateGroupedSelectablePhotos.size - 1) {
                header {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(MaterialTheme.spacing.small)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyGridItemScope.SelectableDevicePhoto(
    selectablePhoto: SelectablePhoto,
    isSelectionMode: Boolean,
    minSize: Dp,
    onClick: (SelectablePhoto) -> Unit,
    onLongClick: (SelectablePhoto) -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(MaterialTheme.shape.photoCorners))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape.copy(CornerSize(MaterialTheme.shape.photoCorners))
            )
            .animateItem()
            .clickable(onClick = { onClick(selectablePhoto) })
            .combinedClickable(
                onClick = { onClick(selectablePhoto) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(selectablePhoto)
                }
            )
    ) {
        PhotoCoilImage(
            modifier = Modifier.align(Alignment.Center),
            data = selectablePhoto.photo.url,
            height = minSize,
        )
        if (isSelectionMode) {
            CircularCheckbox(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(MaterialTheme.spacing.medium),
                checked = selectablePhoto.isSelected
            )
        }
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Text(
                text = selectablePhoto.photo.dateTime.toLocalTime().toPrettyString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

//region Previews
@Preview(showBackground = true)
@Composable
fun SelectableDeviceSelectedPhotoPreview() {
    AppTheme {
        LazyVerticalGrid(
            contentPadding = PaddingValues(MaterialTheme.spacing.medium),
            columns = GridCells.Adaptive(200.dp)
        ) {
            item {
                SelectableDevicePhoto(
                    selectablePhoto = SelectablePhoto(
                        photo = Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 11), "fileName 1", "1600x1200", "url"),
                        isSelected = true
                    ),
                    isSelectionMode = true,
                    minSize = 200.dp,
                    onClick = {},
                    onLongClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectableDeviceNotSelectedPhotoPreview() {
    AppTheme {
        LazyVerticalGrid(
            contentPadding = PaddingValues(MaterialTheme.spacing.medium),
            columns = GridCells.Adaptive(200.dp)
        ) {
            item {
                SelectableDevicePhoto(
                    selectablePhoto = SelectablePhoto(
                        photo = Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 10), "fileName 1", "1600x1200", "url"),
                        isSelected = false
                    ),
                    isSelectionMode = true,
                    minSize = 200.dp,
                    onClick = {},
                    onLongClick = {}
                )
            }
        }
    }
}
//endregion
