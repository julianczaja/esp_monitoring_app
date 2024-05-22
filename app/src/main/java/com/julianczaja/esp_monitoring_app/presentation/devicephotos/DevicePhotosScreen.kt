package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.PermissionRationaleDialog
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
import com.julianczaja.esp_monitoring_app.domain.model.SelectablePhoto
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import java.time.LocalDate
import java.time.LocalDateTime


const val DEFAULT_PHOTO_HEIGHT = 150

@Composable
fun DevicePhotosScreen(
    snackbarHostState: SnackbarHostState,
    navigateToPhotoPreview: (Long, String) -> Unit,
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

                is Event.NavigateToPhotoPreview -> navigateToPhotoPreview(event.photo.deviceId, event.photo.fileName)
                is Event.NavigateToRemovePhotosDialog -> navigateToRemovePhotosDialog(event.photos)
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
        removeSelectedPhotos = viewModel::removeSelectedPhotos,
        onPhotoClick = viewModel::onPhotoClick,
        onPhotoLongClick = viewModel::onPhotoLongClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicePhotosScreenContent(
    uiState: UiState,
    updatePhotos: () -> Unit,
    resetSelections: () -> Unit,
    saveSelectedPhotos: () -> Unit,
    removeSelectedPhotos: () -> Unit,
    onPhotoClick: (SelectablePhoto) -> Unit,
    onPhotoLongClick: (SelectablePhoto) -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState(enabled = { uiState.isOnline })

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            updatePhotos()
        }
    }
    if (!uiState.isLoading) {
        LaunchedEffect(true) {
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            StateBar(isVisible = !uiState.isOnline, title = R.string.you_are_offline)
            SelectedEditBar(uiState.isSelectionMode, removeSelectedPhotos, saveSelectedPhotos, resetSelections)

            when (uiState.dateGroupedSelectablePhotos.isEmpty()) {
                true -> DevicePhotosEmptyScreen()
                false -> SelectablePhotosLazyGrid(
                    dateGroupedSelectablePhotos = uiState.dateGroupedSelectablePhotos,
                    isSelectionMode = uiState.isSelectionMode,
                    onPhotoClick = onPhotoClick,
                    onPhotoLongClick = onPhotoLongClick
                )
            }
        }
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullRefreshState,
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
            .padding(MaterialTheme.spacing.large)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.no_photos_taken_by_this_device),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}


//region Preview
@Preview(showSystemUi = true)
@Composable
private fun DevicePhotosStateItemsPreview() {
    AppBackground {
        val dateGroupedSelectablePhotos = mapOf(
            LocalDate.of(2023, 1, 1) to listOf(
                SelectablePhoto(
                    photo = Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 10), "fileName 1", "1600x1200", "url"),
                    isSelected = false
                ),
                SelectablePhoto(
                    photo = Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 11), "fileName 2", "1600x1200", "url"),
                    isSelected = false
                ),
                SelectablePhoto(
                    photo = Photo(123L, LocalDateTime.of(2023, 1, 1, 10, 12), "fileName 3", "1600x1200", "url"),
                    isSelected = false
                ),
            ),
            LocalDate.of(2023, 1, 2) to listOf(
                SelectablePhoto(
                    photo = Photo(123L, LocalDateTime.of(2023, 1, 2, 10, 13), "fileName 4", "1600x1200", "url"),
                    isSelected = false
                ),
                SelectablePhoto(
                    photo = Photo(123L, LocalDateTime.of(2023, 1, 2, 10, 14), "fileName 5", "1600x1200", "url"),
                    isSelected = false
                ),
            )
        )
        DevicePhotosScreenContent(
            uiState = UiState(
                dateGroupedSelectablePhotos = dateGroupedSelectablePhotos,
                isLoading = false,
                isOnline = false,
                isSelectionMode = false,
            ),
            {}, {}, {}, {}, {}, {},
        )
    }

}

@Preview(showSystemUi = true)
@Composable
private fun DevicePhotosStateSuccessNoItemsPreview() {
    AppBackground {
        DevicePhotosScreenContent(
            UiState(
                dateGroupedSelectablePhotos = emptyMap(),
                isLoading = false,
                isOnline = true,
                isSelectionMode = false,
            ),
            {}, {}, {}, {}, {}, {},
        )
    }
}
//endregion