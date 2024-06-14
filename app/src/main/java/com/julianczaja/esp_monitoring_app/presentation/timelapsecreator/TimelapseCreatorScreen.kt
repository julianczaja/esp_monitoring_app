package com.julianczaja.esp_monitoring_app.presentation.timelapsecreator

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.ErrorText
import com.julianczaja.esp_monitoring_app.components.IntSliderRow
import com.julianczaja.esp_monitoring_app.components.SwitchWithLabel
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import com.julianczaja.esp_monitoring_app.presentation.timelapsecreator.TimelapseCreatorScreenViewModel.UiState
import java.time.LocalDateTime


@Composable
fun TimelapseCreatorScreen(
    onSetAppBarTitle: (Int) -> Unit,
    viewModel: TimelapseCreatorScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState is UiState.Done || uiState is UiState.Error) {
        viewModel.onBackPressed()
    }

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
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Photos count: ")
            Text(text = uiState.photosCount.toString())
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Newest photo: ")
            Text(text = uiState.newestPhotoDateTime.toPrettyString())
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Oldest photo: ")
            Text(text = uiState.oldestPhotoDateTime.toPrettyString())
        }
        HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))
        IntSliderRow(
            label = stringResource(R.string.frame_rate_label),
            value = uiState.frameRate,
            steps = 60,
            enabled = true,
            valueRange = 1f..60f,
            onValueChange = onFrameRateChanged
        )
        SwitchWithLabel(
            label = stringResource(R.string.high_quality_label),
            isChecked = uiState.isHighQuality,
            enabled = true,
            onCheckedChange = onIsHighQualityChanged
        )
        HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.timelapse_estimated_time_label))
            Text(text = stringResource(R.string.seconds_format).format(uiState.estimatedTime))
        }
        HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))
        Button(onClick = onStartClicked) {
            Text(text = stringResource(id = R.string.start_action))
        }
    }
}

@Composable
private fun TimelapseCreatorProcessScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Process
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (uiState.isBusy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Text(text = "Download progress")
        LinearProgressIndicator(
            progress = { uiState.downloadProgress },
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .padding(MaterialTheme.spacing.medium)
                .height(MaterialTheme.spacing.medium)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Text(text = "Process progress")
        LinearProgressIndicator(
            progress = { uiState.processProgress },
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .padding(MaterialTheme.spacing.medium)
                .height(MaterialTheme.spacing.medium)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun TimelapseCreatorDoneScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Done
) {
    val context = LocalContext.current
    val exoPlayer = ExoPlayer.Builder(context).build()

    LaunchedEffect(key1 = uiState) {
        exoPlayer.apply {
            playWhenReady = false
            setMediaItem(MediaItem.fromUri(uiState.timelapseData.path))
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = modifier.padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MaterialTheme.spacing.medium)),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    hideController()
                    setShowRewindButton(false)
                    setShowFastForwardButton(false)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            }
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.timelapse_size_label))
            Text(text = uiState.timelapseData.size)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.timelapse_duration_label))
            Text(text = uiState.timelapseData.duration)
        }
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
            uiState = UiState.Configure(
                photosCount = 321,
                newestPhotoDateTime = LocalDateTime.of(2024, 6, 14, 14, 44),
                oldestPhotoDateTime = LocalDateTime.of(2024, 5, 14, 14, 44),
                frameRate = 30,
                estimatedTime = 10.2f
            ),
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
private fun TimelapseCreatorErrorScreenPreview() {
    AppBackground {
        TimelapseCreatorErrorScreen(uiState = UiState.Error(R.string.unknown_error_message))
    }
}
//endregion
