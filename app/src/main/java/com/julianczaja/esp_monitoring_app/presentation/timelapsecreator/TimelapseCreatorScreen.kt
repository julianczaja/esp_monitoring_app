package com.julianczaja.esp_monitoring_app.presentation.timelapsecreator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.ErrorText
import com.julianczaja.esp_monitoring_app.components.IntSliderRow
import com.julianczaja.esp_monitoring_app.components.SwitchWithLabel
import com.julianczaja.esp_monitoring_app.domain.model.TimelapseData
import com.julianczaja.esp_monitoring_app.presentation.timelapsecreator.TimelapseCreatorScreenViewModel.UiState


@Composable
fun TimelapseCreatorScreen(
    onSetAppBarTitle: (Int) -> Unit,
    viewModel: TimelapseCreatorScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        onSetAppBarTitle(R.string.timelapse_creator_screen_title)
    }

    TimelapseCreatorScreenContent(
        modifier = Modifier.fillMaxSize(),
        uiState = uiState,
        onFrameRateChanged = viewModel::updateFrameRate,
        onIsHighQualityChanged = viewModel::updateIsHighQuality,
        onStartClicked = viewModel::start
    )
}

@Composable
private fun TimelapseCreatorScreenContent(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onFrameRateChanged: (Int) -> Unit,
    onIsHighQualityChanged: (Boolean) -> Unit,
    onStartClicked: () -> Unit
) {
    when (uiState) {
        is UiState.Configure -> TimelapseCreatorConfigureScreen(
            modifier = modifier,
            uiState = uiState,
            onFrameRateChanged = onFrameRateChanged,
            onIsHighQualityChanged = onIsHighQualityChanged,
            onStartClicked = onStartClicked
        )

        is UiState.Process -> TimelapseCreatorProcessScreen(
            modifier = modifier,
            uiState = uiState
        )

        is UiState.Done -> TimelapseCreatorDoneScreen(
            modifier = modifier,
            uiState = uiState
        )

        is UiState.Error -> TimelapseCreatorErrorScreen(
            modifier = modifier,
            uiState = uiState
        )
    }
}

@Composable
private fun TimelapseCreatorConfigureScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Configure,
    onFrameRateChanged: (Int) -> Unit,
    onIsHighQualityChanged: (Boolean) -> Unit,
    onStartClicked: () -> Unit
) {
    Column(modifier) {
        Text(text = "Configure")
        Text(text = "Photos: ${uiState.photosCount}")
        Text(text = "Estimated time: ${uiState.estimatedTime}s")
        IntSliderRow(
            label = "Frame rate",
            value = uiState.frameRate,
            steps = 60,
            enabled = true,
            valueRange = 1f..60f,
            onValueChange = onFrameRateChanged
        )
        SwitchWithLabel(
            label = "High quality",
            isChecked = uiState.isHighQuality,
            enabled = true,
            onCheckedChange = onIsHighQualityChanged
        )
        Button(onClick = onStartClicked) {
            Text(text = "Start")
        }
    }
}

@Composable
private fun TimelapseCreatorProcessScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Process
) {
    Column(modifier) {
        if (uiState.isBusy) {
            LinearProgressIndicator()
        }
        Text(text = "Processing...")
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Download progress")
        LinearProgressIndicator(progress = { uiState.downloadProgress })

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Process progress")
        LinearProgressIndicator(progress = { uiState.processProgress })
    }
}

@Composable
private fun TimelapseCreatorDoneScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Done
) {
    Column(modifier) {
        Text(text = "Done")
        Text(text = "Path: ${uiState.timelapseData}")
    }
}

@Composable
private fun TimelapseCreatorErrorScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Error
) {
    Box(modifier) {
        ErrorText(text = stringResource(uiState.messageId), modifier = Modifier.align(Alignment.Center))
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun TimelapseCreatorConfigureScreenPreview() {
    AppBackground {
        TimelapseCreatorConfigureScreen(
            uiState = UiState.Configure(photosCount = 321, estimatedTime = 10.2f),
            onFrameRateChanged = {},
            onIsHighQualityChanged = {},
            onStartClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun TimelapseCreatorProcessScreenPreview() {
    AppBackground {
        TimelapseCreatorProcessScreen(uiState = UiState.Process(true, .5f, 0f))
    }
}

@PreviewLightDark
@Composable
private fun TimelapseCreatorDoneScreenPreview() {
    AppBackground {
        TimelapseCreatorDoneScreen(uiState = UiState.Done(TimelapseData("path", "size", "duration")))
    }
}

@PreviewLightDark
@Composable
private fun TimelapseCreatorErrorScreenPreview() {
    AppBackground {
        TimelapseCreatorErrorScreen(uiState = UiState.Error(R.string.unknown_error_message))
    }
}
//endregion
