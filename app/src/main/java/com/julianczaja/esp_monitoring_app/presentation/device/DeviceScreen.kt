package com.julianczaja.esp_monitoring_app.presentation.device

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TabRow
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.pager.*
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.PhotoCoilImage
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import com.julianczaja.esp_monitoring_app.toPrettyString
import kotlinx.coroutines.launch


const val DEFAULT_PHOTO_HEIGHT = 150

enum class DevicePage(
    val index: Int,
    @StringRes val titleId: Int,
    @DrawableRes val drawableId: Int,
) {
    Photos(0, R.string.photos_tab_label, R.drawable.ic_baseline_photo_24),
    Saved(1, R.string.saved_tab_label, R.drawable.ic_baseline_bookmarks_24),
    Settings(2, R.string.settings_tab_label, R.drawable.ic_baseline_settings_24)
}

@OptIn(ExperimentalLifecycleComposeApi::class, ExperimentalPagerApi::class)
@Composable
fun DeviceScreen(
    viewModel: DeviceScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.deviceUiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState()
    val pages = listOf(DevicePage.Photos, DevicePage.Saved, DevicePage.Settings)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            backgroundColor = MaterialTheme.colorScheme.background,
            divider = { Divider(color = MaterialTheme.colorScheme.surfaceVariant) },
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                )
            },
            tabs = {
                pages.forEach { devicePage ->
                    Tab(
                        icon = {
                            Icon(
                                painter = painterResource(id = devicePage.drawableId),
                                contentDescription = null
                            )
                        },
                        text = { Text(stringResource(id = devicePage.titleId)) },
                        selected = pagerState.currentPage == devicePage.index,
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(devicePage.index)
                            }
                        },
                    )
                }
            },
            modifier = Modifier.zIndex(1f) // to hide PullRefreshIndicator under TabRow
        )
        HorizontalPager(
            count = pages.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                DevicePage.Photos.index -> DevicePhotosScreenContent(uiState, viewModel::updatePhotos)
                DevicePage.Saved.index -> DeviceSavedScreenContent()
                DevicePage.Settings.index -> DeviceSettingsScreenContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalPagerApi::class)
@Composable
fun PagerScope.DevicePhotosScreenContent(
    uiState: DeviceScreenUiState,
    updatePhotos: () -> Unit,
) {
    val pullRefreshState = rememberPullRefreshState(uiState.isRefreshing, updatePhotos)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        when (uiState.devicePhotosUiState) {
            DevicePhotosUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is DevicePhotosUiState.Success -> if (uiState.devicePhotosUiState.photos.isNotEmpty()) {
                PhotosLazyGrid(photos = uiState.devicePhotosUiState.photos)
            } else {
                Text(
                    text = stringResource(R.string.no_photos),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            is DevicePhotosUiState.Error -> Text(
                text = stringResource(uiState.devicePhotosUiState.messageId),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        PullRefreshIndicator(uiState.isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun BoxScope.PhotosLazyGrid(
    modifier: Modifier = Modifier,
    photos: List<Photo>,
    minSize: Dp = DEFAULT_PHOTO_HEIGHT.dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.Top),
        modifier = modifier
    ) {
        items(items = photos, key = { it.dateTime }) {
            Box {
                PhotoCoilImage(
                    url = it.url,
                    height = minSize,
                    onClick = {
                        // TODO
                    }
                )
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = it.dateTime.toPrettyString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun PagerScope.DeviceSavedScreenContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Saved", modifier = Modifier.align(Alignment.Center))
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PagerScope.DeviceSettingsScreenContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Settings", modifier = Modifier.align(Alignment.Center))
    }
}