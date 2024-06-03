package com.julianczaja.esp_monitoring_app.presentation.device

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreen
import com.julianczaja.esp_monitoring_app.presentation.devicesavedphotos.DeviceSavedPhotosScreen
import kotlinx.coroutines.launch


@Composable
fun DeviceScreen(
    snackbarHostState: SnackbarHostState,
    navigateToPhotoPreview: (Long, String) -> Unit,
    navigateToRemovePhotosDialog: (List<String>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val pages = listOf(DevicePage.Photos, DevicePage.Saved, DevicePage.Info)
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
                    snackbarHostState = snackbarHostState,
                    navigateToPhotoPreview = navigateToPhotoPreview,
                    navigateToRemovePhotosDialog = navigateToRemovePhotosDialog
                )

                DevicePage.Saved.index -> DeviceSavedPhotosScreen(
                    snackbarHostState = snackbarHostState,
                    navigateToPhotoPreview = navigateToPhotoPreview
                )

                DevicePage.Info.index -> Box(modifier = Modifier.fillMaxSize()) {
                    Text(text = "Work in progress...")
                }
            }
        }
    }
}
