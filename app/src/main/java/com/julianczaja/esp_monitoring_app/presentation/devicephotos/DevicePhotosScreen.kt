package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.CircularCheckbox
import com.julianczaja.esp_monitoring_app.components.PhotoCoilImage
import com.julianczaja.esp_monitoring_app.components.header
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.SelectablePhoto
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.AppTheme
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import java.time.LocalDate
import java.time.LocalDateTime


const val DEFAULT_PHOTO_HEIGHT = 150

@Composable
fun DevicePhotosScreen(
    snackbarHostState: SnackbarHostState,
    navigateToPhotoPreview: (Long, String) -> Unit,
    navigateToRemovePhotoDialog: (String) -> Unit, // TODO: how to delete multiple photos?
    viewModel: DevicePhotosScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.devicePhotosUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var updatePhotosCalled by rememberSaveable { mutableStateOf(false) }

    BackHandler(
        enabled = uiState.isSelectionMode,
        onBack = viewModel::resetSelectedPhotos
    )

    LaunchedEffect(true) {
        if (!updatePhotosCalled) {
            viewModel.updatePhotos()
            updatePhotosCalled = true
        }
        viewModel.eventFlow.collect { event ->
            when (event) {
                is Event.ShowError -> snackbarHostState.showSnackbar(
                    message = context.getString(event.messageId),
                    duration = SnackbarDuration.Short
                )

                is Event.NavigateToPhotoPreview -> navigateToPhotoPreview(event.photo.deviceId, event.photo.fileName)
            }
        }
    }

    DevicePhotosScreenContent(
        uiState = uiState,
        updatePhotos = viewModel::updatePhotos,
        resetSelections = viewModel::resetSelectedPhotos,
        saveSelectedPhotos = viewModel::saveSelectedPhotos,
        removeSelectedPhotos = viewModel::removeSelectedPhotos,
        onPhotoClick = viewModel::onPhotoClick,
        onPhotoLongClick = viewModel::onPhotoLongClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicePhotosScreenContent(
    uiState: UiState,
    updatePhotos: () -> Unit,
    resetSelections: () -> Unit,
    saveSelectedPhotos: () -> Unit,
    removeSelectedPhotos: () -> Unit,
    onPhotoClick: (SelectablePhoto) -> Unit,
    onPhotoLongClick: (SelectablePhoto) -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState(enabled = { uiState.isOnline })

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            updatePhotos()
        }
    }
    if (!uiState.isLoading) {
        LaunchedEffect(true) {
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            OfflineBar(uiState.isOnline)
            SelectedEditBar(uiState, removeSelectedPhotos, saveSelectedPhotos, resetSelections)

            when (uiState.dateGroupedSelectablePhotos.isEmpty()) {
                true -> DevicePhotosEmptyScreen()
                false -> SelectablePhotosLazyGrid(
                    dateGroupedSelectablePhotos = uiState.dateGroupedSelectablePhotos,
                    isSelectionMode = uiState.isSelectionMode,
                    onPhotoClick = onPhotoClick,
                    onPhotoLongClick = onPhotoLongClick
                )
            }
        }
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullRefreshState,
        )
    }
}

@Composable
private fun ColumnScope.SelectedEditBar(
    uiState: UiState,
    removeSelectedPhotos: () -> Unit,
    saveSelectedPhotos: () -> Unit,
    resetSelections: () -> Unit
) {
    AnimatedVisibility(visible = uiState.isSelectionMode) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(horizontal = MaterialTheme.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small, Alignment.End),
            ) {
                IconButton(onClick = removeSelectedPhotos) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                }
                IconButton(onClick = saveSelectedPhotos) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_save_24),
                        contentDescription = null
                    )
                }
                IconButton(onClick = resetSelections) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ColumnScope.OfflineBar(isOnline: Boolean) {
    AnimatedVisibility(visible = !isOnline) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                text = stringResource(R.string.you_are_offline),
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun DevicePhotosEmptyScreen() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.large)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.no_photos),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
private fun SelectablePhotosLazyGrid(
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
            items(photos, key = { it.photo.dateTime }) { selectablePhoto ->
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
            .animateItemPlacement()
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
            url = selectablePhoto.photo.url,
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

@Preview(showSystemUi = true)
@Composable
private fun DevicePhotosStateItemsPreview() {
    AppBackground {
        val dateGroupedSelectablePhotos = mapOf(
            LocalDate.of(2023, 1, 1) to listOf(
                SelectablePhoto(
                    photo = Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 10), "fileName 1", "1600x1200", "url"),
                    isSelected = false
                ),
                SelectablePhoto(
                    photo = Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 11), "fileName 2", "1600x1200", "url"),
                    isSelected = false
                ),
                SelectablePhoto(
                    photo = Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 12), "fileName 3", "1600x1200", "url"),
                    isSelected = false
                ),
            ),
            LocalDate.of(2023, 1, 2) to listOf(
                SelectablePhoto(
                    photo = Photo(123L, LocalDateTime.of(2023, 1, 2, 10, 13), "fileName 4", "1600x1200", "url"),
                    isSelected = false
                ),
                SelectablePhoto(
                    photo = Photo(123L, LocalDateTime.of(2023, 1, 2, 10, 14), "fileName 5", "1600x1200", "url"),
                    isSelected = false
                ),
            )
        )
        DevicePhotosScreenContent(
            uiState = UiState(
                dateGroupedSelectablePhotos = dateGroupedSelectablePhotos,
                isLoading = false,
                isOnline = false,
                isSelectionMode = false,
            ),
            {}, {}, {}, {}, {}, {},
        )
    }

}

@Preview(showSystemUi = true)
@Composable
private fun DevicePhotosStateSuccessNoItemsPreview() {
    AppBackground {
        DevicePhotosScreenContent(
            UiState(
                dateGroupedSelectablePhotos = emptyMap(),
                isLoading = false,
                isOnline = true,
                isSelectionMode = false,
            ),
            {}, {}, {}, {}, {}, {},
        )
    }
}
//endregion