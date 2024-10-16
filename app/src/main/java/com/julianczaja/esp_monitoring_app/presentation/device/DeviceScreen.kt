package com.julianczaja.esp_monitoring_app.presentation.device

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.deviceinfo.DeviceInfoScreen
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreen
import com.julianczaja.esp_monitoring_app.presentation.devicetimelapses.DeviceTimelapsesScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch


@Composable
fun DeviceScreen(
    onSetAppBarTitle: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    navigateToPhotoPreview: (Int) -> Unit,
    navigateToRemovePhotosDialog: () -> Unit,
    navigateToSavePhotosDialog: (List<Photo>) -> Unit,
    navigateToTimelapseCreatorScreen: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val pages = remember {
        persistentListOf(DevicePage.Photos, DevicePage.Timelapse, DevicePage.Info)
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(key1 = true) {
        onSetAppBarTitle(R.string.device_screen_title)
    }

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
            modifier = Modifier.zIndex(1f)
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                DevicePage.Photos.index -> DevicePhotosScreen(
                    snackbarHostState = snackbarHostState,
                    navigateToPhotoPreview = navigateToPhotoPreview,
                    navigateToRemovePhotosDialog = navigateToRemovePhotosDialog,
                    navigateToSavePhotosDialog = navigateToSavePhotosDialog,
                    navigateToTimelapseCreatorScreen = navigateToTimelapseCreatorScreen
                )

                DevicePage.Timelapse.index -> DeviceTimelapsesScreen(
                    snackbarHostState = snackbarHostState
                )

                DevicePage.Info.index -> DeviceInfoScreen(
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
