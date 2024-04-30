package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.components.ErrorText
import com.julianczaja.esp_monitoring_app.components.PhotoCoilImage
import com.julianczaja.esp_monitoring_app.components.header
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.AppBackground
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.DevicePhotosScreenUiState
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.DevicePhotosState
import com.julianczaja.esp_monitoring_app.presentation.theme.AppTheme
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import java.time.LocalDate
import java.time.LocalDateTime


const val DEFAULT_PHOTO_HEIGHT = 150

@Composable
fun DevicePhotosScreen(
    navigateToPhotoPreview: (Long, String) -> Unit,
    navigateToRemovePhotoDialog: (String) -> Unit,
    viewModel: DevicePhotosScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.devicePhotosUiState.collectAsStateWithLifecycle()

    DevicePhotosScreenContent(
        uiState = uiState,
        updatePhotos = viewModel::updatePhotos,
        onPhotoClick = { navigateToPhotoPreview(it.deviceId, it.fileName) },
        onPhotoLongClick = { navigateToRemovePhotoDialog(it.fileName) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicePhotosScreenContent(
    uiState: DevicePhotosScreenUiState,
    updatePhotos: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(key1 = uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullRefreshState.endRefresh()
        }
    }
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            updatePhotos()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        when (uiState.devicePhotosUiState) {
            DevicePhotosState.Loading -> DevicePhotosLoadingScreen()
            is DevicePhotosState.Error -> DevicePhotosErrorScreen(uiState.devicePhotosUiState.messageId)
            is DevicePhotosState.Success -> if (uiState.devicePhotosUiState.dateGroupedPhotos.isEmpty()) {
                DevicePhotosEmptyScreen()
            } else {
                DevicePhotosLazyGrid(
                    dateGroupedPhotos = uiState.devicePhotosUiState.dateGroupedPhotos,
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
private fun DevicePhotosErrorScreen(@StringRes errorMessageId: Int) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ErrorText(text = stringResource(errorMessageId))
    }
}

@Composable
private fun DevicePhotosEmptyScreen() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
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
private fun DevicePhotosLoadingScreen() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DefaultProgressIndicator()
    }
}


@Composable
private fun DevicePhotosLazyGrid(
    dateGroupedPhotos: Map<LocalDate, List<Photo>>,
    minSize: Dp = DEFAULT_PHOTO_HEIGHT.dp,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.Top),
        modifier = Modifier.fillMaxSize()
    ) {
        dateGroupedPhotos.onEachIndexed { index, (localDate, photos) ->
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
            items(photos, key = { it.dateTime }) { photo ->
                DevicePhoto(
                    photo = photo,
                    minSize = minSize,
                    onClick = onPhotoClick,
                    onLongClick = onPhotoLongClick
                )
            }
            if (index < dateGroupedPhotos.size - 1) {
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
private fun LazyGridItemScope.DevicePhoto(
    photo: Photo,
    minSize: Dp,
    onClick: (Photo) -> Unit,
    onLongClick: (Photo) -> Unit,
) {
    Box(
        modifier = Modifier.animateItemPlacement()
    ) {
        PhotoCoilImage(
            url = photo.url,
            height = minSize,
            onClick = { onClick(photo) },
            onLongClick = { onLongClick(photo) }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                .align(Alignment.BottomCenter)
        ) {
            Text(
                text = photo.dateTime.toLocalTime().toPrettyString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DevicePhotosStateSuccessPreview() {
    AppTheme {
        AppBackground {
            val dateGroupedPhotos = mapOf(
                LocalDate.of(2023, 1, 1) to listOf(
                    Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 10), "fileName 1", "1600x1200", "url"),
                    Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 11), "fileName 2", "1600x1200", "url"),
                    Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 12), "fileName 3", "1600x1200", "url"),
                ),
                LocalDate.of(2023, 1, 2) to listOf(
                    Photo(123L, LocalDateTime.of(2023, 1, 2, 10, 13), "fileName 4", "1600x1200", "url"),
                    Photo(123L, LocalDateTime.of(2023, 1, 2, 10, 14), "fileName 5", "1600x1200", "url"),
                )
            )
            DevicePhotosScreenContent(
                DevicePhotosScreenUiState(DevicePhotosState.Success(dateGroupedPhotos), false),
                {}, {}, {}
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DevicePhotosStateSuccessNoItemsPreview() {
    AppTheme {
        AppBackground {
            DevicePhotosScreenContent(
                DevicePhotosScreenUiState(DevicePhotosState.Success(emptyMap()), false),
                {}, {}, {}
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DevicePhotosStateLoadingPreview() {
    AppTheme {
        AppBackground {
            DevicePhotosScreenContent(
                DevicePhotosScreenUiState(DevicePhotosState.Loading, false),
                {}, {}, {}
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DevicePhotosStateErrorPreview() {
    AppTheme {
        AppBackground {
            DevicePhotosScreenContent(
                DevicePhotosScreenUiState(DevicePhotosState.Error(R.string.internal_app_error_message), false),
                {}, {}, {}
            )
        }
    }
}
