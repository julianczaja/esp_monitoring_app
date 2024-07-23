package com.julianczaja.esp_monitoring_app.presentation.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.DefaultAppBar
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.components.DeviceItem
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import com.julianczaja.esp_monitoring_app.presentation.widget.PhotoWidgetConfigurationViewModel.Event
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import java.time.LocalDateTime


@Composable
fun PhotoWidgetConfigurationScreen(
    appWidgetId: Int,
    onFinishWithOkResult: () -> Unit,
    viewModel: PhotoWidgetConfigurationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is Event.Error -> snackbarHostState.showSnackbar(
                    message = context.getString(event.messageId),
                    duration = SnackbarDuration.Short
                )

                is Event.FinishWithOkResult -> onFinishWithOkResult()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DefaultAppBar(
                title = stringResource(id = R.string.photo_widget_configuration_screen_title),
                shouldShowNavigationIcon = true,
                onBackClick = onFinishWithOkResult
            )
        }
    ) { padding ->
        PhotoWidgetConfigurationScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            devicesWithLastPhoto = uiState.devicesWithLastPhoto,
            isLoading = uiState.isLoading,
            onDeviceClicked = { viewModel.onDeviceClicked(it, appWidgetId) }
        )
    }
}

@Composable
private fun PhotoWidgetConfigurationScreenContent(
    modifier: Modifier = Modifier,
    devicesWithLastPhoto: ImmutableMap<Device, Photo?>,
    isLoading: Boolean,
    onDeviceClicked: (Device) -> Unit
) {
    val devicesWithLastPhotoList = remember(devicesWithLastPhoto) {
        devicesWithLastPhoto.toList()
    }
    when {
        isLoading -> Box(modifier) {
            DefaultProgressIndicator(Modifier.align(Alignment.Center))
        }

        devicesWithLastPhoto.isNotEmpty() -> Column(
            modifier = modifier
        ) {
            Text(
                modifier = Modifier.padding(MaterialTheme.spacing.large),
                text = stringResource(R.string.select_device_label),
                style = MaterialTheme.typography.headlineSmall
            )
            HorizontalDivider()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(MaterialTheme.spacing.medium)
            ) {
                items(devicesWithLastPhotoList, key = { it.first.id }) { (device, photo) ->
                    DeviceItem(
                        device = device,
                        lastPhotoUri = photo?.thumbnailUrl,
                        onClicked = onDeviceClicked
                    )
                }
            }
        }

        else -> EmptyDevicesScreen(modifier)
    }
}

@Composable
private fun EmptyDevicesScreen(modifier: Modifier = Modifier) {
    Box(modifier) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(R.string.photo_widget_no_devices)
        )
    }
}

//region Preview
@Preview
@Composable
private fun PhotoWidgetConfigurationScreenPreview() {
    AppBackground {
        PhotoWidgetConfigurationScreenContent(
            modifier = Modifier.fillMaxSize(),
            devicesWithLastPhoto = persistentMapOf(
                Device(1, "Device 1") to Photo(1, LocalDateTime.now(), "", "", "", ""),
                Device(12, "Device 2") to null,
                Device(123, "Device 3") to null,
            ),
            isLoading = false,
            onDeviceClicked = {}
        )
    }
}
//endregion