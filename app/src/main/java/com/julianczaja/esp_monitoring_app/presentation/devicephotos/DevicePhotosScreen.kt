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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.Notice
import com.julianczaja.esp_monitoring_app.components.PermissionRationaleDialog
import com.julianczaja.esp_monitoring_app.components.SelectedEditBar
import com.julianczaja.esp_monitoring_app.components.StateBar
import com.julianczaja.esp_monitoring_app.data.utils.getActivity
import com.julianczaja.esp_monitoring_app.data.utils.getPermissionState
import com.julianczaja.esp_monitoring_app.data.utils.getReadExternalStorageImagesPermissionName
import com.julianczaja.esp_monitoring_app.data.utils.openAppSettings
import com.julianczaja.esp_monitoring_app.data.utils.toLocalDate
import com.julianczaja.esp_monitoring_app.domain.model.PermissionState
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.PhotosFilterMode
import com.julianczaja.esp_monitoring_app.domain.model.Selectable
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.components.FilterBar
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.components.SelectablePhotosLazyGrid
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate


const val DEFAULT_PHOTO_WIDTH = 150

@Composable
fun DevicePhotosScreen(
    snackbarHostState: SnackbarHostState,
    navigateToPhotoPreview: (Int) -> Unit,
    navigateToTimelapseCreatorScreen: (List<Photo>) -> Unit,
    navigateToRemovePhotosDialog: (List<Photo>) -> Unit,
    navigateToSavePhotosDialog: (List<Photo>) -> Unit,
    viewModel: DevicePhotosScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.devicePhotosUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var updatePhotosCalled by rememberSaveable { mutableStateOf(false) }

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
                    viewModel.updatePhotos()
                    viewModel.updateSavedPhotos()
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
                is Event.NavigateToTimelapseCreatorScreen -> navigateToTimelapseCreatorScreen(event.photos)
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
                if (storagePermissionState == PermissionState.RATIONALE_NEEDED) {
                    readExternalStoragePermissionLauncher.launch(storagePermissionName)
                } else {
                    context.getActivity().openAppSettings()
                }
            },
            onDismiss = { shouldShowPermissionRationaleDialog = false }
        )
    }

    DevicePhotosScreenContent(
        uiState = uiState,
        shouldShowPermissionMissingNotice = shouldShowPermissionMissingNotice,
        updatePhotos = viewModel::updatePhotos,
        resetSelections = viewModel::resetSelectedPhotos,
        saveSelectedPhotos = viewModel::saveSelectedPhotos,
        createTimelapseFromSelectedPhotos = viewModel::createTimelapseFromSelectedPhotos,
        removeSelectedPhotos = viewModel::removeSelectedPhotos,
        onPhotoClick = viewModel::onPhotoClick,
        onPhotoLongClick = viewModel::onPhotoLongClick,
        onFilterDateClicked = viewModel::onFilterDateClicked,
        onSelectDeselectAllClick = viewModel::onSelectDeselectAllClicked,
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
    updatePhotos: () -> Unit,
    resetSelections: () -> Unit,
    saveSelectedPhotos: () -> Unit,
    createTimelapseFromSelectedPhotos: () -> Unit,
    removeSelectedPhotos: () -> Unit,
    onPhotoClick: (Selectable<Photo>) -> Unit,
    onPhotoLongClick: (Selectable<Photo>) -> Unit,
    onFilterDateClicked: (Selectable<LocalDate>) -> Unit,
    onSelectDeselectAllClick: (LocalDate) -> Unit,
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
            resetSelections = resetSelections
        )
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = uiState.isLoading,
            onRefresh = updatePhotos
        ) {
            when (uiState.dateGroupedSelectablePhotos.isEmpty()) {
                true -> DevicePhotosEmptyScreen(
                    isRefreshed = uiState.isRefreshed,
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
                    onFilterDateClicked = onFilterDateClicked,
                    onSelectDeselectAllClick = onSelectDeselectAllClick,
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
    onFilterDateClicked: (Selectable<LocalDate>) -> Unit,
    onFilterModeClicked: () -> Unit,
    onSelectDeselectAllClick: (LocalDate) -> Unit,
    onPermissionNoticeActionClicked: () -> Unit,
    onPermissionNoticeIgnoreClicked: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val lazyGridState = rememberLazyGridState()
    val visibleItemKey by remember {
        derivedStateOf {
            val layoutInfo = lazyGridState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo

            if (layoutInfo.totalItemsCount == 0) return@derivedStateOf null

            val visibleKey = if (visibleItemsInfo.last().index == layoutInfo.totalItemsCount - 1) {
                visibleItemsInfo.last()
            } else {
                visibleItemsInfo.first()
            }
            return@derivedStateOf try {
                visibleKey.key.toString()
                    .split("|")
                    .first()
                    .toLocalDate()
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(modifier) {
        FilterBar(
            modifier = Modifier.fillMaxWidth(),
            dates = uiState.selectableFilterDates,
            highlightedDate = visibleItemKey,
            filterMode = uiState.filterMode,
            onFilterModeClicked = onFilterModeClicked,
            onDateClicked = {
                onFilterDateClicked(it)
                coroutineScope.launch {
                    delay(100) // fixme: wait until filtering is finished
                    lazyGridState.scrollToItem(0)
                }
            }
        )
        SelectablePhotosLazyGrid(
            modifier = Modifier.fillMaxSize(),
            dateGroupedSelectablePhotos = uiState.dateGroupedSelectablePhotos,
            isSelectionMode = uiState.isSelectionMode,
            state = lazyGridState,
            noticeContent = if (shouldShowPermissionMissingNotice) {
                {
                    Notice(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.storage_photos_permission_notice),
                        actionText = stringResource(id = R.string.grant_permission_button_label),
                        onActionClicked = onPermissionNoticeActionClicked,
                        onIgnoreClicked = onPermissionNoticeIgnoreClicked
                    )
                }
            } else null,
            onPhotoClick = onPhotoClick,
            onPhotoLongClick = onPhotoLongClick,
            onSelectDeselectAllClick = onSelectDeselectAllClick
        )
    }
}

@Composable
private fun DevicePhotosEmptyScreen(
    isRefreshed: Boolean,
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
            dates = persistentListOf(),
            filterMode = filterMode,
            onFilterModeClicked = onFilterModeClicked,
            onDateClicked = { }
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
            visible = isRefreshed,
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

//region Preview
@PreviewLightDark
@Composable
private fun DevicePhotosStateItemsPreview() {
    val date1 = LocalDate.of(2023, 1, 10)
    val date2 = LocalDate.of(2023, 1, 11)
    val dateGroupedSelectablePhotos = persistentMapOf(
        date1 to listOf(
            Selectable(Photo.mock(dateTime = date1.atTime(10, 10)), false),
            Selectable(Photo.mock(dateTime = date1.atTime(10, 11)), false),
            Selectable(Photo.mock(dateTime = date1.atTime(10, 12)), false),
        ),
        date2 to listOf(
            Selectable(Photo.mock(dateTime = date1.atTime(10, 13)), false),
            Selectable(Photo.mock(dateTime = date1.atTime(10, 14)), false),
        )
    )
    AppBackground {
        DevicePhotosScreenContent(
            uiState = UiState(
                dateGroupedSelectablePhotos = dateGroupedSelectablePhotos,
                isLoading = false,
                isOnline = false,
                isSelectionMode = false,
                selectableFilterDates = persistentListOf(
                    Selectable(date2, false),
                    Selectable(date1, false),
                ),
                isRefreshed = true,
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
    val date1 = LocalDate.of(2023, 1, 10)
    val date2 = LocalDate.of(2023, 1, 11)
    val dateGroupedSelectablePhotos = persistentMapOf(
        date1 to listOf(
            Selectable(Photo.mock(dateTime = date1.atTime(10, 10)), true),
            Selectable(Photo.mock(dateTime = date1.atTime(10, 11)), true),
            Selectable(Photo.mock(dateTime = date1.atTime(10, 12)), false),
        ),
        date2 to listOf(
            Selectable(Photo.mock(dateTime = date1.atTime(10, 13)), false),
            Selectable(Photo.mock(dateTime = date1.atTime(10, 14)), false),
        )
    )
    AppBackground {
        DevicePhotosScreenContent(
            uiState = UiState(
                dateGroupedSelectablePhotos = dateGroupedSelectablePhotos,
                isLoading = false,
                isOnline = true,
                isSelectionMode = true,
                selectableFilterDates = persistentListOf(
                    Selectable(date2, false),
                    Selectable(date1, false),
                ),
                isRefreshed = true,
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
                dateGroupedSelectablePhotos = persistentMapOf(),
                isLoading = false,
                isOnline = true,
                isSelectionMode = false,
                selectableFilterDates = persistentListOf(),
                isRefreshed = true,
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
                dateGroupedSelectablePhotos = persistentMapOf(),
                isLoading = false,
                isOnline = true,
                isSelectionMode = false,
                selectableFilterDates = persistentListOf(),
                isRefreshed = true,
                selectedCount = 0,
                filterMode = PhotosFilterMode.ALL,
            ),
            shouldShowPermissionMissingNotice = true,
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
//endregion