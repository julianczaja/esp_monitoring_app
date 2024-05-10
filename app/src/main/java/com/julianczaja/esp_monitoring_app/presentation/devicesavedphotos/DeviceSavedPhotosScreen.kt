package com.julianczaja.esp_monitoring_app.presentation.devicesavedphotos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.PermissionRationaleDialog
import com.julianczaja.esp_monitoring_app.components.PhotoCoilImage
import com.julianczaja.esp_monitoring_app.data.utils.checkPermissionAndDoAction
import com.julianczaja.esp_monitoring_app.data.utils.getActivity
import com.julianczaja.esp_monitoring_app.data.utils.getPermissionState
import com.julianczaja.esp_monitoring_app.data.utils.getReadExternalStoragePermissionName
import com.julianczaja.esp_monitoring_app.data.utils.openAppSettings
import com.julianczaja.esp_monitoring_app.domain.model.PermissionState
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.presentation.devicesavedphotos.DeviceSavedPhotosScreenViewModel.Event

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
    savedPhotos: List<Uri>,
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
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        LazyColumn {
            items(savedPhotos) { uri ->
                PhotoCoilImage(data = uri, height = 100.dp)
            }
        }
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullRefreshState,
        )
    }
}
