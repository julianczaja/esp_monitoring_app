package com.julianczaja.esp_monitoring_app.presentation.device

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.TabRow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.presentation.deviceconfiguration.DeviceSettingsScreen
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreen
import com.julianczaja.esp_monitoring_app.presentation.devicesavedphotos.DeviceSavedScreen
import kotlinx.coroutines.launch


enum class DevicePage(
    val index: Int,
    @StringRes val titleId: Int,
    @DrawableRes val drawableId: Int,
) {
    Photos(0, R.string.photos_tab_label, R.drawable.ic_baseline_photo_24),
    Saved(1, R.string.saved_tab_label, R.drawable.ic_baseline_bookmarks_24),
    Settings(2, R.string.settings_tab_label, R.drawable.ic_baseline_settings_24)
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun DeviceScreen(
    navigateToPhotoPreview: (Long, String) -> Unit,
    navigateToRemovePhotoDialog: (String) -> Unit,
) {
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
                DevicePage.Photos.index -> DevicePhotosScreen(navigateToPhotoPreview, navigateToRemovePhotoDialog)
                DevicePage.Saved.index -> DeviceSavedScreen()
                DevicePage.Settings.index -> DeviceSettingsScreen()
            }
        }
    }
}
