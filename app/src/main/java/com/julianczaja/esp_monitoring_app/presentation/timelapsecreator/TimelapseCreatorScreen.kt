package com.julianczaja.esp_monitoring_app.presentation.timelapsecreator

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.IntSliderRow
import com.julianczaja.esp_monitoring_app.components.SwitchWithLabel
import com.julianczaja.esp_monitoring_app.data.utils.formatBytes
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import com.julianczaja.esp_monitoring_app.presentation.timelapsecreator.TimelapseCreatorScreenViewModel.UiState
import java.time.LocalDateTime


@Composable
fun TimelapseCreatorScreen(
    onSetAppBarTitle: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: TimelapseCreatorScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState !is UiState.Configure) {
        viewModel.onBackPressed()
    }

    LaunchedEffect(true) {
        onSetAppBarTitle(R.string.timelapse_creator_screen_title)
        viewModel.eventFlow.collect { event ->
            when (event) {
                is TimelapseCreatorScreenViewModel.Event.ShowError -> snackbarHostState.showSnackbar(
                    message = context.getString(event.messageId),
                    duration = SnackbarDuration.Short
                )

                TimelapseCreatorScreenViewModel.Event.ShowSaved -> snackbarHostState.showSnackbar(
                    message = context.getString(R.string.timelapse_saved),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    TimelapseCreatorScreenContent(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(MaterialTheme.spacing.large),
        uiState = uiState,
        onFrameRateChanged = viewModel::updateFrameRate,
        onCompressionRateChanged = viewModel::updateCompressionRate,
        onIsHighQualityChanged = viewModel::updateIsHighQuality,
        onIsReversedChanged = viewModel::updateIsReversed,
        onStartClicked = viewModel::start,
        onSaveTimelapseClicked = viewModel::saveTimelapse
    )
}

@Composable
private fun TimelapseCreatorScreenContent(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onFrameRateChanged: (Int) -> Unit,
    onCompressionRateChanged: (Int) -> Unit,
    onIsHighQualityChanged: (Boolean) -> Unit,
    onIsReversedChanged: (Boolean) -> Unit,
    onStartClicked: () -> Unit,
    onSaveTimelapseClicked: () -> Unit,
) {
    when (uiState) {
        is UiState.Configure -> TimelapseCreatorConfigureScreen(
            modifier = modifier,
            uiState = uiState,
            onFrameRateChanged = onFrameRateChanged,
            onCompressionRateChanged = onCompressionRateChanged,
            onIsHighQualityChanged = onIsHighQualityChanged,
            onIsReversedChanged = onIsReversedChanged,
            onStartClicked = onStartClicked
        )

        is UiState.Process -> TimelapseCreatorProcessScreen(
            modifier = modifier,
            uiState = uiState
        )

        is UiState.Preview -> TimelapseCreatorPreviewScreen(
            modifier = modifier,
            uiState = uiState,
            onSaveTimelapseClicked = onSaveTimelapseClicked
        )
    }
}

@Composable
private fun TimelapseCreatorConfigureScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Configure,
    onFrameRateChanged: (Int) -> Unit,
    onCompressionRateChanged: (Int) -> Unit,
    onIsHighQualityChanged: (Boolean) -> Unit,
    onIsReversedChanged: (Boolean) -> Unit,
    onStartClicked: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.timelapse_photos_count_label))
                Text(text = uiState.photosCount.toString())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.timelapse_oldest_photo_label))
                Text(text = uiState.oldestPhotoDateTime.toPrettyString())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.timelapse_newest_photo_label))
                Text(text = uiState.newestPhotoDateTime.toPrettyString())
            }
            HorizontalDivider(modifier = Modifier.padding(top = MaterialTheme.spacing.medium))
            IntSliderRow(
                label = stringResource(R.string.frame_rate_label),
                value = uiState.frameRate,
                steps = 60,
                enabled = true,
                valueRange = 1f..60f,
                onValueChange = onFrameRateChanged
            )
            IntSliderRow(
                label = stringResource(R.string.compression_rate_label),
                value = uiState.compressionRate,
                steps = 31,
                enabled = true,
                valueRange = 1f..31f,
                onValueChange = onCompressionRateChanged
            )
            SwitchWithLabel(
                label = stringResource(R.string.high_quality_label),
                isChecked = uiState.isHighQuality,
                enabled = true,
                onCheckedChange = onIsHighQualityChanged
            )
            SwitchWithLabel(
                label = stringResource(R.string.reverse_order_label),
                isChecked = uiState.isReversed,
                enabled = true,
                onCheckedChange = onIsReversedChanged
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.timelapse_estimated_time_label))
                Text(text = stringResource(R.string.seconds_format).format(uiState.estimatedTime))
            }
        }
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val progressModifier = Modifier
            .padding(MaterialTheme.spacing.medium)
            .height(MaterialTheme.spacing.medium)
            .fillMaxWidth()
        when {
            uiState.processProgress > 0f -> {
                Text(stringResource(R.string.timelapse_processing_label))
                LinearProgressIndicator(
                    progress = { uiState.processProgress },
                    strokeCap = StrokeCap.Round,
                    modifier = progressModifier
                )
            }
            uiState.unZipProgress > 0f -> {
                Text(stringResource(R.string.timelapse_unzipping_label))
                LinearProgressIndicator(
                    progress = { uiState.unZipProgress },
                    strokeCap = StrokeCap.Round,
                    modifier = progressModifier
                )
            }
            uiState.downloadedBytes > 0L -> {
                Text(stringResource(R.string.timelapse_downloaded_format, formatBytes(uiState.downloadedBytes)))
                LinearProgressIndicator(
                    strokeCap = StrokeCap.Round,
                    modifier = progressModifier
                )
            }
            else -> Text(stringResource(R.string.waiting_for_server))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun TimelapseCreatorPreviewScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Preview,
    onSaveTimelapseClicked: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
            }
    }

    LaunchedEffect(key1 = uiState) {
        exoPlayer.apply {
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
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(exoPlayer.videoSize.pixelWidthHeightRatio)
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
                Text(text = formatBytes(uiState.timelapseData.sizeBytes))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.timelapse_duration_label))
                Text(text = stringResource(id = R.string.seconds_format, uiState.timelapseData.durationSeconds))
            }
        }
        if (!uiState.isSaved) {
            Button(
                onClick = onSaveTimelapseClicked,
                enabled = !uiState.isBusy
            ) {
                Text(text = stringResource(id = R.string.save_action))
            }
        }
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun TimelapseCreatorConfigureDialogPreview() {
    AppBackground(Modifier.height(400.dp)) {
        TimelapseCreatorScreenContent(
            modifier = Modifier.fillMaxSize(),
            uiState = UiState.Configure(
                photosCount = 321,
                newestPhotoDateTime = LocalDateTime.of(2024, 6, 14, 14, 44),
                oldestPhotoDateTime = LocalDateTime.of(2024, 5, 14, 14, 44),
                frameRate = 30,
                compressionRate = 3,
                isHighQuality = true,
                isReversed = true,
                estimatedTime = 10.2f
            ),
            onFrameRateChanged = {},
            onCompressionRateChanged = {},
            onIsHighQualityChanged = {},
            onIsReversedChanged = {},
            onStartClicked = {},
            onSaveTimelapseClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun TimelapseCreatorProcessDialogPreview() {
    AppBackground(Modifier.height(400.dp)) {
        TimelapseCreatorScreenContent(
            modifier = Modifier.fillMaxSize(),
            uiState = UiState.Process(1L, 1f, 0.2f),
            onFrameRateChanged = {},
            onCompressionRateChanged = {},
            onIsHighQualityChanged = {},
            onIsReversedChanged = {},
            onStartClicked = {},
            onSaveTimelapseClicked = {}
        )
    }
}
//endregion