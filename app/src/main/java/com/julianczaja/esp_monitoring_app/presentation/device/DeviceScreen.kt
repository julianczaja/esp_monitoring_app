package com.julianczaja.esp_monitoring_app.presentation.device

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TabRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.pager.*
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.PhotoCoilImage
import com.julianczaja.esp_monitoring_app.components.header
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import com.julianczaja.esp_monitoring_app.toPrettyString
import kotlinx.coroutines.launch
import java.time.LocalDate


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
private fun PagerScope.DevicePhotosScreenContent(
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
            DevicePhotosUiState.Loading -> DevicePhotosLoadingScreen()
            is DevicePhotosUiState.Error -> DevicePhotosErrorScreen(uiState.devicePhotosUiState.messageId)
            is DevicePhotosUiState.Success -> if (uiState.devicePhotosUiState.dateGroupedPhotos.isEmpty()) {
                DevicePhotosEmptyScreen()
            } else {
                PhotosLazyGrid(uiState.devicePhotosUiState.dateGroupedPhotos)
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
        Text(
            text = stringResource(errorMessageId),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
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
        CircularProgressIndicator()
    }
}


@Composable
private fun PhotosLazyGrid(
    dateGroupedPhotos: Map<LocalDate, List<Photo>>,
    minSize: Dp = DEFAULT_PHOTO_HEIGHT.dp,
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
                DevicePhoto(photo, minSize)
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
private fun LazyGridItemScope.DevicePhoto(photo: Photo, minSize: Dp) {
    Box(Modifier.animateItemPlacement()) {
        PhotoCoilImage(
            url = photo.url,
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
                text = photo.dateTime.toLocalTime().toPrettyString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}


@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PagerScope.DeviceSavedScreenContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Saved", modifier = Modifier.align(Alignment.Center))
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PagerScope.DeviceSettingsScreenContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Settings", modifier = Modifier.align(Alignment.Center))
    }
}