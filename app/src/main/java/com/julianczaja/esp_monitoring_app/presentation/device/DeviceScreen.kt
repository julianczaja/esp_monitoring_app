package com.julianczaja.esp_monitoring_app.presentation.device

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreen
import com.julianczaja.esp_monitoring_app.presentation.devicesavedphotos.DeviceSavedScreen
import com.julianczaja.esp_monitoring_app.presentation.devicesettings.DeviceSettingsScreen
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceScreen(
    navigateToPhotoPreview: (Long, String) -> Unit,
    navigateToRemovePhotoDialog: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val pages = listOf(DevicePage.Photos, DevicePage.Saved, DevicePage.Settings)
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) },
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
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                DevicePage.Photos.index -> DevicePhotosScreen(
                    navigateToPhotoPreview,
                    navigateToRemovePhotoDialog
                )

                DevicePage.Saved.index -> DeviceSavedScreen()
                DevicePage.Settings.index -> DeviceSettingsScreen()
            }
        }
    }
}
