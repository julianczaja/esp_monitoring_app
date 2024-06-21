package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.julianczaja.esp_monitoring_app.components.PermissionRationaleDialog
import com.julianczaja.esp_monitoring_app.components.PhotosDateFilterBar
import com.julianczaja.esp_monitoring_app.components.SelectablePhotosLazyGrid
import com.julianczaja.esp_monitoring_app.components.SelectedEditBar
import com.julianczaja.esp_monitoring_app.components.StateBar
import com.julianczaja.esp_monitoring_app.data.utils.checkPermissionAndDoAction
import com.julianczaja.esp_monitoring_app.data.utils.getActivity
import com.julianczaja.esp_monitoring_app.data.utils.getPermissionState
import com.julianczaja.esp_monitoring_app.data.utils.getReadExternalStoragePermissionName
import com.julianczaja.esp_monitoring_app.data.utils.openAppSettings
import com.julianczaja.esp_monitoring_app.domain.model.PermissionState
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.SelectableLocalDate
import com.julianczaja.esp_monitoring_app.domain.model.SelectablePhoto
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime


const val DEFAULT_PHOTO_WIDTH = 150

@Composable
fun DevicePhotosScreen(
    snackbarHostState: SnackbarHostState,
    navigateToPhotoPreview: (Long, String) -> Unit,
    navigateToTimelapseCreatorScreen: (List<Photo>) -> Unit,
    navigateToRemovePhotosDialog: (List<String>) -> Unit,
    viewModel: DevicePhotosScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.devicePhotosUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val storagePermissionName = getReadExternalStoragePermissionName()
    var storagePermissionState by rememberSaveable { mutableStateOf<PermissionState?>(null) }
    var updatePhotosCalled by rememberSaveable { mutableStateOf(false) }

    val readExternalStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            when (isGranted) {
                true -> viewModel.saveSelectedPhotos()
                false -> {
                    storagePermissionState = context.getActivity().getPermissionState(storagePermissionName)
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

                is Event.NavigateToPhotoPreview -> navigateToPhotoPreview(event.photo.deviceId, event.photo.fileName)
                is Event.NavigateToRemovePhotosDialog -> navigateToRemovePhotosDialog(event.photos)
                is Event.NavigateToTimelapseCreatorScreen -> navigateToTimelapseCreatorScreen(event.photos)
            }
        }
    }

    storagePermissionState?.let { permissionState ->
        PermissionRationaleDialog(
            title = R.string.storage_permission_needed_title,
            bodyRationale = R.string.storage_permission_rationale_body,
            bodyDenied = R.string.storage_permission_denied_body,
            permissionState = permissionState,
            onRequestPermission = {
                if (permissionState == PermissionState.RATIONALE_NEEDED) {
                    readExternalStoragePermissionLauncher.launch(storagePermissionName)
                } else {
                    context.getActivity().openAppSettings()
                }
                storagePermissionState = null
            },
            onDismiss = { storagePermissionState = null }
        )
    }

    DevicePhotosScreenContent(
        uiState = uiState,
        updatePhotos = viewModel::updatePhotos,
        resetSelections = viewModel::resetSelectedPhotos,
        saveSelectedPhotos = {
            checkPermissionAndDoAction(
                context = context,
                permission = storagePermissionName,
                onGranted = viewModel::saveSelectedPhotos,
                onDenied = { readExternalStoragePermissionLauncher.launch(storagePermissionName) }
            )
        },
        createTimelapseFromSelectedPhotos = viewModel::createTimelapseFromSelectedPhotos,
        removeSelectedPhotos = viewModel::removeSelectedPhotos,
        onPhotoClick = viewModel::onPhotoClick,
        onPhotoLongClick = viewModel::onPhotoLongClick,
        onFilterDateClicked = viewModel::onFilterDateClicked,
        onSelectDeselectAllClick = viewModel::onSelectDeselectAllClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicePhotosScreenContent(
    uiState: UiState,
    updatePhotos: () -> Unit,
    resetSelections: () -> Unit,
    saveSelectedPhotos: () -> Unit,
    createTimelapseFromSelectedPhotos: () -> Unit,
    removeSelectedPhotos: () -> Unit,
    onPhotoClick: (SelectablePhoto) -> Unit,
    onPhotoLongClick: (SelectablePhoto) -> Unit,
    onFilterDateClicked: (SelectableLocalDate) -> Unit,
    onSelectDeselectAllClick: (LocalDate) -> Unit,
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
                true -> DevicePhotosEmptyScreen(uiState.isRefreshed)
                false -> DevicePhotosNotEmptyScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    onPhotoClick = onPhotoClick,
                    onPhotoLongClick = onPhotoLongClick,
                    onFilterDateClicked = onFilterDateClicked,
                    onSelectDeselectAllClick = onSelectDeselectAllClick
                )
            }
        }
    }
}

@Composable
private fun DevicePhotosNotEmptyScreen(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onPhotoClick: (SelectablePhoto) -> Unit,
    onPhotoLongClick: (SelectablePhoto) -> Unit,
    onFilterDateClicked: (SelectableLocalDate) -> Unit,
    onSelectDeselectAllClick: (LocalDate) -> Unit,
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
            return@derivedStateOf (visibleKey.key as? LocalDate)
                ?: (visibleKey.key as? LocalDateTime)?.toLocalDate()
        }
    }

    Column(
        modifier = modifier
    ) {
        AnimatedVisibility(visible = uiState.selectableFilterDates.size > 1) {
            PhotosDateFilterBar(
                dates = uiState.selectableFilterDates,
                highlightedDate = visibleItemKey,
                onDateClicked = {
                    onFilterDateClicked(it)
                    coroutineScope.launch {
                        delay(100) // fixme: wait until filtering is finished
                        lazyGridState.scrollToItem(0)
                    }
                }
            )
        }
        SelectablePhotosLazyGrid(
            modifier = Modifier.fillMaxSize(),
            dateGroupedSelectablePhotos = uiState.dateGroupedSelectablePhotos,
            isSelectionMode = uiState.isSelectionMode,
            state = lazyGridState,
            onPhotoClick = onPhotoClick,
            onPhotoLongClick = onPhotoLongClick,
            onSelectDeselectAllClick = onSelectDeselectAllClick
        )
    }
}

@Composable
private fun DevicePhotosEmptyScreen(isRefreshed: Boolean) {
    AnimatedVisibility(
        visible = isRefreshed,
        enter = fadeIn()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.veryLarge, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.large)
                .verticalScroll(rememberScrollState())
        ) {
            Icon(
                modifier = Modifier.size(200.dp),
                painter = painterResource(id = R.drawable.ic_image_question),
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


//region Preview
@PreviewLightDark
@Composable
private fun DevicePhotosStateItemsPreview() {
    val date1 = LocalDate.of(2023, 1, 10)
    val date2 = LocalDate.of(2023, 1, 11)
    val dateGroupedSelectablePhotos = mapOf(
        date1 to listOf(
            SelectablePhoto(
                photo = Photo(1L, date1.atTime(10, 10), "fileName 1", "1600x1200", "url", "url"),
                isSelected = false
            ),
            SelectablePhoto(
                photo = Photo(2L, date1.atTime(10, 11), "fileName 2", "1600x1200", "url", "url"),
                isSelected = false
            ),
            SelectablePhoto(
                photo = Photo(3L, date1.atTime(10, 12), "fileName 3", "1600x1200", "url", "url"),
                isSelected = false
            ),
        ),
        date2 to listOf(
            SelectablePhoto(
                photo = Photo(4L, date2.atTime(10, 13), "fileName 4", "1600x1200", "url", "url"),
                isSelected = false
            ),
            SelectablePhoto(
                photo = Photo(5L, date2.atTime(10, 14), "fileName 5", "1600x1200", "url", "url"),
                isSelected = false
            ),
        )
    )
    AppBackground {
        DevicePhotosScreenContent(
            uiState = UiState(
                dateGroupedSelectablePhotos = dateGroupedSelectablePhotos,
                isLoading = false,
                isOnline = false,
                isSelectionMode = false,
                selectableFilterDates = listOf(
                    SelectableLocalDate(date2, false),
                    SelectableLocalDate(date1, false),
                ),
                isRefreshed = true,
                selectedCount = 0
            ),
            {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }

}

@PreviewLightDark
@Composable
private fun DevicePhotosStateSelectedItemsPreview() {
    val date1 = LocalDate.of(2023, 1, 10)
    val date2 = LocalDate.of(2023, 1, 11)
    val dateGroupedSelectablePhotos = mapOf(
        date1 to listOf(
            SelectablePhoto(
                photo = Photo(1L, date1.atTime(10, 10), "fileName 1", "1600x1200", "url", "url"),
                isSelected = true
            ),
            SelectablePhoto(
                photo = Photo(2L, date1.atTime(10, 11), "fileName 2", "1600x1200", "url", "url"),
                isSelected = true
            ),
            SelectablePhoto(
                photo = Photo(3L, date1.atTime(10, 12), "fileName 3", "1600x1200", "url", "url"),
                isSelected = false
            ),
        ),
        date2 to listOf(
            SelectablePhoto(
                photo = Photo(4L, date2.atTime(10, 13), "fileName 4", "1600x1200", "url", "url"),
                isSelected = false
            ),
            SelectablePhoto(
                photo = Photo(5L, date2.atTime(10, 14), "fileName 5", "1600x1200", "url", "url"),
                isSelected = false
            ),
        )
    )
    AppBackground {
        DevicePhotosScreenContent(
            uiState = UiState(
                dateGroupedSelectablePhotos = dateGroupedSelectablePhotos,
                isLoading = false,
                isOnline = true,
                isSelectionMode = true,
                selectableFilterDates = listOf(
                    SelectableLocalDate(date2, false),
                    SelectableLocalDate(date1, false),
                ),
                isRefreshed = true,
                selectedCount = 2
            ),
            {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@PreviewLightDark
@Composable
private fun DevicePhotosStateSuccessNoItemsPreview() {
    AppBackground {
        DevicePhotosScreenContent(
            UiState(
                dateGroupedSelectablePhotos = emptyMap(),
                isLoading = false,
                isOnline = true,
                isSelectionMode = false,
                selectableFilterDates = emptyList(),
                isRefreshed = true,
                selectedCount = 0
            ),
            {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
//endregion