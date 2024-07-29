package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.Notice
import com.julianczaja.esp_monitoring_app.components.PermissionRationaleDialog
import com.julianczaja.esp_monitoring_app.components.StateBar
import com.julianczaja.esp_monitoring_app.data.utils.getActivity
import com.julianczaja.esp_monitoring_app.data.utils.getPermissionState
import com.julianczaja.esp_monitoring_app.data.utils.getReadExternalStorageImagesPermissionName
import com.julianczaja.esp_monitoring_app.data.utils.openAppSettings
import com.julianczaja.esp_monitoring_app.domain.model.Day
import com.julianczaja.esp_monitoring_app.domain.model.PermissionState
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.PhotosFilterMode
import com.julianczaja.esp_monitoring_app.domain.model.Selectable
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.components.FilterBar
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.components.SelectablePhotosLazyGrid
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.components.SelectedEditBar
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.absoluteValue


const val DEFAULT_PHOTO_WIDTH = 150

@Composable
fun DevicePhotosScreen(
    snackbarHostState: SnackbarHostState,
    navigateToPhotoPreview: (Int) -> Unit,
    navigateToTimelapseCreatorScreen: () -> Unit,
    navigateToRemovePhotosDialog: (List<Photo>) -> Unit,
    navigateToSavePhotosDialog: (List<Photo>) -> Unit,
    viewModel: DevicePhotosScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.devicePhotosUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val storagePermissionName = getReadExternalStorageImagesPermissionName()
    var storagePermissionState by rememberSaveable {
        mutableStateOf(context.getActivity().getPermissionState(storagePermissionName))
    }
    var shouldShowPermissionMissingNotice by rememberSaveable {
        mutableStateOf(storagePermissionState != PermissionState.GRANTED)
    }
    var shouldShowPermissionRationaleDialog by rememberSaveable { mutableStateOf(false) }

    val readExternalStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            when (isGranted) {
                true -> {
                    storagePermissionState = PermissionState.GRANTED
                    shouldShowPermissionRationaleDialog = false
                    shouldShowPermissionMissingNotice = false
                    viewModel.onPermissionsGranted()
                }

                false -> {
                    storagePermissionState = context.getActivity().getPermissionState(storagePermissionName)
                    shouldShowPermissionRationaleDialog = true
                }
            }
        }
    )

    BackHandler(
        enabled = uiState.isSelectionMode,
        onBack = viewModel::resetSelectedPhotos
    )

    LaunchedEffect(true) {
        if (!uiState.isInitiated) {
            viewModel.init()
        }
        viewModel.eventFlow.collect { event ->
            when (event) {
                is Event.ShowError -> snackbarHostState.showSnackbar(
                    message = context.getString(event.messageId),
                    duration = SnackbarDuration.Short
                )

                is Event.ShowSavedInfo -> snackbarHostState.showSnackbar(
                    message = context.getString(R.string.save_photo_to_external_storage_result_message)
                        .format(event.savedCount, event.totalCount),
                    duration = SnackbarDuration.Short
                )

                Event.ShowNotEnoughSelectedInfo -> snackbarHostState.showSnackbar(
                    message = context.getString(R.string.timelapse_not_enough_photos_selected_message),
                    duration = SnackbarDuration.Short
                )

                is Event.NavigateToPhotoPreview -> navigateToPhotoPreview(event.initialIndex)
                is Event.NavigateToRemovePhotosDialog -> navigateToRemovePhotosDialog(event.photos)
                is Event.NavigateToSavePhotosDialog -> navigateToSavePhotosDialog(event.photos)
                is Event.NavigateToTimelapseCreatorScreen -> navigateToTimelapseCreatorScreen()
            }
        }
    }

    if (shouldShowPermissionRationaleDialog) {
        PermissionRationaleDialog(
            title = R.string.storage_permission_needed_title,
            bodyRationale = R.string.storage_permission_rationale_body,
            bodyDenied = R.string.storage_permission_denied_body,
            permissionState = storagePermissionState,
            onRequestPermission = {
                when (storagePermissionState == PermissionState.RATIONALE_NEEDED) {
                    true -> readExternalStoragePermissionLauncher.launch(storagePermissionName)
                    false -> context.getActivity().openAppSettings()
                }
            },
            onDismiss = { shouldShowPermissionRationaleDialog = false }
        )
    }

    DevicePhotosScreenContent(
        uiState = uiState,
        shouldShowPermissionMissingNotice = shouldShowPermissionMissingNotice,
        refreshData = viewModel::refreshData,
        resetSelections = viewModel::resetSelectedPhotos,
        saveSelectedPhotos = viewModel::saveSelectedPhotos,
        createTimelapseFromSelectedPhotos = viewModel::createTimelapseFromSelectedPhotos,
        removeSelectedPhotos = viewModel::removeSelectedPhotos,
        onPhotoClick = viewModel::onPhotoClick,
        onPhotoLongClick = viewModel::onPhotoLongClick,
        onDayChanged = viewModel::onDayChanged,
        selectDeselectAllPhotos = viewModel::selectDeselectAllPhotos,
        onFilterModeClicked = viewModel::onFilterModeClicked,
        onPermissionNoticeActionClicked = { readExternalStoragePermissionLauncher.launch(storagePermissionName) },
        onPermissionNoticeIgnoreClicked = { shouldShowPermissionMissingNotice = false }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicePhotosScreenContent(
    uiState: UiState,
    shouldShowPermissionMissingNotice: Boolean,
    refreshData: () -> Unit,
    resetSelections: () -> Unit,
    selectDeselectAllPhotos: () -> Unit,
    saveSelectedPhotos: () -> Unit,
    createTimelapseFromSelectedPhotos: () -> Unit,
    removeSelectedPhotos: () -> Unit,
    onPhotoClick: (Selectable<Photo>) -> Unit,
    onPhotoLongClick: (Selectable<Photo>) -> Unit,
    onDayChanged: (Day) -> Unit,
    onFilterModeClicked: () -> Unit,
    onPermissionNoticeActionClicked: () -> Unit,
    onPermissionNoticeIgnoreClicked: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        StateBar(isVisible = !uiState.isOnline, title = R.string.you_are_offline)
        SelectedEditBar(
            isSelectionMode = uiState.isSelectionMode,
            selectedCount = uiState.selectedCount,
            createTimelapseFromSelectedPhotos = createTimelapseFromSelectedPhotos,
            removeSelectedPhotos = removeSelectedPhotos,
            saveSelectedPhotos = saveSelectedPhotos,
            selectDeselectAllPhotos = selectDeselectAllPhotos,
            resetSelections = resetSelections
        )
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = uiState.isLoading,
            onRefresh = refreshData
        ) {
            when (uiState.dayGroupedSelectablePhotos.isEmpty()) {
                true -> DevicePhotosEmptyScreen(
                    isInitiated = uiState.isInitiated,
                    filterMode = uiState.filterMode,
                    shouldShowPermissionMissingNotice = shouldShowPermissionMissingNotice,
                    onPermissionNoticeActionClicked = onPermissionNoticeActionClicked,
                    onPermissionNoticeIgnoreClicked = onPermissionNoticeIgnoreClicked,
                    onFilterModeClicked = onFilterModeClicked
                )

                false -> DevicePhotosNotEmptyScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    shouldShowPermissionMissingNotice = shouldShowPermissionMissingNotice,
                    onPhotoClick = onPhotoClick,
                    onPhotoLongClick = onPhotoLongClick,
                    onDayChanged = onDayChanged,
                    onFilterModeClicked = onFilterModeClicked,
                    onPermissionNoticeActionClicked = onPermissionNoticeActionClicked,
                    onPermissionNoticeIgnoreClicked = onPermissionNoticeIgnoreClicked
                )
            }
        }
    }
}

@Composable
private fun DevicePhotosNotEmptyScreen(
    modifier: Modifier = Modifier,
    uiState: UiState,
    shouldShowPermissionMissingNotice: Boolean,
    onPhotoClick: (Selectable<Photo>) -> Unit,
    onPhotoLongClick: (Selectable<Photo>) -> Unit,
    onDayChanged: (Day) -> Unit,
    onFilterModeClicked: () -> Unit,
    onPermissionNoticeActionClicked: () -> Unit,
    onPermissionNoticeIgnoreClicked: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val days by remember(uiState.dayGroupedSelectablePhotos) {
        mutableStateOf(
            uiState.dayGroupedSelectablePhotos.keys.toImmutableList()
        )
    }
    val photos by remember(uiState.dayGroupedSelectablePhotos) {
        mutableStateOf(
            uiState.dayGroupedSelectablePhotos.values
                .map { it.toImmutableList() }
                .toImmutableList()
        )
    }
    val pagerState = rememberPagerState(pageCount = { days.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onDayChanged(days[page])
        }
    }

    Column(modifier) {
        FilterBar(
            modifier = Modifier.fillMaxWidth(),
            currentDayIndex = pagerState.currentPage,
            days = days,
            filterMode = uiState.filterMode,
            onFilterModeClicked = onFilterModeClicked,
            onDayClicked = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(days.indexOf(it))
                }
            }
        )
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            beyondViewportPageCount = 1,
            reverseLayout = true,
        ) { page ->
            SelectablePhotosLazyGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .pagerScaleTransition(page, pagerState),
                selectablePhotos = photos[page],
                isSelectionMode = uiState.isSelectionMode,
                noticeContent = when {
                    shouldShowPermissionMissingNotice -> {
                        {
                            Notice(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(id = R.string.storage_photos_permission_notice),
                                actionText = stringResource(id = R.string.grant_permission_button_label),
                                onActionClicked = onPermissionNoticeActionClicked,
                                onIgnoreClicked = onPermissionNoticeIgnoreClicked
                            )
                        }
                    }

                    else -> null
                },
                onPhotoClick = onPhotoClick,
                onPhotoLongClick = onPhotoLongClick,
            )
        }
    }
}

@Composable
private fun DevicePhotosEmptyScreen(
    isInitiated: Boolean,
    filterMode: PhotosFilterMode,
    shouldShowPermissionMissingNotice: Boolean,
    onPermissionNoticeActionClicked: () -> Unit,
    onPermissionNoticeIgnoreClicked: () -> Unit,
    onFilterModeClicked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        FilterBar(
            modifier = Modifier.fillMaxWidth(),
            days = persistentListOf(),
            filterMode = filterMode,
            onFilterModeClicked = onFilterModeClicked,
            onDayClicked = { }
        )
        AnimatedVisibility(
            visible = shouldShowPermissionMissingNotice,
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Notice(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium),
                text = stringResource(id = R.string.storage_photos_permission_notice),
                actionText = stringResource(id = R.string.grant_permission_button_label),
                onActionClicked = onPermissionNoticeActionClicked,
                onIgnoreClicked = onPermissionNoticeIgnoreClicked
            )
        }
        AnimatedVisibility(
            visible = isInitiated,
            enter = fadeIn()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.veryLarge, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Icon(
                    modifier = Modifier.size(200.dp),
                    painter = painterResource(id = R.drawable.ic_image_question_thin),
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.photos_not_found_message),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun PagerState.calculateCurrentOffsetForPage(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}

private fun Modifier.pagerScaleTransition(page: Int, pagerState: PagerState) =
    graphicsLayer {
        val pageOffset = pagerState.calculateCurrentOffsetForPage(page).absoluteValue.coerceIn(0f, 1f)

        lerp(
            start = 0.9f,
            stop = 1f,
            fraction = 1f - pageOffset
        ).let {
            scaleX = it
            scaleY = it
        }
    }

//region Preview
@PreviewLightDark
@Composable
private fun DevicePhotosStateItemsPreview() {
    val day1 = Day(1, LocalDate.of(2023, 1, 10))
    val day2 = Day(1, LocalDate.of(2023, 1, 11))
    val dateGroupedSelectablePhotos = persistentMapOf(
        day1 to listOf(
            Selectable(Photo.mock(dateTime = day1.date.atTime(10, 10)), false),
            Selectable(Photo.mock(dateTime = day1.date.atTime(10, 11)), false),
            Selectable(Photo.mock(dateTime = day1.date.atTime(10, 12)), false),
        ),
        day2 to listOf(
            Selectable(Photo.mock(dateTime = day1.date.atTime(10, 13)), false),
            Selectable(Photo.mock(dateTime = day1.date.atTime(10, 14)), false),
        )
    )
    AppBackground {
        DevicePhotosScreenContent(
            uiState = UiState(
                dayGroupedSelectablePhotos = dateGroupedSelectablePhotos,
                isLoading = false,
                isOnline = false,
                isSelectionMode = false,
                isInitiated = true,
                selectedCount = 0,
                filterMode = PhotosFilterMode.ALL,
            ),
            shouldShowPermissionMissingNotice = true,
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }

}

@PreviewLightDark
@Composable
private fun DevicePhotosStateSelectedItemsPreview() {
    val day1 = Day(1, LocalDate.of(2023, 1, 10))
    val day2 = Day(1, LocalDate.of(2023, 1, 11))
    val dateGroupedSelectablePhotos = persistentMapOf(
        day1 to listOf(
            Selectable(Photo.mock(dateTime = day1.date.atTime(10, 10)), true),
            Selectable(Photo.mock(dateTime = day1.date.atTime(10, 11)), true),
            Selectable(Photo.mock(dateTime = day1.date.atTime(10, 12)), false),
        ),
        day2 to listOf(
            Selectable(Photo.mock(dateTime = day2.date.atTime(10, 13)), false),
            Selectable(Photo.mock(dateTime = day2.date.atTime(10, 14)), false),
        )
    )
    AppBackground {
        DevicePhotosScreenContent(
            uiState = UiState(
                dayGroupedSelectablePhotos = dateGroupedSelectablePhotos,
                isLoading = false,
                isOnline = true,
                isSelectionMode = true,
                isInitiated = true,
                selectedCount = 2,
                filterMode = PhotosFilterMode.ALL,
            ),
            shouldShowPermissionMissingNotice = false,
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@PreviewLightDark
@Composable
private fun DevicePhotosStateSuccessNoItemsPreview() {
    AppBackground {
        DevicePhotosScreenContent(
            UiState(
                dayGroupedSelectablePhotos = persistentMapOf(),
                isLoading = false,
                isOnline = true,
                isSelectionMode = false,
                isInitiated = true,
                selectedCount = 0,
                filterMode = PhotosFilterMode.ALL,
            ),
            shouldShowPermissionMissingNotice = false,
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@PreviewLightDark
@Composable
private fun DevicePhotosStateSuccessNoItemsWithNoticePreview() {
    AppBackground {
        DevicePhotosScreenContent(
            UiState(
                dayGroupedSelectablePhotos = persistentMapOf(),
                isLoading = false,
                isOnline = true,
                isSelectionMode = false,
                isInitiated = true,
                selectedCount = 0,
                filterMode = PhotosFilterMode.ALL,
            ),
            shouldShowPermissionMissingNotice = true,
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
//endregion