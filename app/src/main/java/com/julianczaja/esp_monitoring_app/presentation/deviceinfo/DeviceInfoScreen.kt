package com.julianczaja.esp_monitoring_app.presentation.deviceinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.StateBar
import com.julianczaja.esp_monitoring_app.components.StrokePieChart
import com.julianczaja.esp_monitoring_app.components.SwitchWithLabel
import com.julianczaja.esp_monitoring_app.data.utils.getClampedPercent
import com.julianczaja.esp_monitoring_app.data.utils.millisToDefaultFormatLocalDateTime
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.domain.model.DeviceInfo
import com.julianczaja.esp_monitoring_app.domain.model.DeviceServerSettings
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing

@Composable
fun DeviceInfoScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: DeviceInfoScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var refreshDataCalled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(true) {
        if (!refreshDataCalled) {
            viewModel.refreshData()
            refreshDataCalled = true
        }
        viewModel.eventFlow.collect { event ->
            when (event) {
                is DeviceInfoScreenViewModel.Event.ShowError -> snackbarHostState.showSnackbar(
                    message = context.getString(event.messageId),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    DeviceInfoScreenContent(
        uiState = uiState,
        updateDeviceInfo = viewModel::refreshData,
        onDetectMostlyBlackPhotosChange = viewModel::onDetectMostlyBlackPhotosChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceInfoScreenContent(
    modifier: Modifier = Modifier,
    uiState: DeviceInfoScreenViewModel.UiState,
    updateDeviceInfo: () -> Unit,
    onDetectMostlyBlackPhotosChange: (Boolean) -> Unit
) {
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = uiState.isLoading,
        onRefresh = updateDeviceInfo
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())

        ) {
            StateBar(isVisible = !uiState.isOnline, title = R.string.you_are_offline)
            uiState.deviceInfo?.let {
                DeviceInfoContent(
                    modifier = Modifier.padding(MaterialTheme.spacing.large),
                    deviceInfo = it
                )
            }
            uiState.deviceServerSettings?.let {
                HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))
                DeviceServerSettingsContent(
                    modifier = Modifier.padding(MaterialTheme.spacing.large),
                    deviceServerSettings = it,
                    isLoading = uiState.isLoading,
                    onDetectMostlyBlackPhotosChange = onDetectMostlyBlackPhotosChange
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoContent(
    modifier: Modifier = Modifier,
    deviceInfo: DeviceInfo
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.device_info_id_format, deviceInfo.deviceId),
            style = MaterialTheme.typography.headlineSmall
        )

        StrokePieChart(
            modifier = Modifier
                .size(200.dp)
                .padding(MaterialTheme.spacing.large),
            value = deviceInfo.usedSpaceMb,
            maxValue = deviceInfo.spaceLimitMb,
            text = stringResource(R.string.device_info_percent_used_format)
                .format(getClampedPercent(deviceInfo.usedSpaceMb, deviceInfo.spaceLimitMb))
        )
        Text(text = stringResource(R.string.device_info_free_space_format).format(deviceInfo.freeSpaceMb))
        Text(text = stringResource(R.string.device_info_used_space_format).format(deviceInfo.usedSpaceMb))
        Text(text = stringResource(R.string.device_info_space_limit_format).format(deviceInfo.spaceLimitMb))

        HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))

        SpacedTextRow(
            title = stringResource(R.string.device_info_last_photo_size_label),
            value = stringResource(R.string.accurate_float_mb).format(deviceInfo.lastPhotoSizeMb)
        )
        SpacedTextRow(
            title = stringResource(R.string.device_info_average_photo_size_label),
            value = stringResource(R.string.accurate_float_mb).format(deviceInfo.averagePhotoSizeMb)
        )

        HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))

        SpacedTextRow(
            title = stringResource(R.string.device_info_photos_count_label),
            value = deviceInfo.photosCount.toString()
        )
        deviceInfo.newestPhotoTimestamp?.let { timestamp ->
            val newestPhotoString = timestamp.millisToDefaultFormatLocalDateTime().toPrettyString()
            SpacedTextRow(
                title = stringResource(R.string.device_info_newest_photo_date_label),
                value = newestPhotoString
            )
        }
        deviceInfo.oldestPhotoTimestamp?.let { timestamp ->
            val oldestPhotoString = timestamp.millisToDefaultFormatLocalDateTime().toPrettyString()
            SpacedTextRow(
                title = stringResource(R.string.device_info_oldest_photo_date_label),
                value = oldestPhotoString
            )
        }
    }
}

@Composable
private fun DeviceServerSettingsContent(
    modifier: Modifier = Modifier,
    deviceServerSettings: DeviceServerSettings,
    isLoading: Boolean,
    onDetectMostlyBlackPhotosChange: (Boolean) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall
        )
        SwitchWithLabel(
            modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
            label = "Detect mostly black photos",
            isChecked = deviceServerSettings.detectMostlyBlackPhotos,
            enabled = !isLoading,
            onCheckedChange = onDetectMostlyBlackPhotosChange
        )
    }
}

@Composable
private fun SpacedTextRow(
    title: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title)
        Text(text = value)
    }
}


//region Preview
@PreviewLightDark
@Composable
private fun DeviceInfoScreenContentPreview() {
    AppBackground {
        DeviceInfoScreenContent(
            uiState = DeviceInfoScreenViewModel.UiState(
                deviceInfo = DeviceInfo(
                    deviceId = 1L,
                    freeSpaceMb = 40.877f,
                    usedSpaceMb = 59.123f,
                    spaceLimitMb = 100.0f,
                    lastPhotoSizeMb = 1.5612f,
                    averagePhotoSizeMb = 1.313f,
                    photosCount = 125,
                    newestPhotoTimestamp = 1717590096488L,
                    oldestPhotoTimestamp = 1712250092422L
                ),
                deviceServerSettings = DeviceServerSettings(
                    detectMostlyBlackPhotos = true
                ),
                isLoading = false,
                isOnline = true
            ),
            updateDeviceInfo = {},
            onDetectMostlyBlackPhotosChange = {}
        )
    }
}
//endregion
