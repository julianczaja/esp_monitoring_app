package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerScope
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.components.ErrorText
import com.julianczaja.esp_monitoring_app.components.PhotoCoilImage
import com.julianczaja.esp_monitoring_app.components.header
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import com.julianczaja.esp_monitoring_app.toPrettyString
import java.time.LocalDate


const val DEFAULT_PHOTO_HEIGHT = 150

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PagerScope.DevicePhotosScreen(
    viewModel: DevicePhotosScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.devicePhotosUiState.collectAsStateWithLifecycle()

    DevicePhotosScreenContent(
        uiState = uiState,
        updatePhotos = viewModel::updatePhotos,
        onPhotoClick = {}, // TODO: Implement photos full screen preview
        onPhotoLongClick = viewModel::removePhoto // TODO: Open confirmation dialog or implement items selection
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DevicePhotosScreenContent(
    uiState: DevicePhotosScreenUiState,
    updatePhotos: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
) {
    val pullRefreshState = rememberPullRefreshState(uiState.isRefreshing, updatePhotos)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
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
        PullRefreshIndicator(uiState.isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
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
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.CenterHorizontally),
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
                    Divider(
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
        modifier = Modifier
            .animateItemPlacement()
    ) {
        PhotoCoilImage(
            url = photo.url,
            height = minSize,
            onClick = { onClick(photo) },
            onLongClick = { onLongClick(photo) }
        )
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                .fillMaxWidth()
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
