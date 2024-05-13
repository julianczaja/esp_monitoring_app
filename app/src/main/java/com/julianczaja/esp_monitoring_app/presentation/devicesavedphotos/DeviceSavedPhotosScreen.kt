package com.julianczaja.esp_monitoring_app.presentation.devicesavedphotos

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
import com.julianczaja.esp_monitoring_app.data.utils.checkPermissionAndDoAction
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
    val savedPhotos by viewModel.savedPhotos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val storagePermissionName = getReadExternalStoragePermissionName()
    var storagePermissionState by rememberSaveable { mutableStateOf<PermissionState?>(null) }

    val readExternalStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            when (isGranted) {
                true -> viewModel.updateSavedPhotos()
                false -> {
                    storagePermissionState = context.getActivity().getPermissionState(storagePermissionName)
                }
            }
        }
    )

    LaunchedEffect(true) {
        checkPermissionAndDoAction(
            context = context,
            permission = storagePermissionName,
            onGranted = viewModel::updateSavedPhotos,
            onDenied = { readExternalStoragePermissionLauncher.launch(storagePermissionName) }
        )
        viewModel.eventFlow.collect { event ->
            when (event) {
                is Event.ShowError -> snackbarHostState.showSnackbar(
                    message = context.getString(event.messageId),
                    duration = SnackbarDuration.Short
                )

                is Event.NavigateToPhotoPreview -> navigateToPhotoPreview(DeviceIdArgs.NO_VALUE, event.photo.fileName)
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

    DeviceSavedPhotosScreenContent(
        savedPhotos = savedPhotos,
        isLoading = isLoading,
        onRefreshTriggered = {
            checkPermissionAndDoAction(
                context = context,
                permission = storagePermissionName,
                onGranted = viewModel::updateSavedPhotos,
                onDenied = { readExternalStoragePermissionLauncher.launch(storagePermissionName) }
            )
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSavedPhotosScreenContent(
    modifier: Modifier = Modifier,
    savedPhotos: Map<LocalDate, List<SelectablePhoto>>,
    isLoading: Boolean,
    onRefreshTriggered: () -> Unit,
) {
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
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        Column {
            SelectedEditBar(
                isSelectionMode = false, // fixme
                removeSelectedPhotos = {},
                saveSelectedPhotos = {},
                resetSelections = {}
            )
            when (savedPhotos.isEmpty()) {
                true -> EmptyScreen(modifier.fillMaxSize())
                false -> SelectablePhotosLazyGrid(
                    dateGroupedSelectablePhotos = savedPhotos,
                    isSelectionMode = false, // fixme
                    onPhotoClick = {},
                    onPhotoLongClick = {}
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(MaterialTheme.spacing.large)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.no_photos_saved_from_this_device),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}

//region Preview
@Preview(showSystemUi = true)
@Composable
private fun DevicePhotosStateSuccessNoItemsPreview() {
    AppBackground {
        EmptyScreen()
    }
}
//endregion
