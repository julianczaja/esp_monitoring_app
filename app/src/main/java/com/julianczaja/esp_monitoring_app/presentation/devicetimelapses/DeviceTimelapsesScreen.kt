package com.julianczaja.esp_monitoring_app.presentation.devicetimelapses

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.GrantPermissionButton
import com.julianczaja.esp_monitoring_app.components.PermissionRationaleDialog
import com.julianczaja.esp_monitoring_app.components.PhotoInfoRow
import com.julianczaja.esp_monitoring_app.data.utils.formatBytes
import com.julianczaja.esp_monitoring_app.data.utils.getActivity
import com.julianczaja.esp_monitoring_app.data.utils.getPermissionState
import com.julianczaja.esp_monitoring_app.data.utils.getReadExternalStoragePermissionName
import com.julianczaja.esp_monitoring_app.data.utils.openAppSettings
import com.julianczaja.esp_monitoring_app.data.utils.toDayMonthYearString
import com.julianczaja.esp_monitoring_app.domain.model.PermissionState
import com.julianczaja.esp_monitoring_app.domain.model.Timelapse
import com.julianczaja.esp_monitoring_app.domain.model.TimelapseData
import com.julianczaja.esp_monitoring_app.presentation.devicetimelapses.DeviceTimelapsesScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.devicetimelapses.components.TimelapsePreviewDialog
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import java.time.LocalDateTime

@Composable
fun DeviceTimelapsesScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: DeviceTimelapsesScreenViewModel = hiltViewModel()
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
            }
        }
    }
    if (storagePermissionState == PermissionState.GRANTED) {
        DeviceTimelapsesScreenContent(
            modifier = Modifier.fillMaxSize(),
            timelapses = uiState.timelapses,
            isLoading = uiState.isLoading,
            onRefreshTriggered = viewModel::updateTimelapses,
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
private fun DeviceTimelapsesScreenContent(
    modifier: Modifier = Modifier,
    timelapses: List<Timelapse>,
    isLoading: Boolean,
    onRefreshTriggered: () -> Unit
) {
    LaunchedEffect(true) {
        onRefreshTriggered.invoke()
    }

    var isTimelapsePreviewDialogVisible by remember { mutableStateOf(false) }
    var timelapseData: TimelapseData? by remember { mutableStateOf(null) }

    if (isTimelapsePreviewDialogVisible) {
        timelapseData?.let {
            TimelapsePreviewDialog(
                timelapseData = it,
                onDismiss = {
                    timelapseData = null
                    isTimelapsePreviewDialogVisible = false
                }
            )
        }
    }

    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isLoading,
        onRefresh = onRefreshTriggered
    ) {
        when (timelapses.isEmpty()) {
            true -> EmptyScreen(modifier)
            false -> TimelapsesScreen(
                modifier = modifier,
                timelapses = timelapses,
                onTimelapseClicked = {
                    timelapseData = it
                    isTimelapsePreviewDialogVisible = true
                }
            )
        }
    }
}

@Composable
private fun TimelapsesScreen(
    modifier: Modifier,
    timelapses: List<Timelapse>,
    onTimelapseClicked: (TimelapseData) -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(items = timelapses, key = { it.data.path }) {
            Box {
                AsyncImage(
                    contentScale = ContentScale.FillWidth,
                    modifier = modifier
                        .height(200.dp)
                        .clip(RoundedCornerShape(MaterialTheme.shape.photoCorners))
                        .clickable(onClick = { onTimelapseClicked(it.data) }),
                    imageLoader = imageLoader,
                    model = ImageRequest.Builder(context)
                        .data(it.data.path)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .build(),
                    contentDescription = null
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .3f))
                        .align(Alignment.Center),
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(MaterialTheme.spacing.extraLarge)
                            .size(50.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.surface,
                        painter = painterResource(id = R.drawable.ic_play),
                        contentDescription = null
                    )
                }
                PhotoInfoRow(
                    listOf(
                        stringResource(id = R.string.date_format, it.addedDateTime.toDayMonthYearString()),
                        stringResource(id = R.string.duration_seconds_format, it.data.durationSeconds),
                        stringResource(id = R.string.size_format, formatBytes(it.data.sizeBytes)),
                    )
                )
            }
        }
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
            painter = painterResource(id = R.drawable.ic_timelapse),
            contentDescription = null
        )
        Text(
            text = stringResource(R.string.no_timelapses_saved_message),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun DeviceTimelapsesSuccessPreview() {
    AppBackground {
        DeviceTimelapsesScreenContent(
            modifier = Modifier.fillMaxSize(),
            timelapses = listOf(
                Timelapse(
                    addedDateTime = LocalDateTime.now(),
                    data = TimelapseData(path = "path", sizeBytes = 5000, durationSeconds = 3.5f)
                )
            ),
            isLoading = false,
            onRefreshTriggered = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceTimelapsesSuccessNoItemsPreview() {
    AppBackground {
        EmptyScreen(
            modifier = Modifier.fillMaxSize()
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceTimelapsesPermissionRequiredPreview() {
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
