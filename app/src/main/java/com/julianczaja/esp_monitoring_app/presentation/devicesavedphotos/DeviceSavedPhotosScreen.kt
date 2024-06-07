package com.julianczaja.esp_monitoring_app.presentation.devicesavedphotos

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.GrantPermissionButton
import com.julianczaja.esp_monitoring_app.components.PermissionRationaleDialog
import com.julianczaja.esp_monitoring_app.components.SelectablePhotosLazyGrid
import com.julianczaja.esp_monitoring_app.components.SelectedEditBar
import com.julianczaja.esp_monitoring_app.data.utils.getActivity
import com.julianczaja.esp_monitoring_app.data.utils.getPermissionState
import com.julianczaja.esp_monitoring_app.data.utils.getReadExternalStoragePermissionName
import com.julianczaja.esp_monitoring_app.data.utils.openAppSettings
import com.julianczaja.esp_monitoring_app.domain.model.PermissionState
import com.julianczaja.esp_monitoring_app.domain.model.SelectablePhoto
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.presentation.devicesavedphotos.DeviceSavedPhotosScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import java.time.LocalDate

@Composable
fun DeviceSavedPhotosScreen(
    snackbarHostState: SnackbarHostState,
    navigateToPhotoPreview: (Long, String) -> Unit,
    viewModel: DeviceSavedPhotosScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val storagePermissionName = getReadExternalStoragePermissionName()
    var storagePermissionState by rememberSaveable {
        mutableStateOf(
            context.getActivity().getPermissionState(storagePermissionName)
        )
    }

    LaunchedEffect(true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is Event.ShowError -> snackbarHostState.showSnackbar(
                    message = context.getString(event.messageId),
                    duration = SnackbarDuration.Short
                )

                is Event.NavigateToPhotoPreview -> navigateToPhotoPreview(DeviceIdArgs.NO_VALUE, event.photo.fileName)
                is Event.ShowRemovedInfo -> snackbarHostState.showSnackbar(
                    message = context.getString(R.string.remove_photo_from_external_storage_result_message)
                        .format(event.removedCount, event.totalCount),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    if (storagePermissionState == PermissionState.GRANTED) {
        DeviceSavedPhotosScreenContent(
            modifier = Modifier.fillMaxSize(),
            savedPhotos = uiState.dateGroupedSelectablePhotos,
            isLoading = uiState.isLoading,
            isSelectionMode = uiState.isSelectionMode,
            selectedCount = uiState.selectedCount,
            onRefreshTriggered = viewModel::updateSavedPhotos,
            resetSelections = viewModel::resetSelectedPhotos,
            removeSelectedPhotos = viewModel::removeSelectedPhotos,
            onPhotoClick = viewModel::onPhotoClick,
            onPhotoLongClick = viewModel::onPhotoLongClick,
            onSelectDeselectAllClick = viewModel::onSelectDeselectAllClicked
        )
    } else {
        PermissionsRequiredScreen(
            modifier = Modifier.fillMaxSize(),
            storagePermissionState = storagePermissionState,
            storagePermissionName = storagePermissionName,
            onStoragePermissionChanged = { storagePermissionState = it }
        )
    }
}

@Composable
private fun PermissionsRequiredScreen(
    modifier: Modifier = Modifier,
    storagePermissionState: PermissionState,
    storagePermissionName: String,
    onStoragePermissionChanged: (PermissionState) -> Unit,
) {
    val context = LocalContext.current
    var shouldShowPermissionRationaleDialog by rememberSaveable { mutableStateOf(false) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            when (isGranted) {
                true -> {
                    onStoragePermissionChanged(PermissionState.GRANTED)
                    shouldShowPermissionRationaleDialog = false
                }

                false -> {
                    onStoragePermissionChanged(context.getActivity().getPermissionState(storagePermissionName))
                    shouldShowPermissionRationaleDialog = true
                }
            }
        }
    )

    Box(modifier) {
        GrantPermissionButton(
            modifier = Modifier.align(Alignment.Center),
            titleId = R.string.storage_permission_needed_title,
            onButtonClicked = { storagePermissionLauncher.launch(storagePermissionName) }
        )
    }

    if (shouldShowPermissionRationaleDialog) {
        PermissionRationaleDialog(
            title = R.string.storage_permission_needed_title,
            bodyRationale = R.string.storage_permission_rationale_body,
            bodyDenied = R.string.storage_permission_denied_body,
            permissionState = storagePermissionState,
            onRequestPermission = {
                if (storagePermissionState == PermissionState.RATIONALE_NEEDED) {
                    storagePermissionLauncher.launch(storagePermissionName)
                } else {
                    context.getActivity().openAppSettings()
                }
            },
            onDismiss = { shouldShowPermissionRationaleDialog = false }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSavedPhotosScreenContent(
    modifier: Modifier = Modifier,
    savedPhotos: Map<LocalDate, List<SelectablePhoto>>,
    isLoading: Boolean,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onRefreshTriggered: () -> Unit,
    resetSelections: () -> Unit,
    removeSelectedPhotos: () -> Unit,
    onPhotoClick: (SelectablePhoto) -> Unit,
    onPhotoLongClick: (SelectablePhoto) -> Unit,
    onSelectDeselectAllClick: (LocalDate) -> Unit,
) {
    LaunchedEffect(true) {
        onRefreshTriggered.invoke()
    }

    BackHandler(enabled = isSelectionMode, onBack = resetSelections)

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onRefreshTriggered()
        }
    }
    if (!isLoading) {
        LaunchedEffect(true) {
            pullRefreshState.endRefresh()
        }
    }
    Box(
        modifier = modifier.nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        Column {
            SelectedEditBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedCount,
                removeSelectedPhotos = removeSelectedPhotos,
                resetSelections = resetSelections
            )
            when (savedPhotos.isEmpty()) {
                true -> EmptyScreen(modifier)
                false -> SelectablePhotosLazyGrid(
                    modifier = modifier,
                    dateGroupedSelectablePhotos = savedPhotos,
                    isSelectionMode = isSelectionMode,
                    onPhotoClick = onPhotoClick,
                    onPhotoLongClick = onPhotoLongClick,
                    onSelectDeselectAllClick = onSelectDeselectAllClick
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
private fun EmptyScreen(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.veryLarge, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(MaterialTheme.spacing.large)
            .verticalScroll(rememberScrollState())
    ) {
        Icon(
            modifier = Modifier.size(200.dp),
            painter = painterResource(id = R.drawable.ic_image_question),
            contentDescription = null
        )
        Text(
            text = stringResource(R.string.no_photos_saved_message),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun DevicePhotosStateSuccessNoItemsPreview() {
    AppBackground {
        EmptyScreen(
            modifier = Modifier.fillMaxSize()
        )
    }
}

@PreviewLightDark
@Composable
private fun DevicePhotosPermissionRequiredPreview() {
    AppBackground {
        PermissionsRequiredScreen(
            modifier = Modifier.fillMaxSize(),
            storagePermissionState = PermissionState.DENIED,
            storagePermissionName = "",
            onStoragePermissionChanged = {}
        )
    }
}
//endregion
