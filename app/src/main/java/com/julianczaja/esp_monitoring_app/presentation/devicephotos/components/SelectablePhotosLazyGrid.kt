package com.julianczaja.esp_monitoring_app.presentation.devicephotos.components


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.CircularCheckbox
import com.julianczaja.esp_monitoring_app.components.Notice
import com.julianczaja.esp_monitoring_app.components.PhotoCoilImage
import com.julianczaja.esp_monitoring_app.components.PhotoInfoRow
import com.julianczaja.esp_monitoring_app.components.header
import com.julianczaja.esp_monitoring_app.data.utils.toDefaultFormatString
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.Selectable
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DEFAULT_PHOTO_WIDTH
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private const val NUMBER_OF_ITEMS_TO_SCROLL_TO_SHOW_SCROLL_TO_TOP_BUTTON = 20

@Composable
fun SelectablePhotosLazyGrid(
    modifier: Modifier = Modifier,
    selectablePhotos: ImmutableList<Selectable<Photo>>,
    isSelectionMode: Boolean,
    minSize: Dp = DEFAULT_PHOTO_WIDTH.dp,
    state: LazyGridState = rememberLazyGridState(),
    noticeContent: (@Composable () -> Unit)? = null,
    onPhotoClick: (Selectable<Photo>) -> Unit,
    onPhotoLongClick: (Selectable<Photo>) -> Unit,
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
    val scrollProgress by remember(selectablePhotos) {
        derivedStateOf {
            lastVisibleItemIndex / selectablePhotos.size.toFloat()
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
            noticeContent?.let {
                header { it.invoke() }
            }
            items(
                items = selectablePhotos,
                key = { it.item.dateTime.toDefaultFormatString() + "|${it.item.isSaved}" }
            ) { selectablePhoto ->
                SelectableDevicePhoto(
                    selectablePhoto = selectablePhoto,
                    isSelectionMode = isSelectionMode,
                    minSize = minSize,
                    onClick = onPhotoClick,
                    onLongClick = onPhotoLongClick
                )
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
            .safeDrawingPadding()
            .padding(MaterialTheme.spacing.large),
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut()
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
    selectablePhoto: Selectable<Photo>,
    isSelectionMode: Boolean,
    minSize: Dp,
    onClick: (Selectable<Photo>) -> Unit,
    onLongClick: (Selectable<Photo>) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val photoInfoRowItems by remember(selectablePhoto) {
        mutableStateOf(
            persistentListOf(selectablePhoto.item.dateTime.toLocalTime().toPrettyString())
        )
    }

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
            data = selectablePhoto.item.thumbnailUrl,
            height = minSize,
        )
        if (selectablePhoto.item.isSaved) {
            SavedIcon()
        }
        if (isSelectionMode) {
            CircularCheckbox(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(MaterialTheme.spacing.medium),
                checked = selectablePhoto.isSelected
            )
        }
        PhotoInfoRow(photoInfoRowItems)
    }
}

@Composable
private fun BoxScope.SavedIcon() {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(MaterialTheme.spacing.medium)
            .size(24.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.surface,
            painter = painterResource(id = R.drawable.ic_save_24),
            contentDescription = null
        )
    }
}

//region Previews
@PreviewLightDark
@Preview(device = "spec: width = 411dp, height = 891dp, orientation = landscape, dpi = 420", showSystemUi = true)
@Composable
fun SelectableDevicePhotosPreview() {
    val selectablePhotos = persistentListOf(
        Selectable(
            item = Photo.mock(dateTime = LocalDateTime.of(2023, 1, 1, 10, 10)),
            isSelected = false
        ),
        Selectable(
            item = Photo.mock(dateTime = LocalDateTime.of(2023, 1, 1, 10, 11)),
            isSelected = true
        ),
        Selectable(
            item = Photo.mock(dateTime = LocalDateTime.of(2023, 1, 1, 10, 12)),
            isSelected = false
        )
    )

    AppBackground {
        SelectablePhotosLazyGrid(
            selectablePhotos = selectablePhotos,
            isSelectionMode = true,
            onPhotoClick = {},
            onPhotoLongClick = {},
        )
    }
}

@PreviewLightDark
@Preview(device = "spec: width = 411dp, height = 891dp, orientation = landscape, dpi = 420", showSystemUi = true)
@Composable
fun SelectableDevicePhotosWithNoticePreview() {
    val selectablePhotos = persistentListOf(
        Selectable(
            item = Photo.mock(dateTime = LocalDateTime.of(2023, 1, 1, 10, 10)),
            isSelected = false
        ),
        Selectable(
            item = Photo.mock(dateTime = LocalDateTime.of(2023, 1, 1, 10, 11)),
            isSelected = true
        ),
        Selectable(
            item = Photo.mock(dateTime = LocalDateTime.of(2023, 1, 1, 10, 12)),
            isSelected = false
        )
    )

    AppBackground {
        SelectablePhotosLazyGrid(
            selectablePhotos = selectablePhotos,
            noticeContent = {
                Notice(text = "Some text", actionText = "Action", onIgnoreClicked = { }, onActionClicked = {})
            },
            isSelectionMode = true,
            onPhotoClick = {},
            onPhotoLongClick = {},
        )
    }
}
//endregion
