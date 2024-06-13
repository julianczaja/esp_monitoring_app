package com.julianczaja.esp_monitoring_app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.SelectablePhoto
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DEFAULT_PHOTO_WIDTH
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

private const val NUMBER_OF_ITEMS_TO_SCROLL_TO_SHOW_SCROLL_TO_TOP_BUTTON = 20

@Composable
fun SelectablePhotosLazyGrid(
    modifier: Modifier = Modifier,
    dateGroupedSelectablePhotos: Map<LocalDate, List<SelectablePhoto>>,
    isSelectionMode: Boolean,
    minSize: Dp = DEFAULT_PHOTO_WIDTH.dp,
    state: LazyGridState = rememberLazyGridState(),
    onPhotoClick: (SelectablePhoto) -> Unit,
    onPhotoLongClick: (SelectablePhoto) -> Unit,
    onSelectDeselectAllClick: (LocalDate) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val lastVisibleItemIndex by remember {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }
    }
    val isScrollUpVisible by remember {
        derivedStateOf {
            lastVisibleItemIndex > NUMBER_OF_ITEMS_TO_SCROLL_TO_SHOW_SCROLL_TO_TOP_BUTTON
        }
    }
    val scrollProgress by remember(dateGroupedSelectablePhotos) {
        derivedStateOf {
            lastVisibleItemIndex / dateGroupedSelectablePhotos.values.flatten().size.toFloat()
        }
    }

    Box(modifier) {
        LazyVerticalGrid(
            modifier = modifier,
            columns = GridCells.Adaptive(minSize),
            state = state,
            contentPadding = PaddingValues(MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            dateGroupedSelectablePhotos.onEachIndexed { index, (localDate, photos) ->
                header(key = localDate) {
                    Column {
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(
                                    space = MaterialTheme.spacing.small,
                                    alignment = Alignment.Start
                                )
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_calendar),
                                    contentDescription = null
                                )
                                Text(
                                    text = localDate.toString(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            val allPhotosWithDateAreSelected = photos
                                .filter { it.photo.dateTime.toLocalDate() == localDate }
                                .all { it.isSelected }

                            val selectDeselectIcon = when (allPhotosWithDateAreSelected) {
                                true -> R.drawable.ic_deselect_all
                                false -> R.drawable.ic_select_all
                            }
                            IconButton(
                                modifier = Modifier.size(24.dp),
                                onClick = { onSelectDeselectAllClick(localDate) }
                            ) {
                                Icon(
                                    painter = painterResource(id = selectDeselectIcon),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
                items(photos, key = { it.photo.dateTime }) { selectablePhoto ->
                    SelectableDevicePhoto(
                        selectablePhoto = selectablePhoto,
                        isSelectionMode = isSelectionMode,
                        minSize = minSize,
                        onClick = onPhotoClick,
                        onLongClick = onPhotoLongClick
                    )
                }
            }
        }
        ScrollToTopButton(
            isVisible = isScrollUpVisible,
            scrollProgress = scrollProgress,
            onClicked = { coroutineScope.launch { state.animateScrollToItem(0) } }
        )
    }
}

@Composable
private fun BoxScope.ScrollToTopButton(
    isVisible: Boolean,
    scrollProgress: Float,
    onClicked: () -> Unit
) {
    AnimatedVisibility(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(MaterialTheme.spacing.large),
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it * 2 })
    ) {
        val progressColor = MaterialTheme.colorScheme.onPrimaryContainer

        IconButton(
            onClick = onClicked,
            modifier = Modifier
                .drawWithContent {
                    val strokeWidth = 3.dp.toPx()
                    drawContent()
                    drawArc(
                        color = progressColor,
                        size = Size(width = size.width - strokeWidth, height = size.height - strokeWidth),
                        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                        startAngle = 270f,
                        sweepAngle = scrollProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_arrow_up), contentDescription = null)
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
            .run {
                if (selectablePhoto.isSelected) {
                    this.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape.copy(CornerSize(MaterialTheme.shape.photoCorners))
                    )
                } else {
                    this
                }
            }
            .animateItem()
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
            data = selectablePhoto.photo.thumbnailUrl,
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
@PreviewLightDark
@Preview(device = "spec: width = 411dp, height = 891dp, orientation = landscape, dpi = 420", showSystemUi = true)
@Composable
fun SelectableDevicePhotoPreview() {
    val dateGroupedSelectablePhotos = mapOf(
        LocalDate.of(2023, 1, 1) to listOf(
            SelectablePhoto(
                photo = Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 10), "fileName 1", "1600x1200", "url", "url"),
                isSelected = true
            ),
            SelectablePhoto(
                photo = Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 11), "fileName 2", "1600x1200", "url", "url"),
                isSelected = true
            ),
            SelectablePhoto(
                photo = Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 12), "fileName 3", "1600x1200", "url", "url"),
                isSelected = false
            ),
        ),
        LocalDate.of(2023, 1, 2) to listOf(
            SelectablePhoto(
                photo = Photo(123L, LocalDateTime.of(2023, 1, 2, 10, 13), "fileName 4", "1600x1200", "url", "url"),
                isSelected = false
            ),
            SelectablePhoto(
                photo = Photo(123L, LocalDateTime.of(2023, 1, 2, 10, 14), "fileName 5", "1600x1200", "url", "url"),
                isSelected = false
            ),
        )
    )

    AppBackground {
        SelectablePhotosLazyGrid(
            dateGroupedSelectablePhotos = dateGroupedSelectablePhotos,
            isSelectionMode = true,
            onPhotoClick = {},
            onPhotoLongClick = {},
            onSelectDeselectAllClick = {}
        )
    }
}
//endregion
